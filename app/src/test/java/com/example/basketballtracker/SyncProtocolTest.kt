package com.example.basketballtracker

import com.example.basketballtracker.core.data.remote.stats.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SyncProtocolTest {
    private fun httpError(code: Int): HttpException {
        val raw = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://example.test/sync/capabilities?private=value").build())
            .protocol(Protocol.HTTP_1_1).code(code).message("Test response").build()
        return HttpException(Response.error<SyncCapabilities>("{}".toResponseBody("application/json".toMediaType()), raw))
    }

    @Test fun supportedBackendPasses() = runBlocking {
        requireSupportedSyncProtocol { SyncCapabilities(2) }
    }

    @Test fun missingRouteExplainsRequiredUpgradeAndReportsPathWithoutQuery() = runBlocking {
        val cause = httpError(404)
        try {
            requireSupportedSyncProtocol { throw cause }
            fail("Expected deployment mismatch")
        } catch (error: SyncBackendUpgradeRequired) {
            assertSame(cause, error.cause)
            assertEquals("https://example.test/sync/capabilities", error.endpoint)
            assertTrue(error.message!!.contains("saved on this device"))
        }
    }

    @Test fun oldProtocolExplainsRequiredUpgrade() = runBlocking {
        try {
            requireSupportedSyncProtocol { SyncCapabilities(1) }
            fail("Expected old protocol rejection")
        } catch (error: SyncBackendUpgradeRequired) {
            assertEquals(1, error.reportedVersion)
        }
    }

    @Test fun serverOutageStillPropagatesAsRetryableHttpFailure() = runBlocking {
        val cause = httpError(503)
        try {
            requireSupportedSyncProtocol { throw cause }
            fail("Expected server failure")
        } catch (error: HttpException) { assertSame(cause, error) }
    }

    @Test fun cancellationIsNotMisreportedAsBackendUpgrade() = runBlocking {
        val cause = CancellationException("Test cancellation")
        try {
            requireSupportedSyncProtocol { throw cause }
            fail("Expected cancellation")
        } catch (error: CancellationException) { assertSame(cause, error) }
    }
}
