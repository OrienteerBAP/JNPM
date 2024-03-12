package org.orienteer.jnpm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.orienteer.jnpm.dm.VersionInfo;
import org.orienteer.jnpm.traversal.TraversalTree;

import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import com.vdurmont.semver4j.SemverException;

/**
 * Utility class for JNPM functionality
 */
public final class JNPMUtils {
	
	private JNPMUtils() {
	}
	
	private static class StringReplacer implements Function<String, String> {
		private Pattern pattern;
		private String replacement;
		private boolean shouldMatch;
		public StringReplacer(String regex, String replacement, boolean shouldMatch) {
			this(Pattern.compile(regex), replacement, shouldMatch);
		}
		
		public StringReplacer(Pattern pattern, String replacement, boolean shouldMatch) {
			this.pattern = pattern;
			this.replacement = replacement;
			this.shouldMatch = shouldMatch;
		}

		@Override
		public String apply(String t) {
			Matcher m = pattern.matcher(t);
			if(shouldMatch && !m.find()) return null;
			return m.replaceAll(replacement);
		}
	}
	
	private static class NameMatcher implements Predicate<ArchiveEntry> {
		private Pattern pattern;
		public NameMatcher(String regex) {
			this.pattern = Pattern.compile(regex);
		}
		
		@Override
		public boolean test(ArchiveEntry t) {
			return pattern.matcher(t.getName()).matches();
		}
	}
	
	/**
	 * Creates a function for replacement according to pattern
	 * @param regexp pattern to be used for replacement
	 * @param replacement actual replacement
	 * @param shouldMatch if true - return null of not matches found
	 * @return function for performing strings replacements
	 */
	public static Function<String, String> stringReplacer(String regexp, String replacement, boolean shouldMatch) {
		return new StringReplacer(regexp, replacement, shouldMatch);
	}
	
	private static NameMatcher newNameMatcher(String regexp) {
		if(regexp==null || regexp.trim().length()==0) return null;
		return new NameMatcher(regexp);
	}
	
	/**
	 * Convert version constraint to {@link Requirement}
	 * @param versionConstraint - text representation of version constraint
	 * @return null if version constraint is invalid or actual predicate
	 */
	public static Requirement toVersionPredicate(final String versionConstraint) {
		try {
			return Requirement.buildNPM(versionConstraint);
		} catch (SemverException e) {
			return null;
		}
	}
	
	/**
	 * Read file from tarball of a specific version and write it to provided {@link OutputStream}
	 * @param version package version to be read
	 * @param path path to be found in the tarball
	 * @param out {@link OutputStream} to write to
	 * @throws IOException if file can't be read
	 */
	public static void readTarball(VersionInfo version, String path, OutputStream out) throws IOException {
		if(version!=null) readTarball(version.getLocalTarball(), path, out);
		else throw new FileNotFoundException("VersionInfo was not found");
	}
	
	/**
	 * Read file from tarball and write it to provided {@link OutputStream}
	 * @param tarball file to read
	 * @param path path to be found in the tarball
	 * @param out {@link OutputStream} to write to
	 * @throws IOException if file can't be read
	 */
	public static void readTarball(File tarball, String path, OutputStream out) throws IOException {
		if(tarball!=null && tarball.exists() && path!=null) {
			try (InputStream fi = new FileInputStream(tarball);
				     InputStream bi = new BufferedInputStream(fi);
				     InputStream gzi = new GzipCompressorInputStream(bi);
					 TarArchiveInputStream a = new TarArchiveInputStream(gzi)) {
				 	if(path.startsWith("/")) path = path.substring(1);
					ArchiveEntry entry;
					while((entry = a.getNextEntry()) !=null) {
//						log.info("Scanning: "+entry.getName());
						if(path.equals(entry.getName())) {
							IOUtils.copy(a, out);
							return;
						}
					}
				}
		}
		throw new FileNotFoundException("Archived file '"+path+"' was not found in tarball "+tarball);
	}
	
