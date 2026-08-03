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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.vaycore.finance.app.App
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

object LocationInfoHelper {

    private val fusedCacheTimeout = 2.seconds
    private val fusedCurrentTimeout = 10.seconds
    private val platformCurrentTimeout = 10.seconds
    private const val ADDRESS_TIMEOUT_MS = 3_000L
    private val totalTimeout = 15.seconds
    private val geocoderExecutor = Executors.newCachedThreadPool()

    private fun hasCoarsePermission() =
        ContextCompat.checkSelfPermission(
            App.appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getLocationInfo(): Pair<Location?, Address?> =
        withTimeoutOrNull(totalTimeout) {
            if (!hasCoarsePermission()) return@withTimeoutOrNull null to null

            val lm = App.appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = getFusedCache()
                ?: getManagerCache(lm)
                ?: getFusedCurrentLocation()
                ?: getPlatformCurrentLocation(lm)

            location to location?.let { getAddress(it.latitude, it.longitude) }
        } ?: (null to null)

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
        withTimeoutOrNull(fusedCurrentTimeout) {
            val cancellationTokenSource = CancellationTokenSource()
            val client = LocationServices.getFusedLocationProviderClient(App.appContext)
            suspendCancellableCoroutine { cont ->
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

    @SuppressLint("MissingPermission")
    private suspend fun getPlatformCurrentLocation(lm: LocationManager): Location? {
        val providers = coarseProviders(lm)
        if (providers.isEmpty()) return null
        return withTimeoutOrNull(platformCurrentTimeout) {
            requestFirstResult(lm, providers)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFirstResult(
        lm: LocationManager,
        providers: List<String>,
    ): Location? = coroutineScope {
        val handler = Handler(Looper.getMainLooper())
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
                            if (cont.isActive) cont.resume(null)
                            runCatching { lm.removeUpdates(this) }
                        }
                    }
                    val timeout = Runnable {
                        if (cont.isActive) cont.resume(null)
                        runCatching { lm.removeUpdates(listener) }
                    }
                    handler.post {
                        runCatching {
                            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                            handler.postDelayed(timeout, 8_000L)
                        }.onFailure {
                            handler.removeCallbacks(timeout)
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                    cont.invokeOnCancellation {
                        handler.removeCallbacks(timeout)
                        runCatching { lm.removeUpdates(listener) }
                    }
                }
            }
        }
        var result: Location? = null
        for (d in deferred) {
            val loc = d.await()
            if (loc != null) { result = loc; break }
        }
        deferred.forEach { it.cancel() }
        result
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
                    Geocoder(App.appContext, Locale.getDefault())
                        .getFromLocation(latitude, longitude, 1) { complete(it.firstOrNull()) }
                }.onFailure { complete(null) }
            } else {
                geocoderExecutor.execute {
                    val address = runCatching {
                        @Suppress("DEPRECATION")
                        Geocoder(App.appContext, Locale.getDefault())
                            .getFromLocation(latitude, longitude, 1)
                            ?.firstOrNull()
                    }.getOrNull()
                    complete(address)
                }
            }
            cont.invokeOnCancellation { handler.removeCallbacks(timeout) }
        }
}
