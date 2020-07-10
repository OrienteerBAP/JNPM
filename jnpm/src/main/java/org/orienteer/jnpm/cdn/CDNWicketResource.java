package org.orienteer.jnpm.cdn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.util.time.Time;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

/**
 * Wicket {@link IResource} to serve resources from NPM package. Format of the request: /cdn/&lt;package&gt;/&lt;file path&gt;
 * For example:
 * <ul>
 * 	<li>/cdn/bootstrap/dist/css/bootstrap.min.css</li>
 *  <li>/cdn/vue@~2.6.11/dist/vue.js</li>
 * </ul>
 */
public class CDNWicketResource extends AbstractResource {
	
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
            VersionInfo versionInfo = cdnRequest.resolveVersion(versionsCache);
            if(versionInfo!=null) {
            	response.setContentType(JNPMUtils.fileNameToMimeType(cdnRequest.getFileName()));
            	response.setCacheDurationToMaximum();
            	response.setWriteCallback(new WriteCallback() {
					
					@Override
					public void writeData(Attributes attributes) throws IOException {
						try {
							JNPMUtils.readTarball(versionInfo.getLocalTarball(), 
													"/package/"+cdnRequest.getPath(), 
													attributes.getResponse().getOutputStream());
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
        for(int i=0; i<segments;i++) {
        	sb.append('/').append(params.get(i));
        }
        return sb.toString();
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
