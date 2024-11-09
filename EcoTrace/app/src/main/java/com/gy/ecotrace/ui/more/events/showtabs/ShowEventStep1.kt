package com.gy.ecotrace.ui.more.events.showtabs

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.EventTag
import com.gy.ecotrace.customs.QrCache
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.ui.more.events.ShowEventMembersActivity
import com.gy.ecotrace.ui.more.friends.PersonalShareActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ShowEventStep1 : Fragment() {

    private val sharedViewModel: ShowEventViewModel by activityViewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_layout_event__show_step_1, container, false)

    private fun members(value: Int): String = when {
        value % 100 in 11..14 -> "участников"
        value % 10 == 1 -> "участник"
        value % 10 in 2..4 -> "участника"
        else -> "участников"
    }

    private fun day(iso: String?): String = try {
        Instant.parse(iso).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.getDefault()))
    } catch (e: Exception) { "—" }

    private fun avatarUrl(key: String?): String? =
        key?.let { "${BuildConfig.SERVER_API_URI}uploads/users/$it" }

    private fun buildTagChip(tag: EventTag): Chip = Chip(requireContext()).apply {
        text = tag.name
        isCheckable = false
        isClickable = false
        isFocusable = false
        tag.colorFg?.let { runCatching { setTextColor(Color.parseColor(it)) } }
        tag.colorBg?.let { runCatching { chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor(it)) } }
    }

    private fun reapplyJoinBtn(joinButton: Button, member: Boolean) {
        if (member) {
            joinButton.text = "В мероприятии"
            joinButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
            joinButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dirt_white))
            joinButton.setOnClickListener {
                android.app.AlertDialog.Builder(requireActivity())
                    .setTitle("Выход из мероприятия")
                    .setMessage("Вы действительно хотите покинуть это мероприятие?")
                    .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                        sharedViewModel.leaveEvent { reapplyJoinBtn(joinButton, false) }
                    }
                    .setNegativeButton(getString(R.string.cancel)) { d, _ -> d.dismiss() }
                    .show()
            }
        } else {
            joinButton.text = "Присоединиться"
            joinButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.dirt_white))
            joinButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
            joinButton.setOnClickListener {
                sharedViewModel.joinEvent { reapplyJoinBtn(joinButton, true) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val joinBtn = view.findViewById<Button>(R.id.joinEvent)
        val statusView = view.findViewById<TextView>(R.id.eventStatusShort)
        val participantQr = view.findViewById<ImageView>(R.id.eventParticipantQr)
        val tagsLayout = view.findViewById<ChipGroup>(R.id.eventTagsLayout)

        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.refreshEventInfo)
        refreshLayout.setColorSchemeResources(R.color.ok_green)
        refreshLayout.setOnRefreshListener {
            sharedViewModel.getEvent()
            sharedViewModel.getMembersPreview()
            refreshLayout.isRefreshing = false
        }

        sharedViewModel.event.observe(viewLifecycleOwner, Observer { event ->
            event ?: return@Observer

            view.findViewById<TextView>(R.id.eventName).text = event.name
            view.findViewById<TextView>(R.id.eventAbout).text = event.about
            view.findViewById<TextView>(R.id.eventCountMembers).text =
                "${event.membersCount} ${members(event.membersCount)}"

            tagsLayout.removeAllViews()
            for (tag in event.filters) tagsLayout.addView(buildTagChip(tag))

            val (statusText, statusColor, dateSource) = when (event.timing) {
                0 -> Triple("Начнётся", R.color.silver, event.startsAt)
                1 -> Triple("Идёт", R.color.ok_green, event.endsAt)
                else -> Triple("Закончилось", R.color.red_no, event.endsAt)
            }
            statusView.text = statusText
            statusView.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
            view.findViewById<TextView>(R.id.eventStartDate).text = day(dateSource)

            joinBtn.visibility = if (event.timing > 0 || sharedViewModel.isCreator) View.GONE else View.VISIBLE
            reapplyJoinBtn(joinBtn, event.myRole != null)

            val showQr = event.timing == 1 && event.myRole != null && !event.myValidated && !sharedViewModel.isModer
            participantQr.visibility = if (showQr) View.VISIBLE else View.GONE
            if (showQr) {
                val content = "eventparticipanrvalidation?uid=${tokenStore.userId}&eid=${sharedViewModel.currentEvent}"
                val fg = ContextCompat.getColor(requireContext(), R.color.ok_green)
                val bg = ContextCompat.getColor(requireContext(), R.color.dirt2_white)
                viewLifecycleOwner.lifecycleScope.launch {
                    val file = QrCache.ensure(requireContext().applicationContext, content, fg, bg)
                    val bmp = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path) }
                    if (bmp != null) participantQr.setImageBitmap(bmp)
                }
                participantQr.setOnClickListener {
                    val qrActivity = Intent(requireActivity(), PersonalShareActivity::class.java)
                    qrActivity.putExtra("valid", true)
                    qrActivity.putExtra("link", content)
                    qrActivity.putExtra("image", event.imageKey)
                    qrActivity.putExtra("imageF", "events")
                    qrActivity.putExtra("name", event.name)
                    startActivity(qrActivity)
                }
            }
        })

        view.findViewById<TextView>(R.id.showAllMembers).setOnClickListener {
            startActivity(Intent(requireActivity(), ShowEventMembersActivity::class.java))
        }

        val avatarsLayout = view.findViewById<LinearLayout>(R.id.someMembersAvatars)
        val avatarSizePx = (70 * resources.displayMetrics.density).toInt()
        sharedViewModel.getMembersPreview()
        sharedViewModel.membersPreview.observe(viewLifecycleOwner, Observer { previews ->
            avatarsLayout.removeAllViews()
            for (member in previews) {
                val avatarView = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(avatarSizePx, avatarSizePx)
                }
                Glide.with(requireActivity()).load(avatarUrl(member.avatarKey)).circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .into(avatarView)
                avatarsLayout.addView(avatarView)
            }
        })
    }
}
