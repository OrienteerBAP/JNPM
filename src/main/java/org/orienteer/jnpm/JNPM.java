package org.orienteer.jnpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.VersionInfo;

import com.github.zafarkhaja.semver.Version;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class JNPM 
{
	private static JNPM INSTANCE;
	
	private JNPMSettings settings;
	private NPMRegistryService registryService;
	
	private JNPM(JNPMSettings settings) {
		this.settings = settings;
		Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl(settings.getRegistryUrl())
			    .addConverterFactory(JacksonConverterFactory.create())
			    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
			    .build();
		registryService = retrofit.create(NPMRegistryService.class);
	}
	
	public static JNPM instance() {
		if(INSTANCE==null) throw new IllegalStateException("Configure JNPM instance first by calling JNPM.configure(settings)");
		return INSTANCE;
	}
	
	public static JNPM configure(JNPMSettings settings) {
		INSTANCE = new JNPM(settings);
		return INSTANCE;
	}
	
	public JNPMSettings getSettings() {
		return settings;
	}
	
	NPMRegistryService getNpmRegistryService() {
		return registryService;
	}
	
	public PackageInfo retrievePackageInfo(String packageName) {
    	return registryService.getPackageInfo(packageName).blockingGet();
    }
	
    public VersionInfo retrieveVersion(String packageName, String version) {
    	return registryService.getVersionInfo(packageName, version).blockingGet();
    }
    
    public List<VersionInfo> retrieveVersions(String packageName, String versionConstraint) {
    	final Predicate<Version> res=JNPMUtils.toPredicate(versionConstraint);
        if(res!=null) {
        	return registryService.getPackageInfo(packageName)
    						.flatMapObservable(p -> Observable.fromIterable(p.getVersions().values()))
    						.filter(v -> v.satisfies(res))
    						.sorted().toList().blockingGet();
        } else {
        	//It's probably a tag
        	PackageInfo packageInfo = registryService.getPackageInfo(packageName).blockingGet();
        	String version = packageInfo.getDistTags().get(versionConstraint);
        	VersionInfo versionInfo = version!=null?packageInfo.getVersions().get(version):null;
        	return versionInfo!=null?Arrays.asList(versionInfo):new ArrayList<VersionInfo>();
        }
    }
    
    public List<VersionInfo> retrieveVersions(String expression) {
    	int indx = expression.lastIndexOf('@');
    	if(indx>0) {
    		return retrieveVersions(expression.substring(0, indx), expression.substring(indx+1));
    	} else {
    		return retrieveVersions(expression, null);
    	}
    }
}
