package org.orienteer.jnpm.cdn;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class CDNTestApplication extends WebApplication {

	@Override
	public Class<? extends Page> getHomePage() {
		return CDNTestHomePage.class;
	}

}
