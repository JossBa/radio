package de.htw.audioprocessor;

public class Volume implements Processor {
	private double gain; // multiplikationsfaktor, der mit jedem sample kombiniert wird

	public Volume (final double gain) {
		this.gain = gain;
	}

	public void process (final double[] frame) throws NullPointerException { //schleife über alle kanäle und dann mult mit allen samples
		for (int channel = 0; channel < frame.length; ++channel) {
			frame[channel] *= gain;
		}
	}
}


