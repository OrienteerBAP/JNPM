package org.orienteer.jnpm.dm;

import java.util.Date;
import java.util.List;
import java.util.Map;import org.apache.log4j.rewrite.MapRewritePolicy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;
import lombok.ToString;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
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
		private List<RepositoryInfo> repositories;
		private List<String> keywords;
		private String bugsUrl;
		private List<LicenseInfo> licenses;
		private Map<String, Boolean> users;
		
		@JsonProperty("bugs")
		public void setBugs(JsonNode node) {
			if(node.isTextual()) setBugsUrl(node.asText());
			else if(node.isObject()) {
				String url = null;
				if(node.has("url")) url = node.get("url").asText();
				if(url==null && node.has("name")) url = node.get("name").asText();
				setBugsUrl(url);
			}
		}
		
//		@JsonProperty("homepage")
		@JsonSetter("homepage")
		public void parseHomePage(JsonNode node) {
			if(node.isTextual()) setHomepage(node.asText());
		}
		
		@JsonSetter("repository")
		public void setRepository(List<RepositoryInfo> repository) {
			if(repositories==null) repositories = repository;
			else repositories.addAll(repository);
		}
		
		@JsonSetter("license")
		public void setLicense(List<LicenseInfo> license) {
			if(licenses==null) licenses = license;
			else licenses.addAll(license);
		}
		
}
