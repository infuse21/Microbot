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

## 2. Sort using the same location snapshot used for filtering

Read each object's local position once per radius query and retain its computed distance for sorting. Re-reading the position inside a comparator can return null after a successful radius check, and repeats client-thread calls during sorting. A missing anchor should return an empty result. Test with an object that returns a valid position once and null on subsequent reads, alongside an invalid object and a valid bank chest.
