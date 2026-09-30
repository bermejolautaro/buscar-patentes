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
 * zonas de la 008. Corre también al abrir un respaldo de la 7. El SQL es común ([SQL_7_8]).
 */
fun construirBase(): BaseDeDatos =
    Room.databaseBuilder<BaseDeDatos>(name = Carpetas.base + "/" + BaseDeDatos.ARCHIVO)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRACION_7_8)
        .build()

private val MIGRACION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        SQL_7_8.forEach { connection.execSQL(it) }
    }
}
