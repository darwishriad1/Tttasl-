package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AmmoTransaction
import com.example.data.model.AmmoType
import com.example.data.repository.AmmoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AmmoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AmmoRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AmmoRepository(database.ammoDao())
        
        // Seed initial supplies and transactions on a background thread
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    // Raw stream of transactions
    val allTransactions: StateFlow<List<AmmoTransaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Live computed warehouse balance for the 8 ammunition types
    val warehouseBalance: StateFlow<Map<String, Int>> = allTransactions
        .combine(MutableStateFlow(Unit)) { transactions, _ ->
            val balance = AmmoType.values().associate { it.displayName to 0 }.toMutableMap()
            
            // Go through transactions in chronological order (earliest to latest) to calculate
            for (t in transactions.reversed()) {
                val current = balance.getOrDefault(t.ammoType, 0)
                when (t.type) {
                    "توريد" -> balance[t.ammoType] = current + t.quantity
                    "استلام" -> balance[t.ammoType] = current + t.quantity // Return to warehouse
                    "صرف" -> balance[t.ammoType] = (current - t.quantity).coerceAtLeast(0) // Disbursed
                }
            }
            balance
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Live computed sub-unit holdings (custody of Battalion, Company, Platoon)
    // Map of "ك1 س2 ف3" -> Map of AmmoType -> Quantity
    val unitHoldings: StateFlow<Map<String, Map<String, Int>>> = allTransactions
        .combine(MutableStateFlow(Unit)) { transactions, _ ->
            val holdings = mutableMapOf<String, MutableMap<String, Int>>()
            
            // Process chronologically
            for (t in transactions.reversed()) {
                if (t.type == "توريد") continue
                val unitKey = t.formatUnitCode()
                if (unitKey.isEmpty()) continue

                val ammoMap = holdings.getOrPut(unitKey) { mutableMapOf() }
                val currentHold = ammoMap.getOrDefault(t.ammoType, 0)

                if (t.type == "صرف") {
                    ammoMap[t.ammoType] = currentHold + t.quantity
                } else if (t.type == "استلام") {
                    ammoMap[t.ammoType] = (currentHold - t.quantity).coerceAtLeast(0)
                }
            }
            holdings
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Search & Filter States ---
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<String?>(null) // "صرف", "استلام", "توريد", null for all
    val filterAmmoName = MutableStateFlow<String?>(null) // Mapped ammo, null for all
    val filterBattalion = MutableStateFlow("") // Filter by Battalion input (e.g. "ك1" or "الكتيبة 1")
    val filterCompany = MutableStateFlow("")
    val filterPlatoon = MutableStateFlow("")
    val filterDateString = MutableStateFlow("") // Filter by Date e.g., "YYYY-MM-DD"

    // Real-time filtered transactions
    val filteredTransactions: StateFlow<List<AmmoTransaction>> = combine(
        allTransactions,
        searchQuery,
        filterType,
        filterAmmoName,
        filterBattalion,
        filterCompany,
        filterPlatoon,
        filterDateString
    ) { params ->
        val transactions = params[0] as List<AmmoTransaction>
        val query = (params[1] as String).trim().lowercase(Locale.ROOT)
        val type = params[2] as String?
        val ammoName = params[3] as String?
        val batt = (params[4] as String).trim()
        val comp = (params[5] as String).trim()
        val plat = (params[6] as String).trim()
        val dateStr = params[7] as String

        transactions.filter { t ->
            // 1. Search Query (Officer name, notes, ammo type, unit)
            val matchQuery = query.isEmpty() ||
                    t.officerName.lowercase(Locale.ROOT).contains(query) ||
                    t.notes.lowercase(Locale.ROOT).contains(query) ||
                    t.ammoType.lowercase(Locale.ROOT).contains(query) ||
                    t.battalion.lowercase(Locale.ROOT).contains(query) ||
                    t.company.lowercase(Locale.ROOT).contains(query) ||
                    t.platoon.lowercase(Locale.ROOT).contains(query) ||
                    t.formatUnitCode().lowercase(Locale.ROOT).contains(query)

            // 2. Transaction Type
            val matchType = type == null || t.type == type

            // 3. Ammunition Type
            val matchAmmo = ammoName == null || t.ammoType == ammoName

            // 4. Battalion
            val matchBatt = batt.isEmpty() || t.battalion.contains(batt) || t.formatUnitCode().contains(batt)

            // 5. Company
            val matchComp = comp.isEmpty() || t.company.contains(comp) || t.formatUnitCode().contains(comp)

            // 6. Platoon
            val matchPlat = plat.isEmpty() || t.platoon.contains(plat) || t.formatUnitCode().contains(plat)

            // 7. Date Match (compares formatted string "YYYY-MM-DD")
            val matchDate = dateStr.isEmpty() || getFormattedDate(t.timestamp) == dateStr

            matchQuery && matchType && matchAmmo && matchBatt && matchComp && matchPlat && matchDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper: Formats timestamp to YYYY-MM-DD
    fun getFormattedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timestamp))
    }

    // --- Daily/Monthly Reports Calculations ---
    // Structure holding statistical data for a specific date/month range
    data class ReportSummary(
        val periodLabel: String, // E.g., "2026-06-11" or "يونيو 2026"
        val totalImports: Int,  // Sum of quantities for (توريد + استلام)
        val totalDisbursed: Int, // Sum of quantities for (صرف)
        val transactionCount: Int,
        val details: Map<String, Pair<Int, Int>> // Map of AmmoName -> Pair(ImportedQty, DisbursedQty)
    )

    val dailyReports: StateFlow<List<ReportSummary>> = allTransactions
        .combine(MutableStateFlow(Unit)) { transactions, _ ->
            val rdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val groups = transactions.groupBy { rdf.format(Date(it.timestamp)) }

            groups.entries.map { (dateStr, trs) ->
                var imports = 0
                var exports = 0
                val details = mutableMapOf<String, Pair<Int, Int>>()

                for (t in trs) {
                    val currentDetails = details.getOrDefault(t.ammoType, Pair(0, 0))
                    if (t.type == "توريد" || t.type == "استلام") {
                        imports += t.quantity
                        details[t.ammoType] = Pair(currentDetails.first + t.quantity, currentDetails.second)
                    } else if (t.type == "صرف") {
                        exports += t.quantity
                        details[t.ammoType] = Pair(currentDetails.first, currentDetails.second + t.quantity)
                    }
                }

                ReportSummary(
                    periodLabel = dateStr,
                    totalImports = imports,
                    totalDisbursed = exports,
                    transactionCount = trs.size,
                    details = details
                )
            }.sortedByDescending { it.periodLabel }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyReports: StateFlow<List<ReportSummary>> = allTransactions
        .combine(MutableStateFlow(Unit)) { transactions, _ ->
            // Arabic representation for months
            val rmf = SimpleDateFormat("yyyy-MM", Locale.US)
            val monthArabicNames = mapOf(
                "01" to "يناير", "02" to "فبراير", "03" to "مارس", "04" to "أبريل",
                "05" to "مايو", "06" to "يونيو", "07" to "يوليو", "08" to "أغسطس",
                "09" to "سبتمبر", "10" to "أكتوبر", "11" to "نوفمبر", "12" to "ديسمبر"
            )

            val groups = transactions.groupBy { rmf.format(Date(it.timestamp)) }

            groups.entries.map { (yearMonth, trs) ->
                var imports = 0
                var exports = 0
                val details = mutableMapOf<String, Pair<Int, Int>>()

                for (t in trs) {
                    val currentDetails = details.getOrDefault(t.ammoType, Pair(0, 0))
                    if (t.type == "توريد" || t.type == "استلام") {
                        imports += t.quantity
                        details[t.ammoType] = Pair(currentDetails.first + t.quantity, currentDetails.second)
                    } else if (t.type == "صرف") {
                        exports += t.quantity
                        details[t.ammoType] = Pair(currentDetails.first, currentDetails.second + t.quantity)
                    }
                }

                val parts = yearMonth.split("-")
                val year = parts.getOrNull(0) ?: ""
                val monthNum = parts.getOrNull(1) ?: ""
                val monthName = monthArabicNames[monthNum] ?: monthNum
                val periodLabel = "$monthName $year"

                ReportSummary(
                    periodLabel = periodLabel,
                    totalImports = imports,
                    totalDisbursed = exports,
                    transactionCount = trs.size,
                    details = details
                )
            }.sortedWith { a, b -> b.periodLabel.compareTo(a.periodLabel) } // Approximate sorting
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Actions ---
    fun addTransaction(
        type: String,
        battalion: String,
        company: String,
        platoon: String,
        ammoType: String,
        quantity: Int,
        officerName: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val t = AmmoTransaction(
                timestamp = System.currentTimeMillis(),
                type = type,
                battalion = if (type == "توريد") "" else battalion,
                company = if (type == "توريد") "" else company,
                platoon = if (type == "توريد") "" else platoon,
                ammoType = ammoType,
                quantity = quantity,
                officerName = officerName,
                notes = notes
            )
            repository.insert(t)
            onSuccess()
        }
    }

    fun deleteTransaction(transaction: AmmoTransaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAll()
            // Reset seed automatically for clean experience
            repository.seedInitialDataIfNeeded()
        }
    }

    // Helper list to offer suggestions or options for the structures
    val battalionList = listOf("الكتيبة 1", "الكتيبة 2", "الكتيبة 3", "الكتيبة 4")
    val companyList = listOf("السرية 1", "السرية 2", "السرية 3", "السرية 4", "السرية م")
    val platoonList = listOf("الفصيل 1", "الفصيل 2", "الفصيل 3", "الفصيل 4")
}
