package de.sb.radio.persistence;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.sb.radio.processor.Processor;
import de.sb.toolbox.Copyright;
import de.sb.toolbox.bind.JsonProtectedPropertyStrategy;


/**
 * This class models document entities based on database rows, representing a
 * kind of embedded content management system, similarly to what was defined in
 * JSR086 for EJB2. The documents use SHA-256 content hashes as natural document
 * keys, thereby preventing multiple entities storing the same content. The
 * document entities do not track their inverse relationships, as these should
 * be of little general interest. Note that some special considerations must be
 * taken into account when dealing with any kind of large object binary (LOB)
 * column in an object-relational model:
 * <ul>
 * <li>Mapping content into databases has the general advantage over simple file
 * system storage of guaranteeing consistency when performing ACID transactions
 * or database backups.</li>
 * <li>The primary choice is mapping the content to byte arrays, which combines
 * simplicity with effective 2nd level caching of document content. However,
 * content size must be carefully considered in this case, in order to avoid
 * excessive memory demands in a server environment: Content under one MB in
 * size should usually be ok; 64KB is the default size usually reserved for
 * streaming I/O buffers and IP packets, which indicates the super safe lower
 * limit. Larger content should also be bearable if it isn't constantly
 * accessed, maybe up to a few MB in size; a 16MB MEDIUMBLOB cell still fits
 * around 256 times into 4GB of memory. However, once content size exceeds these
 * limits, direct mapping into byte arrays quickly becomes questionable.</li>
 * <li>If byte arrays cannot be used for the complete content because the
 * content size is too large, an interesting alternative is to break up the
 * content into smaller chunks, for example stored in 32KB VARBINARY fields of
 * 1:* related "DocumentChunk" entities. Using JP-QL queries for the chunk
 * identities, the chunks can be accessed serially whenever the content is
 * required, while still maintaining the effectiveness of the 2nd level cache if
 * desired.</li>
 * <li>In opposition to this, JPA mapping to {@link java.sql.Blob} (which
 * promises streaming I/O) is not supported by most JPA implementations. The
 * reasons for this are multi-faceted: Few databases really support server-side
 * streaming I/O of BLOBs; MySQL for example does not and probably never will.
 * Even fewer JDBC connector implementations really support streaming I/O of
 * BLOBs, just copying the whole content before granting read I/O access,
 * implicitly defeating the motivation of using {@code Blob} in the first place.
 * On top of this, {@code Blob} instances must remain connected to the database
 * while their content is accessed, which can easily cause issues with resource
 * closing. And on top of this, the content cannot become part of the 2nd level
 * cache, which implies it has to be transported over a JDBC connection for
 * every request, potentially crowding the latter with traffic. Therefore, JPA
 * {@code Blob} mapping support is shaky at best, and seldom recommended.</li>
 * <li>When an application grows into a serious business, the time will come
 * when there needs to be a decision if the content shall be migrated into a
 * dedicated content management system or content distribution network. However,
 * keep in mind that content management systems are usually based on databases;
 * what's primarily being sold (usually for lots of money) is know-how of how to
 * use a database to manage content efficiently. There is nothing inherently
 * "magic" about them, apart from representing a billion Dollar a year cash cow
 * for some manufacturers.</li>
 * </ul>
 */
