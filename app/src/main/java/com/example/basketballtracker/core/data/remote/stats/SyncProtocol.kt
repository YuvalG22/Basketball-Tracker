package com.example.basketballtracker.core.data.remote.stats

import retrofit2.HttpException
import java.io.IOException

class SyncBackendUpgradeRequired(
    val endpoint: String,
    val reportedVersion: Int? = null,
    cause: HttpException? = null
) : IOException("Cloud sync requires a server update. Your changes are saved on this device.", cause)

suspend fun requireSupportedSyncProtocol(fetch: suspend () -> SyncCapabilities) {
    val capabilities = try {
        fetch()
    } catch (error: HttpException) {
        if (error.code() != 404 && error.code() != 405) throw error
        val request = error.response()?.raw()?.request
        val endpoint = request?.url?.newBuilder()?.query(null)?.build()?.toString() ?: "/sync/capabilities"
        throw SyncBackendUpgradeRequired(endpoint, cause = error)
    }
    if (capabilities.protocolVersion < 2) {
        throw SyncBackendUpgradeRequired("/sync/capabilities", capabilities.protocolVersion)
    }
}
