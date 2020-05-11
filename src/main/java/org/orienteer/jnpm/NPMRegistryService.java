package org.orienteer.jnpm;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NPMRegistryService {

	@GET("/")
	public Single<RegistryInfo> getRegistryInfo();
	
	@GET("/{package}")
	public Single<PackageInfo> getPackageInfo(@Path("package") String packageName);
}
