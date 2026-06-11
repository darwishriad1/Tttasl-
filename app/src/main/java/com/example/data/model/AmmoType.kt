package com.example.data.model

enum class AmmoType(
    val displayName: String,
    val unitName: String,
    val warningThreshold: Int,
    val description: String
) {
    AUTOMATIC(
        displayName = "الذخيرة الآلية",
        unitName = "طلقة",
        warningThreshold = 5000,
        description = "ذخيرة البندقية الآلية عيار 7.62×39 ملم"
    ),
    PIKA(
        displayName = "بيكا",
        unitName = "طلقة",
        warningThreshold = 2000,
        description = "ذخيرة الرشاش المتوسط بيكا عيار 7.62×54 ملم"
    ),
    DUSHKA(
        displayName = "دشكا",
        unitName = "طلقة",
        warningThreshold = 1000,
        description = "ذخيرة الرشاش الثقيل دشكا عيار 12.7 ملم"
    ),
    AA_23MM(
        displayName = "23 ملم",
        unitName = "طلقة",
        warningThreshold = 500,
        description = "ذخيرة المضاد الأرضي 23 ملم الثنائية"
    ),
    MORTAR_60(
        displayName = "هاون 60",
        unitName = "دانة",
        warningThreshold = 50,
        description = "قذائف مصفحة عيار 60 ملم للميدان"
    ),
    MORTAR_82(
        displayName = "هاون 82",
        unitName = "دانة",
        warningThreshold = 40,
        description = "قذائف هاون عيار 82 ملم للمساندة"
    ),
    RPG(
        displayName = "RPG",
        unitName = "قذيفة",
        warningThreshold = 30,
        description = "قذائف قاذف الصواريخ المحمول RPG-7"
    ),
    GRENADE(
        displayName = "قنابل يدوية",
        unitName = "قنبلة",
        warningThreshold = 50,
        description = "قنابل يدوية هجومية ودفاعية"
    );

    companion object {
        fun fromDisplayName(name: String): AmmoType? {
            return values().find { it.displayName == name }
        }
    }
}
