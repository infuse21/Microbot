package net.runelite.client.plugins.microbot.questhelper.tools;

import net.runelite.cache.DBRowManager;
import net.runelite.cache.definitions.DBRowDefinition;
import net.runelite.cache.fs.Store;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Diffs the quest helper's hardcoded requirements against Jagex's own Quest table in the game cache.
 *
 * <p>The cache ships {@code DBTableID.Quest} — around 214 rows of first-party quest metadata that
 * updates itself with every game update. The helper hand-maintains the same information in Java, so
 * the two can fall out of step: a stale requirement either blocks a player who could start the quest,
 * or lets one start who cannot finish it.
 *
 * <p><b>Reading the output.</b> Jagex's table records only what the game gates <i>entry</i> on. The
 * helper legitimately lists more — skills needed to <i>finish</i>, and transitive prerequisites — so
 * the two directions mean very different things:
 * <ul>
 *   <li>{@code only in jagex} — the helper is <b>missing</b> something the game enforces. Investigate.</li>
 *   <li>{@code only in helper} — usually deliberate and correct. Verify before "fixing".</li>
 * </ul>
 *
 * <p>Worked example: Forgettable Tale requires 22 Cooking in the helper where Jagex says 20. That is
 * not a bug — the entry gate really is 20, but 22 is needed mid-quest to make the kelda stout.
 * Lowering it would let players start a quest they cannot finish. Always check the wiki quest page
 * before changing a level.
 *
 * <p>Run with {@code ./gradlew :client:auditQuestRequirements}. See
 * {@code docs/quest-requirements-audit.md}.
 */
public final class QuestRequirementAudit
{
	/** RuneScript skill ids, in order. Sailing is 23. */
	private static final String[] SKILLS = {
		"ATTACK", "DEFENCE", "STRENGTH", "HITPOINTS", "RANGED", "PRAYER", "MAGIC", "COOKING",
		"WOODCUTTING", "FLETCHING", "FISHING", "FIREMAKING", "CRAFTING", "SMITHING", "MINING",
		"HERBLORE", "AGILITY", "THIEVING", "SLAYER", "FARMING", "RUNECRAFT", "HUNTER",
		"CONSTRUCTION", "SAILING"
	};

	private static final int QUEST_TABLE = 0;
	private static final int COL_DISPLAYNAME = 2;
	private static final int COL_REQUIREMENT_STATS = 23;
	private static final int COL_REQUIREMENT_QUESTS = 25;
	private static final int COL_REQUIREMENT_QUESTPOINTS = 26;
	private static final int COL_REQUIREMENT_COMBAT = 27;

	private static final Pattern SKILL_REQ =
		Pattern.compile("new\\s+SkillRequirement\\s*\\(\\s*Skill\\.([A-Z_]+)\\s*,\\s*(\\d+)");
	private static final Pattern QUEST_REQ =
		Pattern.compile("new\\s+QuestRequirement\\s*\\(\\s*QuestHelperQuest\\.([A-Z0-9_]+)");
	private static final Pattern COMBAT_REQ =
		Pattern.compile("new\\s+CombatLevelRequirement\\s*\\(\\s*(\\d+)");
	private static final Pattern QP_REQ =
		Pattern.compile("new\\s+QuestPointRequirement\\s*\\(\\s*(\\d+)");
	/** Requirements are often built into a field and only referenced from getGeneralRequirements(). */
	private static final Pattern FIELD_ASSIGN = Pattern.compile(
		"(\\w+)\\s*=\\s*new\\s+(SkillRequirement|QuestRequirement|CombatLevelRequirement"
			+ "|QuestPointRequirement)\\s*\\(([^;]*?)\\)\\s*;", Pattern.DOTALL);
	private static final Pattern IDENTIFIER = Pattern.compile("\\b([a-z]\\w+)\\b");
	private static final Pattern ENUM_ENTRY =
		Pattern.compile("^\\t([A-Z0-9_]+)\\(\\s*(.*?)\\)\\s*,\\s*$", Pattern.MULTILINE | Pattern.DOTALL);
	private static final Pattern API_QUEST_ENTRY =
		Pattern.compile("([A-Z0-9_]+)\\(\\s*\\d+\\s*,\\s*\"([^\"]+)\"");

	/** One quest's requirements, from either side. */
	private static final class Reqs
	{
		final String name;
		final Set<String> skills = new TreeSet<>();
		final Set<String> quests = new TreeSet<>();
		Integer combat;
		Integer questPoints;
		String file = "";

