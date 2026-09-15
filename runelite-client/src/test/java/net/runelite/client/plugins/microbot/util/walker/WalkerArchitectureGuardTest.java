package net.runelite.client.plugins.microbot.util.walker;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Source-level boundaries for the incremental walker rewrite.
 *
 * <p>These checks intentionally protect ownership rather than package layout. Moving a legacy
 * method into a new class is not decomposition when that class can still replace the active target,
 * restart pathfinding, or publish mutable plugin state. The limits may only move downward as legacy
 * orchestration is deleted.</p>
 */
public class WalkerArchitectureGuardTest
{
	private static final String WALKER_RELATIVE =
		"util/walker/Rs2Walker.java";
	private static final String PATH_API_RELATIVE =
		"util/walker/Rs2PathApi.java";

	private static final Pattern NON_CODE = Pattern.compile(
		"(?s)/\\*.*?\\*/|(?m)//[^\\r\\n]*|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");

	private static final List<String> SHORTEST_PATH_MUTABLE_ACCESS = Arrays.asList(
		"ShortestPathPlugin.getPathfinder(",
		"ShortestPathPlugin.setPathfinder(",
		"ShortestPathPlugin.getPathfinderFuture(",
		"ShortestPathPlugin.setPathfinderFuture(",
		"ShortestPathPlugin.getPathfindingExecutor(",
		"ShortestPathPlugin.setPathfindingExecutor(",
		"ShortestPathPlugin.getPathfinderMutex(",
		"ShortestPathPlugin.getPathfinderConfig(",
		"ShortestPathPlugin.setReachedDistance(",
		"ShortestPathPlugin.setStartPointSet(",
		"ShortestPathPlugin.setLastLocation(",
		"ShortestPathPlugin.getMarker(",
		"ShortestPathPlugin.setMarker(",
		"ShortestPathPlugin.getTransports(",
		"ShortestPathPlugin.exit(",
		"ShortestPathPlugin.pathfinder"
	);

	private static final List<String> FORBIDDEN_HANDLER_LIFECYCLE_ACCESS = Arrays.asList(
		"Rs2Walker.setTarget(",
		"Rs2Walker.clearWalkingRoute(",
		"Rs2Walker.recalculatePath(",
		"Rs2Walker.restartPathfinding(",
		"Rs2WalkerLifecycleRuntime.",
		"Rs2PathApi.setPathfinder(",
		"Rs2PathApi.setPathfinderFuture(",
		"Rs2PathApi.setPathfindingExecutor(",
		"Rs2PathApi.exit("
	);

	private static final List<String> REMOVED_LIFECYCLE_OWNERS = Arrays.asList(
		"pathfindingExecutor",
		"pathfinderFuture",
		"pathfinderMutex",
		"ShortestPathPlugin.setPathfinder("
	);

	private static final List<String> NAVIGATION_ENGINE_INPUT_ACCESS = Arrays.asList(
		"Rs2Walker.",
		"Rs2MiniMap.",
		"Rs2GameObject.",
		"invokeMenuAction(",
		"Microbot.getClient("
	);

	private static final List<String> SHORTEST_PATH_UI_FILES = Arrays.asList(
		"ShortestPathPlugin.java",
		"ShortestPathPanel.java",
		"NavigationPresentation.java",
		"PathTileOverlay.java",
		"PathMapOverlay.java",
		"PathMapTooltipOverlay.java",
		"PathMinimapOverlay.java",
		"ETAOverlayPanel.java",
		"DebugOverlayPanel.java"
	);

	@Test
	public void mutableShortestPathPluginStateStaysBehindFacade() throws IOException
	{
		Path microbotRoot = microbotSourceRoot();
		List<String> violations = new ArrayList<>();
		for (Path source : javaSources(microbotRoot))
		{
			String relative = relative(microbotRoot, source);
			if (relative.startsWith("shortestpath/") || PATH_API_RELATIVE.equals(relative))
			{
				continue;
			}
			String code = codeOnly(source);
			collectMatches(relative, code, SHORTEST_PATH_MUTABLE_ACCESS, violations);
		}
		assertNoViolations("Mutable ShortestPathPlugin state must stay behind Rs2PathApi", violations);
	}

