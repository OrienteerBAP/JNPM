package org.orienteer.jnpm.cdn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

public interface CDNResolver {
	
	public static final VersionInfo NULL_VERSION = new VersionInfo();
	
	public Map<String, VersionInfo> getVersionsCache();
	
	public default void resolveRequest(CDNRequest request, OutputStream out) throws IOException {
		
		VersionInfo version = getVersionsCache().computeIfAbsent(request.getPackageVersionExpression(),
															expression -> {
																VersionInfo ret = JNPMService.instance().bestMatch(expression);
																if(ret==null) return NULL_VERSION;
																ret.downloadTarball().blockingAwait();
																return ret;
															});
		if(version==null || version == NULL_VERSION) throw new FileNotFoundException("Package for expression '"+request.getPackageVersionExpression()+"' was not found");
		JNPMUtils.readTarball(version.getLocalTarball(), "/package/"+request.getPath(), out);
	}
}
