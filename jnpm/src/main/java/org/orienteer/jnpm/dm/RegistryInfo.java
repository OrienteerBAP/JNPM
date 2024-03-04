package org.orienteer.jnpm.dm;

import java.util.Date;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Data class to store information about NPM registry
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RegistryInfo extends AbstractInfo {
	
	/*db_name: "registry"
		doc_count: 376841,
		doc_del_count: 354,
		update_seq: 2889325,
		purge_seq: 0,
		compact_running: false,
		disk_size: 2098360443,
		data_size: 1485346312,
		instance_start_time: "1471680653634734",
		disk_format_version: 6,
		committed_update_seq: 2889325*/
	
	private String dbName;
	private int docCount;
	private int docDelCount;
	private int updateSeq;
	private int purgeSeq;
	private boolean compactRunning;
	private long diskSize;
	private long dataSize;
	private Date instanceStartTime;
	private int diskFormatVersion;
	private int committedUpdateSeq;
	
}
