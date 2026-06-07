package com.mapmate.device

// req: UR-B2, SRS-DI-002, SRS-DI-003, foreground vs background location sharing
// the user's choice for how their location is shared with friends
enum class LocationSharingMode {
    FOREGROUND_ONLY, // only share while app is open
    INCLUDE_BACKGROUND, // share even when app is backgrounded
}

// req: UR-B2, UR-B3, SRS-DI-002, SRS-DI-003, SRS-DI-004, sharing mode & OS permission state
// held by the app layer next to the per-friend PrivacyMode, not bundled into DeviceSnapshot on purpose
// settings != device state
data class LocationSharingConfig( // settings at OS level
    val mode: LocationSharingMode,
    val hasBackgroundPermission: Boolean,
) {
    // requires both: user wants it & OS granted permission
    val backgroundActive: Boolean
        get() = mode == LocationSharingMode.INCLUDE_BACKGROUND && hasBackgroundPermission

    // forground: permission state doesn't matter in this case
    val isForegroundOnly: Boolean
        get() = mode == LocationSharingMode.FOREGROUND_ONLY

    // req: UR-B3, SRS-DI-004, flag the "you asked for background but OS said no" state for a UI prompt
    // user wants background but OS denied it
    val needsBackgroundPermission: Boolean
        get() = mode == LocationSharingMode.INCLUDE_BACKGROUND && !hasBackgroundPermission
}
