package com.example.tripchecker.ui.navigation

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry

private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val NAV_DURATION_MS = 300

/** Respects system animator duration scale (including "Remove animations"). */
fun navAnimationDurationMillis(
    context: Context,
    defaultMs: Int = NAV_DURATION_MS,
): Int =
    runCatching {
        val scale =
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        if (scale == 0f) 0 else defaultMs
    }.getOrDefault(defaultMs)

@Composable
fun rememberNavAnimationDurationMillis(defaultMs: Int = NAV_DURATION_MS): Int {
    val context = LocalContext.current
    return remember(context, defaultMs) { navAnimationDurationMillis(context, defaultMs) }
}

private fun navSlideTween(durationMs: Int): TweenSpec<IntOffset> = tween(durationMs, easing = EmphasizedEasing)

private fun navFadeTween(durationMs: Int): TweenSpec<Float> = tween(durationMs, easing = EmphasizedEasing)

/** Horizontal slide + fade for forward navigation (new screen from the end edge). */
fun AnimatedContentTransitionScope<NavBackStackEntry>.appEnterTransition(durationMs: Int): EnterTransition {
    return slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, navSlideTween(durationMs)) +
        fadeIn(navFadeTween(durationMs))
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.appExitTransition(durationMs: Int): ExitTransition {
    return slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, navSlideTween(durationMs)) +
        fadeOut(navFadeTween(durationMs))
}

/** Horizontal slide + fade when popping back (previous screen from the start edge). */
fun AnimatedContentTransitionScope<NavBackStackEntry>.appPopEnterTransition(durationMs: Int): EnterTransition {
    return slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, navSlideTween(durationMs)) +
        fadeIn(navFadeTween(durationMs))
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.appPopExitTransition(durationMs: Int): ExitTransition {
    return slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, navSlideTween(durationMs)) +
        fadeOut(navFadeTween(durationMs))
}
