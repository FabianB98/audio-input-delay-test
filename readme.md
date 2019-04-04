# Audio Input Delay Test

A very basic and stripped down example for testing the appearance of a sudden delay while capturing audio using the
javax.sound.sampled API when recording audio samples for multiple hours. Created for my Stackoverflow Question
[Sudden delay while recording audio over long time periods inside the JVM](https://stackoverflow.com/questions/55482552/sudden-delay-while-recording-audio-over-long-time-periods-inside-the-jvm).

### Summary of the issue I'm experiencing

After continuously recording audio samples for multiple hours, a sudden delay of roughly one to two seconds is
introduced. Before this point there is no noticeable delay. For a more detailed description of the issue and what I've
already tried, see the linked [Stackoverflow Question](https://stackoverflow.com/questions/55482552/sudden-delay-while-recording-audio-over-long-time-periods-inside-the-jvm)
in case you didn't come from there.

### How to run

Start executing the main method inside the Main.kt. The application will display all possible audio input devices that
it found. You will be prompted to enter the index of the audio input device to use. After this, the application will
show a list of supported audio formats from which you will need to select one (for me the issue starts occurring quicker
if I choose a format with a higher frame size). In case the selected audio format doesn't specify a sample rate, you
will be prompted to enter one. Finally you will need to enter the buffer size (in audio frames) to use for recording
audio samples.

The application will now start capturing audio samples and display their RMS value. If you make a loud sound (e.g.
clapping your hands in front of the selected microphone) you should see this value increasing. If you make no sounds at
all, you should see a smaller value representing the background hiss of the selected microphone. Directly after the 
start you should see that there is an unnoticeable or only very small delay between your clap and the higher RMS values.
After letting the application run for multiple hours, you might see that there is a definitely noticeable delay (at 
least that's what happens on both machines I've tried running the application on).

By pressing enter you can pause the output of the RMS values to enter "flush" (flushes the TargetDataLines internal
buffer), "restart" (stops and starts the line), "reopen" (Stops, closes, reopens and restarts the line), "stop" (Exits
the application) or "quit" (also exits the application) and confirm your input by pressing enter again.

### Closing notes

This test project was made to be as small as possible to reduce the possible amount of problem sources and to make it as
understandable as possible. By no means is it supposed to feature any signs of a good software design. Furthermore it 
only handles the most critical exceptions that might occur during runtime.