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
		CDNWicketResource.mount(this, "/wicketcdn");
		mountPage("/servletPage", CDNTestServletPage.class);
		mountPage("/wicketPage", CDNTestWicketPage.class);
		getMarkupSettings().setStripWicketTags(true);
		//Service initiazation is part of CDNWicketResource, so this is not needed for Wicket
		//But if Servlet is being used - JNPMService should be configured somewhere
		if(!JNPMService.isConfigured()) 
			JNPMService.configure(JNPMSettings.builder().logger(CDNWicketResource.LOGGER).build());
	}

}
