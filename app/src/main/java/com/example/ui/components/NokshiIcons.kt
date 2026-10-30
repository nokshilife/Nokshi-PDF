package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

object NokshiIcons {

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathBuilder().apply(block).nodes,
                fill = SolidColor(Color.Black)
            )
        }.build()
    }

    val Palette: ImageVector by lazy {
        icon("Palette") {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveToRelative(0f, 4.97f, 4.03f, 9f, 9f, 9f)
            curveToRelative(0.83f, 0f, 1.5f, -0.67f, 1.5f, -1.5f)
            curveToRelative(0f, -0.39f, -0.15f, -0.74f, -0.39f, -1.01f)
            curveToRelative(-0.23f, -0.26f, -0.38f, -0.61f, -0.38f, -0.99f)
            curveToRelative(0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
            horizontalLineTo(16f)
            curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
            curveToRelative(0f, -4.42f, -4.03f, -8f, -9f, -8f)
            close()
            moveTo(6.5f, 12f)
            curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveTo(5.67f, 9f, 6.5f, 9f)
            reflectiveCurveTo(8f, 9.67f, 8f, 10.5f)
            reflectiveCurveTo(7.33f, 12f, 6.5f, 12f)
            close()
            moveTo(9.5f, 8f)
            curveTo(8.67f, 8f, 8f, 7.33f, 8f, 6.5f)
            reflectiveCurveTo(8.67f, 5f, 9.5f, 5f)
            reflectiveCurveTo(11f, 5.67f, 11f, 6.5f)
            reflectiveCurveTo(10.33f, 8f, 9.5f, 8f)
            close()
            moveTo(14.5f, 8f)
            curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveTo(13.67f, 5f, 14.5f, 5f)
            reflectiveCurveTo(16f, 5.67f, 16f, 6.5f)
            reflectiveCurveTo(15.33f, 8f, 14.5f, 8f)
            close()
            moveTo(17.5f, 12f)
            curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveTo(16.67f, 9f, 17.5f, 9f)
            reflectiveCurveTo(19f, 9.67f, 19f, 10.5f)
            reflectiveCurveTo(18.33f, 12f, 17.5f, 12f)
            close()
        }
    }

    val Draw: ImageVector by lazy {
        icon("Draw") {
            moveTo(3f, 17.25f)
            verticalLineTo(21f)
            horizontalLineToRelative(3.75f)
            lineTo(17.81f, 9.94f)
            lineToRelative(-3.75f, -3.75f)
            lineTo(3f, 17.25f)
            close()
            moveTo(20.71f, 7.04f)
            curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
            lineToRelative(-2.34f, -2.34f)
            curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
            lineToRelative(-1.83f, 1.83f)
            lineToRelative(3.75f, 3.75f)
            lineToRelative(1.83f, -1.83f)
            close()
        }
    }

    val Colorize: ImageVector by lazy {
        icon("Colorize") {
            moveTo(20.71f, 5.63f)
            lineToRelative(-2.34f, -2.34f)
            curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
            lineToRelative(-3.12f, 3.12f)
            lineToRelative(-1.93f, -1.91f)
            lineToRelative(-1.41f, 1.41f)
            lineToRelative(1.42f, 1.42f)
            lineTo(3f, 16.25f)
            verticalLineTo(21f)
            horizontalLineToRelative(4.75f)
            lineToRelative(8.92f, -8.92f)
            lineToRelative(1.42f, 1.42f)
            lineToRelative(1.41f, -1.41f)
            lineToRelative(-1.92f, -1.92f)
            lineToRelative(3.12f, -3.12f)
            curveToRelative(0.4f, -0.4f, 0.4f, -1.03f, 0.01f, -1.42f)
            close()
        }
    }

    val FontDownload: ImageVector by lazy {
        icon("FontDownload") {
            moveTo(9.93f, 13.5f)
            horizontalLineToRelative(4.14f)
            lineTo(12f, 7.98f)
            close()
            moveTo(20f, 2f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(16f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(4f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(15.95f, 18.5f)
            lineToRelative(-1.14f, -3f)
            horizontalLineTo(9.17f)
            lineToRelative(-1.12f, 3f)
            horizontalLineTo(5.96f)
            lineToRelative(5.11f, -13f)
            horizontalLineToRelative(1.86f)
            lineToRelative(5.11f, 13f)
            horizontalLineToRelative(-2.09f)
            close()
        }
    }

    val Description: ImageVector by lazy {
        icon("Description") {
            moveTo(14f, 2f)
            horizontalLineTo(6f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(4f, 20f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 1.99f, 2f)
            horizontalLineTo(18f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(8f)
            lineToRelative(-6f, -6f)
            close()
            moveTo(16f, 18f)
            horizontalLineTo(8f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(2f)
            close()
            moveTo(16f, 14f)
            horizontalLineTo(8f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(2f)
            close()
            moveTo(13f, 9f)
            verticalLineTo(3.5f)
            lineTo(18.5f, 9f)
            horizontalLineTo(13f)
            close()
        }
    }

    val Image: ImageVector by lazy {
        icon("Image") {
            moveTo(21f, 19f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            close()
            moveTo(8.5f, 13.5f)
            lineToRelative(2.5f, 3.01f)
            lineTo(14.5f, 12f)
            lineToRelative(4.5f, 6f)
            horizontalLineTo(5f)
            lineToRelative(3.5f, -4.5f)
            close()
        }
    }

    val TableChart: ImageVector by lazy {
        icon("TableChart") {
            moveTo(10f, 10.02f)
            horizontalLineToRelative(5f)
            verticalLineTo(21f)
            horizontalLineToRelative(-5f)
            close()
            moveTo(17f, 21f)
            horizontalLineToRelative(3f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-9f)
            horizontalLineToRelative(-5f)
            verticalLineToRelative(11f)
            close()
            moveTo(20f, 3f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(19f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(3f, 19f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(3f)
            verticalLineTo(10f)
            horizontalLineTo(3f)
            verticalLineToRelative(9f)
            close()
        }
    }

    val Slideshow: ImageVector by lazy {
        icon("Slideshow") {
            moveTo(10f, 8f)
            verticalLineToRelative(8f)
            lineToRelative(5f, -4f)
            lineToRelative(-5f, -4f)
            close()
            moveTo(19f, 3f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(19f, 19f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(14f)
            close()
        }
    }

    val DocumentScanner: ImageVector by lazy {
        icon("DocumentScanner") {
            moveTo(7f, 3f)
            horizontalLineTo(4f)
            verticalLineToRelative(3f)
            horizontalLineTo(2f)
            verticalLineTo(1f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(2f)
            close()
            moveTo(22f, 6f)
            verticalLineTo(1f)
            horizontalLineToRelative(-5f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(2f)
            close()
            moveTo(7f, 21f)
            horizontalLineTo(4f)
            verticalLineToRelative(-3f)
            horizontalLineTo(2f)
            verticalLineToRelative(5f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(-2f)
            close()
            moveTo(20f, 18f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(-3f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(-5f)
            horizontalLineToRelative(-2f)
            close()
            moveTo(19f, 6f)
            horizontalLineTo(5f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(14f)
            verticalLineTo(6f)
            close()
            moveTo(15f, 11f)
            horizontalLineTo(9f)
            verticalLineTo(9f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(2f)
            close()
            moveTo(15f, 15f)
            horizontalLineTo(9f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(2f)
            close()
        }
    }

    val FolderOpen: ImageVector by lazy {
        icon("FolderOpen") {
            moveTo(20f, 6f)
            horizontalLineToRelative(-8f)
            lineToRelative(-2f, -2f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(2f, 18f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(8f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(20f, 18f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(10f)
            close()
        }
    }

    val EditNote: ImageVector by lazy {
        icon("EditNote") {
            moveTo(3f, 10f)
            horizontalLineToRelative(11f)
            verticalLineToRelative(2f)
            horizontalLineTo(3f)
            verticalLineToRelative(-2f)
            close()
            moveTo(3f, 6f)
            horizontalLineToRelative(11f)
            verticalLineToRelative(2f)
            horizontalLineTo(3f)
            verticalLineTo(6f)
            close()
            moveTo(3f, 14f)
            horizontalLineToRelative(7f)
            verticalLineToRelative(2f)
            horizontalLineTo(3f)
            verticalLineToRelative(-2f)
            close()
            moveTo(20.59f, 11.93f)
            lineToRelative(-0.71f, -0.71f)
            curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
            lineToRelative(-0.71f, 0.71f)
            lineToRelative(2.12f, 2.12f)
            lineToRelative(0.71f, -0.71f)
            curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
            close()
            moveTo(17.05f, 12.75f)
            lineTo(12f, 17.8f)
            verticalLineTo(20f)
            horizontalLineToRelative(2.2f)
            lineToRelative(5.05f, -5.05f)
            lineToRelative(-2.2f, -2.2f)
            close()
        }
    }

    val AutoAwesome: ImageVector by lazy {
        icon("AutoAwesome") {
            moveTo(19f, 9f)
            lineToRelative(1.25f, -2.75f)
            lineTo(23f, 5f)
            lineToRelative(-2.75f, -1.25f)
            lineTo(19f, 1f)
            lineToRelative(-1.25f, 2.75f)
            lineTo(15f, 5f)
            lineToRelative(2.75f, 1.25f)
            close()
            moveTo(11.5f, 9.5f)
            lineTo(9f, 4f)
            lineTo(6.5f, 9.5f)
            lineTo(1f, 12f)
            lineToRelative(5.5f, 2.5f)
            lineTo(9f, 20f)
            lineToRelative(2.5f, -5.5f)
            lineTo(17f, 12f)
            close()
        }
    }

    val CallMerge: ImageVector by lazy {
        icon("CallMerge") {
            moveTo(17f, 20.41f)
            lineTo(18.41f, 19f)
            lineTo(15f, 15.59f)
            lineTo(13.59f, 17f)
            lineTo(17f, 20.41f)
            close()
            moveTo(7.5f, 8f)
            horizontalLineTo(11f)
            verticalLineToRelative(5.59f)
            lineTo(5.59f, 19f)
            lineTo(7f, 20.41f)
            lineToRelative(6f, -6f)
            verticalLineTo(8f)
            horizontalLineToRelative(3.5f)
            lineTo(12f, 3.5f)
            lineTo(7.5f, 8f)
            close()
        }
    }

    val PictureAsPdf: ImageVector by lazy {
        icon("PictureAsPdf") {
            moveTo(20f, 2f)
            horizontalLineTo(8f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(12f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(4f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(4f, 6f)
            horizontalLineTo(2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(4f)
            verticalLineTo(6f)
            close()
            moveTo(10f, 9f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(-8f)
            verticalLineTo(9f)
            close()
            moveTo(10f, 13f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(-5f)
            verticalLineToRelative(-2f)
            close()
        }
    }

    val ArrowUpward: ImageVector by lazy {
        icon("ArrowUpward") {
            moveTo(4f, 12f)
            lineToRelative(1.41f, 1.41f)
            lineTo(11f, 7.83f)
            verticalLineTo(20f)
            horizontalLineToRelative(2f)
            verticalLineTo(7.83f)
            lineToRelative(5.58f, 5.59f)
            lineTo(20f, 12f)
            lineToRelative(-8f, -8f)
            lineToRelative(-8f, 8f)
            close()
        }
    }

    val ArrowDownward: ImageVector by lazy {
        icon("ArrowDownward") {
            moveTo(20f, 12f)
            lineToRelative(-1.41f, -1.41f)
            lineTo(13f, 16.17f)
            verticalLineTo(4f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(12.17f)
            lineToRelative(-5.58f, -5.59f)
            lineTo(4f, 12f)
            lineToRelative(8f, 8f)
            lineToRelative(8f, -8f)
            close()
        }
    }

    val CheckBox: ImageVector by lazy {
        icon("CheckBox") {
            moveTo(19f, 3f)
            horizontalLineTo(5f)
            curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.11f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.89f, -2f, -2f, -2f)
            close()
            moveTo(10f, 17f)
            lineToRelative(-5f, -5f)
            lineToRelative(1.41f, -1.41f)
            lineTo(10f, 14.17f)
            lineToRelative(7.59f, -7.59f)
            lineTo(19f, 8f)
            lineToRelative(-9f, 9f)
            close()
        }
    }

    val CheckBoxOutlineBlank: ImageVector by lazy {
        icon("CheckBoxOutlineBlank") {
            moveTo(19f, 5f)
            verticalLineToRelative(14f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineToRelative(14f)
            moveTo(19f, 3f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
        }
    }

    val LockOpen: ImageVector by lazy {
        icon("LockOpen") {
            moveTo(12f, 17f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
            reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
            reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
            close()
            moveTo(18f, 8f)
            horizontalLineToRelative(-1f)
            verticalLineTo(6f)
            curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
            reflectiveCurveTo(7f, 3.24f, 7f, 6f)
            horizontalLineToRelative(1.9f)
            curveToRelative(0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
            curveToRelative(1.71f, 0f, 3.1f, 1.39f, 3.1f, 3.1f)
            verticalLineToRelative(2f)
            horizontalLineTo(6f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(10f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(12f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(10f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
        }
    }

    val Download: ImageVector by lazy {
        icon("Download") {
            moveTo(5f, 20f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(5f)
            verticalLineToRelative(2f)
            close()
            moveTo(19f, 9f)
            horizontalLineToRelative(-4f)
            verticalLineTo(3f)
            horizontalLineTo(9f)
            verticalLineToRelative(6f)
            horizontalLineTo(5f)
            lineToRelative(7f, 7f)
            lineToRelative(7f, -7f)
            close()
        }
    }

    val ContentCopy: ImageVector by lazy {
        icon("ContentCopy") {
            moveTo(16f, 1f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(2f)
            verticalLineTo(3f)
            horizontalLineToRelative(12f)
            verticalLineTo(1f)
            close()
            moveTo(19f, 5f)
            horizontalLineTo(8f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(11f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(7f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(19f, 21f)
            horizontalLineTo(8f)
            verticalLineTo(7f)
            horizontalLineToRelative(11f)
            verticalLineToRelative(14f)
            close()
        }
    }

    val OpenInNew: ImageVector by lazy {
        icon("OpenInNew") {
            moveTo(19f, 19f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineToRelative(7f)
            verticalLineTo(3f)
            horizontalLineTo(5f)
            curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-7f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(7f)
            close()
            moveTo(14f, 3f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(3.59f)
            lineToRelative(-9.83f, 9.83f)
            lineToRelative(1.41f, 1.41f)
            lineTo(19f, 6.41f)
            verticalLineTo(10f)
            horizontalLineToRelative(2f)
            verticalLineTo(3f)
            horizontalLineToRelative(-7f)
            close()
        }
    }

    val RadioButtonUnchecked: ImageVector by lazy {
        icon("RadioButtonUnchecked") {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
            reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f)
            close()
            moveTo(12f, 20f)
            curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
            reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
            reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
            reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
            close()
        }
    }

    val SelectAll: ImageVector by lazy {
        icon("SelectAll") {
            moveTo(3f, 5f)
            horizontalLineToRelative(2f)
            verticalLineTo(3f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            close()
            moveTo(3f, 13f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-2f)
            horizontalLineTo(3f)
            verticalLineToRelative(2f)
            close()
            moveTo(7f, 21f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-2f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            close()
            moveTo(19f, 3f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(2f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(7f, 17f)
            horizontalLineToRelative(10f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            verticalLineToRelative(10f)
            close()
            moveTo(9f, 9f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(6f)
            horizontalLineTo(9f)
            verticalLineTo(9f)
            close()
        }
    }

    val NavigateBefore: ImageVector by lazy {
        icon("NavigateBefore") {
            moveTo(15.41f, 7.41f)
            lineTo(14f, 6f)
            lineToRelative(-6f, 6f)
            lineToRelative(6f, 6f)
            lineToRelative(1.41f, -1.41f)
            lineTo(10.83f, 12f)
            close()
        }
    }

    val NavigateNext: ImageVector by lazy {
        icon("NavigateNext") {
            moveTo(10f, 6f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12f)
            lineToRelative(-4.58f, 4.59f)
            lineTo(10f, 18f)
            lineToRelative(6f, -6f)
            close()
        }
    }

    val Undo: ImageVector by lazy {
        icon("Undo") {
            moveTo(12.5f, 8f)
            curveToRelative(-2.65f, 0f, -5.05f, 0.99f, -6.9f, 2.6f)
            lineTo(2f, 7f)
            verticalLineToRelative(9f)
            horizontalLineToRelative(9f)
            lineToRelative(-3.62f, -3.62f)
            curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
            curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
            lineToRelative(2.37f, -0.78f)
            curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
            close()
        }
    }

    val ZoomIn: ImageVector by lazy {
        icon("ZoomIn") {
            moveTo(15.5f, 14f)
            horizontalLineToRelative(-0.79f)
            lineToRelative(-0.28f, -0.27f)
            curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
            curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
            reflectiveCurveTo(3f, 5.91f, 3f, 9.5f)
            reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
            curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.23f, -1.57f)
            lineToRelative(0.27f, 0.28f)
            verticalLineToRelative(0.79f)
            lineToRelative(5f, 4.99f)
            lineTo(20.49f, 19f)
            lineToRelative(-4.99f, -5f)
            close()
            moveTo(9.5f, 14f)
            curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
            reflectiveCurveTo(7.01f, 5f, 9.5f, 5f)
            reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
            reflectiveCurveTo(11.99f, 14f, 9.5f, 14f)
            close()
            moveTo(12f, 10f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(2f)
            horizontalLineTo(9f)
            verticalLineToRelative(-2f)
            horizontalLineTo(7f)
            verticalLineTo(9f)
            horizontalLineToRelative(2f)
            verticalLineTo(7f)
            horizontalLineToRelative(1f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(1f)
            close()
        }
    }

    val NoteAdd: ImageVector by lazy {
        icon("NoteAdd") {
            moveTo(14f, 2f)
            horizontalLineTo(6f)
            curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
            lineTo(4f, 20f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 1.99f, 2f)
            horizontalLineTo(18f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(8f)
            lineToRelative(-6f, -6f)
            close()
            moveTo(16f, 16f)
            horizontalLineToRelative(-3f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-3f)
            horizontalLineTo(8f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(-3f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(2f)
            close()
        }
    }

    val Save: ImageVector by lazy {
        icon("Save") {
            moveTo(17f, 3f)
            horizontalLineTo(5f)
            curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(7f)
            lineToRelative(-4f, -4f)
            close()
            moveTo(12f, 19f)
            curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
            reflectiveCurveToRelative(1.34f, -3f, 3f, -3f)
            reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
            reflectiveCurveToRelative(-1.34f, 3f, -3f, 3f)
            close()
            moveTo(15f, 9f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(4f)
            close()
        }
    }

    val OpenWith: ImageVector by lazy {
        icon("OpenWith") {
            moveTo(10f, 9f)
            horizontalLineToRelative(4f)
            verticalLineTo(6f)
            horizontalLineToRelative(3f)
            lineToRelative(-5f, -5f)
            lineToRelative(-5f, 5f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(3f)
            close()
            moveTo(9f, 10f)
            horizontalLineTo(6f)
            verticalLineTo(7f)
            lineToRelative(-5f, 5f)
            lineToRelative(5f, 5f)
            verticalLineToRelative(-3f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(-4f)
            close()
            moveTo(23f, 12f)
            lineToRelative(-5f, -5f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(-3f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(3f)
            lineToRelative(5f, -5f)
            close()
            moveTo(14f, 15f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(3f)
            horizontalLineTo(7f)
            lineToRelative(5f, 5f)
            lineToRelative(5f, -5f)
            horizontalLineToRelative(-3f)
            verticalLineToRelative(-3f)
            close()
        }
    }

    val TextFields: ImageVector by lazy {
        icon("TextFields") {
            moveTo(2.5f, 4f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(3f)
            verticalLineTo(7f)
            horizontalLineToRelative(5f)
            verticalLineTo(4f)
            horizontalLineToRelative(-13f)
            close()
            moveTo(21.5f, 9f)
            horizontalLineToRelative(-9f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(7f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(-7f)
            horizontalLineToRelative(3f)
            verticalLineTo(9f)
            close()
        }
    }

    val AddPhotoAlternate: ImageVector by lazy {
        icon("AddPhotoAlternate") {
            moveTo(19f, 7f)
            verticalLineToRelative(2.99f)
            horizontalLineToRelative(-2f)
            verticalLineTo(7f)
            horizontalLineToRelative(-3f)
            verticalLineTo(5f)
            horizontalLineToRelative(3f)
            verticalLineTo(2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(-3f)
            close()
            moveTo(16f, 11f)
            verticalLineToRelative(8f)
            horizontalLineTo(4f)
            verticalLineTo(7f)
            horizontalLineToRelative(8f)
            verticalLineTo(5f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(12f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-8f)
            horizontalLineToRelative(-2f)
            close()
            moveTo(7f, 17f)
            lineToRelative(2.25f, -3f)
            lineToRelative(1.75f, 2.26f)
            lineToRelative(2.5f, -3.26f)
            lineTo(15f, 17f)
            horizontalLineTo(7f)
            close()
        }
    }

    val CropFree: ImageVector by lazy {
        icon("CropFree") {
            moveTo(3f, 5f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(2f)
            verticalLineTo(5f)
            horizontalLineToRelative(4f)
            verticalLineTo(3f)
            horizontalLineTo(5f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            close()
            moveTo(5f, 15f)
            horizontalLineTo(3f)
            verticalLineToRelative(4f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-2f)
            horizontalLineTo(5f)
            verticalLineToRelative(-4f)
            close()
            moveTo(19f, 19f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(4f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(4f)
            close()
            moveTo(19f, 3f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
        }
    }

    val BorderColor: ImageVector by lazy {
        icon("BorderColor") {
            moveTo(17.75f, 7f)
            lineTo(14f, 3.25f)
            lineToRelative(-10f, 10f)
            verticalLineTo(17f)
            horizontalLineToRelative(3.75f)
            lineToRelative(10f, -10f)
            close()
            moveTo(2f, 20f)
            horizontalLineToRelative(20f)
            verticalLineToRelative(3f)
            horizontalLineTo(2f)
            verticalLineToRelative(-3f)
            close()
        }
    }

    val Category: ImageVector by lazy {
        icon("Category") {
            moveTo(12f, 2f)
            lineToRelative(-5.5f, 9f)
            horizontalLineToRelative(11f)
            close()
            moveTo(17.5f, 13f)
            curveToRelative(-2.49f, 0f, -4.5f, 2.01f, -4.5f, 4.5f)
            reflectiveCurveToRelative(2.01f, 4.5f, 4.5f, 4.5f)
            reflectiveCurveToRelative(4.5f, -2.01f, 4.5f, -4.5f)
            reflectiveCurveToRelative(-2.01f, -4.5f, -4.5f, -4.5f)
            close()
            moveTo(3f, 21.5f)
            horizontalLineToRelative(8f)
            verticalLineToRelative(-8f)
            horizontalLineTo(3f)
            verticalLineToRelative(8f)
            close()
        }
    }
}
