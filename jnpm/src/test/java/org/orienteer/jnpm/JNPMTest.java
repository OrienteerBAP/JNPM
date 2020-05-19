package org.orienteer.jnpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.imageio.spi.RegisterableService;

import org.junit.BeforeClass;
import org.junit.Test;
import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;

import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Unit test for simple App.
 */
@Slf4j
public class JNPMTest 
{
	static {
		JNPM.configure(JNPMSettings.builder()
						.homeDirectory(Paths.get("target", ".jnpm"))
 						.build());
	}
	
    @Test
    public void registryInfoRetrival() throws IOException {
    	Single<RegistryInfo> info = JNPM.instance().getNpmRegistryService().getRegistryInfo();
    	assertNotNull(info);
    }
    
    @Test
    public void packageInfoRetrival() throws IOException {
    	PackageInfo packageInfo = JNPM.instance().retrievePackageInfo("vue");
    	assertNotNull(packageInfo);
    	assertNotNull(packageInfo.getDistTags());
    	assertTrue(packageInfo.getDistTags().containsKey("latest"));
    }
    
    @Test
    public void versionInforetrival() throws IOException {
    	VersionInfo versionInfo = JNPM.instance().retrieveVersion("vue", "2.6.11");
    	assertNotNull(versionInfo);
    	assertNotNull(versionInfo.getVersion());
    	assertNotNull(versionInfo.getDist());
    	assertNotNull(versionInfo.getDist().getTarballName());
    	assertEquals("vue-2.6.11.tgz", versionInfo.getDist().getTarballName());
    	assertNotNull(versionInfo.getScripts());
    	assertTrue(!versionInfo.getScripts().isEmpty());
    	log.info("Details: "+versionInfo.getDetails());
    	assertNotNull(versionInfo.getDevDependencies());
    	assertTrue(!versionInfo.getDevDependencies().isEmpty());
    }
    
    @Test
    public void cornerCasesOfDeserialization() throws IOException {
    	VersionInfo versionInfo = JNPM.instance().bestMatch("semver", "^5.6.0");
    	assertNotNull(versionInfo);
    	assertNotNull(versionInfo.getVersion());
    	assertNotNull(versionInfo.getDist());
    	assertNotNull(versionInfo.getDist().getTarballName());
    	assertNotNull(versionInfo.getLicenses());
    	assertNotNull(versionInfo.getLicenses().get(0).getType());
    	versionInfo = JNPM.instance().bestMatch("socket.io", "2.1.1");
    	versionInfo = JNPM.instance().bestMatch("merge-stream", "^2.0.0");
    	versionInfo = JNPM.instance().bestMatch("tmp", "^0.0.33");
    	versionInfo = JNPM.instance().bestMatch("fs-extra", "^1.0.0");
    	
    	versionInfo = JNPM.instance().bestMatch("buffer-crc32", "0.2.1");
    	assertNotNull(versionInfo.getContributors());
    	versionInfo = JNPM.instance().bestMatch("buffer-crc32", "0.2.0");
    	versionInfo = JNPM.instance().bestMatch("performance-now", "^2.1.0");
    }
    
    @Test
    public void downloadTarball() throws IOException {
    	VersionInfo versionInfo = JNPM.instance().retrieveVersion("vue", "2.6.11");
    	assertNotNull(versionInfo);
    	File localFile = versionInfo.getLocalTarball();
    	if(localFile.exists()) localFile.delete();
    	versionInfo.downloadTarball().blockingAwait(20, TimeUnit.SECONDS);
    	assertTrue(localFile.exists());
    }
    
    @Test
    public void downloadWithDevDependencies() throws IOException {
    	VersionInfo versionInfo = JNPM.instance().retrieveVersion("vue", "2.6.11");
    	assertNotNull(versionInfo);
    	File localFile = versionInfo.getLocalTarball();
    	if(localFile.exists()) localFile.delete();
    	log.info("version = "+versionInfo);
    	versionInfo.download(true, true, true, false, false).blockingAwait(20, TimeUnit.SECONDS);
    	assertTrue(localFile.exists());
    }
    
    @Test
    public void searchTest() throws IOException {
    	Single<SearchResults> searchResults = JNPM.instance().getNpmRegistryService().search("vue", null, null, null, null, null);
    	assertNotNull(searchResults);
    	
    	searchResults = JNPM.instance().getNpmRegistryService().search("vue", null, null);
    	assertNotNull(searchResults);
    }
}