		Reqs(String name)
		{
			this.name = name;
		}
	}

	private QuestRequirementAudit()
	{
	}

	public static void main(String[] args) throws IOException
	{
		Path cache = args.length > 0 && !args[0].isEmpty()
			? Paths.get(args[0])
			: Paths.get(System.getProperty("user.home"), ".runelite", "jagexcache", "oldschool", "LIVE");
		Path repo = Paths.get(args.length > 1 && !args[1].isEmpty() ? args[1] : ".").toAbsolutePath().normalize();

		if (!Files.isRegularFile(cache.resolve("main_file_cache.dat2")))
		{
			System.err.println("No game cache at " + cache);
			System.err.println("Run the client once, or pass -PcachePath=<dir>.");
			System.exit(2);
		}

		Map<String, Reqs> jagex = readJagex(cache);
		Map<String, Reqs> helper = readHelper(repo);
		report(jagex, helper);
	}

	// ------------------------------------------------------------------ Jagex side

	private static Map<String, Reqs> readJagex(Path cache) throws IOException
	{
		Map<Integer, String> rowNames = new HashMap<>();
		Map<Integer, Object[][]> rowValues = new LinkedHashMap<>();

		try (Store store = new Store(cache.toFile()))
		{
			store.load();
			DBRowManager rows = new DBRowManager(store);
			rows.load();
			for (DBRowDefinition row : rows.getRows())
			{
				if (row.getTableId() != QUEST_TABLE)
				{
					continue;
				}
				Object[][] values = row.getColumnValues();
				String name = firstString(values, COL_DISPLAYNAME);
				if (name == null)
				{
					continue;
				}
				rowNames.put(row.getId(), name);
				rowValues.put(row.getId(), values);
			}
		}

		Map<String, Reqs> out = new TreeMap<>();
		for (Map.Entry<Integer, Object[][]> e : rowValues.entrySet())
		{
			Object[][] values = e.getValue();
			Reqs r = new Reqs(rowNames.get(e.getKey()));

			Object[] stats = column(values, COL_REQUIREMENT_STATS);
			if (stats != null)
			{
				for (int i = 0; i + 1 < stats.length; i += 2)
				{
					int id = asInt(stats[i], -1);
					int level = asInt(stats[i + 1], -1);
					if (id >= 0 && id < SKILLS.length && level > 0)
					{
						r.skills.add(SKILLS[id] + " " + level);
					}
				}
			}

			Object[] prereqs = column(values, COL_REQUIREMENT_QUESTS);
			if (prereqs != null)
			{
				for (Object o : prereqs)
				{
					String n = rowNames.get(asInt(o, -1));
					if (n != null)
					{
						r.quests.add(normalise(n));
					}
				}
			}

			r.combat = firstInt(values, COL_REQUIREMENT_COMBAT);
			r.questPoints = firstInt(values, COL_REQUIREMENT_QUESTPOINTS);
			out.put(normalise(r.name), r);
		}
		return out;
	}

	// ------------------------------------------------------------------ helper side

	private static Map<String, Reqs> readHelper(Path repo) throws IOException
	{
		Path base = repo.resolve("runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper");
		String enumSrc = read(base.resolve("questinfo/QuestHelperQuest.java"));
		String apiSrc = read(repo.resolve("runelite-api/src/main/java/net/runelite/api/Quest.java"));

		Map<String, String> apiNames = new HashMap<>();
		Matcher am = API_QUEST_ENTRY.matcher(apiSrc);
		while (am.find())
		{
			apiNames.putIfAbsent(am.group(1), am.group(2));
		}

		// enum constant -> display name, and helper class -> enum constant
		Map<String, String> enumDisplay = new HashMap<>();
		Map<String, String> classToEnum = new HashMap<>();
		int bodyStart = Math.max(0, enumSrc.indexOf("public enum QuestHelperQuest"));
		Matcher em = ENUM_ENTRY.matcher(enumSrc.substring(bodyStart));
		while (em.find())
		{
			String constant = em.group(1);
			String argsText = em.group(2);

			Matcher cls = Pattern.compile("new\\s+([A-Za-z0-9_]+)\\s*\\(").matcher(argsText);
			if (cls.find())
			{
				classToEnum.put(cls.group(1), constant);
			}

			Matcher lit = Pattern.compile("\"([^\"]{3,60})\"").matcher(argsText);
			Matcher ref = Pattern.compile("\\bQuest\\.([A-Z0-9_]+)").matcher(argsText);
			if (lit.find())
			{
				enumDisplay.put(constant, lit.group(1));
			}
			else if (ref.find() && apiNames.containsKey(ref.group(1)))
			{
				enumDisplay.put(constant, apiNames.get(ref.group(1)));
			}
		}

		Path questDir = base.resolve("helpers/quests");
		Map<String, Reqs> out = new TreeMap<>();
		try (Stream<Path> files = Files.walk(questDir))
		{
			List<Path> javaFiles = new ArrayList<>();
			files.filter(p -> p.toString().endsWith(".java")).forEach(javaFiles::add);

			for (Path f : javaFiles)
			{
				String cls = f.getFileName().toString().replace(".java", "");
				String constant = classToEnum.get(cls);
				String display = constant == null ? null : enumDisplay.get(constant);
				if (display == null)
				{
					continue;
				}
				Reqs r = parseQuest(read(f), display, enumDisplay);
				r.file = repo.relativize(f).toString().replace('\\', '/');
				out.put(normalise(display), r);
			}
		}
		return out;
	}

