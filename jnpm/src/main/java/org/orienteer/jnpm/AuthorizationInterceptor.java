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
	private boolean basicAuth;

	// https://github.com/OrienteerBAP/JNPM/commit/8fd783a32024dff44d1d56d654f8341f9ddbd7b3
	private boolean bearerAuth;

	public AuthorizationInterceptor() {
		this(JNPMService.instance().getSettings());
	}
	
	public AuthorizationInterceptor(JNPMSettings settings) {
		this(settings.getUsername(), settings.getPassword());
	}
	
	public AuthorizationInterceptor(String username, String password) {
		this.username = username;
		this.password = password;
		this.basicAuth = (username != null && !username.trim().isEmpty())
						&& (password != null && !password.trim().isEmpty());
		this.bearerAuth = (username != null && !username.trim().isEmpty())
				&& password == null;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();
		if(basicAuth) {
			request = request.newBuilder()
								.addHeader("Authorization", Credentials.basic(username, password))
								.build();
		}
		if(bearerAuth) {
			request = request.newBuilder()
					.addHeader("Authorization", "Bearer " + username)
					.build();
		}
		return chain.proceed(request);
	}

}
