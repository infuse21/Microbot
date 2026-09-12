# Objects — Entity Guide

Gotchas when querying or interacting with scene tile objects.

Covers utilities under:
- `runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/gameobject/`
- Object-backed bank, transport, and walker lookups

---

## 1. Reject scene objects whose local location is unavailable

A `TileObject` discovered while traversing the scene can have a null local location when the scene is loading or the object snapshot has become stale. Treat that object as outside the requested radius and continue searching instead of dereferencing the location.

**Why this matters:** A stale bank booth caused `Rs2GameObject.findBank()` to throw from its distance predicate, preventing `Rs2Bank.openBank()` from interacting with any valid bank candidate on every retry.

**Pattern to follow:**

```java
LocalPoint objectLocation = objectLocalLocation(object);
if (anchor == null || objectLocation == null) {
    return false;
}
return objectLocation.distanceTo(anchor) <= distance;
```

**Where this applies:** Scene-object radius filters, nearest-object comparators, and any code that converts a cached or scene-traversed `TileObject` to a `LocalPoint`.

**Defensive check:** Include a focused test with a null object location and assert that the candidate is skipped without throwing.
