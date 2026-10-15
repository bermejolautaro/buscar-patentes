package ar.lauta.buscarpatentes.data

/**
 * De la base 7 a la 8: las zonas de la 008 (D7).
 *
 * Suma dos tablas y un índice, y no toca ninguna de las que existen: las salidas y las patentes
 * quedan igual. El SQL está copiado **textual** del `createAllTables` que genera Room, porque Room
 * compara el esquema al abrir y una columna distinta tira la app.
 *
 * Es una lista y no un `Migration` porque cada teléfono llama a Room de otra forma: el Android
 * abre la base con `SupportSQLiteDatabase` y el iPhone con el driver. Lo común es el SQL, y cada
 * `ConstruirBase` lo corre con lo suyo.
 */
val SQL_7_8: List<String> = listOf(
    "CREATE TABLE IF NOT EXISTS `zona` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, " +
        "`borde` TEXT NOT NULL, `creadaEn` INTEGER NOT NULL, `cuentaDesde` INTEGER NOT NULL, `estado` TEXT NOT NULL, " +
        "`problema` TEXT, `terminadaEn` INTEGER, `porcentajeFinal` INTEGER)",
    "CREATE TABLE IF NOT EXISTS `cuadra` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `zonaId` INTEGER NOT NULL, " +
        "`nombre` TEXT, `forma` TEXT NOT NULL, `gemelaDe` INTEGER, `quitada` INTEGER NOT NULL, " +
        "`recorridaAlTerminar` INTEGER NOT NULL)",
    "CREATE INDEX IF NOT EXISTS `index_cuadra_zonaId` ON `cuadra` (`zonaId`)",
)
