package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AmmoTransaction
import com.example.data.model.AmmoType
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AmmoViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Enforce RTL (Arabic Layout) natively
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        TacticalControlCenterScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalControlCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: AmmoViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    // Fetch live states from ViewModel
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val rawTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val warehouseBalance by viewModel.warehouseBalance.collectAsStateWithLifecycle()
    val unitHoldings by viewModel.unitHoldings.collectAsStateWithLifecycle()
    val dailyReports by viewModel.dailyReports.collectAsStateWithLifecycle()
    val monthlyReports by viewModel.monthlyReports.collectAsStateWithLifecycle()

    // Alert dialog state for resetting/wiping data
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "نظام إدارة مخزون الذخيرة العسكري",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "منظومة الجرد والصرف والعهد اللحظية - مستوى اللواء",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_db_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "جرد تجريبي جديد",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            // Field operations tactical tabs layout
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("المستودع العام", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                    label = { Text("حركات الصرف/الاستلام", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_operations")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("عهد الوحدات الفرعية", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_custody")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("السجلات والتقارير", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_reports")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> WarehouseDashboardTab(
                    warehouseBalance = warehouseBalance,
                    unitHoldings = unitHoldings,
                    viewModel = viewModel,
                    onNavigateToRegister = { selectedTab = 1 }
                )
                1 -> RegisterTransactionTab(
                    viewModel = viewModel,
                    warehouseBalance = warehouseBalance
                )
                2 -> UnitCustodyTab(
                    unitHoldings = unitHoldings,
                    viewModel = viewModel
                )
                3 -> ReportsAndLogsTab(
                    transactions = transactions,
                    dailyReports = dailyReports,
                    monthlyReports = monthlyReports,
                    viewModel = viewModel
                )
            }
        }
    }

    // Database Reset/Prepopulate warning dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("إعادة ضبط ومعايرة المخزون", fontWeight = FontWeight.Bold) },
            text = { Text("سيؤدي هذا الإجراء إلى حذف كافة القيود الميدانية المسجلة، وإعادة شحن مستودعات اللواء بالإمدادات القياسية الأساسية وتجهيز البيانات الافتراضية للكتائب. هل تود الاستمرار؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDatabase()
                        showResetDialog = false
                        Toast.makeText(context, "تمت إعادة تعيين مخازن اللواء وتهيئة العينات بنجاح", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("نعم، إعادة تصفير وشحن", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء الأمر")
                }
            }
        )
    }
}

