package com.example.guiherme.customtileslab

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider

class OfflineTilesProvider(private val context: Context,
                           private val width: Int = 256,
                           private val height: Int = 256) : TileProvider {
    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val file = "${zoom}_${x}_$y.png"
        val input = context.openFileInput(file)

        Log.d("Tile", "$file - ${input.available()}")

        val bytes = ByteArray(input.available())
        input.read(bytes)
        input.close()
        return Tile(width, height, bytes)
    }
}