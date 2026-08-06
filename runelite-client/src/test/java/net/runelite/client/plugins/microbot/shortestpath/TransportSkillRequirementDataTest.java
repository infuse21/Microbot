package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.Skill;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Every Skills entry in the shipped transport data must name a skill the parser can resolve.
 *
 * <p>An unresolvable requirement does not fail loudly — {@code Transport} matches the skill name
 * against {@link Skill#getName()} and simply never writes {@code skillLevels}, leaving it 0. Zero is
 * how "no requirement" is encoded, so a malformed requirement silently becomes NO requirement and the
 * transport turns usable by every account.
 *
 * <p>That is not merely permissive. {@code PathfinderConfig.blocksWalkingEdgeWhenUnavailable} blocks
 * the walking edge a shortcut spans when the shortcut is unusable, so the planner routes around it —
 * with the gate erased the edge stays open and the planner actively PREFERS the shortcut as the
 * shortest route, sending the walker back repeatedly.
 *
 * <p>Live case: the Draynor underwall tunnel rows carried {@code "42 Agility<spaces>7"}, a Duration
 * value separated by spaces instead of a tab. The field parsed as skill name {@code "Agility      7"},
 * matched nothing, and a 42 Agility shortcut became free. It looks correct in an editor, which is
 * exactly why it needs a test rather than review.
 */
public class TransportSkillRequirementDataTest {

    private static final String RESOURCE_DIR =
            "/net/runelite/client/plugins/microbot/shortestpath/";

    /** Every transport TSV that carries a Skills column. */
    private static final List<String> FILES = Arrays.asList(
            "transports.tsv",
            "agility_shortcuts.tsv",
            "boats.tsv",
            "canoes.tsv",
            "charter_ships.tsv",
            "fairy_rings.tsv",
            "gnome_gliders.tsv",
            "hot_air_balloons.tsv",
            "magic_carpets.tsv",
            "magic_mushtrees.tsv",
            "minecarts.tsv",
            "quetzals.tsv",
            "ships.tsv",
            "spirit_trees.tsv",
            "teleportation_items.tsv");

    /** Names the parser accepts: any Skill, plus the total/combat/quest-points prefixes. */
    private static boolean resolvable(String skillName) {
        for (Skill skill : Skill.values()) {
            if (skill.getName().equals(skillName)) {
                return true;
            }
        }
        String lower = skillName.toLowerCase();
        return lower.startsWith("total") || lower.startsWith("combat") || lower.startsWith("quest");
    }

    @Test
    public void everySkillRequirementInShippedDataResolves() {
        List<String> offenders = new ArrayList<>();
        Set<String> filesChecked = new HashSet<>();

        for (String file : FILES) {
            try (InputStream in = getClass().getResourceAsStream(RESOURCE_DIR + file)) {
                if (in == null) {
                    continue; // file genuinely absent from this branch; other rows still get checked
                }
                filesChecked.add(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    continue;
                }
                String[] header = headerLine.split("\t", -1);
                int skillsCol = -1;
                for (int i = 0; i < header.length; i++) {
                    if ("Skills".equals(header[i].trim())) {
                        skillsCol = i;
                        break;
                    }
                }
                if (skillsCol < 0) {
                    continue;
                }

                String line;
                int lineNo = 1;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        continue;
                    }
                    String[] fields = line.split("\t", -1);
                    if (skillsCol >= fields.length) {
                        continue;
                    }
                    String cell = fields[skillsCol];
                    if (cell.trim().isEmpty()) {
                        continue;
                    }
                    for (String requirement : cell.split(";")) {
                        String trimmed = requirement.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        String[] levelAndSkill = trimmed.split("\\s+", 2);
                        if (levelAndSkill.length < 2) {
                            offenders.add(file + ":" + lineNo + " [" + cell + "] — no skill name");
                            continue;
                        }
                        if (!resolvable(levelAndSkill[1].trim())) {
                            offenders.add(file + ":" + lineNo + " [" + cell + "] — '"
                                    + levelAndSkill[1].trim() + "' is not a known skill "
                                    + "(spaces where a tab belongs?)");
                        }
                    }
                }
            } catch (Exception e) {
                throw new AssertionError("failed reading " + file, e);
            }
        }

        assertFalse("precondition: the transport resources should be readable", filesChecked.isEmpty());
        assertTrue("skill requirements that the parser will silently DROP, making these transports "
                        + "usable by any account:\n  "
                        + offenders.stream().collect(Collectors.joining("\n  ")),
                offenders.isEmpty());
    }
}
