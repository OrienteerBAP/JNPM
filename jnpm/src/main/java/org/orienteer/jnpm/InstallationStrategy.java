package org.orienteer.jnpm;

import java.nio.file.Path;

import org.orienteer.jnpm.traversal.TraversalTree;

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
			TraversalTree path[] = tree.getPath();
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
	};
}