package org.orienteer.jnpm.traversal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.orienteer.jnpm.dm.VersionInfo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
public class TraversalNode {
	private TraversalNode parent;
	@Getter(AccessLevel.NONE)
	private Map<VersionInfo, TraversalTree> modifiableChildren = Collections.synchronizedMap(new HashMap<VersionInfo, TraversalTree>());
	private Collection<TraversalTree> children = Collections.unmodifiableCollection(modifiableChildren.values());
	private int level;
}
