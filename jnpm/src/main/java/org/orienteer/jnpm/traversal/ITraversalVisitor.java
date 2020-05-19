package org.orienteer.jnpm.traversal;

import org.orienteer.jnpm.dm.VersionInfo;

import io.reactivex.Completable;

@FunctionalInterface
public interface ITraversalVisitor {
	public void visit(VersionInfo version);
	
	public default Completable visitCompletable(VersionInfo version) {
		return Completable.fromAction(() -> visit(version));
	}
}
