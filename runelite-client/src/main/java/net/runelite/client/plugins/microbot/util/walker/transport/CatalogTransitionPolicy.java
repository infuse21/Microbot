package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Conservative eligibility for direct object-backed scene transitions. */
public final class CatalogTransitionPolicy
{
	private static final Set<String> AUDITED_AGILITY_TRAVERSALS = Set.of(
		"3440,3331,0->3441,3329,0|3522|jump|bridge",
		"3441,3329,0->3440,3331,0|3522|jump|bridge",
		"3441,3331,0->3441,3329,0|3522|jump|bridge",
		"3440,3329,0->3440,3331,0|3522|jump|bridge",
		"3441,3328,0->3440,3331,0|3522|jump|bridge",
		"3440,3332,0->3441,3329,0|3522|jump|bridge",
		"3440,3328,0->3440,3331,0|3522|jump|bridge",
		"3441,3332,0->3441,3329,0|3522|jump|bridge",
		"3334,2829,0->3338,2829,0|11948|climb|climbing rocks",
		"3338,2829,0->3334,2829,0|11948|climb|climbing rocks",
		"3334,2828,0->3338,2828,0|11948|climb|climbing rocks",
		"3338,2828,0->3334,2828,0|11948|climb|climbing rocks",
		"3334,2827,0->3338,2827,0|11948|climb|climbing rocks",
		"3338,2827,0->3334,2827,0|11948|climb|climbing rocks",
		"3338,2826,0->3334,2826,0|11948|climb|climbing rocks",
		"3334,2826,0->3338,2826,0|11948|climb|climbing rocks",
		"3348,2829,0->3352,2829,0|11949|climb|climbing rocks",
		"3352,2829,0->3348,2829,0|11949|climb|climbing rocks",
		"3348,2828,0->3352,2828,0|11949|climb|climbing rocks",
		"3352,2828,0->3348,2828,0|11949|climb|climbing rocks",
		"3348,2827,0->3352,2827,0|11949|climb|climbing rocks",
		"3352,2827,0->3348,2827,0|11949|climb|climbing rocks",
		"2740,3830,1->2744,3830,1|19846|climb|rocky handholds",
		"2744,3830,1->2740,3830,1|19847|climb|rocky handholds",
		"2943,3769,0->2950,3767,0|26405|climb|rocky handholds",
		"2943,3767,0->2950,3767,0|26405|climb|rocky handholds",
		"2942,3768,0->2950,3767,0|26405|climb|rocky handholds");
	private static final Set<String> ABYSS_EXIT_RIFT_ROUTES = Set.of(
		"3044,4842,0->2269,4840,0|25382|exitthrough|chaos rift",
		"3050,4837,0->2209,4834,0|25382|exitthrough|death rift",
		"3051,4833,0->2714,4838,0|25382|exitthrough|water rift",
		"3047,4825,0->2846,4836,0|25382|exitthrough|air rift",
		"3044,4822,0->2784,4843,0|25382|exitthrough|mind rift",
		"3039,4821,0->2520,4848,0|25382|exitthrough|body rift",
		"3031,4825,0->2660,4843,0|25382|exitthrough|earth rift",
		"3029,4830,0->2587,4836,0|25382|exitthrough|fire rift",
		"3027,4834,0->3229,4834,0|25382|exitthrough|blood rift",
		"3028,4837,0->2142,4836,0|25382|exitthrough|cosmic rift",
		"3035,4842,0->2400,4844,0|25382|exitthrough|nature rift");
	private static final Set<String> ABYSS_PASSAGE_ROUTES = Set.of(
		"3039,4854,0->3039,4844,0|26250|gothrough|passage",
		"3029,4850,0->3033,4843,0|26250|gothrough|passage",
		"3021,4843,0->3039,4844,0|26250|gothrough|passage",
		"3018,4834,0->3039,4844,0|26250|gothrough|passage",
		"3018,4822,0->3039,4844,0|26250|gothrough|passage",
		"3030,4812,0->3039,4844,0|26250|gothrough|passage",
		"3042,4811,0->3039,4844,0|26250|gothrough|passage",
		"3050,4813,0->3039,4844,0|26250|gothrough|passage",
		"3058,4822,0->3039,4844,0|26250|gothrough|passage",
		"3061,4831,0->3052,4831,0|26250|gothrough|passage",
		"3059,4840,0->3039,4844,0|26250|gothrough|passage",
		"3050,4850,0->3039,4844,0|26250|gothrough|passage");
	private static final Set<String> RUNECRAFTING_EXIT_PORTAL_ROUTES = Set.of(
		"2793,4828,0->2980,3511,0|34749|use|portal",
		"2726,4832,0->3182,3162,0|34750|use|portal",
		"2521,4834,0->3052,3440,0|34753|use|portal",
		"2575,4850,0->3310,3252,0|34752|use|portal",
		"2655,4830,0->3302,3477,0|34751|use|portal",
		"2281,4837,0->3056,3594,0|34757|use|portal",
		"2275,4847,3->3060,3585,0|34757|use|portal",
		"2273,4855,3->3066,3591,0|34757|use|portal",
		"2575,4849,0->3310,3252,0|34752|use|portal",
		"3240,4832,0->3559,9779,0|43478|use|portal",
		"3224,4832,0->3559,9779,0|43478|use|portal",
		"2122,4833,0->2405,4381,0|34754|use|portal",
		"2142,4853,0->2405,4381,0|34754|use|portal",
		"2162,4833,0->2405,4381,0|34754|use|portal",
		"2142,4813,0->2405,4381,0|34754|use|portal",
		"2400,4835,0->2865,3022,0|34756|use|portal");
	private static final Set<String> ENAKHRA_SECRET_ENTRANCE_ROUTES = Set.of(
		"3194,2926,0->3124,9328,1|11046|climbdown|secret entrance",
		"3195,2925,0->3124,9328,1|11046|climbdown|secret entrance",
		"3193,2925,0->3124,9328,1|11046|climbdown|secret entrance",
		"3194,2924,0->3124,9328,1|11046|climbdown|secret entrance",
		"3189,2889,0->3120,9288,1|11047|climbdown|secret entrance",
		"3190,2888,0->3120,9288,1|11047|climbdown|secret entrance",
		"3188,2888,0->3120,9288,1|11047|climbdown|secret entrance",
		"3189,2887,0->3120,9288,1|11047|climbdown|secret entrance",
		"3148,2938,0->3086,9333,1|11045|climbdown|secret entrance",
		"3149,2937,0->3086,9333,1|11045|climbdown|secret entrance",
		"3147,2937,0->3086,9333,1|11045|climbdown|secret entrance",
		"3148,2936,0->3086,9333,1|11045|climbdown|secret entrance",
		"3146,2909,0->3080,9306,1|11048|climbdown|secret entrance",
		"3145,2908,0->3080,9306,1|11048|climbdown|secret entrance",
		"3146,2907,0->3080,9306,1|11048|climbdown|secret entrance",
		"3147,2908,0->3080,9306,1|11048|climbdown|secret entrance");
	private static final Set<String> ENAKHRA_MAGIC_BARRIER_ROUTES = Set.of(
		"3103,9318,1->3103,9320,1|11005|passthrough|magic barrier",
		"3103,9320,1->3103,9318,1|11005|passthrough|magic barrier",
		"3104,9318,1->3104,9320,1|11005|passthrough|magic barrier",
		"3104,9320,1->3104,9318,1|11005|passthrough|magic barrier",
		"3105,9318,1->3105,9320,1|11005|passthrough|magic barrier",
		"3105,9320,1->3105,9318,1|11005|passthrough|magic barrier");
	private static final Set<String> SWAN_SONG_HOLE_ROUTES = Set.of(
		"2344,3650,0->2344,3655,0|12656|enter|hole",
		"2344,3655,0->2344,3650,0|12656|enter|hole");
	private static final Set<String> MOLCH_LIZARD_TEMPLE_ROUTES = Set.of(
		"1313,3685,0->1312,10086,0|34405|enter|lizard dwelling",
		"1312,3685,0->1312,10086,0|34405|enter|lizard dwelling",
		"1293,3659,0->1292,10057,0|34402|enter|lizard dwelling",
		"1292,3659,0->1292,10057,0|34402|enter|lizard dwelling",
		"1315,3665,0->1314,10063,0|34403|enter|lizard dwelling",
		"1314,3665,0->1314,10063,0|34403|enter|lizard dwelling",
		"1329,3670,0->1330,10069,0|34403|enter|lizard dwelling",
		"1329,3669,0->1330,10069,0|34403|enter|lizard dwelling",
		"1294,10077,0->1292,3676,0|34422|jumpin|strange hole",
		"1294,10078,0->1292,3676,0|34422|jumpin|strange hole",
		"1293,10076,0->1292,3676,0|34422|jumpin|strange hole",
		"1292,10076,0->1292,3676,0|34422|jumpin|strange hole",
		"1291,10077,0->1292,3676,0|34422|jumpin|strange hole",
		"1291,10078,0->1292,3676,0|34422|jumpin|strange hole",
		"1292,10079,0->1292,3676,0|34422|jumpin|strange hole",
		"1293,10079,0->1292,3676,0|34422|jumpin|strange hole");
	private static final Set<String> MEIYERDITCH_FLOOR_ROUTES = Set.of(
		"3605,3203,1->3605,3206,1|18129|walkacross|floor",
		"3605,3206,1->3605,3203,1|18130|walkacross|floor",
		"3604,3214,1->3601,3214,1|18132|walkacross|floor",
		"3601,3214,1->3604,3214,1|18133|walkacross|floor",
		"3623,3207,1->3623,3210,1|18135|walkacross|floor",
		"3623,3210,1->3623,3207,1|18136|walkacross|floor");
	private static final Set<String> MEIYERDITCH_COURSE_ROUTES = Set.of(
		"3588,3180,0->3592,3180,0|18037|climbover|wall rubble",
		"3592,3180,0->3588,3180,0|18038|climbover|wall rubble",
		"3589,3179,0->3592,3180,0|18037|climbover|wall rubble",
		"3601,3163,1->3605,3163,1|17960|climbdown|rock",
		"3605,3163,1->3601,3163,1|17959|climbup|rock",
		"3605,3161,1->3605,3163,1|17958|jumponto|rock",
		"3606,3207,1->3606,3208,1|18078|crawlunder|wall",
		"3606,3208,1->3606,3207,1|18078|crawlunder|wall",
		"3594,3223,0->3595,3223,1|18086|climbup|shelf",
		"3595,3223,1->3594,3223,0|18087|climbdown|shelf",
		"3596,3223,1->3597,3223,1|18088|crawlunder|wall",
		"3597,3223,1->3596,3223,1|18088|crawlunder|wall",
		"3615,3210,1->3614,3210,2|18095|climbup|shelf",
		"3614,3210,2->3615,3210,1|18096|climbdown|shelf",
		"3616,3202,2->3622,3202,2|18099|walkacross|washing line",
		"3622,3202,2->3616,3202,2|18100|walkacross|washing line",
		"3623,3217,1->3623,3218,2|18105|climbup|shelf",
		"3623,3218,2->3623,3217,1|18106|climbdown|shelf",
		"3625,3221,2->3626,3221,1|18107|climbdown|shelf",
		"3626,3221,1->3625,3221,2|18108|climbup|shelf");
	private static final Set<String> MEIYERDITCH_PREPARED_FLOOR_ROUTES = Set.of(
		"3590,3173,1->3588,3173,0|18122|climbdown|floor",
		"3588,3173,0->3590,3173,1|18124|climbup|floor",
		"3589,3174,1->3588,3173,0|18122|climbdown|floor",
		"3589,3174,0->3590,3173,1|18124|climbup|floor",
		"3589,3173,1->3588,3173,0|18122|climbdown|floor",
		"3588,3173,1->3588,3173,0|18122|climbdown|floor",
		"3589,3173,0->3590,3173,1|18124|climbup|floor");
	private static final Set<String> MEIYERDITCH_TUNNEL_ROUTES = Set.of(
		"3598,3215,0->3598,3220,0|18083|climbinto|trapdoor tunnel",
		"3598,3220,0->3598,3215,0|18085|climbinto|tunnel",
		"3597,3219,0->3598,3215,0|18085|climbinto|tunnel");
	private static final Set<String> MEIYERDITCH_POST_QUEST_ROUTES = Set.of(
		"3649,3220,0->3631,3219,0|32660|enter|door",
		"3631,3219,0->3649,3219,0|32659|enter|door",
		"3649,3219,0->3631,3219,0|32660|enter|door",
		"3631,3220,0->3649,3219,0|32659|enter|door",
		"3649,3218,0->3631,3219,0|32660|enter|door",
		"3631,3218,0->3649,3219,0|32659|enter|door",
		"3595,3310,1->3595,3312,0|39173|climbup|wall",
		"3595,3312,0->3595,3310,1|39172|climbdown|wall",
		"3640,3253,0->3640,3252,0|17980|push|wall",
		"3640,3252,0->3640,3253,0|17980|push|wall");
	private static final Map<String, Map<Quest, QuestState>> ABYSS_EXIT_RIFT_QUESTS = Map.of(
		"death rift", Map.of(Quest.MOURNINGS_END_PART_II, QuestState.FINISHED),
		"blood rift", Map.of(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED),
		"cosmic rift", Map.of(Quest.LOST_CITY, QuestState.FINISHED));
	private static final Set<String> FREMENNIK_SURFACE_BRIDGES = Set.of(
		"2314,3848,0->2314,3839,0|21311|walkacross|rope bridge",
		"2314,3839,0->2314,3848,0|21310|walkacross|rope bridge",
		"2355,3848,0->2355,3839,0|21313|walkacross|rope bridge",
		"2355,3839,0->2355,3848,0|21312|walkacross|rope bridge",
		"2378,3839,0->2378,3848,0|21314|walkacross|rope bridge",
		"2378,3848,0->2378,3839,0|21315|walkacross|rope bridge",
		"2343,3829,0->2343,3820,0|21309|walkacross|rope bridge",
		"2343,3820,0->2343,3829,0|21308|walkacross|rope bridge",
		"2317,3823,0->2317,3832,0|21306|walkacross|rope bridge",
		"2317,3832,0->2317,3823,0|21307|walkacross|rope bridge");
	private static final Set<String> ISAFDAR_CROSSINGS = Set.of(
		"2215,3156,0->2215,3153,0|3921|stepover|tripwire",
		"2220,3155,0->2220,3152,0|3921|stepover|tripwire",
		"2215,3153,0->2215,3156,0|3921|stepover|tripwire",
		"2220,3152,0->2220,3155,0|3921|stepover|tripwire",
		"2284,3188,0->2287,3188,0|3921|stepover|tripwire",
		"2287,3188,0->2284,3188,0|3921|stepover|tripwire",
		"2202,3237,0->2196,3237,0|3931|cross|log balance",
		"2196,3237,0->2202,3237,0|3931|cross|log balance",
		"2290,3232,0->2290,3239,0|3933|cross|log balance",
		"2290,3239,0->2290,3232,0|3933|cross|log balance",
		"2294,3242,0->2294,3245,0|3921|stepover|tripwire",
		"2294,3245,0->2294,3242,0|3921|stepover|tripwire",
		"2264,3250,0->2258,3250,0|3932|cross|log balance",
		"2258,3250,0->2264,3250,0|3932|cross|log balance");
	private static final Set<String> TARNS_JUMP_ROUTES = Set.of(
		"3144,4576,2->3144,4574,2|20557|jumpto|pillar",
		"3144,4574,2->3144,4572,2|20568|jumpto|ledge",
		"3144,4572,2->3144,4574,2|20557|jumpto|pillar",
		"3144,4574,2->3144,4576,2|20569|jumpto|ledge",
		"3184,4564,1->3184,4562,1|20541|jumpto|pillar",
		"3184,4562,1->3184,4560,1|20540|jumpto|pillar",
		"3184,4560,1->3184,4558,1|20558|jumpto|ledge",
		"3184,4558,1->3184,4560,1|20540|jumpto|pillar",
		"3184,4560,1->3184,4562,1|20541|jumpto|pillar",
		"3184,4562,1->3184,4564,1|20559|jumpto|ledge",
		"3150,4597,1->3148,4597,1|20542|jumpto|pillar",
		"3148,4597,1->3150,4597,1|20560|jumpto|ledge",
		"3148,4597,1->3148,4595,1|20543|jumpto|pillar",
		"3148,4595,1->3148,4597,1|20542|jumpto|pillar",
		"3148,4595,1->3146,4595,1|20544|jumpto|pillar",
		"3146,4595,1->3148,4595,1|20543|jumpto|pillar",
		"3146,4595,1->3144,4595,1|20545|jumpto|pillar",
		"3144,4595,1->3146,4595,1|20544|jumpto|pillar",
		"3144,4595,1->3142,4595,1|20546|jumpto|pillar",
		"3142,4595,1->3144,4595,1|20545|jumpto|pillar",
		"3142,4595,1->3140,4595,1|20562|jumpto|ledge",
		"3140,4595,1->3142,4595,1|20546|jumpto|pillar",
		"3144,4595,1->3144,4597,1|20547|jumpto|pillar",
		"3144,4597,1->3144,4599,1|20548|jumpto|pillar",
		"3144,4599,1->3144,4601,1|20563|jumpto|ledge",
		"3144,4601,1->3144,4599,1|20548|jumpto|pillar",
		"3144,4599,1->3144,4597,1|20547|jumpto|pillar",
		"3144,4597,1->3144,4595,1|20545|jumpto|pillar",
		"3180,4596,1->3182,4596,1|20549|jumpto|pillar",
		"3182,4596,1->3180,4596,1|20564|jumpto|ledge",
		"3180,4600,1->3182,4600,1|20550|jumpto|pillar",
		"3182,4600,1->3180,4600,1|20565|jumpto|ledge",
		"3184,4600,1->3182,4600,1|20550|jumpto|pillar",
		"3182,4600,1->3184,4600,1|20551|jumpto|pillar",
		"3184,4600,1->3184,4598,1|20552|jumpto|pillar",
		"3184,4598,1->3184,4600,1|20551|jumpto|pillar",
		"3184,4598,1->3186,4598,1|20553|jumpto|pillar",
		"3186,4598,1->3184,4598,1|20552|jumpto|pillar",
		"3186,4598,1->3186,4596,1|20554|jumpto|pillar",
		"3186,4596,1->3186,4598,1|20553|jumpto|pillar",
		"3186,4596,1->3188,4596,1|20555|jumpto|pillar",
		"3188,4596,1->3186,4596,1|20554|jumpto|pillar",
		"3188,4596,1->3190,4596,1|20566|jumpto|ledge",
		"3190,4596,1->3188,4596,1|20555|jumpto|pillar",
		"3190,4600,1->3188,4600,1|20556|jumpto|pillar",
		"3188,4600,1->3190,4600,1|20567|jumpto|ledge");
	private static final Set<String> FLOORBOARD_JUMP_ROUTES = Set.of(
		"3598,3203,1->3598,3201,1|18070|jumpto|floorboards",
		"3598,3201,1->3598,3203,1|18071|jumpto|floorboards",
		"3599,3200,1->3601,3200,1|18072|jumpto|floorboards",
		"3601,3200,1->3599,3200,1|18073|jumpto|floorboards",
		"3598,3222,1->3601,3222,1|18089|jumpto|floorboards",
		"3601,3222,1->3598,3222,1|18090|jumpto|floorboards",
		"3615,3218,1->3615,3216,1|18093|jumpto|floorboards",
		"3615,3216,1->3615,3218,1|18094|jumpto|floorboards",
		"3613,3208,3->3613,3205,3|18097|jumpto|floorboards",
		"3613,3205,3->3613,3208,3|18098|jumpto|floorboards",
		"3623,3223,1->3623,3226,1|18109|jumpto|floorboards",
		"3623,3226,1->3623,3223,1|18110|jumpto|floorboards",
		"3622,3230,1->3622,3232,1|18111|jumpto|floorboards",
		"3622,3232,1->3622,3230,1|18112|jumpto|floorboards",
		"3624,3240,1->3626,3240,1|18113|jumpto|floorboards",
		"3626,3240,1->3624,3240,1|18114|jumpto|floorboards",
		"3633,3256,1->3636,3256,1|18117|jumpto|floorboards",
		"3636,3256,1->3633,3256,1|18118|jumpto|floorboards");
	static final Set<Integer> VISIBILITY_RING_IDS = Set.of(4657, 28327, 28329);
	static final String VISIBILITY_RING_OPEN = "visibility-ring-open-inventory";
	static final String VISIBILITY_RING_WEAR = "visibility-ring-wear";
	private static final Set<String> WATERFALL_THRONE_DOOR_ROUTE_KEYS = Set.of(
		"2566,9901,0->2604,9901,0|2002|open|door",
		"2604,9901,0->2566,9901,0|2002|open|door");
	private static final Set<String> DIRECT_DOOR_ROUTE_KEYS = Set.of(
		"2575,9861,0->2511,3463,0|2000|open|door",
		"2511,3463,0->2575,9861,0|2010|open|door",
		"2451,4645,0->2090,3930,0|16774|open|door",
		"3318,9602,0->2748,5374,0|6919|open|door",
		"3317,9602,0->2747,5374,0|6919|open|door",
		"3317,9603,0->2748,5374,0|6919|open|door");
	private static final Set<String> SHADOW_LADDER_ROUTE_KEYS = Set.of(
		"2547,3422,0->2630,5071,0|6560|climbdown|ladder",
		"2546,3421,0->2630,5071,0|6560|climbdown|ladder",
		"2548,3421,0->2630,5071,0|6560|climbdown|ladder",
		"2547,3420,0->2630,5071,0|6560|climbdown|ladder");
	private static final Set<String> DIRECT_ACTIONS = Set.of(
		"climb-up", "climb-down", "climb", "climb up", "climb down",
		"walk-up", "walk-down", "ascend", "descend", "top-floor", "bottom-floor",
		"enter", "exit", "leave", "crawl", "climb-into", "cross");
	private static final Set<String> DIRECT_AGILITY_ACTIONS = Set.of(
		"climb", "squeezethrough", "cross", "enter", "walkacross", "climbinto",
		"climbdown", "jumpover", "jumpto", "climbover", "jump", "climbup",
		"jumpacross", "pass", "squeezepast", "swingacross", "jumpdown", "jumpup",
		"climbthrough", "climbunder", "open", "teethgrip");
	private static final Set<Integer> DENSE_FOREST_IDS = Set.of(
		3937, 3938, 3939, 3998, 3999);
	private static final Set<Integer> DIRECT_HOLE_IDS = Set.of(31791, 28915, 28919, 28920, 28921);
	private static final Set<Integer> CATACOMBS_EXIT_VINE_IDS = Set.of(28895, 28896, 28897, 28898, 42350);
	private static final Set<Integer> DIRECT_STEPS_IDS = Set.of(30189, 30190, 8966, 33261);
	private static final int ENAKHRAS_TEMPLE_SAND_PILE_ID = 10950;
	private static final Set<Integer> CHASM_OF_FIRE_LIFT_IDS = Set.of(30258, 30259);
	private static final Set<Integer> MYTHS_GUILD_MAGICAL_BARRIER_IDS = Set.of(31616, 31617);
	private static final Set<Integer> PRIFDDINAS_CITY_GATE_ENTER_IDS = Set.of(36518, 36519);
	private static final Set<Integer> PRIFDDINAS_CITY_GATE_EXIT_IDS = Set.of(36522, 36523);
	private static final Set<Integer> BASALT_CAUSEWAY_IDS = Set.of(
		4550, 4551, 4552, 4553, 4554, 4555, 4556, 4557, 4558, 4559);
	private static final Set<String> DIRECT_RAFT_ROUTES = Set.of(
		"2510,3494,0->2512,3481,0|1987|board|log raft",
		"2510,3493,0->2512,3481,0|1987|board|log raft",
		"1742,5352,0->2531,3446,0|25216|ride|aged log",
		"1761,5362,0->2531,3446,0|25216|ride|aged log",
		"2567,9680,0->2606,9692,0|2849|board|raft",
		"2606,9692,0->2567,9680,0|2849|board|raft");
	private static final Set<String> EQUIPPED_GRAPPLE_ROUTES = Set.of(
		"3246,3179,0->3259,3179,0|17068|grapple|broken raft",
		"3259,3179,0->3246,3179,0|17068|grapple|broken raft",
		"3033,3390,0->3033,3389,1|17049|grapple|wall",
		"3032,3388,0->3032,3389,1|17050|grapple|wall",
		"2866,3428,0->2869,3428,0|17042|grapple|rocks",
		"2556,3072,0->2556,3073,1|17047|grapple|wall",
		"2556,3075,0->2556,3074,1|17047|grapple|wall",
		"2874,3133,0->2874,3127,0|17074|grapple|strong tree",
		"2874,3127,0->2874,3133,0|17074|grapple|strong tree",
		"2874,3136,0->2874,3142,0|17074|grapple|strong tree",
		"2874,3142,0->2874,3136,0|17074|grapple|strong tree",
		"2841,3427,0->2841,3433,0|17062|grapple|tree");
	private static final Set<String> TEMPLE_OF_THE_EYE_PORTAL_ROUTES = Set.of(
		"3104,9573,0->3615,9470,0|43841|enter|portal",
		"3615,9470,0->3104,9573,0|43692|enter|portal");
	private static final Set<String> MOR_UL_REK_HOT_VENT_ROUTES = Set.of(
		"2493,5174,0->2495,5174,0|30266|pass|hot vent door",
		"2495,5174,0->2493,5174,0|30266|pass|hot vent door",
		"2494,5157,0->2496,5157,0|30266|pass|hot vent door",
		"2496,5157,0->2494,5157,0|30266|pass|hot vent door",
		"2474,5138,0->2474,5136,0|30266|pass|hot vent door",
		"2474,5136,0->2474,5138,0|30266|pass|hot vent door",
		"2457,5120,0->2457,5118,0|30266|pass|hot vent door",
		"2457,5118,0->2457,5120,0|30266|pass|hot vent door",
		"2436,5121,0->2436,5119,0|30266|pass|hot vent door",
		"2436,5119,0->2436,5121,0|30266|pass|hot vent door",
		"2399,5177,0->2399,5175,0|30266|pass|hot vent door");
	private static final Set<String> KARAMJA_VOLCANO_ROUTES = Set.of(
		"2855,3169,0->2855,9569,0|11441|climbdown|rocks",
		"2856,3167,0->2856,9567,0|11441|climbdown|rocks",
		"2855,3168,0->2855,9568,0|11441|climbdown|rocks",
		"2857,3167,0->2857,9567,0|11441|climbdown|rocks",
		"2858,3168,0->2858,9568,0|11441|climbdown|rocks",
		"2858,3169,0->2858,9569,0|11441|climbdown|rocks",
		"2857,3170,0->2857,9570,0|11441|climbdown|rocks",
		"2856,3170,0->2856,9570,0|11441|climbdown|rocks",
		"2855,9569,0->2856,3167,0|18969|climb|climbing rope",
		"2856,9568,0->2856,3167,0|18969|climb|climbing rope",
		"2856,9570,0->2856,3167,0|18969|climb|climbing rope",
		"2857,9569,0->2856,3167,0|18969|climb|climbing rope");
	private static final Set<String> OPENING_EXIT_ROUTES = Set.of(
		"3164,10044,0->3152,3644,0|39648|exit|opening",
		"3164,10043,0->3152,3644,0|39648|exit|opening",
		"3164,10042,0->3152,3644,0|39648|exit|opening",
		"3385,10052,0->3259,3663,0|40389|exit|opening",
		"3406,10145,0->3293,3749,0|40391|exit|opening",
		"2167,9308,0->2310,2919,0|40737|exit|opening",
		"3384,10052,0->3259,3663,0|40389|exit|opening",
		"3386,10052,0->3259,3663,0|40389|exit|opening",
		"3405,10145,0->3294,3749,0|40391|exit|opening",
		"3406,10145,0->3294,3749,0|40391|exit|opening",
		"3407,10145,0->3294,3749,0|40391|exit|opening");
	private static final Set<String> CLIMB_UP_EXIT_ROUTES = Set.of(
		"2696,9683,0->2697,3283,0|18354|climbup|exit",
		"3595,10291,0->3680,3854,0|30844|climbup|exit",
		"3596,10291,0->3680,3854,0|30844|climbup|exit",
		"2618,10266,0->2620,3864,0|15193|climbup|exit",
		"2619,10265,0->2620,3864,0|15193|climbup|exit",
		"2618,10264,0->2620,3864,0|15193|climbup|exit",
		"2617,10265,0->2620,3864,0|15193|climbup|exit");
	private static final Set<String> ROPE_EXIT_ROUTES = Set.of(
		"3168,9572,0->3168,3172,0|5946|climb|climbing rope",
		"3169,9571,0->3169,3171,0|5946|climb|climbing rope",
		"3170,9572,0->3170,3172,0|5946|climb|climbing rope",
		"2832,9657,0->2834,3258,0|25213|climb|climbing rope",
		"2833,9656,0->2834,3258,0|25213|climb|climbing rope",
		"2833,9658,0->2834,3258,0|25213|climb|climbing rope",
		"2834,9657,0->2834,3258,0|25213|climb|climbing rope",
		"3372,9305,0->3375,2904,0|10434|climb|rope",
		"3373,9304,0->3375,2904,0|10434|climb|rope",
		"3373,9306,0->3375,2904,0|10434|climb|rope",
		"3374,9305,0->3375,2904,0|10434|climb|rope",
		"1752,5137,0->2985,3316,0|12230|climb|rope",
		"2880,5311,2->2916,3745,0|26370|climb|rope",
		"2881,5310,2->2916,3745,0|26370|climb|rope",
		"2882,5311,2->2916,3745,0|26370|climb|rope");
	private static final Set<String> QUEST_GATED_ENTRANCE_ROUTES = Set.of(
		"3322,2858,0->3319,2796,0|6621|enter|rock",
		"3322,2859,0->3319,2796,0|6621|enter|rock",
		"3323,2860,0->3319,2796,0|6621|enter|rock",
		"3324,2860,0->3319,2796,0|6621|enter|rock",
		"3322,2857,0->3319,2796,0|6621|enter|rock",
		"3323,2856,0->3319,2796,0|6621|enter|rock",
		"3324,2856,0->3319,2796,0|6621|enter|rock",
		"3325,2856,0->3319,2796,0|6621|enter|rock",
		"3325,2860,0->3319,2796,0|6621|enter|rock",
		"3326,2859,0->3319,2796,0|6621|enter|rock",
		"3326,2857,0->3319,2796,0|6621|enter|rock",
		"3326,2858,0->3319,2796,0|6621|enter|rock",
		"2719,4913,0->2720,4884,2|6310|enter|door",
		"2721,4911,0->2720,4884,2|6310|enter|door",
		"2720,4911,0->2720,4884,2|6310|enter|door",
		"2719,4912,0->2720,4884,2|6310|enter|door",
		"2722,4911,0->2720,4884,2|6310|enter|door",
		"2723,4911,0->2720,4884,2|6310|enter|door",
		"2724,4912,0->2720,4884,2|6310|enter|door",
		"2724,4913,0->2720,4884,2|6310|enter|door",
		"2723,4914,0->2720,4884,2|6310|enter|door",
		"2722,4914,0->2720,4884,2|6310|enter|door",
		"2721,4914,0->2720,4884,2|6310|enter|door",
		"2720,4914,0->2720,4884,2|6310|enter|door",
		"2778,3869,0->2772,10232,0|5009|enter|tunnel",
		"2778,3870,0->2772,10232,0|5009|enter|tunnel",
		"2779,3871,0->2772,10232,0|5009|enter|tunnel",
		"2780,3871,0->2772,10232,0|5009|enter|tunnel",
		"2781,3871,0->2772,10232,0|5009|enter|tunnel",
		"2782,3870,0->2772,10232,0|5009|enter|tunnel",
		"2782,3869,0->2772,10232,0|5009|enter|tunnel",
		"2782,3868,0->2772,10232,0|5009|enter|tunnel",
		"2781,3867,0->2772,10232,0|5009|enter|tunnel",
		"2780,3867,0->2772,10232,0|5009|enter|tunnel",
		"2779,3867,0->2772,10232,0|5009|enter|tunnel",
		"2730,3714,0->2773,10162,0|5008|enter|tunnel",
		"2730,3713,0->2773,10162,0|5008|enter|tunnel",
		"2730,3712,0->2773,10162,0|5008|enter|tunnel",
		"2731,3711,0->2773,10162,0|5008|enter|tunnel",
		"2773,10162,0->2730,3713,0|5014|enter|tunnel",
		"2773,10163,0->2730,3713,0|5014|enter|tunnel",
		"2773,10161,0->2730,3713,0|5014|enter|tunnel",
		"2799,10134,0->2797,3719,0|5013|enter|tunnel",
		"2800,10134,0->2797,3719,0|5013|enter|tunnel",
		"2797,3719,0->2799,10134,0|5012|enter|tunnel",
		"2795,3719,0->2799,10134,0|5012|enter|tunnel",
		"2796,3719,0->2799,10134,0|5012|enter|tunnel",
		"2804,10187,0->2822,3745,0|5011|enter|tunnel",
		"2803,10187,0->2822,3745,0|5011|enter|tunnel",
		"2802,10187,0->2822,3745,0|5011|enter|tunnel");
	private static final Set<String> CRANDOR_HOLE_ROUTES = Set.of(
		"2832,3255,0->2833,9658,0|25154|enter|hole",
		"2832,3256,0->2833,9658,0|25154|enter|hole",
		"2832,3257,0->2833,9658,0|25154|enter|hole",
		"2833,3258,0->2833,9658,0|25154|enter|hole",
		"2834,3258,0->2833,9658,0|25154|enter|hole",
		"2835,3258,0->2833,9658,0|25154|enter|hole",
		"2833,3254,0->2833,9658,0|25154|enter|hole",
		"2834,3254,0->2833,9658,0|25154|enter|hole",
		"2835,3254,0->2833,9658,0|25154|enter|hole",
		"2836,3255,0->2833,9658,0|25154|enter|hole",
		"2836,3256,0->2833,9658,0|25154|enter|hole",
		"2836,3257,0->2833,9658,0|25154|enter|hole");
	private static final Set<String> SHILO_BROKEN_CART_ROUTES = Set.of(
		"2879,2954,0->2876,2952,0|2216|climbover|broken cart",
		"2880,2953,0->2876,2952,0|2216|climbover|broken cart",
		"2880,2952,0->2876,2952,0|2216|climbover|broken cart",
		"2880,2951,0->2876,2952,0|2216|climbover|broken cart",
		"2879,2950,0->2876,2952,0|2216|climbover|broken cart",
		"2877,2954,0->2880,2952,0|2216|climbover|broken cart",
		"2876,2953,0->2880,2952,0|2216|climbover|broken cart",
		"2876,2952,0->2880,2952,0|2216|climbover|broken cart",
		"2876,2951,0->2880,2952,0|2216|climbover|broken cart",
		"2877,2950,0->2880,2952,0|2216|climbover|broken cart");
	private static final Set<String> STRONGHOLD_ESCAPE_ROUTES = Set.of(
		"2122,5251,0->2042,5245,0|23705|climbup|dripping vine",
		"2147,5284,0->2358,5215,0|23706|climbdown|dripping vine",
		"2148,5283,0->2358,5215,0|23706|climbdown|dripping vine",
		"2149,5284,0->2358,5215,0|23706|climbdown|dripping vine",
		"2150,5279,0->2123,5252,0|23703|climbup|goo covered vine",
		"2150,5277,0->2123,5252,0|23703|climbup|goo covered vine",
		"2151,5278,0->2123,5252,0|23703|climbup|goo covered vine",
		"2349,5215,0->3081,3421,0|23732|climbup|bone chain",
		"2350,5214,0->3081,3421,0|23732|climbup|bone chain",
		"2351,5215,0->3081,3421,0|23732|climbup|bone chain",
		"2350,5216,0->3081,3421,0|23732|climbup|bone chain");
	private static final Set<String> WINTERTODT_DOOR_ROUTES = Set.of(
		"1630,3968,0->1630,3963,0|29322|enter|doors of dinh",
		"1630,3963,0->1630,3968,0|29322|enter|doors of dinh",
		"1627,3963,0->1630,3979,0|29322|enter|door of dinh",
		"1628,3963,0->1630,3979,0|29322|enter|door of dinh",
		"1629,3963,0->1630,3979,0|29322|enter|door of dinh",
		"1630,3963,0->1630,3979,0|29322|enter|door of dinh",
		"1631,3963,0->1630,3979,0|29322|enter|door of dinh",
		"1632,3963,0->1630,3979,0|29322|enter|door of dinh",
		"1633,3963,0->1630,3979,0|29322|enter|door of dinh",
		"1627,3963,0->1630,3958,0|29322|enter|door of dinh",
		"1628,3963,0->1630,3958,0|29322|enter|door of dinh",
		"1629,3963,0->1630,3958,0|29322|enter|door of dinh",
		"1630,3963,0->1630,3958,0|29322|enter|door of dinh",
		"1631,3963,0->1630,3958,0|29322|enter|door of dinh",
		"1632,3963,0->1630,3958,0|29322|enter|door of dinh",
		"1633,3963,0->1630,3958,0|29322|enter|door of dinh");
	private static final Set<String> WINTERTODT_GAP_ROUTES = Set.of(
		"1633,4023,0->1631,4023,0|29326|jump|gap",
		"1631,4023,0->1629,4023,0|29326|jump|gap",
		"1629,4023,0->1627,4023,0|29326|jump|gap",
		"1631,4023,0->1633,4023,0|29326|jump|gap",
		"1629,4023,0->1631,4023,0|29326|jump|gap",
		"1627,4023,0->1629,4023,0|29326|jump|gap");
	private static final Set<String> LITHKREN_BROKEN_DOOR_ROUTES = Set.of(
		"3551,10481,0->1568,5061,0|32117|enter|broken grandiose doors",
		"3550,10481,0->1568,5061,0|32117|enter|broken grandiose doors",
		"3549,10481,0->1568,5061,0|32117|enter|broken grandiose doors",
		"1569,5061,0->3549,10481,0|32132|enter|broken grandiose doors",
		"1568,5061,0->3549,10481,0|32132|enter|broken grandiose doors",
		"1567,5061,0->3549,10481,0|32132|enter|broken grandiose doors",
		"1566,5061,0->3549,10481,0|32132|enter|broken grandiose doors",
		"1565,5061,0->3549,10481,0|32132|enter|broken grandiose doors",
		"3680,3854,0->3595,10291,0|32132|enter|broken grandiose doors");
	private static final Set<String> DIRECT_OUTWARD_EXIT_ROUTES = Set.of(
		"2409,9812,0->2402,3419,0|17223|exit|tunnel",
		"2408,9812,0->2402,3419,0|17222|exit|tunnel",
		"3728,5692,0->3060,9766,0|26655|exit|tunnel",
		"2461,10417,0->2465,4010,0|37411|exit|steps",
		"3366,11483,0->3361,3148,0|44636|exit|exit",
		"3225,12445,0->3225,6046,0|36691|exit|steps");
	private static final Set<String> CAMDOZAAL_ROUTES = Set.of(
		"2998,3494,0->2952,5762,0|41357|enter|ruins entrance",
		"2952,5762,0->2998,3494,0|41446|exit|ruins exit");
	private static final Set<String> HEROES_ROCK_SLIDE_ROUTES = Set.of(
		"2840,3516,0->2836,3517,0|2634|mine|rock slide",
		"2839,3516,0->2836,3517,0|2634|mine|rock slide",
		"2837,3517,0->2840,3516,0|2634|mine|rock slide",
		"2837,3518,0->2840,3517,0|2634|mine|rock slide",
		"2840,3517,0->2837,3518,0|2634|mine|rock slide",
		"2840,3518,0->2837,3519,0|2634|mine|rock slide");
	private static final Set<String> STRONGHOLD_SLAYER_TUNNEL_ROUTES = Set.of(
		"2435,9807,0->2429,9807,0|30174|enter|tunnel",
		"2435,9806,0->2429,9806,0|30174|enter|tunnel");
	private static final Set<String> WEISS_HOLE_ROUTES = Set.of(
		"2855,3941,0->2859,3968,0|33227|descend|hole",
		"2854,3941,0->2859,3968,0|33227|descend|hole",
		"2853,3941,0->2859,3968,0|33227|descend|hole",
		"2856,3941,0->2859,3968,0|33227|descend|hole",
		"2852,3941,0->2859,3968,0|33227|descend|hole");
	private static final Set<Integer> ROCK_SLIDE_PICKAXE_IDS = Set.of(
		1265, 1267, 1269, 12297, 1273, 1271, 1275, 11920, 23680, 23276, 13243, 20014);
	private static final Set<String> UNLOCKED_PASSAGE_ROUTES = Set.of(
		"3110,3363,2->2677,5214,2|11355|enter|interdimensional rift",
		"3111,3363,2->2677,5214,2|11355|enter|interdimensional rift",
		"1803,9968,0->1727,9993,0|28918|enter|strange passage",
		"1803,9968,0->1726,9994,0|28918|enter|strange passage",
		"1803,9967,0->1726,9994,0|28918|enter|strange passage",
		"1461,9879,0->1639,10046,0|42249|enter|strange passage");
	private static final Set<Integer> MOR_UL_REK_CAPE_IDS = Set.of(6570, 13329, 24134, 24223);
	private static final Set<Set<Integer>> MOR_UL_REK_CAPES = Set.of(MOR_UL_REK_CAPE_IDS);
	private static final int GUARDIANS_OF_THE_RIFT_BARRIER_ID = 43700;
	private static final int RUBBER_CAP_MUSHROOM_ID = 30606;
	private static final Set<String> NEYPOTZLI_ENTRANCE_ROUTE_KEYS = Set.of(
		"1374,9667,0->1347,9590,0|51377|passthrough|entrance",
		"1347,9590,0->1374,9667,0|51375|passthrough|entrance",
		"1418,9632,0->1387,9591,0|51377|passthrough|entrance",
		"1387,9591,0->1418,9632,0|51375|passthrough|entrance",
		"1513,9563,0->1355,9538,0|51377|passthrough|entrance",
		"1355,9538,0->1513,9563,0|51375|passthrough|entrance",
		"1526,9671,0->1513,9596,0|51377|passthrough|entrance",
		"1513,9596,0->1526,9671,0|51377|passthrough|entrance",
		"1522,9719,0->1390,9676,0|51378|passthrough|entrance",
		"1390,9676,0->1522,9719,0|51378|passthrough|entrance",
		"1403,9717,0->1422,9649,1|51375|passthrough|entrance",
		"1422,9649,1->1403,9717,0|51376|passthrough|entrance",
		"1388,9575,0->1423,9615,1|51375|passthrough|entrance",
		"1423,9615,1->1388,9575,0|51376|passthrough|entrance",
		"1480,9669,0->1457,9649,1|51375|passthrough|entrance",
		"1457,9649,1->1480,9669,0|51376|passthrough|entrance",
		"1510,9675,0->1461,9632,0|51375|passthrough|entrance",
		"1461,9632,0->1510,9675,0|51377|passthrough|entrance",
		"1440,9653,0->1403,9704,0|51377|passthrough|entrance",
		"1403,9704,0->1440,9653,0|51377|passthrough|entrance",
		"1440,9615,1->1439,9599,1|51377|passthrough|entrance",
		"1439,9599,1->1440,9615,1|51375|passthrough|entrance",
		"1435,3128,0->1439,9509,1|51375|passthrough|entrance",
		"1436,3128,0->1439,9509,1|51375|passthrough|entrance",
		"1439,9509,1->1435,3128,0|51375|passthrough|entrance");
	private static final Set<String> DIRECT_CLIMB_UP_ROPE_ROUTE_KEYS = Set.of(
		"3297,9824,0->3312,3450,0|13999|climbup|rope",
		"3298,9823,0->3312,3450,0|13999|climbup|rope",
		"3296,9823,0->3312,3450,0|13999|climbup|rope",
		"3297,9822,0->3312,3450,0|13999|climbup|rope",
		"3483,9510,2->3226,3108,0|3829|climbup|rope",
		"3484,9510,2->3226,3108,0|3829|climbup|rope",
		"3483,9509,2->3226,3108,0|3829|climbup|rope",
		"3508,9493,0->3508,9497,2|3832|climbup|rope",
		"3507,9494,0->3508,9497,2|3832|climbup|rope",
		"3508,9494,0->3508,9497,2|3832|climbup|rope",
		"3206,9379,0->3310,2961,0|6439|climbup|rope",
		"3205,9380,0->3310,2961,0|6439|climbup|rope",
		"3204,9379,0->3310,2961,0|6439|climbup|rope",
		"3205,9378,0->3310,2961,0|6439|climbup|rope",
		"2914,5300,1->2912,5299,2|26371|climbup|rope",
		"2920,5274,0->2919,5276,1|26375|climbup|rope",
		"2919,5274,0->2919,5276,1|26375|climbup|rope",
		"2915,5300,1->2912,5299,2|26371|climbup|rope",
		"1435,10077,3->1435,3671,0|30234|climbup|rope",
		"2128,5647,0->2026,5611,0|28687|climbup|rope");
	private static final Set<String> DIRECT_CLIMB_DOWN_HOLE_ROUTE_KEYS = Set.of(
		"2620,3864,0->2619,10265,0|15203|climbdown|hole",
		"2619,3865,0->2619,10265,0|15203|climbdown|hole",
		"2621,3865,0->2619,10265,0|15203|climbdown|hole",
		"2916,3748,0->2882,5311,2|26419|climbdown|hole",
		"2916,3745,0->2882,5311,2|26419|climbdown|hole",
		"2916,3746,0->2882,5311,2|26419|climbdown|hole",
		"2917,3744,0->2882,5311,2|26419|climbdown|hole",
		"2918,3744,0->2882,5311,2|26419|climbdown|hole",
		"2919,3744,0->2882,5311,2|26419|climbdown|hole",
		"2920,3744,0->2882,5311,2|26419|climbdown|hole",
		"2916,3747,0->2882,5311,2|26419|climbdown|hole",
		"2917,3749,0->2882,5311,2|26419|climbdown|hole",
		"2918,3749,0->2882,5311,2|26419|climbdown|hole",
		"2919,3749,0->2882,5311,2|26419|climbdown|hole",
		"2920,3749,0->2882,5311,2|26419|climbdown|hole",
		"2921,3748,0->2882,5311,2|26419|climbdown|hole",
		"2921,3747,0->2882,5311,2|26419|climbdown|hole",
		"2921,3746,0->2882,5311,2|26419|climbdown|hole");
	static final int ROPE_ITEM_ID = 954;
	static final String ATTACH_ROPE_ACTION = "Use rope";
	private static final Set<String> KALPHITE_ROPE_DESCENT_ROUTE_KEYS = Set.of(
		"3226,3108,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3226,3109,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3227,3110,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3228,3110,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3227,3107,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3228,3107,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3229,3109,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3229,3108,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3508,9498,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3508,9497,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3509,9496,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3510,9496,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3509,9499,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3510,9499,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3511,9497,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3511,9498,2->3508,9493,0|23609|climbdown|tunnel entrance");
	private static final Set<String> CRABCLAW_CAVES_DESCENT_ROUTE_KEYS = Set.of(
		"1673,9800,0->1677,9747,0|31692|climbdown|tunnel entrance",
		"1677,9747,0->1673,9800,0|31692|climbdown|tunnel entrance");
	/** Frozen item-free direct routes audited as one-click scene transitions. */
	private static final Set<String> AUDITED_DIRECT_ROUTE_KEYS = Set.of(
		"3439,3337,0->3442,9734,1|3516|enter|grotto",
		"3440,3337,0->3442,9734,1|3516|enter|grotto",
		"3441,3337,0->3442,9734,1|3516|enter|grotto",
		"3442,9734,1->3440,3337,0|3526|exit|grotto",
		"2386,3333,0->2386,3335,0|3944|enter|huge gate",
		"2386,3335,0->2386,3333,0|3944|enter|huge gate",
		"2385,3333,0->2385,3335,0|3945|enter|huge gate",
		"2385,3335,0->2385,3333,0|3945|enter|huge gate",
		"2304,3194,0->2306,3195,0|8742|pass|tree",
		"2304,3195,0->2306,3195,0|8742|pass|tree",
		"2306,3194,0->2304,3194,0|8742|pass|tree",
		"2306,3195,0->2304,3195,0|8742|pass|tree",
		"3363,3298,0->3363,3300,0|10721|enter|doorway",
		"3363,3300,0->3363,3298,0|10721|enter|doorway",
		"2715,3798,0->2715,3802,1|19690|ascend|steps",
		"2716,3798,0->2716,3802,1|19690|ascend|steps",
		"2726,3801,0->2726,3805,1|19690|ascend|steps",
		"2727,3801,0->2727,3805,1|19690|ascend|steps",
		"2715,3802,1->2715,3798,0|19691|descend|steps",
		"2716,3802,1->2716,3798,0|19691|descend|steps",
		"2726,3805,1->2726,3801,0|19691|descend|steps",
		"2727,3805,1->2727,3801,0|19691|descend|steps",
		"1556,3046,2->1559,3046,0|51644|climbdown|rope",
		"1559,3046,0->1556,3046,2|51647|climbup|rope",
		"1425,2933,0->1427,2933,0|54707|passthrough|entryway",
		"1427,2933,0->1425,2933,0|54707|passthrough|entryway",
		"1259,3430,0->1271,3436,0|57219|passthrough|cave",
		"1271,3436,0->1259,3430,0|57220|passthrough|cave");

