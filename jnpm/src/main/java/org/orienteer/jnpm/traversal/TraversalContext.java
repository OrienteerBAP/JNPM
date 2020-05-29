package org.orienteer.jnpm.traversal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.orienteer.jnpm.dm.VersionInfo;
import org.slf4j.Logger;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;

@Value
@Slf4j
@ToString(of= {"children","direction"})
public class TraversalContext extends AbstractTraversalNode {
	private TraverseDirection direction;
	
	@Getter(AccessLevel.NONE)
	private Map<VersionInfo, TraversalTree> traversed = new ConcurrentHashMap<VersionInfo, TraversalTree>();
	
	public TraversalContext(TraverseDirection direction, VersionInfo... roots) {
		this.direction = direction;
		for (VersionInfo versionInfo : roots) {
			this.modifiableChildren.put(versionInfo, new TraversalTree(this, null, versionInfo));
		}
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
	public Observable<TraversalTree> getNextTraversalNodes(ITraversalRule rule) {
		return Observable.fromIterable(getChildren());
	}
	
	@Override
	public TraversalContext getContext() {
		return this;
	}
	
}
