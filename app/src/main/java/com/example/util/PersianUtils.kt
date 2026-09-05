package com.example.util

import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

data class ColorOption(
    val id: String,
    val name: String,
    val hex: String,
    val color: Color
)

val COLOR_PALETTE = listOf(
    // Blues & Navies
    ColorOption("navy", "سرمه‌ای کلاسیک", "#0F172A", Color(0xFF0F172A)),
    ColorOption("royal_blue", "آبی کاربنی", "#1D4ED8", Color(0xFF1D4ED8)),
    ColorOption("blue", "آبی نیلی", "#2563EB", Color(0xFF2563EB)),
    ColorOption("sky", "آبی درباری روشن", "#0284C7", Color(0xFF0284C7)),
    ColorOption("cyan", "فیروزه‌ای", "#0891B2", Color(0xFF0891B2)),
    ColorOption("petrol", "آبی نفتی", "#164E63", Color(0xFF164E63)),
    
    // Greens & Olives
    ColorOption("teal", "یشمی تیره", "#115E59", Color(0xFF115E59)),
    ColorOption("emerald", "سبز زمردی", "#059669", Color(0xFF059669)),
    ColorOption("forest", "سبز جنگلی", "#15803D", Color(0xFF15803D)),
    ColorOption("olive", "زیتونی سلطنتی", "#4D7C0F", Color(0xFF4D7C0F)),
    ColorOption("sage", "سبز سدری", "#065F46", Color(0xFF065F46)),
    ColorOption("lime", "سبز پسته‌ای", "#65A30D", Color(0xFF65A30D)),
    
    // Golds, Ambers & Browns
    ColorOption("gold", "طلایی شاهانه", "#D97706", Color(0xFFD97706)),
    ColorOption("amber", "کهربایی چرم", "#B45309", Color(0xFFB45309)),
    ColorOption("mustard", "خردلی مخمل", "#CA8A04", Color(0xFFCA8A04)),
    ColorOption("caramel", "کاراملی", "#A16207", Color(0xFFA16207)),
    ColorOption("warm_brown", "قهوه‌ای گردویی", "#78350F", Color(0xFF78350F)),
    ColorOption("chestnut", "فندقی تیره", "#451A03", Color(0xFF451A03)),
    ColorOption("chocolate", "شکلاتی", "#5C2C16", Color(0xFF5C2C16)),
    ColorOption("copper", "مسی برنزی", "#9A3412", Color(0xFF9A3412)),
    
    // Reds, Burgundy & Pinks
    ColorOption("burgundy", "زرشکی درباری", "#881337", Color(0xFF881337)),
    ColorOption("wine", "عنابی سلطنتی", "#9F1239", Color(0xFF9F1239)),
    ColorOption("ruby", "یاقوتی سرخ", "#DC2626", Color(0xFFDC2626)),
    ColorOption("rose", "رز مروارید", "#E11D48", Color(0xFFE11D48)),
    ColorOption("pink", "صورتی چرک", "#DB2777", Color(0xFFDB2777)),
    ColorOption("coral", "مرجانی", "#EA580C", Color(0xFFEA580C)),
    ColorOption("terracotta", "آجری گرم", "#C2410C", Color(0xFFC2410C)),
    
    // Purples & Violets
    ColorOption("purple", "بنفش سلطنتی", "#7E22CE", Color(0xFF7E22CE)),
    ColorOption("violet", "بادمجانی سیر", "#581C87", Color(0xFF581C87)),
    ColorOption("plum", "آلبالویی ملو", "#6B21A8", Color(0xFF6B21A8)),
    ColorOption("indigo", "نیلی شاهانه", "#4338CA", Color(0xFF4338CA)),
    ColorOption("lavender", "یاسی تیره", "#6D28D9", Color(0xFF6D28D9)),
    
    // Neutrals, Grays & Creams
    ColorOption("cream", "کرم نسکافه‌ای", "#A8A29E", Color(0xFFA8A29E)),
    ColorOption("charcoal", "ذغالی نوک‌مدادی", "#334155", Color(0xFF334155)),
    ColorOption("slate", "طوسی ماتیس", "#475569", Color(0xFF475569)),
    ColorOption("black", "مشکی چرم", "#18181B", Color(0xFF18181B))
)

