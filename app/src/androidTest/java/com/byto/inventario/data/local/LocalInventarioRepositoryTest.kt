package com.byto.inventario.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.FiltroMovimientos
import com.byto.inventario.data.model.NuevoMovimiento
import com.byto.inventario.data.model.TipoAlerta
import com.byto.inventario.data.model.TipoMovimiento
import com.byto.inventario.data.repository.ReglaNegocioException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas del repositorio con Room. Son "instrumentadas": corren en un emulador o
 * teléfono, porque Room necesita el SQLite de Android.
 *
 * Cada prueba usa una base en memoria (inMemoryDatabaseBuilder): se crea vacía,
 * no toca los datos reales de la app y desaparece al cerrarla.
 *
 * Ejecutar: clic derecho sobre la clase › Run, con un emulador encendido.
 */
@RunWith(AndroidJUnit4::class)
class LocalInventarioRepositoryTest {

    private lateinit var baseDatos: BytoDatabase
    private lateinit var repositorio: LocalInventarioRepository

    @Before
    fun preparar() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        baseDatos = Room.inMemoryDatabaseBuilder(contexto, BytoDatabase::class.java).build()
        // Sin retardo: las pruebas no necesitan simular la red.
        repositorio = LocalInventarioRepository(baseDatos, retardoMs = 0)
    }

    @After
    fun cerrar() {
        baseDatos.close()
    }

    // ---------- Ayudantes ----------

    private fun movimiento(tipo: TipoMovimiento, codigo: String, cantidad: Int) =
        NuevoMovimiento(tipo, "T-ST-1", codigo, cantidad, operadorId = "OP-001")

    private fun stock(repo: LocalInventarioRepository, codigo: String): Int = runBlocking {
        repo.obtenerExistencia("T-ST-1", codigo).existencia.stock
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

    // ---------- Carga inicial ----------

    @Test
    fun primerArranqueCargaLosDatosSinteticos() = runBlocking {
        assertEquals(13, repositorio.obtenerUbicaciones("GE-001").size)
        assertEquals(25, repositorio.obtenerExistencias("T-ST-1").size)
        assertEquals(6, repositorio.obtenerMovimientos().size)
        assertEquals(7, repositorio.obtenerAlertas().size)
        assertEquals(7, stock(repositorio, "DE-03"))
    }

    // ---------- Persistencia ----------

    @Test
    fun losCambiosQuedanGuardadosParaUnRepositorioNuevo() = runBlocking {
        repositorio.registrarMovimiento(movimiento(TipoMovimiento.SALIDA, "TE-02", 15))
        repositorio.cambiarEstadoUbicacion("T-CC-3", activa = true)

        // Un repositorio nuevo sobre la misma base simula volver a abrir la app:
        // debe leer lo guardado y no volver a cargar los datos iniciales.
        val otro = LocalInventarioRepository(baseDatos, retardoMs = 0)
        assertEquals(5, stock(otro, "TE-02"))
        assertEquals(7, otro.obtenerMovimientos().size)
        assertTrue(otro.obtenerUbicaciones("GE-001").first { it.id == "T-CC-3" }.activa)
    }

    @Test
    fun movimientoRechazadoNoGuardaNada() {
        val codigo = codigoDeError {
            repositorio.registrarMovimiento(movimiento(TipoMovimiento.SALIDA, "TE-02", 30))
        }
        assertEquals("STOCK_INSUFICIENTE", codigo)
        assertEquals(20, stock(repositorio, "TE-02"))
        assertEquals(6, runBlocking { repositorio.obtenerMovimientos().size })
    }

    // ---------- Movimientos y alertas ----------

    @Test
    fun salidaQueDejaStockBajoAbreAlertaYNumeraElMovimiento() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.SALIDA, "TE-02", 15))

        assertEquals("MOV-0007", resultado.movimiento.id)
        assertEquals(1, resultado.alertasGeneradas.size)
        assertEquals(TipoAlerta.STOCK_BAJO, resultado.alertasGeneradas[0].tipo)
        assertEquals("ALE-0008", resultado.alertasGeneradas[0].id)
    }

    @Test
    fun entradaCierraLaReposicionYElStockBajo() = runBlocking {
        // MU-01 en T-ST-1: 0 unidades, con stock bajo y reposición abiertos.
        repositorio.registrarMovimiento(movimiento(TipoMovimiento.ENTRADA, "MU-01", 12))

        val deMu01 = repositorio.obtenerAlertas(ubicacionId = "T-ST-1")
            .filter { it.codigoInterno == "MU-01" }
        assertTrue(deMu01.all { it.estado == EstadoAlerta.CERRADA })
    }

    @Test
    fun conteoConDiferenciaAbreDescalce() = runBlocking {
        val resultado = repositorio.registrarMovimiento(movimiento(TipoMovimiento.CONTEO, "TE-02", 18))

        val descalce = resultado.alertasGeneradas.first { it.tipo == TipoAlerta.DESCALCE }
        assertEquals(-2, descalce.diferencia)
        repositorio.marcarDescalceRevisado(descalce.id)
        assertEquals(
            EstadoAlerta.REVISADA,
            repositorio.obtenerAlertas(tipo = TipoAlerta.DESCALCE).first { it.id == descalce.id }.estado
        )
    }

    // ---------- Reposición ----------

    @Test
    fun reposicionGuardaLaObservacionYNoSeDuplica() {
        runBlocking {
            repositorio.declararReposicion("T-ST-1", "DE-03", "EB-001", "  Enviar desde B-ST-1  ")
        }
        val guardada = runBlocking {
            repositorio.obtenerAlertas(ubicacionId = "T-ST-1", tipo = TipoAlerta.REPOSICION)
                .first { it.codigoInterno == "DE-03" }
        }
        assertEquals("Enviar desde B-ST-1", guardada.observacion)

        val codigo = codigoDeError { repositorio.declararReposicion("T-ST-1", "DE-03", "EB-001") }
        assertEquals("REPOSICION_YA_ABIERTA", codigo)
    }

    @Test
    fun reposicionConObservacionMuyLargaSeRechaza() {
        val codigo = codigoDeError {
            repositorio.declararReposicion("T-ST-1", "DE-03", "EB-001", "x".repeat(201))
        }
        assertEquals("OBSERVACION_MUY_LARGA", codigo)
    }

    // ---------- Historial ----------

    @Test
    fun historialFiltraPorTipoYOrdenaDelMasReciente() = runBlocking {
        val entradas = repositorio.obtenerMovimientos(FiltroMovimientos(tipo = TipoMovimiento.ENTRADA))

        assertEquals(listOf("TE-04", "TE-01"), entradas.map { it.codigoInterno })
    }
}
