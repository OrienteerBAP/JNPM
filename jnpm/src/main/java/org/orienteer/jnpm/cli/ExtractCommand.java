package org.orienteer.jnpm.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.orienteer.jnpm.InstallationStrategy;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.traversal.TraversalTree;

import io.reactivex.Completable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Extract command for JNPM CLI. Please read command description in corresponding annotation.
 */
@Command(name="extract", aliases = "e", description = "Extract packages: very similiar to 'npm install', but without changing package.json modification")
@Slf4j
public class ExtractCommand extends DownloadCommand {

	@Option(names = "-g", description = "Extract package(s) globally")
	private boolean global = false;
	
	@Option(names = {"-o", "--output"}, description = "Output folder to extract to")
	private Path folder = Paths.get("").toAbsolutePath();
	
	@Option(names = {"-s", "--strategy"}, description = "Strategy for extraction: ${COMPLETION-CANDIDATES}")
	private InstallationStrategy strategy = InstallationStrategy.NPM;
	
	@Override
	public Integer call() throws Exception {
		handledFormat = "Installing %s@%s\n";
		if(global) folder = JNPMService.instance().getSettings().getInstallDirectory();
		return super.call();
	}
	
	protected Completable doAction(TraversalTree tree) {
		return tree.install(folder, strategy);
	}
}
