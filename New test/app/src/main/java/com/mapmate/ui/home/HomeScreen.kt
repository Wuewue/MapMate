package com.mapmate.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * MAPMATE home screen.
 *
 * Required dependencies:
 * - com.google.maps.android:maps-compose
 * - com.google.android.gms:play-services-maps
 * - androidx.compose.material:material-icons-extended
 */
@Composable
fun MapMateHomeScreen(
    modifier: Modifier = Modifier,
    placeTitle: String = "Hoang Mai\nDistrict",
    temperatureLabel: String = "30.9C",
    locationAccuracyMeters: Int = 117,
    currentFriend: HomeFriendUiModel = HomeFriendUiModel.sample,
    nearbyPois: List<MapPoiUiModel> = sampleMapPois,
    onAddFriendClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLayersClick: () -> Unit = {},
    onLocateMeClick: () -> Unit = {},
    onMapModeClick: () -> Unit = {},
    onAccuracyClick: () -> Unit = {},
    onHelpLocationClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onCloseInviteClick: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MapMateColors.MapNavy)
    ) {
        val isCompact = maxWidth < 380.dp
        val safePadding = WindowInsets.safeDrawing.asPaddingValues()
        val sidePadding = if (isCompact) 16.dp else 24.dp

        HomeMapLayer()
        MapVisualOverlay(
            currentFriend = currentFriend,
            pois = nearbyPois,
        )

        TopMapContent(
            placeTitle = placeTitle,
            temperatureLabel = temperatureLabel,
            locationAccuracyMeters = locationAccuracyMeters,
            isCompact = isCompact,
            safePadding = safePadding,
            sidePadding = sidePadding,
            onMapModeClick = onMapModeClick,
            onAccuracyClick = onAccuracyClick,
        )

        FloatingMapControls(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = safePadding.calculateTopPadding() + if (isCompact) 56.dp else 68.dp,
                    end = sidePadding,
                ),
            onAddFriendClick = onAddFriendClick,
            onProfileClick = onProfileClick,
            onLayersClick = onLayersClick,
            onLocateMeClick = onLocateMeClick,
        )

        LocationHelpBubble(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (isCompact) 10.dp else 36.dp),
            onClick = onHelpLocationClick,
        )

        InviteFriendsPanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
            isCompact = isCompact,
            onInviteClick = onInviteClick,
            onCloseClick = onCloseInviteClick,
        )
    }
}

@Immutable
data class HomeFriendUiModel(
    val initials: String,
    val statusLabel: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val locationAccuracyMeters: Int,
    val anchor: MapAnchor,
) {
    companion object {
        val sample = HomeFriendUiModel(
            initials = "tt",
            statusLabel = "1H22M",
            batteryPercent = 82,
            isCharging = true,
            locationAccuracyMeters = 117,
            anchor = MapAnchor(x = 0.50f, y = 0.56f),
        )
    }
}

@Immutable
data class MapPoiUiModel(
    val type: PoiType,
    val anchor: MapAnchor,
)

@Immutable
data class MapAnchor(
    val x: Float,
    val y: Float,
)

enum class PoiType {
    Bus,
    Hospital,
    School,
    FriendQuestion,
}

private val sampleMapPois = listOf(
    MapPoiUiModel(PoiType.Bus, MapAnchor(0.30f, 0.30f)),
    MapPoiUiModel(PoiType.Bus, MapAnchor(0.78f, 0.36f)),
    MapPoiUiModel(PoiType.Hospital, MapAnchor(0.36f, 0.40f)),
    MapPoiUiModel(PoiType.School, MapAnchor(0.77f, 0.11f)),
    MapPoiUiModel(PoiType.FriendQuestion, MapAnchor(0.39f, 0.60f)),
    MapPoiUiModel(PoiType.FriendQuestion, MapAnchor(0.48f, 0.66f)),
)

@Composable
private fun HomeMapLayer() {
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        IllustratedMapBackdrop()
        return
    }

    val hoangMai = LatLng(20.9778, 105.8427)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(hoangMai, 14.3f)
    }
    val mapUiSettings = remember {
        MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = true,
        )
    }
    val mapProperties = remember {
        MapProperties(
            isBuildingEnabled = true,
            isIndoorEnabled = true,
            mapStyleOptions = MapStyleOptions(DarkMapStyleJson),
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
    )

    // Adds the same deep, social-map mood as the reference while preserving map detail.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x6B071827))
    )
}

