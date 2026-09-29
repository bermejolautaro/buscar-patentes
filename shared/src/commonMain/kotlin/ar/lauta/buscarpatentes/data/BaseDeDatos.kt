package ar.lauta.buscarpatentes.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters

/**
 * La base del juego, la misma en los dos teléfonos (D6 de la 006).
 *
 * Las entidades y la versión no cambian con la 006: el identity hash es el de siempre, y por
 * eso el Android abre su archivo de hoy sin migrar nada. Cada sistema la arma con su builder:
 * el Android con las migraciones 1→7 (`androidMain`), el iPhone con el driver de SQLite
 * incluido y sin migraciones, porque toda base de iPhone nace en la 7.
 */
@Database(
    entities = [
        RegistroDeCaptura::class,
        EstadoDelJuego::class,
        Recorrido::class,
        PuntoDeTrayecto::class,
        Voto::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Convertidores::class)
@ConstructedBy(BaseDeDatosConstructor::class)
abstract class BaseDeDatos : RoomDatabase() {

    abstract fun registros(): RegistroDao
    abstract fun estadoDelJuego(): EstadoDelJuegoDao
    abstract fun recorridos(): RecorridoDao
    abstract fun puntos(): PuntoDeTrayectoDao
    abstract fun votos(): VotoDao

    companion object {
        /** El nombre del archivo, igual en los dos teléfonos y en el respaldo. */
        const val ARCHIVO = "buscar-patentes.db"
    }
}

/** Lo implementa Room, con KSP, para cada target. */
@Suppress("KotlinNoActualForExpect")
expect object BaseDeDatosConstructor : RoomDatabaseConstructor<BaseDeDatos> {
    override fun initialize(): BaseDeDatos
}
