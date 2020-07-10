package org.orienteer.jnpm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Query;

/**
 * Set of synchronous API to access NPM. Main entry for any code which use JNPM
 */
@Slf4j
public class JNPMService 
{
	private static JNPMService instance;
	
	private JNPMSettings settings;
	private RxJNPMService rxService;
	
	private JNPMService(JNPMSettings settings) {
		this.settings = settings;
		OkHttpClient client = new OkHttpClient.Builder()
	            .connectTimeout(30, TimeUnit.SECONDS)
	            .readTimeout(2,TimeUnit.MINUTES)
	            .writeTimeout(2,  TimeUnit.MINUTES)
//	            .cache(cache)
	            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
	            .protocols(Arrays.asList(Protocol.HTTP_1_1))
	            .build();
		
	    
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl(settings.getRegistryUrl())
			    .client(client)
			    .addConverterFactory(JacksonConverterFactory.create(mapper))
			    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
			    .build();
		rxService = retrofit.create(RxJNPMService.class);
	}
	
	public static JNPMService instance() {
		if(!isConfigured()) throw new IllegalStateException("Configure JNPM instance first by calling JNPM.configure(settings)");
		return instance;
	}
	
	/**
	 * Method for testing purposes - to be able to substitute mocked objects
	 * @param substitution {@link JNPMService} to substitute
	 * @return previous {@link JNPMService}
	 */
	static JNPMService instance(JNPMService substitution) {
		JNPMService preserved = instance;
		instance = substitution;
		return preserved;
	}
	
	public static boolean isConfigured() {
		return instance!=null;
	}
	
	public static synchronized JNPMService configure(JNPMSettings settings) {
		if(isConfigured()) throw new IllegalStateException("You can't configure JNPM twise: it's already initiated");
		try {
			settings.createAllDirectories();
			log.info("Settings: "+settings);
			instance = new JNPMService(settings);
			return instance;
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
	
	public RegistryInfo getRegistryInfo() {
		return rxService.getRegistryInfo().blockingGet();
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
    
    public SearchResults search(String text, Integer size, Integer from,
									Float quality, Float popularity, Float maintenance) {
    	return rxService.search(text, size, from, quality, popularity, maintenance).blockingGet();
    }
    
    public  SearchResults search(String text, Integer size, Integer from) {
		return search(text, size, from, null, null, null);
	}
	
	public SearchResults search(String text, Integer size) {
		return search(text, size, null);
	}
	
	public SearchResults search(String text) {
		return search(text, null);
	}
}
