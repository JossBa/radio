package de.htw.audioprocessor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * Audio processor demo.
 * @author Sascha Baumeister
 *
 */
public class ProcessorDemo {
	private static final AudioFormat WAV_FORMAT = new AudioFormat(44100, 16, 2, true, false);


	/**
	 * Application entry point.
	 * @param args the runtime arguments
	 * @throws UnsupportedAudioFileException if the given audio file type is unsupported
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IOException, UnsupportedAudioFileException {
		final Path audioSourcePath	= Paths.get(args[0]);
		final Path audioSinkPath	= Paths.get(args[1]);
		final Processor processor = new Volume(Double.parseDouble(args[2]));

		System.out.print("working ... ");
		try (AudioOutputStream audioSink = AudioOutputStream.newAudioOutputStream(WAV_FORMAT, AudioFileFormat.Type.WAVE, audioSinkPath)) {
			try (AudioInputStream audioSource = AudioSystem.getAudioInputStream(WAV_FORMAT, AudioSystem.getAudioInputStream(audioSourcePath.toFile()))) {
				final byte[] frameBuffer = new byte[WAV_FORMAT.getFrameSize()]; //4 bytes wegen stereo
				final double[] frame = new double[WAV_FORMAT.getChannels()];

				for (int bytesRead = audioSource.read(frameBuffer); bytesRead == frameBuffer.length; bytesRead = audioSource.read(frameBuffer)) {
					if (processor != null) {
						for (int channel = 0; channel < frame.length; ++channel) { //umwandlung in sample werten mit denen man arbeiten kann
							frame[channel] = unpackNormalizedSample(frameBuffer, 2 * channel);
						}
		
						processor.process(frame); 	// mit normalisierten samples soundprozessierung betreiben (Soundeffekte)
													// jedes frame mit 2 mult --> erhöhen der lautstärke
		
						for (int channel = 0; channel < frame.length; ++channel) {
							setNormalizedSample(frameBuffer, 2 * channel, frame[channel]);
						}
					}

					audioSink.write(frameBuffer);
				}
			}
		}

		System.out.println("done.");
	}


	/**
	 * Unpacks a normalized sample value within range [-1, +1] from the given
	 * frame buffer.
	 * @param frameBuffer the frame buffer
	 * @param offset the sample offset
	 * @return the unpacked and normalized sample 
	 * @throws NullPointerException if the given frame buffer is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset is out of bounds
	 */
	static private double unpackNormalizedSample (byte[] frameBuffer, int offset) throws ArrayIndexOutOfBoundsException {
		final double sample = (frameBuffer[offset] & 0xFF) + (frameBuffer[offset + 1] << 8); // unsigned wert erzeugen mit 0xFF und dann shiften
		return sample >= 0 ? +sample / Short.MAX_VALUE : -sample / Short.MIN_VALUE; // wenn größer null, dann wird pos sample genommen und mit max pos wert von short geteilt
		// wenn kleiner null, dann wird es druch max neg wert geteilt --> dadurch erhält man immer wert zwischen -1 und 1
	}



	/**
	 * Packs a normalized sample value within range [-1, +1] into the given
	 * frame buffer.
	 * @param frameBuffer the frame buffer
	 * @param offset the sample offset
	 * @param the normalized sample to be packed 
	 * @throws NullPointerException if the given frame buffer is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset is out of bounds
	 */
	static private void setNormalizedSample (byte[] frameBuffer, int offset, double sample) throws ArrayIndexOutOfBoundsException {
		sample	= sample >= -1 ? (sample <= +1 ? sample : +1) : -1;

		final long value = Math.round(sample >= 0 ? +sample * Short.MAX_VALUE : -sample * Short.MIN_VALUE);
		frameBuffer[offset]		= (byte) (value >>> 0);	// pack LSB
		frameBuffer[offset + 1]	= (byte) (value >>> 8); // pack HSB
		
		// hier wird obige funktion wieder rückgängig gemacht
	}
}