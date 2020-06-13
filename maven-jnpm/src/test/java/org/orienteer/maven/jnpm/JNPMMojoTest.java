package org.orienteer.maven.jnpm;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class JNPMMojoTest {
	@Rule
	public MojoRule mojoRule = new MojoRule();
	
	@Rule
	public TestResources testResources = new TestResources( "src/test/resources", "target/test-projects");
	
	private File testResourcesBaseDirectory;
	private File testProjectTargetDirectory;
	
	private JNPMMojo mojo;
	
	@Before
	public void setup() throws Exception {
		this.testResourcesBaseDirectory = this.testResources.getBasedir("test-project");
		assertNotNull(this.testResourcesBaseDirectory);
		assertTrue(this.testResourcesBaseDirectory.isDirectory());
		final MavenProject mavenProject = this.mojoRule.readMavenProject(this.testResourcesBaseDirectory);
		assertNotNull(mavenProject);
		this.testProjectTargetDirectory = new File(mavenProject.getBuild().getDirectory());
		this.testProjectTargetDirectory.mkdirs();
		this.mojo = (JNPMMojo)this.mojoRule.lookupConfiguredMojo(mavenProject, "install");
		assertNotNull(this.mojo);
		/*
		 * assertNotNull(this.mojo.getConfiguration());
		 * assertNotNull(this.mojo.getProject());
		 * assertNotNull(this.mojo.getTemplateName());
		 */
	}
	
	@Test
    public void testSomething()
        throws Exception
    {
		mojo.execute();
    }
}
