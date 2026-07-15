package com.vaycore.finance.util.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.vaycore.finance.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object LocationInfoHelper {

    // ── coarse location permission check only ──────────────────────────
    private fun hasCoarsePermission() =
        ContextCompat.checkSelfPermission(
            App.appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Only allow COARSE-related providers (no GPS)
     * fused > network > passive, priority determined by order
     */
    private fun coarseProviders(lm: LocationManager): List<String> {
        val enabled = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())
        return listOf(
            "fused",                            // Google Play fused location, best accuracy
            LocationManager.NETWORK_PROVIDER,   // Wi-Fi / cell tower
            LocationManager.PASSIVE_PROVIDER    // fallback: reuse other apps' location results
        ).filter { enabled.contains(it) }
    }

    // ── public entry ─────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    suspend fun getLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasCoarsePermission()) return@withContext null

        val lm = App.appContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = coarseProviders(lm)
        if (providers.isEmpty()) return@withContext null

        // ① return cached location first, avoid unnecessary listening
        getCachedLocation(lm, providers)?.let { return@withContext it }

        // ② no cache, request all providers concurrently with 10s timeout
        withTimeoutOrNull(10_000L) {
            requestFirstResult(lm, providers)
        }
    }

    // ── cache: pick the most recent ──────────────────────────────────
    @SuppressLint("MissingPermission")
    private fun getCachedLocation(lm: LocationManager, providers: List<String>): Location? =
        providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

    /**
     * Listen to all providers concurrently; first non-null result wins, rest cancelled.
     *
     * Key improvements:
     * - Each provider has an 8s built-in timeout (Handler.postDelayed) to prevent
     *   hanging when neither onLocationChanged nor onProviderDisabled is called.
     * - Uses `async + awaitFirst` instead of the flawed select loop.
     * - Uses requestLocationUpdates (replaces deprecated requestSingleUpdate).
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestFirstResult(
        lm: LocationManager,
        providers: List<String>
    ): Location? = coroutineScope {

        val mainHandler = Handler(Looper.getMainLooper())
        val PROVIDER_TIMEOUT_MS = 8_000L

        val deferred = providers.map { provider ->
            async {
                suspendCancellableCoroutine { cont ->

                    val listener = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            if (cont.isActive) cont.resume(loc)
                            runCatching { lm.removeUpdates(this) }
                        }
                        override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                        override fun onProviderEnabled(p: String) {}
                        override fun onProviderDisabled(p: String) {
                            // fail fast when provider is disabled, don't wait for timeout
                            if (cont.isActive) cont.resume(null)
                            runCatching { lm.removeUpdates(this) }
                        }
                    }

                    // provider built-in timeout: end with null after 8s if no result
                    val timeoutRunnable = Runnable {
                        if (cont.isActive) cont.resume(null)
                        runCatching { lm.removeUpdates(listener) }
                    }

                    mainHandler.post {
                        runCatching {
                            lm.requestLocationUpdates(
                                provider,
                                0L, 0f,
                                listener,
                                Looper.getMainLooper()
                            )
                            mainHandler.postDelayed(timeoutRunnable, PROVIDER_TIMEOUT_MS)
                        }.onFailure {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            if (cont.isActive) cont.resume(null)
                        }
                    }

                    cont.invokeOnCancellation {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        runCatching { lm.removeUpdates(listener) }
                    }
                }
            }
        }

        // await in provider priority order:
        // fused first, cancel rest on first non-null; return null if all null
        var result: Location? = null
        for (d in deferred) {
            val loc = d.await()
            if (loc != null) {
                result = loc
                break
            }
        }
        // ensure all pending requests are cancelled
        deferred.forEach { it.cancel() }
        result
    }

    // ── address resolution ───────────────────────────────────────────
    suspend fun getAddress(latitude: Double, longitude: Double): Address? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        Geocoder(App.appContext, Locale.getDefault())
                            .getFromLocation(latitude, longitude, 1) { results ->
                                cont.resume(results.firstOrNull())
                            }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    Geocoder(App.appContext, Locale.getDefault())
                        .getFromLocation(latitude, longitude, 1)
                        ?.firstOrNull()
                }
            }.getOrNull()
        }

    suspend fun getLocationInfo(): Pair<Location?, Address?> {
        val location = getLocation() ?: return null to null
        val address = getAddress(location.latitude, location.longitude)
        return location to address
    }
}