package org.orienteer.jnpm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.VersionInfo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;

import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
public class JNPM 
{
	private static JNPM INSTANCE;
	
	private JNPMSettings settings;
	private NPMRegistryService registryService;
	
	private JNPM(JNPMSettings settings) {
		this.settings = settings;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl(settings.getRegistryUrl())
			    .addConverterFactory(JacksonConverterFactory.create(mapper))
			    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
			    .build();
		registryService = retrofit.create(NPMRegistryService.class);
	}
	
	public static JNPM instance() {
		if(INSTANCE==null) throw new IllegalStateException("Configure JNPM instance first by calling JNPM.configure(settings)");
		return INSTANCE;
	}
	
	public static synchronized JNPM configure(JNPMSettings settings) {
		if(INSTANCE!=null) throw new IllegalStateException("You can't configure JNPM twise: it's already initiated");
		try {
			settings.createAllDirectories();
			log.info("Settings: "+settings);
			INSTANCE = new JNPM(settings);
			return INSTANCE;
		} catch (Exception e) {
			log.error("Can't configure JNPM due to problems with settings", e);
			return null;
		}
	}
	
	public JNPMSettings getSettings() {
		return settings;
	}
	
	public NPMRegistryService getNpmRegistryService() {
		return registryService;
	}
	
	public PackageInfo retrievePackageInfo(String packageName) {
    	return registryService.getPackageInfo(packageName).blockingGet();
    }
	
    public VersionInfo retrieveVersion(String packageName, String version) {
    	return registryService.getVersionInfo(packageName, version).blockingGet();
    }
    
    public List<VersionInfo> retrieveVersions(String packageName, String versionConstraint) {
    	return registryService.retrieveVersions(packageName, versionConstraint)
    			.sorted().toList().blockingGet();
    }
    
    public List<VersionInfo> retrieveVersions(String expression) {
    	return registryService.retrieveVersions(expression)
    			.sorted().toList().blockingGet();
    }
    
    public VersionInfo bestMatch(String packageName, String versionConstraint) {
    	List<VersionInfo> list = retrieveVersions(packageName, versionConstraint);
    	return list.isEmpty()?null:list.get(list.size()-1);
    }
    
    public VersionInfo bestMatch(String expression) {
    	List<VersionInfo> list = retrieveVersions(expression);
    	return list.isEmpty()?null:list.get(list.size()-1);
    }
}