@Composable
private fun IllustratedMapBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF102840), Color(0xFF0A1B2F), Color(0xFF10253A)),
            )
        )

        for (i in 0..11) {
            val y = size.height * (i / 11f)
            drawLine(
                color = Color(0x334B6072),
                start = Offset(0f, y + (i % 3) * 18f),
                end = Offset(size.width, y - 120f + (i % 4) * 32f),
                strokeWidth = if (i % 4 == 0) 7f else 2.6f,
                cap = StrokeCap.Round,
            )
        }

        for (i in 0..8) {
            val x = size.width * (i / 8f)
            drawLine(
                color = Color(0x2E7E8C98),
                start = Offset(x - 80f, 0f),
                end = Offset(x + 120f, size.height),
                strokeWidth = if (i % 3 == 0) 5.5f else 2.3f,
                cap = StrokeCap.Round,
            )
        }

        drawLine(
            color = Color(0x806F8191),
            start = Offset(size.width * 0.06f, size.height * 0.76f),
            end = Offset(size.width * 0.96f, size.height * 0.93f),
            strokeWidth = 18f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xC7C5B071),
            start = Offset(size.width * 0.06f, size.height * 0.76f),
            end = Offset(size.width * 0.96f, size.height * 0.93f),
            strokeWidth = 8f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MapVisualOverlay(
    currentFriend: HomeFriendUiModel,
    pois: List<MapPoiUiModel>,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        FriendDirectionCone(
            anchor = currentFriend.anchor,
            modifier = Modifier.fillMaxSize(),
        )

        pois.forEach { poi ->
            PoiMarker(
                type = poi.type,
                modifier = Modifier.offset(
                    x = maxWidth * poi.anchor.x - 14.dp,
                    y = maxHeight * poi.anchor.y - 14.dp,
                )
            )
        }

        FriendLocationMarker(
            friend = currentFriend,
            modifier = Modifier.offset(
                x = maxWidth * currentFriend.anchor.x - 76.dp,
                y = maxHeight * currentFriend.anchor.y - 96.dp,
            )
        )
    }
}

@Composable
private fun FriendDirectionCone(
    anchor: MapAnchor,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val origin = Offset(size.width * anchor.x, size.height * anchor.y + 86.dp.toPx())
        val destination = Offset(size.width * 0.78f, size.height * 0.98f)
        val cone = Path().apply {
            moveTo(origin.x - 22.dp.toPx(), origin.y)
            quadraticBezierTo(
                size.width * 0.58f,
                size.height * 0.74f,
                destination.x - 92.dp.toPx(),
                destination.y,
            )
            lineTo(destination.x + 130.dp.toPx(), destination.y)
            quadraticBezierTo(
                size.width * 0.66f,
                size.height * 0.76f,
                origin.x + 22.dp.toPx(),
                origin.y,
            )
            close()
        }

        drawPath(
            path = cone,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x963CA7FF), Color(0x443CA7FF)),
            ),
        )
    }
}

@Composable
private fun TopMapContent(
    placeTitle: String,
    temperatureLabel: String,
    locationAccuracyMeters: Int,
    isCompact: Boolean,
    safePadding: PaddingValues,
    sidePadding: Dp,
    onMapModeClick: () -> Unit,
    onAccuracyClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = sidePadding,
                top = safePadding.calculateTopPadding() + 28.dp,
                end = if (isCompact) 76.dp else 96.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 16.dp else 18.dp),
    ) {
        OutlinedMapTitle(
            text = placeTitle,
            fontSize = if (isCompact) 40.sp else 56.sp,
        )

        WeatherRow(temperatureLabel = temperatureLabel)

        MapModeChip(
            label = "Friendship Map",
            onClick = onMapModeClick,
        )

        AccuracyChip(
            accuracyMeters = locationAccuracyMeters,
            onClick = onAccuracyClick,
        )
    }
}

@Composable
private fun OutlinedMapTitle(
    text: String,
    fontSize: TextUnit,
) {
    val lineHeight = (fontSize.value * 1.04f).sp
    Box {
        val outlineOffsets = listOf(
            -3.dp to 0.dp,
            3.dp to 0.dp,
            0.dp to -3.dp,
            0.dp to 3.dp,
            -2.dp to -2.dp,
            2.dp to 2.dp,
            -2.dp to 2.dp,
            2.dp to -2.dp,
        )

        outlineOffsets.forEach { (x, y) ->
            Text(
                text = text,
                modifier = Modifier.offset(x = x, y = y),
                color = Color.Black,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WeatherRow(
    temperatureLabel: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SunIcon(modifier = Modifier.size(31.dp))
        Text(
            text = temperatureLabel,
            color = Color.Black,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 0.sp),
        )
    }
}

@Composable
private fun SunIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.23f
        val center = Offset(size.width / 2f, size.height / 2f)

        repeat(10) { index ->
            val angle = Math.toRadians((index * 36).toDouble())
            val inner = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius * 1.55f,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius * 1.55f,
            )
            val outer = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius * 2.35f,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius * 2.35f,
            )
            drawLine(Color.Black, inner, outer, strokeWidth = 3.4f, cap = StrokeCap.Round)
        }

        drawCircle(
            color = Color.Black,
            radius = radius,
            center = center,
            style = Stroke(width = 3.4f),
        )
    }
}

