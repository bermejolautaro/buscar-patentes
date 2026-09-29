package ar.lauta.buscarpatentes.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import ar.lauta.buscarpatentes.plataforma.Carpetas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * La base del iPhone (D6, P1): el mismo esquema que el Android, con el SQLite que trae Room.
 *
 * Sin migraciones: toda base de iPhone nace en la 7, o le llega por respaldo desde un
 * Android que ya está en la 7. Las que vengan de la 8 en adelante se escriben en común.
 */
fun construirBase(): BaseDeDatos =
    Room.databaseBuilder<BaseDeDatos>(name = Carpetas.base + "/" + BaseDeDatos.ARCHIVO)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
