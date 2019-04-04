package audiotest

/**
 * A listener that gets called when an [AudioInput] has captured some audio samples.
 */
interface AudioInputListener {
    /**
     * This method will get called whenever a new set of audio samples was captured.
     */
    fun audioFrameCaptured(data: ByteArray)
}