	@Test
	public void interactionHandlersCannotOwnRouteLifecycle() throws IOException
	{
		Path microbotRoot = microbotSourceRoot();
		Path walkerRoot = microbotRoot.resolve("util/walker");
		List<String> violations = new ArrayList<>();
		for (String child : Arrays.asList("door", "obstacle", "transport", "interaction"))
		{
			Path root = walkerRoot.resolve(child);
			if (!Files.isDirectory(root))
			{
				continue;
			}
			for (Path source : javaSources(root))
			{
				String relative = relative(microbotRoot, source);
				collectMatches(relative, codeOnly(source), FORBIDDEN_HANDLER_LIFECYCLE_ACCESS, violations);
			}
		}
		assertNoViolations("Interaction handlers report outcomes; NavigationEngine owns lifecycle", violations);
	}

	@Test
	public void retiredWalkerOrchestrationCannotReturn() throws IOException
	{
		Path microbotRoot = microbotSourceRoot();
		Path walker = microbotSourceRoot().resolve(WALKER_RELATIVE);
		String walkerCode = codeOnly(walker);
		assertFalse("The retired processWalk executor must not return", walkerCode.contains("processWalk("));

		Path navigationRoot = microbotRoot.resolve("util/walker/navigation");
		List<String> violations = new ArrayList<>();
		for (Path source : javaSources(navigationRoot))
		{
			collectMatches(relative(navigationRoot, source), codeOnly(source), Arrays.asList(
				"NavigationExecutionMode",
				"NavigationComparison",
				"ordinaryEngineEnabled",
				"isOrdinaryEngineEnabled",
				"ignoredLegacyDecision",
				"finishFromLegacy"), violations);
		}
		assertNoViolations("Retired dual-executor compatibility state must not return", violations);
		assertTrue("NavigationExecutionMode must remain deleted",
			Files.notExists(navigationRoot.resolve("NavigationExecutionMode.java")));
		assertTrue("NavigationComparison must remain deleted",
			Files.notExists(navigationRoot.resolve("NavigationComparison.java")));
	}

	@Test
	public void routePlannerRuntimeIsTheOnlyPathfinderLifecycleOwner() throws IOException
	{
		Path microbotRoot = microbotSourceRoot();
		List<String> violations = new ArrayList<>();
		for (Path source : javaSources(microbotRoot))
		{
			String relative = relative(microbotRoot, source);
			String code = codeOnly(source);
			collectMatches(relative, code, REMOVED_LIFECYCLE_OWNERS, violations);
			if (!relative.startsWith("util/walker/navigation/") && code.contains("new RoutePlanner("))
			{
				violations.add(relative + " -> new RoutePlanner(");
			}
		}
		assertNoViolations("RoutePlannerRuntime exclusively owns calculation tasks", violations);
	}

	@Test
	public void navigationEngineCannotBypassWalkerActions() throws IOException
	{
		Path navigationRoot = microbotSourceRoot().resolve("util/walker/navigation");
		List<String> violations = new ArrayList<>();
		for (String file : Arrays.asList("NavigationEngine.java", "NavigationEngineRuntime.java"))
		{
			Path source = navigationRoot.resolve(file);
			collectMatches(file, codeOnly(source), NAVIGATION_ENGINE_INPUT_ACCESS, violations);
		}
		assertNoViolations("NavigationEngine must send Phase 3 input only through WalkerActions", violations);
	}

	@Test
	public void pathfinderConfigurationCannotIssueInventoryOrEquipmentInput() throws IOException
	{
		Path config = microbotSourceRoot().resolve("shortestpath/pathfinder/PathfinderConfig.java");
		String code = codeOnly(config);
		List<String> violations = new ArrayList<>();
		collectMatches("PathfinderConfig.java", code,
			Arrays.asList("Rs2Inventory.interact(", "Rs2Equipment.interact("), violations);
		assertNoViolations("Route calculation must remain read-only", violations);
	}

