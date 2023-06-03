package app.statest.camerax

import java.util.*

object Utilities {
    fun getStampTimeId(): String {
        var fullDate: String? = null
        val c = Calendar.getInstance()
        val hour = String.format("%02d", c[Calendar.HOUR_OF_DAY])
        val mint = String.format("%02d", c[Calendar.MINUTE])
        val sec = String.format("%02d", c[Calendar.SECOND])
        val miliSec = String.format("%02d", c[Calendar.MILLISECOND])
        val todaysDate = (c[Calendar.YEAR] * 10000
                + (c[Calendar.MONTH] + 1) * 100
                + c[Calendar.DAY_OF_MONTH])
        val DateString = todaysDate.toString()
        try {
            val Year = String.format("%04d", DateString.substring(0, 4).toInt())
            val Month = String.format("%02d", DateString.substring(4, 6).toInt())
            val Day = String.format("%02d", DateString.substring(6, 8).toInt())
            fullDate = Day + Month + Year + hour + mint + sec + miliSec
        } catch (e: java.lang.Exception) {
            e.message
        }
        return convertArabicDigitsToEnglish(fullDate.toString())
    }
    fun convertArabicDigitsToEnglish(input: String): String {
        return input.replace("\u0660", "0").replace("\u0661", "1")
            .replace("\u0662", "2").replace("\u0663", "3")
            .replace("\u0664", "4").replace("\u0665", "5")
            .replace("\u0666", "6").replace("\u0667", "7")
            .replace("\u0668", "8").replace("\u0669", "9")
    }
}