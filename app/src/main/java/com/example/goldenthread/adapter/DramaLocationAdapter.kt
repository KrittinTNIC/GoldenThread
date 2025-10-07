package com.example.goldenthread.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goldenthread.R
import com.example.goldenthread.ThreadFragment

class DramaLocationAdapter(
    private var items: List<ThreadFragment.LocationDramaItem>,
    private val listener: OnItemButtonClickListener? = null
) : RecyclerView.Adapter<DramaLocationAdapter.LocationViewHolder>() {

    interface OnItemButtonClickListener {
        fun onGoToDrama(item: ThreadFragment.LocationDramaItem)
        fun onGoToNextPoint(item: ThreadFragment.LocationDramaItem)
        fun onFavorite(item: ThreadFragment.LocationDramaItem)
    }

    inner class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderText: TextView = view.findViewById(R.id.orderText)
        val nameEn: TextView = view.findViewById(R.id.nameEnText)
        val nameTh: TextView = view.findViewById(R.id.nameThText)
        val address: TextView = view.findViewById(R.id.addressText)
        val titleEn: TextView = view.findViewById(R.id.titleEnText)
        val titleTh: TextView = view.findViewById(R.id.titleThText)
        val releaseYear: TextView = view.findViewById(R.id.releaseYearText)
        val sceneNotes: TextView = view.findViewById(R.id.sceneNotesText)
        val travelInfo: TextView = view.findViewById(R.id.travelInfoText)
        val btnGoToDrama: Button = view.findViewById(R.id.btnGoToDrama)
        val btnNextPoint: Button = view.findViewById(R.id.btnNextPoint)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.drama_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val item = items[position]
        holder.orderText.text = item.orderInTrip.toString()
        holder.nameEn.text = item.nameEn
        holder.nameTh.text = item.nameTh
        holder.address.text = item.address
        holder.titleEn.text = item.titleEn
        holder.titleTh.text = item.titleTh
        holder.releaseYear.text = "Year: ${item.releaseYear}"
        holder.sceneNotes.text = "Scene Note: ${item.sceneNotes}"
        holder.travelInfo.text = "Travel by car: ${item.carTravelMin} min"

        // Button click listeners
        holder.btnGoToDrama.setOnClickListener { listener?.onGoToDrama(item) }
        holder.btnNextPoint.setOnClickListener { listener?.onGoToNextPoint(item) }
        holder.btnFavorite.setOnClickListener { listener?.onFavorite(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ThreadFragment.LocationDramaItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
