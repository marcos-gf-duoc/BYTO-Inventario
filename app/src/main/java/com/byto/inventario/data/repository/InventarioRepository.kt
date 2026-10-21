package com.byto.inventario.data.repository

import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.ExistenciaDetalle
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.Movimiento
import com.byto.inventario.data.model.NuevoMovimiento
import com.byto.inventario.data.model.Producto
import com.byto.inventario.data.model.ResultadoMovimiento
import com.byto.inventario.data.model.Tablero
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.Ubicacion
import com.byto.inventario.data.model.Usuario

/**
 * Contrato entre los ViewModels y el origen de los datos. Una función por endpoint
 * de docs/arquitectura_API.md (el número de cada una es el del endpoint).
 *
 * E1: lo implementa FakeInventarioRepository (memoria).
 * E2: lo implementará RemoteInventarioRepository (HTTP), sin cambiar los ViewModels.
 *
 * Todas son `suspend` porque en E2 serán llamadas de red que tardan.
 * Todas pueden lanzar [SinConexionException] o [ReglaNegocioException].
 */
interface InventarioRepository {

    /** 1. GET /usuarios/demo — usuarios ficticios para el login simulado. */
    suspend fun obtenerUsuariosDemo(): List<Usuario>

    /** 2. GET /ubicaciones?usuarioId= — ubicaciones del usuario (todas si es Gerente). */
    suspend fun obtenerUbicaciones(usuarioId: String): List<Ubicacion>

    /** 3. PATCH /ubicaciones/{id} — habilitar o deshabilitar una ubicación. */
    suspend fun cambiarEstadoUbicacion(ubicacionId: String, activa: Boolean): Ubicacion

    /**
     * 4. GET /ubicaciones/{id}/existencias?q=&estado= — inventario de una ubicación.
     * @param busqueda texto en nombre, código interno o EAN; null o vacío = todos.
     * @param estado filtra por estado del stock; null = todos.
     */
    suspend fun obtenerExistencias(
        ubicacionId: String,
        busqueda: String? = null,
        estado: EstadoStock? = null
    ): List<ExistenciaDetalle>

    /** 5. GET /ubicaciones/{id}/existencias/{codigoInterno} — detalle de un producto ahí. */
    suspend fun obtenerExistencia(ubicacionId: String, codigoInterno: String): ExistenciaDetalle

    /**
     * 6. GET /productos/ean/{ean} — búsqueda manual por código (pantalla P08).
     * Devuelve null si no hay producto con ese EAN: no es un error, es un resultado posible.
     */
    suspend fun buscarProductoPorEan(ean: String): Producto?

    /** 7. POST /movimientos — registra entrada, salida, ajuste o conteo. */
    suspend fun registrarMovimiento(nuevo: NuevoMovimiento): ResultadoMovimiento

    /** 8. GET /movimientos?... — historial, del más reciente al más antiguo. */
    suspend fun obtenerMovimientos(filtro: FiltroMovimientos = FiltroMovimientos()): List<Movimiento>

    /** 9. GET /alertas?ubicacionId=&tipo=&estado= — un filtro en null significa "todas". */
    suspend fun obtenerAlertas(
        ubicacionId: String? = null,
        tipo: TipoAlerta? = null,
        estado: EstadoAlerta? = null
    ): List<Alerta>

    /** 10. POST /alertas/reposicion — el Encargado declara una reposición en una tienda. */
    suspend fun declararReposicion(
        ubicacionId: String,
        codigoInterno: String,
        declaradaPor: String
    ): Alerta

    /** 11. PATCH /alertas/{id} — marca un descalce como revisado. */
    suspend fun marcarDescalceRevisado(alertaId: String): Alerta

    /** 12. GET /tablero — totales por ubicación y alertas abiertas. */
    suspend fun obtenerTablero(): Tablero
}
