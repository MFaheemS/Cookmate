package com.fast.smdproject

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator

object AnimationUtils {

    // Pop animation for items appearing
    fun popIn(view: View, delay: Long = 0) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    // Pop out animation for items disappearing
    fun popOut(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onEnd?.invoke()
                }
            })
            .start()
    }

    // Fade out animation
    fun fadeOut(view: View, duration: Long = 300, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onEnd?.invoke()
                }
            })
            .start()
    }

    // Fade in animation
    fun fadeIn(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(delay)
            .start()
    }

    // Slide up and fade in
    fun slideUpFadeIn(view: View, delay: Long = 0) {
        view.translationY = 100f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(delay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // Slide out to right
    fun slideOutRight(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .translationX(view.width.toFloat())
            .alpha(0f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.translationX = 0f
                    onEnd?.invoke()
                }
            })
            .start()
    }

    // Button press effect (scale down then back)
    fun buttonPressEffect(view: View, onEnd: (() -> Unit)? = null) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)

        scaleDown.duration = 100
        scaleDownY.duration = 100

        scaleDown.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
                val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)

                scaleUp.duration = 100
                scaleUpY.duration = 100

                scaleUp.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd?.invoke()
                    }
                })

                scaleUp.start()
                scaleUpY.start()
            }
        })

        scaleDown.start()
        scaleDownY.start()
    }

    // Ripple effect for toggle buttons
    fun rippleToggle(view: View, isActive: Boolean) {
        view.animate()
            .scaleX(if (isActive) 1.1f else 1f)
            .scaleY(if (isActive) 1.1f else 1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    // Shake animation for errors
    fun shake(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 500
        animator.start()
    }

    // Pulse animation for notifications
    fun pulse(view: View, repeatCount: Int = 2) {
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f)

        scaleUp.repeatCount = repeatCount
        scaleUpY.repeatCount = repeatCount
        scaleUp.repeatMode = ValueAnimator.REVERSE
        scaleUpY.repeatMode = ValueAnimator.REVERSE
        scaleUp.duration = 300
        scaleUpY.duration = 300

        scaleUp.start()
        scaleUpY.start()
    }

    // Rotate animation for refresh
    fun rotate(view: View, degrees: Float = 360f, duration: Long = 500) {
        view.animate()
            .rotation(view.rotation + degrees)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // Bounce effect
    fun bounce(view: View) {
        view.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
            .start()
    }
}

