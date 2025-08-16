package org.orienteer.jnpm.cdn;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * Class-container to store request for resources out of CDN entry points (for example {@link CDNServlet})
 */
@Value
public class CDNRequest {
	
	private static final VersionInfo NULL_VERSION = new VersionInfo();
	
	private static final String PATH_PATTERN = "/((@[^/]*)/)?([^/@]*)@?([^/]*)(?:/(.*))?";
	private static final Pattern PATH_REGEXP = Pattern.compile(PATH_PATTERN);

	private String scope;
	private String packageName;
	private String versionExpression;
	private String path;
	private String fileName;
	
	@NonFinal
	private Boolean exactVersion;
	
	@NonFinal
	private Boolean forceDownload;
	
	protected CDNRequest(String scope, String packageName, String version, String path) {
		this.scope = scope;
		this.packageName = packageName;
		this.versionExpression = version!=null && !version.isEmpty()?version:"latest";
		if(path != null && !path.isEmpty()) {
			int indx = path.indexOf("?");
			this.path = indx<0?path:path.substring(0, indx);
			indx = path.lastIndexOf("/");
			fileName = indx<0?path:path.substring(indx+1);
		} else {
			this.path = null;
			this.fileName = null;
		}
	}
	
	public boolean isExactVersion() {
		if(exactVersion==null) {
			exactVersion = JNPMUtils.isValidVersion(versionExpression);
		}
		return exactVersion;
	}
	
	public String getPackageVersionExpression() {
		if(scope!=null) {
			return scope+"/"+packageName+"@"+versionExpression;
		} else {
			return packageName+"@"+versionExpression;
		}
	}
	
	public CDNRequest forceDownload() {
		return forceDownload(true);
	}
	
	public CDNRequest forceDownload(boolean forceDownload) {
		this.forceDownload = forceDownload;
		return this;
	}
	
	public VersionInfo resolveVersion(Map<String, VersionInfo> versionsCache) {
		VersionInfo version =  versionsCache.computeIfAbsent(getPackageVersionExpression(),
				expression -> {
					VersionInfo ret = JNPMService.instance().bestMatch(expression);
					return ret==null?NULL_VERSION:ret;
				});
		if(version!=null && version != NULL_VERSION) {
			boolean useCache = forceDownload!=null?!forceDownload:JNPMService.instance().getSettings().isUseCache();
			version.downloadTarball(useCache).blockingAwait();
			return version;
		} else return null;
	}
	
	public static CDNRequest valueOf(String packageInfo, String filePath) {
		String scope = null;
		if(packageInfo.startsWith("@")) {
			int indx = packageInfo.indexOf('/');
			if(indx>0) {
				scope = packageInfo.substring(0, indx);
				packageInfo = packageInfo.substring(indx+1);
			} else {
				throw new IllegalArgumentException("Scoped Package Info '"+packageInfo+"' doesn't have package name");
			}
		}
		return valueOf(scope, packageInfo, filePath);
	}
	
	public static CDNRequest valueOf(String scope, String packageInfo, String filePath) {
		int indx = packageInfo.indexOf('@');
		String pck = indx<0?packageInfo:packageInfo.substring(0, indx);
		String version = indx>=0?packageInfo.substring(indx+1):null;
		return new CDNRequest(scope, pck, version, filePath);
	}
	
	public static CDNRequest valueOf(String fullPath) {
		Matcher matcher = PATH_REGEXP.matcher(fullPath);
		if(matcher.matches()) {
			return new CDNRequest(matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5));
		} else {
			throw new IllegalArgumentException("Path '"+fullPath+"' should corresponds pattern '"+PATH_PATTERN+"'");
		}
	}
	
	public boolean shouldRedirectForVersion() {
		return !isExactVersion();
	}
	
	public boolean shouldRedirectForPath() {
		return path == null || path.isEmpty();
	}
	
	public boolean shouldRedirect() {
		return shouldRedirectForVersion() || shouldRedirectForPath();
	}
	
	public String buildRedirectUrl(VersionInfo resolvedVersion) {
		StringBuilder sb = new StringBuilder();
		
		if(scope != null) {
			sb.append('/').append(scope);
		}
		sb.append('/').append(packageName).append('@').append(resolvedVersion.getVersion());
		
		String redirectPath = path;
		if(redirectPath == null || redirectPath.isEmpty()) {
			redirectPath = getDefaultPath(resolvedVersion);
		}
		
		if(redirectPath != null && !redirectPath.isEmpty()) {
			sb.append('/').append(redirectPath);
		}
		
		return sb.toString();
	}
	
	public String getDefaultPath(VersionInfo versionInfo) {
		if(versionInfo.getUnpkg() != null && !versionInfo.getUnpkg().isEmpty()) {
			return versionInfo.getUnpkg();
		}
		if(versionInfo.getJsdelivr() != null && !versionInfo.getJsdelivr().isEmpty()) {
			return versionInfo.getJsdelivr();
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "CDNRequest for "+getPackageVersionExpression()+":"+getPath();
	}
}
