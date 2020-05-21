package org.orienteer.jnpm.commands;

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

@Command(name="download", aliases = "d", description = "Download packages into local cache")
@Slf4j
public class DownloadCommand implements Callable<Integer>{
	
	@Option(names = "--save-exact", description = "Get only specified package")
	private boolean saveExact = false;
	
	@Option(names = "--no-save", description = "Don't download. Just traverse dependencies")
	private boolean noSave = false;
	
	@Parameters(index = "0", description = "Package to be retrieved", arity = "1")
    private String packageStatement;

	@Override
	public Integer call() throws Exception {
		VersionInfo version = JNPMService.instance().bestMatch(packageStatement);
		if(version==null) {
			System.out.printf("Package '%s' was not found\n", packageStatement);
			return -1;
		} else {
			System.out.printf("Package '%s@%s' was found for query '%s' \n", version.getName(), version.getVersionAsString(), packageStatement);
			RxJNPMService rxService = JNPMService.instance().getRxService();
			ITraversalRule rule = saveExact?ITraversalRule.NO_DEPENDENCIES:ITraversalRule.DEPENDENCIES;
			Observable<TraversalTree> observable = rxService.traverse(version, TraverseDirection.WIDER, true, rule)
					.doOnNext(t->System.out.printf("Downloading %s@%s\n", t.getVersion().getName(), t.getVersion().getVersionAsString()));
			if(noSave) {
				observable.ignoreElements().blockingAwait();
			} else {
				observable.flatMapCompletable(t -> t.getVersion().downloadTarball()).blockingAwait();
			}
			return 0;
		}
	}

}
