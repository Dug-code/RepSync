package com.repsyncdemo.workout.ui.home

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class CustomDotSpan(private val color: Int) : LineBackgroundSpan {
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNum: Int
    ) {
        val oldColor = paint.color
        if (color != 0) {
            paint.color = color
        }
        
        val center = (left + right) / 2f
        val y = bottom + 12f
        val dotRadius = 4f

        canvas.drawCircle(center, y, dotRadius, paint)
        
        paint.color = oldColor
    }
}

class WorkoutCountDecorator(
    private val color: Int,
    private val day: CalendarDay
) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean = this.day == day
    override fun decorate(view: DayViewFacade) {
        view.addSpan(CustomDotSpan(color))
    }
}
