package org.orienteer.jnpm.dm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.compress.utils.IOUtils;
import org.orienteer.jnpm.JNPM;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import io.reactivex.Completable;
import io.reactivex.Observable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonNaming
//@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class VersionInfo extends AbstractArtifactInfo implements Comparable<VersionInfo>{
	
	@JsonIgnore
	private Version version;
	private String versionAsString;
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
	
	public Completable download(boolean getThis, boolean dep, boolean devDep, boolean optDep, boolean peerDep) {
		final Set<VersionInfo> downloaded = Collections.synchronizedSet(new HashSet<VersionInfo>());
		return download(downloaded, getThis, dep, devDep, optDep, peerDep);
	}
	
	private Completable download(Set<VersionInfo> context, boolean getThis, boolean dep, boolean devDep, boolean optDep, boolean peerDep) {
		return Completable.defer(() -> {
			log.info("Package: "+getName()+"@"+getVersionAsString());
			Map<String, String> toDownload = new HashMap<>();
			if(dep && dependencies!=null) toDownload.putAll(dependencies);
			if(devDep && devDependencies!=null) toDownload.putAll(devDependencies);
			if(optDep && optionalDependencies!=null) toDownload.putAll(optionalDependencies);
			if(peerDep && peerDependencies!=null) toDownload.putAll(peerDependencies);
			log.info("To Download:"+toDownload);
			Completable deps =  Observable.fromIterable(toDownload.entrySet())
								.flatMapMaybe(e-> JNPM.instance().getNpmRegistryService()
														.bestMatch(e.getKey(), e.getValue()))
								.doOnError(e -> log.error("Error during handing "+getName()+"@"+getVersionAsString()+" ToDownload: "+toDownload, e))
								.filter(v -> !context.contains(v))
								.doOnNext(v -> context.add(v))
								.flatMapCompletable(v -> v.download(context, true, true, false, false, false));
			return getThis?Completable.mergeArray(downloadTarball(), deps):deps;
		});
	}
	
	public Completable downloadTarball() {
		return Completable.defer(() ->{
			File file = getLocalTarball();
			if(file.exists()) return Completable.complete();
			else {
				return JNPM.instance().getNpmRegistryService()
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
		});
	}
	
	public File getLocalTarball() {
		return JNPM.instance().getSettings().getDownloadDirectory().resolve(getDist().getTarballName()).toFile();
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
	
}
