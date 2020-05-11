package org.orienteer.jnpm.dm;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DistributionInfo extends AbstractInfo {
	
	private String integrity;
	private String shasum;
	private String tarball;
	private int fileCount = -1;
	private long unpackedSize = -1;
	private String npmSignature;
}
