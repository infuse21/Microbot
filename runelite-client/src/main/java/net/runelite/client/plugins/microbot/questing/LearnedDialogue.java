package net.runelite.client.plugins.microbot.questing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Remembers which dialogue option actually advanced a quest, so a menu the quest data doesn't know
 * about only has to be worked out once.
 *
 * <p>Not machine learning — a memoised feedback loop. An entry is keyed by the quest plus the exact
 * SET of options offered, which gives self-invalidation for free: if Jagex rewords a menu the key no
 * longer matches, the stale entry is simply never consulted again, and the new wording is learned
 * afresh. The chosen option is stored as TEXT, not an index, because indexes shuffle between visits.
 *
 * <p>The file is a human-editable TSV under {@code <runelite>/microbot/}, so learned answers are
 * inspectable, diffable, portable between installs, and can be shipped as a starter set.
 *
 * <p><b>Safety.</b> Guessing is never allowed to be reckless: quests whose dialogue makes permanent,
 * irreversible account decisions are excluded entirely (see {@link #GUESS_BLOCKED_QUEST_IDS}), and
 * options whose text suggests a destructive or committing action are never auto-picked. Only picks
 * that demonstrably advanced the quest are ever recorded.
 */
@Slf4j
public final class LearnedDialogue {
	private static final String DELIM = "\t";
	private static final String COMMENT = "#";
	private static final String HEADER =
			"# questId\toptionsKey\tchosen\toptions\tnegatives  (learned dialogue answers — safe to edit or share)";

	/** One learned decision point. */
	public static final class Entry {
		public final int questId;
		public final String optionsKey;
		public String chosen;
		public final String optionsDisplay;
		public final Set<String> negatives = new HashSet<>();

		Entry(int questId, String optionsKey, String chosen, String optionsDisplay) {
			this.questId = questId;
			this.optionsKey = optionsKey;
			this.chosen = chosen;
			this.optionsDisplay = optionsDisplay;
		}
	}

	/**
	 * Quests where a dialogue choice permanently changes the account and can never be undone, so a
	 * wrong guess is unacceptable: gang choice (Shield of Arrav / Hero's Quest), missable content
	 * (A Kingdom Divided, Cold War), item deletion (Regicide), jail/damage/teleport punishments
	 * (The Tourist Trap, Underground Pass, Contact!). In these, an unmatched menu stops and asks for a
	 * human instead of experimenting.
	 */
	private static final Set<Integer> GUESS_BLOCKED_QUEST_IDS = new HashSet<>(Arrays.asList(
			net.runelite.api.Quest.SHIELD_OF_ARRAV.getId(),
			net.runelite.api.Quest.HEROES_QUEST.getId(),
			net.runelite.api.Quest.A_KINGDOM_DIVIDED.getId(),
			net.runelite.api.Quest.COLD_WAR.getId(),
			net.runelite.api.Quest.REGICIDE.getId(),
			net.runelite.api.Quest.THE_TOURIST_TRAP.getId(),
			net.runelite.api.Quest.UNDERGROUND_PASS.getId(),
			net.runelite.api.Quest.CONTACT.getId()
	));

	/**
	 * Option text that must never be auto-picked: starting fights, handing things over, destroying or
	 * selling items, or committing to a faction. Matched on the normalised text as whole words.
	 */
	private static final List<String> DANGEROUS_OPTION_WORDS = Arrays.asList(
			"attack", "fight", "kill", "challenge", "duel", "steal",
			"destroy", "drop", "sell", "give you", "hand over", "delete",
			"phoenix", "black arm", "join the", "i accept", "sacrifice"
	);

	private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
	private static volatile boolean loaded = false;
	private static File file;

	private LearnedDialogue() {
	}

	public static File defaultFile() {
		return new File(new File(RuneLite.RUNELITE_DIR, "microbot"), "learned-dialogue.tsv");
	}

	private static synchronized void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		if (file == null) {
			file = defaultFile();
		}
		if (!file.isFile()) {
			return;
		}
		try {
			String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			try (Scanner scanner = new Scanner(content)) {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.startsWith(COMMENT) || line.isBlank()) {
						continue;
					}
					String[] f = line.split(DELIM, -1);
					if (f.length < 3) {
						continue;
					}
					try {
						int questId = Integer.parseInt(f[0].trim());
						Entry e = new Entry(questId, f[1].trim(), f[2].trim(), f.length > 3 ? f[3] : "");
						if (f.length > 4 && !f[4].isBlank()) {
							e.negatives.addAll(Arrays.stream(f[4].split("\\|"))
									.map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
						}
						ENTRIES.put(mapKey(questId, e.optionsKey), e);
					} catch (NumberFormatException ignored) {
					}
				}
			}
			log.debug("[LearnedDialogue] loaded {} entries from {}", ENTRIES.size(), file);
		} catch (IOException e) {
			log.warn("[LearnedDialogue] unable to read {}: {}", file, e.getMessage());
		}
	}

	/** Stable identity for a decision point: the option set, normalised and order-independent. */
	public static String optionsKey(List<String> options) {
		List<String> normalised = options.stream()
				.map(LearnedDialogue::normalise)
				.filter(s -> !s.isEmpty())
				.sorted()
				.collect(Collectors.toList());
		return Integer.toHexString(String.join("", normalised).hashCode());
	}

	static String normalise(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("<[^>]*>", " ")
				.toLowerCase()
				.replaceAll("[^a-z0-9 ]", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private static String mapKey(int questId, String optionsKey) {
		return questId + "|" + optionsKey;
	}

	/** The option text previously confirmed for this menu, or null. */
	public static String recall(int questId, List<String> options) {
		ensureLoaded();
		Entry e = ENTRIES.get(mapKey(questId, optionsKey(options)));
		return e == null ? null : e.chosen;
	}

	/** Options already known to be wrong for this menu. */
	public static Set<String> negatives(int questId, List<String> options) {
		ensureLoaded();
		Entry e = ENTRIES.get(mapKey(questId, optionsKey(options)));
		return e == null ? Collections.emptySet() : e.negatives;
	}

	/** Whether guessing is permitted at all for this quest (see {@link #GUESS_BLOCKED_QUEST_IDS}). */
	public static boolean guessingAllowed(int questId) {
		return !GUESS_BLOCKED_QUEST_IDS.contains(questId);
	}

	/** Whether this option is too consequential to pick without being told to. */
	public static boolean isDangerousOption(String optionText) {
		String n = normalise(optionText);
		for (String word : DANGEROUS_OPTION_WORDS) {
			if (n.equals(word) || n.startsWith(word + " ") || n.contains(" " + word + " ") || n.endsWith(" " + word)) {
				return true;
			}
		}
		return false;
	}

	/** Records a confirmed-good answer (the pick demonstrably advanced the quest) and persists it. */
	public static synchronized void confirm(int questId, List<String> options, String chosen) {
		ensureLoaded();
		String key = optionsKey(options);
		Entry e = ENTRIES.computeIfAbsent(mapKey(questId, key),
				k -> new Entry(questId, key, chosen, String.join(" | ", options)));
		e.chosen = chosen;
		e.negatives.remove(chosen);
		save();
		log.info("[LearnedDialogue] learned quest {} -> \"{}\"", questId, chosen);
	}

	/** Records that an answer did NOT advance the quest, so it isn't tried again. */
	public static synchronized void reject(int questId, List<String> options, String chosen) {
		ensureLoaded();
		String key = optionsKey(options);
		Entry e = ENTRIES.computeIfAbsent(mapKey(questId, key),
				k -> new Entry(questId, key, "", String.join(" | ", options)));
		e.negatives.add(chosen);
		if (chosen.equals(e.chosen)) {
			e.chosen = ""; // it stopped working — forget it and re-learn
		}
		save();
	}

	private static void save() {
		if (file == null) {
			file = defaultFile();
		}
		try {
			File parent = file.getParentFile();
			if (parent != null && !parent.isDirectory()) {
				Files.createDirectories(parent.toPath());
			}
			List<String> lines = new ArrayList<>();
			lines.add(HEADER);
			for (Entry e : ENTRIES.values()) {
				lines.add(String.join(DELIM,
						Integer.toString(e.questId),
						e.optionsKey,
						e.chosen == null ? "" : e.chosen,
						e.optionsDisplay == null ? "" : e.optionsDisplay,
						String.join("|", e.negatives)));
			}
			Files.write(file.toPath(), String.join(System.lineSeparator(), lines).getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch (IOException e) {
			log.warn("[LearnedDialogue] unable to write {}: {}", file, e.getMessage());
		}
	}
}
