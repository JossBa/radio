package de.sb.radio.processor;

public class Compressor implements Processor {
	private double expansionRatio; // multiplikationsfaktor, der mit jedem sample kombiniert wird

	public Compressor (final double compressionRatio) {
		this.expansionRatio = 1/compressionRatio;
	}


	public void process (final double[] frame) throws NullPointerException { //schleife über alle kanäle und dann mult mit allen samples
		for (int channel = 0; channel < frame.length; ++channel) {
			final double sample = frame[channel];
			frame[channel] = Math.signum(sample) * (1 - Math.pow(1-Math.abs(sample), this.expansionRatio)); 
		}
	}
}
