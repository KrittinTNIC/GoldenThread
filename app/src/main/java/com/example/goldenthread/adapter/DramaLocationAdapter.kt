package com.example.goldenthread.adapter

import com.example.goldenthread.ThreadFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goldenthread.R

class DramaLocationAdapter(
    private var items: List<ThreadFragment.LocationItem>
) : RecyclerView.Adapter<DramaLocationAdapter.LocationViewHolder>() {

    inner class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTh: TextView = view.findViewById(R.id.nameThText)
        val nameEn: TextView = view.findViewById(R.id.nameEnText)
        val address: TextView = view.findViewById(R.id.addressText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.drama_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val item = items[position]
        holder.nameTh.text = item.nameTh
        holder.nameEn.text = item.nameEn
        holder.address.text = item.address
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ThreadFragment.LocationItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
