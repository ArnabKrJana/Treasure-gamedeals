package com.example.treasure.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween

/**
 * Reusable navigation transition animation specifications across Jetpack Compose navigation graphs.
 *
 * Implements standard directional slide transitions with a consistent 350ms duration.
 */
object Transitions {
    /**
     * Standard duration (in milliseconds) for navigation screen slide animations.
     */
    const val NAV_ANIM_DURATION = 350

    /**
     * Slides entering content in from the right edge of the screen toward the left.
     *
     * @return Configured [EnterTransition].
     */
    fun AnimatedContentTransitionScope<*>.slideInFromRight(): EnterTransition =
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(NAV_ANIM_DURATION)
        )

    /**
     * Slides exiting content out through the left edge of the screen.
     *
     * @return Configured [ExitTransition].
     */
    fun AnimatedContentTransitionScope<*>.slideOutToLeft(): ExitTransition =
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(NAV_ANIM_DURATION)
        )

    /**
     * Slides entering content in from the left edge of the screen toward the right.
     *
     * @return Configured [EnterTransition].
     */
    fun AnimatedContentTransitionScope<*>.slideInFromLeft(): EnterTransition =
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(NAV_ANIM_DURATION)
        )

    /**
     * Slides exiting content out through the right edge of the screen.
     *
     * @return Configured [ExitTransition].
     */
    fun AnimatedContentTransitionScope<*>.slideOutToRight(): ExitTransition =
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(NAV_ANIM_DURATION)
        )
}