	/**
	 * Extract content of tarball to specified directory
	 * @param tarball file to extract
	 * @param destinationDir destination directory
	 * @param tree traversal tree currently traversed
	 * @param strategy strategy to map resources
	 * @throws IOException if tarball can't be extracted for some reason
	 */
	public static void extractTarball(File tarball, Path destinationDir, TraversalTree tree, IInstallationStrategy strategy) throws IOException {
		extractTarball(tarball, strategy.mapPath(destinationDir, tree), strategy.entreeMatcher(), strategy.entreeNameMapper());
	}
	
	/**
	 * Extract content of tarball to specified directory
	 * @param tarball file to extract
	 * @param destinationDir destination directory
	 * @param pathConverter converter of paths
	 * @throws IOException if tarball can't be extracted for some reason
	 */
	public static void extractTarball(File tarball, Path destinationDir, Function<String, String> pathConverter) throws IOException {
		extractTarball(tarball, destinationDir, (Predicate<ArchiveEntry>) null, pathConverter); 
	}
	
	/**
	 * Extract content of tarball to specified directory
	 * @param tarball file to extract
	 * @param destinationDir destination directory
	 * @param filePattern pattern for files to be extracted
	 * @param pathConverter converter of paths
	 * @throws IOException if tarball can't be extracted for some reason
	 */
	public static void extractTarball(File tarball, Path destinationDir, String filePattern, Function<String, String> pathConverter) throws IOException {
		extractTarball(tarball, destinationDir, newNameMatcher(filePattern), pathConverter); 
	}
	
	/**
	 * Extract content of tarball to specified directory
	 * @param tarball file to extract
	 * @param destinationDir destination directory
	 * @param matcher predicate to determine which files should be extracted
	 * @param pathConverter converter of paths
	 * @throws IOException if tarball can't be extracted for some reason
	 */
	public static void extractTarball(File tarball, Path destinationDir, Predicate<ArchiveEntry> matcher, Function<String, String> pathConverter) throws IOException {
		if(tarball==null || !tarball.exists()) 
			throw new FileNotFoundException("Tarball was not found: "+tarball);
		try (InputStream fi = new FileInputStream(tarball);
			     InputStream bi = new BufferedInputStream(fi);
			     InputStream gzi = new GzipCompressorInputStream(bi);
				 TarArchiveInputStream a = new TarArchiveInputStream(gzi)) {
			
				ArchiveEntry entry;
				while((entry = a.getNextEntry()) !=null) {
					if(matcher!=null && !matcher.test(entry)) continue;
					String entryName = entry.getName();
//					log.info("Scanning: "+entryName);
					String newName = pathConverter!=null?pathConverter.apply(entryName):entryName;
					if(newName==null) continue;
					Path newPath = destinationDir.resolve(newName);
					if(!newPath.toAbsolutePath().startsWith(destinationDir.toAbsolutePath()))
						throw new IOException("File should be under destination directory. Destination dir: "+destinationDir+". File: "+newPath);
					if(entry.isDirectory()) {
						Files.createDirectories(newPath);
					} else {
						Files.createDirectories(newPath.getParent());
						try (OutputStream o = Files.newOutputStream(newPath)) {
			                IOUtils.copy(a, o);
			            }
					}
					
				}
			}
	}
	
	/**
	 * Method to simplify creation of maps
	 * @param <K> type of keys and values for the map
	 * @param items items which needs to packed into map
	 * @return resultant map
	 */
	public static <K> Map<K, K> toMap(K... items) {
		if(items.length % 2 !=0) throw new IllegalStateException("Expecting even number of arguments");
		Map<K, K> map = new HashMap<>();
		for(int i=0; i<items.length; i=i+2) {
			map.put(items[i], items[i+1]);
		}
		return map;
	}
	
	private static final Map<String, String> MIME_TYPES_MAPPING = new HashMap<>();
	
