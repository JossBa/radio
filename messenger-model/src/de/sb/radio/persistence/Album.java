package de.sb.radio.persistence;

import java.util.Collections;
import java.util.Set;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import de.sb.toolbox.bind.JsonProtectedPropertyStrategy;
import de.sb.toolbox.val.NotEqual;


/**
 * This class models album
 * entities. @JsonbVisibility(JsonProtectedPropertyStrategy.class)
 */
@Entity
@Table(schema = "radio", name = "Album")
@PrimaryKeyJoinColumn(name = "albumIdentity")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public class Album extends BaseEntity {

	@NotNull
	@Size(min = 0, max = 127) // Size steht in der UML
	@Column(nullable = false, updatable = true, length = 128, unique = true)
	private String title;

	@NotEqual("0")
	@Column(nullable = false, updatable = true)
	private short releaseYear;

	@Positive
	@Column(nullable = false, updatable = true)
	private byte trackCount;

	@ManyToOne(optional = false)
	@JoinColumn(name = "coverReference", nullable = false, updatable = true)
	private Document cover;

	@NotNull
	@OneToMany(mappedBy = "album", cascade = { CascadeType.REMOVE, CascadeType.REFRESH })
	private Set<Track> tracks;
	// mappedBy kein @Column


	// wie soll Konstruktor aussehen

	protected Album() {
		this(null);
	}


	public Album(Document cover) {
		this.cover = cover;
		this.tracks = Collections.emptySet(); // fuer die mappedBy Seite
	}


	@JsonbProperty
	public String getTitle () {
		return title;
	}


	public void setTitle (final String title) {
		this.title = title;
	}


	@JsonbProperty
	public short getReleaseYear () {
		return releaseYear;
	}


	public void setReleaseYear (final short releaseYear) {
		this.releaseYear = releaseYear;
	}


	@JsonbProperty
	public byte getTrackCount () {
		return trackCount;
	}


	public void setTrackCount (final byte trackCount) {
		this.trackCount = trackCount;
	}


	@JsonbTransient
	public Document getCover () {
		return cover;
	}


	public void setCover (final Document cover) {
		this.cover = cover;
	}


	@JsonbTransient
	public Set<Track> getTracks () {
		return this.tracks;
	}


	@JsonbProperty
	protected long getCoverReference () {
		return this.cover.getIdentity();
	}


	@JsonbProperty
	protected long[] getTrackReferences () {
		return this.tracks.stream().mapToLong(Track::getIdentity).toArray();
	}
}
