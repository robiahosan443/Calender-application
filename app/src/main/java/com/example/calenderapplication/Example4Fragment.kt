package com.example.calenderapplication

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calenderapplication.ContinuousSelectionHelper.getSelection
import com.example.calenderapplication.ContinuousSelectionHelper.isInDateBetweenSelection
import com.example.calenderapplication.ContinuousSelectionHelper.isOutDateBetweenSelection
import com.example.calenderapplication.databinding.Example3EventItemViewBinding
import com.example.calenderapplication.databinding.Example4CalendarDayBinding
import com.example.calenderapplication.databinding.Example4CalendarHeaderBinding
import com.example.calenderapplication.databinding.Example4FragmentBinding
import com.google.android.material.snackbar.Snackbar
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit


class Example4Fragment : BaseFragment(R.layout.example_4_fragment), HasToolbar, HasBackButton {
    private val events = mutableMapOf<LocalDate, List<Event>>()
    private val selectionFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    private val inputDialog by lazy {
        val editText = AppCompatEditText(requireContext())
        val layout = FrameLayout(requireContext()).apply {
            // Setting the padding on the EditText only pads the input area
            // not the entire EditText so we wrap it in a FrameLayout.
            val padding = dpToPx(20, requireContext())
            setPadding(padding, padding, padding, padding)
            addView(editText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.example_3_input_dialog_title))
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                saveEvent(editText.text.toString())
                // Prepare EditText for reuse.
                editText.setText("")
            }
            .setNegativeButton(R.string.close, null)
            .create()
            .apply {
                setOnShowListener {
                    // Show the keyboard
                    editText.requestFocus()
                    context.inputMethodManager
                        .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                }
                setOnDismissListener {
                    // Hide the keyboard
                    context.inputMethodManager
                        .toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                }
            }
    }

    private val eventsAdapter = Example3EventsAdapter {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.example_3_dialog_delete_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteEvent(it)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun deleteEvent(event: Event) {
        val date = event.date
        events[date] = events[date].orEmpty().minus(event)
        updateAdapterForDate(date, date, "")
    }

    private fun updateAdapterForDate(date: LocalDate, endDate: LocalDate, from: String) {
        eventsAdapter.apply {
            events.clear()
            events.addAll(this@Example4Fragment.events[date].orEmpty())
            notifyDataSetChanged()
        }

        if (from.equals("fromstartDateendDate")) {
            binding.exThreeSelectedDateText.text = selectionFormatter.format(date) + " To " + selectionFormatter.format(endDate)
        } else {
            binding.exThreeSelectedDateText.text = selectionFormatter.format(date)
        }
    }

    override val toolbar: Toolbar
        get() = binding.exFourToolbar
    override val titleRes: Int? = null
    private val today = LocalDate.now()
    private var selection = DateSelection()
    private val headerDateFormatter = DateTimeFormatter.ofPattern("EEE'\n'd MMM")
    private val startBackground: GradientDrawable by lazy {
        requireContext().getDrawableCompat(R.drawable.example_4_continuous_selected_bg_start) as GradientDrawable
    }
    private val endBackground: GradientDrawable by lazy {
        requireContext().getDrawableCompat(R.drawable.example_4_continuous_selected_bg_end) as GradientDrawable
    }
    private lateinit var binding: Example4FragmentBinding
    private var selectedDate: LocalDate? = null
    var min: Int? = null
    var max: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)



        binding = Example4FragmentBinding.bind(view)

        val bundle = arguments
        max = bundle!!.getString("max")?.toInt()
        min = bundle.getString("min")?.toInt()

        Log.d("TAG", "onViewCreated: "+max)
        Log.d("TAG", "onViewCreated: "+min)


        binding.exFourCalendar.post {
            val radius = ((binding.exFourCalendar.width / 7) / 2).toFloat()
            startBackground.setCornerRadius(topLeft = radius, bottomLeft = radius)
            endBackground.setCornerRadius(topRight = radius, bottomRight = radius)
        }

        val daysOfWeek = daysOfWeek()
        binding.legendLayout.root.children.forEachIndexed { index, child ->
            (child as TextView).apply {
                text = daysOfWeek[index].displayText()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColorRes(R.color.example_4_grey)
            }
        }

        configureBinders()
        val currentMonth = YearMonth.now()
        binding.exFourCalendar.setup(
            currentMonth,
            currentMonth.plusMonths(12),
            daysOfWeek.first(),
        )
        binding.exFourCalendar.scrollToMonth(currentMonth)

        binding.exFourSaveButton.setOnClickListener click@{
            val (startDate, endDate) = selection
            if (startDate != null && endDate != null) {
                val text = dateRangeDisplayText(startDate, endDate)
                Snackbar.make(requireView(), text, Snackbar.LENGTH_LONG).show()
            }
            parentFragmentManager.popBackStack()
        }

        bindSummaryViews()
        setUpNoteAdapter()
        if (savedInstanceState == null) {
            // Show today's events initially.
            binding.exFourCalendar.post { selectDate(today) }
        }

        binding.exThreeAddButton.setOnClickListener { inputDialog.show() }
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { binding.exFourCalendar.notifyDateChanged(it) }
            binding.exFourCalendar.notifyDateChanged(date)
            updateAdapterForDate(date, date, "")
        }
    }

    private fun saveEvent(text: String) {
        if (text.isBlank()) {
            Toast.makeText(requireContext(), R.string.example_3_empty_input_text, Toast.LENGTH_LONG)
                .show()
        } else {
            Log.d("TAG", "saveEvent else: " + selectedDate)
            selectedDate?.let {
                events[it] =
                    events[it].orEmpty().plus(Event(UUID.randomUUID().toString(), text, it))
                updateAdapterForDate(it, it, "")

            }
        }
    }

    private fun setUpNoteAdapter() {
        binding.exThreeRv.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = eventsAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        }
    }

    private fun bindSummaryViews() {
        binding.exFourStartDateText.apply {
            if (selection.startDate != null) {
                text = headerDateFormatter.format(selection.startDate)
                setTextColorRes(R.color.example_4_grey)
            } else {
                text = getString(R.string.start_date)
                setTextColor(Color.GRAY)
            }
        }

        binding.exFourEndDateText.apply {
            if (selection.endDate != null) {
                text = headerDateFormatter.format(selection.endDate)
                setTextColorRes(R.color.example_4_grey)
            } else {
                text = getString(R.string.end_date)
                setTextColor(Color.GRAY)
            }
        }

        binding.exFourSaveButton.isEnabled = selection.daysBetween != null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.example_4_menu, menu)
        binding.exFourToolbar.post {
            // Configure menu text to match what is in the Airbnb app.
            binding.exFourToolbar.findViewById<TextView>(R.id.menuItemClear).apply {
                setTextColor(requireContext().getColorCompat(R.color.example_4_grey))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                isAllCaps = false
            }
        }
        menu.findItem(R.id.menuItemClear).setOnMenuItemClickListener {
            selection = DateSelection()
            binding.exFourCalendar.notifyCalendarChanged()
            bindSummaryViews()
            true
        }
    }

    override fun onStart() {
        super.onStart()
        val closeIndicator = requireContext().getDrawableCompat(R.drawable.ic_close)?.apply {
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                requireContext().getColorCompat(R.color.example_4_grey),
                BlendModeCompat.SRC_ATOP
            )
        }
        (activity as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(closeIndicator)
        requireActivity().window.apply {
            // Update status bar color to match toolbar color.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                statusBarColor = requireContext().getColorCompat(R.color.white)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                statusBarColor = Color.GRAY
            }
        }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.apply {
            // Reset status bar color.
            statusBarColor = requireContext().getColorCompat(R.color.colorPrimaryDark)
            decorView.systemUiVisibility = 0
        }
    }

    private fun configureBinders() {
        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay // Will be set when this container is bound.
            val binding = Example4CalendarDayBinding.bind(view)

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate &&
                        (day.date == today || day.date.isAfter(today))
                    ) {
                        selection = getSelection(
                            clickedDate = day.date,
                            dateSelection = selection,
                        )
                        this@Example4Fragment.binding.exFourCalendar.notifyCalendarChanged()
                        // bindSummaryViews()
                    }
                    if (day.position == DayPosition.MonthDate) {
                        selectDate(day.date)
                    }
                }
            }
        }

        binding.exFourCalendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.binding.exFourDayText
                val roundBgView = container.binding.exFourRoundBgView
                val dotView = container.binding.exThreeDotView

                textView.text = null
                textView.background = null
             roundBgView.makeInVisible()
          //     dotView.makeInVisible()

                val (startDate, endDate) = selection

                when (data.position) {
                    DayPosition.MonthDate -> {
                        textView.text = data.date.dayOfMonth.toString()
                        if (data.date.isBefore(today)) {
                            textView.setTextColorRes(R.color.example_4_grey_past)
                        } else {
                            when {
                                startDate == data.date && endDate == null -> {
                                        textView.setTextColorRes(R.color.white)
                                        roundBgView.makeVisible()
                                        roundBgView.setBackgroundResource(R.drawable.example_4_single_selected_bg)
                                    updateAdapterForDate(startDate, startDate, "")
                                }
                                data.date == startDate -> {
                                 updateAdapterForDate(startDate, startDate, "")
                                    if (validDate(startDate, endDate, textView, min!!, max!!)) {
                                        textView.setTextColorRes(R.color.white)
                                        textView.background = startBackground
                                        updateAdapterForDate(startDate, endDate!!, "fromstartDateendDate")
                                    }
                                }
                                startDate != null && endDate != null && (data.date > startDate && data.date < endDate) -> {

                                    if (validDate(startDate, endDate, textView,  min!!, max!!)) {
                                        updateAdapterForDate(startDate, endDate, "fromstartDateendDate")
                                        textView.setTextColorRes(R.color.white)
                                        textView.setBackgroundResource(R.drawable.example_4_continuous_selected_bg_middle)
                                        bindSummaryViews()

                                    }else{
                                        textView.setTextColorRes(R.color.example_4_grey)
                                    }
                                }
                                data.date == endDate -> {
                                  updateAdapterForDate(endDate, endDate, "")
                                    if (validDate(startDate, endDate, textView,  min!!, max!!)) {
                                        textView.setTextColorRes(R.color.white)
                                        textView.background = endBackground
                                        updateAdapterForDate(startDate!!, endDate, "fromstartDateendDate")
                                    }
                                    else{
                                        textView.setTextColorRes(R.color.white)
                                        roundBgView.makeVisible()
                                        roundBgView.setBackgroundResource(R.drawable.example_4_single_selected_bg)
                                    }
                                }
                                data.date == today -> {
                                    textView.setTextColorRes(R.color.example_4_grey)
                                    roundBgView.makeVisible()
                                    roundBgView.setBackgroundResource(R.drawable.example_4_today_bg)
                                }
                                else -> textView.setTextColorRes(R.color.example_4_grey)
                            }
                        }
                    }
                    // Make the coloured selection background continuous on the invisible in and out dates across various months.
                    DayPosition.InDate ->
                        if (startDate != null && endDate != null &&
                            isInDateBetweenSelection(data.date, startDate, endDate)
                        ) {
                            textView.setBackgroundResource(R.drawable.example_4_continuous_selected_bg_middle)
                        }
                    DayPosition.OutDate ->
                        if (startDate != null && endDate != null &&
                            isOutDateBetweenSelection(data.date, startDate, endDate)
                        ) {
                            textView.setBackgroundResource(R.drawable.example_4_continuous_selected_bg_middle)
                        }
                }

                if (data.position == DayPosition.MonthDate) {
                    textView.makeVisible()

                    when (data.date) {
                        today -> {
                            dotView.makeInVisible()
                            Log.d("TAG", "today: "+today)
                        }
                        selectedDate -> {
                            dotView.makeInVisible()
                            Log.d("TAG", "selectedDate: "+selectedDate)
                        }
                        else -> {
                            dotView.isVisible = events[data.date].orEmpty().isNotEmpty()
                        }
                    }
                } else {
                    textView.makeInVisible()
                    dotView.makeInVisible()
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val textView = Example4CalendarHeaderBinding.bind(view).exFourHeaderText
        }
        binding.exFourCalendar.monthHeaderBinder =
            object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    container.textView.text = data.yearMonth.displayText()
                }
            }
    }

    private fun validDate(startDate: LocalDate?, endDate: LocalDate?, textView: TextView, minimum: Int, maximum: Int): Boolean {
        if (startDate != null && endDate != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd")
            val start: Date = sdf.parse(startDate.toString())
            val end: Date = sdf.parse(endDate.toString())
            val diff: Long = end.getTime() - start.getTime()
            var count = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt() + 1

            var isSame: Boolean = (count in minimum..maximum)
            return isSame
        }
       return false
    }
}

data class Event(val id: String, val text: String, val date: LocalDate)

class Example3EventsAdapter(val onClick: (Event) -> Unit) :
    RecyclerView.Adapter<Example3EventsAdapter.Example3EventsViewHolder>() {

    val events = mutableListOf<Event>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Example3EventsViewHolder {
        return Example3EventsViewHolder(
            Example3EventItemViewBinding.inflate(parent.context.layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(viewHolder: Example3EventsViewHolder, position: Int) {
        viewHolder.bind(events[position])

        Log.d("TAG", "onBindViewHolder: "+events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class Example3EventsViewHolder(private val binding: Example3EventItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                onClick(events[bindingAdapterPosition])
            }
        }

        fun bind(event: Event) {
            binding.itemEventText.text = event.text
        }
    }
}
