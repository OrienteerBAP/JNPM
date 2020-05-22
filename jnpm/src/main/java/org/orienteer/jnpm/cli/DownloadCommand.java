package org.orienteer.jnpm.cli;

import java.util.concurrent.Callable;

import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.RxJNPMService;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraversalRule;
import org.orienteer.jnpm.traversal.TraversalTree;
import org.orienteer.jnpm.traversal.TraverseDirection;

import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name="download", aliases = "d", description = "Download packages into local cache")
@Slf4j
public class DownloadCommand implements Callable<Integer>{
	
	@Option(names = "--download",negatable = true, description = "Download by default. Negate if  just attempt to lookup is needed")
	private boolean download = false;
	
	@Option(names = {"--prod"}, negatable = true, description = "Download dependencies (default)")
	private boolean getProd = true;
    @Option(names = {"--dev"}, negatable = true, description = "Download dev dependencies")
    private boolean getDev = false;
    @Option(names = {"--optional"},negatable = true,  description = "Download optional dependencies")
    private boolean getOptional = false;
    @Option(names = {"--peer"},negatable = true,  description = "Download peer dependencies")
    private boolean getPeer = false;
    
    @Option(names = {"-h", "--help"}, usageHelp = true)
    private boolean usageHelp;
	
	@Parameters(index = "0", description = "Package to be retrieved", arity = "1")
    private String packageStatement;
	
	
	@ParentCommand
    private JNPM parent;

	@Override
	public Integer call() throws Exception {
		parent.configure();
		VersionInfo version = JNPMService.instance().bestMatch(packageStatement);
		if(version==null) {
			System.out.printf("Package '%s' was not found\n", packageStatement);
			return -1;
		} else {
			System.out.printf("Package '%s@%s' was found for query '%s' \n", version.getName(), version.getVersionAsString(), packageStatement);
			RxJNPMService rxService = JNPMService.instance().getRxService();
			ITraversalRule rule = ITraversalRule.getRuleFor(getProd, getDev, getOptional, getPeer);
			Observable<TraversalTree> observable = rxService.traverse(version, TraverseDirection.WIDER, true, rule)
					.doOnNext(t->System.out.printf("Downloading %s@%s\n", t.getVersion().getName(), t.getVersion().getVersionAsString()));
			if(download) {
				observable.flatMapCompletable(t -> t.getVersion().downloadTarball()).blockingAwait();
			} else {
				observable.ignoreElements().blockingAwait();
			}
			return 0;
		}
	}

}
