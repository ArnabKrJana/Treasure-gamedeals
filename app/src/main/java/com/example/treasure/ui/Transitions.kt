package com.example.treasure.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween

object Transitions {
    const val NAV_ANIM_DURATION = 350

    fun AnimatedContentTransitionScope<*>.slideInFromRight() =
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(NAV_ANIM_DURATION)
        )

    fun AnimatedContentTransitionScope<*>.slideOutToLeft() =
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(NAV_ANIM_DURATION)
        )

    fun AnimatedContentTransitionScope<*>.slideInFromLeft() =
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(NAV_ANIM_DURATION)
        )

    fun AnimatedContentTransitionScope<*>.slideOutToRight() =
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(NAV_ANIM_DURATION)
        )
}