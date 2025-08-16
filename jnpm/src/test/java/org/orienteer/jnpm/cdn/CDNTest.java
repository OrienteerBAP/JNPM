package org.orienteer.jnpm.cdn;

import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

import static org.mockito.AdditionalAnswers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.dm.VersionInfo;

public class CDNTest {
	
	static {
		if(!JNPMService.isConfigured())
			JNPMService.configure(JNPMSettings.builder()
						.homeDirectory(Paths.get("target", ".jnpm"))
 						.build());
	}
	
	@Test
	public void testParsing() {
		CDNRequest reqInfo = CDNRequest.valueOf("/vue@2.6.11/path/to/the/file.js");
		assertEquals("vue", reqInfo.getPackageName());
		assertEquals("2.6.11", reqInfo.getVersionExpression());
		assertEquals("vue@2.6.11", reqInfo.getPackageVersionExpression());
		assertEquals("path/to/the/file.js", reqInfo.getPath());
		assertEquals("file.js", reqInfo.getFileName());
		assertTrue(reqInfo.isExactVersion());
		
		reqInfo = CDNRequest.valueOf("/@test/vue@2.6.11/path/to/the/file.js");
		assertEquals("@test", reqInfo.getScope());
		assertEquals("vue", reqInfo.getPackageName());
		assertEquals("2.6.11", reqInfo.getVersionExpression());
		assertEquals("@test/vue@2.6.11", reqInfo.getPackageVersionExpression());
		assertEquals("path/to/the/file.js", reqInfo.getPath());
		assertEquals("file.js", reqInfo.getFileName());
		assertTrue(reqInfo.isExactVersion());
		
		reqInfo = CDNRequest.valueOf("/vue@~2.6.11/path/to/the/file2.js");
		assertEquals("vue", reqInfo.getPackageName());
		assertEquals("~2.6.11", reqInfo.getVersionExpression());
		assertEquals("vue@~2.6.11", reqInfo.getPackageVersionExpression());
		assertEquals("path/to/the/file2.js", reqInfo.getPath());
		assertEquals("file2.js", reqInfo.getFileName());
		assertTrue(!reqInfo.isExactVersion());
		
		reqInfo = CDNRequest.valueOf("/vue/path/to/the/file3.js");
		assertEquals("vue", reqInfo.getPackageName());
		assertEquals("latest", reqInfo.getVersionExpression());
		assertEquals("vue@latest", reqInfo.getPackageVersionExpression());
		assertEquals("path/to/the/file3.js", reqInfo.getPath());
		assertEquals("file3.js", reqInfo.getFileName());
		assertTrue(!reqInfo.isExactVersion());
	}
	
	@Test
	public void testServlet() throws Exception{
		HttpServletRequest request = mock(HttpServletRequest.class);       
        HttpServletResponse response = mock(HttpServletResponse.class);    

        when(request.getPathInfo()).thenReturn("/vue@2.6.11/dist/vue.js");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
			
			@Override
			public void write(int b) throws IOException {
				baos.write(b);
			}
			
			@Override
			public void setWriteListener(WriteListener writeListener) {
			}
			
			@Override
			public boolean isReady() {
				return true;
			}
		};
        when(response.getOutputStream()).thenReturn(sos);

        new CDNServlet().doGet(request, response);

        baos.flush(); // it may not have been flushed yet...
        String content = new String(baos.toByteArray());
//        System.out.println("Content: "+content);
        assertTrue(content.contains("Vue.js v2.6.11"));
	}
	
	@Test
	public void testRedirectDetection() {
		// Test redirect for non-exact version
		CDNRequest reqInfo = CDNRequest.valueOf("/vue@~2.6.11/path/to/the/file.js");
		assertTrue("Should redirect for version range", reqInfo.shouldRedirectForVersion());
		assertTrue("Should redirect overall", reqInfo.shouldRedirect());
		
		// Test redirect for missing path
		reqInfo = CDNRequest.valueOf("/vue@2.6.11");
		assertTrue("Should redirect for missing path", reqInfo.shouldRedirectForPath());
		assertTrue("Should redirect overall", reqInfo.shouldRedirect());
		
		// Test no redirect for exact version with path
		reqInfo = CDNRequest.valueOf("/vue@2.6.11/dist/vue.js");
		assertTrue("Should not redirect for exact version", !reqInfo.shouldRedirectForVersion());
		assertTrue("Should not redirect for provided path", !reqInfo.shouldRedirectForPath());
		assertTrue("Should not redirect overall", !reqInfo.shouldRedirect());
		
		// Test redirect for latest version (no version specified)
		reqInfo = CDNRequest.valueOf("/vue/dist/vue.js");
		assertTrue("Should redirect for latest version", reqInfo.shouldRedirectForVersion());
		assertTrue("Should redirect overall", reqInfo.shouldRedirect());
	}
	
	@Test
	public void testRedirectUrlBuilding() {
		CDNRequest reqInfo = CDNRequest.valueOf("/vue@~2.6.11/dist/vue.js");
		
		// Create mock VersionInfo
		VersionInfo mockVersion = new VersionInfo();
		mockVersion.setVersionAsString("2.6.14");
		
		String redirectUrl = reqInfo.buildRedirectUrl(mockVersion);
		assertEquals("/vue@2.6.14/dist/vue.js", redirectUrl);
		
		// Test with scoped package
		reqInfo = CDNRequest.valueOf("/@vue/cli@~4.0.0/bin/vue.js");
		mockVersion.setVersionAsString("4.5.15");
		
		redirectUrl = reqInfo.buildRedirectUrl(mockVersion);
		assertEquals("/@vue/cli@4.5.15/bin/vue.js", redirectUrl);
	}
	
	@Test
	public void testDefaultPathResolution() {
		CDNRequest reqInfo = CDNRequest.valueOf("/vue@2.6.11");
		
		// Test with unpkg field
		VersionInfo mockVersion = new VersionInfo();
		mockVersion.setVersionAsString("2.6.11");
		mockVersion.setUnpkg("dist/vue.js");
		
		assertEquals("dist/vue.js", reqInfo.getDefaultPath(mockVersion));
		
		String redirectUrl = reqInfo.buildRedirectUrl(mockVersion);
		assertEquals("/vue@2.6.11/dist/vue.js", redirectUrl);
		
		// Test with jsdelivr field when unpkg is not available
		mockVersion.setUnpkg(null);
		mockVersion.setJsdelivr("dist/vue.min.js");
		
		assertEquals("dist/vue.min.js", reqInfo.getDefaultPath(mockVersion));
		
		redirectUrl = reqInfo.buildRedirectUrl(mockVersion);
		assertEquals("/vue@2.6.11/dist/vue.min.js", redirectUrl);
		
		// Test with no default path
		mockVersion.setUnpkg(null);
		mockVersion.setJsdelivr(null);
		
		assertEquals(null, reqInfo.getDefaultPath(mockVersion));
	}
	
	@Test
	public void testServletRedirect() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);       
        HttpServletResponse response = mock(HttpServletResponse.class);    

        // Test redirect for version range
        when(request.getPathInfo()).thenReturn("/vue@~2.6.0/dist/vue.js");
        when(request.getQueryString()).thenReturn(null);

        new CDNServlet().doGet(request, response);

        // Verify that sendRedirect was called (we can't easily test the exact URL without more complex mocking)
        verify(response, times(1)).sendRedirect(anyString());
	}
}
