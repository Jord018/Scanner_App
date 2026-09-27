package com.LingTH.fridge.Barcode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.LingTH.fridge.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Scanner : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ScannerScreen()
            }
        }
    }
}
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
            if (!granted) {
                Log.e("CameraPermission", "Camera permission denied")
            }
        }
    )

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            CameraPreview(modifier = Modifier.padding(innerPadding))

        }
    } else {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.NoPhotography,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Camera permission is required to scan barcodes",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
            }
        }
    }
}
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                startCamera(previewView, context) { control ->
                    cameraControl = control
                }
            }
        )
        // ส่ง cameraControl ไปให้ UI Overlay
        CameraOverlayUI(cameraControl)
    }
}


@Composable
fun CameraOverlayUI(cameraControl: CameraControl?) {
    var flashEnabled by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity
    val frameColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Offscreen so BlendMode.Clear only cuts the window in this overlay
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        // Transparent scan window with mint corner brackets
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 260.dp, height = 200.dp)
                .drawWithContent {
                    val cornerRadius = 20.dp.toPx()
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = Color.Transparent
                            blendMode = BlendMode.Clear
                        }
                        canvas.drawRoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            radiusX = cornerRadius,
                            radiusY = cornerRadius,
                            paint = paint
                        )
                    }
                    val len = 36.dp.toPx()
                    val stroke = 4.dp.toPx()
                    val w = size.width
                    val h = size.height
                    listOf(
                        Offset(0f, 0f) to Offset(len, 0f), Offset(0f, 0f) to Offset(0f, len),
                        Offset(w, 0f) to Offset(w - len, 0f), Offset(w, 0f) to Offset(w, len),
                        Offset(0f, h) to Offset(len, h), Offset(0f, h) to Offset(0f, h - len),
                        Offset(w, h) to Offset(w - len, h), Offset(w, h) to Offset(w, h - len),
                    ).forEach { (start, end) ->
                        drawLine(frameColor, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
                    }
                    drawContent()
                }
        )

        // Close button
        FilledTonalIconButton(
            onClick = { activity?.finish() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close scanner")
        }

        // Hint under the frame
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scan Barcode",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Point the camera at the product barcode",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // ปุ่มแฟลช
        FilledTonalIconButton(
            onClick = {
                cameraControl?.let {
                    flashEnabled = !flashEnabled
                    it.enableTorch(flashEnabled)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(32.dp)
                .size(64.dp)
        ) {
            Icon(
                imageVector = if (flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                contentDescription = "Toggle Flash",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}



private fun startCamera(
    previewView: PreviewView,
    context: Context,
    onCameraControlReady: (CameraControl) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        preview.surfaceProvider = previewView.surfaceProvider

        val resolutionSelectorBuilder = ResolutionSelector.Builder().apply {
            setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            setResolutionStrategy(
                ResolutionStrategy(
                    Size(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelectorBuilder.build())
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setOutputImageRotationEnabled(true)
            .build()

        val camera = cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview, imageAnalysis)
        val cameraControl = camera.cameraControl

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), ImageAnalyzer(context, cameraControl))

        onCameraControlReady(cameraControl)

    }, ContextCompat.getMainExecutor(context))
}




private class ImageAnalyzer(private val context: Context,private val cameraControl: CameraControl,) : ImageAnalysis.Analyzer {
    private var isProcessingBarcode = false
    fun Context.goToNextPage(targetActivity: Class<*>, extras: Map<String, String> = emptyMap()) {
        val intent = Intent(this, targetActivity)
        for ((key, value) in extras) {
            intent.putExtra(key, value)
        }
        startActivity(intent)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        Log.d("ImageAnalyzer", "Analyzing image...")
        Log.d(
            "ImageAnalyzer",
            "ImageProxy: width=${imageProxy.width}, height=${imageProxy.height}, format=${imageProxy.format}"
        )
        if (isProcessingBarcode) {
            imageProxy.close()  // ปิด imageProxy ทันทีหากกำลังประมวลผล
            return
        }
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            Log.d("ImageAnalyzer", "Image analyzed")


            // Barcode scanning logic integrated here
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_ITF,
                )
                .enableAllPotentialBarcodes() // Optional
                .build()

            val scanner = BarcodeScanning.getClient(options)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    // Debug: พิมพ์บาร์โค้ดทั้งหมดที่ detect ได้
                    barcodes.forEach {
                        Log.d(
                            "DEBUG_SCAN",
                            "Detected: rawValue=${it.rawValue}, format=${it.format}"
                        )
                    }

                    // กรองเฉพาะบาร์โค้ดที่มี rawValue ที่ใช้งานได้จริง
                    val barcode = barcodes.firstOrNull {
                        val value = it.rawValue
                        value != null &&
                                value.isNotBlank() &&
                                value.matches(Regex("^[0-9A-Za-z\\-]{6,30}$")) // <-- ปรับ regex ตามที่คุณใช้จริง
                    }

                    val barcodeValue = barcode?.rawValue

                    if (barcodeValue != null && !isProcessingBarcode) {
                        isProcessingBarcode = true
                        Log.d("Barcode", "✅ Valid barcode: $barcodeValue")

                        CoroutineScope(Dispatchers.Main).launch {
                            context.goToNextPage(Add::class.java, mapOf("barcode" to barcodeValue))
                        }
                    } else {
                        Log.d("Barcode", "❌ No valid barcode or already processing")
                        cameraControl.setZoomRatio(1.0f)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Barcode", "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    // อย่าปล่อยให้ isProcessingBarcode กลับเป็น false หากกำลังเปลี่ยนหน้าอยู่
                    // ถ้าอยากให้สแกนได้อีกครั้งหลังกลับมา ให้ reset flag ที่หน้าต่อไปแทน
                }
        } else {
            Log.e("ImageAnalyzer", "Media image is null")
            imageProxy.close()
        }
    }
}