@Composable
private fun MapModeChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 310.dp),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 7.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            FriendshipBadge(modifier = Modifier.size(42.dp))
            Text(
                text = label,
                color = Color(0xFF2E2E35),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Change map mode",
                tint = Color(0xFF777777),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun FriendshipBadge(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFFFDB3D))
            drawCircle(
                color = Color(0xFFFFF4A0),
                radius = size.minDimension * 0.34f,
                center = center,
            )
        }
        Text(
            text = "B",
            color = Color.Black,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun AccuracyChip(
    accuracyMeters: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 330.dp),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xD9D7DDE4),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 11.dp, top = 7.dp, end = 8.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9AABBD))
                    .border(2.dp, Color.White, CircleShape),
            )
            Text(
                text = "Location accuracy: ${accuracyMeters}m",
                color = Color(0xFF252A31),
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = "Open location accuracy details",
                tint = Color(0xFF464B52),
                modifier = Modifier.size(25.dp),
            )
        }
    }
}

@Composable
private fun FloatingMapControls(
    modifier: Modifier = Modifier,
    onAddFriendClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLayersClick: () -> Unit,
    onLocateMeClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundMapButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Add friend",
                background = Color(0xFFECEFF5),
                contentColor = Color(0xFF7A7E86),
                onClick = onAddFriendClick,
                size = 66.dp,
            )
            ProfileShortcutButton(
                initials = "tt",
                onClick = onProfileClick,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RoundMapButton(
                icon = Icons.Rounded.MyLocation,
                contentDescription = "Center map on me",
                background = Color(0xEDECF1F6),
                contentColor = Color(0xFF6D8593),
                onClick = onLocateMeClick,
                size = 46.dp,
            )
            RoundMapButton(
                icon = Icons.Rounded.Layers,
                contentDescription = "Map layers",
                background = Color.White,
                contentColor = Color(0xFF59616D),
                onClick = onLayersClick,
                size = 54.dp,
            )
            CrownButton(onClick = onProfileClick)
        }
    }
}

@Composable
private fun RoundMapButton(
    icon: ImageVector,
    contentDescription: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    size: Dp,
) {
    Surface(
        modifier = Modifier.size(size),
        onClick = onClick,
        shape = CircleShape,
        color = background,
        shadowElevation = 5.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(size * 0.50f),
            )
        }
    }
}

@Composable
private fun ProfileShortcutButton(
    initials: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(66.dp),
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF5C5A55),
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun CrownButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(57.dp),
        onClick = onClick,
        shape = RoundedCornerShape(17.dp),
        color = Color.White,
        shadowElevation = 5.dp,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val crown = Path().apply {
                moveTo(size.width * 0.20f, size.height * 0.68f)
                lineTo(size.width * 0.28f, size.height * 0.38f)
                lineTo(size.width * 0.42f, size.height * 0.58f)
                lineTo(size.width * 0.52f, size.height * 0.25f)
                lineTo(size.width * 0.65f, size.height * 0.58f)
                lineTo(size.width * 0.80f, size.height * 0.38f)
                lineTo(size.width * 0.80f, size.height * 0.68f)
                close()
            }
            drawPath(
                path = crown,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF2975), Color(0xFFFFB51F), Color(0xFF30D35A)),
                )
            )
            drawPath(crown, Color.White, style = Stroke(width = 4f))
        }
    }
}

