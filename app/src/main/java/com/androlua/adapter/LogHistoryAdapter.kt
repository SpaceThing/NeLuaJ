package com.androlua.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.androlua.LuaActivity
import com.google.android.material.R as MaterialR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogHistoryAdapter(
    private val context: Context,
    private val logs: List<String?>
) : BaseAdapter() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    override fun getCount(): Int = logs.size

    override fun getItem(position: Int): String? = logs[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textView = (convertView as? TextView) ?: TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(com.google.android.material.color.MaterialColors.getColor(
                this, MaterialR.attr.colorOnSurface
            ))
            setPadding(dp(20), dp(12), dp(20), dp(12))
            gravity = Gravity.CENTER_VERTICAL
            typeface = Typeface.MONOSPACE
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        val timestamp = LuaActivity.getLogTimestamp(position)
        val formattedTime = timestamp?.let { timeFormat.format(Date(it)) }
        textView.text = if (formattedTime == null) {
            getItem(position).orEmpty()
        } else {
            SpannableStringBuilder().apply {
                append(formattedTime)
                setSpan(
                    ForegroundColorSpan(
                        com.google.android.material.color.MaterialColors.getColor(
                            textView, MaterialR.attr.colorOnSurfaceVariant
                        )
                    ),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append('\n')
                append(getItem(position).orEmpty())
            }
        }
        return textView
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
