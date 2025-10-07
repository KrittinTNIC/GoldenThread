package com.example.goldenthread.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goldenthread.R
import com.example.goldenthread.ThreadFragment

class DramaLocationAdapter(
    private var items: List<ThreadFragment.LocationDramaItem>
) : RecyclerView.Adapter<DramaLocationAdapter.LocationViewHolder>() {

    inner class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameEn: TextView = view.findViewById(R.id.nameEnText)
        val nameTh: TextView = view.findViewById(R.id.nameThText)
        val address: TextView = view.findViewById(R.id.addressText)
        val titleEn: TextView = view.findViewById(R.id.titleEnText)
        val titleTh: TextView = view.findViewById(R.id.titleThText)
        val releaseYear: TextView = view.findViewById(R.id.releaseYearText)
        val sceneNotes: TextView = view.findViewById(R.id.sceneNotesText)
        val travelInfo: TextView = view.findViewById(R.id.travelInfoText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.drama_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val item = items[position]
        holder.nameEn.text = item.nameEn
        holder.nameTh.text = item.nameTh
        holder.address.text = item.address
        holder.titleEn.text = item.titleEn
        holder.titleTh.text = item.titleTh
        holder.releaseYear.text = "Year: ${item.releaseYear}"
        holder.sceneNotes.text = "Scene Note: ${item.sceneNotes}"
        holder.travelInfo.text = "Order: ${item.orderInTrip} | Travel: ${item.carTravelMin} min"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ThreadFragment.LocationDramaItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
