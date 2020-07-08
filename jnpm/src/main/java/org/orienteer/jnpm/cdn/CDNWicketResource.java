package org.orienteer.jnpm.cdn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.util.time.Time;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

public class CDNWicketResource extends AbstractResource implements CDNResolver {
	
	public static final String DEFAULT_MOUNT = "/cdn/";
	public static final String RESOURCE_KEY = CDNWicketResource.class.getSimpleName();
	public static final ResourceReference SHARED_RESOURCE = new SharedResourceReference(RESOURCE_KEY);
	
	//For cache purposes: allow to get from browser cache unless server restarted
	private static final Time INIT_TIME = Time.now();

	private Map<String, VersionInfo> versionsCache = new HashMap<String, VersionInfo>();
	
	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes) {
		final ResourceResponse response = new ResourceResponse();
        response.setLastModified(INIT_TIME);
        if (response.dataNeedsToBeWritten(attributes)) {
            PageParameters params = attributes.getParameters();
            String pathInfo = getPathInfo(params);
            CDNRequest cdnRequest = CDNRequest.valueOf(pathInfo);
            VersionInfo versionInfo = resolveVersion(cdnRequest);
            if(versionInfo!=null) {
            	response.setContentType(JNPMUtils.fileNameToMimeType(cdnRequest.getFileName()));
            	response.setCacheDurationToMaximum();
            	response.setWriteCallback(new WriteCallback() {
					
					@Override
					public void writeData(Attributes attributes) throws IOException {
						try {
							resolveRequest(versionInfo, cdnRequest.getPath(), attributes.getResponse().getOutputStream());
						} catch (FileNotFoundException e) {
							Response response = attributes.getResponse();
							if (response instanceof WebResponse)
							{
								WebResponse webResponse = (WebResponse) response;
								webResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource was not found for "+cdnRequest);
							}
						}
					}
				});
            } else {
            	response.setError(HttpServletResponse.SC_NOT_FOUND, "Resource was not found for "+cdnRequest);
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
