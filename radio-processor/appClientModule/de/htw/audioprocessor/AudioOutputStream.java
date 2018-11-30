package de.htw.audioprocessor;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import de.sb.toolbox.Copyright;


/**
 * This abstract class defines audio output streams, i.e. output streams that are connected to an
 * audio format, and which will create header data on the fly during streaming. The class is
 * abstract so that two different implementations can be transparently provided: One is based on
 * source data lines, and straightforwardly wraps there to make them look like output streams. The
 * other is based on pipes, and each instance works in conjunction with a transport tread, which is
 * necessary to perform on-the-fly format conversions, or write audio data into a file without
 * blocking. There are several factory methods to create instances of these two subclasses, which
 * cannot be instantiated directly.<br />
 * This design solves most of the problems caused by the Java Sound API not providing a counterpart
 * to its {@linkplain AudioInputStream} class itself. The design of this class is heavily influenced
 * by said class and the static {@linkplain AudioSystem} factory methods for a common look and feel.
 * @see AudioInputStream
 * @see #getAudioOutputStream(SourceDataLine, boolean)
 * @see #getAudioOutputStream(AudioFormat.Encoding, AudioOutputStream, int)
 * @see #getAudioOutputStream(AudioFormat, AudioOutputStream, int)
 * @see #getAudioOutputStream(AudioFormat, AudioFileFormat.Type, OutputStream)
 * @see #getAudioOutputStream(AudioFormat, AudioFileFormat.Type, Path)
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public abstract class AudioOutputStream extends OutputStream {

	/**
	 * Obtains the audio format of the sound data written to this audio output stream.
	 * @return the audio format
	 */
	public abstract AudioFormat getFormat ();


	/**
	 * Writes the given value to the underlying stream.
	 * @param value the value to write to the stream, usually a byte value
	 * @throws IOException if there's an I/O related problem with this thread or a child transport
	 *         thread
	 */
	public void write (final int value) throws IOException {
		final byte[] buffer = new byte[] { (byte) (value & 0xFF) };
		this.write(buffer, 0, buffer.length);
	}


	/**
	 * Writes the given buffer to the underlying stream.
	 * @param buffer the buffer to write to the stream
	 * @throws NullPointerException if the given buffer is {@code null}
	 * @throws IOException if there's an I/O related problem with this thread or a child transport
	 *         thread
	 */
	public void write (final byte[] buffer) throws NullPointerException, IOException {
		this.write(buffer, 0, buffer.length);
	}


	/**
	 * Writes parts of the given buffer to the underlying stream.
	 * @param buffer the buffer to write to the stream
	 * @param offset the ioffset of the first byte to be written
	 * @param limit the maximum number of bytes to be written
	 * @throws NullPointerException if the given buffer is {@code null}
	 * @throws IndexOutOfBoundsException if the given offset or length is negative, or if their
	 *         sum is greater than the buffer length
	 * @throws IOException if there's an I/O related problem within this thread or the
	 *         correlated transport thread
	 */
	public abstract void write (final byte[] buffer, final int offset, final int limit) throws NullPointerException, IndexOutOfBoundsException, IOException;



	/*
	 * The following static transcoding methods will probably be placed in class AudioSystem once
	 * such a class is added to the JavaSound API, as the input stream methods are already placed
	 * there.
	 */

	/**
	 * Returns an audio input stream that writes its data to the given data line. Creating such a
	 * stream never opens the data line, and closing the stream never stops it. If synchronize is
	 * {@code true}, then the data line is opened upon stream creation; stream closing involves
	 * draining the data line if it is started at the time, flushing, and subsequently stopping it.
	 * If synchronize is {@code false}, then the data line is neither started, drained, flushed or
	 * stopped by the stream; this allows for seamless playback when multiple streams write in
	 * succession on the same data line.<br />
	 * Flushing such a stream has no effect, as data is buffered within the underlying data line.
	 * Note that in opposition to data lines, these streams allow write lengths that are not
	 * multiples of the audio format's frame size; the streams use a single frame-buffer to hide
	 * this from the data-lines whenever necessary. Apart from that, the write methods delegate
	 * directly to the underlying data line.<br />
	 * Note that in live environments with both a source and a target data line involved the target
	 * data line should be configured with a buffer size that is smaller than that of the source
	 * data line to avoid the two starving each other for resources!
	 * @param sourceDataLine the data sink for this object, a source data line
	 * @param synchronize whether or not the data line shall be started upon stream creation, and
	 *        stopped upon stream closing
	 * @throws NullPointerException if the given data line is {@code null}
	 * @throws IllegalStateException if the given data line is not open
	 */
	static public final AudioOutputStream newAudioOutputStream (final SourceDataLine sourceDataLine, final boolean synchronize) {
		return new SystemAudioOutputStream(sourceDataLine, synchronize);
	}


	/**
	 * Returns an audio output stream that writes to the given target, converting from the given
	 * result's source encoding to the target's encoding if necessary. In case the encodings match
	 * the given target is returned immediately to avoid overhead. Otherwise a top priority
	 * transport thread is started that manages the transcoding from the result to the given target.
	 * During conversion a buffer of configurable result frame size is used to fine tune latency
	 * time increase and drop-out risk in case of real-time streaming - usually 64 frames should
	 * work nicely as a compromise.<br />
	 * Note that this method must not be used in J2EE because the latter restricts thread
	 * scheduling!
	 * @param sourceEncoding the encoding of the audio data written to the result stream
	 * @param audioSink the audio sink for this object, an audio output stream
	 * @param bufferSize the size of the transport buffer in source format frames
	 * @return the audio output stream created
	 * @throws NullPointerException if the given target or source encoding is {@code null}
	 * @throws NegativeArraySizeException if the given buffer size is negative
	 * @throws IllegalArgumentException if the given buffer size is zero, or if the format
	 *         conversion is not supported
	 */
	static public final AudioOutputStream newAudioOutputStream (final AudioFormat.Encoding sourceEncoding, final AudioOutputStream audioSink, final int bufferSize) {
		if (sourceEncoding.equals(audioSink.getFormat().getEncoding())) return audioSink;

		final AudioFormat sinkFormat = audioSink.getFormat();
		final AudioFormat sourceFormat = new AudioFormat(sourceEncoding, sinkFormat.getSampleRate(), sinkFormat.getSampleSizeInBits(), sinkFormat.getChannels(), sinkFormat.getFrameSize(), sinkFormat.getFrameRate(), sinkFormat.isBigEndian(), sinkFormat.properties());
		return AudioOutputStream.newAudioOutputStream(sourceFormat, audioSink, bufferSize);
	}


	/**
	 * Returns an audio output stream that writes to the given target, converting from the given
	 * result's source format to the target's format if necessary. In case the formats match the
	 * given target is returned immediately to avoid overhead. Otherwise a top priority transport
	 * thread is started that manages the transcoding from the result to the given target. During
	 * conversion a buffer of configurable result frame size is used to fine tune latency time
	 * increase and drop-out risk in case of real-time streaming - usually 64 frames should work
	 * nicely as a compromise.<br />
	 * Note that this method must not be used in J2EE because the latter restricts thread
	 * scheduling!
	 * @param sourceFormat the format of the audio data written to the result stream
	 * @param audioSink the audio sink for this object, an audio output stream
	 * @param bufferSize the size of the transport buffer in source format frames
	 * @return the audio output stream created
	 * @throws NullPointerException if the given target or source format is {@code null}
	 * @throws NegativeArraySizeException if the given buffer size is negative
	 * @throws IllegalArgumentException if the given buffer size is zero, or if the format
	 *         conversion is not supported
	 */
	static public final AudioOutputStream newAudioOutputStream (final AudioFormat sourceFormat, final AudioOutputStream audioSink, final int bufferSize) {
		if (bufferSize < 0) throw new NegativeArraySizeException();
		if (bufferSize == 0) throw new IllegalArgumentException();
		if (sourceFormat.matches(audioSink.getFormat())) return audioSink;

		final PipedOutputStream pipedSink = new PipedOutputStream();
		final PipedAudioOutputStream result = new PipedAudioOutputStream(pipedSink, sourceFormat);

		final Runnable runnable = new Runnable() {
			public void run () {
				final byte[] buffer = new byte[sourceFormat.getFrameSize() * bufferSize];

				try (InputStream pipedSource = new PipedInputStream(pipedSink)) {
					try (InputStream audioSource = AudioSystem.getAudioInputStream(audioSink.getFormat(), new AudioInputStream(pipedSource, sourceFormat, AudioSystem.NOT_SPECIFIED))) {
						for (int bytesRead = audioSource.read(buffer); bytesRead != -1; bytesRead = audioSource.read(buffer)) {
							audioSink.write(buffer, 0, bytesRead);
						}
					}
				} catch (final Throwable exception) {
					result.transportException = exception; // report problem to result stream
				} finally {
					try {
						audioSink.close();
					} catch (final Exception exception) {}
				}
			}
		};

		final Thread transportThread = new Thread(runnable);
		transportThread.setPriority(Thread.MAX_PRIORITY);
		transportThread.start();
		return result;
	}


	/**
	 * Returns an audio output stream of the given source format, writing audio data to the
	 * specified target file of the given type. Note that this method must not be used in J2EE
	 * because the latter restricts thread scheduling!
	 * @param sourceFormat the audio format of the result stream
	 * @param audioSinkType the file type indicating the kind of header data that has to be injected
	 *        into the audio sink
	 * @param audioSinkPath the audio sink path for this object
	 * @return the audio output stream created
	 * @throws NullPointerException if the given targetFile, targetFileType or source format is
	 *         {@code null}
	 * @throws IllegalArgumentException if the target file type is not supported by the system
	 */
	static public final AudioOutputStream newAudioOutputStream (final AudioFormat sourceFormat, final AudioFileFormat.Type audioSinkType, final Path audioSinkPath) {
		if (audioSinkType == null | audioSinkPath == null) throw new NullPointerException();

		final PipedOutputStream pipedSink = new PipedOutputStream();
		final PipedAudioOutputStream result = new PipedAudioOutputStream(pipedSink, sourceFormat);

		final Runnable runnable = new Runnable() {
			public void run () {
				try (InputStream pipedSource = new PipedInputStream(pipedSink)) {
					try (AudioInputStream audioSource = new AudioInputStream(pipedSource, sourceFormat, AudioSystem.NOT_SPECIFIED)) {
						AudioSystem.write(audioSource, audioSinkType, audioSinkPath.toFile());
					}
				} catch (final Throwable exception) {
					result.transportException = exception; // report problem to result stream
				}
			}
		};
		new Thread(runnable).start();
		return result;
	}


	/**
	 * Returns an audio output stream of the given source format, writing audio data to the
	 * specified target file of the given type. Some file types like
	 * {@linkplain javax.sound.sampled.AudioFileFormat.Type#WAVE} require that the length be written
	 * into the file header; such targets cannot be written from start to finish unless their length
	 * is known in advance. An attempt to write a target of such a file type will fail with an
	 * IOException if the length in the target file type is {@linkplain AudioSystem#NOT_SPECIFIED}.<br />
	 * Note that this method must not be used in J2EE because the latter restricts thread
	 * scheduling!
	 * @param sourceFormat the audio format of the result stream
	 * @param audioSinkType the file type indicating the kind of header data that has to be injected
	 *        into the data sink
	 * @param audioSink the data sink for this object, an output stream
	 * @return the audio output stream created
	 * @throws NullPointerException if the given targetFile, targetFileType or source format is
	 *         {@code null}
	 * @throws IllegalArgumentException if the target file type is not supported by the system
	 */
	static public final AudioOutputStream newAudioOutputStream (final AudioFormat sourceFormat, final AudioFileFormat.Type audioSinkType, final OutputStream audioSink) {
		if (audioSinkType == null | audioSink == null) throw new NullPointerException();

		final PipedOutputStream pipedSink = new PipedOutputStream();
		final PipedAudioOutputStream result = new PipedAudioOutputStream(pipedSink, sourceFormat);

		final Runnable runnable = new Runnable() {
			public void run () {
				try (InputStream pipedSource = new PipedInputStream(pipedSink)) {
					try (AudioInputStream audioSource = new AudioInputStream(pipedSource, sourceFormat, AudioSystem.NOT_SPECIFIED)) {
						AudioSystem.write(audioSource, audioSinkType, audioSink);
					}
				} catch (final Throwable exception) {
					result.transportException = exception; // report problem to result stream
				} finally {
					try {
						audioSink.close();
					} catch (Exception exception) {}
				}
			}
		};
		new Thread(runnable).start();
		return result;
	}



	/**
	 * Private inner class that makes a source data line look like an audio output stream. Creating
	 * such a stream never opens the data line, and closing the stream never stops it. If
	 * synchronize is {@code true}, then the data line is opened upon stream creation; stream
	 * closing involves draining the data line if it is started at the time, flushing, and
	 * subsequently stopping it. If synchronize is {@code false}, then the data line is neither
	 * started, drained, flushed or stopped by the stream; this allows for seamless playback when
	 * multiple streams write in succession on the same data line.<br />
	 * Flushing such a stream has no effect, as data is buffered within the underlying data line.
	 * Note that in opposition to data lines, these streams allow write lengths that are not
	 * multiples of the audio format's frame size; the streams use a single frame-buffer to hide
	 * this from the data-lines whenever necessary. Apart from that, the write methods delegate
	 * directly to the underlying data line.
	 */
	static private final class SystemAudioOutputStream extends AudioOutputStream {

		private final SourceDataLine sourceDataLine;
		private final boolean synchronize;
		private final byte[] alignmentFrame;
		private volatile int alignmentLength;


		/**
		 * Creates a new instance based on an underlying source data line.
		 * @param sourceDataLine the source data line
		 * @param synchronize whether or not the data line shall be started upon stream creation,
		 *        and stopped upon stream closing
		 * @throws NullPointerException if the given data line is {@code null}
		 * @throws IllegalStateException if synchronize is {@code true}, but the data line is not
		 *         open
		 */
		public SystemAudioOutputStream (final SourceDataLine sourceDataLine, final boolean synchronize) {
				final int frameLength = sourceDataLine.getFormat().getFrameSize();
			this.sourceDataLine = sourceDataLine;
			this.synchronize = synchronize;
			this.alignmentFrame = new byte[frameLength == AudioSystem.NOT_SPECIFIED ? 1 : frameLength];
			this.alignmentLength = 0;

			if (this.synchronize) sourceDataLine.start();
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public AudioFormat getFormat () {
			return this.sourceDataLine.getFormat();
		}


		/**
		 * {@inheritDoc} Note that this implementation uses an internal frame for alignment if the
		 * given lengths are not multiples of the underlying format's frame length.
		 * @throws NullPointerException if the given buffer is {@code null}
		 * @throws IndexOutOfBoundsException if the given offset or length is negative, or if their
		 *         sum is greater than the buffer length
		 */
		@Override
		public void write (final byte[] buffer, int offset, int length) {
			if (length < 0) throw new ArrayIndexOutOfBoundsException(length);

			if (this.alignmentLength > 0) {
				final int byteCount = Math.min(length, this.alignmentFrame.length - this.alignmentLength);
				System.arraycopy(buffer, offset, this.alignmentFrame, this.alignmentLength, byteCount);
				this.alignmentLength += byteCount;
				offset += byteCount;
				length -= byteCount;

				if (this.alignmentLength == this.alignmentFrame.length) {
					this.sourceDataLine.write(this.alignmentFrame, 0, this.alignmentFrame.length);
					this.alignmentLength = 0;
				}
			}
			if (length == 0) return;

			this.alignmentLength = length % this.alignmentFrame.length;
			if (this.alignmentLength > 0) {
				System.arraycopy(buffer, offset + length - this.alignmentLength, this.alignmentFrame, 0, this.alignmentLength);
				length -= this.alignmentLength;
				if (length == 0) return;
			}

			this.sourceDataLine.write(buffer, offset, length);
		}


		/**
		 * Flushes this stream. Note that this method does neither stop nor close the underlying
		 * source data line. This is because it neither opened nor started the data-line when this
		 * stream was created, and also because it allows subsequent playback from other sources.
		 */
		@Override
		public void close () {
			if (this.alignmentLength > 0) {
				Arrays.fill(this.alignmentFrame, this.alignmentLength, this.alignmentFrame.length, (byte) 0);
				this.sourceDataLine.write(this.alignmentFrame, 0, this.alignmentFrame.length);
				this.alignmentLength = 0;
			}

			if (this.synchronize) {
				synchronized (this.sourceDataLine) { // prevents the line being stopped while it is drained!
					if (this.sourceDataLine.isActive()) {
						this.sourceDataLine.drain();
					}
				}
				this.sourceDataLine.stop();
			}
		}
	}



	/**
	 * Private inner class that makes a piped output stream look like an audio output stream.
	 * Instances are always connected to a transport thread that siphons audio data from the
	 * corresponding piped input stream and writes it somewhere else. In case of a problem, the
	 * transport thread will report it back to this stream before terminating, which in turn causes
	 * this stream to report the exception once one of it's methods is called. Note that piping
	 * implies a tiny intermediate buffer of 64 frames.
	 */
	static private class PipedAudioOutputStream extends AudioOutputStream {

		private final PipedOutputStream audioSink;		// the piped output stream to write on
		private final AudioFormat audioFormat;			// the audio format used to write data
		private volatile Throwable transportException;	// indicates a transport thread problem


		/**
		 * Constructs an audio output stream that has the requested format, writing audio data to
		 * the specified output stream. The given audio format is used for information purposes only
		 * unless the given stream is an audio output stream and it's audio format differs from the
		 * given audio format. In this case the latter format is converted into the former, a
		 * separate transport thread is run and a tiny 64 frame buffer is used for data transfer.
		 * @param audioSink the data sink for this object, an output stream
		 * @param audioFormat the format of the audio data written to the stream
		 * @throws NullPointerException if the given stream or format is {@code null}
		 */
		public PipedAudioOutputStream (final PipedOutputStream audioSink, final AudioFormat audioFormat) {
	
			if (audioSink == null | audioFormat == null) throw new NullPointerException();

			this.audioSink = audioSink;
			this.audioFormat = audioFormat;
			this.transportException = null;
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public AudioFormat getFormat () {
			return this.audioFormat;
		}


		/**
		 * {@inheritDoc}
		 * @throws NullPointerException if the given buffer is {@code null}
		 * @throws IndexOutOfBoundsException if the given offset or length is negative, or if their
		 *         sum is greater than the buffer length
		 * @throws IOException if there's an I/O related problem within this thread or the
		 *         correlated transport thread
		 */
		@Override
		public void write (final byte[] buffer, final int offset, final int length) throws IOException {
			if (this.transportException == null) {
				this.audioSink.write(buffer, offset, length);
			} else {
				this.throwTransportException();
			}
		}


		/**
		 * Flushes the receiver by flushing the underlying stream.
		 * @throws IOException if there's an I/O related problem with this thread or a transport
		 *         thread
		 */
		@Override
		public void flush () throws IOException {
			if (this.transportException == null) {
				this.audioSink.flush();
			} else {
				this.throwTransportException();
			}
		}


		/**
		 * Closes the underlying data sink and frees up all reserved resources.
		 * @throws IOException if there's an I/O related problem with this thread or a child
		 *         transport thread
		 */
		@Override
		public void close () throws IOException {
			if (this.transportException == null) {
				this.audioSink.close();
			} else {
				this.throwTransportException();
			}
		}


		/**
		 * Re-throws a transport problem that was communicated by a child transport thread; the
		 * exception that indicated the problem is re-thrown with the current stack trace attached.
		 * The method ensures that the underlying stream is closed before throwing the exception.
		 * The transport child thread tries the same in parallel, so anomalies caused by race
		 * conditions can be avoided, and it is guaranteed that the next invocation of an I/O
		 * related method on this stream will correctly report the stream to be closed.
		 * @throws Error if a transport child thread experienced an error
		 * @throws RuntimeException if a transport child thread experienced a runtime exception
		 * @throws IOException if a transport child thread experienced an I/O related problem
		 * @throws InterruptedIOException if the cause doesn't fit one of the other categories
		 */
		private void throwTransportException () throws IOException {
			final Throwable transportException = this.transportException.fillInStackTrace();
			try {
				this.audioSink.close();
			} catch (final Exception exception) {}
			this.transportException = null;

			if (transportException instanceof Error) throw (Error) transportException;
			if (transportException instanceof RuntimeException) throw (RuntimeException) transportException;
			if (transportException instanceof IOException) throw (IOException) transportException;
			final IOException exception = new InterruptedIOException(transportException.getMessage());
			exception.initCause(transportException);
			throw exception;
		}
	}
}