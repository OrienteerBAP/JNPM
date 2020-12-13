package org.orienteer.jnpm;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * {@link Interceptor} to authorize user if username/password were provided
 */
public class AuthorizationInterceptor implements Interceptor {
	
	private String username;
	private String password;
	private boolean byPass;
	
	public AuthorizationInterceptor() {
		this(JNPMService.instance().getSettings());
	}
	
	public AuthorizationInterceptor(JNPMSettings settings) {
		this(settings.getUsername(), settings.getPassword());
	}
	
	public AuthorizationInterceptor(String username, String password) {
		this.username = username;
		this.password = password;
		this.byPass = (username == null || username.trim().isEmpty())
						&& (password == null || password.trim().isEmpty());
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();
		if(!byPass) {
			request = request.newBuilder()
								.addHeader("Authorization", Credentials.basic(username, password))
								.build();
		}
		return chain.proceed(request);
	}

}
