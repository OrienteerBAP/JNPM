package org.orienteer.jnpm.dm;

import java.util.Date;
import java.util.List;
import java.util.Map;import org.apache.log4j.rewrite.MapRewritePolicy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public abstract class AbstractArtifactInfo extends AbstractInfo{
	
		@JsonProperty("_id")
		private String id;
		private String name;
		private String description;
		private String homepage;
		private String readme;
		private String readmeFilename;
		private HumanInfo author;
		private List<HumanInfo> maintainers;
		private List<HumanInfo> contributors;
		private RepositoryInfo repository;
		private List<String> keywords;
		private String bugsUrl;
		private String license;
		private Map<String, Boolean> users;
		
		@JsonProperty("bugs")
		public void setBugs(Map<String, String> map) {
			setBugsUrl(map.get("url"));
		}
		
}
