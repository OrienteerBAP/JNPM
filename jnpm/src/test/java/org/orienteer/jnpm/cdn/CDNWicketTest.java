package org.orienteer.jnpm.cdn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.protocol.http.mock.MockHttpServletResponse;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

public class CDNWicketTest {
	
	private WicketTester tester;
	
	@Before
	public void setup() {
		if(tester==null) {
			tester = new WicketTester(new CDNTestApplication());
		}
	}
	
	@Test
	public void testWicketResource() {
//		tester.executeUrl("/wicketcdn/vue@2.6.11/dist/vue.js");
		tester.executeUrl("./wicketcdn/vue@2.6.11/package.json");
		MockHttpServletResponse response = tester.getLastResponse();
        assertEquals(response.getStatus(), HttpServletResponse.SC_OK);
        assertTrue(response.getDocument().contains("Reactive, component-oriented view layer for modern web interfaces."));
        tester.executeUrl("./wicketcdn/vue@2.6.11/nosuchfile.json");
		response = tester.getLastResponse();
        assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);
        tester.executeUrl("./wicketcdn/nosuchpackageatall/package.json");
		response = tester.getLastResponse();
        assertEquals(response.getStatus(), HttpServletResponse.SC_NOT_FOUND);
	}
}
