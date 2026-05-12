package com.example.basketballtracker.core.data.mapper

import com.example.basketballtracker.core.data.db.entities.PlayerSeasonStatsEntity
import com.example.basketballtracker.features.stats.domain.PlayerSeasonStats

fun PlayerSeasonStatsEntity.toDomain(): PlayerSeasonStats {
    return PlayerSeasonStats(
        playerId = playerId,
        playerName = playerName,
        playerNumber = playerNumber,
        gp = gp,
        pts = pts,
        ast = ast,
        rebTotal = rebTotal,
        rebDef = rebDef,
        rebOff = rebOff,
        stl = stl,
        blk = blk,
        tov = tov,
        pf = pf,
        fgm = fgm,
        fga = fga,
        threem = threem,
        threea = threea,
        ftm = ftm,
        fta = fta
    )
}