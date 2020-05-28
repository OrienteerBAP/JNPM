package org.orienteer.jnpm.traversal;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.orienteer.jnpm.IInstallationStrategy;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

import io.reactivex.Completable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@ToString(of = {"dependencyLevel", "version", "duplicate"})
@EqualsAndHashCode(of={"depender", "version"})
public class TraversalTree {
	
	private TraversalContext context;
	private TraversalTree depender;	
	@Getter(AccessLevel.NONE)
	private Map<VersionInfo, TraversalTree> modifiableDependencies = Collections.synchronizedMap(new HashMap<VersionInfo, TraversalTree>());
	private Collection<TraversalTree> dependencies = Collections.unmodifiableCollection(modifiableDependencies.values());
	private VersionInfo version;
	private int dependencyLevel;
	@NonFinal
	private boolean duplicate = false;
	
	public TraversalTree(TraversalContext context, TraversalTree depender, VersionInfo version) {
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
		return this;
	}
	
	public void markAsDuplicate() {
		this.duplicate = true;
	}
	
	public TraversalTree subTreeFor(VersionInfo version) {
		TraversalTree ret = modifiableDependencies.get(version);
		if(ret==null) {
			ret = new TraversalTree(context, this, version);
		}
		return ret;
	}

}
