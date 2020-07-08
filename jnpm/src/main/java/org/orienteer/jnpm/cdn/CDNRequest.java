package org.orienteer.jnpm.cdn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.zafarkhaja.semver.Version;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
public class CDNRequest {
	
	private static final String PATH_PATTERN = "/([^/@]*)@?([^/]*)/(.*)";
	private static final Pattern PATH_REGEXP = Pattern.compile(PATH_PATTERN);

	private String packageName;
	private String versionExpression;
	private String path;
	private String fileName;
	
	@NonFinal
	private Boolean exactVersion;
	
	public CDNRequest(String packageName, String version, String path) {
		this.packageName = packageName;
		this.versionExpression = version!=null && !version.isEmpty()?version:"latest";
		int indx = path.indexOf("?");
		this.path = indx<0?path:path.substring(0, indx);
		indx = path.lastIndexOf("/");
		fileName = indx<0?path:path.substring(indx+1);
	}
	
	public boolean isExactVersion() {
		if(exactVersion==null) {
			exactVersion = Version.isValid(versionExpression);
		}
		return exactVersion;
	}
	
	public String getPackageVersionExpression() {
		return packageName+"@"+versionExpression;
	}
	
	public static CDNRequest valueOf(String packageInfo, String filePath) {
		int indx = packageInfo.indexOf("@");
		String pck = indx<0?packageInfo:packageInfo.substring(0, indx);
		String version = indx<0?packageInfo.substring(indx+1):null;
		return new CDNRequest(pck, version, filePath);
	}
	
	public static CDNRequest valueOf(String fullPath) {
		Matcher matcher = PATH_REGEXP.matcher(fullPath);
		if(matcher.matches()) {
			return new CDNRequest(matcher.group(1), matcher.group(2), matcher.group(3));
		} else {
			throw new IllegalArgumentException("Path '"+fullPath+"' should corresponds pattern '"+PATH_PATTERN+"'");
		}
	}
}
