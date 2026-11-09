package com.byto.inventario.ui.screens.escaner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as VistaPrevia
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byto.inventario.R
import com.byto.inventario.di.FabricaViewModels
import com.byto.inventario.ui.theme.BYTOTheme
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * P17 Escanear código. [alLeerCodigo] recibe el EAN-13 leído (P08 lo busca);
 * [alEscribirCodigo] vuelve a P08 para escribirlo a mano.
 */
@Composable
fun PantallaEscanearCodigo(
    alLeerCodigo: (String) -> Unit,
    alEscribirCodigo: () -> Unit,
    viewModel: EscanearCodigoViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> viewModel.alResolverPermiso(concedido) }

    LaunchedEffect(Unit) {
        if (tienePermisoCamara(contexto)) {
            viewModel.alResolverPermiso(true)
        } else {
            pedirPermiso.launch(Manifest.permission.CAMERA)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (estado.permiso == PermisoCamara.DENEGADO && tienePermisoCamara(contexto)) {
            viewModel.alResolverPermiso(true)
        }
    }
    LaunchedEffect(estado.codigoLeido) {
        estado.codigoLeido?.let(alLeerCodigo)
    }

    when (estado.permiso) {
        PermisoCamara.SIN_CONSULTAR -> Box(Modifier.fillMaxSize().background(Color.Black))
        PermisoCamara.CONCEDIDO -> ContenidoCamara(
            codigoInvalido = estado.codigoInvalido,
            camaraNoDisponible = estado.camaraNoDisponible,
            linternaEncendida = estado.linternaEncendida,
            alDetectar = viewModel::alDetectarCodigo,
            alFallarCamara = viewModel::alFallarCamara,
            alAlternarLinterna = viewModel::alternarLinterna,
            alEscribirCodigo = alEscribirCodigo
        )
        PermisoCamara.DENEGADO -> ContenidoSinPermiso(
            alPermitir = { pedirPermiso.launch(Manifest.permission.CAMERA) },
            alAbrirAjustes = { abrirAjustesDeLaApp(contexto) },
            alEscribirCodigo = alEscribirCodigo
        )
    }
}

private fun tienePermisoCamara(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun abrirAjustesDeLaApp(contexto: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", contexto.packageName, null))
    contexto.startActivity(intent)
}

@Composable
private fun ContenidoCamara(
    codigoInvalido: Boolean,
    camaraNoDisponible: Boolean,
    linternaEncendida: Boolean,
    alDetectar: (String) -> Unit,
    alFallarCamara: () -> Unit,
    alAlternarLinterna: () -> Unit,
    alEscribirCodigo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (camaraNoDisponible) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.escanear_sin_camara),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Button(onClick = alEscribirCodigo, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.escanear_escribir))
                }
            }
            return@Box
        }

        VistaCamara(
            alDetectar = alDetectar,
            alFallar = alFallarCamara,
            linternaEncendida = linternaEncendida,
            modifier = Modifier.fillMaxSize()
        )
        MarcoGuia(Modifier.align(Alignment.Center))

        // Botón flotante para la linterna (flash)
        FloatingActionButton(
            onClick = alAlternarLinterna,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            containerColor = if (linternaEncendida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                painterResource(R.drawable.ic_flash),
                contentDescription = "Linterna",
                tint = if (linternaEncendida) Color.Yellow else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Franja inferior oscura
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(visible = codigoInvalido, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.escanear_codigo_invalido),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = stringResource(R.string.escanear_instruccion),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = alEscribirCodigo, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.escanear_escribir), color = Color.White)
            }
        }
    }
}

@Composable
private fun VistaCamara(
    alDetectar: (String) -> Unit,
    alFallar: () -> Unit,
    linternaEncendida: Boolean,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current
    val descripcion = stringResource(R.string.escanear_cd_camara)
    val vistaPrevia = remember { PreviewView(contexto) }
    val detectar by rememberUpdatedState(alDetectar)
    val fallar by rememberUpdatedState(alFallar)
    var cameraInstance by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(linternaEncendida) {
        try {
            cameraInstance?.cameraControl?.enableTorch(linternaEncendida)
        } catch (e: Exception) {}
    }

    DisposableEffect(cicloDeVida) {
        val hiloAnalisis = Executors.newSingleThreadExecutor()
        val analizador = AnalizadorCodigos { codigo -> detectar(codigo) }
        val futuroProveedor = ProcessCameraProvider.getInstance(contexto)
        var proveedor: ProcessCameraProvider? = null

        futuroProveedor.addListener({
            try {
                val camaras = futuroProveedor.get()
                proveedor = camaras
                val vista = Preview.Builder().build().also {
                    it.surfaceProvider = vistaPrevia.surfaceProvider
                }
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(hiloAnalisis, analizador) }
                val selector = if (camaras.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                camaras.unbindAll()
                cameraInstance = camaras.bindToLifecycle(cicloDeVida, selector, vista, analisis)
                try {
                    cameraInstance?.cameraControl?.enableTorch(linternaEncendida)
                } catch (e: Exception) {}
            } catch (e: Exception) {
                fallar()
            }
        }, ContextCompat.getMainExecutor(contexto))

        onDispose {
            try {
                cameraInstance?.cameraControl?.enableTorch(false)
            } catch (e: Exception) {}
            proveedor?.unbindAll()
            analizador.cerrar()
            hiloAnalisis.shutdown()
        }
    }

    AndroidView(
        factory = { vistaPrevia },
        modifier = modifier.semantics { contentDescription = descripcion }
    )
}

@Composable
private fun MarcoGuia(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "marcoGuia")
    val avance by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "lineaGuia"
    )
    BoxWithConstraints(
        modifier = modifier
            .size(width = 300.dp, height = 180.dp)
            .border(3.dp, Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        val recorrido = maxHeight - 3.dp
        Box(
            modifier = Modifier
                .offset { IntOffset(0, (recorrido.toPx() * avance).roundToInt()) }
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.inversePrimary)
        )
    }
}

@Composable
private fun ContenidoSinPermiso(
    alPermitir: () -> Unit,
    alAbrirAjustes: () -> Unit,
    alEscribirCodigo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camara),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }
        Text(
            text = stringResource(R.string.escanear_permiso_titulo),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.escanear_permiso_texto),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = alPermitir,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.escanear_permiso_permitir), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = alAbrirAjustes,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.escanear_permiso_ajustes), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = alEscribirCodigo, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.escanear_escribir), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@VistaPrevia(showBackground = true)
@Composable
private fun ContenidoSinPermisoPreview() {
    BYTOTheme {
        ContenidoSinPermiso(alPermitir = {}, alAbrirAjustes = {}, alEscribirCodigo = {})
    }
}