	@Test
	public void shortestPathUiCannotReachLegacyWalkerImplementation() throws IOException
	{
		Path uiRoot = microbotSourceRoot().resolve("shortestpath");
		List<String> violations = new ArrayList<>();
		for (String file : SHORTEST_PATH_UI_FILES)
		{
			collectMatches(file, codeOnly(uiRoot.resolve(file)),
				Arrays.asList("Rs2Walker.", "ShortestPathScript"), violations);
		}
		assertNoViolations("Shortest-path UI must publish intent through NavigationEngine", violations);
	}

	@Test
	public void navigationCoreCannotDependOnShortestPathUi() throws IOException
	{
		Path navigationRoot = microbotSourceRoot().resolve("util/walker/navigation");
		List<String> violations = new ArrayList<>();
		for (Path source : javaSources(navigationRoot))
		{
			collectMatches(relative(navigationRoot, source), codeOnly(source), Arrays.asList(
				"shortestpath.ShortestPathPlugin",
				"shortestpath.ShortestPathPanel",
				"shortestpath.ShortestPathConfig",
				"ui.overlay."), violations);
		}
		assertNoViolations("Navigation core must not depend on shortest-path UI", violations);
	}

	@Test
	public void shortestPathPluginCannotOwnNavigationLifecycleFields() throws IOException
	{
		Path plugin = microbotSourceRoot().resolve("shortestpath/ShortestPathPlugin.java");
		String code = codeOnly(plugin);
		List<String> violations = new ArrayList<>();
		collectMatches("ShortestPathPlugin.java", code, Arrays.asList(
			"private static Pathfinder ",
			"private static Future<",
			"private static ExecutorService ",
			"private static WorldPoint target",
			"private static WorldMapPoint marker"), violations);
		assertNoViolations("ShortestPathPlugin cannot own navigation lifecycle state", violations);
	}

	@Test
	public void legacyShortestPathScriptIsRetired()
	{
		Path script = microbotSourceRoot().resolve("shortestpath/ShortestPathScript.java");
		assertTrue("ShortestPathScript retry and terminal policy must stay retired",
			Files.notExists(script));
	}

	private static void collectMatches(String relative, String code, List<String> forbidden,
		List<String> violations)
	{
		for (String token : forbidden)
		{
			if (code.contains(token))
			{
				violations.add(relative + " -> " + token);
			}
		}
	}

	private static void assertNoViolations(String message, List<String> violations)
	{
		if (!violations.isEmpty())
		{
			fail(message + ":\n  " + String.join("\n  ", violations));
		}
	}

	private static String codeOnly(Path source) throws IOException
	{
		return blankNonCode(new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
	}

	/** Replaces comments and literals with spaces while preserving indices and line numbers. */
	private static String blankNonCode(String source)
	{
		Matcher matcher = NON_CODE.matcher(source);
		StringBuffer result = new StringBuffer(source.length());
		while (matcher.find())
		{
			StringBuilder blank = new StringBuilder(matcher.group().length());
			for (int i = 0; i < matcher.group().length(); i++)
			{
				char c = matcher.group().charAt(i);
				blank.append(c == '\n' || c == '\r' ? c : ' ');
			}
			matcher.appendReplacement(result, Matcher.quoteReplacement(blank.toString()));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private static List<Path> javaSources(Path root) throws IOException
	{
		try (Stream<Path> stream = Files.walk(root))
		{
			return stream.filter(path -> path.toString().endsWith(".java"))
				.collect(Collectors.toList());
		}
	}

	private static String relative(Path root, Path path)
	{
		return root.relativize(path).toString().replace('\\', '/');
	}

	private static Path microbotSourceRoot()
	{
		Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		while (current != null)
		{
			Path candidate = current.resolve(
				"runelite-client/src/main/java/net/runelite/client/plugins/microbot");
			if (Files.isDirectory(candidate))
			{
				return candidate;
			}
			current = current.getParent();
		}
		throw new AssertionError("Unable to locate Microbot source root from " + System.getProperty("user.dir"));
	}
}
