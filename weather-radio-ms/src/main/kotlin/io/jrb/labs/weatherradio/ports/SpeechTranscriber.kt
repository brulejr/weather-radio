package io.jrb.labs.weatherradio.ports

import io.jrb.labs.weatherradio.domain.TranscriptSegment

interface SpeechTranscriber {
    fun latestSegment(): TranscriptSegment?
}
