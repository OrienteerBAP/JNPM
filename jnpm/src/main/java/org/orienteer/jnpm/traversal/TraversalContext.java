package org.orienteer.jnpm.traversal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.orienteer.jnpm.dm.VersionInfo;
import org.slf4j.Logger;

import io.reactivex.Completable;
import io.reactivex.functions.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;

@Value
@Slf4j
@ToString(of= {"rootTree", "direction"})
public class TraversalContext {
	private TraversalTree rootTree;
	private TraverseDirection direction;
	
	@Getter(AccessLevel.NONE)
	private Map<VersionInfo, TraversalTree> traversed = Collections.synchronizedMap(new HashMap<VersionInfo, TraversalTree>());
	
	public TraversalContext(TraverseDirection direction, VersionInfo rootVersion) {
		this.rootTree = new TraversalTree(this, null, rootVersion);
		this.direction = direction;
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
	
}
