package com.repsyncdemo.workout.ui.home

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class CustomDotSpan(private val count: Int, private val color: Int) : LineBackgroundSpan {
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
        val spacing = 12f

        when (count) {
            1 -> {
                canvas.drawCircle(center, y, dotRadius, paint)
            }
            2 -> {
                canvas.drawCircle(center - spacing / 2, y, dotRadius, paint)
                canvas.drawCircle(center + spacing / 2, y, dotRadius, paint)
            }
            else -> {
                // 2 dots and a plus
                canvas.drawCircle(center - spacing, y, dotRadius, paint)
                canvas.drawCircle(center, y, dotRadius, paint)
                
                val originalStrokeWidth = paint.strokeWidth
                paint.strokeWidth = 3f
                canvas.drawLine(center + spacing - 5, y, center + spacing + 5, y, paint)
                canvas.drawLine(center + spacing, y - 5, center + spacing, y + 5, paint)
                paint.strokeWidth = originalStrokeWidth
            }
        }
        
        paint.color = oldColor
    }
}

class WorkoutCountDecorator(
    private val color: Int,
    private val day: CalendarDay,
    private val count: Int
) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean = this.day == day
    override fun decorate(view: DayViewFacade) {
        view.addSpan(CustomDotSpan(count, color))
    }
}
