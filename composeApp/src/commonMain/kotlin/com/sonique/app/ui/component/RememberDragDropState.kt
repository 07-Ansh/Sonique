package com.sonique.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
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
import androidx.compose.ui.unit.IntOffset
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
 * @param itemKeys Lambda providing the current list of item keys in logical order.
 * @param onMove Callback triggered in real-time as items swap during dragging.
 * @param onDrop Callback triggered when the item is released to commit the final position to the backend.
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    minDragIndex: Int = 0,
    itemKeys: () -> List<Any> = { emptyList() },
    onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onDrop: (fromIndex: Int, toIndex: Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val state = remember(lazyListState, minDragIndex) {
        DragDropState(
            state = lazyListState,
            scope = scope,
            minDragIndex = minDragIndex,
            itemKeys = itemKeys,
            onMove = onMove,
            onDrop = onDrop,
            haptic = haptic,
        )
    }
    state.itemKeys = itemKeys
    return state
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
 * scale, translation during drag, drop settling animation, and silky-smooth [animateItem] for neighboring items.
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
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "dragItemScale",
    )

    val zIndex = if (isDragging || isSettling) 2f else 0f

    Box(
        modifier = Modifier
            .animateItem(
                fadeInSpec = null,
                fadeOutSpec = null,
                placementSpec = if (isDragging || isSettling) null else spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold,
                ),
            )
            .zIndex(zIndex)
            .graphicsLayer {
                this.translationY = when {
                    isDragging -> dragDropState.draggingItemOffset
                    isSettling -> dragDropState.settlingAnimatable?.value ?: dragDropState.settlingOffset.value
                    else -> 0f
                }
                this.scaleX = scale
                this.scaleY = scale
                this.shadowElevation = if (isDragging) 12f else 0f
            }
            .then(modifier),
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
    internal var itemKeys: () -> List<Any> = { emptyList() },
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    private val onDrop: (fromIndex: Int, toIndex: Int) -> Unit,
    private val haptic: HapticFeedback? = null,
) {
    var draggedItemKey by mutableStateOf<Any?>(null)
        private set
    var initialIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    var currentLogicalIndex by mutableStateOf<Int?>(null)
        private set

    val currentIndexOfDraggedItem: Int?
        get() = currentLogicalIndex

    var settlingItemKey by mutableStateOf<Any?>(null)
        private set
    var settlingItemIndex by mutableStateOf<Int?>(null)
        private set

    var settlingAnimatable by mutableStateOf<Animatable<Float, AnimationVector1D>?>(null)
        private set
    val settlingOffset = Animatable(0f)

    private var draggedDistance by mutableFloatStateOf(0f)
    private var initialItemOffset by mutableIntStateOf(0)
    private var currentPointerY by mutableFloatStateOf(0f)
    private var scrollVelocity by mutableFloatStateOf(0f)

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
            currentLogicalIndex = hitItem.index
            draggedDistance = 0f
            currentPointerY = offset.y
            scrollVelocity = 0f
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun onDrag(offset: Offset) {
        if (draggedItemKey == null) return
        draggedDistance += offset.y
        currentPointerY += offset.y

        checkForSwap()
        updateOverScroll()
    }

    private fun checkForSwap() {
        val key = draggedItemKey ?: return
        val currentItem = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        val currentIdx = currentLogicalIndex ?: currentItem.index

        // Ensure layout pass has matched currentLogicalIndex before checking next swap,
        // preventing stale index collisions and rapid oscillation.
        if (currentItem.index != currentIdx) {
            return
        }

        val draggedCenter = currentItem.offset + draggingItemOffset + currentItem.size / 2f
        val currentCenter = currentItem.offset + currentItem.size / 2f
        val hysteresisPx = (currentItem.size * 0.10f).coerceIn(6f, 14f)
        val keys = itemKeys()

        // Check downward move (towards next item)
        val nextItem = if (keys.isNotEmpty()) {
            val nextKey = keys.getOrNull(currentIdx + 1)
            if (nextKey != null) state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == nextKey } else null
        } else {
            state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIdx + 1 }
        }

        if (nextItem != null) {
            val nextCenter = nextItem.offset + nextItem.size / 2f
            val midpointDown = (currentCenter + nextCenter) / 2f
            if (draggedCenter > midpointDown + hysteresisPx) {
                val nextIdx = currentIdx + 1
                currentLogicalIndex = nextIdx
                onMove(currentIdx, nextIdx)
                haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                return
            }
        }

        // Check upward move (towards previous item)
        if (currentIdx - 1 >= minDragIndex) {
            val prevItem = if (keys.isNotEmpty()) {
                val prevKey = keys.getOrNull(currentIdx - 1)
                if (prevKey != null) state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == prevKey } else null
            } else {
                state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIdx - 1 }
            }

            if (prevItem != null) {
                val prevCenter = prevItem.offset + prevItem.size / 2f
                val midpointUp = (currentCenter + prevCenter) / 2f
                if (draggedCenter < midpointUp - hysteresisPx) {
                    val prevIdx = currentIdx - 1
                    currentLogicalIndex = prevIdx
                    onMove(currentIdx, prevIdx)
                    haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    return
                }
            }
        }
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

    private fun updateOverScroll() {
        if (draggedItemKey == null) {
            stopOverScroll()
            return
        }

        val layoutInfo = state.layoutInfo
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        if (viewportHeight <= 0f) return

        val edgeThreshold = 140f
        val maxScrollSpeed = 24f

        val topBound = layoutInfo.viewportStartOffset + edgeThreshold
        val bottomBound = layoutInfo.viewportEndOffset - edgeThreshold

        val velocity = when {
            currentPointerY < topBound && state.canScrollBackward -> {
                val ratio = ((topBound - currentPointerY) / edgeThreshold).coerceIn(0f, 1f)
                -ratio * maxScrollSpeed
            }
            currentPointerY > bottomBound && state.canScrollForward -> {
                val ratio = ((currentPointerY - bottomBound) / edgeThreshold).coerceIn(0f, 1f)
                ratio * maxScrollSpeed
            }
            else -> 0f
        }

        scrollVelocity = velocity

        if (velocity != 0f) {
            if (overscrollJob?.isActive != true) {
                startOverScrollJob()
            }
        } else {
            stopOverScroll()
        }
    }

    private fun startOverScrollJob() {
        overscrollJob?.cancel()
        overscrollJob = scope.launch {
            while (isActive && draggedItemKey != null && scrollVelocity != 0f) {
                val delta = scrollVelocity
                val consumed = state.scrollBy(delta)
                if (consumed != 0f) {
                    checkForSwap()
                    updateOverScroll()
                } else {
                    stopOverScroll()
                    break
                }
                delay(16)
            }
        }
    }

    private fun stopOverScroll() {
        overscrollJob?.cancel()
        overscrollJob = null
        scrollVelocity = 0f
    }

    fun onDragEnd() {
        stopOverScroll()
        val key = draggedItemKey
        val initial = initialIndexOfDraggedItem
        val current = currentLogicalIndex

        if (initial != null && current != null && initial != current) {
            onDrop(initial, current)
        }

        if (key != null) {
            val dropOffset = draggingItemOffset
            val animatable = Animatable(dropOffset)
            settlingAnimatable = animatable
            settlingItemKey = key
            settlingItemIndex = current

            draggedItemKey = null
            initialIndexOfDraggedItem = null
            currentLogicalIndex = null
            draggedDistance = 0f
            initialItemOffset = 0
            currentPointerY = 0f
            scrollVelocity = 0f

            scope.launch {
                animatable.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
                settlingItemKey = null
                settlingItemIndex = null
                settlingAnimatable = null
            }
        } else {
            resetState()
        }
    }

    fun onDragCancel() {
        stopOverScroll()
        resetState()
    }

    private fun resetState() {
        draggedItemKey = null
        settlingItemKey = null
        settlingItemIndex = null
        settlingAnimatable = null
        draggedDistance = 0f
        initialItemOffset = 0
        currentLogicalIndex = null
        initialIndexOfDraggedItem = null
        currentPointerY = 0f
        scrollVelocity = 0f
    }
}
