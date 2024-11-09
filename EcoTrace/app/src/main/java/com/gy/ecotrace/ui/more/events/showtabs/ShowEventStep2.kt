package com.gy.ecotrace.ui.more.events.showtabs

import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.EventZone
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ShowEventStep2 : Fragment() {

    private val sharedViewModel: ShowEventViewModel by activityViewModels()
    private val tooltips = HashMap<MapObject, String>()
    private val points = HashMap<MapObject, Point>()
    private val scheduleById = HashMap<Long, String>()
    private val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault())

    private lateinit var mapView: MapView

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
        val view = inflater.inflate(R.layout.activity_layout_event__show_step_2, container, false)
        mapView = view.findViewById(R.id.mapView)
        return view
    }

    private fun format(iso: String?): String = try {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).format(fmt)
    } catch (e: Exception) { "—" }

    private val tapListener = MapObjectTapListener { mapObject, _ ->
        val tooltip = tooltips[mapObject] ?: ""
        val point = points[mapObject]
        if (point != null) {
            AlertDialog.Builder(requireActivity())
                .setMessage(tooltip.ifBlank { "Метка" })
                .setPositiveButton("Открыть на карте") { _, _ ->
                    val label = Uri.encode(tooltip.substringBefore(".").take(50).ifBlank { "Точка" })
                    val lat = point.latitude
                    val lon = point.longitude
                    val appIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("yandexmaps://maps.yandex.ru/?ll=$lon,$lat&z=16&pt=$lon,$lat,pm2rdm~"))
                    val webIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://yandex.ru/maps/?ll=$lon,$lat&z=16&pt=$lon,$lat,pm2rdm~&text=$label"))
                    try { startActivity(appIntent) }
                    catch (_: Exception) { startActivity(webIntent) }
                }
                .setNegativeButton("Закрыть", null)
                .show()
        } else if (tooltip.isNotBlank()) {
            AlertDialog.Builder(requireActivity())
                .setMessage(tooltip)
                .setPositiveButton("Закрыть", null)
                .show()
        }
        true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val marksCollection = mapView.map.mapObjects.addCollection()

        sharedViewModel.getSchedule()
        sharedViewModel.schedule.observe(viewLifecycleOwner, Observer { items ->
            scheduleById.clear()
            items.forEach { scheduleById[it.id] = "${format(it.startsAt)} - ${format(it.endsAt)}" }
        })

        sharedViewModel.getZones()
        sharedViewModel.zones.observe(viewLifecycleOwner, Observer { zones ->
            marksCollection.clear()
            tooltips.clear()
            points.clear()
            for (zone in zones) render(marksCollection, zone)
            zones.firstOrNull()?.let { first ->
                mapView.map.move(
                    CameraPosition(Point(first.latitude, first.longitude), 13.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 0.6f), null
                )
            }
        })
    }

    private fun render(collection: com.yandex.mapkit.map.MapObjectCollection, zone: EventZone) {
        val point = Point(zone.latitude, zone.longitude)
        val linked = zone.scheduleItemId?.let { scheduleById[it] }
        val tooltip = buildString {
            append(zone.label ?: "")
            if (linked != null) append(". Связано со временным событием $linked")
        }

        val mapObject: MapObject = if (zone.isZone) {
            collection.addCircle(
                Circle(point, (zone.radiusM ?: 100).toFloat()),
                runCatching { Color.parseColor(zone.colorStroke) }.getOrDefault(Color.BLUE), 2f,
                runCatching { Color.parseColor(zone.colorFill) }.getOrDefault(Color.argb(60, 0, 0, 255))
            ).also { it.addTapListener(tapListener) }
        } else {
            collection.addPlacemark(point).also {
                it.setIcon(ImageProvider.fromResource(context, com.yandex.maps.mobile.R.drawable.search_layer_pin_selected_default))
                it.setIconStyle(IconStyle().apply { anchor = PointF(0.5f, 1f) })
                it.addTapListener(tapListener)
            }
        }
        tooltips[mapObject] = tooltip
        points[mapObject] = point
    }
}
