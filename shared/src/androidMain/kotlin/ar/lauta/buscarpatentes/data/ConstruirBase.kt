package ar.lauta.buscarpatentes.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ar.lauta.buscarpatentes.domain.Probabilidad

/**
 * La base del Android: el mismo archivo, el mismo modo de Room y las mismas migraciones que
 * antes de la 006 (D6, FR-030). Pasar a esta versión es una actualización, no una
 * reinstalación.
 *
 * Las migraciones 1→7 usan `SupportSQLiteDatabase`, que solo existe acá. El iPhone no las
 * necesita: su base nace en la 7, o le llega por respaldo desde un Android que ya está en la 7.
 */
fun construirBase(context: Context): BaseDeDatos = Migraciones.construir(context)

private object Migraciones {

        /**
         * v1 → v2: aparece la tabla del estado del juego (User Story 3).
         *
         * Migración real, no destructiva: la app ya está instalada y puede tener patentes
         * guardadas. Borrarlas para agregar una tabla nueva sería tirar evidencia que el
         * Principio II dice que es sagrada.
         */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS estado_del_juego (
                        id INTEGER NOT NULL PRIMARY KEY,
                        numeroActual INTEGER NOT NULL,
                        avisosActivos INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v2 -> v3: aparecen los recorridos y sus puntos de trayecto (User Story 5).
         *
         * Igual que la anterior, no destructiva. `registro_de_captura.recorridoId` ya
         * existia desde v1, asi que las capturas viejas quedan con recorrido nulo, que es
         * exactamente lo que eran: capturas sueltas en la vereda.
         */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recorrido` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`iniciadoEn` INTEGER NOT NULL, " +
                        "`finalizadoEn` INTEGER, " +
                        "`estado` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `punto_de_trayecto` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`recorridoId` INTEGER NOT NULL, " +
                        "`latitud` REAL NOT NULL, " +
                        "`longitud` REAL NOT NULL, " +
                        "`precisionMetros` REAL NOT NULL, " +
                        "`registradoEn` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_punto_de_trayecto_recorridoId` " +
                        "ON `punto_de_trayecto` (`recorridoId`)",
                )
            }
        }

        /**
         * v3 -> v4: el recorrido guarda su camino ajustado a las calles (FR-031).
         *
         * Columna nullable y nada mas: los recorridos que ya estan guardados arrancan sin
         * ajustar, que es exactamente lo que son, y se ajustan solos la proxima vez que la
         * app abra con conexion. Ninguna fila se toca ni se pierde.
         */
        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recorrido` ADD COLUMN `caminoAjustado` TEXT")
            }
        }

        /**
         * v4 -> v5: aparecen los votos de permanencia.
         *
         * Tabla nueva y nada mas. Los registros que ya estan guardados quedan sin votos,
         * que es exactamente lo que son: vistos una vez y nunca vueltos a mirar. Sin votos
         * la probabilidad es la neutra, asi que ninguno arranca mintiendo.
         */
        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `voto` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`registroId` INTEGER NOT NULL, " +
                        "`valor` INTEGER NOT NULL, " +
                        "`votadoEn` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_voto_registroId` ON `voto` (`registroId`)",
                )
            }
        }

        /**
         * v5 -> v6: el jugador elige cómo se dibujan los recorridos (FR-003 de la 004).
         *
         * Una columna con `DEFAULT`, que es lo que resuelve solo el caso de la app ya
         * instalada: quien actualiza abre en cobertura, que es el modo que la feature quiere
         * mostrar primero. Ninguna fila se toca.
         *
         * Va en esta tabla y no en un almacén aparte porque es una preferencia del jugador,
         * igual que `avisosActivos`, que ya vive acá.
         */
        private val MIGRACION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `estado_del_juego` " +
                        "ADD COLUMN `modoMapa` TEXT NOT NULL DEFAULT 'COBERTURA'",
                )
            }
        }

        /**
         * v6 -> v7: la probabilidad arranca en 0 en vez de 5.
         *
         * Todas las patentes nacen con la columna en 0. Las que tenían más de 5 pasan a
         * contar desde 5 y conservan sus votos, así que muestran exactamente lo mismo. Las
         * que tenían 5 o menos pierden sus votos y quedan en 0: sin eso no hay forma de
         * que un historial como `[-1, +1]` termine en 0 contando desde cualquier base.
         *
         * El orden en que se leen los votos es el de [VotoDao.valoresDe]: la cuenta tiene
         * que dar lo mismo que la pantalla.
         */
        private val MIGRACION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `registro_de_captura` " +
                        "ADD COLUMN `probabilidadInicial` INTEGER NOT NULL DEFAULT 0",
                )
                val votos = mutableMapOf<Long, MutableList<Int>>()
                db.query("SELECT registroId, valor FROM voto ORDER BY registroId, votadoEn, id")
                    .use { c ->
                        while (c.moveToNext()) {
                            votos.getOrPut(c.getLong(0)) { mutableListOf() }.add(c.getInt(1))
                        }
                    }
                for ((registroId, valores) in votos) {
                    if (Probabilidad.conservaSusVotos(valores)) {
                        db.execSQL(
                            "UPDATE registro_de_captura SET probabilidadInicial = ? WHERE id = ?",
                            arrayOf<Any>(Probabilidad.INICIAL_VIEJA, registroId),
                        )
                    } else {
                        db.execSQL("DELETE FROM voto WHERE registroId = ?", arrayOf(registroId))
                    }
                }
            }
        }

        // Sin instancia guardada: la guarda el contenedor, que la cambia al restaurar un respaldo.
        fun construir(context: Context): BaseDeDatos =
            Room.databaseBuilder(
                context.applicationContext,
                BaseDeDatos::class.java,
                BaseDeDatos.ARCHIVO,
            )
                .addMigrations(
                    MIGRACION_1_2,
                    MIGRACION_2_3,
                    MIGRACION_3_4,
                    MIGRACION_4_5,
                    MIGRACION_5_6,
                    MIGRACION_6_7,
                )
                .build()
}
