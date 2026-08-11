package com.vaycore.finance.util.deivce

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
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vaycore.finance.app.App
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object LocationInfoHelper {

    private val fusedCacheTimeout = 500.milliseconds
    private val freshLocationTimeout = 2.seconds
    private const val ADDRESS_TIMEOUT_MS = 1_000L
    private val totalTimeout = 3.seconds
    private val geocoderExecutor = Executors.newCachedThreadPool()

    private fun hasCoarsePermission() =
        ContextCompat.checkSelfPermission(
            App.appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getLocationInfo(): Pair<Location?, Address?> {
        if (!hasCoarsePermission()) return null to null

        val startedAt = SystemClock.elapsedRealtime()
        val lm = App.appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = withTimeoutOrNull(totalTimeout) {
            getCachedLocation(lm) ?: getFreshLocation(lm)
        } ?: return null to null

        // Address lookup only uses the remaining budget and cannot discard a valid coordinate.
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val remainingAddressTime = (totalTimeout.inWholeMilliseconds - elapsed)
            .coerceAtMost(ADDRESS_TIMEOUT_MS)
        val address = if (remainingAddressTime > 0L) {
            withTimeoutOrNull(remainingAddressTime.milliseconds) {
                getAddress(location.latitude, location.longitude)
            }
        } else {
            null
        }
        return location to address
    }

    /** Prefer an immediately available platform cache before consulting the fused cache. */
    private suspend fun getCachedLocation(lm: LocationManager): Location? =
        getManagerCache(lm) ?: getFusedCache()

    @SuppressLint("MissingPermission")
    private suspend fun getFusedCache(): Location? =
        withTimeoutOrNull(fusedCacheTimeout) {
            val client = LocationServices.getFusedLocationProviderClient(App.appContext)
            suspendCancellableCoroutine<Location?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getFusedCurrentLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            val cancellationTokenSource = CancellationTokenSource()
            val client = LocationServices.getFusedLocationProviderClient(App.appContext)
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token,
            )
                .addOnSuccessListener { location ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
                .addOnCanceledListener {
                    if (cont.isActive) cont.resume(null)
                }
            cont.invokeOnCancellation { cancellationTokenSource.cancel() }
        }

    @SuppressLint("MissingPermission")
    private fun getManagerCache(lm: LocationManager): Location? {
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun coarseProviders(lm: LocationManager): List<String> {
        val enabled = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { enabled.contains(it) }
    }

    /** Race fused, network and passive providers; the first valid location wins. */
    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(lm: LocationManager): Location? =
        withTimeoutOrNull(freshLocationTimeout) {
            coroutineScope {
                val requests = buildList<Deferred<Location?>> {
                    add(async { getFusedCurrentLocation() })
                    coarseProviders(lm).forEach { provider ->
                        add(async { requestProviderLocation(lm, provider) })
                    }
                }
                awaitFirstValidLocation(requests)
            }
        }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun requestProviderLocation(
        lm: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { cont ->
        val handler = Handler(Looper.getMainLooper())
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (cont.isActive) cont.resume(location)
                runCatching { lm.removeUpdates(this) }
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                if (cont.isActive) cont.resume(null)
                runCatching { lm.removeUpdates(this) }
            }
        }

        handler.post {
            if (!cont.isActive) return@post
            runCatching {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }
        cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
    }

    private suspend fun awaitFirstValidLocation(
        requests: List<Deferred<Location?>>,
    ): Location? {
        val pending = requests.toMutableList()
        return try {
            while (pending.isNotEmpty()) {
                val (completed, location) = select {
                    pending.forEach { request ->
                        request.onAwait { request to it }
                    }
                }
                pending.remove(completed)
                if (location != null) return location
            }
            null
        } finally {
            requests.forEach { it.cancel() }
        }
    }

    private suspend fun getAddress(latitude: Double, longitude: Double): Address? =
        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            val timeout = Runnable {
                if (cont.isActive) cont.resume(null)
            }
            val complete: (Address?) -> Unit = { address ->
                handler.removeCallbacks(timeout)
                if (cont.isActive) cont.resume(address)
            }

            handler.postDelayed(timeout, ADDRESS_TIMEOUT_MS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    Geocoder(App.appContext, Locale.ENGLISH)
                        .getFromLocation(latitude, longitude, 1) { complete(it.firstOrNull()) }
                }.onFailure { complete(null) }
            } else {
                geocoderExecutor.execute {
                    val address = runCatching {
                        @Suppress("DEPRECATION")
                        Geocoder(App.appContext, Locale.ENGLISH)
                            .getFromLocation(latitude, longitude, 1)
                            ?.firstOrNull()
                    }.getOrNull()
                    complete(address)
                }
            }
            cont.invokeOnCancellation { handler.removeCallbacks(timeout) }
        }
}
