package com.gy.ecotrace.customs

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.TextView
import android.widget.Toast
import com.gy.ecotrace.R
import org.w3c.dom.Text
import kotlin.random.Random

class SpinnerMultiChoice(context: Context, items: List<String>) :
    ArrayAdapter<String>(context, 0, items) {

    private val selectedItems = mutableMapOf<String, Boolean>().apply {
        items.forEach { this[it] = false }
        this[items[0]] = true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.widget_spinner_multichoice_selected_items, parent, false)
        val textView = view as TextView
        val selectedCount = selectedItems.values.count { it }
        val checkedText = when (selectedCount) {
            0 -> "Ничего не выбрано"
            selectedItems.size -> "Выбрано все"
            else -> selectedItems.filterValues { it }.keys.joinToString(", ")
        }
        textView.text = checkedText
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val checkedTextView = (convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.widget_spinner_multichoice_item, parent, false))
                as TextView
        val item = getItem(position) ?: ""

        checkedTextView.text = item

        checkedTextView.setOnClickListener {
            if (selectedItems[item] == true &&
                selectedItems.filterValues { it }.keys.size == 1) {
                Toast.makeText(context,
                    "Нужно оставить хотя бы что-то одно!",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedItems[item] = !(selectedItems[item] ?: false)
            notifyDataSetChanged()
        }
        return checkedTextView
    }

    fun getUnSelectedItems(): List<String> {
        return selectedItems.filter { !it.value }.keys.toList()
    }
}

