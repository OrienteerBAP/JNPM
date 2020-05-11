package org.orienteer.jnpm;

import org.orienteer.jnpm.dm.RegistryInfo;

import io.reactivex.Single;
import retrofit2.http.GET;

public interface NPMRegistryService {

	@GET("/")
	public Single<RegistryInfo> getRegistryInfo();
}
