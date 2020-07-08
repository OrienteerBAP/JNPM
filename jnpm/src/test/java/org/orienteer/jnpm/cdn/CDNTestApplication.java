package org.orienteer.jnpm.cdn;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;

public class CDNTestApplication extends WebApplication {

	@Override
	public Class<? extends Page> getHomePage() {
		return CDNTestServletPage.class;
	}
	
	@Override
	protected void init() {
		super.init();
		if(!JNPMService.isConfigured()) 
			JNPMService.configure(JNPMSettings.builder().build());
		CDNWicketResource.mount(this, "/wicketcdn");
		mountPage("/servletPage", CDNTestServletPage.class);
		mountPage("/wicketPage", CDNTestWicketPage.class);
		getMarkupSettings().setStripWicketTags(true);
	}

}
