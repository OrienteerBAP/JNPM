package org.orienteer.jnpm.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.JNPMSettings.JNPMSettingsBuilder;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "jnpm", mixinStandardHelpOptions = true, 
description = "Java implementation of Node Package Manager",
subcommands = DownloadCommand.class)
@Slf4j
public class JNPM implements Callable<Integer> {
	
	private static JNPMSettings DEFAULT_SETTINGS = JNPMSettings.builder().build();
	
	@Option(names = "--home-dir", description = "Home directory for JNPM (default: ${DEFAULT-VALUE})")
	private Path homeDirectory = DEFAULT_SETTINGS.getHomeDirectory();
	
	@Option(names = "--download-dir", description = "Cache directory for JNPM to download packages to (default: <home-dir>/cache/)")
	private Path downloadDirectory;
	
	public static void main(String... args) {
		CommandLine top = new CommandLine(new JNPM());
		int exitCode = top.execute(args);
        System.exit(exitCode);
	}
	
	public void configure() {
		JNPMSettings.JNPMSettingsBuilder builder = JNPMSettings.builder();
		builder.homeDirectory(homeDirectory);
		if(downloadDirectory!=null) builder.downloadDirectory(downloadDirectory);
		JNPMService.configure(builder.build());
	}

	@Override
	public Integer call() throws Exception {
		CommandLine.usage(this, System.err);
		return -1;
	}
}
