package de.sb.radio.persistence;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;


@Entity
@Table(schema = "radio", name = "Track")
@PrimaryKeyJoinColumn(name = "trackIdentity")
public class Track extends BaseEntity {

	@NotNull @Size(min = 1, max = 127)
	@Column(nullable = false, updatable = true, length = 127)
	private String name;
	
	@NotNull @Size(min = 1, max = 127)
	@Column(nullable = false, updatable = true, length = 127)
	private String artist;
	
	@NotNull @Size(min = 1, max = 31)
	@Column(nullable = false, updatable = true, length = 127)
	private String genre;
	
	@PositiveOrZero
	@Column(nullable = false, updatable = true)
	private byte ordinal;
	
	
	// Ueberall wo JoinColumn steht, muss zusätzliche Methode erstellt werden
	// das sind sog. Reference Properties, die geladen werden müssen
	// bedeutet also, dass immer wenn ein Album geladen wird, auch immer die Beziehung zu Tracks dazu geladen werden soll
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "albumReference", nullable = false, updatable = true)
	private Album album;
	
	
	// wenn eine Person gemarshalt wird, dann sollen dazu auch die ownerReferences von Tracks geladen werden
	@NotNull
	@ManyToOne (optional = false)
	@JoinColumn(name = "ownerReference", nullable = false, updatable = true)
	private Person owner;
	
	
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "recordingReference",nullable = false, updatable = true)
	private Document recording;

	
	
	protected Track(){
		this(null);
	}

	public Track(Document recording){
		this.recording = recording;
	}
	
	
	@JsonbProperty
	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@JsonbProperty
	public String getArtist() {
		return this.artist;
	}

	public void setArtist(final String artist) {
		this.artist = artist;
	}
	
	@JsonbProperty
	public String getGenre() {
		return this.genre;
	}

	public void setGenre(final String genre) {
		this.genre = genre;
	}
	
	@JsonbProperty
	public byte getOrdinal() {
		return this.ordinal;
	}

	public void setOrdinal(final byte ordinal) {
		this.ordinal = ordinal;
	}

	//@JsonbTransient um zu konfigurieren dass die Information nicht gemarshaled
	// werden soll --> das wird dann unten mittels der getAlbumReference gemarshallt
	@JsonbTransient
	public Album getAlbum() {
		return this.album;
	}

	public void setAlbum(final Album album) {
		this.album = album;
	}

	@JsonbTransient
	public Person getOwner() {
		return this.owner;
	}

	public void setOwner(final Person owner) {
		this.owner = owner;
	}


	@JsonbTransient
	public Document getRecording() {
		return this.recording;
	}

	public void setRecording(final Document recording) {
		this.recording = recording;
	}
	
	@JsonbProperty
	protected long getAlbumReference(){
		return album.getIdentity();
	}
	
	@JsonbProperty
	protected long getOwnerReference(){
		return owner.getIdentity();
	}
	
	@JsonbProperty
	protected long getRecordingReference(){
		return recording.getIdentity();
	}
}
