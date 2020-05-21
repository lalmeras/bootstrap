package org.likide.bootstrap.picocli;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class GraalSubstitutions {
}

@TargetClass(className = "picocli.CommandLine", innerClass = "Interpreter")
final class Picocli {
	@Substitute
	private void registerBuiltInConverters() {
		
	}
}
