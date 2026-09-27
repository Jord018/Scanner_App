package com.LingTH.fridge


import Databases.ProductData
import Databases.daysUntilExpiry
import InventoryDatabase
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.LingTH.fridge.Barcode.Add
import com.LingTH.fridge.Barcode.Edit
import com.LingTH.fridge.Barcode.Scanner
import com.LingTH.fridge.Notification.BootReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.LingTH.fridge.Notification.scheduleExpiryChecksFromSettings
import com.LingTH.fridge.sortandfilter.FilterViewModel
import com.LingTH.fridge.sortandfilter.FilterViewModelFactory
import com.LingTH.fridge.sortandfilter.getPrimaryCategory
import com.LingTH.fridge.ui.theme.MyApplicationTheme
import com.LingTH.fridge.ui.theme.expiryStatus

// Test tags shared with the UI tests
object UiTags {
    const val SEARCH_FIELD = "search_field"
    const val PRODUCT_GRID = "product_grid"
    const val PRODUCT_CARD = "product_card"
    const val EXPIRY_BADGE = "expiry_badge"
    const val EMPTY_STATE = "empty_state"
    const val SCAN_FAB = "scan_fab"
    const val ADD_FAB = "add_fab"
    const val SAVE_BUTTON = "save_button"
}

class MainActivity2 : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(this, "คุณปฏิเสธการอนุญาตแจ้งเตือน", Toast.LENGTH_SHORT).show()
                }
            }

            when {
                ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ok
                }

                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    AlertDialog.Builder(this)
                        .setTitle("แจ้งเตือนสำคัญ")
                        .setMessage("แอปต้องการสิทธิ์การแจ้งเตือนเพื่อให้แจ้งเตือนวันหมดอายุของสินค้า")
                        .setPositiveButton("อนุญาต") { _, _ ->
                            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("ยกเลิก", null)
                        .show()
                }

                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        cancelLegacyRepeatingAlarm(this)
        lifecycleScope.launch(Dispatchers.IO) {
            scheduleExpiryChecksFromSettings(applicationContext, replace = false)
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val route = navBackStackEntry?.destination?.route
                val isSettingsScreen = route == "settings"
                val isFilterScreen = route == "Sorting and Filter"
                val isTutorialScreen = route == "tutorial"

                var searchText by remember { mutableStateOf("") }
                val context = LocalContext.current
                val database = InventoryDatabase.getDatabase(context)
                val productDao = database.productDao()
                val filterViewModel: FilterViewModel = viewModel(
                    factory = FilterViewModelFactory(productDao)
                )
                LaunchedEffect(searchText) {
                    filterViewModel.setSearchText(searchText)

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = "test_channel"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(
                            channelId,
                            "Test Channel",
                            NotificationManager.IMPORTANCE_HIGH
                        )
                        notificationManager.createNotificationChannel(channel)
                    }
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        if (!isSettingsScreen && !isTutorialScreen) {
                            TopBar(
                                searchText = searchText,
                                onSearchTextChange = { searchText = it },
                                isFilterScreen = isFilterScreen,
                                onFilterChanged = { isFiltering ->
                                    if (isFiltering) {
                                        navController.navigate("Sorting and Filter") {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.popBackStack()
                                    }
                                },
                                onHelpClick = { navController.navigate("tutorial") }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (route == "productList" || route == null) {
                            ProductFabs(
                                onScan = { context.startActivity(Intent(context, Scanner::class.java)) },
                                onAddManually = { context.startActivity(Intent(context, Add::class.java)) }
                            )
                        }
                    },
                    bottomBar = {
                        if (!isTutorialScreen) {
                            BottomBar(navController, isSettingsScreen) { isSettings ->
                                if (isSettings) {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    NavigationGraph(
                        navController = navController,
                        paddingValues = paddingValues,
                        filterViewModel = filterViewModel
                    )
                }
            }
        }
    }
}


@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isFilterScreen: Boolean,
    onFilterChanged: (Boolean) -> Unit,
    onHelpClick: () -> Unit = {},
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        navigationIcon = {
            if (isFilterScreen) {
                IconButton(onClick = { onFilterChanged(false) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.iconapp),
                    contentDescription = "Logo Icon",
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                )
            }
        },
        title = {
            if (!isFilterScreen) {
                SearchField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Sort & Filter",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        actions = {
            if (!isFilterScreen) {
                IconButton(onClick = onHelpClick) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Tutorial")
                }
            }
            FilledIconButton(
                onClick = { onFilterChanged(!isFilterScreen) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isFilterScreen) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isFilterScreen) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text("Search your fridge", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
            .height(52.dp)
            .testTag(UiTags.SEARCH_FIELD)
    )
}


@Composable
fun ProductCard(
    product: ProductData,
    onClick: () -> Unit,
) {
    val category = getPrimaryCategory(product.categories)
    val status = expiryStatus(product.daysUntilExpiry())

    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.PRODUCT_CARD)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            SmartImageLoader(
                imagePath = product.image_url,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = status.color,
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .testTag(UiTags.EXPIRY_BADGE)
            ) {
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = product.product_name.ifBlank { "Unnamed item" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (category.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}


@Composable
fun BottomBar(
    navController: NavHostController,
    isSettingsScreen: Boolean,
    onSettingsChanged: (Boolean) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(
            selected = !isSettingsScreen,
            onClick = {
                if (isSettingsScreen) {
                    onSettingsChanged(false)
                } else {
                    navController.navigate("productList") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = {
                Icon(
                    if (!isSettingsScreen) Icons.Filled.Kitchen else Icons.Outlined.Kitchen,
                    contentDescription = null
                )
            },
            label = { Text("My Fridge") }
        )
        NavigationBarItem(
            selected = isSettingsScreen,
            onClick = { if (!isSettingsScreen) onSettingsChanged(true) },
            icon = {
                Icon(
                    if (isSettingsScreen) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = null
                )
            },
            label = { Text("Settings") }
        )
    }
}


@Composable
fun ProductFabs(onScan: () -> Unit, onAddManually: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onAddManually,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.testTag(UiTags.ADD_FAB)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Add manually")
        }
        ExtendedFloatingActionButton(
            onClick = onScan,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
            text = { Text("Scan") },
            modifier = Modifier.testTag(UiTags.SCAN_FAB)
        )
    }
}


// Older versions set a 10-second repeating alarm here; remove it if it is still registered
fun cancelLegacyRepeatingAlarm(context: Context) {
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, BootReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) ?: return
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    pendingIntent.cancel()
}
@Composable
fun SmartImageLoader(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    if (imagePath.isBlank()) {
        // รูป default ถ้า imagePath ว่าง
        Image(
            painter = painterResource(id = R.drawable.iconapp),
            contentDescription = "Default Image",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val painter = rememberAsyncImagePainter(
            model = imagePath,
            error = painterResource(id = R.drawable.iconapp), // กรณีโหลดไม่สำเร็จ
            placeholder = painterResource(id = R.drawable.iconapp)
        )

        Image(
            painter = painter,
            contentDescription = "Product Image",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ProductListScreen(
    navController: NavHostController,
    paddingValues: PaddingValues,
    viewModel: FilterViewModel
) {
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    ProductGrid(
        products = filteredProducts,
        hasAnyProducts = allProducts.isNotEmpty(),
        contentPadding = paddingValues,
        onProductClick = { product ->
            val intent = Intent(navController.context, Edit::class.java)
            intent.putExtra("productData", product)
            navController.context.startActivity(intent)
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ProductGrid(
    products: List<ProductData>,
    hasAnyProducts: Boolean,
    onProductClick: (ProductData) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    if (products.isEmpty()) {
        EmptyState(
            title = if (hasAnyProducts) "No matching items" else "Your fridge is empty",
            message = if (hasAnyProducts) "Try a different search or clear your filters."
            else "Tap Scan to add your first item.",
            modifier = Modifier.padding(contentPadding)
        )
        return
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val columnCount = when {
        screenWidth < 600.dp -> 2
        screenWidth < 900.dp -> 3
        else -> 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier
            .padding(contentPadding)
            .testTag(UiTags.PRODUCT_GRID),
        // Extra bottom space so the FABs don't cover the last row
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 136.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) }
            )
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(UiTags.EMPTY_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Kitchen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TutorialVideoScreen(
    navController: NavController,
    videoUri: Uri
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    // 1️⃣ สร้าง state สำหรับสถานะการเล่น
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    // 2️⃣ Listener สำหรับอัปเดตสถานะเมื่อ play/pause เปลี่ยน
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playWhenReady: Boolean) {
                isPlaying = playWhenReady
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("How to use Mr. Fridge", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video Player
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(300.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val pos = exoPlayer.currentPosition
                    exoPlayer.seekTo((pos - 10_000).coerceAtLeast(0))
                }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause()
                            else exoPlayer.play()
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPlaying) "หยุด" else "เล่น",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(onClick = {
                    val pos = exoPlayer.currentPosition
                    val dur = exoPlayer.duration
                    exoPlayer.seekTo((pos + 10_000).coerceAtMost(dur))
                }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Skip Button
            Button(
                onClick = {
                    // Tutorial is opened from the product list; "main" was never a route
                    if (!navController.popBackStack()) navController.navigate("productList")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp)
            ) {
                Text("Skip Tutorial")
            }
        }
    }
}
