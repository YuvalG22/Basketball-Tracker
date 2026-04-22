package com.example.basketballtracker.utils

import kotlin.math.max
import kotlin.math.sqrt

fun formatClock(secRemaining: Int): String {
    val safe = max(0, secRemaining)
    val mm = safe / 60
    val ss = safe % 60
    return "%01d:%02d".format(mm, ss)
}

fun formatPlayerName(fullName: String?): String {
    if (fullName.isNullOrBlank()) return ""

    val parts = fullName.trim().split(" ")
    if (parts.size < 2) return fullName

    val firstInitial = parts[0].first()
    val lastName = parts.last()

    return "$firstInitial. $lastName"
}

fun periodLabel(period: Int): String {
    return when(period) {
        in 1..4 -> "Q$period"
        5 -> "OT"
        else -> "OT${period - 4}"
    }
}

fun calculateShotDistance(x: Float, y: Float): Float {
    val hoopX = 7.5f
    val hoopY = 1.575f

    val dx = x - hoopX
    val dy = y - hoopY

    return sqrt(dx * dx + dy * dy)
}