package com.fast.smdproject

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import kotlin.random.Random

class JungleLeavesAnimation(
    private val context: Context,
    private val container: ViewGroup
) {

    private val activeLeaves = mutableListOf<ImageView>()

    /**
     * Trigger a one-time burst of jungle leaves falling
     * @param count Number of leaves to spawn (default: 20)
     */
    fun triggerBurst(count: Int = 20) {
        // Wait for container to be measured
        container.post {
            // Spawn all leaves at once with slight delays for a natural cascade effect
            for (i in 0 until count) {
                container.postDelayed({
                    createFallingLeaf()
                }, i * 50L) // 50ms delay between each leaf spawn
            }
        }
    }

    /**
     * Clean up any remaining leaves
     */
    fun cleanup() {
        activeLeaves.forEach { leaf ->
            container.removeView(leaf)
        }
        activeLeaves.clear()
    }

    private fun createFallingLeaf() {
        if (container.width == 0) return // Container not measured yet

        val leaf = ImageView(context).apply {
            val size = Random.nextInt(40, 70) // Bigger leaves for more impact
            layoutParams = ViewGroup.LayoutParams(size, size)

            // Use leaf drawable
            setImageResource(R.drawable.ic_leaf)

            // Jungle-themed green tints with more variety
            val jungleGreenShades = listOf(
                android.graphics.Color.parseColor("#2E7D32"), // Dark green
                android.graphics.Color.parseColor("#388E3C"), // Medium dark green
                android.graphics.Color.parseColor("#43A047"), // Medium green
                android.graphics.Color.parseColor("#66BB6A"), // Light green
                android.graphics.Color.parseColor("#7CB342"), // Lime green
                android.graphics.Color.parseColor("#558B2F"), // Olive green
                android.graphics.Color.parseColor("#689F38")  // Yellow green
            )
            setColorFilter(jungleGreenShades.random())

            // Random starting position across the entire width
            x = Random.nextFloat() * (container.width - size)
            y = -size.toFloat() // Start above the screen

            // Random starting rotation
            rotation = Random.nextFloat() * 360f
            alpha = 0.95f // Slightly transparent for a natural look
        }

        container.addView(leaf)
        activeLeaves.add(leaf)

        animateLeaf(leaf)
    }

    private fun animateLeaf(leaf: ImageView) {
        // Varying fall speeds for more dynamic effect
        val fallDuration = Random.nextLong(2500, 5000) // 2.5-5 seconds
        val endY = container.height.toFloat() + 100f // Fall past the bottom

        // Falling animation
        val fallAnimator = ObjectAnimator.ofFloat(leaf, "translationY", leaf.y, endY).apply {
            duration = fallDuration
            interpolator = android.view.animation.AccelerateInterpolator(1.2f)
        }

        // Swaying animation (zigzag left-right movement for jungle feel)
        val swayAmount = Random.nextInt(50, 120).toFloat()
        val swayDirection = if (Random.nextBoolean()) 1 else -1
        val swayAnimator = ObjectAnimator.ofFloat(
            leaf,
            "translationX",
            leaf.x,
            leaf.x + (swayAmount * swayDirection * 0.5f),
            leaf.x - (swayAmount * swayDirection * 0.3f),
            leaf.x + (swayAmount * swayDirection * 0.7f),
            leaf.x - (swayAmount * swayDirection * 0.2f),
            leaf.x + (swayAmount * swayDirection * 0.4f)
        ).apply {
            duration = fallDuration
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        }

        // Enhanced rotation animation (multiple spins for dramatic effect)
        val rotationAmount = Random.nextInt(720, 1440).toFloat() // 2-4 full rotations
        val rotationDirection = if (Random.nextBoolean()) 1 else -1
        val rotationAnimator = ObjectAnimator.ofFloat(
            leaf,
            "rotation",
            leaf.rotation,
            leaf.rotation + (rotationAmount * rotationDirection)
        ).apply {
            duration = fallDuration
            interpolator = android.view.animation.LinearInterpolator()
        }

        // Gentle scale animation (leaves get slightly smaller as they fall)
        val scaleAnimator = ObjectAnimator.ofFloat(leaf, "scaleX", 1f, 0.7f).apply {
            duration = fallDuration
            interpolator = android.view.animation.DecelerateInterpolator()
        }
        val scaleYAnimator = ObjectAnimator.ofFloat(leaf, "scaleY", 1f, 0.7f).apply {
            duration = fallDuration
            interpolator = android.view.animation.DecelerateInterpolator()
        }

        // Fade out at the end
        val fadeStartDelay = (fallDuration * 0.75).toLong()
        val fadeDuration = (fallDuration * 0.25).toLong()
        val fadeAnimator = ObjectAnimator.ofFloat(leaf, "alpha", 0.95f, 0f).apply {
            startDelay = fadeStartDelay
            duration = fadeDuration
            interpolator = android.view.animation.AccelerateInterpolator()
        }

        // Start all animations
        fallAnimator.start()
        swayAnimator.start()
        rotationAnimator.start()
        scaleAnimator.start()
        scaleYAnimator.start()
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

