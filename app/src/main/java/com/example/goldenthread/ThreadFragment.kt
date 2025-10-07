package com.example.goldenthread

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
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

class ThreadFragment : Fragment(), OnMapReadyCallback,
    DramaLocationAdapter.OnItemButtonClickListener {

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

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

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
        recyclerAdapter = DramaLocationAdapter(emptyList(), this)
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

        // Map fragment initialization
        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this) ?: Log.e(TAG, "MapFragment not found")
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

        loadAllCsvSafely()
        addMarkersFromLocations()

        // Dynamic scaling
        mMap?.setOnCameraIdleListener {
            val zoom = mMap?.cameraPosition?.zoom ?: return@setOnCameraIdleListener
            iconBitmap?.let { bmp ->
                val scaled = getScaledMarkerIcon(bmp, zoom)
                for (marker in markers) marker.setIcon(scaled)
            }
        }

        // Marker click
        mMap?.setOnMarkerClickListener { marker ->
            val tag = marker.tag
            if (tag is List<*>) {
                val listItem = tag.filterIsInstance<LocationDramaItem>()
                if (listItem.isNotEmpty()) {
                    recyclerAdapter.updateData(listItem)
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
                }
            } else if (tag is LocationDramaItem) {
                recyclerAdapter.updateData(listOf(tag))
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
            }
            true
        }

        // Hide bottom sheet on map click
        mMap?.setOnMapClickListener {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Enable user location
        checkLocationPermission()
    }

    private fun loadAllCsvSafely() {
        try { dramas.clear(); dramas.addAll(readDramasFromCSV("GoldenThread Data - Dramas.csv")) } catch (t: Throwable) { Log.e(TAG, "Error loading dramas CSV: ${t.message}", t) }
        try { dlList.clear(); dlList.addAll(readDramaLocationsFromCSV("GoldenThread Data - dl.csv")) } catch (t: Throwable) { Log.e(TAG, "Error loading dl CSV: ${t.message}", t) }
        try { locations.clear(); locations.addAll(readLocationsFromCSV("GoldenThread Data -  Locations.csv")) } catch (t: Throwable) { Log.e(TAG, "Error loading locations CSV: ${t.message}", t) }
    }

    private fun addMarkersFromLocations() {
        mMap ?: run { Log.e(TAG, "Map not ready"); return }
        if (locations.isEmpty()) { Log.w(TAG, "No locations"); return }

        markers.clear()
        val boundsBuilder = LatLngBounds.Builder()
        for (loc in locations) {
            if (loc.latitude == 0.0 && loc.longitude == 0.0) continue
            val position = LatLng(loc.latitude, loc.longitude)
            val scaledIcon = iconBitmap?.let { getScaledMarkerIcon(it, 10f) }
            val markerOptions = MarkerOptions().position(position).title(loc.nameEn).snippet(loc.address)
            scaledIcon?.let { markerOptions.icon(it) }
            val marker = mMap!!.addMarker(markerOptions)
            if (marker != null) {
                val relatedDL = dlList.filter { it.locationId == loc.id }
                val items = relatedDL.mapNotNull { dl ->
                    val drama = dramas.find { it.dramaId == dl.dramaId }
                    LocationDramaItem(
                        nameEn = loc.nameEn,
                        nameTh = loc.nameTh,
                        address = loc.address,
                        titleEn = drama?.titleEn ?: "Unknown Drama",
                        titleTh = drama?.titleTh ?: "",
                        releaseYear = drama?.releaseYear ?: "",
                        sceneNotes = dl.sceneNotes,
                        orderInTrip = dl.orderInTrip,
                        carTravelMin = dl.carTravelMin,
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                }
                marker.tag = if (items.isNotEmpty()) items else LocationDramaItem(
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
                markers.add(marker)
                boundsBuilder.include(position)
            }
        }

        if (markers.isNotEmpty()) {
            try {
                val bounds = boundsBuilder.build()
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (t: Throwable) { Log.e(TAG, "Error moving camera: ${t.message}", t) }
        }
    }

    private fun getScaledMarkerIcon(original: Bitmap, zoom: Float): BitmapDescriptor {
        val size = (64 * (zoom / 7f)).toInt().coerceIn(32, 84)
        val scaled = Bitmap.createScaledBitmap(original, size, size, false)
        return BitmapDescriptorFactory.fromBitmap(scaled)
    }

    // --- Location Permission ---
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            enableMyLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }

    private fun enableMyLocation() {
        try {
            mMap?.isMyLocationEnabled = true
            mMap?.uiSettings?.isMyLocationButtonEnabled = true
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    // ---- CSV readers ----
    private fun readLocationsFromCSV(fileName: String): List<LocationData> {
        val list = mutableListOf<LocationData>()
        try {
            val stream = requireContext().assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(stream))
            reader.readLine() // skip header
            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val parts = parseCsv(line)
                if (parts.size >= 6) {
                    val lat = parts[4].toDoubleOrNull() ?: 0.0
                    val lng = parts[5].toDoubleOrNull() ?: 0.0
                    list.add(LocationData(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim().replace("\"",""), lat, lng))
                }
            }
            reader.close()
        } catch (t: Throwable) { Log.e(TAG, "readLocationsFromCSV failed: ${t.message}", t) }
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
                if (parts.size >= 4) list.add(Drama(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()))
            }
            reader.close()
        } catch (t: Throwable) { Log.e(TAG, "readDramasFromCSV failed: ${t.message}", t) }
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
                    list.add(DL(parts[0].trim(), parts[1].trim(), parts[2].trim().replace("\"",""), parts[3].toIntOrNull() ?: 0, parts[4].toIntOrNull() ?: 0))
                }
            }
            reader.close()
        } catch (t: Throwable) { Log.e(TAG, "readDramaLocationsFromCSV failed: ${t.message}", t) }
        return list
    }

    private fun parseCsv(line: String): List<String> {
        val regex = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")
        return line.split(regex).map { it.trim() }
    }

    // ---- Recycler button click callbacks ----
    override fun onGoToDrama(item: LocationDramaItem) {
        // Implement navigation to drama details
        Log.d(TAG, "Go to drama clicked: ${item.titleEn}")
    }

    override fun onGoToNextPoint(item: LocationDramaItem) {
        // Implement next point navigation (e.g., zoom to next marker)
        val nextMarker = markers.find { m ->
            val tag = m.tag
            if (tag is List<*>) tag.any { it is LocationDramaItem && it.orderInTrip == item.orderInTrip + 1 }
            else if (tag is LocationDramaItem) tag.orderInTrip == item.orderInTrip + 1
            else false
        }
        nextMarker?.let { mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it.position, 15f)) }
    }

    override fun onFavorite(item: LocationDramaItem) {
        // Implement favorite toggle logic here
        Log.d(TAG, "Favorite clicked: ${item.titleEn}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            mMap?.setOnCameraIdleListener(null)
            mMap?.setOnMarkerClickListener(null)
            mMap?.setOnMapClickListener(null)
            mMap?.clear()
        } catch (t: Throwable) { Log.w(TAG, "Error clearing map: ${t.message}") }
        _binding = null
    }
}
