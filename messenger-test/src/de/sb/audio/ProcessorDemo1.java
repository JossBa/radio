package de.sb.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * Audio processor demo.
 * @author Sascha Baumeister
 *
 */
public class ProcessorDemo1 {
	private static final int PCM_SIGNED_SIZE = 2;

	/**
	 * Application entry point.
	 * @param args the runtime arguments
	 * @throws UnsupportedAudioFileException if the given audio file type is unsupported
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IOException, UnsupportedAudioFileException {
		final Path audioSourcePath	= Paths.get(args[0]);
		final Path audioSinkPath	= Paths.get(args[1]);
		final Processor processor = new Compressor(Double.parseDouble(args[2]));
		System.out.print("working ... ");

		// step 1: convert file format frames into WAV PCM_SIGNED frame content, excluding the audio headers
		final byte[] frameContent; 
		final AudioFormat frameFormat;
		try (ByteArrayOutputStream byteSink = new ByteArrayOutputStream()) {
			try (AudioInputStream fileSource = AudioSystem.getAudioInputStream(audioSourcePath.toFile())) {
				final float frameRate = fileSource.getFormat().getSampleRate();
				final int frameWidth = fileSource.getFormat().getChannels();
				frameFormat = new AudioFormat(Encoding.PCM_SIGNED, frameRate, PCM_SIGNED_SIZE * Byte.SIZE, frameWidth, PCM_SIGNED_SIZE * frameWidth, frameRate, false);

				try (AudioInputStream audioSource = AudioSystem.getAudioInputStream(frameFormat, fileSource)) {
					final byte[] frameBuffer = new byte[frameFormat.getFrameSize()];
					for (int bytesRead = audioSource.read(frameBuffer); bytesRead != -1; bytesRead = audioSource.read(frameBuffer)) {
						byteSink.write(frameBuffer, 0, bytesRead);
					}
				}
			}
			frameContent = byteSink.toByteArray();
		}

		// step 2: process the WAV PCM_SIGNED frames
		final double[] frame = new double[frameFormat.getChannels()];
		for (int position = 0; position < frameContent.length; position += frame.length * PCM_SIGNED_SIZE) {
			for (int channel = 0; channel < frame.length; ++channel) {
				frame[channel] = readNormalizedSample(frameContent, position + channel * PCM_SIGNED_SIZE);
			}

			processor.process(frame);

			for (int channel = 0; channel < frame.length; ++channel) {
				writeNormalizedSample(frameContent, position + channel * PCM_SIGNED_SIZE, frame[channel]);
			}
		}

		// Step 3: convert WAV PCM_SIGNED frame content into WAV file format, including the audio headers
		final byte[] fileContent;
		try (ByteArrayOutputStream byteSink = new ByteArrayOutputStream()) {
			try (ByteArrayInputStream byteSource = new ByteArrayInputStream(frameContent)) {
				try (AudioInputStream audioSource = new AudioInputStream(byteSource, frameFormat, frameContent.length / frameFormat.getFrameSize())) {
					AudioSystem.write(audioSource, Type.WAVE, byteSink);
				}
			}
			fileContent = byteSink.toByteArray();
		}

		Files.write(audioSinkPath, fileContent);
		System.out.println("done.");
	}


	/**
	 * Reads a normalized sample value within range [-1, +1] from the given
	 * frame buffer.
	 * @param frameBuffer the frame buffer
	 * @param offset the sample offset
	 * @return the unpacked and normalized sample 
	 * @throws NullPointerException if the given frame buffer is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset is out of bounds
	 */
	static private double readNormalizedSample (byte[] frameBuffer, int offset) throws ArrayIndexOutOfBoundsException {
		final double sample = (frameBuffer[offset] & 0xFF) + (frameBuffer[offset + 1] << 8);
		return sample >= 0 ? +sample / Short.MAX_VALUE : -sample / Short.MIN_VALUE;
	}


	/**
	 * Writes a normalized sample value within range [-1, +1] into the given
	 * frame buffer.
	 * @param frameBuffer the frame buffer
	 * @param offset the sample offset
	 * @param the normalized sample to be packed 
	 * @throws NullPointerException if the given frame buffer is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset is out of bounds
	 */
	static private void writeNormalizedSample (byte[] frameBuffer, int offset, double sample) throws ArrayIndexOutOfBoundsException {
		sample	= sample >= -1 ? (sample <= +1 ? sample : +1) : -1;

		final long value = Math.round(sample >= 0 ? +sample * Short.MAX_VALUE : -sample * Short.MIN_VALUE);
		frameBuffer[offset]		= (byte) (value >>> 0);	// pack LSB
		frameBuffer[offset + 1]	= (byte) (value >>> 8); // pack HSB
	}
}