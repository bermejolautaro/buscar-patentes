package ar.lauta.buscarpatentes

import ar.lauta.buscarpatentes.data.AlmacenFotos
import ar.lauta.buscarpatentes.data.BaseDeDatos

/**
 * Contenedor manual de dependencias (D7).
 *
 * Sin Hilt ni Koin: una base y un almacén de fotos. El Principio IV dice que un framework de
 * inyección acá es infraestructura que nadie pidió. Si algún día construir esto a mano duele de
 * verdad, se agrega el framework entonces.
 *
 * Desde la 006 lo arma cada sistema al arrancar —`App.onCreate` en el Android, el `AppDelegate`
 * en el iPhone— con la base que sabe construir, y lo usan igual las pantallas comunes y el código
 * de cada sistema: el servicio del recorrido y los avisos corren sin ninguna pantalla abierta.
 */
class Contenedor(val baseDeDatos: BaseDeDatos, val fotos: AlmacenFotos) {
    val registros by lazy { baseDeDatos.registros() }
    val estadoDelJuego by lazy { baseDeDatos.estadoDelJuego() }
    val recorridos by lazy { baseDeDatos.recorridos() }
    val puntos by lazy { baseDeDatos.puntos() }
    val votos by lazy { baseDeDatos.votos() }
    val zonas by lazy { baseDeDatos.zonas() }
}

/** El de la app. Lo fija [iniciarContenedor], antes que cualquier pantalla. */
lateinit var contenedor: Contenedor
    private set

private lateinit var abrirBase: () -> BaseDeDatos

/** [abrir] construye la base de este sistema. Se guarda para [reabrirContenedor]. */
fun iniciarContenedor(abrir: () -> BaseDeDatos, fotos: AlmacenFotos) {
    abrirBase = abrir
    contenedor = Contenedor(abrir(), fotos)
}

/**
 * Otra base sobre el mismo archivo, después de que restaurar lo reemplazó (R6 de la 006).
 *
 * Alcanza con cambiar el global: restaurar se hace desde Ajustes, y las pantallas que miran los
 * datos arrancan de cero al volver. Lo que corre sin pantalla toma el nuevo en su próximo uso.
 */
fun reabrirContenedor() {
    contenedor = Contenedor(abrirBase(), contenedor.fotos)
}
