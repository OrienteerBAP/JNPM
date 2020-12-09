package org.orienteer.jnpm;

import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.orienteer.jnpm.traversal.ITraversalRule.DEPENDENCIES;
import static org.orienteer.jnpm.traversal.ITraversalRule.DEV_DEPENDENCIES;
import static org.orienteer.jnpm.traversal.ITraversalRule.combine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;
import org.orienteer.jnpm.dm.PackageInfo;
import org.orienteer.jnpm.dm.RegistryInfo;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.dm.search.SearchResults;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraversalContext;
import org.orienteer.jnpm.traversal.TraversalTree;
import org.orienteer.jnpm.traversal.TraverseDirection;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import junit.framework.AssertionFailedError;
import lombok.extern.slf4j.Slf4j;

/**
 * Unit test for simple App.
 */
@Slf4j
public class JNPMTest 
{
	
	private static final Random RANDOM = new Random(); 
	static {
		if(!JNPMService.isConfigured())
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
    public void nonExistingPackageInfoRetrival() throws IOException {
    	PackageInfo packageInfo = JNPMService.instance()
    								.getPackageInfo("nosuchpackage");
    	assertTrue(packageInfo==null);
    	VersionInfo version = JNPMService.instance().getVersionInfo("nosuchpackage", "1.0");
    	assertTrue(version==null);
    	//Check for existing package but not existing version
    	version = JNPMService.instance().getVersionInfo("vue", "0.0.1");
    	assertTrue(version==null);
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
    	versionInfo.download(DEPENDENCIES, DEV_DEPENDENCIES).blockingAwait();
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
					.traverse(TraverseDirection.WIDER,
							  DEPENDENCIES, a); 
			TestObserver<TraversalTree> test = traversal.doOnNext(t->log.info("Traverse"+t)).test();
			test.await(5, TimeUnit.SECONDS);
			test.assertComplete();
			List<TraversalTree> trace = test.values();
			assertNotNull(trace);
			assertEquals(3, trace.size());
			assertEquals(a, trace.get(0).getVersion());
			assertEquals(0, trace.get(0).getDependencyLevel());
			assertEquals(b, trace.get(1).getVersion());
			assertEquals(1, trace.get(1).getDependencyLevel());
			assertEquals(a, trace.get(2).getVersion());
			assertEquals(2, trace.get(2).getDependencyLevel());
		} finally {
			JNPMService.instance(original);
		}
    }
    
    
    @Test
    public void testRules() throws Exception {
		VersionInfo main = new VersionInfo();
		main.setName("main");
		main.setVersionAsString("1.0.0");
		main.setDependencies(JNPMUtils.toMap("dep", "1.0.0"));
		main.setDevDependencies(JNPMUtils.toMap("devdep", "2.0.0"));
		main.setOptionalDependencies(JNPMUtils.toMap("optdep", "3.0.0"));
		main.setPeerDependencies(JNPMUtils.toMap("peerdep", "4.0.0"));
		assertTrue(ITraversalRule.NO_DEPENDENCIES.getNextDependencies(main).isEmpty());
		assertEquals(1, ITraversalRule.DEPENDENCIES.getNextDependencies(main).size());
		assertTrue(ITraversalRule.DEPENDENCIES.getNextDependencies(main).containsKey("dep"));
		assertEquals(1, ITraversalRule.DEV_DEPENDENCIES.getNextDependencies(main).size());
		assertTrue(ITraversalRule.DEV_DEPENDENCIES.getNextDependencies(main).containsKey("devdep"));
		assertEquals(1, ITraversalRule.OPT_DEPENDENCIES.getNextDependencies(main).size());
		assertTrue(ITraversalRule.OPT_DEPENDENCIES.getNextDependencies(main).containsKey("optdep"));
		assertEquals(1, ITraversalRule.PEER_DEPENDENCIES.getNextDependencies(main).size());
		assertTrue(ITraversalRule.PEER_DEPENDENCIES.getNextDependencies(main).containsKey("peerdep"));
		
		ITraversalRule comb = ITraversalRule.combine(ITraversalRule.DEPENDENCIES, ITraversalRule.DEV_DEPENDENCIES);
		
		assertEquals(2, comb.getNextDependencies(main).size());
		assertTrue(comb.getNextDependencies(main).containsKey("dep"));
		assertTrue(comb.getNextDependencies(main).containsKey("devdep"));
    }
    
