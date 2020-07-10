package org.orienteer.maven.jnpm;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.orienteer.jnpm.IInstallationStrategy;
import org.orienteer.jnpm.InstallationStrategy;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;
import org.orienteer.jnpm.RxJNPMService;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraversalTree;
import org.orienteer.jnpm.traversal.TraverseDirection;

import io.reactivex.Observable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Goal to download, extract and attach npm resources
 *
 */
@Mojo (name = "install", 
			defaultPhase = LifecyclePhase.GENERATE_RESOURCES, 
			requiresDependencyResolution = ResolutionScope.NONE,
			requiresProject = true,
			requiresOnline = true,
			threadSafe = true)
public class JNPMMojo
    extends AbstractMojo
{
    /**
     * Location of the output directory
     */
	@Parameter(defaultValue = "${project.build.directory}/jnpm/", required = true)
    private File outputDirectory;
	
	/**
	 * NPM packages to be downloaded and extracted (For example: vue@2.6.11)
	 */
	@Parameter(required = true)
	private String[] packages;
	
	/**
	 * Installation strategy to be used
	 */
	@Parameter(defaultValue = "WEBJARS")
	private InstallationStrategy strategy;
	
	/**
	 * Download direct dependencies
	 */
	@Parameter(defaultValue = "false")
	private boolean getProd;
	
	/**
	 * Download development dependencies
	 */
	@Parameter(defaultValue = "false")
	private boolean getDev;
	
	/**
	 * Download optional dependencies
	 */
	@Parameter(defaultValue = "false")
	private boolean getOptional;
	
	/**
	 * Download peer dependencies
	 */
	@Parameter(defaultValue = "false")
	private boolean getPeer;
	
	/**
	 * Attach downloaded resources to the build process
	 */
	@Parameter(defaultValue = "true")
	private boolean attachResources;
	
	/**
	 * What should be included as resources (Default: empty - means everything)
	 */
	@Parameter
    private List<String> includes;
	
	/**
	 * What has to be excluded from resources to be attached
	 */
	@Parameter
    private List<String> excludes;
	
	@Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
	
	@Component
    private MavenProjectHelper projectHelper;

    public void execute()
        throws MojoExecutionException
    {
    	getLog().info("Output directory: "+outputDirectory);
    	getLog().info("Packages for installation: "+String.join(", ", packages));
    	getLog().info("Strategy for installation: "+strategy);
    	if(!JNPMService.isConfigured())
    		JNPMService.configure(JNPMSettings.builder().build());
    	RxJNPMService rxService = JNPMService.instance().getRxService();
    	getLog().info("Prod="+getProd+" dev="+getDev+" optional="+getOptional+" peer="+getPeer);
    	ITraversalRule rule = ITraversalRule.getRuleFor(getProd, getDev, getOptional, getPeer);
    	Observable<TraversalTree> observable = rxService.traverse(TraverseDirection.WIDER, rule, packages)
				.doOnNext(t->System.out.printf("Downloading %s@%s\n", t.getVersion().getName(), t.getVersion().getVersionAsString()));
		observable.flatMapCompletable(t -> t.install(outputDirectory.toPath(), strategy)).blockingAwait();
		if(attachResources) projectHelper.addResource(project, outputDirectory.getAbsolutePath(), includes, excludes);
    }
}
