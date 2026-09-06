package com.sonique.app.ui.component.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CaptureController internal constructor(
    internal val graphicsLayer: GraphicsLayer,
) {
    private val requests = Channel<CaptureRequest>(capacity = Channel.UNLIMITED)
    internal val captureRequests = requests.receiveAsFlow()

    fun captureAsync(): Deferred<ImageBitmap> {
        val deferred = CompletableDeferred<ImageBitmap>()
        requests.trySend(CaptureRequest(deferred))
        return deferred
    }

    internal class CaptureRequest(
        val imageBitmapDeferred: CompletableDeferred<ImageBitmap>,
    )
}

@Composable
fun rememberCaptureController(): CaptureController {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { CaptureController(graphicsLayer) }
}

fun Modifier.capturable(controller: CaptureController): Modifier = this then CapturableModifierNodeElement(controller)

private data class CapturableModifierNodeElement(
    private val controller: CaptureController,
) : ModifierNodeElement<CapturableModifierNode>() {
    override fun create(): CapturableModifierNode = CapturableModifierNode(controller)

    override fun update(node: CapturableModifierNode) {
        node.updateController(controller)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "capturable"
        properties["controller"] = controller
    }
}

private class CapturableModifierNode(
    controller: CaptureController,
) : Modifier.Node(),
    DrawModifierNode {
    private val currentController = MutableStateFlow(controller)

    private val currentGraphicsLayer get() = currentController.value.graphicsLayer

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch { serveCaptureRequests() }
    }

    fun updateController(newController: CaptureController) {
        currentController.value = newController
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun serveCaptureRequests() {
        currentController
            .flatMapLatest { it.captureRequests }
            .collect { request ->
                val deferred = request.imageBitmapDeferred
                try {
                    deferred.complete(currentGraphicsLayer.toImageBitmap())
                } catch (error: Throwable) {
                    deferred.completeExceptionally(error)
                }
            }
    }

    override fun ContentDrawScope.draw() {
        currentGraphicsLayer.record { this@draw.drawContent() }
        drawLayer(currentGraphicsLayer)
    }
}