    @Test
    public void traversal() throws IOException {
    	
    	Observable<TraversalTree> traversal = JNPMService.instance().getRxService()
									    		.traverse(TraverseDirection.WIDER,
									    				  ITraversalRule.combine(DEPENDENCIES, DEV_DEPENDENCIES), 
									    				  "a@2.1.2", "b@2.0.1", "c@1.0.0", "d@1.0.1");
    	TestObserver<TraversalContext> test = traversal.doOnNext(t -> log.info("Traverse: "+t))
								     			.lastElement()
								    			.map(TraversalTree::getContext)
								    			.doOnSuccess(ctx -> log.info("Retrieved: "+ctx.getTraversed().size()))
								    			.doOnComplete(() -> log.info("Completed"))
								    			.doOnError(t -> log.error("Error accured", t))
								    			.test();
    	List<TraversalContext> ctxs = test.awaitDone(60, TimeUnit.SECONDS)
				.values();
    	assertNotNull(ctxs);
    	assertEquals(1, ctxs.size());
    	TraversalContext ctx = ctxs.get(0);
    	assertTrue(ctx.getTraversed().size()>100);
    	test.dispose();
    }
    
    @Test
    public void searchTest() throws IOException {
    	Single<SearchResults> searchResults = JNPMService.instance().getRxService().search("vue", null, null, null, null, null);
    	assertNotNull(searchResults);
    	
    	searchResults = JNPMService.instance().getRxService().search("vue", null, null);
    	assertNotNull(searchResults);
    }
    
    @Test
    public void tarballAccessTest() throws IOException {
		File file = new File("src/test/resources/test.tar.gz");
		assertEquals("a", readTarball(file, "test/a.txt"));
		assertEquals("b", readTarball(file, "test/a/b.txt"));
		assertEquals("c", readTarball(file, "test/a/b/c.txt"));
    }
    
    private String readTarball(File file, String path) throws IOException {
    	try(ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
			JNPMUtils.readTarball(file, path, baos);
			return new String(baos.toByteArray());
		}
    }
    
    @Test
    public void tarballExtractTest() throws IOException {
    	File tarball = new File("src/test/resources/test.tar.gz");
		Path destinationDir = Paths.get("target/extractTo"+RANDOM.nextInt(999999));
		JNPMUtils.extractTarball(tarball, destinationDir, ".*", JNPMUtils.stringReplacer("^test/", ""));
		assertEquals("a", readFile(destinationDir.resolve("a.txt")));
		assertEquals("b", readFile(destinationDir.resolve("a/b.txt")));
		assertEquals("c", readFile(destinationDir.resolve("a/b/c.txt")));
    }
    
    private String readFile(Path path) throws IOException {
    	try(InputStream fis = Files.newInputStream(path);) {
			return new String(IOUtils.toByteArray(fis));
		}
    }
    
    @Test
    public void installTest() throws IOException {
    	TraversalTree tree = JNPMService.instance().getRxService()
				.traverse(TraverseDirection.WIDER, ITraversalRule.NO_DEPENDENCIES, "vue@2.6.11")
				.blockingFirst();
    	assertInstallation(tree, "target/flatInstall", InstallationStrategy.FLAT, "vue-2.6.11/package.json", "\"version\": \"2.6.11\"");
    	assertInstallation(tree, "target/npmInstall", InstallationStrategy.NPM, "node_modules/vue/package.json", "\"version\": \"2.6.11\"");
    	assertInstallation(tree, "target/simpleInstall", InstallationStrategy.SIMPLE, "vue/package.json", "\"version\": \"2.6.11\"");
    }
    
