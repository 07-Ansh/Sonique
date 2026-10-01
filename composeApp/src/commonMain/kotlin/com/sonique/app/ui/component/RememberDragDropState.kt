package com.sonique.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.sonique.domain.data.model.browse.album.Track

/**
 * Encapsulates a track in a queue with a persistent unique key across reordering operations.
 */
data class QueueDisplayItem(
    val stableKey: String,
    val originalTrack: Track,
)

/**
 * Creates and remembers a [DragDropState] for a [LazyColumn].
 *
 * @param lazyListState The state of the LazyColumn.
 * @param minDragIndex The minimum index that can be dragged or swapped (e.g. 1 to keep index 0 pinned).
 * @param onMove Callback triggered in real-time as items swap during dragging.
 * @param onDrop Callback triggered when the item is released to commit the final position to the backend.
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    minDragIndex: Int = 0,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onDrop: (fromIndex: Int, toIndex: Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    return remember(lazyListState, minDragIndex) {
        DragDropState(
            state = lazyListState,
            scope = scope,
            minDragIndex = minDragIndex,
            onMove = onMove,
            onDrop = onDrop,
            haptic = haptic,
        )
    }
}

/**
 * Attaches long-press drag & drop reorder gesture handling to a [LazyColumn].
 */
fun Modifier.dragDropList(dragDropState: DragDropState): Modifier = this.pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            dragDropState.onDragStart(offset)
        },
        onDrag = { change, dragAmount ->
            change.consume()
            dragDropState.onDrag(dragAmount)
        },
        onDragEnd = {
            dragDropState.onDragEnd()
        },
        onDragCancel = {
            dragDropState.onDragCancel()
        },
    )
}

/**
 * Wraps a list item inside a reorderable [LazyColumn] using its persistent [key] to handle lift elevation,
 * scale, translation during drag, drop settling animation, and [animateItem] for neighboring items.
 */
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isDragging: Boolean) -> Unit,
) {
    val isDragging = key == dragDropState.draggedItemKey || (key is Int && key == dragDropState.currentIndexOfDraggedItem)
    val isSettling = key == dragDropState.settlingItemKey || (key is Int && key == dragDropState.settlingItemIndex)

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "dragItemScale",
    )

    val translationY = when {
        isDragging -> dragDropState.draggingItemOffset
        isSettling -> dragDropState.settlingOffset.value
        else -> 0f
    }

    val zIndex = if (isDragging || isSettling) 2f else 0f

    val animateItemModifier = if (!isDragging && !isSettling) {
        Modifier.animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
            placementSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .zIndex(zIndex)
            .graphicsLayer {
                this.translationY = translationY
                this.scaleX = scale
                this.scaleY = scale
                this.shadowElevation = if (isDragging) 12f else 0f
            }
            .then(animateItemModifier),
    ) {
        content(isDragging)
    }
}

/**
 * Backward-compatible overload for callers passing an [index] instead of a custom key.
 */
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isDragging: Boolean) -> Unit,
) {
    DraggableItem(
        dragDropState = dragDropState,
        key = index as Any,
        modifier = modifier,
        content = content,
    )
}

