package org.orienteer.jnpm;

import java.nio.file.Path;
import java.util.function.Function;

import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.traversal.TraversalTree;

/**
 * Interface to define strategy how to rollout/install package contents
 */
public interface IInstallationStrategy {
	
	public Path mapPath(Path rootPath, TraversalTree tree);
	
	public default String versionFolder(VersionInfo version) {
		return version.getName()+"-"+version.getVersionAsString();
	}
	
	public default Function<String, String> entreeNameMapper() {
		return JNPMUtils.stringReplacer("package/", "");
	}
}