	private static Reqs parseQuest(String src, String display, Map<String, String> enumDisplay)
	{
		Reqs r = new Reqs(display);
		String block = methodBody(src, "getGeneralRequirements");
		if (block.isEmpty())
		{
			return r;
		}

		collectInline(block, r, enumDisplay);

		// Resolve bare field references, e.g. `return Arrays.asList(sailingSkillRequirement, ...)`.
		Map<String, String> fields = new HashMap<>();
		Matcher fa = FIELD_ASSIGN.matcher(src);
		while (fa.find())
		{
			fields.put(fa.group(1), fa.group(2) + "|" + fa.group(3));
		}

		Matcher ids = IDENTIFIER.matcher(block);
		while (ids.find())
		{
			String assigned = fields.get(ids.group(1));
			if (assigned == null)
			{
				continue;
			}
			int bar = assigned.indexOf('|');
			applyField(assigned.substring(0, bar), assigned.substring(bar + 1), r, enumDisplay);
		}
		return r;
	}

	private static void collectInline(String block, Reqs r, Map<String, String> enumDisplay)
	{
		Matcher m = SKILL_REQ.matcher(block);
		while (m.find())
		{
			r.skills.add(m.group(1) + " " + Integer.parseInt(m.group(2)));
		}
		m = QUEST_REQ.matcher(block);
		while (m.find())
		{
			String d = enumDisplay.get(m.group(1));
			if (d != null)
			{
				r.quests.add(normalise(d));
			}
		}
		m = COMBAT_REQ.matcher(block);
		if (m.find())
		{
			r.combat = Integer.parseInt(m.group(1));
		}
		m = QP_REQ.matcher(block);
		if (m.find())
		{
			r.questPoints = Integer.parseInt(m.group(1));
		}
	}

	private static void applyField(String kind, String args, Reqs r, Map<String, String> enumDisplay)
	{
		if ("SkillRequirement".equals(kind))
		{
			Matcher m = Pattern.compile("Skill\\.([A-Z_]+)\\s*,\\s*(\\d+)").matcher(args);
			if (m.find())
			{
				r.skills.add(m.group(1) + " " + Integer.parseInt(m.group(2)));
			}
		}
		else if ("QuestRequirement".equals(kind))
		{
			Matcher m = Pattern.compile("QuestHelperQuest\\.([A-Z0-9_]+)").matcher(args);
			if (m.find())
			{
				String d = enumDisplay.get(m.group(1));
				if (d != null)
				{
					r.quests.add(normalise(d));
				}
			}
		}
		else
		{
			Matcher m = Pattern.compile("(\\d+)").matcher(args);
			if (m.find())
			{
				int v = Integer.parseInt(m.group(1));
				if ("CombatLevelRequirement".equals(kind))
				{
					r.combat = v;
				}
				else
				{
					r.questPoints = v;
				}
			}
		}
	}

	// ------------------------------------------------------------------ diff

