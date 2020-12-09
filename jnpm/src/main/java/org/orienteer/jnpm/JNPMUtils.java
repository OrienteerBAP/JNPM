package org.orienteer.jnpm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.orienteer.jnpm.dm.VersionInfo;

import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import com.vdurmont.semver4j.Semver.SemverType;

import lombok.experimental.UtilityClass;

/**
 * Utility class for JNPM functionality
 */
public final class JNPMUtils {
	
	private JNPMUtils() {
	}
	
	private static class StringReplacer implements Function<String, String> {
		private Pattern pattern;
		private String replacement;
		public StringReplacer(String regex, String replacement) {
			this(Pattern.compile(regex), replacement);
		}
		
		public StringReplacer(Pattern pattern, String replacement) {
			this.pattern = pattern;
			this.replacement = replacement;
		}

		@Override
		public String apply(String t) {
			return pattern.matcher(t).replaceAll(replacement);
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
	 * @param replacement actual replacament
	 * @return function for performing strings replacements
	 */
	public static Function<String, String> stringReplacer(String regexp, String replacement) {
		return new StringReplacer(regexp, replacement);
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
				     ArchiveInputStream a = new TarArchiveInputStream(gzi)) {
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
			     ArchiveInputStream a = new TarArchiveInputStream(gzi)) {
			
				ArchiveEntry entry;
				while((entry = a.getNextEntry()) !=null) {
					if(matcher!=null && !matcher.test(entry)) continue;
					String entryName = entry.getName();
//					log.info("Scanning: "+entryName);
					String newName = pathConverter!=null?pathConverter.apply(entryName):entryName;
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
	
	private static final MimetypesFileTypeMap MIME_TYPE_MAP = new MimetypesFileTypeMap();
	
	/**
	 * Suggest mime type according to filename
	 * @param fileName
	 * @return
	 */
	public static String fileNameToMimeType(String fileName) {
		if(fileName==null) return null;
		int indx = fileName.lastIndexOf('.');
		if(indx>=0) {
			String extension = fileName.substring(indx+1).toLowerCase();
			switch (extension) {
			case "json":
				return "application/json";
			case "js":
				return "text/javascript";
			case "css":
				return "text/css";
			case "htm":
			case "html":
			case "xhtml":
				return "text/html";
			}
		}
		return MIME_TYPE_MAP.getContentType(fileName);
	}
	
	public static boolean isValidVersion(String versionExpression) {
		try {
			new Semver(versionExpression, SemverType.NPM);
			return true;
		} catch (SemverException e) {
			return false;
		}
	}
}
