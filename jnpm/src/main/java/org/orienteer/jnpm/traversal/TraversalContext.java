package org.orienteer.jnpm.traversal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.orienteer.jnpm.dm.VersionInfo;

import io.reactivex.Completable;
import io.reactivex.functions.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.AccessLevel;

@Value
public class TraversalContext {
	private TraverseDirection direction;
	private ITraversalVisitor visitor;
	private Function<VersionInfo, Completable> visitCompletableFunction;
	
	@Getter(AccessLevel.NONE)
	private Set<VersionInfo> traversed = Collections.synchronizedSet(new HashSet<VersionInfo>());
	
	public TraversalContext(TraverseDirection direction, ITraversalVisitor visitor) {
		this.direction = direction;
		this.visitor = visitor;
		this.visitCompletableFunction = visitor::visitCompletable;
	}
	
	public TraversalContext(TraverseDirection direction, Function<VersionInfo, Completable> visitCompletableFunction) {
		this.direction = direction;
		this.visitCompletableFunction = visitCompletableFunction;
		this.visitor = ITraversalVisitor.wrap(visitCompletableFunction);
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
	
}
