package com.example.goldenthread.util

import android.content.Context
import android.content.SharedPreferences
import com.example.goldenthread.model.Drama
import com.google.gson.Gson

object Favoritemanager {
    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorites_list"

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    /**
     * Must be called once (app start). Best place: MainActivity.onCreate() or your Application class.
     */
    fun init(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun prefsReady(): Boolean = ::sharedPreferences.isInitialized

    fun getFavorites(): MutableList<Drama> {
        if (!prefsReady()) return mutableListOf()
        val json = sharedPreferences.getString(KEY_FAVORITES, null) ?: return mutableListOf()
        return try {
            val arr = gson.fromJson(json, Array<Drama>::class.java)
            arr?.toMutableList() ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun isFavorite(drama: Drama): Boolean {
        if (!prefsReady()) return false
        return getFavorites().any { it.dramaId == drama.dramaId }
    }

    fun addFavorite(drama: Drama) {
        if (!prefsReady()) return
        val favorites = getFavorites()
        if (favorites.none { it.dramaId == drama.dramaId }) {
            favorites.add(drama)
            saveFavorites(favorites)
        }
    }

    fun removeFavorite(drama: Drama) {
        if (!prefsReady()) return
        val favorites = getFavorites()
        val changed = favorites.removeAll { it.dramaId == drama.dramaId }
        if (changed) saveFavorites(favorites)
    }

    private fun saveFavorites(favorites: List<Drama>) {
        if (!prefsReady()) return
        val json = gson.toJson(favorites)
        sharedPreferences.edit().putString(KEY_FAVORITES, json).apply()
    }
}
