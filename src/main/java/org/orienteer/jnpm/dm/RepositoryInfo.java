package org.orienteer.jnpm.dm;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

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