    private void assertInstallation(TraversalTree tree, String pathPrefix, IInstallationStrategy strategy, String filePath, String content) throws IOException {
    	Path destinationDir = Paths.get(pathPrefix+RANDOM.nextInt(999999));
    	tree.install(destinationDir, strategy).blockingAwait();
    	Path filePathToRead = destinationDir.resolve(filePath);
    	assertTrue("Target file not found: "+filePathToRead, filePathToRead.toFile().exists());
    	String packageContent = readFile(filePathToRead);
    	assertTrue("Target file doesn't contain required content: "+content,packageContent.contains(content));
    }
    
    @Test
    public void readmeExamplesTest() throws Exception {
    	//Print NPM Registry Information
    	System.out.println(JNPMService.instance().getRegistryInfo());
    	//Retrieve and print VUE package latest version
    	System.out.println(JNPMService.instance().getPackageInfo("vue").getLatest());
    	//Print package description for vue@2.6.11
    	System.out.println(JNPMService.instance().getVersionInfo("vue", "2.6.11").getDescription());
    	//Print latest version prior to vue version 2 official release
    	System.out.println(JNPMService.instance().bestMatch("vue@<2").getVersionAsString());
    	//Download tarball for vue@2.6.11 and print localpath
    	VersionInfo vueVersion = JNPMService.instance().getVersionInfo("vue", "2.6.11");
    	vueVersion.downloadTarball().blockingAwait();
    	System.out.println(vueVersion.getLocalTarball().getAbsolutePath());
    	//Search for "vue" and print description for first result
    	System.out.println(JNPMService.instance().search("vue").getObjects().get(0).getSearchPackage().getDescription());
    	//Traverse through all dev dependencies of latest vue package, print information
    	// and install as NPM do (node_modules/vue and etc)
    	JNPMService.instance().getRxService()
    		.traverse(TraverseDirection.WIDER, ITraversalRule.DEV_DEPENDENCIES, "vue")
    		.subscribe(t -> {System.out.println(t); t.install(Paths.get("target", "readme"), InstallationStrategy.NPM);});
    }
    
    @Test
    public void semverTest() throws Exception {
    	assertTrue(new Semver("2.6.11", SemverType.NPM).satisfies("2.x"));
    	assertTrue(!new Semver("2.6.11", SemverType.NPM).satisfies("1.x"));
    	assertTrue(!new Semver("2.6.11", SemverType.NPM).satisfies("3.x"));
    	assertSatisfies("2.6.1", new String[]{"1.x", "2.x", "3.x"},
    							 new boolean[] {false, true, false});
    	assertSatisfies("1.2.0", new String[]{"1.x", "2.x", "3.x"},
				 new boolean[] {true, false, false});
    	assertSatisfies("2.6.11", new String[]{"~1.5", "~2.5", "~2.6"},
				 				  new boolean[] {false, false, true});
    	//TBD Uncomment when https://github.com/vdurmont/semver4j/issues/48 will be resolved
    	assertSatisfies("3.0.0-beta.1", new String[]{"~1.5", "~2.5", "~2.6", "~3.0"/*, "<3"*/},
    									new boolean[] {false, false, false, false/*, true*/});
    }
    
    private void assertSatisfies(String versionStr, String[] conditions, boolean[] results) {
    	assertNotNull("Conditions shouldn't be bull", conditions);
    	assertNotNull("Results samples shouldn't be bull", results);
    	assertEquals("Length of conditions and results should match", conditions.length, results.length);
    	assertTrue("Version should be valid", JNPMUtils.isValidVersion(versionStr));
    	Semver version = new Semver(versionStr, SemverType.NPM);
    	for(int i=0; i<conditions.length; i++) {
    		if(results[i] ^ version.satisfies(conditions[i]))
    			throw new AssertionFailedError("Version '"+versionStr+"' should "+(results[i]?"":"NOT ")+"satisfy '"+conditions[i]+"'");
    	}
    }
}
