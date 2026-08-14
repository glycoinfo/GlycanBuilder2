# Which positions take a linkage, and when an anomer is shown

The rules that decide what a residue will accept and what is drawn on it. Written down because they
are chemistry rather than programming: reading the code tells you what it does, not whether that is
right, and the two have differed here.

Where a rule is a judgement rather than a fact, it says so and says whose judgement it was.

## What can take a linkage

### The residue type's own list

Each residue type declares the positions it will accept, and that list is the authority. It already
accounts for what the sugar is made of. A sample rather than the whole set — there are 134 types, and
each row below stands for a kind:

| Residue | Positions | Why |
|---|---|---|
| Glc | 2 3 4 5 6 | a plain hexose |
| GlcNAc | 3 4 5 6 | 2 carries the N-acetyl |
| GlcN | 3 4 5 6 N | 2 carries the amino group, and the nitrogen itself can be substituted |
| Neu5Ac | 1 2 3 4 6 7 8 9 N | 5 carries the N-acetyl |
| Xyl | 2 3 4 5 | a pentose has no 6 |
| GlcA | 2 3 4 5 6 | **6 stays open** — see below |

**A built-in substituent does not always close its position.** GlcA carries a carboxyl at 6 and the
position remains available, because a carboxyl can be esterified. GlcNAc's N-acetyl at 2 does close
it. The distinction is chemical, not structural, which is why the list is maintained per residue
rather than derived from the formula — and why code must consult the list rather than reason about
it.

**The list is not a general test of whether a bond may attach there.** It omits the anomeric carbon,
because that is normally where the residue attaches to its *parent* rather than where children
attach — but a bridge can attach there, and 1,6-anhydro does. Refusing every bond at a position
absent from the list therefore refuses real structures: measured, `WURCS=2.0/1,1,0/[a2122h-1x_1-5_1-6]/1/`
stops round-tripping. Whatever consults this list has to know whether it is asking about an ordinary
glycosidic bond or about a bridge, and nothing in the model draws that distinction yet.

*Confirmed by I. Yamada, 2026-08-14: "GlcA の 6 位は結合位置として提示すべきではない、という理解で合っ
ていますか" — "いいえ、間違っています。エステル結合などの可能性があります。"*

**47 of the 134 types declare no list at all** — reducing ends (freeEnd, redEnd, PA, 2AB, …) and
substituents (Me, DH, …). No list means **no constraint**, not no positions: anything reading the
list has to treat an empty one as "nothing to say" rather than "nothing allowed", or those 47 would
accept nothing.

### The ring

The ring oxygen occupies a position, and that position takes no linkage. Which position depends on
the ring form and where the anomeric centre is:

- **pyranose** with the anomeric centre at 1: position 5 is closed
- **pyranose** with the anomeric centre at 2: position 6 is closed
- **furanose**: the ring is smaller, and the position two carbons along is closed instead
- **open chain**: no ring, so nothing is closed by one

**The ring atom is not always oxygen.** It can be nitrogen, and such sugars exist. That is a fact
about what the residue *is*, not about what can be attached to it: the position is still part of the
ring and still takes no glycosidic bond. Nothing here should refuse a residue for having a nitrogen
in its ring.

### The reducing end

An **alditol** — a reduced sugar — has no ring and no anomeric centre. Its position 1 is an ordinary
hydroxyl and **can take a linkage**, which it cannot when the same sugar is in a ring.

### What another child already has

A carbon carries one glycosidic bond. A position another child already occupies is not available,
and this holds across kinds: a methyl at 4 and a branch at 4 are the same claim on the same atom.

**Only a stated position counts.** An unknown position — `?`, which is most of what a structure read
from a database carries — says nothing about where anything is, so two of those do not conflict.
A bond naming several positions at once is the same case: "one of these" is not a claim on any one
of them.

## What is drawn

### The anomeric configuration

α and β describe the configuration at the anomeric centre. **Where there is no anomeric centre there
is nothing to describe, and nothing should be shown** — not "unknown", not a placeholder. An alditol
and an open-chain form both fall under this.

*Confirmed by I. Yamada, 2026-08-14: "開鎖体でアノマー位が無い場合、アノマー表記（α/β）はすべきでは
ありません。非表示が良いのではないでしょうか？"*

Showing "α" on something that has no anomeric carbon states something untrue about the molecule, and
showing "?" states that it is unknown when it is not unknown but absent. The two are different and
the drawing should not confuse them.

## Where these rules live in the code

They should live in one place and be consulted from every other. As of 2026-08-14 they do not:

They live in `Residue.availableLinkagePositions` and `Residue.acceptsPosition`, and everything else
asks. `addChild` and `canAddChild` enforce them; the linkage dialog shows what they say rather than
working it out again.

Until this was done they were in three places and none of them was the model — the type's list, which
only the dialog asked; the ring and anomeric rules, which only the dialog knew; and what a sibling
had taken, which only the model knew. A structure drawn through the dialog obeyed rules that one
built any other way did not, which is how a Man came to carry two branches at position 4 (#34).

**A bridge is exempt, deliberately.** It is not an ordinary glycosidic bond and may attach at the
anomeric carbon, so the type's list does not apply to it; only the sibling rule does.
`acceptsPosition` takes the child for exactly that reason.
