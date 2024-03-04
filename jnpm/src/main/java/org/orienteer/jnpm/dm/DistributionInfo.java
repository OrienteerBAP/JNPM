package org.orienteer.jnpm.dm;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Data class to store package distribution information
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DistributionInfo extends AbstractInfo {
	
	private String integrity;
	private String shasum;
	private String tarball;
	private int fileCount = -1;
	private long unpackedSize = -1;
	private String npmSignature;
	
	public String getTarballName() {
		try {
			String path = new URL(tarball).getPath();
			return path.substring(path.lastIndexOf("/")+1);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("URL is incorrect. We should not recieve such urls from NPM", e);
		}
	}
}
