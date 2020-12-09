package org.orienteer.jnpm.traversal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.orienteer.jnpm.dm.VersionInfo;
import org.slf4j.Logger;

import io.reactivex.Observable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Root node for all traversal: direct childs represents sets of initial packages to be traversed
 */
@Value
@Slf4j
public class TraversalContext extends AbstractTraversalNode {
	private TraverseDirection direction;
	
	@Getter(AccessLevel.NONE)
	private Map<VersionInfo, TraversalTree> traversed = new ConcurrentHashMap<VersionInfo, TraversalTree>();
	
	public TraversalContext(TraverseDirection direction, ITraversalRule rule, VersionInfo... roots) {
		super(rule);
		this.direction = direction;
		for (VersionInfo versionInfo : roots) {
			this.modifiableChildren.put(versionInfo, new TraversalTree(this, null, versionInfo, rule));
		}
		this.level=-1;
	}
	
	public boolean alreadyTraversed(VersionInfo version, TraversalTree thisTree) {
		TraversalTree traversedInThatTree = traversed.get(version);
		return traversedInThatTree!=null && !traversedInThatTree.equals(thisTree);
	}
	
	public void markTraversed(VersionInfo version, TraversalTree tree) {
		traversed.putIfAbsent(version, tree);
	}
	
	public Set<VersionInfo> getTraversed() {
		return Collections.unmodifiableSet(traversed.keySet());
	}
	
	public Logger getLogger() {
		return log;
	}
	
	@Override
	public Observable<TraversalTree> getNextTraversalNodes() {
		return Observable.fromIterable(getChildren());
	}
	
	@Override
	public TraversalContext getContext() {
		return this;
	}
	
	@Override
	public String toString() {
		return "TraversalContext("+traversed.size()+" items)";
	}
	
}
