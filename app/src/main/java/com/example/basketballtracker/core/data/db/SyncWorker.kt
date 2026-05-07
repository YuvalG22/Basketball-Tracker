package com.example.basketballtracker.core.data.db

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: BasketballRepository // תלוי איך ה-Repository שלך מוזרק
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting background sync...")

            // 1. קודם מעלים מה שנוצר במכשיר
            repository.syncPending()

            // 2. אחר כך מושכים עדכונים מהענן
            repository.fetchAllFromCloud()

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            // אם נכשל, אנדרואיד ינסה שוב מאוחר יותר (Retry)
            Result.retry()
        }
    }
}