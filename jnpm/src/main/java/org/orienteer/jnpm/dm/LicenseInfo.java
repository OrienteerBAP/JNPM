package org.orienteer.jnpm.dm;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Data class to store license information
 */
@Data
@JsonNaming
public class LicenseInfo {
	private String type;
	private String url;
	
	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public LicenseInfo(String type) {
        this(type, null);
    }
	
	@JsonCreator
    public LicenseInfo(@JsonProperty("type") String type, @JsonProperty("url") String url) {
        this.type = type;
        this.url = url;
    }

}
