package de.sb.radio.persistence;

import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.InheritanceType.JOINED;
import static javax.xml.bind.annotation.XmlAccessType.NONE;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.Positive;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import de.sb.toolbox.bind.JsonProtectedPropertyStrategy;
import de.sb.toolbox.bind.XmlLongToStringAdapter;


/**
 * This abstract class defines entities as the root of a JOINED_TABLE
 * inheritance tree. Having a common root class allows for the unique generation
 * of primary key across all subclasses, and additionally for polymorphic
 * queries. Note that this implementation accesses it's own field using accessor
 * methods to allow JPA entity proxies to fetch the correct state. Note that
 * this class has a natural ordering that is inconsistent with
 * {@link Object#equals(Object)}.
 */
@Entity
@Table(schema = "radio", name = "BaseEntity")
@Inheritance(strategy = JOINED)
@DiscriminatorColumn(name = "discriminator")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public abstract class BaseEntity implements Comparable<BaseEntity> {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private volatile long identity;

	@Positive
	@Version
	@Column(nullable = false, updatable = true)
	private volatile int version;

	@Column(nullable = false, updatable = false, insertable = true)
	private volatile long creationTimestamp;


	/**
	 * Creates a new instance.
	 */
	public BaseEntity() {
		this.identity = 0;
		this.version = 1;
		this.creationTimestamp = System.currentTimeMillis();
	}


	/**
	 * Returns the identity, i.e. the primary key value. The key may not be set
	 * by an application, it is initialized to zero for transient instances and
	 * reset to its real value once the instance has been inserted into the
	 * database.
	 * 
	 * @return the identity (primary key)
	 */
	@JsonbProperty
	public long getIdentity () {
		return this.identity;
	}


	/**
	 * Sets the identity. This operation is provided solely for marshaling
	 * purposes.
	 * 
	 * @param identity
	 *            the identity
	 */
	protected void setIdentity (final long identity) {
		this.identity = identity;
	}


	/**
	 * Returns the version. This property is currently inactive.
	 * 
	 * @return the version
	 */
	@JsonbProperty
	public int getVersion () {
		return this.version;
	}


	/**
	 * Sets the version.
	 * 
	 * @return the version
	 */
	public void setVersion (final int version) {
		this.version = version;
	}


	/**
	 * Returns the creation timestamp.
	 * 
	 * @return the creation timestamp in milliseconds since midnight 1/1/1970
	 *         UTC
	 */
	@JsonbProperty
	public long getCreationTimestamp () {
		return this.creationTimestamp;
	}


	/**
	 * Sets the creation timestamp. This operation is provided solely for
	 * marshaling purposes.
	 * 
	 * @param creationTimestamp
	 *            the creation timestamp
	 */
	protected void setCreationTimestamp (final long creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}


	/**
	 * Returns a text representation containing the entity's identity.
	 * 
	 * @return the text representation.
	 */
	@Override
	public String toString () {
		return this.getClass().getName() + '@' + this.getIdentity();
	}


	/**
	 * Returns {@code 0} if the given entity and the receiver represent the same
	 * entity (albeit possibly in different states). Otherwise, returns a
	 * positive value if this entity's identity is larger than the given
	 * entity's, or a negative value if this entity's identity is smaller than
	 * the given entity's identity.
	 * 
	 * @param entity
	 *            the entity
	 * @return a strictly negative value if smaller, zero if entity equal, or a
	 *         strictly positive value if greater than the given entity
	 * @throws NullPointerException
	 *             if the given entity is {@code null}
	 */
	public int compareTo (final BaseEntity entity) {
		return Long.compare(this.getIdentity(), entity.getIdentity());
	}
}