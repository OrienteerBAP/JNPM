package org.orienteer.jnpm.traversal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.orienteer.jnpm.dm.VersionInfo;

public class TraversalRule implements ITraversalRule {
	
	private final String name;
	private final Function<VersionInfo, Map<String, String>> extractor;
	
	public TraversalRule(String name, Function<VersionInfo, Map<String, String>> extractor) {
		this.name = name;
		this.extractor = extractor;
	}
	
	@Override
	public Map<String, String> getNextDependencies(VersionInfo version) {
		if(version==null) return new HashMap<String, String>();
		Map<String, String> ret = extractor.apply(version);
		return ret!=null?ret:new HashMap<String, String>();
	}
	
	@Override
	public String toString() {
		return "TraversalRule: "+name;
	}
	
}
