package org.orienteer.jnpm.traversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orienteer.jnpm.dm.VersionInfo;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@ToString(of = {"version", "duplicate"})
@EqualsAndHashCode(of={"parent", "version"})
public class TraversalTree {
	
	private TraversalContext context;
	private TraversalTree parent;	
	@Getter(AccessLevel.NONE)
	private List<TraversalTree> modifiableChildren = Collections.synchronizedList(new ArrayList<>());
	private List<TraversalTree> children = Collections.unmodifiableList(modifiableChildren);
	private VersionInfo version;
	@NonFinal
	private boolean duplicate = false;
	
	public TraversalTree(TraversalContext context, TraversalTree parent, VersionInfo version) {
		this.context = context;
		this.parent = parent;
		this.version = version;
	}
	
	public TraversalTree commitAsChild() {
		if(parent!=null) {
			parent.modifiableChildren.add(this);
		}
		return this;
	}
	
	public void markAsDuplicate() {
		this.duplicate = true;
	}

}
