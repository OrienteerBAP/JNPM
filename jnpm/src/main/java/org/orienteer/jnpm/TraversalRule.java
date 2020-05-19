package org.orienteer.jnpm;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.orienteer.jnpm.dm.VersionInfo;

public class TraversalRule implements ITraversalRule {
	
	private Function<VersionInfo, Map<String, String>> extractor;
	
	public TraversalRule(Function<VersionInfo, Map<String, String>> extractor) {
		this.extractor = extractor;
	}
	
	public Map<String, String> getNextDependencies(VersionInfo version) {
		if(version==null) return new HashMap<String, String>();
		Map<String, String> ret = extractor.apply(version);
		return ret!=null?ret:new HashMap<String, String>();
	}
	
}
