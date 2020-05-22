package org.orienteer.jnpm.cli;

import picocli.CommandLine.Option;

public class InstallCommand {
	
	static class ExclusiveSave {
		@Option(names = {"-P", "--save-prod"}, required = true) boolean saveProd;
        @Option(names = {"-D", "--save-dev"}, required = true) boolean saveDev;
        @Option(names = {"-O", "--save-optional"}, required = true) boolean saveOptional;
	}
}
