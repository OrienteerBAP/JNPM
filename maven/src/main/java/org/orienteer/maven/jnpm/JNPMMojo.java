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
import org.orienteer.jnpm.ILogger;
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
     * Prefix for the directory under outputDirectory to which files will be placed
     */
	@Parameter
	private String pathPrefix;
	
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
	
	/**
	 * NPM registry URL to be used for package lookup and retrieval
	 */
	@Parameter(defaultValue = JNPMSettings.DEFAULT_REGISTRY_URL)
	private String registryUrl;
	
	/**
	 * Username for authentication (optional)
	 */
	@Parameter
	private String username;
	
	/**
	 * Password for authentication (optional)
	 */
	@Parameter
	private String password;
	
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
    	
    	final Path outputDirectoryPath = outputDirectory.toPath();
    	final Path targetPath;
    	if(pathPrefix!=null && pathPrefix.trim().length()>0) {
    		targetPath = outputDirectoryPath.resolve(pathPrefix);
    		if(!targetPath.toAbsolutePath().startsWith(outputDirectoryPath.toAbsolutePath()))
    			throw new MojoExecutionException("'pathPrefix' should point to subdirectory");
    		getLog().info("Target directory for extract: "+targetPath);
    	} else {
    		targetPath = outputDirectoryPath;
    	}
    	if(!JNPMService.isConfigured())
    		JNPMService.configure(prepareSettingsBuilder().build());
    	RxJNPMService rxService = JNPMService.instance().getRxService();
    	getLog().info("Prod="+getProd+" dev="+getDev+" optional="+getOptional+" peer="+getPeer);
    	ITraversalRule rule = ITraversalRule.getRuleFor(getProd, getDev, getOptional, getPeer);
    	Observable<TraversalTree> observable = rxService.traverse(TraverseDirection.WIDER, rule, packages)
				.doOnNext(t->ILogger.getLogger().log(String.format("Downloading %s@%s\n", t.getVersion().getName(), t.getVersion().getVersionAsString())));
		observable.flatMapCompletable(t -> t.install(targetPath, strategy)).blockingAwait();
		if(attachResources) projectHelper.addResource(project, outputDirectory.getAbsolutePath(), includes, excludes);
    }
    
    protected JNPMSettings.JNPMSettingsBuilder prepareSettingsBuilder() {
    	return JNPMSettings.builder()
    					   .registryUrl(registryUrl)
    					   .username(username).password(password)
    					   .logger(new ILogger() {
								@Override
								public void log(String message, Throwable exc) {
									getLog().error(message, exc);
								}
								
								@Override
								public void log(String message) {
									getLog().info(message);
								}
							});
    }
}
