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
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
public class JNPMService 
{
	private static JNPMService INSTANCE;
	
	private JNPMSettings settings;
	private RxJNPMService rxService;
	
	private JNPMService(JNPMSettings settings) {
		this.settings = settings;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl(settings.getRegistryUrl())
			    .addConverterFactory(JacksonConverterFactory.create(mapper))
			    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
			    .build();
		rxService = retrofit.create(RxJNPMService.class);
	}
	
	public static JNPMService instance() {
		if(INSTANCE==null) throw new IllegalStateException("Configure JNPM instance first by calling JNPM.configure(settings)");
		return INSTANCE;
	}
	
	/**
	 * Method for testing purposes - to be able to substitute mocked objects
	 * @param substitution {@link JNPMService} to substitute
	 * @return previous {@link JNPMService}
	 */
	static JNPMService instance(JNPMService substitution) {
		JNPMService preserved = INSTANCE;
		INSTANCE = substitution;
		return preserved;
	}
	
	public static synchronized JNPMService configure(JNPMSettings settings) {
		if(INSTANCE!=null) throw new IllegalStateException("You can't configure JNPM twise: it's already initiated");
		try {
			settings.createAllDirectories();
			log.info("Settings: "+settings);
			INSTANCE = new JNPMService(settings);
			return INSTANCE;
		} catch (Exception e) {
			log.error("Can't configure JNPM due to problems with settings", e);
			return null;
		}
	}
	
	public JNPMSettings getSettings() {
		return settings;
	}
	
	public RxJNPMService getRxService() {
		return rxService;
	}
	
	public PackageInfo getPackageInfo(String packageName) {
    	return rxService.getPackageInfo(packageName).blockingGet();
    }
	
    public VersionInfo getVersionInfo(String packageName, String version) {
    	return rxService.getVersionInfo(packageName, version).blockingGet();
    }
    
    public List<VersionInfo> retrieveVersions(String packageName, String versionConstraint) {
    	return rxService.retrieveVersions(packageName, versionConstraint)
    			.sorted().toList().blockingGet();
    }
    
    public List<VersionInfo> retrieveVersions(String expression) {
    	return rxService.retrieveVersions(expression)
    			.sorted().toList().blockingGet();
    }
    
    public VersionInfo bestMatch(String packageName, String versionConstraint) {
    	return rxService.bestMatch(packageName, versionConstraint).blockingGet();
    }
    
    public VersionInfo bestMatch(String expression) {
    	return rxService.bestMatch(expression).blockingGet();
    }
}
