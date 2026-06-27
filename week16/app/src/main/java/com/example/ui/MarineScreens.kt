package com.example.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import com.example.data.RegressionConstants
import com.example.data.MonthRegression
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarineRecord
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

// Navigation screens
enum class MarineScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LIVE("即時觀測", Icons.Default.Cloud),
    ANALYTICS("統計分析", Icons.Default.Assessment),
    HISTORY("歷史資料", Icons.Default.Search),
    WIND_POWER("風機計算", Icons.Default.Bolt),
    PATROL("巡邏紀錄", Icons.Default.CameraAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarineAppLayout(viewModel: MarineViewModel) {
    var currentScreen by remember { mutableStateOf(MarineScreen.LIVE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "台中港海氣象觀測系統",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                MarineScreen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(text = screen.title, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                MarineScreen.LIVE -> LiveWeatherScreen(viewModel)
                MarineScreen.ANALYTICS -> AnalyticsScreen(viewModel)
                MarineScreen.HISTORY -> HistoryQueryScreen(viewModel)
                MarineScreen.WIND_POWER -> WindPowerScreen(viewModel)
                MarineScreen.PATROL -> PatrolLogScreen(viewModel)
            }
        }
    }
}

// ==========================================
// SCREEN 1: Real-time Live Weather Screen (Refactored Retrofit)
// ==========================================
@Composable
fun LiveWeatherScreen(viewModel: MarineViewModel) {
    val cwaState by viewModel.cwaState.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val distKm by viewModel.userDistanceKm.collectAsState()
    val gpsStatus by viewModel.locationStatusMessage.collectAsState()
    
    val context = LocalContext.current
    var isEditingKey by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(apiKey) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Main Live State Header
            when (val state = cwaState) {
                is CwaState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("載入即時海氣象資訊中...")
                        }
                    }
                }
                is CwaState.Success -> {
                    val r = state.record
                    
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Notice if simulation is active
                        if (state.isSimulated) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Simulated", tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "已啟用氣象觀測展示模式 (點擊下方齒輪輸入中央氣象署 API 金鑰即可載入真實即時氣象)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Big Live Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "台中港觀測站",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "觀測時間：${r.time}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.fetchRealTimeData() },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Grid for 4 ocean/wind stats
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Wind Speed
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("平均風速", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text("${r.windSpeed}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(" m/s", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 4.dp))
                                        }
                                        Text("風向：${r.windDirection}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    
                                    // Wave Height
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("即時波高", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text("${r.waveHeight}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(" m", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 4.dp))
                                        }
                                        Text("波向：${r.waveDirection}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("波浪週期", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text("${r.wavePeriod}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(" s", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 2.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("觀測站座標", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                        Text("24.2889° N, 120.4797° E", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
                is CwaState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("載入失敗：${state.message}", color = MaterialTheme.colorScheme.onErrorContainer)
                            Button(onClick = { viewModel.fetchRealTimeData() }) {
                                Text("重試")
                            }
                        }
                    }
                }
            }
        }

        // Hardware GPS Position Module
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GPS 行動定位功能", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Button(
                            onClick = { viewModel.triggerGPSLocation(context) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("取得定位", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("定位狀態：$gpsStatus", fontSize = 13.sp, fontWeight = FontWeight.Medium)

                    if (distKm != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "您目前距離台中港觀測站大約：$distKm 公里",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("取得您的 GPS 定位後，系統將自動以大圓航線公式 (Haversine) 計算您與台中港觀測浮標的實際距離。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Configuration Module
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = "API key settings")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("氣象開放資料介接設定", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        IconButton(onClick = { isEditingKey = !isEditingKey }) {
                            Icon(if (isEditingKey) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Toggle")
                        }
                    }

                    if (isEditingKey) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            label = { Text("請輸入中央氣象署 CWA 授權碼") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("CWA-XXXXXXXXXXXXXXXX") }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateApiKey(keyInput)
                                isEditingKey = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("儲存金鑰並刷新即時觀測")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (apiKey.isBlank()) "目前使用：台中港即時模擬沙盒資料" else "目前已綁定氣象署 API 金鑰，正以 Retrofit 導入台中港 (O-A0019-001) 即時海象浮標資料",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: Statistical Analytics (全面整合 CSV 數據匯入)
// ==========================================
@Composable
fun AnalyticsScreen(viewModel: MarineViewModel) {

    val context = LocalContext.current

    val chartPeriod by viewModel.selectedChartPeriod.collectAsState()
    val barDatas by viewModel.averageWindSpeedBars.collectAsState()
    val allRecords by viewModel.officialObservations.collectAsState()
    val scatterMonth by viewModel.selectedScatterMonth.collectAsState()

    // 💡 這裡的數據流此時已經 100% 接上來自 CSV 檔案解算出來的即時資料
    val scatterPoints by viewModel.scatterPoints.collectAsState()
    val pR by viewModel.pearsonR.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "各月風速與波高二次迴歸及散佈分析",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("系統已成功由專案外部匯入真實海洋歷史觀測資料庫 (marine_data.csv)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        // 月份切換選擇器 (包含左右箭頭與 Dropdown 下拉功能)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (scatterMonth > 1) viewModel.setScatterMonth(scatterMonth - 1) },
                    enabled = scatterMonth > 1,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                }

                Box(modifier = Modifier.weight(1f)) {
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "選擇觀測月份：${scatterMonth} 月",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        (1..12).forEach { m ->
                            val monthDescription = when (m) {
                                10, 11, 12, 1, 2 -> "${m} 月 (冬/東北季風增能)"
                                6, 7, 8 -> "${m} 月 (夏/西南風與颱風)"
                                else -> "${m} 月 (春秋/過渡季節)"
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = monthDescription,
                                        fontWeight = if (scatterMonth == m) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (scatterMonth == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (scatterMonth == m) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (scatterMonth == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.setScatterMonth(m)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { if (scatterMonth < 12) viewModel.setScatterMonth(scatterMonth + 1) },
                    enabled = scatterMonth < 12,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                }
            }
        }

        // 散佈圖卡片與統計結果面板
        item {
            val regression = RegressionConstants.getForMonth(scatterMonth)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "【 ${scatterMonth} 月份 】散佈與迴歸分析",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "樣本數: ${scatterPoints.size} 筆", // 即時顯示 CSV 真實行數
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 💡 動態依據 scatterMonth 讀取外置圖片 scatter_1.png ~ scatter_12.png
                    val imageResourceName = "scatter_$scatterMonth"
                    val imageResId = context.resources.getIdentifier(
                        imageResourceName,
                        "drawable",
                        context.packageName
                    )

                    if (imageResId != 0) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = "${scatterMonth}月份散佈圖",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未找到資源：$imageResourceName.png", color = Color.Red)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 統計分析指標面板
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pearson 相關係數 r (實時計算)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.4f", pR),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "關連性判定：${resultTextForR(pR)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "二次迴歸方程式：",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = regression.formulaText,
                                fontSize = 13.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "設計相關指數 R = ${regression.r} (理論迴歸對照)",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // 下方的 24 小時平均風速圖 (完美保留，不受影響)
        item {
            var hourlyGroupIndex by remember(chartPeriod) { mutableStateOf(0) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "三個月份 / 半年份 24 小時平均風速圖",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "可選擇顯示三個月或六個月的平均風速趨勢圖",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setChartPeriod(3) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (chartPeriod == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (chartPeriod == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("每 3 個月 (季平均)")
                        }

                        Button(
                            onClick = { viewModel.setChartPeriod(6) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (chartPeriod == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (chartPeriod == 6) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("每 6 個月 (半年平均)")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chartPeriod == 3) {
                            val quarterNames = listOf("1~3 月", "4~6 月", "7~9 月", "10~12 月")
                            quarterNames.forEachIndexed { index, label ->
                                FilterChip(
                                    selected = hourlyGroupIndex == index,
                                    onClick = { hourlyGroupIndex = index },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        } else {
                            val halfNames = listOf("1 ~ 6 月", "7 ~ 12 月")
                            halfNames.forEachIndexed { index, label ->
                                FilterChip(
                                    selected = hourlyGroupIndex == index,
                                    onClick = { hourlyGroupIndex = index },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HourlyWindAverageLineChart(periodType = chartPeriod, groupIndex = hourlyGroupIndex)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "說明：自台中港觀測資料庫顯示，Q4 (10-12月) 及 Q1 (3-Month的前1-3月) 迎風切面平均風速最大，是離岸風電機組發電功率的黃金巔峰期，而在年中 (4-9月) 平均風速較低。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// Draw scatter plots on Canvas customized month-by-month
@Composable
fun MonthlyScatterChart(
    month: Int,
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    val regression = RegressionConstants.getForMonth(month)
    val maxWind = regression.maxX
    val maxWave = regression.maxY
    
    val dotColor = Color(0xFF1E88E5) // Clean vibrant ocean blue for scatter dots
    val lineColor = Color(0xFFE53935) // Elegant red shown in the regression curve image
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(4.dp))
            .padding(10.dp)
    ) {
        // Red regression formula on top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${month}月",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = regression.formulaText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = lineColor
                )
                Text(
                    text = "R = ${regression.r}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = lineColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis label block
            val yLabelCount = (maxWave / regression.yLabelStep).toInt()
            val yValues = List(yLabelCount + 1) { index ->
                String.format("%.1f", maxWave - index * regression.yLabelStep)
            }

            // Left padding labels column for Y-axis
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 6.dp, bottom = 12.dp), // aligned with canvas bottom ticks offset
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yValues.forEach { valStr ->
                    // Trim .0 if it's an integer for visual neatness matching Excel charts
                    val displayStr = if (valStr.endsWith(".0")) valStr.substringBefore(".0") else valStr
                    Text(
                        text = displayStr,
                        fontSize = 8.5.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Central Canvas Area
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(0.5.dp, Color.LightGray)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val chartW = size.width
                        val chartH = size.height

                        // Draw Grid lines
                        // X Grids (Vertical lines)
                        val xSteps = (maxWind / regression.xLabelStep).toInt()
                        for (i in 0..xSteps) {
                            val value = i * regression.xLabelStep
                            val x = (value / maxWind * chartW).toFloat()
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, chartH),
                                strokeWidth = 1f
                            )
                        }

                        // Y Grids (Horizontal lines)
                        val ySteps = (maxWave / regression.yLabelStep).toInt()
                        for (i in 0..ySteps) {
                            val value = i * regression.yLabelStep
                            val y = chartH - (value / maxWave * chartH).toFloat()
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(chartW, y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Regression Dashed Line (Quadratic curve)
                        val linePath = Path()
                        val divisions = 120
                        var pathStarted = false
                        for (i in 0..divisions) {
                            val w = (i.toDouble() / divisions.toDouble()) * maxWind
                            val wv = regression.a * w * w + regression.b * w + regression.c
                            
                            val x = (w / maxWind * chartW).toFloat()
                            val y = chartH - (wv / maxWave * chartH).toFloat()
                            
                            // Prevent drawing line parts that climb out of maxY bounds
                            if (y in 0f..chartH && x in 0f..chartW) {
                                if (!pathStarted) {
                                    linePath.moveTo(x, y)
                                    pathStarted = true
                                } else {
                                    linePath.lineTo(x, y)
                                }
                            }
                        }
                        
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        )

                        // Draw Blue Scatter Points
                        points.forEach { (wind, wave) ->
                            val x = (wind / maxWind * chartW).toFloat()
                            val y = chartH - (wave / maxWave * chartH).toFloat()
                            if (x in 0f..chartW && y in 0f..chartH) {
                                drawCircle(
                                    color = dotColor.copy(alpha = 0.82f),
                                    radius = 4f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // X-Axis tick label numbers
                val xLabelCount = (maxWind / regression.xLabelStep).toInt()
                val xValues = List(xLabelCount + 1) { index ->
                    String.format("%.0f", index * regression.xLabelStep)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    xValues.forEach { valStr ->
                        Text(
                            text = valStr,
                            fontSize = 8.5.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Axis Title annotations
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "波高 (m)", 
                fontSize = 8.sp, 
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "風速 (m/s)", 
                fontSize = 8.sp, 
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Draw bar charts on Canvas
@Composable
fun BarChartCanvas(items: List<MarineViewModel.AverageBar>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val barAccentColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padLeft = 40f
            val padBottom = 40f
            val padRight = 10f
            val padTop = 20f

            val chartW = size.width - padLeft - padRight
            val chartH = size.height - padBottom - padTop

            // MAX wind average limit: 16 m/s
            val maxWind = 16.0

            // Draw horizontal grids
            for (i in 0..4) {
                val value = i * 4.0
                val y = size.height - padBottom - (value / maxWind * chartH).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, y),
                    end = Offset(size.width - padRight, y),
                    strokeWidth = 1f
                )
            }

            // Draw Axes
            drawLine(
                color = axisColor,
                start = Offset(padLeft, padTop),
                end = Offset(padLeft, size.height - padBottom),
                strokeWidth = 2f
            )
            drawLine(
                color = axisColor,
                start = Offset(padLeft, size.height - padBottom),
                end = Offset(size.width - padRight, size.height - padBottom),
                strokeWidth = 2f
            )

            // Draw Bars
            if (items.isNotEmpty()) {
                val numBars = items.size
                val barGapFraction = 0.4f
                val sectionW = chartW / numBars
                val barW = sectionW * (1f - barGapFraction)
                val gapW = sectionW * barGapFraction

                items.forEachIndexed { index, bar ->
                    val x = padLeft + (index * sectionW) + (gapW / 2)
                    val barH = (bar.avgWindSpeed / maxWind * chartH).toFloat()
                    val y = size.height - padBottom - barH

                    // Colorful Gradient bar
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barAccentColor, barColor)
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH)
                    )
                }
            }
        }

        // Overlay text descriptions
        Box(modifier = Modifier.fillMaxSize()) {
            Text("風速 (m/s)", fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 40.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { bar ->
                    Text(
                        text = bar.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Values on top of bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(start = 40.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { bar ->
                    val valueText = String.format("%.1f m/s", bar.avgWindSpeed)
                    Text(
                        text = valueText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun resultTextForR(r: Double): String {
    val absR = abs(r)
    return when {
        absR >= 0.8 -> "極高度線性正相關 (r ≧ 0.8)"
        absR >= 0.6 -> "高度正相關 (0.6 ≦ r < 0.8)"
        absR >= 0.4 -> "中度正相關 (0.4 ≦ r < 0.6)"
        absR >= 0.2 -> "低度正相關 (0.2 ≦ r < 0.4)"
        else -> "無顯著關連性 (r < 0.2)"
    }
}


// ==========================================
// SCREEN 3: Historical Queries & Filter List (Room Persistence & RecyclerView)
// ==========================================
@Composable
fun HistoryQueryScreen(viewModel: MarineViewModel) {
    val records by viewModel.filteredHistoryRecords.collectAsState()
    val favorites by viewModel.favoriteRecords.collectAsState()
    
    val query by viewModel.historySearchQuery.collectAsState()
    val selMonth by viewModel.historySelectedMonth.collectAsState()
    val selYear by viewModel.historySelectedYear.collectAsState()
    val minWind by viewModel.historyMinWindSpeed.collectAsState()
    val minWave by viewModel.historyMinWaveHeight.collectAsState()

    var showFilters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.historySearchQuery.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_search_input"),
                        placeholder = { Text("搜尋觀測時間 (如 2024-10)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FilledIconButton(
                        onClick = { showFilters = !showFilters },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Advanced filter")
                    }
                }

                // Advanced filters drawer
                AnimatedVisibility(visible = showFilters) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Year Dropdown / selection
                            Column(modifier = Modifier.weight(1f)) {
                                Text("年份", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0, 2024, 2025).forEach { yr ->
                                        val label = if (yr == 0) "全部" else yr.toString()
                                        FilterChip(
                                            selected = selYear == yr,
                                            onClick = { viewModel.historySelectedYear.value = yr },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                            
                            // Month selection
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("月份類型", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0, 1, 7, 10).forEach { m ->
                                        val label = when (m) {
                                            0 -> "全部"
                                            1 -> "1月"
                                            7 -> "7月"
                                            10 -> "10月"
                                            else -> "${m}月"
                                        }
                                        FilterChip(
                                            selected = selMonth == m,
                                            onClick = { viewModel.historySelectedMonth.value = m },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Wind scale slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("最低風速篩選: ${String.format("%.1f", minWind)} m/s", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { viewModel.historyMinWindSpeed.value = 0f }) {
                                    Text("重設", fontSize = 11.sp)
                                }
                            }
                            Slider(
                                value = minWind,
                                onValueChange = { viewModel.historyMinWindSpeed.value = it },
                                valueRange = 0f..20f
                            )
                        }

                        // Wave height slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("最低波高篩選: ${String.format("%.1f", minWave)} m", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { viewModel.historyMinWaveHeight.value = 0f }) {
                                    Text("重設", fontSize = 11.sp)
                                }
                            }
                            Slider(
                                value = minWave,
                                onValueChange = { viewModel.historyMinWaveHeight.value = it },
                                valueRange = 0f..5f
                            )
                        }
                    }
                }
            }
        }

        // List Header with count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共篩選出 ${records.size} 筆歷史紀錄",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
            
            if (favorites.isNotEmpty()) {
                Text(
                    text = "${favorites.size} 筆最愛紀錄",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // The dynamic, recycled ListView (Compose LazyColumn - RecyclerView counterpart)
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = "Empty list",
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("無符合當前篩選條件之歷史紀錄", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = records,
                    key = { it.id } // Enables highly efficient RecyclerView-like animations and index recycling
                ) { item ->
                    HistoryItemCard(
                        record = item,
                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                    )
                }
            }
        }
    }
}

// Single card list widget (RecyclerView item style)
@Composable
fun HistoryItemCard(
    record: MarineRecord,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_card_${record.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isFavorite) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (record.isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.time,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Icon(
                    imageVector = if (record.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite status",
                    tint = if (record.isFavorite) Color.Red else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .clickable { onToggleFavorite() }
                        .padding(4.dp)
                        .minimumInteractiveComponentSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Wave column
                Column(modifier = Modifier.weight(1f)) {
                    Text("波高 wave", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${record.waveHeight}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(" m", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Text("波向: ${record.waveDirection} | 週期: ${record.wavePeriod}s", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Wind column
                Column(modifier = Modifier.weight(1f)) {
                    Text("風速 wind", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${record.windSpeed}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(" m/s", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Text("風向: ${record.windDirection}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


// ==========================================
// SCREEN 4: Wind Turbine Power Generation Calculator
// ==========================================
@Composable
fun WindPowerScreen(viewModel: MarineViewModel) {
    var rotorRadius by remember { mutableStateOf(80f) } // rotor radius in meters
    var airDensity by remember { mutableStateOf(1.225f) } // standard kg/m^3
    var powerCoefficient by remember { mutableStateOf(0.40f) } // typical efficiency
    var windSpeedInput by remember { mutableStateOf(11.5f) } // wind speed in m/s

    val cwaState by viewModel.cwaState.collectAsState()

    // Calculate Power: P = 0.5 * Cp * rho * A * v^3, where Area A = PI * r^2
    val area = PI * rotorRadius.toDouble().pow(2)
    val calculatedPowerWatts = 0.5 * powerCoefficient.toDouble() * airDensity.toDouble() * area * windSpeedInput.toDouble().pow(3)
    val powerKw = calculatedPowerWatts / 1000.0
    val powerMw = powerKw / 1000.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Equation / Formula Explanation header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "離岸風力發電 - 理論發電功率公式",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "P = ½ • Cₚ • ρ • A • v³",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "(其中 A = π • r²，r 代表葉片半徑；v 為切面風速)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "此公式證明：風電機組的發電瓦數 P 與葉片半徑 r 的平方、以及風速 v 的三次方呈極高的幾何正比關係。風速些微上升能帶動爆發性的供電增長！",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }
            }
        }

        // Live wind linking button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("輸入參數設定", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                Button(
                    onClick = {
                        if (cwaState is CwaState.Success) {
                            val r = (cwaState as CwaState.Success).record
                            windSpeedInput = r.windSpeed.toFloat()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Link, contentDescription = "Link", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("自動套用台中港即時風速", fontSize = 11.sp)
                }
            }
        }

        // Sliders & settings box
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Blade Radius (r)
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("葉片半徑 (r r-value)：${rotorRadius.toInt()} 公尺", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("掃掠面積：${area.toInt()} ㎡", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Slider(value = rotorRadius, onValueChange = { rotorRadius = it }, valueRange = 10f..150f)
                    }

                    // Wind Speed (v)
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("切面風速 (v)：${String.format("%.1f", windSpeedInput)} m/s", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(value = windSpeedInput, onValueChange = { windSpeedInput = it }, valueRange = 0f..25f)
                    }

                    // Power Coefficient Cp
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("風能轉化率 (Cₚ)：${String.format("%.2f", powerCoefficient)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Betz 極限: 0.59", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Slider(value = powerCoefficient, onValueChange = { powerCoefficient = it }, valueRange = 0.1f..0.59f)
                    }

                    // Air density rho
                    Column {
                        Text("空氣密度 (ρ)：${String.format("%.3f", airDensity)} kg/m³", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(value = airDensity, onValueChange = { airDensity = it }, valueRange = 1.10f..1.30f)
                    }
                }
            }
        }

        // Power Output Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("當前設定發電功率預估", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("千瓦計 kW", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                            Text(
                                text = String.format("%,.1f", powerKw),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("百萬瓦計 MW (百萬度)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                            Text(
                                text = String.format("%.3f", powerMw),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Draw dynamic power curves on Canvas
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("機組風速發電曲線與目前位置", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Draw Power curve Canvas
                    PowerCurveCanvas(
                        r = rotorRadius.toDouble(),
                        cp = powerCoefficient.toDouble(),
                        rho = airDensity.toDouble(),
                        currentV = windSpeedInput.toDouble()
                    )
                }
            }
        }
    }
}

// Canvas component to draw wind power math curve
@Composable
fun PowerCurveCanvas(
    r: Double,
    cp: Double,
    rho: Double,
    currentV: Double,
    modifier: Modifier = Modifier
) {
    val curveColor = MaterialTheme.colorScheme.primary
    val markerColor = Color.Red
    val axisColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padLeft = 40f
            val padBottom = 30f
            val padRight = 10f
            val padTop = 15f

            val chartW = size.width - padLeft - padRight
            val chartH = size.height - padBottom - padTop

            // Limit bounds on axis (X wind max 25 m/s, Y Power max mapped at 25m/s)
            val maxV = 25.0
            val area = PI * r.pow(2)
            val getPower = { v: Double -> 0.5 * cp * rho * area * v.pow(3) }
            val maxP = getPower(maxV)

            // Draw grid lines
            for (i in 1..4) {
                val gridYVal = i * (maxP / 5.0)
                val y = size.height - padBottom - (gridYVal / maxP * chartH).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, y),
                    end = Offset(size.width - padRight, y),
                    strokeWidth = 1f
                )
            }

            // Draw axis
            drawLine(
                color = axisColor,
                start = Offset(padLeft, padTop),
                end = Offset(padLeft, size.height - padBottom),
                strokeWidth = 2f
            )
            drawLine(
                color = axisColor,
                start = Offset(padLeft, size.height - padBottom),
                end = Offset(size.width - padRight, size.height - padBottom),
                strokeWidth = 2f
            )

            // Plot Power Curve line Path
            val path = Path()
            var started = false
            for (vStep in 0..50) {
                val v = vStep * (maxV / 50.0)
                val p = getPower(v)
                val x = padLeft + (v / maxV * chartW).toFloat()
                val y = size.height - padBottom - (p / maxP * chartH).toFloat()

                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 6f)
            )

            // Highlight current wind power marker dot
            val currentPower = getPower(currentV)
            val markerX = padLeft + (currentV / maxV * chartW).toFloat()
            val markerY = size.height - padBottom - (currentPower / maxP * chartH).toFloat()

            if (markerX in padLeft..(size.width - padRight) && markerY in padTop..(size.height - padBottom)) {
                // Drop lines to axes
                drawLine(
                    color = markerColor.copy(alpha = 0.5f),
                    start = Offset(markerX, markerY),
                    end = Offset(markerX, size.height - padBottom),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawLine(
                    color = markerColor.copy(alpha = 0.5f),
                    start = Offset(padLeft, markerY),
                    end = Offset(markerX, markerY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // The pulsing red coordinate circle dot
                drawCircle(
                    color = markerColor,
                    radius = 8f,
                    center = Offset(markerX, markerY)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Text("發電功率", fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp))
            Text("風速 (m/s)", fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp, end = 12.dp))
            
            Text("25", fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp, end = 14.dp))
            Text("0", fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 16.dp, start = 36.dp))
        }
    }
}


// ==========================================
// SCREEN 5: Patrol Note Logs (Local Room + Photo Camera)
// ==========================================
@Composable
fun PatrolLogScreen(viewModel: MarineViewModel) {
    val notes by viewModel.userNotes.collectAsState()
    val lat by viewModel.userLatitude.collectAsState()
    val lon by viewModel.userLongitude.collectAsState()
    val distKm by viewModel.userDistanceKm.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var noteDescription by remember { mutableStateOf("") }
    var currentPhotoPath by remember { mutableStateOf<String?>(null) }
    
    // Camera intent launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                // Save captured Bitmap into local storage cache
                val file = File(context.cacheDir, "patrol_${System.currentTimeMillis()}.jpg")
                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    currentPhotoPath = file.absolutePath
                } catch (e: Exception) {
                    currentPhotoPath = "simulated_marine_photo_placeholder"
                }
            } else {
                currentPhotoPath = "simulated_marine_photo_placeholder"
            }
        } else {
            // Pick a simulated aesthetic ocean scene if taking real camera fails
            currentPhotoPath = "simulated_marine_photo_placeholder"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module explaining camera duty
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "港內海面/風機巡邏日誌紀錄",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "此功能協助現職運維巡視人員在台中港拍照存證，自動帶入當前 GPS 定位位置並將記錄儲存於本機 Room 安全資料庫。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            // Automatically request Location update quickly before adding note
                            viewModel.triggerGPSLocation(context)
                            
                            // Reset input parameters
                            noteDescription = ""
                            currentPhotoPath = null
                            showAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Take a patrol note photo")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新增現場巡邏日誌記錄")
                    }
                }
            }
        }

        item {
            Text(
                text = "現有巡邏紀錄列表 (${notes.size} 筆)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Recycle list items
        if (notes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "目前無巡邏紀錄。點選上方按鈕照相建立第一筆吧！",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(notes, key = { it.id }) { item ->
                PatrolItemCard(
                    record = item,
                    onDelete = { viewModel.deleteUserNote(item) }
                )
            }
        }
    }

    // Capture Entry dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("撰寫現場巡察日誌") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. GPS 定位連結：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (lat != null && lon != null) {
                        Text(
                            text = "已鎖定本裝置定位:\n緯度 ${String.format("%.4f", lat)}° | 經度 ${String.format("%.4f", lon)}°\n離台中港約: $distKm km",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text("定點座標載入中，或未授權定位...", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("2. 點擊按鈕開啟相機拍照：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                cameraLauncher.launch(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Camera, contentDescription = "Camera launcher", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("啟動硬體相機", fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentPhotoPath != null) {
                                Text("照片已就緒 ✔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("未照相", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("3. 日誌描述註記：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = noteDescription,
                        onValueChange = { noteDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("請輸入目前海浪/風能機組現況...") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteDescription.isNotBlank()) {
                            viewModel.saveUserPatrolNote(noteDescription, currentPhotoPath)
                            showAddDialog = false
                        }
                    },
                    enabled = noteDescription.isNotBlank()
                ) {
                    Text("儲存紀錄")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// Single Card representing Patrol Record
@Composable
fun PatrolItemCard(
    record: MarineRecord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo decoder preview
            SafeLocalImage(
                photoPath = record.userPhotoPath ?: "simulated_marine_photo_placeholder",
                modifier = Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            )

            // Text parameters
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.time,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete patrol",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(2.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.userNoteText ?: "無文字說明",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (record.latitude != null && record.longitude != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = "Coordinates", modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("GPS: %.4f° N, %.4f° E", record.latitude, record.longitude),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// Custom local image decodes native JPeg file from device caching space
@Composable
fun SafeLocalImage(photoPath: String, modifier: Modifier = Modifier) {
    val imageBitmap = remember(photoPath) {
        try {
            if (photoPath == "simulated_marine_photo_placeholder" || photoPath.contains("simulated")) {
                null
            } else {
                val file = File(photoPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(photoPath)?.asImageBitmap()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    if (imageBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = imageBitmap,
            contentDescription = "巡視相片",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Aesthetic material fallback logo representing Offshore Wind Turbine & Waves
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.WindPower,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "運維紀錄照片",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HourlyWindAverageLineChart(
    periodType: Int,
    groupIndex: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    
    val months = when (periodType) {
        3 -> {
            when (groupIndex) {
                0 -> listOf(1, 2, 3)
                1 -> listOf(4, 5, 6)
                2 -> listOf(7, 8, 9)
                3 -> listOf(10, 11, 12)
                else -> listOf(1, 2, 3)
            }
        }
        6 -> {
            when (groupIndex) {
                0 -> listOf(1, 2, 3, 4, 5, 6)
                1 -> listOf(7, 8, 9, 10, 11, 12)
                else -> listOf(1, 2, 3, 4, 5, 6)
            }
        }
        else -> listOf(1, 2, 3)
    }

    val maxY = when {
        periodType == 3 && groupIndex == 1 -> 8.0
        periodType == 3 && groupIndex == 2 -> 8.0
        periodType == 3 && groupIndex == 0 -> 12.0
        periodType == 3 && groupIndex == 3 -> 14.0
        periodType == 6 && groupIndex == 0 -> 12.0
        else -> 14.0
    }

    val yStep = if (maxY == 8.0) 1.0 else 2.0
    val yCount = (maxY / yStep).toInt()

    val colorPalette = if (periodType == 3) {
        listOf(
            com.example.data.HourlyWindData.color3MonthNavy,
            com.example.data.HourlyWindData.color3MonthOrange,
            com.example.data.HourlyWindData.color3MonthGreen
        )
    } else {
        listOf(
            com.example.data.HourlyWindData.color6MonthBlue,
            com.example.data.HourlyWindData.color6MonthOrange,
            com.example.data.HourlyWindData.color6MonthGrey,
            com.example.data.HourlyWindData.color6MonthYellow,
            com.example.data.HourlyWindData.color6MonthLightBlue,
            com.example.data.HourlyWindData.color6MonthGreen
        )
    }

    var activeHourIndex by remember(periodType, groupIndex) { mutableStateOf<Int?>(null) }

    val contentColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(
        color = contentColor.copy(alpha = 0.7f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal
    )
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val borderLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                val chartTitleText = when (periodType) {
                    3 -> "${months.joinToString("、")}月平均風速(m/s)"
                    else -> "${months.first()}~${months.last()}月平均風速(m/s)"
                }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chartTitleText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                var chartWidthPx by remember { mutableStateOf(1f) }
                val density = LocalDensity.current
                val padLeftPx = with(density) { 32.dp.toPx() }
                val padRightPx = with(density) { 15.dp.toPx() }
                val padBottomPx = with(density) { 32.dp.toPx() }
                val padTopPx = with(density) { 15.dp.toPx() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                chartWidthPx = size.width.toFloat()
                            }
                    ) {
                        val chartW = size.width - padLeftPx - padRightPx
                        val chartH = size.height - padBottomPx - padTopPx

                        drawRect(
                            color = borderLineColor,
                            topLeft = Offset(padLeftPx, padTopPx),
                            size = Size(chartW, chartH),
                            style = Stroke(width = 1f)
                        )

                        for (i in 0..yCount) {
                            val v = i * yStep
                            val y = padTopPx + chartH - (v / maxY * chartH).toFloat()
                            
                            drawLine(
                                color = gridColor,
                                start = Offset(padLeftPx, y),
                                end = Offset(padLeftPx + chartW, y),
                                strokeWidth = 1f
                            )

                            val textLayoutResult = textMeasurer.measure(
                                text = v.toInt().toString(),
                                style = labelStyle
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    padLeftPx - textLayoutResult.size.width - 6.dp.toPx(),
                                    y - textLayoutResult.size.height / 2f
                                )
                            )
                        }

                        val stepX = chartW / 23f
                        for (h in 0..23) {
                            val x = padLeftPx + h * stepX

                            drawLine(
                                color = gridColor.copy(alpha = 0.05f),
                                start = Offset(x, padTopPx),
                                end = Offset(x, padTopPx + chartH),
                                strokeWidth = 1f
                            )

                            if (h % 2 == 0 || h == 23) {
                                val hourTextLayout = textMeasurer.measure(
                                    text = h.toString(),
                                    style = labelStyle.copy(fontSize = 8.sp)
                                )
                                drawText(
                                    textLayoutResult = hourTextLayout,
                                    topLeft = Offset(
                                        x - hourTextLayout.size.width / 2f,
                                        padTopPx + chartH + 5.dp.toPx()
                                    )
                                )
                            }
                        }

                        val axisLabelX = textMeasurer.measure(
                            text = "時",
                            style = labelStyle.copy(fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.8f))
                        )
                        drawText(
                            textLayoutResult = axisLabelX,
                            topLeft = Offset(
                                padLeftPx + chartW / 2f - axisLabelX.size.width / 2f,
                                padTopPx + chartH + 18.dp.toPx()
                            )
                        )

                        months.forEachIndexed { idx, m ->
                            val color = colorPalette[idx % colorPalette.size]
                            val points = com.example.data.HourlyWindData.getForMonth(context, m)
                            val path = Path()

                            points.forEachIndexed { h, w ->
                                val x = padLeftPx + h * stepX
                                val y = padTopPx + chartH - (w / maxY * chartH).toFloat()
                                if (h == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }

                        activeHourIndex?.let { hIndex ->
                            val activeX = padLeftPx + hIndex * stepX
                            
                            drawLine(
                                color = Color.DarkGray.copy(alpha = 0.6f),
                                start = Offset(activeX, padTopPx),
                                end = Offset(activeX, padTopPx + chartH),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            months.forEachIndexed { idx, m ->
                                val color = colorPalette[idx % colorPalette.size]
                                val points = com.example.data.HourlyWindData.getForMonth(context, m)
                                val w = points.getOrElse(hIndex) { 0.0 }
                                val activeY = padTopPx + chartH - (w / maxY * chartH).toFloat()

                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(activeX, activeY)
                                )
                                drawCircle(
                                    color = color,
                                    radius = 3.dp.toPx(),
                                    center = Offset(activeX, activeY)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(periodType, groupIndex) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull()
                                        val relativeX = change?.position?.x

                                        val chartW = chartWidthPx - padLeftPx - padRightPx

                                        if (relativeX != null && relativeX >= padLeftPx && relativeX <= padLeftPx + chartW) {
                                            activeHourIndex = ((relativeX - padLeftPx) / (chartW / 23f)).roundToInt().coerceIn(0, 23)
                                        } else {
                                            activeHourIndex = null
                                        }

                                        if (event.changes.all { c -> !c.pressed }) {
                                            activeHourIndex = null
                                        }
                                    }
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                activeHourIndex?.let { hIndex ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "【 ${hIndex} 時 】平均風速讀數：",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val chunkedMonths = months.chunked(3)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                chunkedMonths.forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        chunk.forEach { m ->
                                            val idx = months.indexOf(m)
                                            val color = colorPalette[idx % colorPalette.size]
                                            val points = com.example.data.HourlyWindData.getForMonth(context, m)
                                            val w = points.getOrElse(hIndex) { 0.0 }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(color, RoundedCornerShape(2.dp))
                                                )
                                                Text(
                                                    text = "${m}月: ${String.format("%.1f", w)} m/s",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        if (chunk.size < 3) {
                                            repeat(3 - chunk.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = if (periodType == 3) Arrangement.End else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val labelRowModifier = if (periodType == 3) {
                        Modifier.padding(end = 12.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    
                    if (periodType == 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = labelRowModifier
                        ) {
                            months.forEachIndexed { idx, m ->
                                val color = colorPalette[idx % colorPalette.size]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.width(18.dp).height(2.dp).background(color))
                                        Box(modifier = Modifier.size(5.dp).background(color, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.width(4.dp).height(2.dp).background(color))
                                    }
                                    Text(
                                        text = "${m}月",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            val chunks = months.chunked(3)
                            chunks.forEachIndexed { chunkIndex, chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    chunk.forEachIndexed { idx, m ->
                                        val actualIdx = chunkIndex * 3 + idx
                                        val color = colorPalette[actualIdx % colorPalette.size]
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.width(12.dp).height(2.dp).background(color))
                                                Box(modifier = Modifier.size(4.dp).background(color, RoundedCornerShape(0.5.dp)))
                                                Box(modifier = Modifier.width(4.dp).height(2.dp).background(color))
                                            }
                                            Text(
                                                text = "${m}月",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
