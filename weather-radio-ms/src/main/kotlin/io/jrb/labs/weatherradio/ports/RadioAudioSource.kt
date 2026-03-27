package io.jrb.labs.weatherradio.ports

interface RadioAudioSource {
    fun start()
    fun stop()
    fun isRunning(): Boolean
}
