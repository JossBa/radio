package de.sb.radio.processor;

import de.sb.toolbox.Copyright;


/**
 * Parametric audio compressor/expander. If the processor's compression ratio exceeds 1, the processor performs as a compressor.
 * If the compression ratio is within range {@code ]0, 1[}, the processor performs as an expander. If
 * the processor's compression ratio is exactly 1, it behaves neutral.
 */
@Copyright(year=2018, holders="Sascha Baumeister")
public class Compressor implements Processor {
	private double compressionRatio;


	/**
	 * Initializes a new neutral instance based on compression ratio 1.
	 */
	public Compressor () {
		this.compressionRatio = 1;
	}


	/**
	 * Returns the compression ratio.
	 * @return the compression ratio
	 */
	public double getCompressionRatio () {
		return this.compressionRatio;
	}


	/**
	 * Sets the compression ratio. Compression ratio values within range {@code ]0, 1[} cause audio expansion,
	 * while compression ratio values within range {@code ]1, ∞[} cause audio compression.
	 * @param compressionRatio the compression ratio
	 * @throws IllegalArgumentException if the given compression ratio is out of range {@code ]0, ∞[}
	 */
	public void setCompressionRatio (final double compressionRatio) throws IllegalArgumentException {
		if (compressionRatio <= 0 | !Double.isFinite(compressionRatio)) throw new IllegalArgumentException();

		this.compressionRatio = compressionRatio;
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 */
	public void process (final double[] frame) throws NullPointerException {
		for (int channel = 0; channel < frame.length; ++channel) {
			final double sample = frame[channel];
			frame[channel] = Math.signum(sample) * (1 - Math.pow(1 - Math.abs(sample), this.compressionRatio));
		}
	}
}