class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
    private val minDragIndex: Int = 0,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    private val onDrop: (fromIndex: Int, toIndex: Int) -> Unit,
    private val haptic: HapticFeedback? = null,
) {
    var draggedItemKey by mutableStateOf<Any?>(null)
        private set
    var initialIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    var settlingItemKey by mutableStateOf<Any?>(null)
        private set
    var settlingItemIndex by mutableStateOf<Int?>(null)
        private set
    val settlingOffset = Animatable(0f)

    private var draggedDistance by mutableFloatStateOf(0f)
    private var initialItemOffset by mutableIntStateOf(0)

    val draggingItemOffset: Float
        get() {
            val key = draggedItemKey ?: return 0f
            val itemInfo = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
                ?: return draggedDistance
            return initialItemOffset + draggedDistance - itemInfo.offset
        }

    private var overscrollJob by mutableStateOf<Job?>(null)

    fun onDragStart(offset: Offset) {
        val hitItem = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            offset.y.toInt() in item.offset..(item.offset + item.size)
        }

        if (hitItem != null && hitItem.index >= minDragIndex) {
            draggedItemKey = hitItem.key
            initialItemOffset = hitItem.offset
            initialIndexOfDraggedItem = hitItem.index
            currentIndexOfDraggedItem = hitItem.index
            draggedDistance = 0f
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun onDrag(offset: Offset) {
        val key = draggedItemKey ?: return
        draggedDistance += offset.y

        val currentItem = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        if (currentItem != null) {
            val currentCenter = currentItem.offset + draggingItemOffset + currentItem.size / 2f
            val currentIndex = currentItem.index

            // Find target item whose center was crossed
            val targetItem = if (draggedDistance > 0) {
                // Moving down: find furthest item below whose center has been passed
                state.layoutInfo.visibleItemsInfo
                    .filter { it.index > currentIndex && it.index >= minDragIndex && currentCenter > (it.offset + it.size / 2f) }
                    .maxByOrNull { it.index }
            } else {
                // Moving up: find furthest item above whose center has been passed
                state.layoutInfo.visibleItemsInfo
                    .filter { it.index < currentIndex && it.index >= minDragIndex && currentCenter < (it.offset + it.size / 2f) }
                    .minByOrNull { it.index }
            }

            if (targetItem != null) {
                val targetIndex = targetItem.index
                currentIndexOfDraggedItem = targetIndex
                onMove(currentIndex, targetIndex)
                haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        handleOverScroll()
    }

    /**
     * Backward-compatibility hook for existing callers that manually handle drag completion.
     */
    fun onDragInterrupted(end: Boolean = false) {
        if (end) onDragEnd() else onDragCancel()
    }

    /**
     * Backward-compatibility hook returning 0f since overscroll is handled internally.
     */
    fun checkForOverScroll(): Float = 0f

    private fun handleOverScroll() {
        val key = draggedItemKey ?: run {
            overscrollJob?.cancel()
            return
        }
        val currentItem = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        val currentTop = currentItem.offset + draggingItemOffset
        val currentBottom = currentTop + currentItem.size

        val viewportStart = state.layoutInfo.viewportStartOffset
        val viewportEnd = state.layoutInfo.viewportEndOffset
        val edgeThreshold = 180f // Threshold from edge in px

        val scrollDelta = when {
            currentBottom > viewportEnd - edgeThreshold -> {
                val proximity = (currentBottom - (viewportEnd - edgeThreshold)).coerceIn(0f, edgeThreshold)
                (proximity / edgeThreshold) * 22f
            }
            currentTop < viewportStart + edgeThreshold && currentItem.index > minDragIndex -> {
                val proximity = ((viewportStart + edgeThreshold) - currentTop).coerceIn(0f, edgeThreshold)
                -(proximity / edgeThreshold) * 22f
            }
            else -> 0f
        }

        if (scrollDelta != 0f) {
            if (overscrollJob?.isActive != true) {
                overscrollJob = scope.launch {
                    while (isActive && draggedItemKey != null) {
                        state.scrollBy(scrollDelta)
                        delay(16)
                    }
                }
            }
        } else {
            overscrollJob?.cancel()
        }
    }

    fun onDragEnd() {
        overscrollJob?.cancel()
        val key = draggedItemKey
        val initial = initialIndexOfDraggedItem
        val current = currentIndexOfDraggedItem

        if (initial != null && current != null && initial != current) {
            onDrop(initial, current)
        }

        if (key != null) {
            val dropOffset = draggingItemOffset
            settlingItemKey = key
            settlingItemIndex = current
            draggedItemKey = null
            initialIndexOfDraggedItem = null
            currentIndexOfDraggedItem = null
            draggedDistance = 0f
            initialItemOffset = 0

            scope.launch {
                settlingOffset.snapTo(dropOffset)
                settlingOffset.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                settlingItemKey = null
                settlingItemIndex = null
            }
        } else {
            draggedItemKey = null
            settlingItemKey = null
            settlingItemIndex = null
            draggedDistance = 0f
            initialItemOffset = 0
            currentIndexOfDraggedItem = null
            initialIndexOfDraggedItem = null
        }
    }

    fun onDragCancel() {
        overscrollJob?.cancel()
        draggedItemKey = null
        settlingItemKey = null
        settlingItemIndex = null
        draggedDistance = 0f
        initialItemOffset = 0
        currentIndexOfDraggedItem = null
        initialIndexOfDraggedItem = null
    }
}
