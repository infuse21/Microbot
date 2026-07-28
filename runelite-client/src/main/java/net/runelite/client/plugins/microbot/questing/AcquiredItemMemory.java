package net.runelite.client.plugins.microbot.questing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Remembers, per quest, which items we have already obtained — so a consumable the quest has used up
 * is never bought again.
 *
 * <p>Observation alone isn't enough: it lives in the script instance, so a client restart forgets that
 * the 3 balls of wool were ever held, the quest still lists them, and the executor goes shopping for
 * them again. This store survives restarts.
 *
 * <p>Deliberately blunt: once an item has been obtained for a quest it is not obtained again for that
 * quest. Under-buying is recoverable (hand the item over, or delete the row); a re-buy loop burns gp
 * unattended. Entries for a quest are cleared when it is restarted from NOT_STARTED, so replays work.
 *
 * <p>Human-editable TSV under {@code <runelite>/microbot/}, one row per quest.
 */
@Slf4j
public final class AcquiredItemMemory {
	private static final String DELIM = "\t";
	private static final String COMMENT = "#";
	private static final String HEADER = "# questId\titemIds  (items already obtained for this quest — safe to edit)";

	private static final Map<Integer, Set<Integer>> BY_QUEST = new LinkedHashMap<>();
	private static volatile boolean loaded = false;
	private static File file;

	private AcquiredItemMemory() {
	}

	public static File defaultFile() {
		return new File(new File(RuneLite.RUNELITE_DIR, "microbot"), "questing-acquired.tsv");
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
					if (f.length < 2) {
						continue;
					}
					try {
						int questId = Integer.parseInt(f[0].trim());
						Set<Integer> ids = Arrays.stream(f[1].split(","))
								.map(String::trim).filter(t -> !t.isEmpty())
								.map(Integer::parseInt).collect(Collectors.toCollection(HashSet::new));
						BY_QUEST.put(questId, ids);
					} catch (NumberFormatException ignored) {
					}
				}
			}
		} catch (IOException e) {
			log.warn("[AcquiredItemMemory] unable to read {}: {}", file, e.getMessage());
		}
	}

	/** Item ids already obtained for this quest. */
	public static synchronized Set<Integer> forQuest(int questId) {
		ensureLoaded();
		return new HashSet<>(BY_QUEST.getOrDefault(questId, java.util.Collections.emptySet()));
	}

	/** Records that an item has been obtained for this quest. */
	public static synchronized void record(int questId, int itemId) {
		ensureLoaded();
		if (BY_QUEST.computeIfAbsent(questId, k -> new HashSet<>()).add(itemId)) {
			save();
		}
	}

	/** Forgets everything for a quest (used when it is restarted from scratch). */
	public static synchronized void clearQuest(int questId) {
		ensureLoaded();
		if (BY_QUEST.remove(questId) != null) {
			save();
		}
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
			for (Map.Entry<Integer, Set<Integer>> e : BY_QUEST.entrySet()) {
				lines.add(e.getKey() + DELIM
						+ e.getValue().stream().map(String::valueOf).collect(Collectors.joining(",")));
			}
			Files.write(file.toPath(), String.join(System.lineSeparator(), lines).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			log.warn("[AcquiredItemMemory] unable to write {}: {}", file, e.getMessage());
		}
	}
}
