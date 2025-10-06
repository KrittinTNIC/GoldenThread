package com.example.goldenthread.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.goldenthread.R
import com.example.goldenthread.model.Drama

class DramaDropdownAdapter(context: Context, private val allDramas: List<Drama>) :
    ArrayAdapter<Drama>(context, 0, ArrayList(allDramas)) {

    private var filteredList: List<Drama> = allDramas

    override fun getCount(): Int = filteredList.size
    override fun getItem(position: Int): Drama? = filteredList.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown_drama, parent, false)

        val drama = getItem(position)
        val ivPoster = view.findViewById<ImageView>(R.id.ivPoster)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)

        if (drama != null) {
            tvTitle.text = drama.titleEn.ifBlank { drama.titleTh }
            Glide.with(context)
                .load(drama.posterUrl)
                .placeholder(R.drawable.placeholder_poster)
                .error(R.drawable.placeholder_poster)
                .into(ivPoster)
        } else {
            tvTitle.text = ""
            ivPoster.setImageResource(R.drawable.placeholder_poster)
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim() ?: ""
                filteredList = if (query.isEmpty()) {
                    allDramas
                } else {
                    allDramas.filter {
                        it.titleEn.contains(query, ignoreCase = true) ||
                                it.titleTh.contains(query, ignoreCase = true)
                    }
                }
                return FilterResults().apply { values = filteredList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = (results?.values as? List<Drama>) ?: emptyList()
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                // Show title in the AutoCompleteTextView when an item is selected
                return (resultValue as? Drama)?.titleEn?.takeIf { it.isNotBlank() }
                    ?: (resultValue as? Drama)?.titleTh ?: ""
            }
        }
    }
}
