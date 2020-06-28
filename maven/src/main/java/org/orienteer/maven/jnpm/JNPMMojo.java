package org.orienteer.maven.jnpm;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * Goal which download and extract npm resources
 *
 */
@Mojo (name = "install", 
			defaultPhase = LifecyclePhase.GENERATE_RESOURCES, 
			requiresDependencyResolution = ResolutionScope.NONE,
			threadSafe = true)
public class JNPMMojo
    extends AbstractMojo
{
    /**
     * Location of the output directory
     */
	@Parameter(defaultValue = "${project.build.directory}/jnpm/", required = true)
    private File outputDirectory;
	
	@Parameter(required = true)
	private String packages;
	
	@Parameter(defaultValue = "WEBJARS")
	private InstallationStrategy strategy;
	
	@Parameter(defaultValue = "false")
	private boolean getProd;
	@Parameter(defaultValue = "false")
	private boolean getDev;
	@Parameter(defaultValue = "false")
	private boolean getOptional;
	@Parameter(defaultValue = "false")
	private boolean getPeer;
	
	@Parameter(defaultValue = "true")
	private boolean includeAsResources;
	
	@Parameter
    private List<String> includes;
	
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
    	getLog().info("Packages for installation: "+packages);
    	getLog().info("Strategy for installation: "+strategy);
    	if(!JNPMService.isConfigured())
    		JNPMService.configure(JNPMSettings.builder().build());
    	RxJNPMService rxService = JNPMService.instance().getRxService();
    	getLog().info("Prod="+getProd+" dev="+getDev+" optional="+getOptional+" peer="+getPeer);
    	ITraversalRule rule = ITraversalRule.getRuleFor(getProd, getDev, getOptional, getPeer);
    	String[] packageStatements = packages.split("\\s*[,\\s]\\s*");
    	Observable<TraversalTree> observable = rxService.traverse(TraverseDirection.WIDER, rule, packageStatements)
				.doOnNext(t->System.out.printf("Downloading %s@%s\n", t.getVersion().getName(), t.getVersion().getVersionAsString()));
		observable.flatMapCompletable(t -> t.install(outputDirectory.toPath(), strategy)).blockingAwait();
		if(includeAsResources) projectHelper.addResource(project, outputDirectory.getAbsolutePath(), includes, excludes);
    }
}
