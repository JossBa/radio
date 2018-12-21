package de.sb.audio;

public class Volume implements Processor {
	private double gain;

	
	public Volume (final double gain) {
		this.gain = gain;
	}


	public void process (final double[] frame) throws NullPointerException {
		for (int channel = 0; channel < frame.length; ++channel) {
			frame[channel] *= gain;
		}
	}
}