	private static void report(Map<String, Reqs> jagex, Map<String, Reqs> helper)
	{
		int matched = 0;
		int identical = 0;
		int missingInHelper = 0;
		StringBuilder body = new StringBuilder();

		for (Map.Entry<String, Reqs> e : helper.entrySet())
		{
			Reqs h = e.getValue();
			Reqs j = jagex.get(e.getKey());
			if (j == null)
			{
				continue;
			}
			matched++;

			Set<String> skillsOnlyHelper = minus(h.skills, j.skills);
			Set<String> skillsOnlyJagex = minus(j.skills, h.skills);
			Set<String> questsOnlyHelper = minus(h.quests, j.quests);
			Set<String> questsOnlyJagex = minus(j.quests, h.quests);
			boolean combatDiffers = zero(h.combat) != zero(j.combat);
			boolean qpDiffers = zero(h.questPoints) != zero(j.questPoints);

			if (skillsOnlyHelper.isEmpty() && skillsOnlyJagex.isEmpty() && questsOnlyHelper.isEmpty()
				&& questsOnlyJagex.isEmpty() && !combatDiffers && !qpDiffers)
			{
				identical++;
				continue;
			}
			if (!skillsOnlyJagex.isEmpty() || !questsOnlyJagex.isEmpty())
			{
				missingInHelper++;
			}

			body.append('\n').append(h.name).append("  (").append(h.file).append(")\n");
			line(body, "  MISSING - jagex requires", skillsOnlyJagex, questsOnlyJagex, jagex);
			line(body, "  extra in helper", skillsOnlyHelper, questsOnlyHelper, jagex);
			if (combatDiffers)
			{
				body.append("  combat level: helper=").append(h.combat).append(" jagex=").append(j.combat).append('\n');
			}
			if (qpDiffers)
			{
				body.append("  quest points: helper=").append(h.questPoints)
					.append(" jagex=").append(j.questPoints).append('\n');
			}
		}

		System.out.println("Quest requirement audit (helper vs Jagex's cache table)");
		System.out.println("  jagex quest rows      : " + jagex.size());
		System.out.println("  helper quests parsed  : " + helper.size());
		System.out.println("  matched by name       : " + matched);
		System.out.println("  identical             : " + identical);
		System.out.println("  differing             : " + (matched - identical));
		System.out.println("  ...missing something jagex enforces: " + missingInHelper);
		System.out.println();
		System.out.println("'MISSING' means the helper omits something the game gates entry on - investigate.");
		System.out.println("'extra in helper' is usually deliberate (completion-only skills, transitive");
		System.out.println("prerequisites) - verify against the wiki before changing anything.");
		System.out.println(body);
	}

	private static void line(StringBuilder sb, String label, Set<String> skills, Set<String> quests,
							 Map<String, Reqs> jagex)
	{
		if (skills.isEmpty() && quests.isEmpty())
		{
			return;
		}
		sb.append(label).append(':');
		for (String s : skills)
		{
			sb.append(' ').append(s).append(';');
		}
		for (String q : quests)
		{
			Reqs r = jagex.get(q);
			sb.append(' ').append(r != null ? r.name : q).append(';');
		}
		sb.append('\n');
	}

	// ------------------------------------------------------------------ helpers

	private static Set<String> minus(Set<String> a, Set<String> b)
	{
		Set<String> out = new TreeSet<>(a);
		out.removeAll(b);
		return out;
	}

	private static int zero(Integer i)
	{
		return i == null ? 0 : i;
	}

	private static String normalise(String s)
	{
		return s.toLowerCase().replace("&", "and").replaceAll("[^a-z0-9]", "");
	}

	private static String read(Path p) throws IOException
	{
		return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
	}

	/** The body of a method, brace-matched from its declaration. */
	private static String methodBody(String src, String name)
	{
		int i = src.indexOf(name);
		if (i < 0)
		{
			return "";
		}
		int open = src.indexOf('{', i);
		if (open < 0)
		{
			return "";
		}
		int depth = 0;
		for (int k = open; k < src.length(); k++)
		{
			char c = src.charAt(k);
			if (c == '{')
			{
				depth++;
			}
			else if (c == '}')
			{
				depth--;
				if (depth == 0)
				{
					return src.substring(open, k);
				}
			}
		}
		return "";
	}

	private static Object[] column(Object[][] values, int col)
	{
		return values == null || col >= values.length ? null : values[col];
	}

	private static String firstString(Object[][] values, int col)
	{
		Object[] c = column(values, col);
		return c == null || c.length == 0 || c[0] == null ? null : c[0].toString();
	}

	private static Integer firstInt(Object[][] values, int col)
	{
		Object[] c = column(values, col);
		if (c == null || c.length == 0 || !(c[0] instanceof Number))
		{
			return null;
		}
		return ((Number) c[0]).intValue();
	}

	private static int asInt(Object o, int fallback)
	{
		return o instanceof Number ? ((Number) o).intValue() : fallback;
	}
}
