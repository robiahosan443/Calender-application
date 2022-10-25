package com.example.calenderapplication

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate

class DayViewContainer(view: View, function: () -> Unit) : ViewContainer(view) {

    val textView = view.findViewById<TextView>(R.id.calendarDayText)


    // With ViewBinding
// val textView = CalendarDayLayoutBinding.bind(view).calendarDayText

}




