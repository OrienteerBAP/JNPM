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
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.handler.RedirectRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.UrlRenderer;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Time;
import org.orienteer.jnpm.ILogger;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Wicket {@link IResource} to serve resources from NPM package. Format of the request: /cdn/&lt;package&gt;/&lt;file path&gt;
 * For example:
 * <ul>
 * 	<li>/cdn/bootstrap/dist/css/bootstrap.min.css</li>
 *  <li>/cdn/vue@~2.6.11/dist/vue.js</li>
 * </ul>
 */
@Slf4j
public class CDNWicketResource extends AbstractResource {
	
	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_MOUNT = "/cdn/";
	public static final String RESOURCE_KEY = CDNWicketResource.class.getSimpleName();
	public static final ResourceReference SHARED_RESOURCE = new SharedResourceReference(RESOURCE_KEY);
	
	//For cache purposes: allow to get from browser cache unless server restarted
	private static final Time INIT_TIME = Time.now();
	public static final ILogger LOGGER = new ILogger() {
		
		@Override
		public void log(String message, Throwable exc) {
			log.error(message, exc);
		}
		
		@Override
		public void log(String message) {
			log.info(message);
		}
	};

	private Map<String, VersionInfo> versionsCache = new HashMap<String, VersionInfo>();
	
	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes) {
		final ResourceResponse response = new ResourceResponse();
        response.setLastModified(INIT_TIME);
        if (response.dataNeedsToBeWritten(attributes)) {
            PageParameters params = attributes.getParameters();
            String pathInfo = getPathInfo(params);
            CDNRequest cdnRequest = CDNRequest.valueOf(pathInfo);
            StringValue forceSV = params.get("force");
            if(!forceSV.isEmpty()) cdnRequest.forceDownload(forceSV.toBoolean(false));
            
            VersionInfo versionInfo = cdnRequest.resolveVersion(versionsCache);
            if(versionInfo == null) {
            	response.setError(HttpServletResponse.SC_NOT_FOUND, "Package was not found for " + cdnRequest.getPackageVersionExpression());
            	return response;
            }
            
            if(cdnRequest.shouldRedirect()) {
            	String baseRedirectUrl = cdnRequest.buildRedirectUrl(versionInfo);
            	if(baseRedirectUrl != null) {
            		Url fullRedirectUrl = buildRedirectUrl(baseRedirectUrl, params);
            		UrlRenderer urlRenderer = RequestCycle.get().getUrlRenderer();
            		String renderedRedirectUrl = urlRenderer.renderUrl(fullRedirectUrl);
            		RequestCycle.get().scheduleRequestHandlerAfterCurrent(new RedirectRequestHandler(renderedRedirectUrl));
            		response.setError(HttpServletResponse.SC_FOUND);
            		return response;
            	}
            }
            
            if(cdnRequest.getPath() == null || cdnRequest.getPath().isEmpty()) {
            	response.setError(HttpServletResponse.SC_NOT_FOUND, "No path specified and no default path available for " + cdnRequest.getPackageVersionExpression());
            	return response;
            }
            
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
	
	protected Url buildRedirectUrl(String baseRedirectUrl, PageParameters params) {
		Url redirectUrl = Url.parse(baseRedirectUrl);
		for(String namedKey : params.getNamedKeys()) {
			java.util.List<StringValue> values = params.getValues(namedKey);
			for(StringValue value : values) {
				redirectUrl.addQueryParameter(namedKey, value.toString());
			}
		}
		return redirectUrl;
	}

	public static void mount(WebApplication app) {
		mount(app, DEFAULT_MOUNT, null);
	}
	
	public static void mount(WebApplication app, String path, JNPMSettings settings) {
		app.getSharedResources().add(RESOURCE_KEY, new CDNWicketResource());
		app.mountResource(path, SHARED_RESOURCE);
		if(!JNPMService.isConfigured()) {
			JNPMSettings.JNPMSettingsBuilder builder = settings!=null?settings.toBuilder():JNPMSettings.builder();
			if(settings==null || settings.getLogger().equals(ILogger.DEFAULT)) builder.logger(LOGGER);
			JNPMService.configure(builder.build());
		}
	}
	
	public static void unmount(WebApplication app) {
		unmount(app, DEFAULT_MOUNT);
	}
	
	public static void unmount(WebApplication app, String path) {
		app.unmount(path);
	}

}