@Composable
private fun LocationHelpBubble(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.widthIn(max = 310.dp),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 9.dp, end = 8.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(33.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "?",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = "Location not accurate?",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = "Open location help",
                tint = Color(0xFF333333),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun PoiMarker(
    type: PoiType,
    modifier: Modifier = Modifier,
) {
    when (type) {
        PoiType.Bus -> SmallIconPin(
            modifier = modifier,
            icon = Icons.Rounded.DirectionsBus,
            iconTint = Color(0xFF7A94A4),
        )

        PoiType.Hospital -> MapPin(
            modifier = modifier,
            icon = Icons.Rounded.LocalHospital,
            background = Color(0xFFFF4266),
            iconTint = Color.White,
            size = 45.dp,
        )

        PoiType.School -> SmallIconPin(
            modifier = modifier,
            icon = Icons.Rounded.School,
            iconTint = Color(0xFF6C8898),
        )

        PoiType.FriendQuestion -> QuestionPin(modifier = modifier)
    }
}

@Composable
private fun SmallIconPin(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
) {
    Surface(
        modifier = modifier.size(30.dp),
        shape = CircleShape,
        color = Color(0xE9FFFFFF),
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun MapPin(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    background: Color,
    iconTint: Color,
    size: Dp,
) {
    Box(modifier = modifier.requiredSize(size), contentAlignment = Alignment.TopCenter) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            val circleRadius = canvasSize.minDimension * 0.36f
            val center = Offset(canvasSize.width / 2f, circleRadius + 2.dp.toPx())
            val tail = Path().apply {
                moveTo(center.x - circleRadius * 0.42f, center.y + circleRadius * 0.55f)
                quadraticBezierTo(
                    center.x,
                    canvasSize.height,
                    center.x + circleRadius * 0.42f,
                    center.y + circleRadius * 0.55f,
                )
                close()
            }
            drawPath(tail, Color.White)
            drawCircle(Color.White, circleRadius + 4.dp.toPx(), center)
            drawPath(tail, background)
            drawCircle(background, circleRadius, center)
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(size * 0.42f),
        )
    }
}

@Composable
private fun QuestionPin(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(35.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "?",
            color = Color.White,
            fontSize = 33.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.offset(x = 1.dp, y = 1.dp),
        )
        Text(
            text = "?",
            color = Color(0xFFFF234D),
            fontSize = 31.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun FriendLocationMarker(
    friend: HomeFriendUiModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.requiredSize(width = 152.dp, height = 188.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopCenter)
                .offset(y = 12.dp)
        ) {
            drawCircle(
                color = Color(0x6851C6FF),
                radius = size.minDimension * 0.43f,
                center = center,
            )
            drawCircle(
                color = Color(0x5D9A66EA),
                radius = size.minDimension * 0.34f,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.36f),
                radius = size.minDimension * 0.18f,
                center = Offset(center.x, center.y + 30.dp.toPx()),
                style = Stroke(width = 4.dp.toPx()),
            )
        }

        UserBubble(
            initials = friend.initials,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 34.dp),
        )

        StatusPill(
            friend = friend,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 139.dp),
        )
    }
}

@Composable
private fun UserBubble(
    initials: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.requiredSize(width = 98.dp, height = 112.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val circleRadius = canvasSize.width * 0.43f
            val center = Offset(size.width / 2f, circleRadius + 2.dp.toPx())
            val tail = Path().apply {
                moveTo(center.x - 18.dp.toPx(), center.y + circleRadius * 0.70f)
                quadraticBezierTo(
                    center.x,
                    canvasSize.height,
                    center.x + 18.dp.toPx(),
                    center.y + circleRadius * 0.70f,
                )
                close()
            }

            drawPath(tail, MapMateColors.AvatarBlue)
            drawCircle(MapMateColors.AvatarBlue, circleRadius, center)
        }
        Text(
            text = initials,
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 37.dp),
        )
    }
}

@Composable
private fun StatusPill(
    friend: HomeFriendUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "?",
                color = Color(0xFFFF234D),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 20.sp,
            )
            Text(
                text = friend.statusLabel,
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            BatteryStatus(
                percent = friend.batteryPercent,
                isCharging = friend.isCharging,
            )
        }
    }
}

