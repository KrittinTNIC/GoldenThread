package com.example.goldenthread.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.goldenthread.databinding.ItemPosterBinding
import com.example.goldenthread.model.Drama

class PosterAdapter(
    private val context: Context,
    private val dramaList: List<Drama>,
    private val onDramaClick: ((Drama) -> Unit)? = null
) : RecyclerView.Adapter<PosterAdapter.PosterViewHolder>() {

    inner class PosterViewHolder(val binding: ItemPosterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(context), parent, false)
        return PosterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        val drama = dramaList[position]

        Glide.with(context)
            .load(drama.posterUrl)
            .placeholder(com.example.goldenthread.R.drawable.placeholder_poster)
            .error(com.example.goldenthread.R.drawable.placeholder_poster)
            .into(holder.binding.posterImage)

        holder.itemView.setOnClickListener {
            onDramaClick?.invoke(drama)
        }
    }

    override fun getItemCount(): Int = dramaList.size
}
