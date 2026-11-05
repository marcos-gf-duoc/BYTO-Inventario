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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

/**
 * P17 Escanear código. [alLeerCodigo] recibe el EAN-13 leído (P08 lo busca);
 * [alEscribirCodigo] vuelve a P08 para escribirlo a mano.
 *
 * Flujo del permiso de cámara (obligatorio en Android para usarla):
 * 1. Si ya estaba concedido, se abre la cámara de inmediato.
 * 2. Si no, Android muestra su diálogo "¿Permitir que BYTO tome fotos?".
 * 3. Si el usuario lo niega, se explica para qué sirve y se ofrecen tres salidas:
 *    pedirlo otra vez, abrir los ajustes de la app o escribir el código a mano.
 */
@Composable
fun PantallaEscanearCodigo(
    alLeerCodigo: (String) -> Unit,
    alEscribirCodigo: () -> Unit,
    viewModel: EscanearCodigoViewModel = viewModel(factory = FabricaViewModels.Factory)
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current

    // Lanzador del diálogo de permiso del sistema; su respuesta llega a la lambda.
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
    // ON_RESUME: al volver de los ajustes de Android, el usuario pudo haber dado el permiso.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (estado.permiso == PermisoCamara.DENEGADO && tienePermisoCamara(contexto)) {
            viewModel.alResolverPermiso(true)
        }
    }
    LaunchedEffect(estado.codigoLeido) {
        estado.codigoLeido?.let(alLeerCodigo)
    }

    when (estado.permiso) {
        // Mientras Android muestra su diálogo, la pantalla queda en negro (como la cámara apagada).
        PermisoCamara.SIN_CONSULTAR -> Box(Modifier.fillMaxSize().background(Color.Black))
        PermisoCamara.CONCEDIDO -> ContenidoCamara(
            codigoInvalido = estado.codigoInvalido,
            camaraNoDisponible = estado.camaraNoDisponible,
            alDetectar = viewModel::alDetectarCodigo,
            alFallarCamara = viewModel::alFallarCamara,
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

/**
 * Si el usuario negó el permiso dos veces, Android ya no vuelve a mostrar el diálogo:
 * la única forma de darlo es desde los ajustes de la app.
 */
private fun abrirAjustesDeLaApp(contexto: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", contexto.packageName, null))
    contexto.startActivity(intent)
}

@Composable
private fun ContenidoCamara(
    codigoInvalido: Boolean,
    camaraNoDisponible: Boolean,
    alDetectar: (String) -> Unit,
    alFallarCamara: () -> Unit,
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
            modifier = Modifier.fillMaxSize()
        )
        MarcoGuia(Modifier.align(Alignment.Center))

        // Franja inferior oscura: el texto blanco se lee bien sobre cualquier imagen de la cámara.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Aparece y desaparece con un fundido, para que se note el cambio.
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

/**
 * La vista de CameraX (PreviewView) es una View clásica de Android, no un Composable:
 * AndroidView la inserta dentro de Compose.
 *
 * La cámara se "ata" al ciclo de vida de la pantalla (bindToLifecycle): CameraX la
 * enciende cuando la pantalla está visible y la apaga sola al salir de ella.
 */
@Composable
private fun VistaCamara(
    alDetectar: (String) -> Unit,
    alFallar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current
    val descripcion = stringResource(R.string.escanear_cd_camara)
    val vistaPrevia = remember { PreviewView(contexto) }
    // rememberUpdatedState: el analizador, creado una sola vez, siempre llama a la versión más nueva.
    val detectar by rememberUpdatedState(alDetectar)
    val fallar by rememberUpdatedState(alFallar)

    DisposableEffect(cicloDeVida) {
        // Hilo propio para analizar imágenes, así el hilo principal (la interfaz) no se traba.
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
                // KEEP_ONLY_LATEST: si el análisis va lento, se descartan los cuadros viejos.
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(hiloAnalisis, analizador) }
                // Cámara trasera si existe; si no (algunas tablets), la frontal.
                val selector = if (camaras.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                camaras.unbindAll()
                camaras.bindToLifecycle(cicloDeVida, selector, vista, analisis)
            } catch (e: Exception) {
                // Sin cámara o cámara ocupada por otra app: se ofrece escribir el código.
                fallar()
            }
        }, ContextCompat.getMainExecutor(contexto))

        onDispose {
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

/**
 * Rectángulo que indica dónde poner el código, con una línea que lo recorre de arriba
 * abajo. La animación muestra que el escáner está activo. Es decorativo (sin descripción).
 */
@Composable
private fun MarcoGuia(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "marcoGuia")
    // Avance de la línea: de 0 (arriba) a 1 (abajo) y de vuelta, sin fin.
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
        Box(
            modifier = Modifier
                .offset(y = (maxHeight - 3.dp) * avance)
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
        // Decorativo: el título dice lo mismo en texto.
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
            // Texto en el color de marca, como los demás botones con borde de la app.
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
