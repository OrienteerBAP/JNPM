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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.github.zafarkhaja.semver.expr.MavenParser;
import com.github.zafarkhaja.semver.util.UnexpectedElementException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JNPMUtils {
	
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
			this.pattern = pattern.compile(regex);
		}
		
		@Override
		public boolean test(ArchiveEntry t) {
			return pattern.matcher(t.getName()).matches();
		}
	}
	
	private JNPMUtils() {
		
	}
	
	public static Function<String, String> stringReplacer(String regexp, String replacement) {
		return new StringReplacer(regexp, replacement);
	}
	
	private static NameMatcher newNameMatcher(String regexp) {
		if(regexp==null || regexp.trim().length()==0) return null;
		return new NameMatcher(regexp);
	}
	
	public static Predicate<Version> toVersionPredicate(String versionConstraint) {
		if(versionConstraint==null) return v->true;
		Predicate<Version> res=null;
        try {
            res = ExpressionParser.newInstance().parse(versionConstraint);
        } catch (ParseException | UnexpectedElementException e) {
            try {
                res = new MavenParser().parse(versionConstraint);
            } catch (ParseException | UnexpectedElementException e2) {
                //NOP
            }
        }
        return res;
	}
	
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
	
	public static void extractTarball(File tarball, Path destinationDir, Function<String, String> pathConverter) throws IOException {
		extractTarball(tarball, destinationDir, (Predicate<ArchiveEntry>) null, pathConverter); 
	}
	
	public static void extractTarball(File tarball, Path destinationDir, String filePattern, Function<String, String> pathConverter) throws IOException {
		extractTarball(tarball, destinationDir, newNameMatcher(filePattern), pathConverter); 
	}
	
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
	
	public static <K> Map<K, K> toMap(K... items) {
		if(items.length % 2 !=0) throw new IllegalStateException("Expecting even number of arguments");
		Map<K, K> map = new HashMap<>();
		for(int i=0; i<items.length; i=i+2) {
			map.put(items[i], items[i+1]);
		}
		return map;
	}
}
