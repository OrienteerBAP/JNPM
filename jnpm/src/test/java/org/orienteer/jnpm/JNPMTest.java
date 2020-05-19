package org.orienteer.jnpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.orienteer.jnpm.traversal.ITraversalRule.DEPENDENCIES;
import static org.orienteer.jnpm.traversal.ITraversalRule.DEV_DEPENDENCIES;
import static org.orienteer.jnpm.traversal.ITraversalRule.combine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;
import org.orienteer.jnpm.traversal.TraversalContext;

import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

/**
 * Unit test for simple App.
 */
@Slf4j
public class JNPMTest 
{
	static {
		JNPMService.configure(JNPMSettings.builder()
						.homeDirectory(Paths.get("target", ".jnpm"))
 						.build());
	}
	
    @Test
    public void registryInfoRetrival() throws IOException {
    	Single<RegistryInfo> info = JNPMService.instance().getRxService().getRegistryInfo();
    	assertNotNull(info);
    }
    
    @Test
    public void packageInfoRetrival() throws IOException {
    	PackageInfo packageInfo = JNPMService.instance().getPackageInfo("vue");
    	assertNotNull(packageInfo);
    	assertNotNull(packageInfo.getDistTags());
    	assertTrue(packageInfo.getDistTags().containsKey("latest"));
    }
    
    @Test
    public void versionInforetrival() throws IOException {
    	VersionInfo versionInfo = JNPMService.instance().getVersionInfo("vue", "2.6.11");
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
    	VersionInfo versionInfo = JNPMService.instance().bestMatch("semver", "^5.6.0");
    	assertNotNull(versionInfo);
    	assertNotNull(versionInfo.getVersion());
    	assertNotNull(versionInfo.getDist());
    	assertNotNull(versionInfo.getDist().getTarballName());
    	assertNotNull(versionInfo.getLicenses());
    	assertNotNull(versionInfo.getLicenses().get(0).getType());
    	versionInfo = JNPMService.instance().bestMatch("socket.io", "2.1.1");
    	versionInfo = JNPMService.instance().bestMatch("merge-stream", "^2.0.0");
    	versionInfo = JNPMService.instance().bestMatch("tmp", "^0.0.33");
    	versionInfo = JNPMService.instance().bestMatch("fs-extra", "^1.0.0");
    	
    	versionInfo = JNPMService.instance().bestMatch("buffer-crc32", "0.2.1");
    	assertNotNull(versionInfo.getContributors());
    	versionInfo = JNPMService.instance().bestMatch("buffer-crc32", "0.2.0");
    	versionInfo = JNPMService.instance().bestMatch("performance-now", "^2.1.0");
    }
    
    @Test
    public void downloadTarball() throws IOException {
    	VersionInfo versionInfo = JNPMService.instance().getVersionInfo("vue", "2.6.11");
    	assertNotNull(versionInfo);
    	File localFile = versionInfo.getLocalTarball();
    	if(localFile.exists()) localFile.delete();
    	versionInfo.downloadTarball().blockingAwait(20, TimeUnit.SECONDS);
    	assertTrue(localFile.exists());
    }
    
    @Test
    public void downloadWithDevDependencies() throws IOException {
    	VersionInfo versionInfo = JNPMService.instance().getVersionInfo("vue", "2.6.11");
    	assertNotNull(versionInfo);
    	File localFile = versionInfo.getLocalTarball();
    	if(localFile.exists()) localFile.delete();
    	log.info("version = "+versionInfo);
    	TraversalContext res = versionInfo.download(true, DEPENDENCIES, DEV_DEPENDENCIES)
					    		.doOnSuccess(ctx -> log.info("Downloaded "+ctx.getTraversed().size()))
					    		.blockingGet();
    	assertTrue(localFile.exists());
    	assertTrue(res.getTraversed().size()>1000);
    }
    
    @Test
    public void searchTest() throws IOException {
    	Single<SearchResults> searchResults = JNPMService.instance().getRxService().search("vue", null, null, null, null, null);
    	assertNotNull(searchResults);
    	
    	searchResults = JNPMService.instance().getRxService().search("vue", null, null);
    	assertNotNull(searchResults);
    }
}
