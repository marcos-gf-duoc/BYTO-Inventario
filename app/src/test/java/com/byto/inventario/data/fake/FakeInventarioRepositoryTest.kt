package com.byto.inventario.data.fake

import com.byto.inventario.data.model.Alerta
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.NuevoMovimiento
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.repository.ReglaNegocioException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de registrarMovimiento sobre los datos sintéticos de T-ST-1:
 * DE-03 = 7 (Bajo, alerta abierta), RH-01 = 3 (Crítico), MU-01 = 0 (Sin stock + reposición),
 * TE-02 = 20 (Normal, sin alertas).
 *
 * Las funciones del repositorio son `suspend`; runBlocking las ejecuta y espera el resultado,
 * algo aceptable en una prueba (en la app nunca se bloquea así el hilo principal).
 */
class FakeInventarioRepositoryTest {

    private lateinit var repositorio: FakeInventarioRepository

    /** @Before corre antes de cada prueba: cada una parte de datos limpios. */
    @Before
    fun preparar() {
        // Sin retardo: las pruebas no necesitan simular la red.
        repositorio = FakeInventarioRepository(retardoMs = 0)
    }

    // ---------- Ayudantes ----------

    private fun movimiento(
        tipo: TipoMovimiento,
        codigo: String,
        cantidad: Int,
        motivo: String? = null,
        ubicacion: String = "T-ST-1"
    ) = NuevoMovimiento(tipo, ubicacion, codigo, cantidad, operadorId = "OP-001", motivo = motivo)

    private fun stock(codigo: String, ubicacion: String = "T-ST-1"): Int = runBlocking {
        repositorio.obtenerExistencia(ubicacion, codigo).existencia.stock
    }

    private fun alertas(codigo: String, tipo: TipoAlerta): List<Alerta> = runBlocking {
        repositorio.obtenerAlertas(ubicacionId = "T-ST-1", tipo = tipo).filter { it.codigoInterno == codigo }
    }

    /** Ejecuta el bloque y devuelve el código del error de negocio; falla si no hubo error. */
    private fun codigoDeError(bloque: suspend () -> Unit): String {
        try {
            runBlocking { bloque() }
        } catch (e: ReglaNegocioException) {
            return e.codigo
        }
        fail("Se esperaba un ReglaNegocioException")
        return ""
    }

    // ---------- Rechazos ----------

    @Test
    fun salidaMayorAlStockSeRechazaYNoCambiaNada() {
        val codigo = codigoDeError { repositorio.registrarMovimiento(movimiento(TipoMovimiento.SALIDA, "DE-03", 8)) }
        assertEquals("STOCK_INSUFICIENTE", codigo)
        assertEquals(7, stock("DE-03"))
    }

    @Test
    fun ajusteSinMotivoSeRechaza() {
        val codigo = codigoDeError { repositorio.registrarMovimiento(movimiento(TipoMovimiento.AJUSTE, "RH-01", -1)) }
        assertEquals("MOTIVO_OBLIGATORIO", codigo)
        assertEquals(3, stock("RH-01"))
    }

    @Test
    fun ajusteConResultadoNegativoSeRechaza() {
        val codigo = codigoDeError {
            repositorio.registrarMovimiento(movimiento(TipoMovimiento.AJUSTE, "RH-01", -4, motivo = "Merma"))
        }
        assertEquals("STOCK_NEGATIVO", codigo)
        assertEquals(3, stock("RH-01"))
    }

    @Test
    fun noSeRegistraEnUbicacionInactivaNiEnCasaMatriz() {
        assertEquals("UBICACION_INACTIVA", codigoDeError {
            repositorio.registrarMovimiento(movimiento(TipoMovimiento.ENTRADA, "TE-01", 1, ubicacion = "T-CC-3"))
        })
        assertEquals("UBICACION_SIN_STOCK", codigoDeError {
            repositorio.registrarMovimiento(movimiento(TipoMovimiento.ENTRADA, "TE-01", 1, ubicacion = "CM"))
        })
    }

    // ---------- Movimiento guardado ----------

