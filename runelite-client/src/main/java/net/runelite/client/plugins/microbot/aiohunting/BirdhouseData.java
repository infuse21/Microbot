package net.runelite.client.plugins.microbot.aiohunting;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

final class BirdhouseData
{
	static final List<Space> SPACES = List.of(
		new Space(new WorldPoint(3763, 3755, 0), VarPlayerID.BIRDHOUSE_TRANSMIT_D),
		new Space(new WorldPoint(3768, 3761, 0), VarPlayerID.BIRDHOUSE_TRANSMIT_C),
		new Space(new WorldPoint(3677, 3882, 0), VarPlayerID.BIRDHOUSE_TRANSMIT_A),
		new Space(new WorldPoint(3679, 3815, 0), VarPlayerID.BIRDHOUSE_TRANSMIT_B));

	static final Set<String> ACCEPTED_SEEDS = Set.of(
		"potato seed", "onion seed", "cabbage seed", "tomato seed",
		"sweetcorn seed", "strawberry seed", "barley seed",
		"hammerstone seed", "asgarnian seed", "jute seed",
		"yanillian seed", "krandorian seed", "wildblood seed",
		"marigold seed", "rosemary seed", "nasturtium seed",
		"woad seed", "limpwurt seed");

	static final Set<Integer> NEST_IDS = Set.of(
		ItemID.BIRD_NEST_EGG_RED,
		ItemID.BIRD_NEST_EGG_GREEN,
		ItemID.BIRD_NEST_EGG_BLUE,
		ItemID.BIRD_NEST_SEEDS,
		ItemID.BIRD_NEST_RING,
		ItemID.BIRD_NEST_SEEDS_JAN2019,
		ItemID.BIRD_NEST_DECENTSEEDS_JAN2019);

	private BirdhouseData()
	{
	}

	static boolean isEmpty(int value)
	{
		return value == 0;
	}

	static boolean isSeeded(int value)
	{
		return value > 0 && value % 3 == 0;
	}

	@Getter
	static final class Space
	{
		private final WorldPoint location;
		private final int varpId;

		private Space(WorldPoint location, int varpId)
		{
			this.location = location;
			this.varpId = varpId;
		}
	}
}
