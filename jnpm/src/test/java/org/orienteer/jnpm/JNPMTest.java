package org.orienteer.jnpm;

import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.orienteer.jnpm.traversal.ITraversalRule.DEPENDENCIES;
import static org.orienteer.jnpm.traversal.ITraversalRule.DEV_DEPENDENCIES;
import static org.orienteer.jnpm.traversal.ITraversalRule.combine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraversalContext;
import org.orienteer.jnpm.traversal.TraversalTree;
import org.orienteer.jnpm.traversal.TraverseDirection;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
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
    	versionInfo.download(true, DEPENDENCIES, DEV_DEPENDENCIES).blockingAwait();
    	assertTrue(localFile.exists());
    }
    
    @Test
    public void mockedTraversal() throws Exception {
    	JNPMService jnpm = JNPMService.instance();
    	RxJNPMService rxJnpm = jnpm.getRxService();
    	
    	JNPMService spyJnpm = spy(jnpm);
    	RxJNPMService spyRxJnpm = mock(RxJNPMService.class, delegatesTo(rxJnpm));
    	doReturn(spyRxJnpm).when(spyJnpm).getRxService();
    	
    	JNPMService original = JNPMService.instance(spyJnpm);
    	
    	try {
			VersionInfo a = new VersionInfo();
			a.setName("a");
			a.setVersionAsString("1.0.0");
			Map<String, String> depA = new HashMap<String, String>();
			depA.put("b", "2.0.0");
			a.setDependencies(depA);
			
			VersionInfo b = new VersionInfo();
			b.setName("b");
			b.setVersionAsString("2.0.0");
			Map<String, String> depB = new HashMap<String, String>();
			depB.put("a", "1.0.0");
			b.setDependencies(depB);
			
			doReturn(a).when(spyJnpm).getVersionInfo("a", "1.0.0");
			when(spyRxJnpm.bestMatch("b", "2.0.0")).thenReturn(Maybe.just(b));
			when(spyRxJnpm.bestMatch("a", "1.0.0")).thenReturn(Maybe.just(a));
			
			assertEquals("a", JNPMService.instance().getVersionInfo("a", "1.0.0").getName());
			assertEquals("1.0.0", JNPMService.instance().getVersionInfo("a", "1.0.0").getVersionAsString());
			assertEquals(a, JNPMService.instance().getRxService().bestMatch("a", "1.0.0").blockingGet());
			assertEquals(b, JNPMService.instance().getRxService().bestMatch("b", "2.0.0").blockingGet());
			
			Observable<TraversalTree> traversal = JNPMService.instance().getRxService()
					.traverse(a, 
							  TraverseDirection.WIDER, 
							  true, 
							  DEPENDENCIES);
			TestObserver<TraversalTree> test = traversal.doOnNext(t->log.info("Traverse"+t)).test();
			test.await(5, TimeUnit.SECONDS);
			test.assertComplete();
			List<TraversalTree> trace = test.values();
			assertNotNull(trace);
			assertEquals(3, trace.size());
			assertEquals(a, trace.get(0).getVersion());
			assertEquals(0, trace.get(0).getLevel());
			assertEquals(b, trace.get(1).getVersion());
			assertEquals(1, trace.get(1).getLevel());
			assertEquals(a, trace.get(2).getVersion());
			assertEquals(2, trace.get(2).getLevel());
		} finally {
			JNPMService.instance(original);
		}
    }
    
    @Test
    public void traversal() throws IOException {
    	VersionInfo versionInfo = JNPMService.instance().getVersionInfo("vue", "2.6.11");
    	assertNotNull(versionInfo);
    	
    	Observable<TraversalTree> traversal = JNPMService.instance().getRxService()
									    		.traverse(versionInfo, 
									    				  TraverseDirection.WIDER, 
									    				  true, 
									    				  ITraversalRule.combine(DEPENDENCIES, DEV_DEPENDENCIES));
    	TestObserver<TraversalContext> test = traversal.doOnNext(t -> log.info("Traverse: "+t))
								     			.lastElement()
								    			.map(TraversalTree::getContext)
								    			.doOnSuccess(ctx -> log.info("Retrieved: "+ctx.getTraversed().size()))
								    			.test();
    	List<TraversalContext> ctxs = test.awaitDone(20, TimeUnit.SECONDS)
				.values();
    	assertNotNull(ctxs);
    	assertEquals(1, ctxs.size());
    	TraversalContext ctx = ctxs.get(0);
    	
    	test.dispose();
    }
    
    @Test
    public void searchTest() throws IOException {
    	Single<SearchResults> searchResults = JNPMService.instance().getRxService().search("vue", null, null, null, null, null);
    	assertNotNull(searchResults);
    	
    	searchResults = JNPMService.instance().getRxService().search("vue", null, null);
    	assertNotNull(searchResults);
    }
}