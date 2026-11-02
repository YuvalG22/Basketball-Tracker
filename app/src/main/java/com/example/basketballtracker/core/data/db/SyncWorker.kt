package com.example.basketballtracker.core.data.db

import android.content.Context
import kotlinx.coroutines.CancellationException
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.basketballtracker.core.data.BasketballApp
import com.example.basketballtracker.core.data.remote.stats.SyncBackendUpgradeRequired

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

            repository.synchronize()

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SyncBackendUpgradeRequired) {
            Log.e("SyncWorker", "Backend update required: ${e.endpoint}; protocol=${e.reportedVersion ?: "unavailable"}", e)
            // Recheck on the next scheduled/manual sync; rapid retries cannot add a missing route.
            Result.failure(workDataOf("syncError" to e.message))
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            // אם נכשל (למשל אין אינטרנט), אנדרואיד ינסה שוב מאוחר יותר
            Result.retry()
        }
    }
}
