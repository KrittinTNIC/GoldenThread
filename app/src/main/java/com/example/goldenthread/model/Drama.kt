package com.example.goldenthread.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Drama(
    val dramaId: String,
    val titleEn: String,
    val titleTh: String,
    val releaseYear: Int,
    val duration: String,
    val summary: String,
    val posterUrl: String,
    val genre: String
) : Parcelable
