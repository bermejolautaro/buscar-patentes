package ar.lauta.buscarpatentes.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters

/**
 * La base del juego, la misma en los dos teléfonos (D6 de la 006).
 *
 * Cada sistema la arma con su builder: el Android con las migraciones 1→8 (`androidMain`), el
 * iPhone con el driver de SQLite incluido. La 8 suma las zonas de la 008 y es la primera
 * migración que corre en los dos: el SQL vive en común ([SQL_7_8]), y el iPhone la usa al abrir
 * un respaldo de la 7.
 */
@Database(
    entities = [
        RegistroDeCaptura::class,
        EstadoDelJuego::class,
        Recorrido::class,
        PuntoDeTrayecto::class,
        Voto::class,
        Zona::class,
        Cuadra::class,
    ],
    version = 8,
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
    abstract fun zonas(): ZonaDao

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
