package net.runelite.client.plugins.microbot.questing;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The real dialogue options every quest actually offers, scraped from the Old School RuneScape
 * Wiki's transcript namespace.
 *
 * <p><b>Why this exists.</b> Quest authors attach answers to the step where they expect a menu, but
 * measured against the real corpus only about two thirds of in-game menus have a declared answer at
 * all. For the rest the bot used to fall through to "click the first option that doesn't look
 * dangerous" — which on a shopkeeper is usually the one that opens the shop. Roughly one menu in
 * three was a guess, and 142 of those menus had a non-quest option sitting in first place.
 *
 * <p><b>What the data gives us.</b> The wiki's transcript style guide mandates
 * {@code {{topt|...}}} for a selectable option and {@code {{topt|quest=no|...}}} for one that is not
 * part of the quest, so the corpus carries both halves of the answer:
 * <ul>
 *   <li>{@code Q} — an option that belongs to the quest.</li>
 *   <li>{@code N} — an option explicitly marked as not quest related. Never auto-picked.</li>
 *   <li>{@code M} — a whole menu that has exactly <b>one</b> quest option, mapped to that answer.
 *       Menus with two or more quest options are omitted on purpose: those are real branches
 *       (which gang, which reward) and must not be decided automatically.</li>
 * </ul>
 *
 * <p>This is the authority on what a menu offers. A documented answer for the exact menu on screen
 * outranks the quest's own declared choices, whose vocabulary spans every step and can match a string
 * authored for a different conversation.
 *
 * <p>Data: {@code quest-dialogue.tsv}, CC BY-NC-SA 3.0, from oldschool.runescape.wiki.
 * Regenerate per {@code docs/quest-dialogue-corpus.md} after a game update rewords dialogue.
 */
@Slf4j
public final class QuestDialogueCorpus {
	private static final String RESOURCE = "quest-dialogue.tsv";

	/** quest -> normalised option -> is it part of the quest. */
	private static final Map<String, Map<String, Boolean>> OPTIONS = new HashMap<>();
	/** quest -> normalised sorted option set -> the single quest answer. */
	private static final Map<String, Map<String, String>> MENUS = new HashMap<>();

	private static volatile boolean loaded;

	private QuestDialogueCorpus() {
	}

	private static synchronized void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		try (InputStream in = QuestDialogueCorpus.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				log.warn("[QuestDialogueCorpus] {} missing from the classpath — falling back to guessing", RESOURCE);
				return;
			}
			try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = r.readLine()) != null) {
					if (line.isEmpty() || line.charAt(0) == '#') {
						continue;
					}
					String[] f = line.split("\t", -1);
					if (f.length < 3) {
						continue;
					}
					String quest = questKey(f[1]);
					if (quest.isEmpty()) {
						continue;
					}
					switch (f[0]) {
						case "Q":
						case "N":
							String option = normalise(f[2]);
							if (!option.isEmpty()) {
								OPTIONS.computeIfAbsent(quest, k -> new HashMap<>())
										.merge(option, "Q".equals(f[0]), (a, b) -> a || b);
							}
							break;
						case "M":
							if (f.length >= 4 && !f[2].isEmpty() && !f[3].isEmpty()) {
								MENUS.computeIfAbsent(quest, k -> new HashMap<>())
										.putIfAbsent(f[2], f[3]);
							}
							break;
						default:
							break;
					}
				}
			}
			log.info("[QuestDialogueCorpus] loaded real dialogue for {} quests ({} menus)",
					OPTIONS.size(), MENUS.values().stream().mapToInt(Map::size).sum());
		} catch (IOException e) {
			log.warn("[QuestDialogueCorpus] unable to read {}: {}", RESOURCE, e.getMessage());
		}
	}

	/** Lowercase, tags and punctuation stripped, whitespace collapsed. Mirrors the generator. */
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

	/** Quest names differ in punctuation between the wiki and the client — compare on letters only. */
	static String questKey(String questName) {
		if (questName == null) {
			return "";
		}
		return questName.toLowerCase().replace("&", "and").replaceAll("[^a-z0-9]", "");
	}

	/** The identity of a decision point: its option set, normalised, de-duplicated and sorted. */
	static String menuKey(List<String> options) {
		Set<String> normalised = new TreeSet<>();
		for (String o : options) {
			String n = normalise(o);
			if (!n.isEmpty()) {
				normalised.add(n);
			}
		}
		return String.join("|", normalised);
	}

	/** Whether the corpus knows anything about this quest at all. */
	public static boolean hasQuest(String questName) {
		ensureLoaded();
		return OPTIONS.containsKey(questKey(questName));
	}

	/**
	 * The answer for this exact menu, when the wiki records precisely one quest option for it.
	 *
	 * @return the matching live option text, or null if the menu is unknown or is a real branch
	 */
	public static String answerForMenu(String questName, List<String> liveOptions) {
		ensureLoaded();
		if (questName == null || liveOptions == null || liveOptions.size() < 2) {
			return null;
		}
		Map<String, String> menus = MENUS.get(questKey(questName));
		if (menus == null) {
			return null;
		}
		String answer = menus.get(menuKey(liveOptions));
		if (answer == null) {
			return null;
		}
		// hand back the live string, not the wiki's, so the caller can index straight into the menu
		String wanted = normalise(answer);
		for (String live : liveOptions) {
			if (normalise(live).equals(wanted)) {
				return live;
			}
		}
		return null;
	}

	/** Options this quest is known to use — the wiki's positive list. */
	public static List<String> questRelatedOptions(String questName, List<String> liveOptions) {
		return filter(questName, liveOptions, true);
	}

	/** True when the wiki explicitly marks this option as not part of the quest (shop, flavour, chat). */
	public static boolean isNonQuestOption(String questName, String option) {
		ensureLoaded();
		Map<String, Boolean> known = OPTIONS.get(questKey(questName));
		if (known == null) {
			return false;
		}
		return Boolean.FALSE.equals(known.get(normalise(option)));
	}

	private static List<String> filter(String questName, List<String> liveOptions, boolean wantQuestRelated) {
		ensureLoaded();
		if (questName == null || liveOptions == null) {
			return Collections.emptyList();
		}
		Map<String, Boolean> known = OPTIONS.get(questKey(questName));
		if (known == null) {
			return Collections.emptyList();
		}
		Set<String> out = new LinkedHashSet<>();
		for (String live : liveOptions) {
			Boolean isQuest = known.get(normalise(live));
			if (isQuest != null && isQuest == wantQuestRelated) {
				out.add(live);
			}
		}
		return List.copyOf(out);
	}
}
