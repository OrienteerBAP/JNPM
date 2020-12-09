package org.orienteer.jnpm.dm;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Data class to store repository information
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RepositoryInfo extends AbstractInfo {
	
	private String type;
	private String url;
	
	public RepositoryInfo() {
		
	}
	
	public RepositoryInfo(String data) {
		if(data!=null && data.trim().length()>0) {
			if(data.contains(":")) url = data;
			else type = data;
		}
	}
}
