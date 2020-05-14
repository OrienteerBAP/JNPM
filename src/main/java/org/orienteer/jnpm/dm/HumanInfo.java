package org.orienteer.jnpm.dm;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class HumanInfo extends AbstractInfo {

	private String name;
	private String email;
	private String url;
	
	public HumanInfo() {
		
	}
	
	public HumanInfo(String data) {
		if(data!=null && data.trim().length()>0) {
			if(data.contains("@")) email = data;
			else name = data;
		}
	}
	
	public String getUsername() {
		return getName();
	}
	
	public void setUsername(String username) {
		setName(username);
	}
}