@Composable
private fun BatteryStatus(
    percent: Int,
    isCharging: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isCharging) Color(0xFF22B84E) else Color(0xFF757575)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.BatteryChargingFull,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = "$percent%",
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun InviteFriendsPanel(
    modifier: Modifier = Modifier,
    isCompact: Boolean,
    onInviteClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(true) }
    if (!isVisible) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isCompact) 168.dp else 185.dp),
        shape = RoundedCornerShape(23.dp),
        color = Color.Black,
        shadowElevation = 12.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            InviteArtwork(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(if (isCompact) 210.dp else 245.dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 30.dp, top = 22.dp, bottom = 24.dp, end = 132.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Invite",
                    color = Color.White,
                    fontSize = if (isCompact) 29.sp else 34.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    text = "Map your world with more friends",
                    color = Color(0xFF9D9DA3),
                    fontSize = if (isCompact) 21.sp else 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (isCompact) 29.sp else 34.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(3) {
                        InvitePlusButton(onClick = onInviteClick)
                    }
                }
            }

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 22.dp, end = 20.dp)
                    .size(39.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E2E31)),
                onClick = {
                    isVisible = false
                    onCloseClick()
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close invite card",
                    tint = Color(0xFF9B9BA0),
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
private fun InvitePlusButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(56.dp),
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF66666A),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Invite friend",
                tint = Color(0xFFC8C8CA),
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun InviteArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF45F0AA), Color(0xFF6F45FF), Color.Transparent),
                center = Offset(size.width * 0.60f, size.height * 0.95f),
                radius = size.minDimension * 0.82f,
            ),
            radius = size.minDimension * 0.58f,
            center = Offset(size.width * 0.68f, size.height * 0.98f),
        )

        val faceCenter = Offset(size.width * 0.49f, size.height * 0.32f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF45A), Color(0xFFFFB927)),
                center = faceCenter,
                radius = size.minDimension * 0.38f,
            ),
            radius = size.minDimension * 0.31f,
            center = faceCenter,
        )
        drawCircle(Color(0xFF6E4C36), radius = 8.dp.toPx(), center = Offset(faceCenter.x - 23.dp.toPx(), faceCenter.y))
        drawCircle(Color(0xFF6E4C36), radius = 8.dp.toPx(), center = Offset(faceCenter.x + 22.dp.toPx(), faceCenter.y + 5.dp.toPx()))
        drawArc(
            color = Color(0xFF6E4C36),
            startAngle = 205f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(faceCenter.x - 46.dp.toPx(), faceCenter.y - 42.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(38.dp.toPx(), 30.dp.toPx()),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = Color(0xFF6E4C36),
            startAngle = 250f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(faceCenter.x + 22.dp.toPx(), faceCenter.y - 45.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(40.dp.toPx(), 30.dp.toPx()),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(Color(0xFF6E4C36), radius = 8.dp.toPx(), center = Offset(faceCenter.x + 4.dp.toPx(), faceCenter.y + 28.dp.toPx()))

        val heart = Path().apply {
            moveTo(faceCenter.x + 60.dp.toPx(), faceCenter.y - 2.dp.toPx())
            cubicTo(
                faceCenter.x + 77.dp.toPx(),
                faceCenter.y - 26.dp.toPx(),
                faceCenter.x + 105.dp.toPx(),
                faceCenter.y - 8.dp.toPx(),
                faceCenter.x + 82.dp.toPx(),
                faceCenter.y + 25.dp.toPx(),
            )
            cubicTo(
                faceCenter.x + 57.dp.toPx(),
                faceCenter.y + 2.dp.toPx(),
                faceCenter.x + 42.dp.toPx(),
                faceCenter.y - 20.dp.toPx(),
                faceCenter.x + 60.dp.toPx(),
                faceCenter.y - 2.dp.toPx(),
            )
            close()
        }
        drawPath(
            path = heart,
            brush = Brush.linearGradient(listOf(Color(0xFFFF3D74), Color(0xFFFF89AA))),
        )

        drawCircle(Color(0xFFFFD130), radius = 31.dp.toPx(), center = Offset(size.width * 0.38f, size.height * 0.73f))
        drawCircle(Color(0xFFFFD130), radius = 38.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.74f))
        drawArc(
            color = Color(0xFF7A4E24),
            startAngle = 25f,
            sweepAngle = 125f,
            useCenter = false,
            topLeft = Offset(size.width * 0.72f, size.height * 0.67f),
            size = androidx.compose.ui.geometry.Size(52.dp.toPx(), 35.dp.toPx()),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private object MapMateColors {
    val MapNavy = Color(0xFF10253A)
    val AvatarBlue = Color(0xFF3E8AF3)
}

private const val DarkMapStyleJson = """
[
  {"elementType":"geometry","stylers":[{"color":"#17283B"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#A77B58"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#0C1725"}]},
  {"featureType":"administrative","elementType":"geometry.stroke","stylers":[{"color":"#2C3C50"}]},
  {"featureType":"landscape","elementType":"geometry","stylers":[{"color":"#13263A"}]},
  {"featureType":"poi","elementType":"geometry","stylers":[{"color":"#182E43"}]},
  {"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#D18F63"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#40505F"}]},
  {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#142234"}]},
  {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#B69A6E"}]},
  {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#203247"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0B2036"}]},
  {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#6A8195"}]}
]
"""

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MapMateHomeScreenPreview() {
    MaterialTheme {
        MapMateHomeScreen()
    }
}
