package audiotest

import javax.sound.sampled.*
import kotlin.math.min

/**
 * Represents an audio input device.
 */
class AudioInput(val mixer: Mixer, val line: TargetDataLine) {
    /**
     * Gets all supported [AudioFormat]s for this audio input device.
     */
    fun getSupportedAudioFormats(): Array<AudioFormat> = (line.lineInfo as DataLine.Info).formats

    /**
     * Opens the audio input device with the given [format] and the given [bufferSize]. [bufferSize] might be negative
     * if the lines default buffer size should be used.
     */
    fun open(format: AudioFormat, bufferSize: Int = -1) {
        val bufferSizeInBytes = bufferSize * format.frameSize
        if (!line.isOpen) {
            //Open the line.
            if (bufferSize > 0)
                line.open(format, bufferSizeInBytes)
            else
                line.open(format)
        }

        //Check if the buffer size was set correctly.
        if (bufferSize > 0 && line.bufferSize != bufferSizeInBytes)
            System.err.println(
                "Couldn't set the buffer size to the desired $bufferSizeInBytes bytes! Actual buffer " +
                        "size is ${line.bufferSize} bytes instead..."
            )
    }

    /**
     * Closes the audio input device.
     */
    fun close() {
        line.close()
    }

    /**
     * Starts the audio input device, so it may engage in data I/O.
     */
    fun start() {
        line.start()
    }

    /**
     * Stops the audio input device.
     */
    fun stop() {
        line.stop()
    }

    /**
     * Flushes the audio input devices internal buffer.
     */
    fun flush() {
        line.flush()
    }

    override fun toString(): String = mixer.mixerInfo.name

    protected fun finalize() {
        stop()
        close()
    }

    /**
     * A thread that continuously captures audio samples in packets of [sampleSize] audio frames until interrupted.
     */
    inner class AudioCaptureThread(val listener: AudioInputListener, val sampleSize: Int) : Thread() {
        private var running = true

        init {
            start()
        }

        override fun run() {
            //Create a buffer for the audio samples.
            val bytesToRead = sampleSize * line.format.frameSize
            val data = ByteArray(bytesToRead)

            while (!isInterrupted && running) {
                //Read the next set of audio samples.
                var bytesRead = 0
                while (bytesRead < bytesToRead) {
                    bytesRead += line.read(data, bytesRead, min(bytesToRead, bytesToRead - bytesRead))
                }

                //Update the listener.
                listener.audioFrameCaptured(data)
            }
        }

        override fun interrupt() {
            super.interrupt()
            running = false
        }
    }

    companion object {
        /**
         * Gets a list of all available [AudioInput]s.
         */
        fun getAudioInputs(): ArrayList<AudioInput> {
            val result = ArrayList<AudioInput>()

            //Iterate over all available Mixers.
            for (info in AudioSystem.getMixerInfo()) {
                val mixer = try {
                    AudioSystem.getMixer(info)
                } catch (e: SecurityException) {
                    System.err.println("Couldn't access Mixer \"${info.name}\" due to security restrictions!")
                    e.printStackTrace()
                    continue
                }

                //Iterate over all available TargetDataLines of the current Mixer.
                for (lineInfo in mixer.targetLineInfo) {
                    val line = try {
                        mixer.getLine(lineInfo)
                    } catch (e: LineUnavailableException) {
                        System.err.println(
                            "Couldn't get the TargetDataLine for Mixer \"${info.name}\" as it is " +
                                    "currently unavailable!"
                        )
                        e.printStackTrace()
                        continue
                    } catch (e: SecurityException) {
                        System.err.println(
                            "Couldn't get the TargetDataLine for Mixer \"${info.name}\" due to " +
                                    "security restrictions!"
                        )
                        e.printStackTrace()
                        continue
                    }

                    if (line is TargetDataLine)
                        result.add(AudioInput(mixer, line))
                }
            }

            return result
        }
    }
}