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
public class VersionInfo extends AbstractArtifactInfo {
	
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
	
}
