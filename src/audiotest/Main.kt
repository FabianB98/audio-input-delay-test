package audiotest

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.ceil
import kotlin.math.sqrt

const val SAMPLE_SIZE = 1024

fun main() {
    //Get all audio input devices.
    val inputs = AudioInput.getAudioInputs()

    //Show a list of all audio inputs.
    println("Found the following audio inputs:")
    inputs.forEachIndexed { index, audioInput -> println("  - $index: $audioInput") }
    println()

    //Ask the user to select an input device.
    println("Enter the index of the input device to use...")
    val inputDeviceIndex = readLine()?.toIntOrNull() ?: 0
    val input = inputs[inputDeviceIndex]
    println()

    //Show all supported audio formats of the selected input device.
    val inputFormats = input.getSupportedAudioFormats()
    println("Desired input device supports the following audio formats:")
    inputFormats.forEachIndexed { index, audioFormat -> println("  - $index: $audioFormat") }
    println()

    //Let the user select an audio format.
    println("Enter the index of the audio format to use...")
    val inputFormatIndex = readLine()?.toIntOrNull() ?: 0
    var inputFormat = inputFormats[inputFormatIndex]
    println()

    //Let the user specify a sample rate if the audio format doesn't have one.
    if (inputFormat.sampleRate == AudioSystem.NOT_SPECIFIED.toFloat()) {
        println("Desired audio format doesn't specify a sample rate. Please enter the sample rate to use...")
        val sampleRate = readLine()?.toFloatOrNull() ?: 44100.0f
        inputFormat = AudioFormat(
            inputFormat.encoding,
            sampleRate,
            inputFormat.sampleSizeInBits,
            inputFormat.channels,
            inputFormat.frameSize,
            sampleRate,
            inputFormat.isBigEndian
        )
        println()
    }

    //Let the user specify a buffer size.
    println("Enter the desired buffer size...")
    val bufferSize = readLine()?.toIntOrNull() ?: 4096
    println()

    //Start capturing audio data.
    println("Starting to capture audio samples as soon as you're ready (Press enter to start). To pause the console " +
                "output while the application is capturing audio, press enter again.")
    readLine()
    input.open(inputFormat, bufferSize)
    input.start()
    val listener = object : AudioInputListener {
        @Volatile
        var output: Boolean = true

        private val dataAsInts = IntArray(SAMPLE_SIZE * inputFormat.channels)

        override fun audioFrameCaptured(data: ByteArray) {
            if (output) {
                //Calculate and print the RMS value of the captured audio samples.
                bytesToInts(data, inputFormat, dataAsInts)
                val quadraticSum = dataAsInts.sumByDouble { it.toDouble() * it.toDouble() }
                val rms = sqrt(quadraticSum / dataAsInts.size)
                println("RMS: $rms")
            }
        }
    }
    val thread = input.AudioCaptureThread(listener, SAMPLE_SIZE)

    //Handle user inputs.
    loop@
    while (true) {
        readLine()
        listener.output = false
        println("Output paused. Enter \"flush\", \"restart\", \"reopen\", \"stop\", \"quit\" or nothing...")

        val line = readLine()?.trim()
        when (line) {
            "flush" -> {
                println("Flushing audio input...")
                input.flush()
            }
            "restart" -> {
                println("Restarting audio input...")
                input.stop()
                input.start()
            }
            "reopen" -> {
                println("Reopening audio input...")
                input.stop()
                input.close()
                input.open(inputFormat, bufferSize)
                input.start()
            }
            "stop", "quit" -> {
                println("Stopping...")
                break@loop
            }
        }

        println("Output unpaused.")
        listener.output = true
    }

    //Clean up.
    thread.interrupt()
    thread.join()
    input.stop()
    input.close()
}

/**
 * Converts some given data in the form of a byte array to an integer array representing the same data.
 *
 * @param data The byte array containing the data to transform.
 * @param format the [AudioFormat] of the given [data].
 * @param result the array in which the result should be stored.
 */
private fun bytesToInts(data: ByteArray, format: AudioFormat, result: IntArray) {
    //Determine the amount of bytes per integer, the byte order and the offset for the conversion.
    val bytesPerInt = ceil(format.sampleSizeInBits.toFloat() / 8.0f).toInt()
    val order = if (format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
    val srcPos = if (format.isBigEndian) 4 - bytesPerInt else 0

    //Determine the formula to use for getting the most significant (i.e. highest order) byte of a number.
    val getHighestByte = if (format.isBigEndian)
        { intIndex: Int -> intIndex * bytesPerInt }
    else
        { intIndex: Int -> (intIndex + 1) * bytesPerInt - 1 }
    val signed = format.encoding == AudioFormat.Encoding.PCM_SIGNED

    //Perform the actual conversion.
    for (i in 0 until data.size / bytesPerInt) {
        //Determine if the highest bit of the is set.
        val highestBit = data[getHighestByte(i)].toInt() and 0x80 != 0

        //Create exactly four bytes that represent the same number.
        val localBytes = ByteArray(4,
            if (highestBit && signed) { _ -> 0xFF.toByte() } else { _ -> 0x00.toByte() })
        for (j in 0 until bytesPerInt)
            localBytes[srcPos + j] = data[i * bytesPerInt + j]

        //Convert the four bytes into an integer.
        result[i] = ByteBuffer.wrap(localBytes).order(order).int
    }
}