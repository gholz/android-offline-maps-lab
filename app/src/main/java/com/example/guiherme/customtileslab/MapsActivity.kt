package com.example.guiherme.customtileslab

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_maps.*
import okhttp3.OkHttpClient
import okhttp3.Request

class MapsActivity : AppCompatActivity() {

    companion object {
        private val MEETING_POINT = LatLng(41.8896656, 12.4902269)
        private const val DEFAULT_ZOOM = 16f
    }

    private val bounds = MEETING_POINT.toBounds(2000)
    private var map: GoogleMap? = null
    private var overlay: TileOverlay? = null
    private var offline = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        initMap()
        initButtons()
    }

    private fun initMap() {
        val fragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment
        fragment.getMapAsync {
            configMap(it)
        }
    }

    private fun initButtons() {
        buttonOnlineOffline.isEnabled = isDownloaded()
        buttonOnlineOffline.setOnClickListener {
            offline = !offline
            if (offline) {
                displayOfflineMap()
            } else {
                displayOnlineMap()
            }
        }

        buttonDownloadClear.setText(if (isDownloaded()) R.string.button_clear else R.string.button_download)
        buttonDownloadClear.setOnClickListener {
            if (isDownloaded()) {
                clearOfflineCache()
                buttonDownloadClear.setText(R.string.button_download)
            } else {
                downloadOfflineMap()
                buttonDownloadClear.isEnabled = false
            }
        }

        buttonCenter.setOnClickListener {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(MEETING_POINT, DEFAULT_ZOOM))
        }
    }

    private fun configMap(map: GoogleMap) {
        map.uiSettings.isZoomGesturesEnabled = false
        map.setLatLngBoundsForCameraTarget(bounds)
        map.addMarker(MarkerOptions().position(MEETING_POINT).title("Meeting point"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(MEETING_POINT, DEFAULT_ZOOM))
        this.map = map
    }

    private fun displayOfflineMap() {
        map?.let {
            it.mapType = GoogleMap.MAP_TYPE_NONE
            overlay = it.addTileOverlay(TileOverlayOptions()
                    .tileProvider(OfflineTilesProvider(this)))
            buttonOnlineOffline.setText(R.string.button_online)
        }
    }

    private fun displayOnlineMap() {
        map?.let {
            it.mapType = GoogleMap.MAP_TYPE_NORMAL
            overlay?.remove()
            overlay = null
            buttonOnlineOffline.setText(R.string.button_offline)
        }
    }

    private fun downloadOfflineMap() {
        val client = OkHttpClient.Builder().build()
        Flowable.fromIterable(getTileUrls())
                .map {
                    client.newCall(Request.Builder().url(it).build()).execute()
                }
                .filter {
                    it.isSuccessful
                }
                .map {
                    val fileName = it.request().url().encodedPath()
                            .replace("/", "_")
                            .substring(1)
                    val output = openFileOutput(fileName, Context.MODE_PRIVATE)
                    output.write(it.body()?.bytes())
                    output.close()
                    return@map fileName
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("Tile", "Downloaded $it")
                }, {
                    Log.e("Tile", "error", it)
                }, {
                    Log.d("Tile", "Complete")
                    buttonOnlineOffline.isEnabled = true
                    buttonDownloadClear.isEnabled = true
                    buttonDownloadClear.setText(R.string.button_clear)
                })
    }

    private fun clearOfflineCache() {
        getTileFileNames().forEach {
            Log.d("Tile", "Deleting file $it")
            deleteFile(it)
        }
        displayOnlineMap()
        buttonOnlineOffline.isEnabled = false
    }

    private fun isDownloaded(): Boolean = fileList().contains(getTileFileNames().first())

    private fun getTileUrls(): List<String> =
            getTileNames("http://a.tile.openstreetmap.org/%d/%d/%d.png")

    private fun getTileFileNames(): List<String> = getTileNames("%d_%d_%d.png")

    private fun getTileNames(format: String): List<String> {
        val northEastTile = bounds.northeast.toTileCoordinates(DEFAULT_ZOOM.toInt())
        val southWestTile = bounds.southwest.toTileCoordinates(DEFAULT_ZOOM.toInt())

        val startX = Math.min(northEastTile.first, southWestTile.first) - 1
        val startY = Math.min(northEastTile.second, southWestTile.second) - 1
        val endX = Math.max(northEastTile.first, southWestTile.first)
        val endY = Math.max(northEastTile.second, southWestTile.second) + 1

        val names = mutableListOf<String>()
        for (y in startY..endY) {
            (startX..endX).mapTo(names) {
                format.format(DEFAULT_ZOOM.toInt(), it, y)
            }
        }
        return names.toList()
    }
}
