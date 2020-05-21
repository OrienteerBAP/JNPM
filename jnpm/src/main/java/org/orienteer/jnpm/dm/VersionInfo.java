package org.orienteer.jnpm.dm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.compress.utils.IOUtils;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraverseDirection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonNaming
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class VersionInfo extends AbstractArtifactInfo implements Comparable<VersionInfo>{
	
	@JsonIgnore
	private Version version;
	@ToString.Include private String versionAsString;
	private String main;
	private Map<String, String> scripts;
	private Map<String, String> gitHooks;
	private Map<String, String> dependencies;
	private Map<String, String> optionalDependencies;
	private Map<String, String> devDependencies;
	private Map<String, String> peerDependencies;
	private List<String> bundleDependencies;
	private String gitHead;
	private String nodeVersion;
	private String npmVersion;
	private DistributionInfo dist;
	private HumanInfo npmUser;
	private String unpkg;
	private String jsdelivr;
	private String module;
	private String types;
	private boolean sideEffects = false;
	
	/*
	 * public Single<TraversalContext> traverse(TraverseDirection direction, boolean
	 * doForThis, ITraversalRule rule, ITraversalVisitor visitor) { TraversalContext
	 * ctx = new TraversalContext(null, direction, visitor); return traverse(ctx,
	 * doForThis, rule) .toSingleDefault(ctx); }
	 * 
	 * public Single<TraversalContext> traverse(TraverseDirection direction, boolean
	 * doForThis, ITraversalRule rule, Function<VersionInfo, Completable>
	 * visitCompletableFunction) { TraversalContext ctx = new TraversalContext(null,
	 * direction, visitCompletableFunction); return traverse(ctx, doForThis, rule)
	 * .toSingleDefault(ctx); }
	 * 
	 * private Completable traverse(TraversalContext ctx, boolean doForThis,
	 * ITraversalRule rule) { return Completable.defer(() -> {
	 * log.info("Package: "+getName()+"@"+getVersionAsString()); List<Completable>
	 * setToDo = new ArrayList<>(); if(doForThis)
	 * setToDo.add(ctx.visitCompletable(this));
	 * 
	 * Map<String, String> toDownload = getNextDependencies(rule);
	 * log.info("To Download:"+toDownload);
	 * 
	 * if(!toDownload.isEmpty()) { //Need to download first and then go deeper
	 * Flowable<VersionInfo> cachedDependencies =
	 * Observable.fromIterable(toDownload.entrySet()) .flatMapMaybe(e->
	 * JNPMService.instance().getRxService() .bestMatch(e.getKey(), e.getValue()))
	 * .doOnError(e ->
	 * log.error("Error during handing "+getName()+"@"+getVersionAsString()
	 * +" ToDownload: "+toDownload, e)) .filter(v -> !ctx.alreadyTraversed(v))
	 * .doOnNext(v -> ctx.markTraversed(v)) .cache()
	 * .toFlowable(BackpressureStrategy.BUFFER); switch (ctx.getDirection()) { case
	 * WIDER: // Download tarballs first
	 * setToDo.add(cachedDependencies.flatMapCompletable(ctx.
	 * getVisitCompletableFunction())); // Go to dependencies
	 * setToDo.add(cachedDependencies.flatMapCompletable(v -> v.traverse(ctx, false,
	 * ITraversalRule.DEPENDENCIES))); break; case DEEPER: // Go to dependencies
	 * right away setToDo.add(cachedDependencies.flatMapCompletable(v ->
	 * v.traverse(ctx, true, ITraversalRule.DEPENDENCIES))); break; } } return
	 * Completable.concat(setToDo); }); }
	 */
	
	public Completable download(boolean getThis, ITraversalRule... rules) {
		return JNPMService.instance().getRxService()
				.traverse(this, TraverseDirection.WIDER, getThis, ITraversalRule.combine(rules))
				.flatMapCompletable(t -> t.getVersion().downloadTarball());
	}
	
	
	public Completable downloadTarball() {
		return Completable.defer(() ->{
			File file = getLocalTarball();
			if(file.exists()) return Completable.complete();
			else {
				return JNPMService.instance().getRxService()
					.downloadFile(getDist().getTarball())
					.map((r)->{
						InputStream is = r.body().byteStream();
						log.info("Trying create file on path: "+file.getAbsolutePath());
						file.createNewFile();
						FileOutputStream fos = new FileOutputStream(file);
						IOUtils.copy(is, fos);
						is.close();
						fos.close();
						return file;
					}).ignoreElement();
			}
		}).subscribeOn(Schedulers.io());
	}
	
	public Map<String, String> getNextDependencies(ITraversalRule rule) {
		return rule.getNextDependencies(this);
	}
	
	public File getLocalTarball() {
		return JNPMService.instance().getSettings().getDownloadDirectory().resolve(getDist().getTarballName()).toFile();
	}
	
	public String getVersionAsString() {
		return version!=null?version.toString():versionAsString;
	}
	
	@JsonProperty("version")
	public void setVersionAsString(String version) {
		try {
			this.version = Version.valueOf(version);
			this.versionAsString = null;
		} catch (ParseException e) {
			this.version = null;
			this.versionAsString = version;
		}
	}
	
	public boolean isVersionValid() {
		return version!=null && versionAsString ==null;
	}
	
	public boolean satisfies(String expression) {
		return version!=null && version.satisfies(expression);
	}
	
	public boolean satisfies(Predicate<Version> predicate) {
		return version!=null && predicate.test(version);
	}

	@Override
	public int compareTo(VersionInfo o) {
		Version version = getVersion();
		Version thatVersion = o.getVersion();
		if(version!=null && thatVersion!=null) return version.compareTo(thatVersion);
		else return getVersionAsString().compareTo(o.getVersionAsString());
	}
	
	@Override
	public String toString() {
		return "Version(\""+getName()+"@"+getVersionAsString()+"\")";
	}
	
}
