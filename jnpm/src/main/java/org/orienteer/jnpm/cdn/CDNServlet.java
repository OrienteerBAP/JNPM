package org.orienteer.jnpm.cdn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.orienteer.jnpm.dm.VersionInfo;

public class CDNServlet extends HttpServlet implements CDNResolver {
	
	private Map<String, VersionInfo> versionsCache = new HashMap<String, VersionInfo>();
	private MimetypesFileTypeMap mimeTypeMap = new MimetypesFileTypeMap();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		req.getPathInfo().
		try {
			CDNRequest request = CDNRequest.valueOf(req.getPathInfo());
			resp.setContentType(mimeTypeMap.getContentType(request.getFileName()));
			resolveRequest(request, resp.getOutputStream());
			resp.addHeader("Cache-Control", "public, max-age=604800, immutable");
		} catch (IllegalArgumentException | FileNotFoundException e) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.getWriter().write(e.getMessage());
		}
	}

	@Override
	public Map<String, VersionInfo> getVersionsCache() {
		return versionsCache;
	}
}
