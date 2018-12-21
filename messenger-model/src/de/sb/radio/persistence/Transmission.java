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
	@Column(name = "lastTransmissionTimestamp" ,nullable = true, updatable = true)
	private Long timestamp;

	@Column(name = "lastTransmissionAddress" ,nullable = true, updatable = true, length = 63)
	private String address;
	
	public Transmission() {}

	@JsonbProperty
	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@JsonbProperty
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
