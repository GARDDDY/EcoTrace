package com.gy.ecotrace.customs

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.widget.PopupWindowCompat
import com.gy.ecotrace.R

class CustomMultiChoiceSpinner(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val spinnerTextView: TextView = LayoutInflater.from(context).inflate(R.layout.widget_spinner_multichoice_selected_items, this, true)
        as TextView
    private val popupWindow: PopupWindow
    private val items = listOf("Item 1", "Item 2", "Item 3")
    private val selectedItems = mutableSetOf<String>()

    init {

        val popupView = LayoutInflater.from(context).inflate(R.layout.widget_custom_multichoice_spinner_layout_dropdown, null)
        val dropdownList = popupView.findViewById<LinearLayout>(R.id.lv_items)

        for (item in items) {
            val itemView = LayoutInflater.from(context).inflate(R.layout.widget_spinner_multichoice_item, null)
                as CheckedTextView
            itemView.text = item
            itemView.setOnClickListener {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item)
                } else {
                    selectedItems.add(item)
                }
                updateSpinnerText()
            }
            dropdownList.addView(itemView)
        }

        popupWindow = PopupWindow(popupView, spinnerTextView.width, LayoutParams.WRAP_CONTENT, true)

        spinnerTextView.setOnClickListener {
            if (popupWindow.isShowing) {
                popupWindow.dismiss()
            } else {
                PopupWindowCompat.showAsDropDown(popupWindow, spinnerTextView, 0, 0, Gravity.CENTER)
            }
        }
    }

    private fun updateSpinnerText() {
        spinnerTextView.text = if (selectedItems.isEmpty()) {
            "Select Items"
        } else {
            selectedItems.joinToString(", ")
        }
    }
}
