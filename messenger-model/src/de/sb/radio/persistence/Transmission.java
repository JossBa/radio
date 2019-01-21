package de.sb.radio.persistence;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;
import de.sb.toolbox.bind.JsonProtectedPropertyStrategy;



@Embeddable
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public class Transmission {
	@Column(name = "lastTransmissionTimestamp", nullable = true, updatable = true)
	private Long timestamp;

	@Column(name = "lastTransmissionAddress", nullable = true, updatable = true, length = 63)
	private String address;
	
	@Column(name = "lastTransmissionOffer", nullable = true, updatable = true, length = 4096)
	private String offer;
	

	@JsonbProperty
	public Long getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@JsonbProperty
	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@JsonbProperty
	public String getOffer() {
		return this.offer;
	}

	public void setOffer(String offer) {
		this.offer = offer;
	}
}