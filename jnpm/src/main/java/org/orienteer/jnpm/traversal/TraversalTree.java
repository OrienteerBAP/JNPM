package org.orienteer.jnpm.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orienteer.jnpm.dm.VersionInfo;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@ToString(of = {"level", "version", "duplicate"})
@EqualsAndHashCode(of={"parent", "version"})
public class TraversalTree {
	
	private TraversalContext context;
	private TraversalTree parent;	
	@Getter(AccessLevel.NONE)
	private Map<VersionInfo, TraversalTree> modifiableChildren = Collections.synchronizedMap(new HashMap<VersionInfo, TraversalTree>());
	private Collection<TraversalTree> children = Collections.unmodifiableCollection(modifiableChildren.values());
	private VersionInfo version;
	private int level;
	@NonFinal
	private boolean duplicate = false;
	
	public TraversalTree(TraversalContext context, TraversalTree parent, VersionInfo version) {
		this.context = context;
		this.parent = parent;
		this.version = version;
		this.level = parent!=null?parent.level+1:0;
	}
	
	public TraversalTree commit() {
		if(parent!=null) {
			parent.modifiableChildren.put(version, this);
		}
		context.markTraversed(version, this);
		if(context.alreadyTraversed(version, this)) markAsDuplicate();
		return this;
	}
	
	public void markAsDuplicate() {
		this.duplicate = true;
	}
	
	public TraversalTree subTreeFor(VersionInfo version) {
		TraversalTree ret = modifiableChildren.get(version);
		if(ret==null) {
			ret = new TraversalTree(context, this, version);
		}
		return ret;
	}

}
