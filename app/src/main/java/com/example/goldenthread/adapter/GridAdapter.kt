package com.example.goldenthread.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.goldenthread.R
import com.example.goldenthread.databinding.ItemGridBinding
import com.example.goldenthread.model.Drama

class GridAdapter(
    private val context: Context,
    private val dramaList: List<Drama>,
    private val onDramaClick: (Drama) -> Unit
) : RecyclerView.Adapter<GridAdapter.GridViewHolder>() {

    inner class GridViewHolder(val binding: ItemGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemGridBinding.inflate(LayoutInflater.from(context), parent, false)
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val drama = dramaList[position]

        // ✅ Load smaller poster for grid
        Glide.with(context)
            .load(drama.posterUrl)
            .placeholder(R.drawable.placeholder_poster)
            .error(R.drawable.placeholder_poster)
            .into(holder.binding.gridImage)

        // ✅ Handle item click
        holder.itemView.setOnClickListener {
            onDramaClick(drama)
        }
    }

    override fun getItemCount(): Int = dramaList.size
}
