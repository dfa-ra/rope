package app.rope.android.data

/**
 * Landscape playback for the in-thread video viewer. Not the call overlay
 * (no CallVideoRenderer). LocalStore stays v6.
 */
object VideoLandRules {
    const val ROTATE = "Альбом"
    const val EXIT = "Книжная"

    /** ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE */
    const val ORIENT_LANDSCAPE = 6

    /** ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED */
    const val ORIENT_UNLOCKED = -1

    fun showsRotate(isVideo: Boolean, pathReady: Boolean): Boolean = isVideo && pathReady

    fun fillBleed(landscape: Boolean, isVideo: Boolean): Boolean = landscape && isVideo

    fun activityOrientation(wantLandscape: Boolean): Int =
        if (wantLandscape) ORIENT_LANDSCAPE else ORIENT_UNLOCKED

    fun contentDescription(landscape: Boolean): String = if (landscape) EXIT else ROTATE
}
