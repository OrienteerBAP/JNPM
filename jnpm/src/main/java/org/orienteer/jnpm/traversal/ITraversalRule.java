package org.orienteer.jnpm.traversal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orienteer.jnpm.dm.VersionInfo;

public interface ITraversalRule {

	public static final ITraversalRule DEPENDENCIES = new TraversalRule(VersionInfo::getDependencies);
	public static final ITraversalRule DEV_DEPENDENCIES = new TraversalRule(VersionInfo::getDevDependencies);
	public static final ITraversalRule OPT_DEPENDENCIES = new TraversalRule(VersionInfo::getOptionalDependencies);
	public static final ITraversalRule PEER_DEPENDENCIES = new TraversalRule(VersionInfo::getPeerDependencies);
	
	public static final ITraversalRule NO_DEPENDENCIES = new TraversalRule(v -> new HashMap<String, String>());
	
	public Map<String, String> getNextDependencies(VersionInfo version);
	
	public default Map<String, String> getNextDependencies(TraversalTree tree) {
		return getNextDependencies(tree.getVersion());
	}
	
	public static ITraversalRule getRuleFor(boolean dep, boolean devDep, boolean optDep, boolean peerDep) {
		List<ITraversalRule> rules = new ArrayList<>();
		if(dep) rules.add(DEPENDENCIES);
		if(devDep) rules.add(DEV_DEPENDENCIES);
		if(optDep) rules.add(OPT_DEPENDENCIES);
		if(peerDep) rules.add(PEER_DEPENDENCIES);
		return rules.isEmpty()?
				NO_DEPENDENCIES
				:(rules.size()==1?
						rules.get(0)
						:combine(rules.toArray(new ITraversalRule[rules.size()])));
	}
	
	public static ITraversalRule combine(final ITraversalRule... rules) {
		return new CombinedRule(rules);
	}
	
	static class CombinedRule implements ITraversalRule {
		
		private ITraversalRule[] rules;
		
		public CombinedRule(ITraversalRule... rules) {
			this.rules = rules;
		}
		
		@Override
		public Map<String, String> getNextDependencies(VersionInfo version) {
			Map<String, String> ret = new HashMap<>();
			for (ITraversalRule traversalRule : rules) {
				Map<String, String> mapToAdd = traversalRule.getNextDependencies(version);
				//Potentially we should merge by validating required versions and take greater one
				if(mapToAdd!=null) ret.putAll(mapToAdd);
			}
			return ret;
		}
		
	}
}
