package org.orienteer.jnpm;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SPI interface for logging in JNPM to provide required flexibility in logging approach
 */
public interface ILogger {
	
	/**
	 * Default {@link ILogger} implementation which use {@link Logger}
	 */
	public static final ILogger DEFAULT =  new ILogger() {
		    private final Logger defaultSystemLogger = Logger.getLogger("JNPM");
	
			@Override
			public void log(String message) {
				defaultSystemLogger.info(message);
			}
	
			@Override
			public void log(String message, Throwable exc) {
				defaultSystemLogger.log(Level.WARNING, message, exc);
			}
			
		};
	
	public void log(String message);
	public void log(String message, Throwable exc);
	
	public static ILogger getLogger() {
		return JNPMService.instance().getSettings().getLogger();
	}
}
