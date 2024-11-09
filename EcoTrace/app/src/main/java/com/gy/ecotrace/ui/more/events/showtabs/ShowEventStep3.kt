package com.gy.ecotrace.ui.more.events.showtabs

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.gy.ecotrace.R
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShowEventStep3 : Fragment() {

    private val sharedViewModel: ShowEventViewModel by activityViewModels()
    private val relationships: HashMap<MapObject, Pair<String, String?>> = hashMapOf()

    private lateinit var mapView: MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activitylayout_show_event_step3, container, false)
        mapView = view.findViewById(R.id.mapView)
        return view
    }

    private fun parseTime(timeString: String): String {
        val end = mutableListOf<String>()
        for (timeStamp in timeString.split('_')) {
            val timestampInMillis = timeStamp.toLong() * 1000
            val date = Date(timestampInMillis)
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            end.add(sdf.format(date))
        }
        return "${end[0]} - ${end[1]}"
    }


    private val dotTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is PlacemarkMapObject) {
            val coordData = relationships[mapObject]!!
            val additionalText = when(coordData.second) {
                null -> ""
                else -> ". Эта точка связана со временным событием ${parseTime(coordData.second!!)}"
            }
            Toast.makeText(requireActivity(), "${coordData.first}$additionalText", Toast.LENGTH_LONG).show()
        }
        true
    }

    private val circleTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is CircleMapObject) {
            val coordData = relationships[mapObject]!!
            val additionalText = when(coordData.second) {
                null -> ""
                else -> ". Этот радиус связан со временным событием ${parseTime(coordData.second!!)}"
            }
            Toast.makeText(requireActivity(), "${coordData.first}$additionalText", Toast.LENGTH_LONG).show()
        }
        true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val marksCollection = mapView.map.mapObjects.addCollection()

        sharedViewModel.getCoords()
        sharedViewModel.eventCoords.observe(viewLifecycleOwner, Observer {
            it?.let {
                for (coord in it) {
                    when (coord.objectType) {
                        2 -> {
                            val placeMark = marksCollection.addPlacemark(coord.objectCenter)
                            placeMark.setIcon(
                                ImageProvider.fromResource(context,
                                    com.yandex.maps.mobile.R.drawable.search_layer_pin_selected_default))
                            placeMark.setIconStyle(IconStyle().apply {
                                anchor = PointF(0.5f, 1f)
                            })
                            relationships[placeMark] = Pair(coord.objectName, coord.objectRelation)
                            placeMark.addTapListener(dotTapListener)
                        }

                        0 -> {
                            val addedCircle =
                                marksCollection.addCircle(Circle(coord.objectCenter, coord.circleRadius!!),
                                    Color.parseColor(coord.strokeColor), 2f, Color.parseColor(coord.fillColor)) // stroke, strokeWidth, fill
                            relationships[addedCircle] = Pair(coord.objectName, coord.objectRelation)
                            addedCircle.addTapListener(circleTapListener)
                        }
                    }
                }
            }
        })
    }
}