package de.sb.audio;

import java.io.IOException;
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
public class ProcessorDemo2 {
	private static final int PCM_SIGNED_SIZE = 2;
//	static private final Encoding MP3 = new Encoding("MP3");
//	static private final AudioFormat MP3_FORMAT = new AudioFormat(MP3, -1F, -1, +2, -1, -1F, false);

	/**
	 * Application entry point.
	 * @param args the runtime arguments
	 * @throws UnsupportedAudioFileException if the given audio file type is unsupported
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IOException, UnsupportedAudioFileException {
		final Path audioSourcePath	= Paths.get(args[0]);
		final Path audioSinkPath	= Paths.get(args[1]);
		final Processor volume = new Volume(1.0);
		final Processor compressor = new Compressor(Double.parseDouble(args[2]));
		final Processor chain = frame -> { volume.process(frame); compressor.process(frame); };
		System.out.print("working ... ");

		try (AudioInputStream fileSource = AudioSystem.getAudioInputStream(audioSourcePath.toFile())) {
			final float frameRate = fileSource.getFormat().getSampleRate();
			final int frameWidth = fileSource.getFormat().getChannels();
			final AudioFormat frameFormat = new AudioFormat(Encoding.PCM_SIGNED, frameRate, PCM_SIGNED_SIZE * Byte.SIZE, frameWidth, PCM_SIGNED_SIZE * frameWidth, frameRate, false);

			try (AudioInputStream audioSource = AudioSystem.getAudioInputStream(frameFormat, fileSource)) {
				try (AudioInputStream processorSource = new AudioProcessorStream(audioSource, chain)) {
					AudioSystem.write(processorSource, Type.WAVE, audioSinkPath.toFile());
				}
			}
		}
		System.out.println("done.");
	}
}