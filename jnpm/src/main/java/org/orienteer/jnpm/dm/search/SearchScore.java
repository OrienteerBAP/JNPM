package org.orienteer.jnpm.dm.search;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Data class to store score for search results
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchScore {
	@JsonProperty("final")
	private double finalScore;
	private double qualityScore;
	private double popularityScore;
	private double maintenanceScore;
	
	@JsonProperty("detail")
	public void setDetail(Map<String, Double> detail) {
		qualityScore = detail.get("quality");
		popularityScore = detail.get("popularity");
		maintenanceScore = detail.get("maintenance");
	}
}
