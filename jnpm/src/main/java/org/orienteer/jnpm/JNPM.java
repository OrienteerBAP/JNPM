package org.orienteer.jnpm;

import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.orienteer.jnpm.commands.DownloadCommand;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "jnpm", mixinStandardHelpOptions = true, 
description = "Java implementation of Node Package Manager",
subcommands = DownloadCommand.class)
@Slf4j
public class JNPM implements Callable<Integer> {
	public static void main(String... args) {
		JNPMService.configure(JNPMSettings.builder().build());
		CommandLine top = new CommandLine(new JNPM());
		int exitCode = top.execute(args);
        System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {
		CommandLine.usage(this, System.err);
		return -1;
	}
}
