package com.example.goldenthread

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goldenthread.adapters.DramaDropdownAdapter
import com.example.goldenthread.adapters.GridAdapter
import com.example.goldenthread.adapters.PosterAdapter
import com.example.goldenthread.databinding.FragmentExploreBinding
import com.example.goldenthread.model.Drama
import com.example.goldenthread.ui.MovieDetailFragment
import com.example.goldenthread.util.loadDramasFromCSV
import com.google.android.material.tabs.TabLayout

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var allDramas: List<Drama>
    private var currentDramas: List<Drama> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allDramas = loadDramasFromCSV(requireContext())

        // 🎠 Carousel
        val carouselDramas =
            allDramas.filter { it.dramaId in listOf("D015", "D005", "D008", "D019") }
        binding.rvPosters.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPosters.adapter = PosterAdapter(requireContext(), carouselDramas) { drama ->
            openMovieDetail(drama)
        }

        // Categories
        val popularDramas = allDramas.take(6)
        val dramaDramas = allDramas.filter { it.genre.contains("Drama", ignoreCase = true) }
        val actionDramas = allDramas.filter { it.genre.contains("Action", ignoreCase = true) }
        val blglDramas = allDramas.filter { it.genre.contains("BL") || it.genre.contains("GL") }

        val categoryMap = mapOf(
            "Popular" to popularDramas,
            "Drama" to dramaDramas,
            "Action" to actionDramas,
            "BL/GL" to blglDramas
        )

        // Default Grid setup
        currentDramas = popularDramas
        binding.rvGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvGrid.adapter = GridAdapter(requireContext(), currentDramas) { drama ->
            openMovieDetail(drama)
        }

        // Tabs
        val categories = listOf("Popular", "Drama", "Action", "BL/GL")
        for (cat in categories) {
            binding.tabCategories.addTab(binding.tabCategories.newTab().setText(cat))
        }

        binding.tabCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedDramas = categoryMap[tab.text] ?: emptyList()
                currentDramas = selectedDramas
                binding.rvGrid.adapter =
                    GridAdapter(requireContext(), currentDramas) { drama ->
                        openMovieDetail(drama)
                    }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 🔍 Setup Search Dropdown with posters
        val searchBox = binding.searchBox as AutoCompleteTextView
        val dropdownAdapter = DramaDropdownAdapter(requireContext(), allDramas)
        searchBox.setAdapter(dropdownAdapter)

// Always re-show dropdown when typing (even after space)
        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    searchBox.showDropDown()
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

// When user picks from dropdown
        searchBox.setOnItemClickListener { _, _, position, _ ->
            val drama = dropdownAdapter.getItem(position)
            if (drama != null) {
                openMovieDetail(drama)
            }
        }

// When user presses Enter/Search
        searchBox.setOnEditorActionListener { _, _, _ ->
            val query = searchBox.text.toString()
            val drama = allDramas.find {
                it.titleEn.contains(query, ignoreCase = true) ||
                        it.titleTh.contains(query, ignoreCase = true)
            }
            if (drama != null) {
                openMovieDetail(drama)
                true
            } else false
        }

    }

    private fun openMovieDetail(drama: Drama) {
        val fragment = MovieDetailFragment().apply {
            arguments = Bundle().apply {
                putString("dramaId", drama.dramaId)
                putString("titleEn", drama.titleEn)
                putString("titleTh", drama.titleTh)
                putString("releaseYear", drama.releaseYear.toString())
                putString("duration", drama.duration)
                putString("summary", drama.summary)
                putString("posterUrl", drama.posterUrl)
                putString("genre", drama.genre)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
