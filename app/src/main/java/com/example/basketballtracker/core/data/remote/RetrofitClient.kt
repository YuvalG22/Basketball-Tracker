package com.example.basketballtracker.core.data.remote

import com.example.basketballtracker.core.data.remote.events.EventApi
import com.example.basketballtracker.core.data.remote.games.GameApi
import com.example.basketballtracker.core.data.remote.players.PlayerApi
import com.example.basketballtracker.core.data.remote.roster.RosterApi
import com.example.basketballtracker.core.data.remote.stats.StatsApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://basketball-tracker-api.onrender.com/"
    private val retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create()).build()
    }
    val eventApi: EventApi by lazy { retrofit.create(EventApi::class.java) }
    val gameApi: GameApi by lazy { retrofit.create(GameApi::class.java) }
    val playerApi: PlayerApi by lazy { retrofit.create(PlayerApi::class.java) }
    val rosterApi: RosterApi by lazy { retrofit.create(RosterApi::class.java) }
    val statsApi: StatsApi by lazy { retrofit.create(StatsApi::class.java) }
}
