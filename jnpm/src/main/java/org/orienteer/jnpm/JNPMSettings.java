package org.orienteer.jnpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Builder;
import lombok.Value;

/**
 * Container for JNPM settings 
 */
@Builder
@Value
public class JNPMSettings {
	
	@Builder.Default private String registryUrl = "http://registry.npmjs.org/";
	@Builder.Default private Path homeDirectory = Paths.get(System.getProperty("user.home"), ".jnpm");
	@Builder.Default private boolean validateSignature = true;
	private Path downloadDirectory;
	private Path installDirectory;
	
	public Path getDownloadDirectory() {
		return downloadDirectory!=null?downloadDirectory:getHomeDirectory().resolve("cache");
	}
	
	public Path getInstallDirectory() {
		return installDirectory!=null?installDirectory:getHomeDirectory();
	}
	
	public void createAllDirectories() throws IOException {
		Files.createDirectories(getHomeDirectory());
		Files.createDirectories(getDownloadDirectory());
		Files.createDirectories(getInstallDirectory());
	}
}
