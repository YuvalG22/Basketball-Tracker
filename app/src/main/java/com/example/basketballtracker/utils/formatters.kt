package com.example.basketballtracker.utils

import kotlin.math.max

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