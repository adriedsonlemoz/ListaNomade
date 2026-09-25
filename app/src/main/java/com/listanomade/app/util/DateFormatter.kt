package com.listanomade.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    fun format(millis: Long): String = synchronized(formatter) { formatter.format(Date(millis)) }
}
