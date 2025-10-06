package com.example.goldenthread.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.goldenthread.R
import com.example.goldenthread.databinding.FragmentMovieDetailBinding
import com.example.goldenthread.model.Drama
import com.example.goldenthread.util.Favoritemanager

class MovieDetailFragment : Fragment() {

    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private var currentDrama: Drama? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ✅ Read arguments and build Drama object
        val drama = Drama(
            dramaId = arguments?.getString("dramaId") ?: "",
            titleEn = arguments?.getString("titleEn") ?: "Untitled",
            titleTh = arguments?.getString("titleTh") ?: "",
            releaseYear = arguments?.getString("releaseYear")?.toIntOrNull() ?: 0,
            duration = arguments?.getString("duration") ?: "-",
            summary = arguments?.getString("summary") ?: "No description available.",
            posterUrl = arguments?.getString("posterUrl") ?: "",
            genre = arguments?.getString("genre") ?: "Unknown"
        )
        currentDrama = drama

        // Load poster
        Glide.with(this)
            .load(drama.posterUrl)
            .placeholder(R.drawable.placeholder_poster)
            .error(R.drawable.placeholder_poster)
            .into(binding.ivMoviePoster)

        // Set text fields
        binding.tvMovieTitle.text = drama.titleEn.ifBlank { drama.titleTh }
        binding.tvMovieDescription.text = drama.summary
        binding.tvYear.text = drama.releaseYear.toString()
        binding.tvDuration.text = drama.duration
        binding.tvGenre.text = drama.genre

        // Update favorite button state
        updateFavoriteButton()

        // Toggle favorite when clicked
        binding.favoriteButton.setOnClickListener {
            currentDrama?.let {
                if (Favoritemanager.isFavorite(it)) {
                    Favoritemanager.removeFavorite(it)
                } else {
                    Favoritemanager.addFavorite(it)
                }
                updateFavoriteButton()
            }
        }
    }

    private fun updateFavoriteButton() {
        currentDrama?.let {
            if (Favoritemanager.isFavorite(it)) {
                binding.favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
            } else {
                binding.favoriteButton.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