	private CatalogTransitionPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (ShantayPassPolicy.isEligible(transport))
		{
			return true;
		}
		if (EnergyBarrierPolicy.isEligible(transport))
		{
			return true;
		}
		if (transport == null || transport.getOrigin() == null
			|| transport.getDestination() == null || transport.getObjectId() <= 0
			|| isBlank(transport.getAction()) || isBlank(transport.getName())
			|| transport.getCurrencyAmount() > 0)
		{
			return false;
		}
		if (isKalphiteRopeDescentRoute(transport))
		{
			return isKalphiteRopeSetup(transport) || isKalphiteInstalledDescent(transport);
		}
		if (isShadowDungeonLadder(transport) || ZanarisEntrancePolicy.isEligible(transport)
			|| isWaterfallThroneDoor(transport) || isMorUlRekHotVentDoor(transport)
			|| isCrandorHole(transport) || isShiloBrokenCart(transport)
			|| isStrongholdEscape(transport) || isWintertodtDoor(transport)
			|| isWintertodtGap(transport)
			|| isLithkrenBrokenDoor(transport) || isDirectOutwardExit(transport)
			|| isCamdozaalRoute(transport) || isUnlockedPassage(transport)
			|| isHeroesRockSlide(transport) || isStrongholdSlayerTunnel(transport)
			|| isWeissHole(transport))
		{
			return true;
		}
		if (isEquippedGrappleShortcut(transport) || isBarehandGrappleShortcut(transport))
		{
			return true;
		}
		if (!transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		if (isShortAgilityCrossing(transport) || isMeiyerditchCourseTraversal(transport)
			|| isMeiyerditchPreparedFloor(transport) || isMeiyerditchPostQuestAccess(transport)) return true;
		boolean changesScene = transport.getOrigin().getPlane() != transport.getDestination().getPlane()
			|| transport.getOrigin().distanceTo2D(transport.getDestination()) > 1;
		if (!changesScene)
		{
			return false;
		}
		if (isPohPortal(transport))
		{
			return true;
		}
		if (transport.getType() == TransportType.AGILITY_SHORTCUT)
		{
			return DIRECT_AGILITY_ACTIONS.contains(normalizeDirectAction(
				transport.getAction()));
		}
		if (isOrdinaryDirectTransition(transport))
		{
			return true;
		}
		if (transport.getType() != TransportType.TRANSPORT
			|| !DIRECT_ACTIONS.contains(normalize(transport.getAction())))
		{
			return false;
		}
		String name = normalize(transport.getName());
		return name.contains("ladder") || name.contains("stair")
			|| name.contains("trapdoor") || name.contains("cave")
			|| name.contains("gangplank");
	}

