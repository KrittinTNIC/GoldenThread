package com.example.goldenthread

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goldenthread.adapter.DramaLocationAdapter
import com.example.goldenthread.databinding.FragmentThreadBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.BufferedReader
import java.io.InputStreamReader

class ThreadFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentThreadBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private val markers = mutableListOf<Marker>()
    private var iconBitmap: Bitmap? = null

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private lateinit var recyclerAdapter: DramaLocationAdapter

    // Data holders
    private val dramas = mutableListOf<Drama>()
    private val dlList = mutableListOf<DL>()
    private val locations = mutableListOf<LocationData>()

    private val TAG = "ThreadFragment"

    // ---- Data classes ----
    data class LocationData(
        val id: String,
        val nameEn: String,
        val nameTh: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )

    data class Drama(
        val dramaId: String,
        val titleEn: String,
        val titleTh: String,
        val releaseYear: String
    )

    data class DL(
        val dramaId: String,
        val locationId: String,
        val sceneNotes: String,
        val orderInTrip: Int,
        val carTravelMin: Int
    )

    data class LocationDramaItem(
        val nameEn: String,
        val nameTh: String,
        val address: String,
        val titleEn: String,
        val titleTh: String,
        val releaseYear: String,
        val sceneNotes: String,
        val orderInTrip: Int,
        val carTravelMin: Int,
        val latitude: Double,
        val longitude: Double
    )

    // ------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentThreadBinding.inflate(inflater, container, false)

        // Recycler setup
        recyclerAdapter = DramaLocationAdapter(emptyList())
        binding.locationRecyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerAdapter
        }

        // BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior?.apply {
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = 0
        }

        // Map fragment initialization (safe)
        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this) ?: Log.e(TAG, "MapFragment is null or not found (id=R.id.map).")
        } catch (t: Throwable) {
            Log.e(TAG, "Error getting map fragment: ${t.message}", t)
        }

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        try {
            iconBitmap = BitmapFactory.decodeResource(resources, R.drawable.goldenthread_icon)
        } catch (t: Throwable) {
            Log.e(TAG, "Icon decode failed: ${t.message}", t)
        }

        // load CSVs safely (logs any errors)
        try {
            loadAllCsvSafely()
            addMarkersFromLocations()
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error during map setup: ${t.message}", t)
        }

        // dynamic scaling on camera idle — use safe map reference
        mMap?.setOnCameraIdleListener {
            val zoom = mMap?.cameraPosition?.zoom ?: return@setOnCameraIdleListener
            iconBitmap?.let { bmp ->
                val scaled = getScaledMarkerIcon(bmp, zoom)
                for (marker in markers) marker.setIcon(scaled)
            }
        }

        // marker click handler (safe)
        mMap?.setOnMarkerClickListener { marker ->
            val tag = marker.tag
            if (tag is LocationDramaItem) {
                try {
                    // zoom to marker
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
                    // update recycler and show bottom sheet
                    recyclerAdapter.updateData(listOf(tag))
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                } catch (t: Throwable) {
                    Log.e(TAG, "Error handling marker click: ${t.message}", t)
                }
            } else {
                Log.w(TAG, "Marker tag is not LocationDramaItem (or is null).")
            }
            true
        }

        // hide bottom sheet on map click
        mMap?.setOnMapClickListener {
            try {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
            } catch (t: Throwable) {
                Log.e(TAG, "Error hiding bottom sheet: ${t.message}", t)
            }
        }
    }

    private fun loadAllCsvSafely() {
        try {
            dramas.clear(); dramas.addAll(readDramasFromCSV("GoldenThread Data - Dramas.csv"))
        } catch (t: Throwable) {
            Log.e(TAG, "Error loading dramas CSV: ${t.message}", t)
        }
        try {
            dlList.clear(); dlList.addAll(readDramaLocationsFromCSV("GoldenThread Data - dl.csv"))
        } catch (t: Throwable) {
            Log.e(TAG, "Error loading dl CSV: ${t.message}", t)
        }
        try {
            locations.clear(); locations.addAll(readLocationsFromCSV("GoldenThread Data -  Locations.csv"))
        } catch (t: Throwable) {
            Log.e(TAG, "Error loading locations CSV: ${t.message}", t)
        }
    }

    private fun addMarkersFromLocations() {
        mMap ?: run {
            Log.e(TAG, "Map not ready; cannot add markers.")
            return
        }

        if (locations.isEmpty()) {
            Log.w(TAG, "No locations to display (locations list empty).")
            return
        }

        markers.clear()
        val boundsBuilder = LatLngBounds.Builder()
        for (loc in locations) {
            try {
                // If lat/lng are zero, log and skip (avoid placing at 0,0)
                if (loc.latitude == 0.0 && loc.longitude == 0.0) {
                    Log.w(TAG, "Skipping location with zero coords: ${loc.id} ${loc.nameEn}")
                    continue
                }
                val position = LatLng(loc.latitude, loc.longitude)
                val scaledIcon = iconBitmap?.let { getScaledMarkerIcon(it, 10f) }
                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(loc.nameEn)
                    .snippet(loc.address)
                scaledIcon?.let { markerOptions.icon(it) }

                val marker = mMap!!.addMarker(markerOptions)
                if (marker != null) {
                    // Build tag: if there are DL entries, create LocationDramaItem(s). If none, fallback.
                    val relatedDL = dlList.filter { it.locationId == loc.id }
                    val items = if (relatedDL.isNotEmpty()) {
                        relatedDL.firstNotNullOfOrNull { dl ->
                            val drama = dramas.find { it.dramaId == dl.dramaId }
                            if (drama != null) {
                                LocationDramaItem(
                                    nameEn = loc.nameEn,
                                    nameTh = loc.nameTh,
                                    address = loc.address,
                                    titleEn = drama.titleEn,
                                    titleTh = drama.titleTh,
                                    releaseYear = drama.releaseYear,
                                    sceneNotes = dl.sceneNotes,
                                    orderInTrip = dl.orderInTrip,
                                    carTravelMin = dl.carTravelMin,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude
                                )
                            } else {
                                // DL exists but drama missing: still create fallback item
                                LocationDramaItem(
                                    nameEn = loc.nameEn,
                                    nameTh = loc.nameTh,
                                    address = loc.address,
                                    titleEn = "Unknown Drama",
                                    titleTh = "",
                                    releaseYear = "",
                                    sceneNotes = dl.sceneNotes,
                                    orderInTrip = dl.orderInTrip,
                                    carTravelMin = dl.carTravelMin,
                                    latitude = loc.latitude,
                                    longitude = loc.longitude
                                )
                            }
                        } // choose first for tag (we expand to list in recycler when marker clicked)
                    } else {
                        // no dl entries -> fallback item with location info only
                        LocationDramaItem(
                            nameEn = loc.nameEn,
                            nameTh = loc.nameTh,
                            address = loc.address,
                            titleEn = "No associated drama",
                            titleTh = "",
                            releaseYear = "",
                            sceneNotes = "",
                            orderInTrip = 0,
                            carTravelMin = 0,
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    }

                    // store the LocationDramaItem as tag (we show single item; you can change to list)
                    marker.tag = items
                    markers.add(marker)
                    boundsBuilder.include(position)
                    Log.d(TAG, "Added marker: ${loc.id} ${loc.nameEn} @ ${loc.latitude},${loc.longitude}")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error adding marker for ${loc.id}: ${t.message}", t)
            }
        }

        // move camera to show all markers if any
        try {
            if (markers.isNotEmpty()) {
                val bounds = boundsBuilder.build()
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error moving camera to bounds: ${t.message}", t)
        }
    }

    private fun getScaledMarkerIcon(original: Bitmap, zoom: Float): BitmapDescriptor {
        val size = (64 * (zoom / 7f)).toInt().coerceIn(32, 84)
        val scaled = Bitmap.createScaledBitmap(original, size, size, false)
        return BitmapDescriptorFactory.fromBitmap(scaled)
    }

    // ---- CSV readers (robust) ----
    private fun readLocationsFromCSV(fileName: String): List<LocationData> {
        val list = mutableListOf<LocationData>()
        try {
            val stream = requireContext().assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(stream))
            reader.readLine() // skip header
            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val parts = parseCsv(line)
                // Expect at least 6 columns (id,name_en,name_th,address,lat,lng)
                if (parts.size >= 6) {
                    val lat = parts[4].toDoubleOrNull() ?: 0.0
                    val lng = parts[5].toDoubleOrNull() ?: 0.0
                    list.add(
                        LocationData(
                            id = parts[0].trim(),
                            nameEn = parts[1].trim(),
                            nameTh = parts[2].trim(),
                            address = parts[3].trim().replace("\"", ""),
                            latitude = lat,
                            longitude = lng
                        )
                    )
                } else {
                    Log.w(TAG, "Skipping malformed location line (parts=${parts.size}): $line")
                }
            }
            reader.close()
        } catch (t: Throwable) {
            Log.e(TAG, "readLocationsFromCSV failed: ${t.message}", t)
        }
        return list
    }

    private fun readDramasFromCSV(fileName: String): List<Drama> {
        val list = mutableListOf<Drama>()
        try {
            val stream = requireContext().assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(stream))
            reader.readLine()
            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val parts = parseCsv(line)
                if (parts.size >= 4) {
                    list.add(Drama(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()))
                } else {
                    Log.w(TAG, "Skipping malformed drama line: $line")
                }
            }
            reader.close()
        } catch (t: Throwable) {
            Log.e(TAG, "readDramasFromCSV failed: ${t.message}", t)
        }
        return list
    }

    private fun readDramaLocationsFromCSV(fileName: String): List<DL> {
        val list = mutableListOf<DL>()
        try {
            val stream = requireContext().assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(stream))
            reader.readLine()
            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val parts = parseCsv(line)
                if (parts.size >= 5) {
                    list.add(
                        DL(
                            dramaId = parts[0].trim(),
                            locationId = parts[1].trim(),
                            sceneNotes = parts[2].trim().replace("\"", ""),
                            orderInTrip = parts[3].toIntOrNull() ?: 0,
                            carTravelMin = parts[4].toIntOrNull() ?: 0
                        )
                    )
                } else {
                    Log.w(TAG, "Skipping malformed dl line: $line")
                }
            }
            reader.close()
        } catch (t: Throwable) {
            Log.e(TAG, "readDramaLocationsFromCSV failed: ${t.message}", t)
        }
        return list
    }

    // CSV split that respects quoted commas
    private fun parseCsv(line: String): List<String> {
        val regex = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")
        return line.split(regex).map { it.trim() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // remove map listeners and clear to avoid DeadObjectException during teardown
        try {
            mMap?.setOnCameraIdleListener(null)
            mMap?.setOnMarkerClickListener(null)
            mMap?.setOnMapClickListener(null)
            mMap?.clear()
        } catch (t: Throwable) {
            Log.w(TAG, "Error clearing map listeners: ${t.message}")
        }
        _binding = null
    }
}

