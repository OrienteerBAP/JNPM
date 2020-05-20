package org.orienteer.jnpm.traversal;

import java.util.Collections;
import java.util.HashSet;
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
	private ITraversalVisitor visitor;
	private Function<VersionInfo, Completable> visitCompletableFunction;
	
	@Getter(AccessLevel.NONE)
	private Set<VersionInfo> traversed = Collections.synchronizedSet(new HashSet<VersionInfo>());
	
	public TraversalContext(VersionInfo rootVersion, TraverseDirection direction, ITraversalVisitor visitor) {
		this.rootTree = new TraversalTree(this, null, rootVersion);
		this.direction = direction;
		this.visitor = visitor;
		this.visitCompletableFunction =visitor!=null?visitor::visitCompletable:null;
	}
	
	public TraversalContext(VersionInfo rootVersion, TraverseDirection direction, Function<VersionInfo, Completable> visitCompletableFunction) {
		this.rootTree = new TraversalTree(this, null, rootVersion);
		this.direction = direction;
		this.visitCompletableFunction = visitCompletableFunction;
		this.visitor = visitCompletableFunction!=null?ITraversalVisitor.wrap(visitCompletableFunction):null;
	}
	
	public Completable visitCompletable(VersionInfo version) throws Exception {
		return visitCompletableFunction.apply(version);
	}
	
	public boolean alreadyTraversed(VersionInfo version) {
		return traversed.contains(version);
	}
	
	public void markTraversed(VersionInfo version) {
		traversed.add(version);
	}
	
	public Set<VersionInfo> getTraversed() {
		return Collections.unmodifiableSet(traversed);
	}
	
	public Logger getLogger() {
		return log;
	}
	
}
