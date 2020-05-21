package org.orienteer.jnpm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraversalContext;
import org.orienteer.jnpm.traversal.TraversalTree;
import org.orienteer.jnpm.traversal.TraverseDirection;
import org.slf4j.Logger;

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
   
	
	public default Observable<TraversalTree> traverse(VersionInfo rootVersion, TraverseDirection direction, boolean doForThis, ITraversalRule rule) {
		TraversalContext ctx = new TraversalContext(rootVersion, direction);
		return traverse(ctx, null, rootVersion, doForThis, rule);
	}
    
	public default Observable<TraversalTree> traverse(TraversalContext ctx, TraversalTree parentTree, VersionInfo version, boolean doForThis, ITraversalRule rule) {
		return Observable.defer(() -> {
			Logger log = ctx.getLogger();
//			log.info("Package: "+version.getName()+"@"+version.getVersionAsString());
			
			final TraversalTree thisTree = parentTree!=null?parentTree.subTreeFor(version):ctx.getRootTree(); 
			List<Observable<TraversalTree>>  setToDo = new ArrayList<>();
			if(doForThis) setToDo.add(Observable.just(thisTree).doOnNext(TraversalTree::commit));
			
			if(!thisTree.isDuplicate()) {
				Map<String, String> toDownload = rule.getNextDependencies(version);
				
				if(!toDownload.isEmpty()) {
					Observable<VersionInfo> cachedDependencies = Observable.fromIterable(toDownload.entrySet())
												.flatMapMaybe(e-> JNPMService.instance().getRxService()
																	.bestMatch(e.getKey(), e.getValue()))
												.doOnError(e -> log.error("Error during handing "+version.getName()+"@"+version.getVersionAsString()+" ToDownload: "+toDownload, e))
												.cache();
					switch (ctx.getDirection()) {
						case WIDER:
							// Go wider first
							setToDo.add(cachedDependencies
									.map(v ->  thisTree.subTreeFor(v).commit()));
							// Go to dependencies
							setToDo.add(cachedDependencies.flatMap(v -> traverse(ctx, thisTree, v, false, ITraversalRule.DEPENDENCIES)));
							
							break;
						case DEEPER:
							// Go to dependencies right away
							setToDo.add(cachedDependencies.flatMap(v -> traverse(ctx, thisTree, v, true, ITraversalRule.DEPENDENCIES)));
							break;
					}
				}
			}
			return Observable.concat(setToDo);
		});
	}

}
