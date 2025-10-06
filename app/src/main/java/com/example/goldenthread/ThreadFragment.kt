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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.BufferedReader
import java.io.InputStreamReader

class ThreadFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentThreadBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private val markers = mutableListOf<Marker>()
    private var iconBitmap: Bitmap? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var recyclerAdapter: DramaLocationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThreadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isHideable = true

        // Setup RecyclerView adapter with empty list initially
        recyclerAdapter = DramaLocationAdapter(emptyList())
        binding.locationRecyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerAdapter
        }

        // Setup map fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        iconBitmap = BitmapFactory.decodeResource(resources, R.drawable.goldenthread_icon)

        val locations = readLocationsFromCSV("GoldenThread Data -  Locations.csv")
        if (locations.isEmpty()) {
            Log.e("Map", "No locations found in CSV")
            return
        }

        val boundsBuilder = LatLngBounds.Builder()

        for (loc in locations) {
            val position = LatLng(loc.latitude, loc.longitude)
            val initialZoom = 10f
            val scaledIcon = getScaledMarkerIcon(iconBitmap!!, initialZoom)
            val marker = mMap.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(position)
                    .title(loc.nameEn)
                    .snippet(loc.address)
                    .icon(scaledIcon)
            )
            if (marker != null) {
                markers.add(marker)
                marker.tag = loc // store the location object for later
            }
            boundsBuilder.include(position)
        }

        // Move camera to fit all markers
        val bounds = boundsBuilder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))

        // Dynamic scaling for markers when zoom changes
        mMap.setOnCameraIdleListener {
            val zoom = mMap.cameraPosition.zoom
            iconBitmap?.let { bitmap ->
                val scaled = getScaledMarkerIcon(bitmap, zoom)
                for (marker in markers) {
                    marker.setIcon(scaled)
                }
            }
        }

        // Marker click listener
        mMap.setOnMarkerClickListener { marker ->
            val locationData = marker.tag as? LocationData
            if (locationData != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))

                // Update RecyclerView with clicked location
                recyclerAdapter.updateData(
                    listOf(
                        LocationItem(
                            locationData.nameEn,
                            locationData.nameTh,
                            locationData.address
                        )
                    )
                )

                // Show bottom sheet with animation
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            true
        }

        // Hide bottom sheet when tapping on map
        mMap.setOnMapClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    private fun getScaledMarkerIcon(original: Bitmap, zoom: Float): BitmapDescriptor {
        val size = (64 * (zoom / 7f)).toInt().coerceIn(32, 84)
        val scaled = Bitmap.createScaledBitmap(original, size, size, false)
        return BitmapDescriptorFactory.fromBitmap(scaled)
    }

    private data class LocationData(
        val id: String,
        val nameEn: String,
        val nameTh: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )

    data class LocationItem(
        val nameEn: String,
        val nameTh: String,
        val address: String
    )

    private fun readLocationsFromCSV(fileName: String): List<LocationData> {
        val locations = mutableListOf<LocationData>()
        try {
            val inputStream = requireContext().assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // skip header
            reader.forEachLine { line ->
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                if (parts.size >= 6) {
                    val id = parts[0].trim()
                    val nameEn = parts[1].trim()
                    val nameTh = parts[2].trim()
                    val address = parts[3].trim().replace("\"", "")
                    val lat = parts[4].toDoubleOrNull() ?: return@forEachLine
                    val lng = parts[5].toDoubleOrNull() ?: return@forEachLine
                    locations.add(LocationData(id, nameEn, nameTh, address, lat, lng))
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CSV", "Error reading CSV: ${e.message}")
        }
        return locations
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
