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

        // Bottom sheet setup
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = 200 // you can adjust this height in dp later

        // RecyclerView setup
        recyclerAdapter = DramaLocationAdapter(emptyList())
        binding.locationRecyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerAdapter
        }

        // Map setup
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        iconBitmap = BitmapFactory.decodeResource(resources, R.drawable.goldenthread_icon)

        // Read all CSVs
        val locations = readLocationsFromCSV("GoldenThread Data -  Locations.csv")
        val dramas = readDramasFromCSV("GoldenThread Data - Dramas.csv")
        val dramaLocations = readDramaLocationsFromCSV("GoldenThread Data - dl.csv")

        if (locations.isEmpty()) {
            Log.e("Map", "No locations found.")
            return
        }

        val boundsBuilder = LatLngBounds.Builder()

        for (loc in locations) {
            val position = LatLng(loc.latitude, loc.longitude)
            val scaledIcon = getScaledMarkerIcon(iconBitmap!!, 10f)
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(loc.nameEn)
                    .snippet(loc.address)
                    .icon(scaledIcon)
            )
            if (marker != null) {
                markers.add(marker)
                marker.tag = loc
            }
            boundsBuilder.include(position)
        }

        val bounds = boundsBuilder.build()
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

        // Dynamic scaling
        mMap.setOnCameraIdleListener {
            val zoom = mMap.cameraPosition.zoom
            iconBitmap?.let {
                val scaled = getScaledMarkerIcon(it, zoom)
                for (marker in markers) marker.setIcon(scaled)
            }
        }

        // Marker click listener
        mMap.setOnMarkerClickListener { marker ->
            val loc = marker.tag as? LocationData ?: return@setOnMarkerClickListener true
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))

            // Find related dramas and scenes
            val relatedDL = dramaLocations.filter { it.locationId == loc.id }
            val relatedItems = relatedDL.mapNotNull { dl ->
                val drama = dramas.find { it.dramaId == dl.dramaId }
                drama?.let {
                    LocationDramaItem(
                        name_en = loc.nameEn,
                        name_th = loc.nameTh,
                        address = loc.address,
                        title_en = it.titleEn,
                        title_th = it.titleTh,
                        release_year = it.releaseYear,
                        scene_notes = dl.sceneNotes,
                        order_in_trip = dl.orderInTrip,
                        car_travel_min = dl.carTravelMin
                    )
                }
            }

            recyclerAdapter.updateData(relatedItems)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            true
        }

        // Hide sheet when tapping on map
        mMap.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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

    private data class Drama(
        val dramaId: String,
        val titleEn: String,
        val titleTh: String,
        val releaseYear: String
    )

    private data class DramaLocation(
        val dramaId: String,
        val locationId: String,
        val sceneNotes: String,
        val orderInTrip: Int,
        val carTravelMin: Int
    )

    data class LocationDramaItem(
        val name_en: String,
        val name_th: String,
        val address: String,
        val title_en: String,
        val title_th: String,
        val release_year: String,
        val scene_notes: String,
        val order_in_trip: Int,
        val car_travel_min: Int
    )

    private fun readLocationsFromCSV(fileName: String): List<LocationData> {
        val list = mutableListOf<LocationData>()
        try {
            val reader = BufferedReader(InputStreamReader(requireContext().assets.open(fileName)))
            reader.readLine() // skip header
            reader.forEachLine { line ->
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                if (parts.size >= 6) {
                    list.add(
                        LocationData(
                            id = parts[0].trim(),
                            nameEn = parts[1].trim(),
                            nameTh = parts[2].trim(),
                            address = parts[3].trim().replace("\"", ""),
                            latitude = parts[4].toDoubleOrNull() ?: 0.0,
                            longitude = parts[5].toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CSV", "Error reading Locations: ${e.message}")
        }
        return list
    }

    private fun readDramasFromCSV(fileName: String): List<Drama> {
        val list = mutableListOf<Drama>()
        try {
            val reader = BufferedReader(InputStreamReader(requireContext().assets.open(fileName)))
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                if (parts.size >= 4) {
                    list.add(
                        Drama(
                            dramaId = parts[0].trim(),
                            titleEn = parts[1].trim(),
                            titleTh = parts[2].trim(),
                            releaseYear = parts[3].trim()
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CSV", "Error reading Dramas: ${e.message}")
        }
        return list
    }

    private fun readDramaLocationsFromCSV(fileName: String): List<DramaLocation> {
        val list = mutableListOf<DramaLocation>()
        try {
            val reader = BufferedReader(InputStreamReader(requireContext().assets.open(fileName)))
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                if (parts.size >= 5) {
                    list.add(
                        DramaLocation(
                            dramaId = parts[0].trim(),
                            locationId = parts[1].trim(),
                            sceneNotes = parts[2].trim().replace("\"", ""),
                            orderInTrip = parts[3].toIntOrNull() ?: 0,
                            carTravelMin = parts[4].toIntOrNull() ?: 0
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CSV", "Error reading DL: ${e.message}")
        }
        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