@Entity
@Table(schema = "radio", name = "Document")
@PrimaryKeyJoinColumn(name = "documentIdentity")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class Document extends BaseEntity {
	static private final byte[] EMPTY_CONTENT = new byte[0];
	static private final byte[] EMPTY_CONTENT_HASH = HashTools.sha256HashCode(EMPTY_CONTENT);
	static private final int PCM_SIGNED_SIZE = 2;
	static private final double POS_NORM = +1D / Short.MAX_VALUE;
	static private final double NEG_NORM = -1D / Short.MIN_VALUE;
	static private final double POS_DENORM = +1D * Short.MAX_VALUE;
	static private final double NEG_DENORM = -1D * Short.MIN_VALUE;
	
	@NotNull
	@Size(min = 32, max = 32)
	@Column(nullable = false, updatable = true, length = 32, unique = true)
	private byte[] contentHash;

	@NotNull
	@Size(min = 1, max = 63)
	@Pattern(regexp = "^[a-z]+/[[a-z][0-9]\\-\\+\\.]+$")
	@Column(nullable = false, updatable = true, length = 63)
	private String contentType;

	@NotNull
	@Size(min = 1)
	@Column(nullable = false, updatable = true)
	private byte[] content;


	/**
	 * Creates a new instance.
	 */
	public Document() {
		this.content = EMPTY_CONTENT;
		this.contentHash = EMPTY_CONTENT_HASH;
		this.contentType = "application/octet-stream";
	}


	/**
	 * Returns the content hash.
	 * 
	 * @return the quasi-unique SHA-256 hash of the content
	 */
	@JsonbProperty
	public byte[] getContentHash () {
		return this.contentHash;
	}


	/**
	 * Returns the content type.
	 * 
	 * @return the content type
	 */
	@JsonbProperty
	public String getContentType () {
		return this.contentType;
	}


	/**
	 * Sets the content type.
	 * 
	 * @param contentType
	 *            the content type
	 */
	public void setContentType (final String contentType) {
		this.contentType = contentType;
	}


	/**
	 * Returns the content.
	 * 
	 * @return the content
	 */
	@JsonbTransient
	public byte[] getContent () {
		return this.content;
	}


	/**
	 * Sets the content.
	 * 
	 * @param content
	 *            the content
	 */
	public void setContent (final byte[] content) {
		this.content = content;
		this.contentHash = HashTools.sha256HashCode(content);
	}


	/**
	 * Returns the scaled content of an image of the given file type.
	 * 
	 * @param fileType
	 *            the file type
	 * @param content
	 *            the binary content
	 * @param width
	 *            the target width, or zero for proportional scaling
	 * @param height
	 *            the target height, or zero for proportional scaling
	 * @return the resized content
	 * @throws NullPointerException
	 *             if the given file type or content is {@code null}
	 * @throws IllegalArgumentException
	 *             if the given width or height is negative, or if the given
	 *             file type is not supported
	 */
	static public byte[] scaledImageContent (final String fileType, final byte[] content, final int width, final int height) throws NullPointerException, IllegalArgumentException {
		try {
			if (fileType == null | content == null)
				throw new NullPointerException();
			if (width < 0 | height < 0)
				throw new IllegalArgumentException();
			if (width == 0 & height == 0)
				return content;

			final BufferedImage originalImage;
			try (InputStream byteSource = new ByteArrayInputStream(content)) {
				originalImage = ImageIO.read(byteSource);
			}

			final int scaleWidth = width == 0 ? originalImage.getWidth() * height / originalImage.getHeight() : width;
			final int scaleHeight = height == 0 ? originalImage.getHeight() * width / originalImage.getWidth() : height;
			final BufferedImage scaledImage = new BufferedImage(scaleWidth, scaleHeight, originalImage.getType());
			final Graphics2D graphics = scaledImage.createGraphics();
			try {
				graphics.drawImage(originalImage, 0, 0, scaleWidth, scaleHeight, null);
			} finally {
				graphics.dispose();
			}

			try (ByteArrayOutputStream byteSink = new ByteArrayOutputStream()) {
				final boolean supported = ImageIO.write(scaledImage, fileType, byteSink);
				if (!supported)
					throw new IllegalArgumentException();
				return byteSink.toByteArray();
			}
		} catch (final IOException exception) {
			// there should never be I/O errors with byte array based I/O
			// streams
			throw new AssertionError(exception);
		}
	}
	

	/**
	 * Converts the given audio file content into WavE format, and processes it using the given audio processor.
	 * @param audioContent the audio file content to be processed
	 * @param audioProcessor the audio processor
	 * @return the processed WAVE audio file content
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws UnsupportedAudioFileException if the given content is not valid audio file data recognized by the system
	 */
	static public byte[] processedAudioContent (final byte[] audioContent, final Processor audioProcessor) throws NullPointerException, UnsupportedAudioFileException {
		final AudioFormat sourceFormat = extractAudioFrameFormat(audioContent);
		final float frameRate = sourceFormat.getSampleRate();
		final int frameWidth = sourceFormat.getChannels();
		final AudioFormat audioFrameFormat = new AudioFormat(Encoding.PCM_SIGNED, frameRate, PCM_SIGNED_SIZE * Byte.SIZE, frameWidth, PCM_SIGNED_SIZE * frameWidth, frameRate, false);

		final byte[] audioFrameData = convertToAudioFrames(audioContent, audioFrameFormat);
		processAudioFrames(audioFrameData, audioFrameFormat.getChannels(), audioProcessor);
		return convertToAudioContent(audioFrameData, audioFrameFormat, Type.WAVE);
	}


	/**
	 * Returns the audio frame format matching the given audio file content.
	 * @param audioContent the audio file content
	 * @return the matching audio frame format
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws UnsupportedAudioFileException if the given content is not valid audio file data recognized by the system
	 */
	static private AudioFormat extractAudioFrameFormat (final byte[] audioContent) throws NullPointerException, UnsupportedAudioFileException {
		try {
			try (ByteArrayInputStream byteSource = new ByteArrayInputStream(audioContent)) {
				return AudioSystem.getAudioFileFormat(byteSource).getFormat();
			}
		} catch (final IOException exception) {
			throw new AssertionError(exception);
		}
	}


	/**
	 * Converts the given audio file content to the given audio frame format, and returns the resulting raw audio frames.
	 * @param audioContent the audio file content to be converted
	 * @param audioFrameFormat the audio frame format
	 * @return the converted raw audio frame data
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given audio format's frame size is negative, or if the audio format conversion is
	 *         not supported
	 * @throws UnsupportedAudioFileException if the given content is not valid audio file data recognized by the system
	 */
	static private byte[] convertToAudioFrames (final byte[] audioContent, final AudioFormat audioFrameFormat) throws NullPointerException, IllegalArgumentException, UnsupportedAudioFileException {
		if (audioFrameFormat.getFrameSize() <= 0) throw new IllegalArgumentException();

		try {
			try (ByteArrayOutputStream byteSink = new ByteArrayOutputStream()) {
				try (AudioInputStream audioSource = AudioSystem.getAudioInputStream(audioFrameFormat, AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioContent)))) {
					final byte[] buffer = new byte[audioFrameFormat.getFrameSize() << 10];
					for (int bytesRead = audioSource.read(buffer); bytesRead != -1; bytesRead = audioSource.read(buffer)) {
						byteSink.write(buffer, 0, bytesRead);
					}
				}

				return byteSink.toByteArray();
			}
		} catch (final IOException exception) {
			throw new AssertionError(exception);
		}
	}


	/**
	 * Converts the given raw audio frames into audio file content of the given audio file type. The given frame format should
	 * match the given audio frame data, otherwise the result will be undefined.
	 * @param audioFrameData the audio frame data to be converted
	 * @param audioFrameFormat the audio frame format
	 * @param audioFileType the audio file type
	 * @return the converted audio file content
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given audio format's frame size is negative
	 */
	static private byte[] convertToAudioContent (final byte[] audioFrameData, final AudioFormat audioFrameFormat, final Type audioFileType) throws NullPointerException, IllegalArgumentException {
		if (audioFrameFormat.getFrameSize() <= 0) throw new IllegalArgumentException();
		final long frameCount = audioFrameData.length / audioFrameFormat.getFrameSize();

		try {
			try (ByteArrayOutputStream audioSink = new ByteArrayOutputStream()) {
				try (AudioInputStream audioSource = new AudioInputStream(new ByteArrayInputStream(audioFrameData), audioFrameFormat, frameCount)) {
					AudioSystem.write(audioSource, audioFileType, audioSink);
				}
				return audioSink.toByteArray();
			}
		} catch (final IOException exception) {
			throw new AssertionError(exception);
		}
	}


	/**
	 * Processes the given audio frame data using the given audio processor. The audio frame data must provided in PCM signed,
	 * little endian format.
	 * @param audioFrameData the audio frame data to be processed
	 * @param audioFrameWidth the number of channels per audio frame
	 * @param audioProcessor the audio processor
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given audio frame width is negative
	 */
	static private void processAudioFrames (final byte[] audioFrameData, final int audioFrameWidth, final Processor audioProcessor) throws NullPointerException, IllegalArgumentException {
		if (audioFrameWidth <= 0) throw new IllegalArgumentException();
		final ByteBuffer frameBuffer = ByteBuffer.wrap(audioFrameData).order(LITTLE_ENDIAN);

		final double[] frame = new double[audioFrameWidth];
		while (frameBuffer.hasRemaining()) {
			for (int channel = 0; channel < frame.length; ++channel) {
				final double sample = (double) frameBuffer.getShort();
				frame[channel] = sample * (sample >= 0 ? POS_NORM : NEG_NORM);
			}

			audioProcessor.process(frame);

			for (int channel = 0; channel < frame.length; ++channel) {
				double sample = frame[channel];
				if (sample < -1) sample = -1;
				if (sample > +1) sample = +1;
				frameBuffer.putShort((short) Math.round(sample * (sample >= 0 ? POS_DENORM : NEG_DENORM)));
			}
		}
	}
}