	{
		MIME_TYPES_MAPPING.put("json", "application/json");
		MIME_TYPES_MAPPING.put("js", "text/javascript");
		MIME_TYPES_MAPPING.put("cjs", "application/node");
		MIME_TYPES_MAPPING.put("mjs", "text/javascript;goal=Module");
		MIME_TYPES_MAPPING.put("css", "text/css");
		MIME_TYPES_MAPPING.put("htm", "text/html");
		MIME_TYPES_MAPPING.put("html", "text/html");
		MIME_TYPES_MAPPING.put("xhtml", "text/html");
		MIME_TYPES_MAPPING.put("txt", "text/plain");
		MIME_TYPES_MAPPING.put("text", "text/plain");
		MIME_TYPES_MAPPING.put("gif", "image/gif");
		MIME_TYPES_MAPPING.put("ief", "image/ief");
		MIME_TYPES_MAPPING.put("jpeg", "image/jpeg");
		MIME_TYPES_MAPPING.put("jpg", "image/jpeg");
		MIME_TYPES_MAPPING.put("jpe", "image/jpeg");
		MIME_TYPES_MAPPING.put("tiff", "image/tiff");
		MIME_TYPES_MAPPING.put("tif", "image/tiff");
		MIME_TYPES_MAPPING.put("png", "image/png");
		MIME_TYPES_MAPPING.put("xwd", "image/x-xwindowdump");
		MIME_TYPES_MAPPING.put("ai", "application/postscript");
		MIME_TYPES_MAPPING.put("eps", "application/postscript");
		MIME_TYPES_MAPPING.put("ps", "application/postscript");
		MIME_TYPES_MAPPING.put("rtf", "application/rtf");
		MIME_TYPES_MAPPING.put("tex", "application/x-tex");
		MIME_TYPES_MAPPING.put("texinfo", "application/x-texinfo");
		MIME_TYPES_MAPPING.put("texi", "application/x-texinfo");
		MIME_TYPES_MAPPING.put("t", "application/x-troff");
		MIME_TYPES_MAPPING.put("tr", "application/x-troff");
		MIME_TYPES_MAPPING.put("roff", "application/x-troff");
		MIME_TYPES_MAPPING.put("au", "audio/basic");
		MIME_TYPES_MAPPING.put("midi", "audio/midi");
		MIME_TYPES_MAPPING.put("mid", "audio/midi");
		MIME_TYPES_MAPPING.put("aifc", "audio/x-aifc");
		MIME_TYPES_MAPPING.put("aif", "audio/x-aiff");
		MIME_TYPES_MAPPING.put("aiff", "audio/x-aiff");
		MIME_TYPES_MAPPING.put("mpeg", "audio/x-mpeg");
		MIME_TYPES_MAPPING.put("mpg", "audio/x-mpeg");
		MIME_TYPES_MAPPING.put("wav", "audio/x-wav");
		MIME_TYPES_MAPPING.put("mpeg", "video/mpeg");
		MIME_TYPES_MAPPING.put("mpg", "video/mpeg");
		MIME_TYPES_MAPPING.put("mpe", "video/mpeg");
		MIME_TYPES_MAPPING.put("qt", "video/quicktime");
		MIME_TYPES_MAPPING.put("mov", "video/quicktime");
		MIME_TYPES_MAPPING.put("avi", "video/x-msvideo");
	}
	
	/**
	 * Suggest mime type according to filename
	 * @param fileName name of a file to analize
	 * @return mime type or null of type can't be recognized
	 */
	public static String fileNameToMimeType(String fileName) {
		if(fileName==null) return null;
		String extension = fileName;
		int indx = fileName.lastIndexOf('.');
		if(indx>=0) 
			extension = fileName.substring(indx+1).toLowerCase();
		return MIME_TYPES_MAPPING.getOrDefault(extension, "application/octet-stream");
	}
	
	/**
	 * Check version expression for validity
	 * @param versionExpression version expression to be checked
	 * @return true - if Semver can be create from specified version
	 */
	public static boolean isValidVersion(String versionExpression) {
		try {
			new Semver(versionExpression, SemverType.NPM);
			return true;
		} catch (SemverException e) {
			return false;
		}
	}
}
