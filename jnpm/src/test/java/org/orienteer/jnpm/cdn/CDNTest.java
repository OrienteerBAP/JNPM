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
}
