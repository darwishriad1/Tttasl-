package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ammo_transactions")
data class AmmoTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val type: String, // "صرف" (Disbursement), "استلام" (Reception/Return), "توريد" (Supply/Replenishment)
    val battalion: String = "", // E.g., "الكتيبة 1"
    val company: String = "",   // E.g., "السرية 2"
    val platoon: String = "",   // E.g., "الفصيل 3"
    val ammoType: String,       // E.g., "الذخيرة الآلية"
    val quantity: Int,          // Always positive number
    val officerName: String,    // Person in charge
    val notes: String = ""      // Additional comments
) {
    // Helper to format the organization description quickly, e.g., "ك1 س2 ف3"
    fun formatUnitCode(): String {
        if (type == "توريد") return "القيادة العامة"
        
        val b = battalion.trim().replace("الكتيبة", "ك").replace(" ", "")
        val c = company.trim().replace("السرية", "س").replace(" ", "")
        val p = platoon.trim().replace("الفصيل", "ف").replace(" ", "")
        
        return listOf(b, c, p).filter { it.isNotEmpty() }.joinToString(" ")
    }
}
