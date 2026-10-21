package com.byto.inventario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.byto.inventario.data.model.EstadoAlerta
import com.byto.inventario.data.model.EstadoStock
import com.byto.inventario.data.model.Perfil
import com.byto.inventario.data.repository.InventarioRepository
import com.byto.inventario.data.repository.ReglaNegocioException
import com.byto.inventario.data.repository.SinConexionException
import com.byto.inventario.ui.components.ChipEstadoStock
import com.byto.inventario.ui.theme.BYTOTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // TEMPORAL (sub-paso 1c). viewModels{} crea el ViewModel una sola vez y lo conserva
    // al rotar la pantalla. La fábrica le entrega el repositorio desde el AppContainer.
    private val verificacionViewModel: VerificacionViewModel by viewModels {
        viewModelFactory {
            initializer {
                VerificacionViewModel((application as BytoApp).container.inventarioRepository)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BYTOTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // collectAsState: cada vez que cambia el StateFlow, la pantalla se redibuja.
                    val estado by verificacionViewModel.estado.collectAsState()
                    PantallaVerificacion(
                        estado = estado,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// ======================================================================
// TEMPORAL: todo lo que sigue solo comprueba que los datos falsos y el tema
// funcionan. Se borrará cuando existan las pantallas reales (login, etc.).
// ======================================================================

/** Totales que muestra la pantalla de verificación. */
data class TotalesVerificacion(
    val ubicaciones: Int,
    val productos: Int,
    val existencias: Int,
    val movimientos: Int,
    val alertasAbiertas: Int
)

/** Estado de la pantalla: cargando, con error o con los totales listos. */
data class EstadoVerificacion(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val totales: TotalesVerificacion? = null
)

/**
 * Pide los datos al repositorio (por su interfaz) y calcula los totales.
 * Solo usa funciones del contrato de la API, igual que lo hará el repositorio remoto.
 */
class VerificacionViewModel(
    private val repositorio: InventarioRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoVerificacion())
    val estado: StateFlow<EstadoVerificacion> = _estado.asStateFlow()

    init {
        // viewModelScope: la corrutina se cancela sola si el ViewModel se destruye.
        viewModelScope.launch {
            try {
                // El Gerente ve todas las ubicaciones; se busca por perfil para no fijar un ID.
                val gerente = repositorio.obtenerUsuariosDemo().first { it.perfil == Perfil.GERENTE }
                val ubicaciones = repositorio.obtenerUbicaciones(gerente.id)

                // async lanza las 13 consultas a la vez (cada una tarda ~300 ms);
                // awaitAll espera a que terminen todas.
                val existencias = ubicaciones
                    .map { ubicacion -> async { repositorio.obtenerExistencias(ubicacion.id) } }
                    .awaitAll()
                    .flatten()

                val movimientos = repositorio.obtenerMovimientos()
                val alertasAbiertas = repositorio.obtenerAlertas(estado = EstadoAlerta.ABIERTA)

                _estado.value = EstadoVerificacion(
                    cargando = false,
                    totales = TotalesVerificacion(
                        ubicaciones = ubicaciones.size,
                        // No hay endpoint de catálogo: se cuentan los productos distintos del inventario.
                        productos = existencias.map { it.producto.codigoInterno }.distinct().size,
                        existencias = existencias.size,
                        movimientos = movimientos.size,
                        alertasAbiertas = alertasAbiertas.size
                    )
                )
            } catch (e: SinConexionException) {
                _estado.value = EstadoVerificacion(cargando = false, error = true)
            } catch (e: ReglaNegocioException) {
                _estado.value = EstadoVerificacion(cargando = false, error = true)
            }
        }
    }
}

/** Solo dibuja el estado que recibe; no conoce el repositorio. */
@Composable
fun PantallaVerificacion(estado: EstadoVerificacion, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Desplazable: con letra grande del sistema el contenido puede no caber.
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.verificacion_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        when {
            estado.cargando -> {
                // Decorativo: el texto de al lado ya explica que está cargando.
                CircularProgressIndicator()
                Text(stringResource(R.string.verificacion_cargando))
            }
            estado.error || estado.totales == null -> {
                Text(
                    text = stringResource(R.string.verificacion_error),
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                val t = estado.totales
                Text(stringResource(R.string.verificacion_ubicaciones, t.ubicaciones))
                Text(stringResource(R.string.verificacion_productos, t.productos))
                Text(stringResource(R.string.verificacion_existencias, t.existencias))
                Text(stringResource(R.string.verificacion_movimientos, t.movimientos))
                Text(stringResource(R.string.verificacion_alertas_abiertas, t.alertasAbiertas))
            }
        }

        Text(
            text = stringResource(R.string.verificacion_titulo_estados),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        // FlowRow pasa los chips a la línea siguiente si no caben (por ejemplo, con letra grande).
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EstadoStock.entries.forEach { ChipEstadoStock(it) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaVerificacionPreview() {
    BYTOTheme {
        PantallaVerificacion(
            EstadoVerificacion(
                cargando = false,
                totales = TotalesVerificacion(13, 25, 300, 6, 7)
            )
        )
    }
}
