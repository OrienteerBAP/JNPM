package org.orienteer.jnpm.dm;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PackageInfo extends AbstractArtifactInfo {

	@JsonProperty("_rev")
	private String rev;
	@JsonProperty("dist-tags")
	private Map<String, String> distTags;
	private Map<String, VersionInfo> versions;
	private Map<String, Date> time;
	private Map<String, Boolean> users;
	
	public Date getCreated() {
		return getTime().get("created");
	}
	
	public Date getModified() {
		return getTime().get("modified");
	}
	
	public String getLatest() {
		return getDistTags().get("latest");
	}
	
	@Override
	public String toString() {
		return "Package(\""+getName()+"\")";
	}
}
