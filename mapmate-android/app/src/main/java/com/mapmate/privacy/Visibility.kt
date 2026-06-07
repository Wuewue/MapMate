package com.mapmate.privacy

import com.mapmate.device.Coordinates
import java.time.Instant

// req: UR-C3, SRS-PRV-006, tells the UI what to render (mode & freshness)
// the OUTPUT of PrivacyResolver.resolve()
// no init check here, PrivacyResolver upholds the HIDDEN -> null coords contract
data class Visibility(
    val coordinates: Coordinates?, // nullable cause it could render nothing
    val precision: Precision, // what mode to render, it's an enum below
    val asOfTime: Instant?,
    val approximateRadiusMeters: Int? = null,
)
// req: UR-C3, SRS-PRV-006, the render-state the UI switches on
enum class Precision {
    EXACT,
    APPROXIMATE,
    FROZEN,
    LAST_KNOWN,
    HIDDEN,
}
