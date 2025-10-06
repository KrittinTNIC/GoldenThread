package com.example.goldenthread

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.goldenthread.databinding.FragmentThreadBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import java.io.BufferedReader
import java.io.InputStreamReader

class ThreadFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentThreadBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThreadBinding.inflate(inflater, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        loadLocationsFromCSV()
    }

    private fun loadLocationsFromCSV() {
        val inputStream = requireContext().assets.open("GoldenThread Data - Location.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val boundsBuilder = LatLngBounds.Builder()
        var line: String?

        // Skip header if needed
        reader.readLine()

        while (reader.readLine().also { line = it } != null) {
            val columns = parseCSVLine(line!!)
            if (columns.size >= 6) {
                val nameEn = columns[1]
                val nameTh = columns[2]
                val latitude = columns[4].toDoubleOrNull()
                val longitude = columns[5].toDoubleOrNull()

                if (latitude != null && longitude != null) {
                    val position = LatLng(latitude, longitude)
                    map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(nameEn)
                            .snippet(nameTh)
                    )
                    boundsBuilder.include(position)
                }
            }
        }

        reader.close()

        // Move camera to show all markers
        val bounds = boundsBuilder.build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    // Handles CSV lines that may include commas inside quotes
    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var insideQuote = false

        for (char in line) {
            when (char) {
                '"' -> insideQuote = !insideQuote
                ',' -> if (insideQuote) current.append(char) else {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

