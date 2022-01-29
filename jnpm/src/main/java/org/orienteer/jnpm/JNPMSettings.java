package org.orienteer.jnpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * Container for JNPM settings 
 */
@Builder(toBuilder = true)
@Value
public class JNPMSettings {
	
	public static final String DEFAULT_REGISTRY_URL = "http://registry.npmjs.org/";
	
	@Builder.Default private String registryUrl = DEFAULT_REGISTRY_URL;
	@Builder.Default private Path homeDirectory = Paths.get(System.getProperty("user.home"), ".jnpm");
	@Builder.Default private boolean validateSignature = true;
	@Builder.Default private ILogger logger = ILogger.DEFAULT;
	@Builder.Default private Level httpLoggerLevel = Level.NONE;
	private String username;
	@ToString.Exclude private String password;
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