	public static boolean isEquippedGrappleShortcut(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.GRAPPLE_SHORTCUT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| transport.isQuestLocked() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty()
			|| !EQUIPPED_GRAPPLE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName()))))
		{
			return false;
		}
		int agility = transport.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()];
		int ranged = transport.getSkillLevels()[net.runelite.api.Skill.RANGED.ordinal()];
		int strength = transport.getSkillLevels()[net.runelite.api.Skill.STRENGTH.ordinal()];
		Set<Set<Integer>> items = transport.getItemIdRequirements();
		switch (transport.getObjectId())
		{
			case 17068:
				return agility == 8 && ranged == 37 && strength == 19 && items.isEmpty();
			case 17049:
			case 17050:
				return agility == 11 && ranged == 19 && strength == 37
					&& items.equals(Set.of(Set.of(9419)));
			case 17042:
				return agility == 32 && ranged == 35 && strength == 35
					&& items.equals(Set.of(Set.of(9419)));
			case 17047:
				return agility == 39 && ranged == 21 && strength == 38
					&& items.equals(Set.of(Set.of(9419)));
			case 17074:
				return agility == 53 && ranged == 42 && strength == 21
					&& items.equals(Set.of(Set.of(9419)));
			case 17062:
				return agility == 36 && ranged == 39 && strength == 22
					&& items.equals(Set.of(Set.of(9419)));
			default:
				return false;
		}
	}

	public static boolean isBarehandGrappleShortcut(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.AGILITY_SHORTCUT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| transport.isQuestLocked() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty() || !transport.getItemIdRequirements().isEmpty()
			|| !"2841,3427,0->2841,3433,0|17062|grapple|tree".equals(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName()))))
		{
			return false;
		}
		return transport.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()] == 72
			&& transport.getSkillLevels()[net.runelite.api.Skill.RANGED.ordinal()] == 0
			&& transport.getSkillLevels()[net.runelite.api.Skill.STRENGTH.ordinal()] == 0;
	}

	private static boolean isOrdinaryDirectTransition(Transport transport)
	{
		if (transport.getType() != TransportType.TRANSPORT)
		{
			return false;
		}
		String action = normalizeDirectAction(transport.getAction());
		String name = normalize(transport.getName());
		return isAuditedDirectDoor(transport)
			|| isAbyssExitRift(transport)
			|| isAbyssPassage(transport)
			|| isRunecraftingExitPortal(transport)
			|| isEnakhraSecretEntrance(transport)
			|| isEnakhraMagicBarrier(transport)
			|| isSwanSongHole(transport)
			|| isMolchLizardTempleTransition(transport)
			|| isMeiyerditchFloor(transport)
			|| isMeiyerditchCourseTraversal(transport)
			|| isMeiyerditchPreparedFloor(transport)
			|| isMeiyerditchTunnel(transport)
			|| isMeiyerditchPostQuestAccess(transport)
			|| isFloorboardJump(transport)
			|| isTarnsJump(transport)
			|| isIsafdarCrossing(transport)
			|| isFremennikSurfaceBridge(transport)
			|| isAuditedAgilityTraversal(transport)
			|| AUDITED_DIRECT_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| NEYPOTZLI_ENTRANCE_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| DIRECT_CLIMB_UP_ROPE_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| DIRECT_CLIMB_DOWN_HOLE_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| isCrabclawCavesDescent(transport, action, name)
			|| "climbover".equals(action) && "stile".equals(name)
			|| "climb".equals(action) && "rocks".equals(name)
			|| isBasaltCausewayTransition(transport, action, name)
			|| DIRECT_RAFT_ROUTES.contains(routeKey(transport, action, name))
			|| TEMPLE_OF_THE_EYE_PORTAL_ROUTES.contains(routeKey(transport, action, name))
			|| isGuardiansOfTheRiftBarrier(transport, action, name)
			|| isMorUlRekHotVentDoor(transport)
			|| isKaramjaVolcanoTransition(transport)
			|| isOpeningExitTransition(transport)
			|| isClimbUpExitTransition(transport)
			|| isRopeExitTransition(transport)
			|| isQuestGatedEntranceTransition(transport)
			|| "pass".equals(action) && "barrier".equals(name)
				&& transport.getObjectId() == 32153
			|| "enter".equals(action) && "dense forest".equals(name)
				&& DENSE_FOREST_IDS.contains(transport.getObjectId())
			|| "enter".equals(action) && "lift".equals(name)
				&& CHASM_OF_FIRE_LIFT_IDS.contains(transport.getObjectId())
			|| "jumpon".equals(action) && "rubber cap mushroom".equals(name)
				&& transport.getObjectId() == RUBBER_CAP_MUSHROOM_ID
			|| "pass".equals(action) && "magical barrier".equals(name)
				&& MYTHS_GUILD_MAGICAL_BARRIER_IDS.contains(transport.getObjectId())
			|| "city gate".equals(name)
				&& ("enter".equals(action)
					&& PRIFDDINAS_CITY_GATE_ENTER_IDS.contains(transport.getObjectId())
					|| "exit".equals(action)
					&& PRIFDDINAS_CITY_GATE_EXIT_IDS.contains(transport.getObjectId()))
			|| "enter".equals(action) && "passageway".equals(name)
				&& (transport.getObjectId() == 7258
					|| isTarnsLairPassagewayId(transport.getObjectId()))
			|| "enter".equals(action) && "tunnel".equals(name)
				&& transport.getObjectId() == 2141
			|| "enter".equals(action) && "hole".equals(name)
				&& DIRECT_HOLE_IDS.contains(transport.getObjectId())
			|| "climbup".equals(action) && "vine".equals(name)
				&& CATACOMBS_EXIT_VINE_IDS.contains(transport.getObjectId())
			|| "climb".equals(action) && "sand pile".equals(name)
				&& transport.getObjectId() == ENAKHRAS_TEMPLE_SAND_PILE_ID
			|| "climb".equals(action) && "steps".equals(name)
				&& DIRECT_STEPS_IDS.contains(transport.getObjectId())
				&& (transport.getOrigin().getPlane() != transport.getDestination().getPlane()
					|| transport.getOrigin().distanceTo2D(transport.getDestination()) > 2)
			|| "jumpto".equals(action) && "pillar".equals(name)
				&& isEasyRevenantCavesPillar(transport);
	}

	static boolean isMorUlRekHotVentDoor(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 30266 || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || transport.isQuestLocked()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| !transport.isMembers() || !transport.getItemIdRequirements().equals(MOR_UL_REK_CAPES))
		{
			return false;
		}
		return MOR_UL_REK_HOT_VENT_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static Set<Integer> morUlRekCapeIds()
	{
		return MOR_UL_REK_CAPE_IDS;
	}

	static boolean isKaramjaVolcanoTransition(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| transport.isQuestLocked() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty() || transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0))
		{
			return false;
		}
		return KARAMJA_VOLCANO_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isOpeningExitTransition(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| transport.isQuestLocked() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty() || !transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0))
		{
			return false;
		}
		return OPENING_EXIT_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isClimbUpExitTransition(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| transport.isQuestLocked() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty() || !transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0))
		{
			return false;
		}
		return CLIMB_UP_EXIT_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isRopeExitTransition(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| transport.isQuestLocked() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty() || !transport.getItemIdRequirements().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0))
		{
			return false;
		}
		boolean expectedMembers = transport.getObjectId() != 25213;
		return transport.isMembers() == expectedMembers
			&& ROPE_EXIT_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isQuestGatedEntranceTransition(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.isMembers() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0)
			|| !QUEST_GATED_ENTRANCE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName()))))
		{
			return false;
		}
		Quest quest;
		switch (transport.getObjectId())
		{
			case 6310:
				quest = Quest.THE_GOLEM;
				break;
			case 6621:
				quest = Quest.ICTHLARINS_LITTLE_HELPER;
				break;
			case 5008:
			case 5009:
			case 5011:
			case 5012:
			case 5013:
			case 5014:
				quest = Quest.TROLL_ROMANCE;
				break;
			default:
				return false;
		}
		int expectedDuration = transport.getObjectId() == 6310
			|| transport.getObjectId() == 5009 || transport.getObjectId() == 5011 ? 1 : 0;
		return transport.getDuration() == expectedDuration
			&& transport.getQuests().equals(Map.of(quest, QuestState.FINISHED));
	}

	static boolean isCrandorHole(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 25154 || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0)
			|| transport.getDuration() != 1
			|| !transport.getQuests().equals(Map.of(Quest.DRAGON_SLAYER_I, QuestState.FINISHED)))
		{
			return false;
		}
		return CRANDOR_HOLE_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isShiloBrokenCart(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 2216 || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || !transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0)
			|| transport.getDuration() != 2
			|| !transport.getQuests().equals(Map.of(Quest.SHILO_VILLAGE, QuestState.FINISHED)))
		{
			return false;
		}
		return SHILO_BROKEN_CART_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isStrongholdEscape(Transport transport)
	{
		if (!isBareDirectRoute(transport) || transport.isMembers() || transport.getDuration() != 0
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0))
		{
			return false;
		}
		return STRONGHOLD_ESCAPE_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isWintertodtDoor(Transport transport)
	{
		if (!isBareDirectRoute(transport) || !transport.isMembers()
			|| transport.getObjectId() != 29322 || transport.getDuration() != 1)
		{
			return false;
		}
		boolean entersPrison = transport.getDestination().equals(new WorldPoint(1630, 3979, 0));
		int firemaking = transport.getSkillLevels()[net.runelite.api.Skill.FIREMAKING.ordinal()];
		if (firemaking != (entersPrison ? 50 : 0))
		{
			return false;
		}
		return WINTERTODT_DOOR_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isWintertodtGap(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 29326 || !transport.isMembers()
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.getItemIdRequirements().isEmpty() || transport.isQuestLocked()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 2
			|| !hasOnlySkill(transport, net.runelite.api.Skill.AGILITY, 60))
		{
			return false;
		}
		return WINTERTODT_GAP_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isWintertodtGapObject(int objectId)
	{
		return objectId == 29326;
	}

	static boolean isLithkrenBrokenDoor(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| !Set.of(32117, 32132).contains(transport.getObjectId())
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.isMembers() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0)
			|| transport.getDuration() != 0
			|| !transport.getQuests().equals(Map.of(Quest.DRAGON_SLAYER_II, QuestState.FINISHED)))
		{
			return false;
		}
		return LITHKREN_BROKEN_DOOR_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isDirectOutwardExit(Transport transport)
	{
		if (!isBareDirectRoute(transport) || !transport.isMembers() || transport.getDuration() != 0
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0))
		{
			return false;
		}
		return DIRECT_OUTWARD_EXIT_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isCamdozaalRoute(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0 || transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0)
			|| transport.getDuration() != 0
			|| !transport.getQuests().equals(Map.of(Quest.BELOW_ICE_MOUNTAIN, QuestState.FINISHED)))
		{
			return false;
		}
		return CAMDOZAAL_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isHeroesRockSlide(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 2634 || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || !transport.isMembers()
			|| !transport.getItemIdRequirements().equals(Set.of(ROCK_SLIDE_PICKAXE_IDS))
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 10
			|| !transport.getQuests().equals(Map.of(Quest.HEROES_QUEST, QuestState.FINISHED))
			|| !hasOnlySkill(transport, net.runelite.api.Skill.MINING, 50))
		{
			return false;
		}
		return HEROES_ROCK_SLIDE_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isStrongholdSlayerTunnel(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 30174 || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || !transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty() || transport.isQuestLocked()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 0
			|| !hasOnlySkill(transport, net.runelite.api.Skill.AGILITY, 72))
		{
			return false;
		}
		return STRONGHOLD_SLAYER_TUNNEL_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isWeissHole(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 33227 || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || !transport.isMembers()
			|| !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 0
			|| !transport.getQuests().equals(Map.of(
				Quest.MAKING_FRIENDS_WITH_MY_ARM, QuestState.FINISHED))
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0))
		{
			return false;
		}
		return WEISS_HOLE_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	private static boolean hasOnlySkill(Transport transport, net.runelite.api.Skill skill, int level)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != (i == skill.ordinal() ? level : 0))
			{
				return false;
			}
		}
		return true;
	}

	static boolean isUnlockedPassage(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.isMembers() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarplayers().isEmpty()
			|| java.util.Arrays.stream(transport.getSkillLevels()).anyMatch(level -> level != 0)
			|| !UNLOCKED_PASSAGE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName()))))
		{
			return false;
		}
		if (transport.getObjectId() == 11355)
		{
			return transport.getDuration() == 1 && transport.getVarbits().isEmpty()
				&& transport.getQuests().equals(Map.of(Quest.ERNEST_THE_CHICKEN,
					QuestState.FINISHED));
		}
		if (!transport.getQuests().isEmpty())
		{
			return false;
		}
		if (transport.getObjectId() == 28918)
		{
			boolean zeroDuration = transport.getDestination().equals(new WorldPoint(1727, 9993, 0));
			return transport.getDuration() == (zeroDuration ? 0 : 1)
				&& hasExactVarbit(transport, 5087, 1);
		}
		return transport.getObjectId() == 42249 && transport.getDuration() == 2
			&& hasExactVarbit(transport, 12341, 1);
	}

	private static boolean isBareDirectRoute(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getCurrencyAmount() == 0
			&& !transport.isQuestLocked() && transport.getItemIdRequirements().isEmpty()
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty();
	}

	static boolean isFremennikSurfaceBridge(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.getItemIdRequirements().isEmpty()) return false;
		return FREMENNIK_SURFACE_BRIDGES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())))
			&& (transport.getObjectId() < 21314
				|| transport.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()] == 40);
	}

	static boolean isAuditedAgilityTraversal(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()) return false;
		if (!AUDITED_AGILITY_TRAVERSALS.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())))) return false;
		int level = transport.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()];
		if (transport.getObjectId() == 26405)
		{
			return level == 60
				&& transport.getQuests().equals(Map.of(Quest.TROLL_STRONGHOLD, QuestState.IN_PROGRESS));
		}
		int expected = transport.getObjectId() == 3522 ? 1
			: transport.getObjectId() == 11948 ? 0
			: transport.getObjectId() == 11949 ? 30 : 35;
		return level == expected && transport.getQuests().isEmpty();
	}

	static boolean isAuditedAgilityTraversalObject(int objectId)
	{
		return objectId == 3522 || objectId == 11948 || objectId == 11949
			|| objectId == 19846 || objectId == 19847 || objectId == 26405;
	}

	static boolean isFremennikSurfaceBridgeObject(int objectId)
	{
		return objectId >= 21306 && objectId <= 21315;
	}

	static boolean isIsafdarCrossing(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.getItemIdRequirements().isEmpty()) return false;
		return ISAFDAR_CROSSINGS.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())))
			&& (transport.getObjectId() == 3921
				|| transport.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()] == 45
					&& transport.getQuests().equals(Map.of(Quest.REGICIDE, QuestState.IN_PROGRESS)));
	}

	static boolean isIsafdarCrossingObject(int objectId)
	{
		return objectId == 3921 || objectId >= 3931 && objectId <= 3933;
	}

	static boolean isShortAgilityCrossing(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.AGILITY_SHORTCUT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.isConsumable() || !transport.getItemIdRequirements().isEmpty()
			|| transport.getCurrencyAmount() != 0 || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty()) return false;
		boolean trellis = transport.getObjectId() == 2149;
		return transport.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()] == (trellis ? 35 : 50)
			&& transport.getQuests().equals(Map.of(trellis ? Quest.GARDEN_OF_TRANQUILLITY
				: Quest.LEGENDS_QUEST, QuestState.FINISHED))
			&& Set.of("3228,3470,0->3228,3471,0|2149|climb|trellis",
				"3228,3471,0->3228,3470,0|2149|climb|trellis",
				"2790,9295,0->2789,9296,0|2926|jumpover|jagged wall",
				"2789,9296,0->2790,9295,0|2926|jumpover|jagged wall")
				.contains(routeKey(transport, normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isAbyssExitRift(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() != 25382 || transport.isConsumable()
			|| !transport.getItemIdRequirements().isEmpty() || transport.getCurrencyAmount() != 0
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty())
		{
			return false;
		}
		String name = normalize(transport.getName());
		return ABYSS_EXIT_RIFT_ROUTES.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), name))
			&& transport.getQuests().equals(ABYSS_EXIT_RIFT_QUESTS.getOrDefault(name, Map.of()));
	}

	static boolean isAbyssPassage(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && transport.getQuests().isEmpty()
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& ABYSS_PASSAGE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isRunecraftingExitPortal(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && transport.getQuests().isEmpty()
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& RUNECRAFTING_EXIT_PORTAL_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isEnakhraSecretEntrance(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0
			&& transport.getQuests().equals(Map.of(Quest.ENAKHRAS_LAMENT, QuestState.FINISHED))
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& ENAKHRA_SECRET_ENTRANCE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isEnakhraMagicBarrier(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& transport.isMembers() && !transport.isConsumable()
			&& transport.getItemIdRequirements().isEmpty() && transport.getCurrencyAmount() == 0
			&& transport.getQuests().equals(Map.of(Quest.ENAKHRAS_LAMENT, QuestState.FINISHED))
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& ENAKHRA_MAGIC_BARRIER_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isSwanSongHole(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0
			&& transport.getQuests().equals(Map.of(Quest.SWAN_SONG, QuestState.FINISHED))
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& SWAN_SONG_HOLE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isMolchLizardTempleTransition(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && transport.getQuests().isEmpty()
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& MOLCH_LIZARD_TEMPLE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isMeiyerditchFloor(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && hasMeiyerditchCourseRequirements(transport)
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& MEIYERDITCH_FLOOR_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isMeiyerditchFloorObject(int objectId)
	{
		return objectId == 18129 || objectId == 18130 || objectId == 18132
			|| objectId == 18133 || objectId == 18135 || objectId == 18136;
	}

	static boolean isMeiyerditchCourseTraversal(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && hasMeiyerditchCourseRequirements(transport)
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& MEIYERDITCH_COURSE_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isMeiyerditchCourseObject(int objectId)
	{
		return objectId == 17958 || objectId == 17959 || objectId == 17960
			|| objectId == 18037 || objectId == 18038 || objectId == 18078
			|| objectId == 18086 || objectId == 18087 || objectId == 18088
			|| objectId == 18095 || objectId == 18096 || objectId == 18099
			|| objectId == 18100 || objectId == 18105 || objectId == 18106
			|| objectId == 18107 || objectId == 18108;
	}

	static boolean isMeiyerditchPreparedFloor(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && hasMeiyerditchCourseRequirements(transport)
			&& hasExactVarbit(transport, 2589, 1) && transport.getVarplayers().isEmpty()
			&& MEIYERDITCH_PREPARED_FLOOR_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isMeiyerditchPreparedFloorObject(int objectId)
	{
		return objectId == 18122 || objectId == 18124;
	}

	private static boolean hasExactVarbit(Transport transport, int id, int value)
	{
		if (transport.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit requirement = transport.getVarbits().iterator().next();
		return requirement.getVarbitId() == id && requirement.getValue() == value
			&& requirement.getOperator() == TransportVarbit.Operator.EQUAL;
	}

	static boolean isMeiyerditchTunnel(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && hasMeiyerditchCourseRequirements(transport)
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& MEIYERDITCH_TUNNEL_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isMeiyerditchTunnelObject(int objectId)
	{
		return objectId == 18083 || objectId == 18085;
	}

	static boolean isMeiyerditchPostQuestAccess(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0 && hasNoSkillRequirements(transport)
			&& transport.getQuests().equals(Map.of(
				Quest.DARKNESS_OF_HALLOWVALE, QuestState.FINISHED))
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& MEIYERDITCH_POST_QUEST_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isMeiyerditchPostQuestObject(int objectId)
	{
		return objectId == 17980 || objectId == 32659 || objectId == 32660
			|| objectId == 39172 || objectId == 39173;
	}

	private static boolean hasNoSkillRequirements(Transport transport)
	{
		for (int level : transport.getSkillLevels())
		{
			if (level != 0)
			{
				return false;
			}
		}
		return true;
	}

	static boolean isFloorboardJump(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& FLOORBOARD_JUMP_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())))
			&& hasMeiyerditchCourseRequirements(transport);
	}

	private static boolean hasMeiyerditchCourseRequirements(Transport transport)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			int expected = i == net.runelite.api.Skill.AGILITY.ordinal() ? 26 : 0;
			if (levels[i] != expected)
			{
				return false;
			}
		}
		return transport.getQuests().equals(Map.of(
			Quest.DARKNESS_OF_HALLOWVALE, QuestState.IN_PROGRESS));
	}

	static boolean isTarnsJump(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& TARNS_JUMP_ROUTES.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isTarnsJumpObject(int objectId)
	{
		return objectId >= 20540 && objectId <= 20569;
	}

	static boolean isKalphiteRopeSetup(Transport transport)
	{
		return isKalphiteRopeDescentRoute(transport)
			&& transport.isConsumable()
			&& transport.getItemIdRequirements().equals(Set.of(Set.of(ROPE_ITEM_ID)))
			&& hasOnlyVarbit(transport, transport.getObjectId() == 3827 ? 4586 : 11705,
				0, TransportVarbit.Operator.EQUAL);
	}

	private static boolean isKalphiteInstalledDescent(Transport transport)
	{
		if (!isKalphiteRopeDescentRoute(transport) || transport.isConsumable()
			|| !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		return transport.getObjectId() == 3827
			? hasOnlyVarbit(transport, 4586, 1, TransportVarbit.Operator.EQUAL)
			: hasOnlyVarbit(transport, 11705, 0, TransportVarbit.Operator.GREATER_THAN);
	}

	static boolean isKalphiteRopeDescentRoute(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null)
		{
			return false;
		}
		return KALPHITE_ROPE_DESCENT_ROUTE_KEYS.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	private static boolean isCrabclawCavesDescent(Transport transport, String action, String name)
	{
		return CRABCLAW_CAVES_DESCENT_ROUTE_KEYS.contains(routeKey(transport, action, name))
			&& !transport.isConsumable() && transport.getVarbits().isEmpty()
			&& transport.getQuests().equals(Map.of(Quest.THE_DEPTHS_OF_DESPAIR,
				QuestState.IN_PROGRESS));
	}

	private static boolean hasOnlyVarbit(Transport transport, int id, int value,
		TransportVarbit.Operator operator)
	{
		if (transport.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit requirement = transport.getVarbits().iterator().next();
		return requirement.getVarbitId() == id && requirement.getValue() == value
			&& requirement.getOperator() == operator;
	}

	private static boolean isGuardiansOfTheRiftBarrier(Transport transport, String action,
		String name)
	{
		if (transport.getObjectId() != GUARDIANS_OF_THE_RIFT_BARRIER_ID
			|| !"quickpass".equals(action) || !"barrier".equals(name))
		{
			return false;
		}
		int originX = transport.getOrigin().getX();
		int originY = transport.getOrigin().getY();
		int destinationX = transport.getDestination().getX();
		int destinationY = transport.getDestination().getY();
		return originX == destinationX && originX >= 3613 && originX <= 3617
			&& transport.getOrigin().getPlane() == 0
			&& transport.getDestination().getPlane() == 0
			&& (originY == 9482 && destinationY == 9484
				|| originY == 9484 && destinationY == 9482);
	}

	private static String routeKey(Transport transport, String action, String name)
	{
		return pointKey(transport.getOrigin()) + "->" + pointKey(transport.getDestination())
			+ "|" + transport.getObjectId() + "|" + action + "|" + name;
	}

	private static String pointKey(net.runelite.api.coords.WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}

	private static boolean isBasaltCausewayTransition(Transport transport, String action,
		String name)
	{
		if (!BASALT_CAUSEWAY_IDS.contains(transport.getObjectId()))
		{
			return false;
		}
		return "jumpacross".equals(action) && "basalt rock".equals(name)
			|| "jumpto".equals(action)
				&& ("beach".equals(name) || "rocky shore".equals(name));
	}

	private static boolean isEasyRevenantCavesPillar(Transport transport)
	{
		if (transport.getObjectId() != 31561)
		{
			return false;
		}
		return hasDirectedEndpoints(transport, 3220, 10088, 3220, 10084)
			|| hasDirectedEndpoints(transport, 3220, 10084, 3220, 10088);
	}

	private static boolean hasDirectedEndpoints(Transport transport, int originX, int originY,
		int destinationX, int destinationY)
	{
		return transport.getOrigin().getX() == originX && transport.getOrigin().getY() == originY
			&& transport.getOrigin().getPlane() == 0
			&& transport.getDestination().getX() == destinationX
			&& transport.getDestination().getY() == destinationY
			&& transport.getDestination().getPlane() == 0;
	}

	private static boolean isTarnsLairPassagewayId(int objectId)
	{
		return objectId == 15771 || objectId == 16132 || objectId == 18308
			|| objectId == 19029 || objectId == 20482 || objectId == 20539
			|| isBetween(objectId, 20489, 20492)
			|| isBetween(objectId, 20497, 20506)
			|| isBetween(objectId, 20509, 20532)
			|| isBetween(objectId, 20535, 20536);
	}

	private static boolean isBetween(int value, int minimum, int maximum)
	{
		return value >= minimum && value <= maximum;
	}

	static boolean isPohPortal(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.POH
			|| !"portal".equals(normalize(transport.getName())))
		{
			return false;
		}
		String action = normalize(transport.getAction());
		return "home".equals(action) || "enter".equals(action);
	}

	static boolean isShadowDungeonLadder(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& transport.getCurrencyAmount() == 0 && !transport.isConsumable()
			&& transport.getItemIdRequirements().equals(Set.of(VISIBILITY_RING_IDS))
			&& transport.getQuests().equals(Map.of(Quest.DESERT_TREASURE_I, QuestState.FINISHED))
			&& SHADOW_LADDER_ROUTE_KEYS.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	static boolean isWaterfallThroneDoor(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& transport.getCurrencyAmount() == 0 && !transport.isConsumable()
			&& transport.getItemIdRequirements().equals(Set.of(Set.of(298)))
			&& transport.getQuests().equals(Map.of(Quest.WATERFALL_QUEST, QuestState.FINISHED))
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& WATERFALL_THRONE_DOOR_ROUTE_KEYS.contains(routeKey(transport,
				normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	private static boolean isAuditedDirectDoor(Transport transport)
	{
		if (!DIRECT_DOOR_ROUTE_KEYS.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName()))))
		{
			return false;
		}
		if (transport.getObjectId() == 2010)
		{
			return transport.getQuests().equals(Map.of(Quest.WATERFALL_QUEST, QuestState.FINISHED));
		}
		return transport.getObjectId() != 6919 || transport.getQuests().equals(
			Map.of(Quest.DEATH_TO_THE_DORGESHUUN, QuestState.FINISHED));
	}

	public static boolean supportsClosedVariant(String action)
	{
		String normalized = normalize(action);
		return normalized.equals("climb-down") || normalized.equals("climb down");
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeDirectAction(String value)
	{
		return normalize(value).replace("-", "").replace(" ", "");
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
