package org.orienteer.jnpm.cli;

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;

import okhttp3.logging.HttpLoggingInterceptor.Level;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main class for JNPM CLI
 */
@Command(name = "jnpm", mixinStandardHelpOptions = true, 
description = "Java implementation of Node Package Manager",
subcommands = {DownloadCommand.class, ExtractCommand.class})
public class JNPM implements Callable<Integer> {
	
	private static final JNPMSettings DEFAULT_SETTINGS = JNPMSettings.builder().build();
	
	@Option(names = "--home-dir", description = "Home directory for JNPM (default: ${DEFAULT-VALUE})")
	private Path homeDirectory = DEFAULT_SETTINGS.getHomeDirectory();
	
	@Option(names = "--download-dir", description = "Cache directory for JNPM to download packages to (default: <home-dir>/cache/)")
	private Path downloadDirectory;
	
	@Option(names = "--install-dir", description = "Global install directory for JNPM (default: <home-dir>/node_modules/)")
	private Path installDirectory;
	
	@Option(names = "--registry", description = "NPM registry URL to be used for package lookup and retrieval (default: "+JNPMSettings.DEFAULT_REGISTRY_URL+")")
	private URL registryUrl;
	
	@Option(names = {"-U", "--username"}, description = "Username for authentication (optional)")
	private String username;
	
	@Option(names = {"-P", "--password"}, description = "Password for authentication (optional)")
	private String password;
	
	@Option(names = {"-L", "--http-logger-level"}, description = {"HTTP Logger Level for debugging", 
																	"Valid values: ${COMPLETION-CANDIDATES}"})
	private Level httpLoggerLevel;
	
	@Option(names = {"-f", "--force"}, description = "Force to fetch remote resources even if a local copy exists on disk")
    private boolean forceDownload = false;
	
	public static void main(String... args) {
		CommandLine top = new CommandLine(new JNPM());
		int exitCode = top.execute(args);
        System.exit(exitCode);
	}
	
	public void configure() {
		JNPMSettings.JNPMSettingsBuilder builder = JNPMSettings.builder();
		builder.homeDirectory(homeDirectory);
		if(downloadDirectory!=null) builder.downloadDirectory(downloadDirectory);
		if(registryUrl!=null) builder.registryUrl(registryUrl.toString());
		builder.username(username).password(password);
		if(httpLoggerLevel!=null) builder.httpLoggerLevel(httpLoggerLevel);
		builder.useCache(!forceDownload);
		JNPMService.configure(builder.build());
	}

	@Override
	public Integer call() throws Exception {
		CommandLine.usage(this, System.err);
		return -1;
	}
}
