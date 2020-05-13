package org.orienteer.jnpm;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class JNPMSettings {
	
	@Builder.Default private String registryUrl = "http://registry.npmjs.org/";
	@Builder.Default private Path homeDirectory = Paths.get(System.getProperty("user.home"), ".jnpm");
	private Path downloadDirectory;
	
	public Path getDownloadDirectory() {
		return downloadDirectory!=null?downloadDirectory:getDownloadDirectory().resolve("cache");
	}
}
