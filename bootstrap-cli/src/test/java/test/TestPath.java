package test;

import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.likide.bootstrap.picocli.DefaultProvider;

public class TestPath {

	@Test
	public void testFallback() {
		Assertions.assertEquals(
				"default",
				DefaultProvider.defaultEnvironmentName(Paths.get("/bootstrap/bootstrap")),
				"Environment name should fallback to 'default'"
		);
	}

	@Test
	public void testDirectory() {
		Assertions.assertEquals(
				"phacochere",
				DefaultProvider.defaultEnvironmentName(Paths.get("/bootstrap/phacochere")),
				"Environment name should be directory name"
		);
	}

	@Test
	public void testIgnoreBootstrap() {
		Assertions.assertEquals(
				"phacochere",
				DefaultProvider.defaultEnvironmentName(Paths.get("/bootstrap/phacochere/bootstrap")),
				"Environment name should be phacochere as bootstrap directory is ignored"
		);
	}
}
