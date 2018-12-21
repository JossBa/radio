package de.sb.audio;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;


/**
 * This class provides audio input streams that are capable of audio processing 
 */
public class AudioProcessorStream extends AudioInputStream {
	static private final int SAMPLE_SIZE = 2;
	static private final double POS_NORM = +1.0 / Short.MAX_VALUE;
	static private final double NEG_NORM = -1.0 / Short.MIN_VALUE;
	static private final double POS_DENORM = +1.0 * Short.MAX_VALUE;
	static private final double NEG_DENORM = -1.0 * Short.MIN_VALUE;
	
	private final Processor processor;


	/**
	 * Initializes a new instance based on the given audio stream and audio processors.
	 * @param stream the audio stream
	 * @param processor the audio processor
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given stream is not encoded in little-endian 16bit signed format
	 */
	public AudioProcessorStream (AudioInputStream stream, Processor processor) throws NullPointerException, IllegalArgumentException {
		super(stream, stream.getFormat(), stream.getFrameLength());

		final AudioFormat format = stream.getFormat();
		if (format.isBigEndian() | !Encoding.PCM_SIGNED.equals(format.getEncoding()) | format.getSampleSizeInBits() != SAMPLE_SIZE * Byte.SIZE) throw new IllegalArgumentException();

		if (processor == null) throw new NullPointerException();
		this.processor = processor;
	}


	/**
	 * Obtains the audio processor.
	 * @return the audio processor used to process the audio data
	 */
	protected Processor getProcessor () {
		return this.processor;
	}


	/**
	 * Reads the next byte of data from the audio input stream. This operation always
	 * throws an {@link UnsupportedOperationException} because the WAV format's frame
	 * size cannot equal a single byte.
	 * @throws UnsupportedOperationException because this operation is not supported
	 */
	public int read () throws UnsupportedOperationException {
		 throw new UnsupportedOperationException();
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IOException {@inheritDoc}
	 */
	public int read (byte[] buffer) throws NullPointerException, IndexOutOfBoundsException, IOException {
		return this.read(buffer, 0, buffer.length);
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws IndexOutOfBoundsException {@inheritDoc}
	 * @throws IOException {@inheritDoc}
	 */
	public int read (byte[] buffer, int offset, int length) throws NullPointerException, IOException {
		final int bytesRead = super.read(buffer, offset, length);
		final double[] frame = new double[this.getFormat().getChannels()];

		for (int index = 0; index < bytesRead; index += frame.length * SAMPLE_SIZE) {
			for (int position = offset + index, channel = 0; channel < frame.length; ++channel, position += SAMPLE_SIZE) {
				frame[channel] = readNormalizedSample(buffer, position);
			}

			this.processor.process(frame);

			for (int position = offset + index, channel = 0; channel < frame.length; ++channel, position += SAMPLE_SIZE) {
				writeDenormalizedSample(buffer, position, frame[channel]);
			}
		}

		return bytesRead;
	}


	/**
	 * Reads a normalized sample value from the given frame buffer.
	 * @param frameBuffer the frame buffer
	 * @param offset the sample offset
	 * @return the normalized sample read, within range [-1, +1]
	 * @throws NullPointerException if the given frame buffer is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset is out of bounds
	 */
	static protected double readNormalizedSample (byte[] frameBuffer, int offset) throws ArrayIndexOutOfBoundsException {
		final double sample = (frameBuffer[offset] & 0xff) + (frameBuffer[offset + 1] << 8);
		return sample * (sample >= 0 ? POS_NORM : NEG_NORM);
	}



	/**
	 * Writes a denormalized sample value into the given frame buffer.
	 * @param frameBuffer the frame buffer
	 * @param offset the sample offset
	 * @param the normalized sample to be written, withing range [-1, +1] 
	 * @throws NullPointerException if the given frame buffer is {@code null}
	 * @throws ArrayIndexOutOfBoundsException if the given offset is out of bounds
	 */
	static protected void writeDenormalizedSample (byte[] frameBuffer, int offset, double sample) throws ArrayIndexOutOfBoundsException {
		sample	= sample >= -1 ? (sample <= +1 ? sample : +1) : -1;

		final long value = Math.round(sample * (sample >= 0 ? POS_DENORM : NEG_DENORM));
		frameBuffer[offset]		= (byte) (value >>> 0);	// pack LSB
		frameBuffer[offset + 1]	= (byte) (value >>> 8); // pack HSB
	}
}