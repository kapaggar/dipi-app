package org.dhamma.dipi.staff.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.PhotoCrop
import org.dhamma.dipi.staff.model.PhotoGeometry
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import kotlin.math.roundToInt

@Composable
fun PhotoEditor(
    editor: PhotoEditorUi,
    onAction: (PhotoReviewAction) -> Unit,
    loadOriginal: suspend (Int) -> ImageBitmap?,
    loadCorrected: suspend (Int) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val c = LocalDipi.current
    val original by produceState<ImageBitmap?>(null, editor.applicantId, editor.recipe) {
        value = loadOriginal(editor.applicantId)
    }
    val corrected by produceState<ImageBitmap?>(null, editor.applicantId, editor.recipe) {
        value = if (editor.compareOriginal) original else loadCorrected(editor.applicantId)
    }
    val shown = if (editor.compareOriginal) original else corrected ?: original
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(editor.name, fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .border(1.dp, c.hairlineStrong)
                .background(c.field)
                .testTag("photo-editor-preview"),
            contentAlignment = Alignment.Center,
        ) {
            when {
                editor.missing -> Text("No photo", color = c.muted)
                editor.unsupported -> Text("Unsupported image", color = c.hard)
                shown != null -> {
                    Image(
                        bitmap = shown,
                        contentDescription = "Working photo of ${editor.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    if (!editor.compareOriginal && editor.sourceWidth > 0) {
                        CropFrame(
                            recipeCrop = editor.recipe.crop,
                            rotatedW = PhotoGeometry.rotatedSize(
                                editor.sourceWidth,
                                editor.sourceHeight,
                                editor.recipe.clockwise,
                            ).first,
                            rotatedH = PhotoGeometry.rotatedSize(
                                editor.sourceWidth,
                                editor.sourceHeight,
                                editor.recipe.clockwise,
                            ).second,
                            onCrop = { onAction(PhotoReviewAction.SetCrop(it)) },
                        )
                    }
                }
                else -> Text("Loading", color = c.muted)
            }
        }
        EditorActions(editor, onAction)
    }
}

@Composable
private fun EditorActions(editor: PhotoEditorUi, onAction: (PhotoReviewAction) -> Unit) {
    val c = LocalDipi.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorButton("Rotate left", enabled = !editor.missing) {
                onAction(PhotoReviewAction.Rotate(-90))
            }
            EditorButton("Rotate right", enabled = !editor.missing) {
                onAction(PhotoReviewAction.Rotate(90))
            }
            EditorButton("Rotate 180", enabled = !editor.missing) {
                onAction(PhotoReviewAction.Rotate(180))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorButton("Undo", enabled = editor.canUndo) { onAction(PhotoReviewAction.Undo) }
            EditorButton("Reset", enabled = !editor.missing) { onAction(PhotoReviewAction.Reset) }
            EditorButton(
                if (editor.compareOriginal) "Corrected" else "Original",
                enabled = !editor.missing,
            ) { onAction(PhotoReviewAction.ToggleCompare) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onAction(PhotoReviewAction.KeepAndNext) },
                enabled = !editor.missing && !editor.unsupported,
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f),
            ) { Text("Keep & next") }
            OutlinedButton(
                onClick = { onAction(PhotoReviewAction.SaveDraft) },
                enabled = !editor.missing && !editor.unsupported,
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f),
            ) { Text("Save draft") }
        }
        TextButton(
            onClick = { onAction(PhotoReviewAction.CloseEditor) },
            modifier = Modifier.height(48.dp),
        ) { Text("Cancel", color = c.muted) }
    }
}

@Composable
private fun EditorButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 48.dp)
            .semantics { contentDescription = label },
    ) { Text(label, fontSize = 13.sp) }
}

@Composable
private fun CropFrame(
    recipeCrop: PhotoCrop?,
    rotatedW: Int,
    rotatedH: Int,
    onCrop: (PhotoCrop) -> Unit,
) {
    if (rotatedW <= 0 || rotatedH <= 0) return
    val c = LocalDipi.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val image = fittedRect(boxW, boxH, rotatedW.toFloat(), rotatedH.toFloat())
        val crop = recipeCrop ?: maxPortraitCrop(rotatedW, rotatedH)
        val left = image.left + image.width * crop.x / rotatedW
        val top = image.top + image.height * crop.y / rotatedH
        val width = image.width * crop.width / rotatedW
        val height = image.height * crop.height / rotatedH
        Box(
            Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(width.toDpSafe(), height.toDpSafe())
                .border(2.dp, c.accent)
                .pointerInput(crop, rotatedW, rotatedH, image) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onCrop(panZoomCrop(crop, pan, zoom, image, rotatedW, rotatedH))
                    }
                }
                .semantics { contentDescription = "Crop frame" },
        )
        Handle("Crop handle top-left", left, top)
        Handle("Crop handle bottom-right", left + width - 48f, top + height - 48f)
    }
}

@Composable
private fun Handle(label: String, x: Float, y: Float) {
    val c = LocalDipi.current
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(48.dp)
            .border(1.dp, c.accent)
            .semantics { contentDescription = label },
    )
}

private data class Fitted(val left: Float, val top: Float, val width: Float, val height: Float)

private fun fittedRect(boxW: Float, boxH: Float, imgW: Float, imgH: Float): Fitted {
    val scale = minOf(boxW / imgW, boxH / imgH)
    val w = imgW * scale
    val h = imgH * scale
    return Fitted((boxW - w) / 2f, (boxH - h) / 2f, w, h)
}

private fun maxPortraitCrop(width: Int, height: Int): PhotoCrop {
    val ratio = 13.0 / 14.0
    var h = height
    var w = (h * ratio).roundToInt()
    if (w > width) {
        w = width
        h = (w / ratio).roundToInt().coerceAtLeast(1)
    }
    val x = ((width - w) / 2).coerceAtLeast(0)
    val y = ((height - h) / 2).coerceAtLeast(0)
    return PhotoCrop(x, y, w.coerceAtLeast(1), h.coerceAtLeast(1))
}

private fun panZoomCrop(
    crop: PhotoCrop,
    pan: Offset,
    zoom: Float,
    image: Fitted,
    rotatedW: Int,
    rotatedH: Int,
): PhotoCrop {
    val sx = if (image.width == 0f) 1f else rotatedW / image.width
    val sy = if (image.height == 0f) 1f else rotatedH / image.height
    var w = (crop.width * zoom).roundToInt().coerceIn(1, rotatedW)
    var h = (w * 14.0 / 13.0).roundToInt().coerceIn(1, rotatedH)
    if (h > rotatedH) {
        h = rotatedH
        w = (h * 13.0 / 14.0).roundToInt().coerceIn(1, rotatedW)
    }
    val x = (crop.x + pan.x * sx).roundToInt().coerceIn(0, (rotatedW - w).coerceAtLeast(0))
    val y = (crop.y + pan.y * sy).roundToInt().coerceIn(0, (rotatedH - h).coerceAtLeast(0))
    return PhotoCrop(x, y, w, h)
}

@Composable
private fun Float.toDpSafe() = (this / androidx.compose.ui.platform.LocalDensity.current.density).dp
