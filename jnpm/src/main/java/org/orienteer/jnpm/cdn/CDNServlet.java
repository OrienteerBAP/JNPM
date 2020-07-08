package org.orienteer.jnpm.cdn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.JNPMUtils;
import org.orienteer.jnpm.dm.VersionInfo;

public class CDNServlet extends HttpServlet implements CDNResolver {
	
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
//		req.getPathInfo().
		try {
			CDNRequest request = CDNRequest.valueOf(req.getPathInfo());
			resp.setContentType(JNPMUtils.fileNameToMimeType(request.getFileName()));
			resolveRequest(request, resp.getOutputStream());
			resp.addHeader("Cache-Control", "public, max-age=604800, immutable");
		} catch (IllegalArgumentException | FileNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource was not found for provided path '"+req.getPathInfo()+"'");
		}
	}

	@Override
	public Map<String, VersionInfo> getVersionsCache() {
		return versionsCache;
	}
}
