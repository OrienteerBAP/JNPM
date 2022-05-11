package org.orienteer.jnpm;

import java.nio.file.Path;
import java.util.function.Function;

import org.orienteer.jnpm.traversal.TraversalTree;

/**
 * Set of built-in installation strategies
 */
public enum InstallationStrategy implements IInstallationStrategy{
	FLAT {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath.resolve(versionFolder(tree.getVersion()));
		}
	},
	NPM  {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			TraversalTree[] path = tree.getPath();
			Path ret = rootPath;
			for (TraversalTree traversalTree : path) {
				ret = ret.resolve("node_modules").resolve(traversalTree.getVersion().getName());
			}
			return ret;
		}
	},
	SIMPLE {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath.resolve(tree.getVersion().getName());
		}
	},
	SIMPLE_VERSIONED {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath.resolve(tree.getVersion().getName())
						   .resolve(tree.getVersion().getVersionAsString());
		}
	},
	WEBJARS {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath.resolve("META-INF/resources/webjars")
						   .resolve(tree.getVersion().getName())
					       .resolve(tree.getVersion().getVersionAsString());
		}
	},
	ONE_DUMP {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath;
		}
	},
	DIST {
		@Override
		public Path mapPath(Path rootPath, TraversalTree tree) {
			return rootPath;
		}
		
		@Override
		public Function<String, String> entreeNameMapper() {
			return JNPMUtils.stringReplacer("package/dist/", "", true);
		}
	}
}
