package org.orienteer.jnpm;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NPMRegistryService {

	@GET("/")
	public Single<RegistryInfo> getRegistryInfo();
	
	@GET("/{package}")
	public Single<PackageInfo> getPackageInfo(@Path("package") String packageName);
	

	@GET("/{package}/{version}")
	public Single<VersionInfo> getVersionInfo(@Path("package") String packageName, @Path("version") String version);

	@GET("/-/v1/search")
	public Single<SearchResults> search(@Query("text") String text,
										@Query("size") Integer size,
										@Query("from") Integer from,
										@Query("quality") Float quality,
										@Query("popularity") Float popularity,
										@Query("maintenance") Float maintenance);
	
	public default Single<SearchResults> search(String text, Integer size, Integer from) {
		return search(text, size, from, null, null, null);
	}
	
	public default Single<SearchResults> search(String text, Integer size) {
		return search(text, size, null);
	}
	
	public default Single<SearchResults> search(String text) {
		return search(text, null);
	}

}