// ==========================================
// TAB 1: Warehouse Dashboard (الرئيسية - مستودع اللواء)
// ==========================================
@Composable
fun WarehouseDashboardTab(
    warehouseBalance: Map<String, Int>,
    unitHoldings: Map<String, Map<String, Int>>,
    viewModel: AmmoViewModel,
    onNavigateToRegister: () -> Unit
) {
    var showSupplyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Core Summary Military Ribbon
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ملخص جاهزية العتاد والذخائر للواء",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "حالة الاستعداد: نشط",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val totalPieces = warehouseBalance.values.sum()
                        val distinctWithIssues = AmmoType.values().count {
                            val bal = warehouseBalance.getOrDefault(it.displayName, 0)
                            bal <= it.warningThreshold
                        }
                        
                        SummaryItem(
                            label = "إجمالي الذخائر بالمخزن",
                            value = String.format("%,d", totalPieces),
                            subLabel = "تغطية ممتازة",
                            icon = Icons.Default.Done,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        SummaryItem(
                            label = "الوحدات ذات العهد النشطة",
                            value = "${unitHoldings.keys.size} وحدات",
                            subLabel = "موزعة بالميدان",
                            icon = Icons.Default.AccountBox,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        SummaryItem(
                            label = "الأصناف منخفضة المخزون",
                            value = "$distinctWithIssues أصناف",
                            subLabel = "تتطلب توريد فوري",
                            icon = Icons.Default.Warning,
                            color = if (distinctWithIssues > 0) Color(0xFFFF8C00) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Quick Command Buttons Panel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showSupplyDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("supply_dialog_open"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("توريد مركزي للمستودع", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("صرف عاجل لوحدة", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "سجل مخزن السلاح المركزي (الرصيد التلقائي الحالي)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Grid of 8 Ammunition Cards
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val ammoTypes = AmmoType.values()
                // Displaying them as elegant military cards
                ammoTypes.forEach { type ->
                    val balance = warehouseBalance.getOrDefault(type.displayName, 0)
                    AmmunitionDetailedCard(
                        type = type,
                        currentBalance = balance,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        item {
             Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Supply Replenishment Dialog
    if (showSupplyDialog) {
        SupplyCentralWarehouseDialog(
            viewModel = viewModel,
            onDismiss = { showSupplyDialog = false }
        )
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    subLabel: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun AmmunitionDetailedCard(
    type: AmmoType,
    currentBalance: Int,
    modifier: Modifier = Modifier
) {
    // Map of specific weapon visual representations
    val weaponIcon = when (type) {
        AmmoType.AUTOMATIC -> "🔫"
        AmmoType.PIKA -> "🔥"
        AmmoType.DUSHKA -> "🛡️"
        AmmoType.AA_23MM -> "🚀"
        AmmoType.MORTAR_60 -> "💥"
        AmmoType.MORTAR_82 -> "🧨"
        AmmoType.RPG -> "🎯"
        AmmoType.GRENADE -> "🟢"
    }

    val isAlert = currentBalance <= type.warningThreshold
    val isDepleted = currentBalance == 0

    val ringColor = animateColorAsState(
        targetValue = when {
            isDepleted -> Color(0xFFDC3545)
            isAlert -> Color(0xFFFF8C00)
            else -> Color(0xFF28A745)
        }
    )

    val statusText = when {
        isDepleted -> "منفذ!"
        isAlert -> "تنبيه: مخزون حرج"
        else -> "رصيد كافٍ"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (isAlert) ringColor.value.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weapon Visual Category Bubble
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ringColor.value.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = weaponIcon,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Alert metrics display
                Text(
                    text = "حد الأمان للتخزين: ${type.warningThreshold} ${type.unitName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
            }

            // Quantities
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%,d", currentBalance)} ${type.unitName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = ringColor.value
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ringColor.value.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ringColor.value
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// CENTRAL REPLENISHMENT DIALOG
// ==========================================
@Composable
fun SupplyCentralWarehouseDialog(
    viewModel: AmmoViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedAmmo by remember { mutableStateOf(AmmoType.AUTOMATIC) }
    var quantityText by remember { mutableStateOf(1000) }
    var replenisherName by remember { mutableStateOf("عميد ركن/ عادل الرويلي") }
    var supplyNotes by remember { mutableStateOf("تعويض المخزون وتوريد مركزي") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "أمر توريد مركزي طارئ لمستودع اللواء",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "تقوم هذه العملية بزيادة الرصيد المباشر للمستودع العام للذخيرة دون الارتباط بكتيبة معينة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Divider()

                // Pick Ammo
                Text("اختر نوع العتاد المورد:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                var expandedAmmoChoice by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedAmmoChoice = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedAmmo.displayName, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = expandedAmmoChoice,
                        onDismissRequest = { expandedAmmoChoice = false }
                    ) {
                        AmmoType.values().forEach {
                            DropdownMenuItem(
                                text = { Text(it.displayName) },
                                onClick = {
                                    selectedAmmo = it
                                    expandedAmmoChoice = false
                                }
                            )
                        }
                    }
                }

                // Pick Qty with steps
                Text("إجمالي الكمية الموردة (${selectedAmmo.unitName}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { quantityText = (quantityText - 100).coerceAtLeast(1) }) {
                        Text("-100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "$quantityText",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { quantityText += 100 }) {
                        Text("+100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickAmts = listOf(500, 1000, 5000)
                    quickAmts.forEach { value ->
                        OutlinedButton(
                            onClick = { quantityText = value },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+$value", fontSize = 10.sp)
                        }
                    }
                }

                // Officer
                OutlinedTextField(
                    value = replenisherName,
                    onValueChange = { replenisherName = it },
                    label = { Text("المسؤول المورد (الرتبة والاسم الكامل)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = supplyNotes,
                    onValueChange = { supplyNotes = it },
                    label = { Text("ملاحظات أمر التوريد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addTransaction(
                                type = "توريد",
                                battalion = "",
                                company = "",
                                platoon = "",
                                ammoType = selectedAmmo.displayName,
                                quantity = quantityText,
                                officerName = replenisherName,
                                notes = supplyNotes
                            )
                            Toast.makeText(context, "تم قيد أمر التوريد وزيادة رصيد ${selectedAmmo.displayName} بمقدار $quantityText", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_supply_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("اعتماد التوريد والمخرن", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء الأمر")
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: Register Transaction (تسجيل حركة صرف أو استلام)
// ==========================================
@Composable
fun RegisterTransactionTab(
    viewModel: AmmoViewModel,
    warehouseBalance: Map<String, Int>
) {
    val context = LocalContext.current

    // Operation Inputs States
    var selectedType by remember { mutableStateOf("صرف") } // "صرف" or "استلام"
    var enteredBattalion by remember { mutableStateOf("الكتيبة 1") }
    var enteredCompany by remember { mutableStateOf("السرية 1") }
    var enteredPlatoon by remember { mutableStateOf("الفصيل 1") }
    var selectedAmmo by remember { mutableStateOf(AmmoType.AUTOMATIC) }
    var enteredQuantity by remember { mutableStateOf("100") }
    var enteredOfficerName by remember { mutableStateOf("رائد/ خالد العاصمي") }
    var enteredNotes by remember { mutableStateOf("صرف روتيني لتأمين القطاع") }

    val currentWarehouseAmmoBal = warehouseBalance.getOrDefault(selectedAmmo.displayName, 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "تسجيل حركة ميدانية جديدة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "قم بملء حقول القيادة لتنصيب عمليات صرف العتاد إلى الكتائب أو استلام المرتجعات",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        // Operation selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isDisbursement = selectedType == "صرف"
                    Button(
                        onClick = {
                            selectedType = "صرف"
                            enteredNotes = "صرف عتاد لتأمين النطاق"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("type_disburse"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDisbursement) MaterialTheme.colorScheme.error else Color.Transparent
                        ),
                        elevation = null
                    ) {
                        Text(
                            "صرف للوحدات (خارج 📤)",
                            fontWeight = FontWeight.Bold,
                            color = if (isDisbursement) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = {
                            selectedType = "استلام"
                            enteredNotes = "مرتجع الذخيرة الفائضة"
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("type_receive"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDisbursement) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        elevation = null
                    ) {
                        Text(
                            "استلام مرتجع (داخل 📥)",
                            fontWeight = FontWeight.Bold,
                            color = if (!isDisbursement) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Sub unit picker (Battalion, Company, Platoon)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "الهيكل التنظيمي للوحدة الفرعية المستهدفة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Battalion Input & Suggestions
                    OutlinedTextField(
                        value = enteredBattalion,
                        onValueChange = { enteredBattalion = it },
                        label = { Text("الكتيبة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_battalion"),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.battalionList) { bat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { enteredBattalion = bat }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    bat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Company Input & Suggestions
                    OutlinedTextField(
                        value = enteredCompany,
                        onValueChange = { enteredCompany = it },
                        label = { Text("السرية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_company"),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.companyList) { comp ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { enteredCompany = comp }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    comp,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Platoon Input & Suggestions
                    OutlinedTextField(
                        value = enteredPlatoon,
                        onValueChange = { enteredPlatoon = it },
                        label = { Text("الفصيل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_platoon"),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.platoonList) { plat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { enteredPlatoon = plat }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    plat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ammunition choice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "اختر نوع الذخيرة المطلوبة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(AmmoType.values()) { at ->
                            val isChosen = selectedAmmo == at
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clickable { selectedAmmo = at },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isChosen) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isChosen) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (isChosen) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        at.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    
                    // Available Balance alert
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("المتوفر بمخزن الصكوك حالياً:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${String.format("%,d", currentWarehouseAmmoBal)} ${selectedAmmo.unitName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Quantity inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "الكمية المراد تعيينها:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = enteredQuantity,
                        onValueChange = { enteredQuantity = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_qty"),
                        suffix = { Text(selectedAmmo.unitName) }
                    )

                    // Quick buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val offsets = listOf(10, 50, 200, 1000)
                        offsets.forEach { offset ->
                            OutlinedButton(
                                onClick = {
                                    val current = enteredQuantity.toIntOrNull() ?: 0
                                    enteredQuantity = "${current + offset}"
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("+$offset", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Responsible individuals & remarks
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = enteredOfficerName,
                        onValueChange = { enteredOfficerName = it },
                        label = { Text("الضابط الآمر / الجرد العسكري المسؤول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_officer")
                    )

                    OutlinedTextField(
                        value = enteredNotes,
                        onValueChange = { enteredNotes = it },
                        label = { Text("بيان وملاحظات مستند الصرف/الاستلام") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Record Button
        item {
            val isDisbursal = selectedType == "صرف"
            val buttonColor = if (isDisbursal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            
            Button(
                onClick = {
                    val qty = enteredQuantity.toIntOrNull() ?: 0
                    if (qty <= 0) {
                        Toast.makeText(context, "خطأ: الرجاء إدخال كمية صحيحة أكبر من صفر", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (enteredBattalion.isEmpty() || enteredCompany.isEmpty() || enteredPlatoon.isEmpty()) {
                        Toast.makeText(context, "خطأ: يجب تحديد الكتيبة والسرية والفصيل بدقة", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (enteredOfficerName.isEmpty()) {
                        Toast.makeText(context, "خطأ: يجب تسجيل اسم المسؤول ضابط السلاح", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    
                    // Specific safety check on Disbursement: cannot disburse what we do not have!
                    if (isDisbursal && qty > currentWarehouseAmmoBal) {
                        Toast.makeText(context, "تنبيه مانع: الكمية المطلوبة ($qty) تفوق المتوفر بالمستودع ($currentWarehouseAmmoBal)", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    viewModel.addTransaction(
                        type = selectedType,
                        battalion = enteredBattalion,
                        company = enteredCompany,
                        platoon = enteredPlatoon,
                        ammoType = selectedAmmo.displayName,
                        quantity = qty,
                        officerName = enteredOfficerName,
                        notes = enteredNotes,
                        onSuccess = {
                            Toast.makeText(context, "تم حفظ المعاملة بنجاح وتحديث السجلات العسكرية التلقائية", Toast.LENGTH_SHORT).show()
                            // Clear quantity for next record
                            enteredQuantity = "0"
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_transaction_button"),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Icon(
                    imageVector = if (isDisbursal) Icons.Default.Send else Icons.Default.AddCircle,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDisbursal) "إصدار وتأمين عهدة الذخيرة (صرف 📤)" else "قيد المرتجع العسكري (استلام 📥)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ==========================================
// TAB 3: Unit Custody - جرد عهد الوحدات الميدانية
// ==========================================
@Composable
fun UnitCustodyTab(
    unitHoldings: Map<String, Map<String, Int>>,
    viewModel: AmmoViewModel
) {
    var searchQueryUnit by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "الجرد المفصل لعهدة العتاد والذخائر للوحدات الميدانية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Search Bar
        OutlinedTextField(
            value = searchQueryUnit,
            onValueChange = { searchQueryUnit = it },
            label = { Text("بحث باسم الكتيبة، السرية، أو الفصيل الفرعي...") },
            modifier = Modifier.fillMaxWidth().testTag("search_unit_bar"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQueryUnit.isNotEmpty()) {
                    IconButton(onClick = { searchQueryUnit = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true
        )

        val filteredHoldings = unitHoldings.filterKeys { key ->
            searchQueryUnit.isEmpty() || key.lowercase(Locale.ROOT).contains(searchQueryUnit.lowercase(Locale.ROOT))
        }

        if (filteredHoldings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "لا توجد ذخير عهد مسجلة أو جاري تداولها للوحدات المطابقة للبحث حاليًا",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHoldings.keys.toList()) { unitCode ->
                    val holdingsMap = filteredHoldings[unitCode] ?: emptyMap()
                    UnitCustodyCard(
                        unitCode = unitCode,
                        holdings = holdingsMap
                    )
                }
            }
        }
    }
}

@Composable
fun UnitCustodyCard(
    unitCode: String,
    holdings: Map<String, Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Unit identification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جرد عهدة الوحدة: $unitCode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                val totalItemsCount = holdings.values.sum()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "المجموع: ${String.format("%,d", totalItemsCount)} قطعة",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Divider()

            // List of items currently in possession of this unit
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val nonZeroHoldings = holdings.filterValues { it > 0 }
                if (nonZeroHoldings.isEmpty()) {
                    Text(
                        "الوحدة خالية تمامًا من العهد العسكرية حاليًا (تم تصفير الرصيد)",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    nonZeroHoldings.forEach { (ammoName, qty) ->
                        val ammoType = AmmoType.values().find { it.displayName == ammoName }
                        val unitLabel = ammoType?.unitName ?: "قطعة"
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ammoName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%,d", qty)} $unitLabel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: Reports And History Logs Tab
// ==========================================
@Composable
fun ReportsAndLogsTab(
    transactions: List<AmmoTransaction>,
    dailyReports: List<AmmoViewModel.ReportSummary>,
    monthlyReports: List<AmmoViewModel.ReportSummary>,
    viewModel: AmmoViewModel
) {
    var reportsSubTab by remember { mutableStateOf(0) } // 0 = logs, 1 = reports

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Switch sub tab
        TabRow(selectedTabIndex = reportsSubTab) {
            Tab(
                selected = reportsSubTab == 0,
                onClick = { reportsSubTab = 0 },
                text = { Text("سجل حركة المخزن المباشر", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = reportsSubTab == 1,
                onClick = { reportsSubTab = 1 },
                text = { Text("تقارير الاستيراد والتصدير", fontWeight = FontWeight.Bold) }
            )
        }

        when (reportsSubTab) {
            0 -> LiveTransactionsLogsSection(transactions = transactions, viewModel = viewModel)
            1 -> SystemReportsSection(dailyReports = dailyReports, monthlyReports = monthlyReports)
        }
    }
}

@Composable
fun LiveTransactionsLogsSection(
    transactions: List<AmmoTransaction>,
    viewModel: AmmoViewModel
) {
    // Collect search flows
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterTypeVal by viewModel.filterType.collectAsStateWithLifecycle()
    val filterAmmoNameVal by viewModel.filterAmmoName.collectAsStateWithLifecycle()
    
    var showExtendedFilters by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Search Text
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            label = { Text("ابحث باسم الضابط المسؤول، البيان أو الوحدة...") },
            modifier = Modifier.fillMaxWidth().testTag("search_history_bar"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    IconButton(onClick = { showExtendedFilters = !showExtendedFilters }) {
                        Icon(Icons.Default.Edit, contentDescription = "تصفية متقدمة")
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                }
            },
            singleLine = true
        )

        // Collapsible Filters Panel
        AnimatedVisibility(visible = showExtendedFilters) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("أدوات التصفية المتقدمة لغرفة المراقبة:", fontWeight = FontWeight.Bold, fontSize = 11.sp)

                    // Type Filter Outlines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val types = listOf(Pair("الكل", null), Pair("صرف للوحدات", "صرف"), Pair("استرداد مرتجع", "استلام"), Pair("توريد ومخزون لواء", "توريد"))
                        types.forEach { (label, value) ->
                            val selected = filterTypeVal == value
                            OutlinedButton(
                                onClick = { viewModel.filterType.value = value },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Ammo Filter Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صنف الذخيرة:", fontSize = 11.sp, modifier = Modifier.width(80.dp))
                        var expandedAmmo by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedAmmo = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(filterAmmoNameVal ?: "جميع الذخائر العبثية", fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedAmmo,
                                onDismissRequest = { expandedAmmo = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("جميع التجهيزات والذخائر") },
                                    onClick = {
                                        viewModel.filterAmmoName.value = null
                                        expandedAmmo = false
                                    }
                                )
                                AmmoType.values().forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.displayName) },
                                        onClick = {
                                            viewModel.filterAmmoName.value = it.displayName
                                            expandedAmmo = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Raw clearing of advanced tools
                    OutlinedButton(
                        onClick = {
                            viewModel.filterType.value = null
                            viewModel.filterAmmoName.value = null
                            viewModel.searchQuery.value = ""
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("إعادة تصفير الإعدادات المتقدمة", fontSize = 10.sp)
                    }
                }
            }
        }

        // Live list
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد تحركات مسجلة تطابق مدخلات البحث الحالية", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { trx ->
                    TransactionHistoryRow(transaction = trx, onDelete = {
                        viewModel.deleteTransaction(trx)
                    })
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryRow(
    transaction: AmmoTransaction,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showConfirmDelete by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
    val timestampFormatted = sdf.format(Date(transaction.timestamp))

    val statusDetails = when (transaction.type) {
        "صرف" -> Triple("صرف 📤", Color(0xFFDC3545), "معاملة صرف للوحدة")
        "استلام" -> Triple("استلام 📥", Color(0xFF28A745), "معاملة توريد مرتجع")
        else -> Triple("توريد 🏷️", Color(0xFF17A2B8), "توريد مركزي للواء")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusDetails.second.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusDetails.first,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusDetails.second,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }

                // Date Time Text
                Text(
                    text = timestampFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                
                // Fast Delete Indicator
                IconButton(
                    onClick = { showConfirmDelete = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الحركة",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body info
            Text(
                text = "${transaction.ammoType} بمقدار ${String.format("%,d", transaction.quantity)} حبة",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Target Unit
                Text(
                    text = "المستفيد: " + if (transaction.type == "توريد") "المستودع الرئيسي" else transaction.formatUnitCode(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                // Authorizing Officer
                Text(
                    text = "بإشراف: ${transaction.officerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (transaction.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "بيان: ${transaction.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("هل تود التراجع عن قيد هذه الحركة؟") },
            text = { Text("سيؤدي هذا إلى شطب هذا السجل اللحظي نهائيًا وإعادة المعايرة التلقائية للرصيد والعهدة للوحدات الميدانية.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                        Toast.makeText(context, "تم إلغاء القيد وشطب المعاملة عسكرياً", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("نعم، تراجع واحذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("إبقاء الحركة")
                }
            }
        )
    }
}

// ==========================================
// REPORTS SUBSECTION (التقارير اليومية والشهرية)
// ==========================================
@Composable
fun SystemReportsSection(
    dailyReports: List<AmmoViewModel.ReportSummary>,
    monthlyReports: List<AmmoViewModel.ReportSummary>
) {
    var reportsSubSelector by remember { mutableStateOf(0) } // 0 = daily, 1 = monthly

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { reportsSubSelector = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportsSubSelector == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    "تقارير يومية لحركة اللواء",
                    color = if (reportsSubSelector == 0) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = { reportsSubSelector = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportsSubSelector == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    "تقارير شهرية تجميعية للمستودع",
                    color = if (reportsSubSelector == 1) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val displayedReports = if (reportsSubSelector == 0) dailyReports else monthlyReports

        if (displayedReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد تقارير إحصائية متاحة لنطاق الحركات الحالي", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedReports) { report ->
                    ReportSummaryCard(report = report)
                }
            }
        }
    }
}

@Composable
fun ReportSummaryCard(report: AmmoViewModel.ReportSummary) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الدورة الإحصائية للبند: ${report.periodLabel}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Summary metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("إجمالي التوريد والاستلام 📥", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${String.format("%,d", report.totalImports)} قطعة",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF28A745)
                    )
                }
                Column {
                    Text("إجمالي الصرف للوحدات 📤", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${String.format("%,d", report.totalDisbursed)} قطعة",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC3545)
                    )
                }
                Column {
                    Text("عدد التحركات", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${report.transactionCount} حركات مسجلة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Details section if clicked (Expanded)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Divider()
                    Text(
                        "التوزيع الإحصائي التفصيلي لكل صنف:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    report.details.forEach { (ammoName, pair) ->
                        val (imp, exp) = pair
                        if (imp > 0 || exp > 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp)
                            ) {
                                Text(ammoName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "مستورات ومردودات: $imp حبة",
                                        fontSize = 11.sp,
                                        color = if (imp > 0) Color(0xFF28A745) else Color.Gray
                                    )
                                    Text(
                                        "صرف خارجي: $exp حبة",
                                        fontSize = 11.sp,
                                        color = if (exp > 0) Color(0xFFDC3545) else Color.Gray
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
