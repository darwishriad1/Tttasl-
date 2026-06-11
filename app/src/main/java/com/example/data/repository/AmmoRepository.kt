package com.example.data.repository

import com.example.data.local.AmmoDao
import com.example.data.model.AmmoTransaction
import com.example.data.model.AmmoType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AmmoRepository(private val ammoDao: AmmoDao) {

    val allTransactions: Flow<List<AmmoTransaction>> = ammoDao.getAllTransactions()

    suspend fun insert(transaction: AmmoTransaction): Long {
        return ammoDao.insertTransaction(transaction)
    }

    suspend fun delete(transaction: AmmoTransaction) {
        ammoDao.deleteTransaction(transaction)
    }

    suspend fun clearAll() {
        ammoDao.clearAll()
    }

    // Checking if empty and seeding high-fidelity, Arabic military data
    suspend fun seedInitialDataIfNeeded() {
        val currentList = allTransactions.first()
        if (currentList.isEmpty()) {
            val baseTime = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L) // 5 days ago
            val calendar = Calendar.getInstance().apply {
                timeInMillis = baseTime
            }

            // 1. Central Warehouse replenishment (Supply)
            val supplies = listOf(
                AmmoTransaction(
                    timestamp = calendar.timeInMillis,
                    type = "توريد",
                    ammoType = AmmoType.AUTOMATIC.displayName,
                    quantity = 60000,
                    officerName = "عميد ركن/ عادل الرويلي",
                    notes = "توريد دفعة مخزون دورية الربع الثاني للواء"
                ),
                AmmoTransaction(
                    timestamp = calendar.timeInMillis + 1000,
                    type = "توريد",
                    ammoType = AmmoType.PIKA.displayName,
                    quantity = 15000,
                    officerName = "عميد ركن/ عادل الرويلي",
                    notes = "تأمين ذخيرة عيار 7.62×54مم للرشاشات المتوسطة"
                ),
                AmmoTransaction(
                    timestamp = calendar.timeInMillis + 2000,
                    type = "توريد",
                    ammoType = AmmoType.DUSHKA.displayName,
                    quantity = 5000,
                    officerName = "عقيد/ فيصل العلي",
                    notes = "تأمين ذخائر الرشاشات الثقيلة 12.7مم"
                ),
                AmmoTransaction(
                    timestamp = calendar.timeInMillis + 3000,
                    type = "توريد",
                    ammoType = AmmoType.AA_23MM.displayName,
                    quantity = 2500,
                    officerName = "عقيد/ فيصل العلي",
                    notes = "إمداد ذخيرة الدفاع الجوي 23 ملم"
                ),
                AmmoTransaction(
                    timestamp = calendar.timeInMillis + 4000,
                    type = "توريد",
                    ammoType = AmmoType.MORTAR_60.displayName,
                    quantity = 120,
                    officerName = "مقدم/ سالم الحربي",
                    notes = "توريد دانات هاون عيار 60 ملم خفيف"
                ),
                AmmoTransaction(
                    timestamp = calendar.timeInMillis + 5000,
                    type = "توريد",
                    ammoType = AmmoType.MORTAR_82.displayName,
                    quantity = 80,
                    officerName = "مقدم/ سالم الحربي",
                    notes = "توريد دانات هاون عيار 82 ملم متوسط"
                ),
                AmmoTransaction(
                    timestamp = calendar.timeInMillis + 6000,
                    type = "توريد",
                    ammoType = AmmoType.RPG.displayName,
                    quantity = 150,
                    officerName = "مقدم/ سالم الحربي",
                    notes = "شحنة قذائف مضادة للدروع RPG-7"
                ),
                AmmoTransaction(
                    timestamp = calendar.timeInMillis + 7000,
                    type = "توريد",
                    ammoType = AmmoType.GRENADE.displayName,
                    quantity = 300,
                    officerName = "عقيد/ فيصل العلي",
                    notes = "صناديق قنابل يدوية طراز دفاعي صقر-1"
                )
            )

            for (s in supplies) {
                ammoDao.insertTransaction(s)
            }

            // 2. Sample disbursements to sub-units (Batallion/Company/Platoon)
            // Day -3: Disbursement to ك1 س1 ف2
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            val trx1 = AmmoTransaction(
                timestamp = calendar.timeInMillis,
                type = "صرف",
                battalion = "الكتيبة 1",
                company = "السرية 1",
                platoon = "الفصيل 2",
                ammoType = AmmoType.AUTOMATIC.displayName,
                quantity = 5000,
                officerName = "رائد/ خالد العاصمي",
                notes = "صرف لمهمة الحراسة وتأمين القطاع الشمالي"
            )
            val trx2 = AmmoTransaction(
                timestamp = calendar.timeInMillis + 5000,
                type = "صرف",
                battalion = "الكتيبة 1",
                company = "السرية 1",
                platoon = "الفصيل 2",
                ammoType = AmmoType.PIKA.displayName,
                quantity = 1200,
                officerName = "رائد/ خالد العاصمي",
                notes = "صرف لرشاشات الإسناد في القطاع"
            )
            ammoDao.insertTransaction(trx1)
            ammoDao.insertTransaction(trx2)

            // Day -2: Disbursement to ك2 س3 ف1
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val trx3 = AmmoTransaction(
                timestamp = calendar.timeInMillis,
                type = "صرف",
                battalion = "الكتيبة 2",
                company = "السرية 3",
                platoon = "الفصيل 1",
                ammoType = AmmoType.RPG.displayName,
                quantity = 20,
                officerName = "نقيب/ مساعد الشمري",
                notes = "تدريب على الرماية الميدانية"
            )
            val trx4 = AmmoTransaction(
                timestamp = calendar.timeInMillis + 10000,
                type = "صرف",
                battalion = "الكتيبة 2",
                company = "السرية 3",
                platoon = "الفصيل 1",
                ammoType = AmmoType.GRENADE.displayName,
                quantity = 40,
                officerName = "نقيب/ مساعد الشمري",
                notes = "تدريب حي في الميدان"
            )
            ammoDao.insertTransaction(trx3)
            ammoDao.insertTransaction(trx4)

            // Day -1: Return of unused automatic rounds from ك1 س1 ف2 (Return type)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val trx5 = AmmoTransaction(
                timestamp = calendar.timeInMillis,
                type = "استلام",
                battalion = "الكتيبة 1",
                company = "السرية 1",
                platoon = "الفصيل 2",
                ammoType = AmmoType.AUTOMATIC.displayName,
                quantity = 450,
                officerName = "رائد/ خالد العاصمي",
                notes = "إرجاع الذخيرة الفائضة بعد انتهاء الدورية بنجاح"
            )
            ammoDao.insertTransaction(trx5)
        }
    }
}
