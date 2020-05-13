package org.orienteer.jnpm.dm;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class VersionInfo extends AbstractArtifactInfo {
	
	@JsonIgnore
	private Version version;
	private String versionAsString;
	private String main;
	private Map<String, String> scripts;
	private Map<String, String> gitHooks;
	private Map<String, String> dependencies;
	private Map<String, String> optionalDependencies;
	private Map<String, String> devDependencies;
	private Map<String, String> peerDependencies;
	private List<String> bundleDependencies;
	private String gitHead;
	private String nodeVersion;
	private String npmVersion;
	private DistributionInfo dist;
	private HumanInfo npmUser;
	private String unpkg;
	private String jsdelivr;
	private String module;
	private String types;
	private boolean sideEffects = false;
	
	public String getVersionAsString() {
		return version!=null?version.toString():versionAsString;
	}
	
	@JsonProperty("version")
	public void setVersionAsString(String version) {
		try {
			this.version = Version.valueOf(version);
			this.versionAsString = null;
		} catch (ParseException e) {
			this.version = null;
			this.versionAsString = version;
		}
	}
	
	public boolean isVersionValid() {
		return version!=null && versionAsString ==null;
	}
	
	public boolean satisfies(String expression) {
		return version!=null && version.satisfies(expression);
	}
	
	public boolean satisfies(Predicate<Version> predicate) {
		return version!=null && predicate.test(version);
	}
	
}
