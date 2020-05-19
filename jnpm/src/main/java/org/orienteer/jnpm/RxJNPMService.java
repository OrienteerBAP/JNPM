package org.orienteer.jnpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;

import com.github.zafarkhaja.semver.Version;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface RxJNPMService {

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
	
	@Streaming
    @GET
    public Single<Response<ResponseBody>> downloadFile(@Url String fileUrl);
	
	public default Single<SearchResults> search(String text, Integer size, Integer from) {
		return search(text, size, from, null, null, null);
	}
	
	public default Single<SearchResults> search(String text, Integer size) {
		return search(text, size, null);
	}
	
	public default Single<SearchResults> search(String text) {
		return search(text, null);
	}
	
	public default Observable<VersionInfo> retrieveVersions(String packageName, String versionConstraint) {
    	final Predicate<Version> res=JNPMUtils.toPredicate(versionConstraint);
        if(res!=null) {
        	return getPackageInfo(packageName)
    						.flatMapObservable(p -> Observable.fromIterable(p.getVersions().values()))
    						.filter(v -> v.satisfies(res));
        } else {
        	//It's probably a tag
        	return getPackageInfo(packageName).flatMapObservable((p) -> {
        		String version = p.getDistTags().get(versionConstraint);
        		VersionInfo versionInfo = version!=null?p.getVersions().get(version):null;
        		return versionInfo!=null?Observable.just(versionInfo):Observable.empty();
        	});
        }
    }
	
	public default Observable<VersionInfo> retrieveVersions(String expression) {
    	int indx = expression.lastIndexOf('@');
    	if(indx>0) {
    		return retrieveVersions(expression.substring(0, indx), expression.substring(indx+1));
    	} else {
    		return retrieveVersions(expression, null);
    	}
    }
	
	public default Maybe<VersionInfo> bestMatch(String packageName, String versionConstraint) {
		return retrieveVersions(packageName, versionConstraint).sorted().lastElement();
    }
    
    public default Maybe<VersionInfo> bestMatch(String expression) {
    	return retrieveVersions(expression).sorted().lastElement();
    }

}
