package com.neko.server

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.AttrRes
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.neko.v2ray.R

class PingChartBottomSheet : BottomSheetDialogFragment() {

    private lateinit var chart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chart = view.findViewById(R.id.lineChart)

        val serverName = arguments?.getString(ARG_SERVER_NAME) ?: return
        val pingHistory = PingHistoryManager(requireContext()).getHistory(serverName)

        val entries = pingHistory.mapIndexed { index, ping -> Entry(index.toFloat(), ping.toFloat()) }

        val dataSet = LineDataSet(entries, "Ping (ms)").apply {
            color = requireContext().getColorFromAttr(R.attr.colorThemeUwu)
            setDrawCircles(true)
            setDrawValues(false)
            lineWidth = 2f
        }

        val textColor = requireContext().getColorFromAttr(R.attr.colorThemeUwu)

        chart.apply {
            data = LineData(dataSet)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.textColor = textColor
            xAxis.textSize = 12f

            axisLeft.textColor = textColor
            axisLeft.textSize = 12f

            axisRight.isEnabled = false

            dataSet.valueTextColor = textColor // if drawValues is enabled

            description.isEnabled = false
            legend.isEnabled = false // set to true if you want legend, then apply legend.textColor

            animateX(500)
            invalidate()
        }
    }

    @ColorInt
    fun Context.getColorFromAttr(@AttrRes attrColor: Int): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(attrColor))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }

    companion object {
        private const val ARG_SERVER_NAME = "server"

        fun newInstance(serverName: String): PingChartBottomSheet {
            val fragment = PingChartBottomSheet()
            fragment.arguments = Bundle().apply {
                putString(ARG_SERVER_NAME, serverName)
            }
            return fragment
        }
    }
}
