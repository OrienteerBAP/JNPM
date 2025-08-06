package org.orienteer.jnpm.dm.search;

import java.util.Date;
import java.util.List;

import org.orienteer.jnpm.dm.AbstractInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Data class to store search results
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchResults extends AbstractInfo {
	private int total;
	private Date time;
	private List<SearchResultItem> objects;
}
