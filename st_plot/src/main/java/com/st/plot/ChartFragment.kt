/*
 * Copyright (c) 2019  STMicroelectronics – All rights reserved
 * The STMicroelectronics corporate logo is a trademark of STMicroelectronics
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name nor trademarks of STMicroelectronics International N.V. nor any other
 *   STMicroelectronics company nor the names of its contributors may be used to endorse or
 *   promote products derived from this software without specific prior written permission.
 *
 * - All of the icons, pictures, logos and other images that are provided with the source code
 *   in a directory whose title begins with st_images may only be used for internal purposes and
 *   shall not be redistributed to any third party or modified in any way.
 *
 * - Any redistributions in binary form shall not include the capability to display any of the
 *   icons, pictures, logos and other images that are provided with the source code in a directory
 *   whose title begins with st_images.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package com.st.plot

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsSpinner
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@AndroidEntryPoint
class ChartFragment : Fragment() {

    companion object {
        private const val TAG = "ChartFragment"
    }

    private lateinit var mPlot: LineChart
    private lateinit var mYAxisLabel: TextView
    private lateinit var mFeatureSelector: AbsSpinner
    private lateinit var mStartPlotButton: MaterialButton
    private lateinit var mFeatureValue: TextView
    private lateinit var buttonViewSerialConsole: Button
    private lateinit var scrollViewSerialConsole: ScrollView
    private lateinit var textViewSerialConsole: TextView

    private val dataViewModel: PlotViewModel by viewModels({ requireParentFragment() })
    private val settingsViewModel: PlotSettingsViewModel by viewModels({ requireParentFragment() })

    private lateinit var mLineColors: IntArray

    private var mFirstTimeRestore: Boolean = true
    private var serialConsoleIsRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.chart_fragment, container, false)
        mPlot = view.findViewById(R.id.plotFeature_chart)
        mYAxisLabel = view.findViewById(R.id.plotFeature_yLabel)
        mFeatureSelector = view.findViewById(R.id.plotFeature_featureSelector)
        initializePlot(mPlot)
        mLineColors = resources.getIntArray(R.array.dataSetColor)
        mFeatureValue = view.findViewById(R.id.plotFeature_featureValue)
        mStartPlotButton = view.findViewById(R.id.plotFeature_startPlotButton)
        mStartPlotButton.setOnClickListener { onStartStopPlotButtonClicked() }
        mStartPlotButton.setOnLongClickListener { onResumePausePlotButtonClicked() }
        mFeatureSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(
                p0: AdapterView<*>?,
                p1: View?,
                selectedIndex: Int,
                p3: Long
            ) {
                settingsViewModel.onSelectedIndex(selectedIndex)
            }
        }

        buttonViewSerialConsole = view.findViewById(R.id.plotFeature_demo_button_serial_console)
        buttonViewSerialConsole.setOnClickListener { startStopSerialConsole() }
        if(settingsViewModel.isExpert) {
            buttonViewSerialConsole.visibility=View.VISIBLE
        }

        scrollViewSerialConsole = view.findViewById(R.id.plotFeature_serial_console_scrollview)
        textViewSerialConsole = view.findViewById(R.id.plotFeature_serial_console_text)

        return view
    }

    private fun startStopSerialConsole() {
        if(serialConsoleIsRunning) {
            serialConsoleIsRunning = false
            scrollViewSerialConsole.visibility = View.GONE
            dataViewModel.stopReceiveDebugMessage()
            textViewSerialConsole.text="Serial Console Output:\n"
        } else {
            serialConsoleIsRunning = true
            scrollViewSerialConsole.visibility = View.VISIBLE
            dataViewModel.startReceiveDebugMessage()
        }
    }

    private fun initializePlot(chart: LineChart) {
        //hide chart description
        chart.description.isEnabled = true
        chart.description.text = ""

        // isEnable touch gestures
        chart.setTouchEnabled(true)

        // isEnable scaling and dragging
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true)

        val xl = chart.xAxis
        xl.position = XAxis.XAxisPosition.BOTTOM
        xl.setDrawLabels(false)
        xl.setDrawGridLines(false)
        xl.setAvoidFirstLastClipping(true)

        chart.axisRight.isEnabled = false
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = resources.getColor(com.st.ui.R.color.labelPlotContrast)

        chart.setNoDataText(resources.getString(R.string.plotFeature_noData))
        chart.setNoDataTextColor(resources.getColor(com.st.ui.R.color.colorAccent))
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        chart.legend.textColor = resources.getColor(com.st.ui.R.color.labelPlotContrast)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachPlotSettingsViewModel(savedInstanceState)
        attachDataViewModel()
    }

    private fun buildLineDataSet(name: String, color: Int): LineDataSet {
        return LineDataSet(LinkedList(), name).apply {
            axisDependency = YAxis.AxisDependency.LEFT
            setDrawCircles(false)
            setDrawValues(false)
            setColor(color)
            setDrawHighlightIndicators(false)
        }
    }

    private fun attachPlotSettingsViewModel(savedInstanceState: Bundle?) {
        settingsViewModel.legendItems.observe(viewLifecycleOwner) { items ->
            val lineData = items.mapIndexed { i, name ->
                buildLineDataSet(name, mLineColors[i % mLineColors.size])
            }
            restorePlotData(lineData, savedInstanceState)
            mPlot.data = LineData(lineData)
            mPlot.description.text = ""
            //Enable if we want to show the Legend only if we are more that one line
            //mPlot.legend.isEnabled = lineData.size != 1
        }
        settingsViewModel.yAxisLabel.observe(viewLifecycleOwner) { yLabel ->
            mYAxisLabel.text = yLabel
        }
        settingsViewModel.supportedFeature.observe(viewLifecycleOwner) { featureList ->
            mFeatureSelector.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                featureList
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        settingsViewModel.selectedFeatureIndex.observe(viewLifecycleOwner) { index ->
            mFeatureSelector.setSelection(index)
        }
        settingsViewModel.selectedFeature.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { selectedFeature ->
                dataViewModel.resetPlot()

                if (dataViewModel.isPlotting.value == true) {
                    settingsViewModel.startPlotSelectedFeature()
                    dataViewModel.startPlotFeature(selectedFeature)
                }
            })

        settingsViewModel.plotBoundary.observe(viewLifecycleOwner) { boundary ->
            val yAxis = mPlot.axisLeft
            boundary.nLabels?.let {
                yAxis.labelCount = it
            }

            if (boundary.enableAutoScale) {
                mPlot.isAutoScaleMinMaxEnabled = true
                yAxis.apply {
                    resetAxisMaximum()
                    resetAxisMinimum()
                }
            } else {
                mPlot.isAutoScaleMinMaxEnabled = false
                yAxis.apply {
                    if (boundary.max != null)
                        axisMaximum = boundary.max
                    if (boundary.min != null)
                        axisMinimum = boundary.min
                }
            }
            mPlot.invalidate()
        }

    }

    private fun attachDataViewModel() {
        dataViewModel.lastPlotData.observe(viewLifecycleOwner) { lastData ->
            //if we have a number of data different from the number of legends
            //rebuild the plot
            if (lastData?.y?.size != settingsViewModel.legendItems.value?.size) {
                settingsViewModel.startPlotSelectedFeature()
            }
            mPlot.data?.let { lineData ->
                lastData?.y?.forEachIndexed { index, value ->
                    lineData.addEntry(Entry(lastData.x.toFloat(), value), index)
                }
                lineData.removeEntryOlderThan(settingsViewModel.plotDuration.value)
                lineData.notifyDataChanged()
                mPlot.notifyDataSetChanged()
                mPlot.invalidate()
            }
        }

        dataViewModel.lastDataDescription.observe(viewLifecycleOwner) { description ->
            //mPlot.description.text = description
            mFeatureValue.text = description
        }
        dataViewModel.isPlotting.observe(viewLifecycleOwner) { isPlotting ->
            if (isPlotting) {
                mStartPlotButton.setIconResource(com.st.ui.R.drawable.ic_stop)
            } else {
                mStartPlotButton.setIconResource(com.st.ui.R.drawable.ic_play)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataViewModel.debugMessages.collect {
                    if (!it.isNullOrEmpty()) {
                        val string = textViewSerialConsole.text.toString() + it
                        textViewSerialConsole.text = string
                        scrollViewSerialConsole.post { scrollViewSerialConsole.fullScroll(View.FOCUS_DOWN) }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "state - save")
        if (::mPlot.isInitialized) {
            mFirstTimeRestore = true
            mPlot.data?.dataSets?.forEachIndexed { index, lineData ->
                saveLineData(index, lineData, outState)
            }
        }
        super.onSaveInstanceState(outState)
    }

    private fun saveLineData(index: Int, lineData: ILineDataSet, outState: Bundle) {

        val entrys = ArrayList<Entry>(lineData.entryCount)
        for (i in 0 until lineData.entryCount) {
            entrys.add(lineData.getEntryForIndex(i))
        }
        outState.putParcelableArrayList("values_${index}", entrys)
    }

    private fun loadLineData(index: Int, lineData: ILineDataSet, inState: Bundle) {
        val entrys = inState.getParcelableArrayList<Entry>("values_${index}")
        entrys?.forEach { lineData.addEntry(it) }
    }

    private fun restorePlotData(lineData: List<LineDataSet>, savedInstanceState: Bundle?) {
        val status = savedInstanceState ?: return
        if (mFirstTimeRestore) {
            lineData.forEachIndexed { index, lineDataSet ->
                loadLineData(index, lineDataSet, status)
            }
            mFirstTimeRestore = false
            Log.d(TAG, "state restore")
        } else {
            Log.d(TAG, "state NOT restore")
        }
    }

    override fun onPause() {
        stopPlotting()
        super.onPause()
    }

    fun stopPlotting() {
        dataViewModel.stopPlotFeature()
    }


    private fun onStartStopPlotButtonClicked() {
        if (dataViewModel.isPlotting.value == false) {
            settingsViewModel.startPlotSelectedFeature()
        }
        settingsViewModel.selectedFeature.value?.let {
            dataViewModel.onStartStopButtonPressed(it)
            //Delete the Last Sample Value
            mFeatureValue.text = ""
        }
    }

    private fun onResumePausePlotButtonClicked(): Boolean {
        if (dataViewModel.isPlotting.value == true)
            settingsViewModel.selectedFeature.value?.let {
                dataViewModel.onResumePauseButtonPressed()
            }
        return true
    }

}


private fun LineData.removeEntryOlderThan(timeRange: Duration?) {
    if (timeRange == null)
        return
    val plotRangeMs = (xMax - xMin).toDouble().milliseconds
    if (plotRangeMs > timeRange) {
        val minValidX = (xMax - timeRange.toDouble(DurationUnit.MILLISECONDS)).toFloat()
        dataSets.forEach {
            it.removeXLessThan(minValidX)
        }
    }
}

private fun ILineDataSet.removeXLessThan(value: Float) {
    while (getEntryForIndex(0).x < value) {
        removeFirst()
    }
}
