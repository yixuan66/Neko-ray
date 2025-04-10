package com.neko.themeengine

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerSize
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * A custom ImageView that renders itself as a circle using Material Components.
 *
 * This view:
 * - Automatically clips to a circular shape using a `ShapeAppearanceModel`.
 * - Allows background color to be set via both direct color int and resource ID.
 * - Ensures equal width and height for perfect circular dimensions.
 * - Animates the corner size slightly on touch for a subtle press effect.
 *
 * Useful for avatars, profile pictures, or any circular image display.
 */
class CircleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = -1
) : ShapeableImageView(context, attrs, defStyleAttr) {

    init {
        val shapeAppearanceModel = ShapeAppearanceModel()
            .toBuilder()
            .setAllCornerSizes(CornerSize {
                return@CornerSize it.height() / 2
            })
            .build()
        background = MaterialShapeDrawable(shapeAppearanceModel)
    }

    override fun setBackgroundColor(color: Int) {
        (background as MaterialShapeDrawable).fillColor = ColorStateList.valueOf(color)
    }

    fun setBackgroundColorRes(@ColorRes color: Int) {
        setBackgroundColor(ResourcesCompat.getColor(resources, color, context.theme))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    private lateinit var shapeDrawableAnimator: ValueAnimator

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                shapeDrawableAnimator = ValueAnimator.ofFloat(2f, 4f)
                shapeDrawableAnimator.addUpdateListener {
                    (background as MaterialShapeDrawable).setCornerSize(CornerSize { rect ->
                        return@CornerSize rect.height() / it.animatedValue as Float
                    })
                }
                shapeDrawableAnimator.start()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                shapeDrawableAnimator.reverse()
            }
        }
        return super.onTouchEvent(event)
    }
}