val JALALI_MONTH_NAMES = listOf(
    "فروردین", "اردیبهشت", "خرداد",
    "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر",
    "دی", "بهمن", "اسفند"
)

val GREGORIAN_MONTH_NAMES = listOf(
    "ژانویه", "فوریه", "مارس",
    "آوریل", "مه", "ژوئن",
    "ژوئیه", "اوت", "سپتامبر",
    "اکتبر", "نوامبر", "دسامبر"
)

val PERSIAN_WEEKDAYS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

val PERSIAN_WEEKDAYS_FULL = listOf(
    "شنبه", "یکشنبه", "دوشنبه",
    "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
)

object PersianUtils {

    fun parseColor(hex: String, fallback: Color = Color(0xFF2563EB)): Color {
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            val colorInt = if (cleanHex.length == 6) {
                ("FF$cleanHex").toLong(16)
            } else {
                cleanHex.toLong(16)
            }
            Color(colorInt)
        } catch (_: Exception) {
            fallback
        }
    }

    fun getModelColor(modelName: String): String {
        val trimmed = modelName.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return COLOR_PALETTE[0].hex
        var hash = 0
        for (i in trimmed.indices) {
            hash = (hash shl 5) - hash + trimmed[i].code
        }
        val index = Math.abs(hash) % COLOR_PALETTE.size
        return COLOR_PALETTE[index].hex
    }

    fun toPersianDigits(input: Any?): String {
        if (input == null) return ""
        val str = input.toString()
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (c in str) {
            if (c in '0'..'9') {
                sb.append(persianDigits[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var res: String = input
        for (i in 0..9) {
            res = res.replace(persianDigits[i].toString(), i.toString())
            res = res.replace(arabicDigits[i].toString(), i.toString())
        }
        return res
    }

    fun formatNumberWithCommas(num: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,###", symbols)
        return toPersianDigits(formatter.format(num))
    }

    fun formatNumberWithCommas(num: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = if (num % 1.0 == 0.0) DecimalFormat("#,###", symbols) else DecimalFormat("#,###.##", symbols)
        return toPersianDigits(formatter.format(num))
    }

    fun formatCurrency(amount: Long, unit: String = "تومان"): String {
        return "${formatNumberWithCommas(amount)} $unit"
    }

    /**
     * Safe arithmetic piece counter with conversion rules support:
     * e.g. If 3 -> 2, 2 -> 1.5, 1 -> 1, 0.5 -> 0.5:
     * "3+3+1+1" -> (2 + 2 + 1 + 1) = 6.0
     * First converts each piece according to enabled conversion rules, then sums them up.
     */
    fun evaluateCountFormula(
        formula: String,
        conversionRules: List<com.example.model.UnitConversionRule> = emptyList()
    ): Double {
        if (formula.isBlank()) return 0.0
        val raw = formula.trim()
        if (raw.isBlank()) return 0.0

        val enabledRules = conversionRules.filter { it.isEnabled }

        fun convertSinglePart(token: String): Double {
            val cleanToken = token.trim()
            if (cleanToken.isBlank()) return 0.0

            // 1. Direct text match with rule's pieceKey (Persian or English digits/text)
            val textMatch = enabledRules.firstOrNull { rule ->
                rule.pieceKey.isNotBlank() && (
                    rule.pieceKey.trim().equals(cleanToken, ignoreCase = true) ||
                    toEnglishDigits(rule.pieceKey.trim()).equals(toEnglishDigits(cleanToken), ignoreCase = true)
                )
            }
            if (textMatch != null) {
                return textMatch.calculatedUnits
            }

            // 2. Numeric match if token can be parsed as a number
            val sanitized = toEnglishDigits(cleanToken).replace("/", ".").replace(",", "")
            val numericVal = sanitized.toDoubleOrNull()
            if (numericVal != null) {
                val numRuleMatch = enabledRules.firstOrNull { rule ->
                    (rule.pieceKey.isNotBlank() && (rule.pieceKey.toDoubleOrNull() != null && Math.abs((rule.pieceKey.toDoubleOrNull() ?: -999.0) - numericVal) < 0.0001)) ||
                    (rule.pieceCount > 0.0 && Math.abs(rule.pieceCount - numericVal) < 0.0001)
                }
                return numRuleMatch?.calculatedUnits ?: numericVal
            }

            return 0.0
        }

        return try {
            val parts = raw.split('+')
            var total = 0.0
            for (p in parts) {
                val trimmed = p.trim()
                if (trimmed.contains('*')) {
                    val mulParts = trimmed.split('*')
                    var mulResult = 1.0
                    for (m in mulParts) {
                        mulResult *= convertSinglePart(m)
                    }
                    total += mulResult
                } else {
                    total += convertSinglePart(trimmed)
                }
            }
            Math.round(total * 100.0) / 100.0
        } catch (_: Exception) {
            convertSinglePart(raw)
        }
    }

    // Jalali date calculation algorithms
    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var jy: Int
        var gy2 = gy
        if (gy2 > 1600) {
            jy = 979
            gy2 -= 1600
        } else {
            jy = 0
            gy2 -= 621
        }
        val gy3 = if (gm > 2) gy2 + 1 else gy2
        var days = 365 * gy2 + ((gy3 + 3) / 4) - ((gy3 + 99) / 100) + ((gy3 + 399) / 400) - 80 + gd + g_d_m[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    data class GregorianDate(val year: Int, val month: Int, val day: Int)

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): GregorianDate {
        var gy: Int
        var jy2 = jy
        if (jy2 > 979) {
            gy = 1600
            jy2 -= 979
        } else {
            gy = 621
        }
        var days = 365 * jy2 + ((jy2 / 33) * 8) + (((jy2 % 33) + 3) / 4) + 78 + jd + (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30 + 186))
        gy += 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val sal_a = intArrayOf(
            0, 31,
            if ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 0
        for (i in 0 until 13) {
            val v = sal_a[i]
            if (gd <= v) break
            gd -= v
            gm++
        }
        return GregorianDate(gy, gm, gd)
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        if (month in 1..6) return 31
        if (month in 7..11) return 30
        val r = (year + 38) * 31 % 128
        val isLeap = r <= 31
        return if (isLeap) 30 else 29
    }

    fun getDaysInGregorianMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getTodayJalaliString(): String {
        val cal = Calendar.getInstance()
        val j = gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val mStr = if (j.month < 10) "0${j.month}" else j.month.toString()
        val dStr = if (j.day < 10) "0${j.day}" else j.day.toString()
        return "${j.year}/$mStr/$dStr"
    }

    fun getTodayGregorianString(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
        val d = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        return "$y/$m/$d"
    }

    fun getTodayDateByCalendar(calendarType: com.example.model.CalendarType): String {
        return if (calendarType == com.example.model.CalendarType.GREGORIAN) {
            getTodayGregorianString()
        } else {
            getTodayJalaliString()
        }
    }

    fun parseJalaliParts(dateStr: String): JalaliDate? {
        val english = toEnglishDigits(dateStr).trim()
        val parts = english.split('/', '-', '.')
        if (parts.size >= 3) {
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            return JalaliDate(y, m, d)
        }
        return null
    }

    /**
     * Formats remaining balance following the rule:
     * When total received > total work (balance < 0), display negative number with "(بدهکاری)"
     */
    fun formatRemainingBalanceText(balance: Long, currencyUnit: String): String {
        return when {
            balance == 0L -> "تسویه حساب کامل"
            balance > 0 -> formatCurrency(balance, currencyUnit)
            else -> "-${formatCurrency(Math.abs(balance), currencyUnit)} (بدهکاری)"
        }
    }

    fun getRemainingBalanceLabel(balance: Long): String {
        return when {
            balance == 0L -> "تسویه حساب کامل"
            balance < 0 -> "باقی‌مانده (بدهکاری)"
            else -> "باقی‌مانده"
        }
    }

    /**
     * Returns 0 for شنبه (Saturday) through 6 for جمعه (Friday)
     */
    fun getJalaliDayOfWeek(jalaliDateStr: String): Int? {
        val jp = parseJalaliParts(jalaliDateStr) ?: return null
        val g = jalaliToGregorian(jp.year, jp.month, jp.day)
        val cal = Calendar.getInstance().apply {
            set(g.year, g.month - 1, g.day)
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    val PERSIAN_WEEKDAY_NAMES = listOf(
        "شنبه",
        "یکشنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنجشنبه",
        "جمعه"
    )
}
