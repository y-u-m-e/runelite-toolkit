package ws.yume.toolkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Keeps the toolkit plugin-neutral: every source file under {@code src/main/java/ws/yume/toolkit}
 * must be in a {@code ws.yume.toolkit} package and may import only the JDK, the RuneLite client
 * API, the libraries that ship with the client, and the toolkit itself. An import from a plugin's
 * own package fails the build.
 *
 * <p>The path is relative to the Gradle project directory, so the test also works in a plugin that
 * vendored the toolkit with its tests, where it checks only the vendored toolkit tree.
 */
public class ImportBoundaryTest
{
    private static final Path TOOLKIT_SRC = Paths.get("src", "main", "java", "ws", "yume", "toolkit");

    /** Import roots that the RuneLite client provides to every Plugin Hub plugin, plus the toolkit. */
    private static final String[] ALLOWED_PREFIXES = {
        "java.",
        "javax.",
        "net.runelite.",
        "com.google.gson.",
        "com.google.common.",
        "com.google.inject.",
        "lombok.",
        "org.slf4j.",
        "ws.yume.toolkit.",
    };

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+(?:\\.\\*)?)\\s*;");
    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");

    @Test
    public void mainSourcesImportOnlyAllowedPackages() throws IOException
    {
        assertTrue("toolkit source tree not found at " + TOOLKIT_SRC.toAbsolutePath(), Files.isDirectory(TOOLKIT_SRC));

        List<Path> sources;
        try (Stream<Path> walk = Files.walk(TOOLKIT_SRC))
        {
            sources = walk.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
        assertTrue("no toolkit sources found", !sources.isEmpty());

        List<String> problems = new ArrayList<>();
        for (Path source : sources)
        {
            boolean sawPackage = false;
            for (String line : Files.readAllLines(source, StandardCharsets.UTF_8))
            {
                Matcher pkg = PACKAGE.matcher(line);
                if (pkg.find())
                {
                    sawPackage = true;
                    String name = pkg.group(1);
                    if (!name.equals("ws.yume.toolkit") && !name.startsWith("ws.yume.toolkit."))
                    {
                        problems.add(source + ": package " + name);
                    }
                }
                Matcher imp = IMPORT.matcher(line);
                if (imp.find() && !allowed(imp.group(1)))
                {
                    problems.add(source + ": import " + imp.group(1));
                }
            }
            if (!sawPackage)
            {
                problems.add(source + ": no package declaration");
            }
        }

        assertTrue("toolkit sources reach outside the allowed packages:\n" + String.join("\n", problems), problems.isEmpty());
    }

    private static boolean allowed(String name)
    {
        for (String prefix : ALLOWED_PREFIXES)
        {
            if (name.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }
}
