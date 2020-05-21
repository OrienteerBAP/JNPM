package org.orienteer.jnpm;

import java.util.function.Predicate;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.github.zafarkhaja.semver.expr.MavenParser;
import com.github.zafarkhaja.semver.util.UnexpectedElementException;

public final class JNPMUtils {
	
	private JNPMUtils() {
		
	}
	
	public static Predicate<Version> toPredicate(String versionConstraint) {
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
}
