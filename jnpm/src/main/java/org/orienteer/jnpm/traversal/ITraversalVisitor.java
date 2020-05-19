package org.orienteer.jnpm.traversal;

import org.orienteer.jnpm.dm.VersionInfo;

import io.reactivex.Completable;
import io.reactivex.functions.Function;

/**
 * Rx Visitor for traversing.
 * One of the methods should be implemented
 */
public interface ITraversalVisitor {
	public default void visit(VersionInfo version) {
		visitCompletable(version).blockingAwait();
	}
	
	public default Completable visitCompletable(VersionInfo version) {
		return Completable.fromAction(() -> visit(version));
	}
	
	public static ITraversalVisitor wrap(final Function<VersionInfo, Completable> visitCompletable) {
		return new ITraversalVisitor() {

			@Override
			public Completable visitCompletable(VersionInfo version) {
				try {
					return visitCompletable.apply(version);
				} catch (Exception e) {
					throw new IllegalStateException("We should not have this unchecked exception", e);
				}
			}
		};
	}
}
