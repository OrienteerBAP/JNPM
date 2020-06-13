package org.orienteer.jnpm.traversal;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.orienteer.jnpm.IInstallationStrategy;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

import io.reactivex.Completable;
import io.reactivex.Observable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@EqualsAndHashCode(of={"depender", "version"})
public class TraversalTree extends AbstractTraversalNode {
	
	private TraversalContext context;
	private TraversalTree depender;	
	@Getter(AccessLevel.NONE)
	private Map<VersionInfo, TraversalTree> modifiableDependencies = Collections.synchronizedMap(new HashMap<VersionInfo, TraversalTree>());
	private Collection<TraversalTree> dependencies = Collections.unmodifiableCollection(modifiableDependencies.values());
	private VersionInfo version;
	private int dependencyLevel;
	@NonFinal
	private boolean duplicate = false;
	
	TraversalTree(TraversalContext context, TraversalTree depender, VersionInfo version, ITraversalRule rule) {
		super(depender!=null?depender:context, rule);
		this.context = context;
		this.depender = depender;
		this.version = version;
		this.dependencyLevel = depender!=null?depender.dependencyLevel+1:0;
	}
	
	public Completable install(final Path targetFolder, final IInstallationStrategy strategy) {
		return Completable.concatArray(getVersion().downloadTarball(),
								Completable.fromAction(()-> {
									File tarball = getVersion().getLocalTarball();
									JNPMUtils.extractTarball(tarball, strategy.mapPath(targetFolder, TraversalTree.this), strategy.entreeNameMapper());
								}));
	}
	
	public TraversalTree commit() {
		if(depender!=null) {
			depender.modifiableDependencies.put(version, this);
		}
		context.markTraversed(version, this);
		if(context.alreadyTraversed(version, this)) markAsDuplicate();
		
		if(parent!=null) {
			//TODO: Adjust parent per dependencies
			AbstractTraversalNode betterParent = findProperParent(version);
			if(betterParent!=null) parent = betterParent;
			level = parent.getLevel()+1;
			parent.modifiableChildren.put(version, this);
		}
		return this;
	}
	
	public void markAsDuplicate() {
		this.duplicate = true;
	}
	
	public TraversalTree subTreeFor(VersionInfo version) {
		TraversalTree ret = modifiableDependencies.get(version);
		if(ret==null) {
			ret = new TraversalTree(context, this, version, TraversalRule.DEPENDENCIES);
		}
		return ret;
	}
	
	@Override
	public Observable<TraversalTree> getNextTraversalNodes() {
		return getVersion().getDependencies(rule).map(v -> subTreeFor(v));
	}
	
	@Override
	public boolean isTraversableDeeper() {
		return !isDuplicate();
	}
	
	public TraversalTree[] getPath() {
		TraversalTree[] ret = new TraversalTree[getLevel()+1];
		for(AbstractTraversalNode current = this; current!=null; current = current.getParent()) {
			if(current instanceof TraversalTree)
				ret[current.getLevel()] = (TraversalTree) current;
		}
		return ret;
	}
	
	@Override
	public String toString() {
		return "TraversalTree(\""+version.getName()+"@"+version.getVersionAsString()+"\", level="+getLevel()+", dependencyLevel="+dependencyLevel+", duplicate="+duplicate+")";
	}

}
