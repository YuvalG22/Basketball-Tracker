package com.example.basketballtracker.core.data.db

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.basketballtracker.core.data.BasketballApp

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting background sync...")

            // שליפת ה-SyncManager מתוך מחלקת ה-App שיצרנו
            val app = applicationContext as BasketballApp
            val repository = app.syncManager

            // 1. קודם מעלים מה שנוצר במכשיר
            repository.syncPending()

            // 2. אחר כך מושכים עדכונים מהענן
            repository.fetchAllFromCloud()

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            // אם נכשל (למשל אין אינטרנט), אנדרואיד ינסה שוב מאוחר יותר
            Result.retry()
        }
    }
}