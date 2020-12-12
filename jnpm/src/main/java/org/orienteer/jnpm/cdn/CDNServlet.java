package org.orienteer.jnpm.cdn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

/**
 * {@link HttpServlet} to serve resources from NPM package. Format of the request: /cdn/&lt;package&gt;/&lt;file path&gt;
 * For example:
 * <ul>
 * 	<li>/cdn/bootstrap/dist/css/bootstrap.min.css</li>
 *  <li>/cdn/vue@~2.6.11/dist/vue.js</li>
 * </ul>
 */
public class CDNServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private Map<String, VersionInfo> versionsCache = new HashMap<String, VersionInfo>();
	
	@Override
	public void init() throws ServletException {
		if(!JNPMService.isConfigured()) {
			//TODO: Enhance by other init params
			JNPMService.configure(JNPMSettings.builder().build());
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			CDNRequest request = CDNRequest.valueOf(req.getPathInfo());
			resp.setContentType(JNPMUtils.fileNameToMimeType(request.getFileName()));
			resp.addHeader("Cache-Control", "public, max-age=604800, immutable");
			JNPMUtils.readTarball(request.resolveVersion(versionsCache), 
					"/package/"+request.getPath(), 
					resp.getOutputStream());
		} catch (IllegalArgumentException | FileNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource was not found for provided path '"+req.getPathInfo()+"'");
		}
	}

}
