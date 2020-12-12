package org.orienteer.jnpm;

import java.util.ArrayList;
import java.util.List;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;
import org.orienteer.jnpm.traversal.AbstractTraversalNode;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraversalContext;
import org.orienteer.jnpm.traversal.TraversalTree;
import org.orienteer.jnpm.traversal.TraverseDirection;

import com.vdurmont.semver4j.Requirement;

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

/**
 * Set of asynchronous API to access NPM.
 */
@SuppressWarnings("checkstyle:typename")
public interface RxJNPMService {

	@GET("/")
	public Single<RegistryInfo> getRegistryInfo();
	
	@GET("/{package}")
	public Maybe<PackageInfo> getPackageInfo(@Path("package") String packageName);
	

	@GET("/{package}/{version}")
	public Maybe<VersionInfo> getVersionInfo(@Path("package") String packageName, @Path("version") String version);

	@GET("/-/v1/search")
	public Single<SearchResults> search(@Query("text") String text,
										@Query("size") Integer size,
										@Query("from") Integer from,
										@Query("quality") Float quality,
										@Query("popularity") Float popularity,
										@Query("maintenance") Float maintenance);
	
	@Streaming
    @GET
    public Maybe<Response<ResponseBody>> downloadFile(@Url String fileUrl);
	
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
    	final Requirement res = JNPMUtils.toVersionPredicate(versionConstraint);
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
    		return retrieveVersions(expression, "latest");
    	}
    }
	
	public default Maybe<VersionInfo> bestMatch(String packageName, String versionConstraint) {
		return retrieveVersions(packageName, versionConstraint).sorted().lastElement();
    }
    
    public default Maybe<VersionInfo> bestMatch(String expression) {
    	return retrieveVersions(expression).sorted().lastElement();
    }
    
    public default Observable<TraversalTree> traverse(TraverseDirection direction, ITraversalRule rule, String... specifications) {
    	List<VersionInfo> roots = Observable.fromArray(specifications).flatMapMaybe(s-> bestMatch(s)).toList().blockingGet();
    	return traverse(direction, rule, roots.toArray(new VersionInfo[roots.size()]));
    }
   
	
	public default Observable<TraversalTree> traverse(TraverseDirection direction, ITraversalRule rule, VersionInfo... roots) {
		TraversalContext ctx = new TraversalContext(direction, rule, roots);
		return traverse(ctx, true);
	}
    
	public default Observable<TraversalTree> traverse(AbstractTraversalNode node, boolean doForThis) {
		TraversalContext ctx = node.getContext();
		return Observable.defer(() -> {
			
			List<Observable<TraversalTree>>  setToDo = new ArrayList<>();
			if(doForThis && node instanceof TraversalTree) 
				setToDo.add(Observable.just((TraversalTree)node).doOnNext(TraversalTree::commit));
			
			if(node.isTraversableDeeper()) {
				
					Observable<TraversalTree> cachedDependencies = node.getNextTraversalNodes()
												.cache();
					switch (ctx.getDirection()) {
						case WIDER:
							// Go wider first
							setToDo.add(cachedDependencies
									.doOnNext(t ->  t.commit()));
							// Go to dependencies
							setToDo.add(cachedDependencies.flatMap(t -> traverse(t, false)));
							
							break;
						case DEEPER:
							// Go to dependencies right away
							setToDo.add(cachedDependencies.flatMap(t -> traverse(t, true)));
							break;
					}
			}
			return Observable.concat(setToDo);
		});
	}

}
