package com.example.basketballtracker.utils

private val englishTeamNames = mapOf(
    "המרכז האקדמי לוינסקי וינגייט" to "Levinsky-Wingate Academic Center",
    "אוניברסיטת בר אילן" to "Bar-Ilan University",
    "המסלול האקדמי המכללה למינהל" to "College of Management",
    "המכללה האקדמית להנדסה עש סמי שמעון אשדוד" to "Sami Shamoon College of Engineering - Ashdod",
    "אגודת הסטודנטים אריאל" to "Ariel University",
    "שנקר" to "Shenkar",
    "אוניברסיטת רייכמן" to "Reichman University",
    "אוניברסיטת תל-אביב" to "Tel Aviv University",
    "מכון טכנולוגי חולון" to "Holon Institute of Technology"
)

fun englishTeamName(name: String): String = englishTeamNames[name.trim()] ?: name
