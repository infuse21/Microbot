# Phase E1 closure — transport component extraction (computed 2026-08-13)

Input for the E1 executing session. The METHOD closure below is reliable (call-graph reachability
from `handleSelectedTransport`, exclusive = no callers outside the component). The FIELD partition
computed alongside it was NOT reliable (the field scanner misread multi-line declarations and
classified HANDLER_RANGE / doorLeg* / STALL_* as transport-exclusive) — recompute fields with a
proper parse, or classify per-field by grep during the move. Mechanism decided: new class
`Rs2WalkerTransports` in the SAME package (util.walker), moved members package-private, shared
Rs2Walker members de-privated and consumed via static imports (compiler arbitrates collisions,
e.g. Rs2Walker.sleepUntil vs Global.sleepUntil). Field misplacement is cosmetic under this
mechanism (statics are statics); the real risks are static-initializer order and import
collisions. Full suite + corridor gate as always.

## Methods to move (92, ~2848 lines)

- `adjacentSamePlaneTransportSuppressionPoints` (13)
- `applyWalkerDestination` (3)
- `attemptObserved` (19)
- `attemptObservedWithoutAttemptRecord` (14)
- `awaitTerminalTravelLanding` (16)
- `canoeMapDestinationsComponentId` (9)
- `canoeMapMainComponentId` (9)
- `charterWidgetMatchesDestination` (18)
- `clickQuetzalMapDestination` (44)
- `confirmCharterTravelIfPrompted` (5)
- `consumeExpectedTransportDestination` (20)
- `ensureRequiredItemBeforeTransport` (16)
- `equipTransportProvider` (8)
- `findCharterDestinationTextWidget` (34)
- `findCharterDestinationWidget` (16)
- `findClickableCharterWidget` (13)
- `findQuetzalMapDestinationWidget` (30)
- `findTerminalTravelObject` (16)
- `finishHandledTransport` (53)
- `finishQuetzalWhistleTransport` (20)
- `getDesiredRotation` (22)
- `getFirstWidgetAction` (10)
- `getTransportActionOptions` (16)
- `handleAlKharidTollGate` (35)
- `handleCanoe` (114)
- `handleCharterShip` (20)
- `handleFairyRing` (77)
- `handleGlider` (63)
- `handleInventoryTeleports` (80)
- `handleMagicCarpet` (11)
- `handleMasterScrollBook` (21)
- `handleMinigameTeleport` (82)
- `handleObject` (107)
- `handleObjectExceptions` (177)
- `handlePohTransport` (6)
- `handleQuetzal` (25)
- `handleSeasonalTransport` (68)
- `handleSelectedTransport` (639)
- `handleSpiritTree` (52)
- `handleTeleportItem` (26)
- `handleTeleportSpell` (30)
- `handleWearableTeleports` (22)
- `handleWildernessObelisk` (16)
- `hasPrecomputedContinuationFromTransport` (26)
- `hasReachedAlKharidTollDestination` (5)
- `hasReachedTerminalTravelLanding` (26)
- `hasWidgetActions` (4)
- `incrementSeasonalHandlerMiss` (3)
- `interactWithAdventureLog` (59)
- `invokeCharterDestinationWidget` (23)
- `isAlKharidTollGateCompositionCandidate` (14)
- `isAlKharidTollGateObjectId` (3)
- `isAlKharidTollGateSceneCandidate` (13)
- `isAlKharidTollGateTransport` (6)
- `isClientThreadReadTimeout` (10)
- `isDialogueBasedTeleportItem` (14)
- `isExplicitShipMenuAction` (7)
- `isLumbridgeHomeTeleport` (4)
- `isMinecartMenuVisible` (3)
- `isPayTollAction` (3)
- `isPlayerWithinChebyshevInclusive` (8)
- `isPlayerWithinChebyshevOf` (8)
- `isQuetzalMapInterfaceVisible` (10)
- `isQuetzalWhistleItemId` (6)
- `isSettledNearAdjacentSamePlaneLanding` (37)
- `isTeleportAllowedAtWildernessLevel` (3)
- `isTerminalTravelObjectCompositionCandidate` (21)
- `isTerminalTravelObjectSceneCandidate` (15)
- `isTerminalTravelTransport` (5)
- `logRouteClear` (9)
- `markAdjacentSamePlaneTransportHandled` (5)
- `markTerminalTravelAttempt` (11)
- `nearbyTilesIgnoringCollision` (20)
- `normalizeCharterWidgetText` (9)
- `prepareTeleportSpellProviders` (51)
- `prepareTransportObjectForInteraction` (9)
- `quetzalMapLabelForDestination` (23)
- `recordTransportAttempt` (4)
- `recordTransportResult` (12)
- `resolveQuetzalMapOptionLabel` (21)
- `resolveTerminalNpcInteractionAction` (15)
- `resolveTransportObjectAction` (23)
- `rotateSlotToDesiredRotation` (30)
- `sameOrNearTransportDestination` (6)
- `selectMinecartDestination` (18)
- `selectTerminalTravelDialogueDestination` (39)
- `shouldRecalculatePathAfterTransport` (13)
- `teleportItemLeafAction` (7)
- `terminalNpcInteractionCandidates` (12)
- `transportSettlePending` (16)
- `waitForPostHandleObjectLanding` (45)
- `walkReachableMiniMapToward` (19)

## Shared Rs2Walker members the component calls (stay, de-private)

`clearRecentTransportContext`, `compactWorldPoint`, `euclideanSq`, `getClosestIndexReachableTiles`,
`getClosestTileIndex`, `info`, `isAdjacentSamePlaneTransport`, `isDoorInteractionSettling`,
`isNearPath`, `isNearSamePlane`, `isRecentEvent`, `isTransportInteractionSettling`,
`isWalkCancelled`, `markStationaryDoorOpened`, `rangedTransportEdgeKey`, `recalculatePath`,
`recentlyOpenedStationaryDoorOnSegment`, `setTarget`, `sleepUntil`, `spInfo`, `walkFastCanvas`,
`walkFastLocal`, `walkMiniMap`, `walkMiniMapToward`, plus fields `routeState`, `currentTarget`,
`currentWalkDistance`, `config`, `debug`, `doorAttemptLedger`, `expectedTransportDestinations`,
`recentCurrentTileTransportByEdge`, `TERMINAL_TRAVEL_ATTEMPTED_EDGES`, `seasonalTransportHandlers`
(verify each at move time).
