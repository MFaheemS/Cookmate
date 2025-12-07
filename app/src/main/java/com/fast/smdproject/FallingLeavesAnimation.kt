package com.fast.smdproject

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import kotlin.random.Random

class FallingLeavesAnimation(
    private val context: Context,
    private val container: ViewGroup,
    private val headerHeight: Int
) {

    private val activeLeaves = mutableListOf<ImageView>()
    private var isAnimating = false

    fun start() {
        if (isAnimating) return
        isAnimating = true

        // Create leaves at intervals
        createLeafPeriodically()
    }

    fun stop() {
        isAnimating = false
        // Remove all active leaves
        activeLeaves.forEach { leaf ->
            container.removeView(leaf)
        }
        activeLeaves.clear()
    }

    private fun createLeafPeriodically() {
        if (!isAnimating) return

        createFallingLeaf()

        // Create next leaf after random delay (1-3 seconds)
        container.postDelayed({
            createLeafPeriodically()
        }, Random.nextLong(1000, 3000))
    }

    private fun createFallingLeaf() {
        val leaf = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                Random.nextInt(30, 50), // Random size between 30-50dp
                Random.nextInt(30, 50)
            )

            // Use leaf drawable
            setImageResource(R.drawable.ic_leaf)

            // Random green tint
            val greenShades = listOf(
                android.graphics.Color.parseColor("#7CB342"),
                android.graphics.Color.parseColor("#66BB6A"),
                android.graphics.Color.parseColor("#81C784"),
                android.graphics.Color.parseColor("#AED581")
            )
            setColorFilter(greenShades.random())

            // Random starting position (across the width)
            x = Random.nextFloat() * (container.width - 50f)
            y = 0f // Start at top

            // Random rotation
            rotation = Random.nextFloat() * 360f
            alpha = 1f
        }

        container.addView(leaf)
        activeLeaves.add(leaf)

        animateLeaf(leaf)
    }

    private fun animateLeaf(leaf: ImageView) {
        val fallDuration = Random.nextLong(4000, 7000) // 4-7 seconds to fall
        val endY = headerHeight.toFloat()

        // Falling animation
        val fallAnimator = ObjectAnimator.ofFloat(leaf, "translationY", 0f, endY).apply {
            duration = fallDuration
            interpolator = android.view.animation.LinearInterpolator()
        }

        // Swaying animation (left-right movement)
        val swayAmount = Random.nextInt(30, 80).toFloat()
        val swayAnimator = ObjectAnimator.ofFloat(
            leaf,
            "translationX",
            leaf.x,
            leaf.x + swayAmount,
            leaf.x - swayAmount,
            leaf.x + swayAmount / 2,
            leaf.x
        ).apply {
            duration = fallDuration
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        }

        // Rotation animation
        val rotationAnimator = ObjectAnimator.ofFloat(
            leaf,
            "rotation",
            leaf.rotation,
            leaf.rotation + Random.nextInt(360, 720).toFloat()
        ).apply {
            duration = fallDuration
            interpolator = android.view.animation.LinearInterpolator()
        }

        // Fade out animation (starts at 70% of fall duration)
        val fadeStartDelay = (fallDuration * 0.7).toLong()
        val fadeDuration = (fallDuration * 0.3).toLong()

        val fadeAnimator = ObjectAnimator.ofFloat(leaf, "alpha", 1f, 0f).apply {
            startDelay = fadeStartDelay
            duration = fadeDuration
            interpolator = android.view.animation.AccelerateInterpolator()
        }

        // Start all animations
        fallAnimator.start()
        swayAnimator.start()
        rotationAnimator.start()
        fadeAnimator.start()

        // Remove leaf when animation completes
        fallAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                container.removeView(leaf)
                activeLeaves.remove(leaf)
            }
        })
    }
}

