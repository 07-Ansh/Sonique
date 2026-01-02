package com.sonique.kotlinytmusicscraper.models.youtube

import com.mohamedrejeb.ksoup.entities.KsoupEntities
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("transcript")
data class Transcript(
    val text: List<Text>,
) {
    @Serializable
    @SerialName("text")
    data class Text(
        val start: String,
        val dur: String,
        @XmlValue(true) val content: String,
    )
}

fun Transcript.tryDecodeText(): Transcript {
    return copy(
        text = this.text.map {
            it.copy(
                content = KsoupEntities.decodeHtml(it.content)
            )
        }
    )
}

