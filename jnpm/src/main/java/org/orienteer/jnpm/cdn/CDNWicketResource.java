package org.orienteer.jnpm.cdn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.util.time.Time;
import org.orienteer.jnpm.dm.VersionInfo;

public class CDNWicketResource extends AbstractResource implements CDNResolver {
	
	public static final String DEFAULT_MOUNT = "/cdn/${package}/${version}/";
	public static final String RESOURCE_KEY = CDNWicketResource.class.getSimpleName();
	public static final ResourceReference SHARED_RESOURCE = new SharedResourceReference(RESOURCE_KEY);
	
	//For cache purposes: allow to get from browser cache unless server restarted
	private static final Time INIT_TIME = Time.now();

	private Map<String, VersionInfo> versionsCache = new HashMap<String, VersionInfo>();
	private MimetypesFileTypeMap mimeTypeMap = new MimetypesFileTypeMap();
	
	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes) {
		final ResourceResponse response = new ResourceResponse();
        response.setLastModified(INIT_TIME);
        if (response.dataNeedsToBeWritten(attributes)) {
            PageParameters params = attributes.getParameters();
            String pckg = params.get("package").toString();
            String version = params.get("version").toString();
            String filePath = getPathInfo(params);
            CDNRequest cdnRequest = new CDNRequest(pckg, version, filePath);
            VersionInfo versionInfo = resolveVersion(cdnRequest);
            if(versionInfo!=null) {
            	response.setContentType(mimeTypeMap.getContentType(cdnRequest.getFileName()));
            	response.setCacheDurationToMaximum();
            	response.setWriteCallback(new WriteCallback() {
					
					@Override
					public void writeData(Attributes attributes) throws IOException {
						resolveRequest(versionInfo, filePath, attributes.getResponse().getOutputStream());
					}
				});
            } else {
            	response.setError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        return response;
	}
	
	protected String getPathInfo(PageParameters params) {
		int segments = params.getIndexedCount();
		if(segments==0) return null;
		StringBuilder sb = new StringBuilder();
        for(int i=0; i<params.getIndexedCount();i++) {
        	sb.append(params.get(i));
        	if(i<segments-1) sb.append('/');
        }
        return sb.toString();
	}

	@Override
	public Map<String, VersionInfo> getVersionsCache() {
		return versionsCache;
	}
	
	public static void mount(WebApplication app) {
		mount(app, DEFAULT_MOUNT);
	}
	
	public static void mount(WebApplication app, String path) {
		app.getSharedResources().add(RESOURCE_KEY, new CDNWicketResource());
		app.mountResource(path, SHARED_RESOURCE);
	}
	
	public static void unmount(WebApplication app) {
		unmount(app, DEFAULT_MOUNT);
	}
	
	public static void unmount(WebApplication app, String path) {
		app.unmount(path);
	}

}
