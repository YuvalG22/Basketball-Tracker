# Basketball Tracker — Detailed Feature Guide

**Source snapshot:** commit `06aee1a`  
**Prepared:** 25 October 2026  
**Application:** Basketball Tracker for Android

This guide describes the features present in the current source code, how users move through them, and what information the app records. It distinguishes visible controls from functionality that exists only in supporting code. It is a source-based inventory, not a claim that every workflow has been verified on a device.

## Contents

- [1. App overview](#1-app-overview)
- [2. Navigation and home screen](#2-navigation-and-home-screen)
- [3. Player roster](#3-player-roster)
- [4. Creating a game](#4-creating-a-game)
- [5. Live scoreboard and game clock](#5-live-scoreboard-and-game-clock)
- [6. Lineups and substitutions](#6-lineups-and-substitutions)
- [7. Recording player and opponent statistics](#7-recording-player-and-opponent-statistics)
- [8. Interactive shot chart and assists](#8-interactive-shot-chart-and-assists)
- [9. Play-by-play and undo](#9-play-by-play-and-undo)
- [10. Ending a game and viewing its summary](#10-ending-a-game-and-viewing-its-summary)
- [11. Game history](#11-game-history)
- [12. Individual player history and analysis](#12-individual-player-history-and-analysis)
- [13. Season statistics and leaders](#13-season-statistics-and-leaders)
- [14. Storage and cloud synchronization](#14-storage-and-cloud-synchronization)
- [15. Interface and device support](#15-interface-and-device-support)
- [16. Availability notes and known limitations](#16-availability-notes-and-known-limitations)
- [17. Statistics glossary](#17-statistics-glossary)
- [18. Typical end-to-end workflow](#18-typical-end-to-end-workflow)

## 1. App overview

Basketball Tracker is an Android basketball scorekeeping and statistics app. Its main workflow is to create a game, choose a roster, manage the players on court, record basketball events, and review the resulting player and team statistics.

The live-game workspace is designed around a wide, tablet-style layout. Historical and season screens provide follow-up analysis.

The tracked team is labeled **Afeka** in several screens. Opponent names are entered separately for each game. The app does not expose a team-name editor or a multi-team management screen.

| Area | Main capabilities |
| --- | --- |
| Home | Latest game, continue a live game, open a completed summary, create a game, manual sync |
| Players | Add players, browse the roster, open player statistics |
| New game | Opponent, home/away, regular-season or playoff details, game roster |
| Live game | Clock, scoreboard, substitutions, player events, opponent shooting, shot locations, assists, play-by-play, undo |
| Game summary | Final result, quarter scoring, team totals, player box score |
| Games | Browse previous games and delete games |
| Player analysis | Averages, individual game logs, quarter breakdowns, shot charts |
| Season stats | Team totals or averages, shooting efficiency, category leaders |
| Data | Local database, pending uploads, cloud downloads and reconciliation |

## 2. Navigation and home screen

### Main navigation

The bottom navigation bar provides four destinations: **Home**, **Games**, **Players**, and **Stats**. It appears on those main screens. New-game setup, the live workspace, player details, and game summaries use their own screen flows.

The live workspace hides the Android system bars for more usable space. System bars return outside the live screen.

### Latest-game card

Home highlights the most recently created local game. The card includes the opponent, date, home/away arrangement, scores, and game status. Playoff games include their stage and game number.

For a live game, the card displays the saved period and clock and offers **Tap to continue**. Its score is calculated from recorded scoring events. For a completed game, it displays the final score and offers **Tap for summary**. The winning side receives visual emphasis.

### Home actions

- **NEW GAME** opens game setup.
- **Sync Now**, represented by a cloud icon, starts an upload-and-download cycle.
- A progress indicator replaces the sync icon while the operation is running.
- The sync button is disabled during that operation to prevent another manual sync from the same control.

There is no separate synchronization history or detailed success/error report on Home.

## 3. Player roster

### Browsing players

The Players screen lists players with their names and jersey numbers and shows the roster count. Players are loaded from the local database in name order. An empty-state card is shown when the roster has no players.

Tapping a player opens that player's individual statistics and game history.

### Adding a player

The **Add Player** dialog asks for a name and jersey number. Names must contain non-whitespace text, and the number must parse as an integer. The jersey-number field accepts digit characters.

The Add control stays disabled until both inputs are valid. Saving trims surrounding whitespace from the name, creates the player locally, and attempts a cloud upload.

**Availability note:** update and delete operations exist in the player repository and ViewModel, but the current roster screen does not expose player-edit or player-delete controls.

## 4. Creating a game

### Game details

The New Game screen places game details and roster selection side by side.

| Input | Behavior |
| --- | --- |
| Opponent | Free-text opponent name; an empty value is saved as “Unknown” |
| Home game | Toggle determines whether Afeka is displayed as home or away |
| Competition type | Choose regular season or playoffs |
| Regular-season round | Numeric input, limited to three entered digits; missing input becomes round 0 |
| Playoff stage | Quarter Final, Semi Final, or Final |
| Playoff game number | Numeric value identifying the game within the playoff matchup |
| Game date | Automatically captured from the device when setup opens |

The screen does not expose a calendar/date picker. The configured default is four quarters of ten minutes each; the current UI does not expose a quarter-length selector.

### Choosing the game roster

Users select participating players from the existing roster. At least **five players** must be selected before Start Game becomes available. More than five may be selected so that substitutes are included.

Until the minimum is reached, the button states how many additional players are needed.

Starting creates the game and its roster entries locally and attempts to upload them. The live screen opens after the setup operation completes.

**Roster versus lineup:** choosing players for a game does not automatically put five players on court. The initial lineup is established with the substitution controls.

## 5. Live scoreboard and game clock

### Live workspace

The live screen contains a scoreboard across the top and three working areas below it:

- **Players:** on-court and bench lists.
- **Player Stats / Play-by-Play:** selected-player shooting and court interaction, or the event log.
- **Actions:** opponent shooting controls and player statistic buttons.

### Scoreboard

The scoreboard displays both teams' names and scores, the current period, remaining time, and team-foul counts for the current period. Home/away selection determines the sides on which the teams appear.

Scores update from the recorded scoring events, with animated score changes. Period labels support Q1–Q4 and overtime periods.

### Clock controls

- **Play/Pause** starts or stops the countdown.
- Time is displayed as minutes and seconds.
- **Next Period** is available while the clock is paused.
- Advancing records period-end and period-start events and starts the next period with a reset, paused clock.
- When the running clock reaches zero, the countdown stops and the app records substitutions out for the players currently on court.

Advancing beyond the fourth quarter is supported. Additional periods currently use the same configured length as regulation quarters; there is no separately configured overtime duration.

### Correcting the clock and period

Tapping the clock opens **Adjust Game Clock**. Users can add or subtract:

- 10 seconds.
- 30 seconds.
- 1 minute.
- One period through the period-correction controls.

Clock corrections are bounded between zero and the configured quarter length. Manual period corrections are bounded from 1 to 10.

### Saving and restoring live state

The app persists the period, remaining seconds, running/paused status, and the clock's saved start timestamp. Reopening a live game restores those values. If the saved clock was running, restoration deducts elapsed wall-clock time.

This means a running clock can advance while the live screen is not visible. It is not automatically paused simply because the user leaves the screen.

## 6. Lineups and substitutions

### On-court and bench groups

Game-roster players are separated into **On Court** and **Bench** based on substitution events.

Player cards show abbreviated names, jersey numbers, personal-foul dots, whole minutes played, points, rebounds, and assists. Selection is visually highlighted. Bench players are ordered using their recorded playing time.

### Substitution controls

- Use the upward arrow on a bench player to record a substitution in.
- Use the downward arrow on an on-court player to record a substitution out.
- The bench substitution-in control is disabled when five players are already on court.
- Substitution events include the period and clock, providing the basis for playing-time and plus/minus calculations.

The lineup is managed explicitly by the operator; the app does not automatically choose starters or substitutes.

### Detailed live player card

Long-pressing a player opens a dialog with their identity, opponent, date, detailed statistics, and shot chart. The dialog offers **All**, **Q1**, **Q2**, **Q3**, and **Q4** filters.

The detailed card includes minutes, points, assists, rebounds, steals, blocks, turnovers, fouls, plus/minus, field-goal shooting, three-point shooting, and free throws.

The current implementation has player-selection and time-calculation defects in this dialog, described in Section 16.

## 7. Recording player and opponent statistics

### Player action buttons

After selecting a player, the main action panel offers these controls:

| Control | Recorded result |
| --- | --- |
| FT MADE | One point, one free throw made, one free throw attempted |
| FT MISS | One free throw attempted |
| REB D | Defensive rebound |
| REB O | Offensive rebound |
| STL | Steal |
| BLK | Block |
| TOV | Turnover |
| PF | Personal foul |

Two-point and three-point shots are entered through the interactive court rather than dedicated player shot buttons.

The main player buttons require a selected player, an unfinished game, and fewer than five personal fouls for that player. They are also disabled while a made shot is awaiting assist selection. These checks are applied to the button panel; they are not a universal validation rule across every input path.

A standalone manual AST button is commented out in the current version. The active assist-entry workflow is the made-shot assist picker.

### Opponent shooting controls

Opponent events are recorded at team level, without selecting an opponent player:

- OPP 2PT MADE and OPP 2PT MISS.
- OPP 3PT MADE and OPP 3PT MISS.
- OPP FT MADE and OPP FT MISS.

The opponent section shows cumulative field goals, three-pointers, and free throws as made/attempted totals with percentages. Successful attempts also update the opponent's score.

Opponent buttons are currently passed an enabled value directly, so they do not require selection of an Afeka player.

### Derived statistics

Recorded events drive team scores and player box scores. A made field goal increments both made and attempted counts. Total rebounds combine offensive and defensive rebounds. Percentages are calculated from makes divided by attempts; zero attempts display as zero percent.

Plus/minus is calculated by applying scoring changes to the players recorded as on court at the time. Playing time is calculated from substitution intervals and the game clock. Displayed minutes are whole minutes rather than a minutes-and-seconds value.

Opponent fouls are supported in the event model and foul-count calculation, but the current live UI does not expose an opponent-foul entry button.

## 8. Interactive shot chart and assists

### Court input

The Player Stats tab shows the selected player's shooting totals and a half-court diagram. The court records the position of field-goal attempts.

The current gesture handler accepts **stylus input only**:

- A short stylus tap records a missed field goal.
- A stylus press held for approximately half a second records a made field goal.
- The position determines whether the event is a two-point or three-point attempt.

Ordinary finger input does not record shots through this court handler.

### Stored shot information

Each court attempt includes court coordinates and calculated distance from the hoop. The diagram uses a 15-by-14 court coordinate space and a hoop-relative three-point calculation with corner boundaries.

Made shots are drawn as green circles, and missed shots as red crosses. The selected player's existing shots appear on the live court; historical and detail views also display shot-location charts.

Free throws are recorded through buttons and do not receive court coordinates from this workflow.

### Assist picker

For an Afeka made two-pointer or three-pointer, the app asks **Who passed the ball?**

The choices are **No assist** or another currently on-court player. The scorer is excluded. Confirmation saves the made shot with an optional assister reference; selecting an assister also records a separate assist event for that player.

Dismissing the picker discards the pending made-shot entry. There are timing and synchronization limitations in this workflow, listed in Section 16.

## 9. Play-by-play and undo

### Event log

The Play-by-Play tab displays recorded events in basketball time order. Entries can show the event type, player or opponent, period, clock, and a score snapshot. Assisted made shots can display the assister's name.

Team and opponent entries appear on the appropriate sides according to home/away selection. Scoring events have additional visual emphasis. Period starts and ends appear as dedicated markers.

The list automatically scrolls to its newest displayed entry when events are added.

### Filters

- **All** shows the full event list, including substitutions and non-scoring statistics.
- **Score** shows events classified as scoring events; this classification also includes period markers.

The filter changes the displayed log, not the underlying recorded events or box-score totals.

### Undo

The undo icon reverses the most recently created active event. Synced events are marked for remote deletion; events with no remote ID are deleted locally. Removing an event updates the event-driven score and statistics.

Undo operates on individual events. A made shot and its automatically generated assist are separate records; there is no grouped action history or redo control.

## 10. Ending a game and viewing its summary

### Finish-game flow

The scoreboard's settings icon opens a menu containing **End Game**. A confirmation dialog offers End or Cancel.

Ending stops the clock, calculates the final scores, marks the game finished, attempts pending synchronization, and returns the user to Home. Tapping the completed latest-game card opens its summary.

### Result and quarter scoring

The summary shows the opponent, round, date, final result, and scores. A quarter table presents Afeka's and the opponent's points for each configured regulation quarter.

### Team totals

The team dashboard includes:

- Points, total rebounds, and assists.
- Steals, blocks, turnovers, and personal fouls.
- Field goals, three-pointers, and free throws as made/attempted totals and percentages.

### Player box score

The box score groups players into starters and bench. Starters are inferred from the first lineup to reach five players through substitution events.

The player-name column stays fixed while the statistics can scroll horizontally. Columns cover MIN, PTS, REB, AST, DREB, OREB, STL, BLK, FG, 3PT, FT, TO, PF, and +/-. A team-total row aggregates the counting statistics.

The current summary does not expose editing, sharing, or file export. Although its subtitle mentions game leaders, there is no separate active game-leaders section.

## 11. Game history

The **Games** tab lists stored games, newest-created first. Each card shows the competition context, opponent, home/away score arrangement, date, and a win/loss badge, with visual emphasis on the winning side.

Regular-season cards show a round. Playoff cards show a playoff stage and game number. Selecting a card opens its game summary.

A trash icon opens a deletion confirmation. Confirming removes local events, marks the game for deletion, and attempts cloud deletion. Pending game deletion is retried through synchronization.

There is no search, user-selectable sort, competition filter, or date-range filter in the current history screen. Its cards use a win/loss presentation rather than a dedicated draw state.

## 12. Individual player history and analysis

### Player overview

Opening a roster player shows the player's name, counted games, and six averages:

- Points per game.
- Rebounds per game.
- Assists per game.
- Steals per game.
- Blocks per game.
- Turnovers per game.

These player-profile statistics are derived from locally available events. The profile game list is built from games for which the player has recorded events; being named on a roster alone does not create a game-log entry.

### Game log and detailed sheet

Game cards show game context and a compact player performance summary. Selecting a card opens a bottom sheet with **All** and **Q1–Q4** filters.

The sheet includes minutes, points, free throws, two-pointers, three-pointers, combined field goals, total/defensive/offensive rebounds, assists, turnovers, steals, blocks, personal fouls, and a shot-location chart.

Quarter selection filters the displayed statistics and shots. Dedicated overtime filter buttons are not provided in these detail sheets.

The shot-chart component contains filtering support in code, but its makes-only and misses-only controls are commented out. The current visible chart displays all qualifying shots for the selected game/quarter.

## 13. Season statistics and leaders

### Display modes

The Stats tab provides **Per Game** and **Total** modes, defaulting to Per Game. Switching modes changes counting-stat displays and counting-stat leader rankings. Shooting percentages remain percentage-based.

### Team overview

The dashboard presents games, points or points per game, rebounds, assists, steals, blocks, turnovers, personal fouls, and shooting summaries for FG%, 3P%, and FT%.

Shooting summaries combine players' makes and attempts before calculating percentages. They do not average individual percentages.

The displayed team game count is currently the largest player games-played count in the downloaded season dataset.

### Category leaders

Nine categories each display up to three ranked players with their name, jersey number, and value:

| Counting-stat categories | Shooting-efficiency categories |
| --- | --- |
| Points | Field-goal percentage |
| Rebounds | Three-point percentage |
| Assists | Free-throw percentage |
| Steals | |
| Blocks | |
| Turnovers | |

Shooting leaders need at least one attempt in the relevant category. Counting-stat categories are ranked from highest to lowest; the turnovers category therefore shows the highest turnover totals or averages.

The screen shows an empty state when season statistics are unavailable. It does not expose a season/year selector. Unlike the local player profile, this dashboard uses season aggregates downloaded from the backend.

## 14. Storage and cloud synchronization

### Local records

The app uses a Room database to store players, games, game rosters, events, and downloaded season aggregates. Game records also store the clock and competition details.

New players, games, roster entries, and events are written locally and then uploaded when their creation workflow attempts it. Failed uploads generally leave records marked pending.

Existing local rosters, games, and events can be read without downloading them again. Connectivity is still needed for cloud transfer and refreshing backend-generated season statistics. Some UI workflows await their upload attempt, so a slow connection can delay completion even after the local write.

### Manual synchronization

Home's Sync Now first processes pending event deletions and uploads pending players, games, rosters, and events. It then fetches players, games, rosters, events, and season statistics from the cloud.

The season-stat refresh requests recalculation from the backend before downloading the aggregate results. Cloud games missing from a fetched game list are reconciled by deleting their matching local game records.

Remote IDs are used to match cloud records to local records and translate most game/player relationships between installations.

### Live-game synchronization

Opening a live game, undoing an event, and finishing a game trigger pending synchronization. Clock persistence also triggers a throttled sync attempt, at most once every five seconds through that clock-triggered path.

Individual creation operations may also upload directly, separately from the sync manager.

### Background worker availability

A background sync worker exists and can upload pending changes and fetch cloud data. However, registration of the periodic worker is **commented out in the current MainActivity**. Fresh installations of this snapshot do not schedule hourly synchronization through that code.

A device that previously scheduled the work may retain that persisted job; the current snapshot does not explicitly cancel it. Manual and live-game sync paths remain active.

## 15. Interface and device support

- Android application with a minimum supported API level of 26 and a target SDK of 36.
- Jetpack Compose and Material 3 interface.
- System-selected light and dark color schemes, with many individual components using explicit dark colors.
- Green accents for selected items, primary actions, and highlighted statistics.
- Bundled Inter typography, card layouts, badges, foul dots, and abbreviated player names in compact areas.
- Haptic feedback on several recording, clock, long-press, and undo interactions.
- Scrollable lists, player detail sheets, dialogs, and a horizontally scrollable box score.
- A live layout built for wide/tablet use; some player analysis components adapt to available screen width.
- Resizable Android activity; left-to-right layout is explicitly enforced by the Compose theme.
- Localized device date/number formatting in some areas, with English labels and English-formatted dates in others.

No app-level theme picker, language picker, or general Settings screen is exposed.

## 16. Availability notes and known limitations

### Code present without an active user control

| Capability | Current availability |
| --- | --- |
| Edit or delete a roster player | Supporting operations exist; roster UI exposes adding and viewing |
| Reset the current quarter | ViewModel method exists; no active reset button |
| Custom quarter length | Stored/configurable in supporting interfaces; no setup selector |
| Opponent personal-foul entry | Event model and counter exist; no active entry button |
| Manual standalone assist entry | Button commented out; made-shot assist picker remains active |
| Main live shot-chart period selector | Filtering state exists, but no visible selector changes it |
| Makes-only / misses-only historical chart filters | Controls commented out |
| Shooting doughnut charts in game summary | Components exist but are not called by the active summary |
| Periodic background sync | Worker exists; scheduling code commented out |
| General Settings | Route constant exists; no registered Settings destination |

### Features not exposed in this snapshot

There is no visible account/login flow, multiple-team or league management, player photo/profile editor, team-name editor, shot clock, timeout tracking, automated video capture, CSV/PDF export, or in-app sharing action.

A FileProvider declaration is present, but it is not connected to a user-facing export workflow. This feature guide is an external project document, not an export produced by the app.

### Previously identified implementation issues

The following issues remain in the source snapshot. They qualify the feature descriptions above and should be addressed before relying on these workflows in production.

| Area | Known issue |
| --- | --- |
| Database upgrades | The migration removing shotZone uses DROP COLUMN, which is incompatible with older supported Android SQLite versions |
| Cloud reconciliation | Fetching a cloud game can overwrite unsynced local changes |
| Undo during upload | An event can be uploaded after its local record was deleted, allowing it to reappear on refresh |
| Assist persistence | The separate assist event is saved only after the made-shot upload attempt completes |
| Assist synchronization | Downloaded assister references use another device's local player ID rather than translating the remote ID |
| Shot timing | A made shot uses the clock at assist confirmation rather than the initial shot interaction |
| Live player detail | Some statistics use the selected player instead of the player whose dialog was opened |
| Playing time | Several detail calculations use a ten-second quarter instead of the game's duration |
| Offline game deletion | Main game queries do not filter deletion markers, so pending-deleted games can remain visible |

This document does not certify correctness beyond source inspection. Backend behavior, real-device gestures, layouts, and network-failure scenarios were not exercised for this export.

## 17. Statistics glossary

| Abbreviation | Meaning |
| --- | --- |
| GP | Games played |
| MIN | Whole minutes played |
| PTS / PPG | Points / points per game |
| REB / RPG | Total rebounds / rebounds per game |
| DREB / REB D | Defensive rebounds |
| OREB / REB O | Offensive rebounds |
| AST / APG | Assists / assists per game |
| STL / SPG | Steals / steals per game |
| BLK / BPG | Blocks / blocks per game |
| TO / TOV / TPG / TOPG | Turnovers / turnovers per game, depending on the display |
| PF | Personal fouls |
| FG / FGM / FGA | Field goals / field goals made / field goals attempted |
| FG% | Field-goal makes divided by attempts |
| 3PT / 3P% | Three-point shooting totals / percentage |
| FT / FT% | Free-throw shooting totals / percentage |
| +/- | Team scoring margin accumulated while a player is recorded as on court |
| Q1–Q4 | Regulation quarters |
| OT | Overtime |

## 18. Typical end-to-end workflow

1. Open Players and add names and jersey numbers for the team roster.
2. Open New Game from Home.
3. Enter the opponent and home/away status, and choose regular-season or playoff context.
4. Select at least five participating players and start the game.
5. Substitute five players in to establish the opening lineup.
6. Start the clock and select the player associated with each action.
7. Use the stylus court for made/missed field goals, the assist picker for assisted makes, and buttons for free throws and other player events.
8. Record opponent attempts from the opponent action panel.
9. Pause and correct the clock when needed, manage substitutions, and use play-by-play plus undo to review or correct recent entries.
10. Advance periods with Next Period, re-establishing the lineup after automatic period-end substitutions.
11. End the game from the scoreboard menu and confirm.
12. Review the summary, browse Games or individual player history, and use Sync Now to refresh cloud data and season aggregates.

---

**Source basis:** navigation, active Compose screens, live-game models and ViewModel, repositories, Room entities/DAOs, sync manager/worker, application theme, and build configuration at commit `06aee1a`.

