package ar.lauta.buscarpatentes.data

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import ar.lauta.buscarpatentes.plataforma.Carpetas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * La base del iPhone (D6, P1): el mismo esquema que el Android, con el SQLite que trae Room.
 *
 * Las bases de iPhone nacieron en la 7, así que las migraciones empiezan en la 7→8, la de las
 * zonas de la 008, y siguen con la 8→9, el peso de la 010. Corren también al abrir un respaldo
 * viejo. El SQL es común ([SQL_7_8], [SQL_8_9]).
 */
fun construirBase(): BaseDeDatos =
    Room.databaseBuilder<BaseDeDatos>(name = Carpetas.base + "/" + BaseDeDatos.ARCHIVO)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRACION_7_8, MIGRACION_8_9)
        .build()

private val MIGRACION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        SQL_7_8.forEach { connection.execSQL(it) }
    }
}

private val MIGRACION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        SQL_8_9.forEach { connection.execSQL(it) }
    }
}