    @Test
    fun movimientoGuardaStockAntesDespuesYOperador() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.ENTRADA, "TE-02", 5))
        val guardado = resultado.movimiento
        assertEquals(20, guardado.stockAntes)
        assertEquals(25, guardado.stockDespues)
        assertEquals("OP-001", guardado.operadorId)
        assertEquals(25, stock("TE-02"))
        // El historial lo devuelve primero porque es el más reciente.
        assertEquals(guardado.id, repositorio.obtenerMovimientos().first().id)
    }

    // ---------- Conteo ----------

    @Test
    fun conteoSinDiferenciaNoGeneraAlertas() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.CONTEO, "TE-02", 20))
        assertTrue(resultado.alertasGeneradas.isEmpty())
        assertEquals(20, stock("TE-02"))
    }

    @Test
    fun conteoConDiferenciaAbreDescalceConLaDiferencia() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.CONTEO, "TE-02", 15))
        val descalce = resultado.alertasGeneradas.single()
        assertEquals(TipoAlerta.DESCALCE, descalce.tipo)
        assertEquals(-5, descalce.diferencia)
        // 15 sigue siendo Normal: no abre stock bajo. El conteo reemplaza el stock.
        assertEquals(15, stock("TE-02"))
    }

    @Test
    fun conteoBajoElMinimoAbreDescalceYStockBajo() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.CONTEO, "TE-02", 8))
        assertEquals(
            listOf(TipoAlerta.DESCALCE, TipoAlerta.STOCK_BAJO),
            resultado.alertasGeneradas.map { it.tipo }
        )
    }

    // ---------- Stock bajo: apertura, actualización y cierre ----------

    @Test
    fun salidaBajoElMinimoAbreStockBajo() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.SALIDA, "TE-02", 12))
        val alerta = resultado.alertasGeneradas.single()
        assertEquals(TipoAlerta.STOCK_BAJO, alerta.tipo)
        assertEquals(EstadoStock.BAJO, alerta.nivel)
        assertEquals(EstadoAlerta.ABIERTA, alerta.estado)
    }

    @Test
    fun siYaHabiaStockBajoSoloSeActualizaSuNivel() = runBlocking {
        // DE-03 ya tiene una alerta de stock bajo abierta (nivel BAJO, stock 7).
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.SALIDA, "DE-03", 3))
        assertTrue("No debe abrir otra alerta", resultado.alertasGeneradas.isEmpty())
        val abiertas = alertas("DE-03", TipoAlerta.STOCK_BAJO).filter { it.estado == EstadoAlerta.ABIERTA }
        assertEquals(1, abiertas.size)
        assertEquals(EstadoStock.CRITICO, abiertas.single().nivel)
    }

    @Test
    fun alVolverAlMinimoSeCierraElStockBajo() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.ENTRADA, "DE-03", 3))
        assertTrue(resultado.alertasGeneradas.isEmpty())
        val stockBajo = alertas("DE-03", TipoAlerta.STOCK_BAJO)
        assertTrue(stockBajo.all { it.estado == EstadoAlerta.CERRADA })
    }

    // ---------- Reposición ----------

    @Test
    fun entradaCierraLaReposicionAbierta() = runBlocking {
        // MU-01 está sin stock y con reposición declarada por EB-001.
        repositorio.registrarMovimiento(movimiento(TipoMovimiento.ENTRADA, "MU-01", 5))
        assertEquals(EstadoAlerta.CERRADA, alertas("MU-01", TipoAlerta.REPOSICION).single().estado)
        // Con 5 unidades sigue bajo el mínimo: el stock bajo queda abierto, ahora en nivel BAJO.
        val stockBajo = alertas("MU-01", TipoAlerta.STOCK_BAJO).single()
        assertEquals(EstadoAlerta.ABIERTA, stockBajo.estado)
        assertEquals(EstadoStock.BAJO, stockBajo.nivel)
    }

    @Test
    fun salidaNoCierraLaReposicion() = runBlocking {
        // MU-01 tiene 0: primero se ajusta +2 para poder hacer una salida.
        repositorio.registrarMovimiento(movimiento(TipoMovimiento.AJUSTE, "MU-01", 2, motivo = "Error de registro"))
        repositorio.registrarMovimiento(movimiento(TipoMovimiento.SALIDA, "MU-01", 1))
        assertEquals(EstadoAlerta.ABIERTA, alertas("MU-01", TipoAlerta.REPOSICION).single().estado)
    }
}
