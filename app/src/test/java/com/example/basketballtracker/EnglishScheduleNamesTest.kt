package com.example.basketballtracker

import com.example.basketballtracker.utils.englishTeamName
import com.example.basketballtracker.features.schedule.ui.fixtureDate
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class EnglishScheduleNamesTest {
    @Test fun importedOpponentsUseEnglishAndCustomNamesArePreserved() {
        assertEquals("Levinsky-Wingate Academic Center", englishTeamName("המרכז האקדמי לוינסקי וינגייט"))
        assertEquals("Bar-Ilan University", englishTeamName("אוניברסיטת בר אילן"))
        assertEquals("College of Management", englishTeamName("המסלול האקדמי המכללה למינהל"))
        assertEquals("Sami Shamoon College of Engineering - Ashdod", englishTeamName("המכללה האקדמית להנדסה עש סמי שמעון אשדוד"))
        assertEquals("Ariel University", englishTeamName("אגודת הסטודנטים אריאל"))
        assertEquals("Shenkar", englishTeamName("שנקר"))
        assertEquals("Reichman University", englishTeamName("אוניברסיטת רייכמן"))
        assertEquals("Tel Aviv University", englishTeamName("אוניברסיטת תל-אביב"))
        assertEquals("Holon Institute of Technology", englishTeamName("מכון טכנולוגי חולון"))
        assertEquals("Custom opponent", englishTeamName("Custom opponent"))
    }
    @Test fun scheduleDatesStayEnglishOnHebrewDevices() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("he-IL"))
            assertEquals("26 Nov 2026", fixtureDate(1795644000000L))
        } finally { Locale.setDefault(previous) }
    }
}
