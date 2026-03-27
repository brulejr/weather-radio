package io.jrb.labs.weatherradio.service

import io.jrb.labs.weatherradio.domain.RadioSignalStatus
import io.jrb.labs.weatherradio.domain.SameMessage
import io.jrb.labs.weatherradio.domain.TranscriptSegment
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class InMemoryRadioStateRepository {

    private val radioStatusRef = AtomicReference<RadioSignalStatus?>()
    private val sameMessageRef = AtomicReference<SameMessage?>()
    private val transcriptRef = AtomicReference<TranscriptSegment?>()

    fun radioStatus(): RadioSignalStatus? = radioStatusRef.get()
    fun latestSameMessage(): SameMessage? = sameMessageRef.get()
    fun latestTranscript(): TranscriptSegment? = transcriptRef.get()

    fun updateRadioStatus(status: RadioSignalStatus) {
        radioStatusRef.set(status)
    }

    fun updateSameMessage(message: SameMessage) {
        sameMessageRef.set(message)
    }

    fun updateTranscript(segment: TranscriptSegment) {
        transcriptRef.set(segment)
    }
}
