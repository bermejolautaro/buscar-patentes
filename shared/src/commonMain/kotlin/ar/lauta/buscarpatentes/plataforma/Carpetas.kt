package ar.lauta.buscarpatentes.plataforma

/**
 * Dónde vive cada cosa en cada teléfono (P1 de la 006). Rutas absolutas, sin barra al final.
 *
 * En el iPhone la base y las fotos van en `Application Support`, fuera del alcance de la app
 * Archivos; los respaldos en `Documents`, que sí se ve desde Archivos y desde iTunes (D8).
 */
expect object Carpetas {
    /** La carpeta de la base. */
    val base: String

    /** Las fotos de las patentes. */
    val fotos: String

    /** Los respaldos que saca la app, incluido el previo a restaurar. */
    val respaldos: String

    /** Archivos de paso: se pueden perder sin que importe. */
    val temporal: String
}
