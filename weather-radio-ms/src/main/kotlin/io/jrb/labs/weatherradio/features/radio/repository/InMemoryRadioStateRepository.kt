package io.jrb.labs.weatherradio.features.radio.repository

import io.jrb.labs.weatherradio.domain.RadioSignalStatus
import java.util.concurrent.atomic.AtomicReference


class InMemoryRadioStateRepository : RadioStateRepository {

    private val radioStatusRef = AtomicReference<RadioSignalStatus?>()

    override fun radioStatus(): RadioSignalStatus? = radioStatusRef.get()

    override fun updateRadioStatus(status: RadioSignalStatus) {
        radioStatusRef.set(status)
    }

}
