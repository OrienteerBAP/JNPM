package org.orienteer.jnpm.dm.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.orienteer.jnpm.dm.AbstractArtifactInfo;
import org.orienteer.jnpm.dm.HumanInfo;
import org.orienteer.jnpm.dm.RepositoryInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SearchResultPackage extends AbstractArtifactInfo {
	
	private HumanInfo publisher;
	private String npmUrl;
	
	@JsonProperty("links")
	public void setLinks(Map<String, String> links) {
		for(Map.Entry<String, String> entry : links.entrySet()) {
			switch (entry.getKey()) {
				case "homepage":
					setHomepage(entry.getValue());
					break;
				case "repository":
					RepositoryInfo rep = new RepositoryInfo();
					setRepositories(Arrays.asList(rep));
				case "bugs":
					setBugsUrl(entry.getValue());
					break;
				case "npm":
					setNpmUrl(entry.getValue());
					break;

			}
		}
	}
}
