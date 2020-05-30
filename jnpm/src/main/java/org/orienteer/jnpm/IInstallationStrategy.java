package org.orienteer.jnpm;

import java.nio.file.Path;
import java.util.function.Function;

import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.traversal.TraversalTree;

public interface IInstallationStrategy {
	
	public static final IInstallationStrategy FLAT_EXTRACT = new IInstallationStrategy() {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath.resolve(versionFolder(tree.getVersion()));
		}
	};
	
	public static final IInstallationStrategy NPM_LIKE = new IInstallationStrategy() {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			TraversalTree path[] = tree.getPath();
			Path ret = rootPath;
			for (TraversalTree traversalTree : path) {
				ret = ret.resolve("node_modules").resolve(traversalTree.getVersion().getName());
			}
			return ret;
		}
	};
	
	public static final IInstallationStrategy FLAT_SIMPLE_EXTRACT = new IInstallationStrategy() {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath.resolve(tree.getVersion().getName());
		}
	};
	
	
	
	public Path mapPath(Path rootPath, TraversalTree tree);
	
	public default String versionFolder(VersionInfo version) {
		return version.getName()+"-"+version.getVersionAsString();
	}
	
	public default Function<String, String> entreeNameMapper() {
		return JNPMUtils.stringReplacer("package/", "");
	}
}
