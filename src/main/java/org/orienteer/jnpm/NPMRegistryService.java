package org.orienteer.jnpm;

import org.orienteer.jnpm.dm.RegistryInfo;

import retrofit2.Call;
import retrofit2.http.GET;

public interface NPMRegistryService {

	@GET("/")
	public Call<RegistryInfo> getRegistryInfo();
}
