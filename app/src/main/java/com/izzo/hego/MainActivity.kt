package com.izzo.hego

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.core.content.ContextCompat
import com.izzo.hego.data.DeliveryJsonDataSource
import com.izzo.hego.data.HealthCertificateJsonDataSource
import com.izzo.hego.data.UserPropertiesJsonDataSource
import com.izzo.hego.model.Delivery
import com.izzo.hego.ui.theme.HeGoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeGoTheme {
                HeGoApp()
            }
        }
    }
}

@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeGoApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.FEED) }
    var acceptedDelivery by remember { mutableStateOf<Delivery?>(null) }
    val context = LocalContext.current
    val deliveries = remember { DeliveryJsonDataSource.loadFromAssets(context) }
    var globalIsValid by rememberSaveable {
        mutableStateOf(UserPropertiesJsonDataSource.load(context).isValid)
    }

    val noIndicatorItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent
        )
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { NavCircleIcon(iconRes = it.icon, contentDescription = it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                    colors = noIndicatorItemColors
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    expandedHeight = 88.dp,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    title = {
                        Image(
                            painter = painterResource(R.drawable.hego_isotipo_white),
                            contentDescription = "App logo",
                            modifier = Modifier.size(72.dp)
                        )
                    }
                )
            }
        ) { innerPadding ->
            when (currentDestination) {
                AppDestinations.FEED -> FeedScreen(
                    deliveries = deliveries,
                    onAcceptDelivery = { delivery -> acceptedDelivery = delivery },
                    isValid = globalIsValid,
                    onValidityChange = { globalIsValid = it },
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.MAP -> MapScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.HEALTH -> HealthScreen(
                    isValid = globalIsValid,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.CONFIRMATION -> ConfirmationScreen(
                    isValid = globalIsValid,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        acceptedDelivery?.let { delivery ->
            DeliveryAcceptedDialog(
                delivery = delivery,
                onClose = { acceptedDelivery = null }
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    FEED("Feed", R.drawable.ic_home),
    MAP("Map", R.drawable.ic_nav_map_pin),
    HEALTH("Health", R.drawable.ic_nav_health_cross),
    CONFIRMATION("Confirmation", R.drawable.ic_nav_confirmation),
}

@Composable
private fun NavCircleIcon(iconRes: Int, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(Color(0xFFFF9800), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun FeedScreen(
    deliveries: List<Delivery>,
    onAcceptDelivery: (Delivery) -> Unit,
    isValid: Boolean,
    onValidityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier.clickable { menuExpanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HamburgerMenuIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "deliveries",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isValid) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_health_check),
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text("Valid")
                            }
                        },
                        onClick = {
                            onValidityChange(true)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isValid) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_health_check),
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text("Invalid")
                            }
                        },
                        onClick = {
                            onValidityChange(false)
                            menuExpanded = false
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = Color(0xFF4CAF50),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "connected",
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Search icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_qr),
                contentDescription = "QR icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "List",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Map",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = true,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (deliveries.isEmpty()) {
                item {
                    Text(
                        text = "No deliveries yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(deliveries, key = { it.id }) { delivery ->
                    DeliveryCard(
                        delivery = delivery,
                        onAcceptDelivery = { onAcceptDelivery(delivery) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryCard(
    delivery: Delivery,
    onAcceptDelivery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFFF3E5),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = Color(0xFFFFCC80),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = restaurantTypeIcon(delivery.restaurantType)),
                contentDescription = "Restaurant type icon",
                tint = Color(0xFFE65100)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = delivery.restaurantName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = delivery.restaurantAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = Color(0xFFFF9800),
                    shape = CircleShape
                )
                .clickable(onClick = onAcceptDelivery),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun restaurantTypeIcon(restaurantType: String): Int {
    return when (restaurantType.lowercase()) {
        "pizza" -> R.drawable.ic_food_pizza
        "sushi" -> R.drawable.ic_food_sushi
        else -> R.drawable.ic_food_burger
    }
}

@Composable
private fun HamburgerMenuIcon(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize())
}

@Composable
fun HealthScreen(isValid: Boolean, modifier: Modifier = Modifier) {
    var currentStep by rememberSaveable { mutableStateOf(HealthStep.IDENTITY_CAPTURE) }
    var certificateValid by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userProperties = remember { UserPropertiesJsonDataSource.load(context) }
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (currentStep) {
            HealthStep.IDENTITY_CAPTURE -> {
                Text(
                    text = "Step 1: Identity verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (hasCameraPermission) {
                    CameraPreviewCard(
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                        label = "Front camera view"
                    )
                } else {
                    CameraPermissionCard(
                        onRequestPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
                Text(
                    text = "Center your face in the frame and continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        scope.launch {
                            currentStep = HealthStep.IDENTITY_READING
                            delay(2200)
                            currentStep = HealthStep.IDENTITY_VERIFIED
                            delay(1800)
                            currentStep = HealthStep.QR_CAPTURE
                        }
                    },
                    enabled = hasCameraPermission
                ) {
                    Text("Face centered")
                }
            }

            HealthStep.IDENTITY_READING -> {
                Text(
                    text = "Step 1: Identity verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HealthProgressStatus(
                    message = "reading biometric data...",
                    showLoader = true
                )
            }

            HealthStep.IDENTITY_VERIFIED -> {
                Text(
                    text = "Step 1: Identity verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HealthProgressStatus(
                    message = "biometrics verified",
                    showLoader = false,
                    subMessage = "identified: ${userProperties.name}"
                )
            }

            HealthStep.QR_CAPTURE -> {
                Text(
                    text = "Step 2: QR verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (hasCameraPermission) {
                    CameraPreviewCard(
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                        label = "Back camera view"
                    )
                } else {
                    CameraPermissionCard(
                        onRequestPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
                Text(
                    text = "Point the camera to the certificate QR and validate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        scope.launch {
                            currentStep = HealthStep.CERTIFICATE_READING
                            delay(2200)
                            certificateValid = isValid
                            currentStep = HealthStep.CERTIFICATE_VERIFIED
                            delay(1800)
                            currentStep = HealthStep.RESULT
                        }
                    },
                    enabled = hasCameraPermission
                ) {
                    Text("Scan QR and validate")
                }
            }

            HealthStep.CERTIFICATE_READING -> {
                Text(
                    text = "Step 2: QR verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HealthProgressStatus(
                    message = "reading certificate data...",
                    showLoader = true
                )
            }

            HealthStep.CERTIFICATE_VERIFIED -> {
                Text(
                    text = "Step 2: QR verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HealthProgressStatus(
                    message = "certificate verified",
                    showLoader = false
                )
            }

            HealthStep.RESULT -> {
                Text(
                    text = "Step 3: Certificate result",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                HealthValidationResult(isValid = certificateValid)

                Button(onClick = { currentStep = HealthStep.IDENTITY_CAPTURE }) {
                    Text("Start again")
                }
            }
        }
    }
}

private enum class HealthStep {
    IDENTITY_CAPTURE,
    IDENTITY_READING,
    IDENTITY_VERIFIED,
    QR_CAPTURE,
    CERTIFICATE_READING,
    CERTIFICATE_VERIFIED,
    RESULT,
}

@Composable
private fun HealthProgressStatus(
    message: String,
    showLoader: Boolean,
    subMessage: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showLoader) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = Color(0xFF2E7D32),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_health_check),
                        contentDescription = "Verified",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subMessage != null) {
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewCard(
    cameraSelector: CameraSelector,
    label: String,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraError by remember(cameraSelector) { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.TopStart
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            }
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .padding(10.dp)
                .background(
                    color = Color(0x88000000),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        cameraError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(
                        color = Color(0x88000000),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    DisposableEffect(previewView, lifecycleOwner, cameraSelector) {
        val currentPreviewView = previewView
        if (currentPreviewView == null) {
            return@DisposableEffect onDispose {}
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                if (!cameraProvider.hasCamera(cameraSelector)) {
                    cameraError = "Selected camera is not available"
                    return@addListener
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = currentPreviewView.surfaceProvider
                }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                    cameraError = null
                }.onFailure {
                    cameraError = "Unable to start camera preview"
                    Log.e("CameraPreviewCard", "Camera bind failed", it)
                }
            },
            mainExecutor
        )

        onDispose {
            cameraProviderFuture.addListener(
                {
                    runCatching {
                        cameraProviderFuture.get().unbindAll()
                    }
                },
                mainExecutor
            )
        }
    }
}

@Composable
private fun CameraPermissionCard(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                color = Color(0xFFE7E7E7),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Camera permission is required",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF616161)
            )

            Button(onClick = onRequestPermission) {
                Text("Grant camera permission")
            }
        }
    }
}

@Composable
private fun CameraMockCard(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                color = Color(0xFFE7E7E7),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF616161)
        )
    }
}

@Composable
private fun HealthValidationResult(isValid: Boolean) {
    val circleColor = if (isValid) Color(0xFF2E7D32) else Color(0xFFC62828)
    val resultText = if (isValid) "VALID" else "INVALID"
    val resultIcon = if (isValid) R.drawable.ic_health_check else R.drawable.ic_health_cross

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    color = circleColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(resultIcon),
                contentDescription = "Validation icon",
                modifier = Modifier.size(78.dp),
                tint = Color.White
            )
        }

        Text(
            text = resultText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = circleColor
        )
    }
}

@Composable
fun ConfirmationScreen(isValid: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val userProperties = remember { UserPropertiesJsonDataSource.load(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        HealthValidationResult(isValid = isValid)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "name:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userProperties.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DeliveryAcceptedDialog(
    delivery: Delivery,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = "delivery accepted",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "delivery number: ${delivery.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.map),
                        contentDescription = "Map",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_location_pin),
                        contentDescription = "Restaurant location",
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.Center)
                    )

                    Text(
                        text = delivery.restaurantAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(
                                color = Color(0x88000000),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    )
}