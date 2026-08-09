package com.dragonaltar.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ApiBoundaryTest {
	private static final Pattern INTERNAL_TYPE = Pattern.compile("com/dragonaltar/(?!api/)");

	@Test
	void compiledApiContainsOnlyApiPackagesAndNoInternalTypeReferences() throws IOException {
		Path classes = Path.of("target", "classes");
		try (var files = Files.walk(classes)) {
			for (Path classFile : files.filter(path -> path.toString().endsWith(".class")).toList()) {
				String relative = classes.relativize(classFile).toString().replace('\\', '/');
				assertTrue(relative.startsWith("com/dragonaltar/api/"),
						() -> "Unexpected API artifact class: " + relative);

				String bytecode = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
				assertFalse(INTERNAL_TYPE.matcher(bytecode).find(), () -> relative + " references an internal package");
			}
		}
	}
}
