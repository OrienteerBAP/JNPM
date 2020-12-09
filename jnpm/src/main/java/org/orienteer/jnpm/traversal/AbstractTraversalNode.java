package org.orienteer.jnpm.traversal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.orienteer.jnpm.dm.VersionInfo;

import io.reactivex.Observable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * Abstract class for traversal results: parent for {@link TraversalContext} (root) and {@link TraversalTree} (sub-trees to reflect dependencies)
 */
@Value
@NonFinal
public abstract class AbstractTraversalNode {
	@NonFinal
	protected AbstractTraversalNode parent;
	@Getter(AccessLevel.NONE)
	protected Map<VersionInfo, TraversalTree> modifiableChildren = new ConcurrentHashMap<VersionInfo, TraversalTree>();
	private Collection<TraversalTree> children = Collections.unmodifiableCollection(modifiableChildren.values());
	@NonFinal
	protected int level;
	protected ITraversalRule rule;
	
	public AbstractTraversalNode(ITraversalRule rule) {
		this(null, rule);
	}
	
	public AbstractTraversalNode(AbstractTraversalNode parent, ITraversalRule rule) {
		this.parent = parent;
		this.rule = rule;
		this.level = parent!=null?parent.level+1:0;
	}
	
	public abstract Observable<TraversalTree> getNextTraversalNodes();
	
	public boolean isTraversableDeeper() {
		return true;
	}
	
	protected AbstractTraversalNode findProperParent(VersionInfo version) {
		AbstractTraversalNode ret = null;
		if(parent!=null) {
			ret = parent.findProperParent(version);
			if(ret!=null) return ret;
		}
		Optional<VersionInfo> found = modifiableChildren.keySet().stream()
												.filter(v -> 
															version.getName().equals(v.getName())
															&& !version.getVersionAsString().equals(v.getVersionAsString()))
												.findAny();
		return found.isPresent()?null:this;
	}
	
	public abstract TraversalContext getContext();
}
