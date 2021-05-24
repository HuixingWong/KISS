package fr.neamar.kiss.nextgeneration

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.annotation.RequiresApi
import androidx.core.view.children
import kotlin.math.abs

class TPager(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs) {
    private var downX = 0f
    private var downY = 0f
    private var downScrollX = 0f
    private var downScrollY = 0f
    private var scrolling = false
    private val overScroller: OverScroller = OverScroller(context)
    private val viewConfiguration: ViewConfiguration = ViewConfiguration.get(context)
    private val velocityTracker = VelocityTracker.obtain()
    private var minVelocity = viewConfiguration.scaledMinimumFlingVelocity
    private var maxVelocity = viewConfiguration.scaledMaximumFlingVelocity
    private var pagingSlop = viewConfiguration.scaledPagingTouchSlop

    private var targetPage = 0

    private var verticalPage = 0

    private var orientation = HORIZONTAL


    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        reset()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reset()
    }

    fun reset() {
        Handler(Looper.getMainLooper()).post {
            scrollTo(width, 0)
            targetPage = 1
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var childLeft = 0
        val childTop = 0
        var childRight = width
        val childBottom = height * 2
        for (child in children) {
            child.layout(childLeft, childTop, childRight, childBottom)
            childLeft += width
            childRight += width
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            velocityTracker.clear()
        }
        velocityTracker.addMovement(event)
        var result = false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                scrolling = false
                downX = event.x
                downY = event.y
                downScrollX = scrollX.toFloat()
                downScrollY = scrollY.toFloat()
            }
            MotionEvent.ACTION_MOVE -> if (!scrolling) {
                val dx = abs(downX - event.x)
                val dy = abs(downY - event.y)
                if (dx > pagingSlop || dy > pagingSlop) {
                    orientation = if (dx > dy) {
                        HORIZONTAL
                    } else {
                        VERTICAL
                    }
                    scrolling = true
                    parent.requestDisallowInterceptTouchEvent(true)
                    result = true
                }
            }
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            velocityTracker.clear()
        }
        velocityTracker.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downScrollX = scrollX.toFloat()
                downScrollY = scrollY.toFloat()
            }
            MotionEvent.ACTION_MOVE -> {
                var dy = -1
                if (targetPage == 1) {
                    dy = (downY - event.y + downScrollY).toInt()
                        .coerceAtLeast(0)
                        .coerceAtMost(height * 2)
                }
                val dx = (downX - event.x + downScrollX).toInt()
                    .coerceAtLeast(0)
                    .coerceAtMost(width * (childCount - 1))
                if (orientation == HORIZONTAL) {
                    scrollTo(dx, 0)
                } else {
                    scrollTo(width, dy)
                }
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.computeCurrentVelocity(
                    1000,
                    maxVelocity.toFloat()
                )
                if (orientation == HORIZONTAL) {
                    val vx = velocityTracker.xVelocity
                    overScroller.startScroll(scrollX, 0, computeXDistance(vx), 0)
                    postInvalidateOnAnimation()
                } else {
                    val vy = velocityTracker.yVelocity
                    overScroller.startScroll(width, scrollY, 0, computeYDistance(vy))
                    postInvalidateOnAnimation()
                }
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun computeScroll() {
        if (overScroller.computeScrollOffset()) {
            scrollTo(overScroller.currX, overScroller.currY)
            postInvalidateOnAnimation()
        }
    }

    private fun computeXFraction(): Float {
        return (overScroller.currX - overScroller.startX).toFloat() / (overScroller.finalX - overScroller.startX).toFloat()
    }

    private fun computeYFraction(): Float {
        return (overScroller.currY - overScroller.startY).toFloat() / (overScroller.finalY - overScroller.startY).toFloat()
    }

    private fun computeYDistance(vy: Float): Int {
        verticalPage = if (abs(vy) < minVelocity) {
            if (scrollY > height / 2) 1 else 0
        } else {
            if (vy < 0) 1 else 0
        }
        return if (verticalPage == 1) height - scrollY else -scrollY
    }

    private fun computeXDistance(vx: Float): Int {
        targetPage = if (abs(vx) < minVelocity) {
            if (vx > 0) {
                if (scrollX > width / 2) minOf(getMaxPage(), targetPage + 1) else targetPage
            } else {
                if (scrollX > width / 2) maxOf(0, targetPage - 1) else targetPage
            }
        } else {
            if (vx < 0)
                minOf(getMaxPage(), targetPage + 1)
            else
                maxOf(0, targetPage - 1)
        }
        return (width * targetPage) - scrollX
    }

    private fun getMaxPage(): Int = childCount - 1

}
