package io.jrb.labs.weatherradio.ports

import io.jrb.labs.weatherradio.domain.SameMessage

interface SameDecoder {
    fun latestMessage(): SameMessage?
}
