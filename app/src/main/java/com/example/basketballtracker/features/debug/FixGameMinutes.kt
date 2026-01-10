import com.example.basketballtracker.features.livegame.data.LiveGameRepository
import com.example.basketballtracker.features.livegame.ui.EventType
import com.example.basketballtracker.features.livegame.ui.LiveEvent

suspend fun fixQuarterBoundarySubs(
    gameId: Long,
    quarterLengthSec: Int,
    quartersCount: Int,
    liveRepo: LiveGameRepository
) {
    val events = liveRepo.getLiveEventsOnce(gameId)

    fun has(type: EventType, pid: Long, period: Int, clock: Int): Boolean {
        return events.any {
            it.type == type &&
                    it.playerId == pid &&
                    it.period == period &&
                    it.clockSecRemaining == clock
        }
    }

    // סדר כרונולוגי
    val sorted = events.sortedWith(
        compareBy<LiveEvent>({ it.period }, { -it.clockSecRemaining }, { it.createdAt })
    )

    val onCourt = linkedSetOf<Long>()
    var currentPeriod = 1

    suspend fun closePeriod(p: Int) {
        // OUT לכל מי שעל המגרש בסוף רבע
        val toClose = onCourt.toList()
        toClose.forEach { pid ->
            if (!has(EventType.SUB_OUT, pid, p, 0)) {
                liveRepo.addEvent(
                    gameId = gameId,
                    playerId = pid,
                    type = EventType.SUB_OUT,
                    period = p,
                    clockSecRemaining = 0
                )
            }
        }
    }

    suspend fun openNextPeriod(nextP: Int, carried: List<Long>) {
        // אם שחקן המשיך לרבע הבא בלי שעשית IN ידני, נוסיף IN בתחילת הרבע הבא
        carried.forEach { pid ->
            if (!has(EventType.SUB_IN, pid, nextP, quarterLengthSec)) {
                liveRepo.addEvent(
                    gameId = gameId,
                    playerId = pid,
                    type = EventType.SUB_IN,
                    period = nextP,
                    clockSecRemaining = quarterLengthSec
                )
            }
        }
    }

    // “להריץ” את החילופים לאורך המשחק
    for (e in sorted) {
        // מעבר רבע → סוגרים את הקודם ופותחים את הבא למי שנשאר בפנים
        if (e.period != currentPeriod) {
            val carried = onCourt.toList()
            closePeriod(currentPeriod)
            if (currentPeriod < quartersCount) {
                openNextPeriod(currentPeriod + 1, carried)
            }
            currentPeriod = e.period
        }

        val pid = e.playerId ?: continue
        when (e.type) {
            EventType.SUB_IN -> onCourt.add(pid)
            EventType.SUB_OUT -> onCourt.remove(pid)
            else -> Unit
        }
    }

    // לסגור גם את הרבע האחרון
    closePeriod(currentPeriod)
}
