# What to work on next

A standing, ordered view of the open issues. The tracker says what is wrong; this says what to do
about it first, and why — the part that is otherwise carried in somebody's head and lost when they
are not the one picking up the next piece of work.

Written to be edited. When an issue is opened, closed, or turns out to be something other than what
it said, change this file in the same breath.

## How an issue is placed

Rank by **what a wrong answer costs the person receiving it**, not by how hard it looks or how
recently it was filed.

| Band | What belongs in it | The question it answers |
|---|---|---|
| **P1** | A wrong answer nobody can see is wrong | Would a user publish this and never know? |
| **P2** | Work is lost, or the output cannot be used | Does someone lose a drawing, or a figure they cannot put in a paper? |
| **P3** | Visibly wrong, but visibly | Does it look wrong enough that nobody is misled? |
| **P4** | Something the application does not do yet | Is anyone blocked, or is it a wish? |
| **P5** | Build, dependencies, release plumbing | Does it reach a user at all? |
| **P6** | Documentation, naming, open questions | Is the answer a change or a sentence? |

Two rules that decide most placements:

- **Silence outranks severity.** A mass that is quietly wrong (#127) sits above an export that
  fails loudly (#66): the loud one sends someone to the tracker, the quiet one goes into a paper.
- **A verified claim outranks a plausible one.** Before something is ranked, check whether it is
  still true. Three issues in the first pass of this list were already fixed and nobody had noticed.

## Before ranking anything, verify it

Issues here run to five years old, and the code has moved. Reproduce first — it is usually minutes,
and it changes the list. Load the dictionaries before measuring anything mass- or composition-
related: `ResidueDictionary` and the mass tables are static and empty until some workspace has been
built, and a composition measured before that reads −72.0423 rather than 910.3278. That artefact
looks exactly like the bug that was fixed in 1.34.3, and it is not.

Record the measurement in the issue. "Still happens on 1.35.2" is worth more than another opinion,
and it is what lets the next person trust this file.

---

## Verified, and closed

Measured on 1.35.2, commented with the measurement, and closed on 2026-08-14.

| # | Title | What was measured |
|---|---|---|
| #67 | Composition m/z is incorrect | Hex₃HexNAc₂ as a composition weighs 910.3278, as does the same glycan linked. Fixed by the 1.34.1–1.34.3 composition work |
| #150 | Ambiguous linkage read as the parent's position | Fixed in 1.34.0, `AmbiguousLinkageKeepsUnknownPositionsTest` holds it |
| #158 | org.jdom has no fixed version | Duplicate of #175, which carries the fuller investigation |
| #125 | Redrawing G11127BT | Half done, and split: the structure no longer takes the JVM down, it is refused with a sentence. Being unable to draw it carried on as **#200** under the title it should have had |

**#17** (AUdxxxxxh no longer supported) is still open and still wants the same thing: G00771KP reads
and draws, so ask the reporter to confirm the import is what they meant before closing — it is their
report.

---

## Fixed and closed, on 2026-08-15

For a day these were all marked done here and open on GitHub — the tracker showed a backlog that did
not exist, and no reporter had been told. Each is now closed with a comment carrying what was measured
and the version it went out in.

| # | What it was | Released in |
|---|---|---|
| #132 | `v_stripes`/`h_stripes` never painted | 1.36.0 |
| #107 | Composition export to WURCS fails | 1.36.0 |
| #127 | `MassOptions.ISOTOPE` has no effect | 1.36.0, for the neutral mass — the adducts became **#203** |
| #34 | Duplicated linkage position in a branched glycan | 1.37.0 |
| #186 | PNG background not transparent | 1.37.0 |
| #188 | SVG gives every character a white background | 1.37.0 |
| #106 | Saving on close offers Save As for an already-saved file | 1.37.0 |
| #178 | "Open additional document" leaves the document counted as unchanged | 1.37.0 |
| #179 | "Remember files after restarting" carries a reducing end into new imports | 1.37.0 |
| #90 | No second window on macOS | Not a code change — closed with the `open -n` workaround |

**#88 is fixed and deliberately still open.** The fix is on `develop` (merged as #199) and no version
a user can install carries it, so closing it would read as done to somebody whose build still shows
the problem. It is commented to that effect and closes with the next release.

**#83** is on `develop` too (merged as #201) but is not fixed by it — removing an angle that was never
used does not answer whether the symbol looks wrong, which is what the reporter was asked.

### Two things measured, now filed

Both were found while measuring something else and existed nowhere but this file until 2026-08-15.

- **#203** — `IonCloud` captures an adduct's mass when the ion is set rather than when the mass is
  computed, so an m/z can pair an average neutral mass with a monoisotopic adduct. Nil for sodium,
  +0.135 per potassium, +0.484 per chloride, and *negative* for lithium. The remainder of #127.
- **#204** — a repeat unit's own linkage position does not survive a round trip, `l1` → `l?`. Found
  while measuring #57, and a different fault from the one that issue reports.

---

## Eighteen issues arrived while nobody was triaging

Filed 2026-08-19 to 08-27, after the last pass of this file, and re-checked on 2026-09-11. They are
grouped here rather than scattered into the bands below, because they arrived as clusters and are
cheaper taken as clusters.

**#238 is the one to take first, and it is ours.** Measured on 2026-09-11:

```
in : WURCS=2.0/2,2,1/[a2122h-1a_1-5_1*N_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O]/1-2/a3-b1
out: WURCS=2.0/2,2,1/[a2122h-1a_1-5_2*NCC/3=O]      [a2122h-1b_1-5_2*NCC/3=O]/1-2/a3-b1
```

The `1*N` is gone, and the imported structure holds `Glc` with an `NAc` at 2 rather than the amine at
1 — which is exactly what the reporter describes. 1.28.0 and 1.35.2 keep it; **1.37.0 drops it**, and
1.40.0 still does. The substituent sits at position 1, which is also where the parent linkage
attaches, and 1.37.0 is where #34 made `addChild` refuse a position another child already holds. That
is a hypothesis, not a measurement — but it is the first place to look, and the band is P1: nobody
sees a substituent that was never drawn.

**#239 is not a regression, whatever the title says.** `WURCS=2.0/1,1,0/[a2122h-1x_1-?]/1/` writes
back as `[a2122h-1x_1-?_1-?]` — the unknown ring emitted twice — and it does so on 1.28.0, 1.35.2,
1.37.0 and 1.40.0 alike. Real, and worth fixing; just not something recent work broke, so it does not
carry #238's urgency. Reproduces with one residue, which is the cheapest possible test.

| cluster | issues | note |
|---|---|---|
| **Copy and paste, and selection** | #229, #240, #241, #242, #244, #245 | Six reports, one area. Changing several selected residues at once (#229, #245) and the controls going dead on select-all (#244) are the same missing idea: an operation over a selection rather than over the last residue clicked |
| **WURCS in and out** | #236, #237, #239, #228 | #236 (invalid WURCS loads silently) is the same want as **#95**, and wurcsframework already has `WURCSValidator`/`WURCSValidationReport` for it — one call answers both. #228 is inside **PR #227**'s scope |
| **Uncertain terminal residues** | #230, #231, #234 | M. Matsubara picked up #230 on 2026-09-05 |
| **Composition** | #235 | Joins the composition decision below (#7, #100, #109, #117, #220) rather than standing alone |
| **Drawing and export** | #232, #233 | Independent and small: exporting one selected structure rather than all of them, and annotation unreadable on dark residues |
| **Chemistry** | #243 | Ring and anomer when a PA label is removed |

## The `/` a reducing end cannot be called

glyconavi/glycanbuilder2web#34 asks for `GalNAc-Ser/Thr`. The refusal is this project's, not the web
application's, and it is the format rather than a rule someone chose:

```java
// GWSParser
String residue_str = "([abo?][1-9N?])?+([DL]-)?+([a-zA-z0-9_#=.]+)(?:,([?opfa]))?+";
String cleaved_str = "/([a-zA-z0-9_#]+)";
```

`/` is not merely absent from the name class — it is **the character that introduces a cleavage**.
Measured: `Ser_Thr` round-trips; `Ser/Thr` writes
`Ser/Thr=87.0320u--?b1D-GalNAc,p$MONO,Und,0,0,Ser/Thr=87.0320u` and fails to read back with
`invalid format for linkage: =87.0320u--?b1D-GalNAc,p`. The parser takes `Ser` as the name, `/Thr` as
the cleavage, and chokes on the rest.

**The web application refuses it at the keystroke; this one does not refuse it at all.**
`MassOptionsStructureDialog` hands `field_other_name.getText()` straight to
`ResidueType.createOtherReducingEnd`, so the desktop will take `Ser/Thr` and write a `.gws` that
cannot be reopened. That asymmetry is the bug here, and it is P2: the work is lost, and only on
reopening.

### What actually survives, measured

A reducing end named for each character, written to GWS and read back on 1.40.0:

| in the name | round-trips | why |
|---|---|---|
| `_` `.` `#` | **yes** | in the name class |
| `^` `]` | **yes, by accident** | the class is written `[a-zA-z0-9_#=.]`, and `a-zA-z` is a typo: it spans ASCII 65-122, so `[`, `\`, `]`, `^`, `` ` `` slip in. `]` ends a repeat and `^` follows it, so a name carrying either inside a repeat block is a fault waiting to be found |
| `/` | no | `invalid format for linkage: =87.0320u--?b1D-GalNAc,p` - `Ser` is taken as the name and `/Thr` as a cleavage |
| `-` | no | `--` is a linkage, so a single `-` ends the name |
| `,` | no | separates the ring form |
| space | no | not in the class |
| `%` | no | not in the class, so **percent-encoding is not available** |

**`-` matters as much as `/`**, and is likelier to be typed: `Ser-Thr` breaks exactly the same way.

### Encoding: the cheap option is real, and needs no format change

`Ser_2F_Thr` **round-trips today, unchanged, on the parser as it stands.** That is the whole of the
finding. An escape built only from characters the class already accepts - `_` plus hex digits, say
`_2F_` for `/` and `_2D_` for `-` - passes through every reader that exists, including 1.28.0 and
GlycoWorkbench, because to them it is an ordinary name.

What that buys and what it costs:

- **No format change, no flag day.** Old readers do not break; they show `Ser_2F_Thr` where a new one
  shows `Ser/Thr`. A degraded label is a far better failure than a file that will not open.
- **The decoding is one place** - wherever a residue name is displayed - and the encoding is one
  place: `ResidueType.createOtherReducingEnd`, which is already the single door custom names come
  through, from both the desktop dialog and `ResidueDictionary`.
- **It is not reversible for free.** A user who legitimately types `_2F_` gets `/` back. The escape
  character has to be escaped first (`_` → `_5F_`), which is ordinary but must be written down.
- **It does not fix `]` and `^`.** Those are admitted by the typo rather than by design; encoding the
  name does not stop a *dictionary* residue name containing them. Worth fixing the class separately,
  and deliberately, since narrowing it could reject files somebody already has.

The alternatives, for the record:

| approach | cost | old readers |
|---|---|---|
| **`_XX_` hex escape** (above) | small, one encode + one decode | fine - show the escaped form |
| Widen the name class, disambiguate from `cleaved_str` | format change; `/` is genuinely ambiguous with cleavage, so it needs a delimiter or quoting | **reject the file** |
| Quote the name, e.g. `"Ser/Thr"=87.0320u` | format change, but unambiguous and readable | **reject the file** |
| Keep a safe stored name, carry the display label elsewhere | needs somewhere to carry it; GWS has no field for it, so it would have to go in the name anyway | fine |

The escape is the only one that does not divide files into old and new. **Not started, and not a
coding decision alone** - whoever owns the GWS format should say whether an escape is acceptable
before anything is written.

### What it would mean for the web application

glyconavi/glycanbuilder2web#34 is a request to *use* the character, not to be told why it is refused,
and an escape is what makes that possible: the dialog would stop rejecting `/`, encode it on the way
into the residue type, and decode it for display. The refusal there is currently the only thing
standing between a user and an unopenable file, so **it should not be relaxed before the encoding
exists** - in that order, or the web application starts writing the files the desktop already can.

Worth deciding at the same time: whether the escape covers everything a label might want -
`-`, `,`, space and `(` `)` are all likelier in an aglycon name than `/` - or only the characters
asked for. Covering the class once is cheaper than returning to it per character.

## P1 — a wrong answer nobody can see is wrong

| # | Title | What is known |
|---|---|---|
| ~~#132~~ | ~~`v_stripes`/`h_stripes` never painted~~ | **Done.** Three bars, clipped to the outline. The test asserts the drawn image and, separately, that the stripes exist — Tal is green and All is blue, so comparing the pair passes on colour alone |
| ~~#107~~ | ~~Composition export to WURCS fails~~ | **Done.** `org.glycoinfo.application.glycanbuilder.composition` builds the composition as unlinked nodes and lets `WURCSFactory` canonicalize, which is what glycompconverter does. No new dependency; output pinned against that implementation's, residue by residue |
| ~~#219~~ | ~~A composition with sialic acid exports as an empty file~~ | **Fixed, on `develop`.** The residue type is `NeuAc` and composition WURCS calls it `Neu5Ac`; nothing translated. Not fixable by renaming — `Neu5Ac` is already a type, from `conf/compositions`. The table lives in the encoder now, where glycanbuilder2web has always kept its own |
| ~~#127~~ | ~~`MassOptions.ISOTOPE` has no effect~~ | **Done for the neutral mass**, 1.36.0, and closed. Residues, water, hydrogen and the derivatization all follow the choice; measured against literature (Glc 180.156, Man₃GlcNAc₂ 910.82) |
| ~~#203~~ | ~~An m/z pairs an average neutral mass with a monoisotopic adduct~~ | **Fixed in PR #205, merged to `develop` on 2026-08-15.** `IonCloud` captured an adduct's mass when the ion was set rather than when it was computed, so the choice could not reach it. `computeMZ`/`computeMass` take the flag now and `getIonsMass(boolean)` recomputes the total; a charge given an explicit mass keeps it. The test is on potassium and chloride, with sodium as the control — one stable isotope, so a sodiated m/z was right by accident and a test on the default adduct would have passed before the fix |
| ~~#185~~ | ~~EPS/PS/PDF export writes 0 bytes silently~~ | **Closed 2026-08-17.** A failed transcode is refused by name instead of being written as an empty file — it had been logged and returned as null, and the null was written. The underlying Windows failure never reproduced here: all five formats write on macOS with the pinned batik 1.19 / fop 2.11. Closed once the Store was updated, since the blocker was a version too old to test against; the reporter was asked to reopen rather than refile |
| **#66** | WURCS export fails with `"_map" is null` on a heavily modified Fuc | **Does not reproduce on 1.36.0** — a Fuc with 2-O-Me, 3-NH₂ and 4-O-Me writes WURCS, drawn or imported, alone or as a branch. The substituent MAP handling has been reworked since it was filed. Waiting on a retest and the GWS |
| ~~#34~~ | ~~Duplicated linkage position in a branched glycan~~ | **Done.** A stated position another child holds is refused, in `addChild` as well as `canAddChild` — the two had grown apart and adding is the path that makes the structure. Unknown positions still stack, and a file that already contains one still opens |

## P2 — work is lost, or the output cannot be used

| # | Title | Note |
|---|---|---|
| ~~#186~~ | ~~PNG background not transparent~~ | **Done.** The renderer could always paint without one; the export passed opaque for every format alike. BMP and JPEG keep theirs, having no alpha to write |
| ~~#188~~ | ~~SVG gives every character its own white background~~ | **Done.** `clearRect` on an SVG surface paints the background colour rather than removing anything, and the export set that colour to white. Measured: white rectangles 9 → 0, colours intact |
| ~~#204~~ | ~~A repeat unit's own linkage position does not survive a round trip~~ | **Fixed in PR #206, merged to `develop` on 2026-08-15.** It was dropped on the way in, not on the way out: `addChild` rebuilds a linkage from its bonds and ends by taking the child's anomeric carbon, and the closing marker is created fresh with none — so the `?` it was born with was written over the position that had just been worked out. `makeEdgeWithStartBracket` had always set it for the opening marker; only the closing side was missing the line, which is why one end of a repeat survived and the other did not. G03246MZ now round-trips character for character |
| **#187** | Ungrouping in PowerPoint destroys Fuc and Man | May be answered by #188 — there is no white background left to go hunting for. Worth a retest before anything else is done |
| ~~#106~~ | ~~Saving on close offers Save As for an already-saved file~~ | **Done.** The close prompt called `onSaveAs` outright; `onSave` writes to the file the document came from and falls back by itself |
| ~~#178~~ | ~~"Open additional document" leaves the document counted as unchanged~~ | **Done.** `setFilename` cleared the changed flag as a side effect, and a merge took the merged file's name as well. A merge now keeps its own name and counts as changed |
| ~~#179~~ | ~~"Remember files after restarting" carries a customised reducing end into new imports~~ | **Done.** The WURCS reader clears it alongside the derivatization and ion cloud it already cleared: what the sequence states is the answer, and what it does not state is a default rather than a leftover |

## P3 — visibly wrong

~~#88 (no right margin on a bridge)~~ — **done, on `develop` and not yet released**: the cleared area
was the glyphs' bounds cast to int, which truncates the origin one way and the width the other.

~~**#29** (bisecting GlcNAc position)~~ — **fixed in PR #207, merged to `develop` on 2026-08-15.**

The convention: branches are drawn in the numeric order of their linkage positions, so a bisecting
GlcNAc at 4 sits between the 3- and 6-antennae (I. Yamada, 2026-08-14). GlycoCraft states the same
thing as an explicit rule and gives worked numbers —
`/Users/yamada/git/gitlab/glycoinfo-dev/glycocraft/LAYOUT_ALGORITHM.md`, §4.5 Step 3 and §5:

> DrawGlycan-SNFG ルール: acceptorPos=4 (例: bisecting GlcNAc) は y=0 に固定
>
> 最終配置: α1-6 Man at y=-70, bisecting GlcNAc at y=0, α1-3 Man at y=+70

Now measured, default orientation: 6-antenna y=30, bisecting GlcNAc y=82 level with its β-Man,
3-antenna y=134. Was 82 / 30 / 134 — the bisecting above the 6-antenna, with the 6-antenna pushed onto
its parent's line.

**What it turned out to be.** Every ordinary monosaccharide is placed straight out, so all of a
residue's branches arrive in one region and are stacked in whatever order that region's list is in —
which was the order the children were stored. Attaching one afterwards appended it, so it was stacked
last, which is the far edge. That is why the issue is phrased as "if you add it after drawing": the
same molecule imported from a sequence drew correctly, because the sequence listed the children
differently.

`AbstractGlycanRenderer.inPositionOrder` sorts that region before it is stacked, ascending, in all four
orientations — which is what three of the four were already doing for a plain biantennary core, so a
middle branch moves into place and nothing already right moves at all.

**Two dead ends worth not repeating.** Sorting the children in `assignPosition` changed nothing: the
region list is rebuilt from `PositionManager.getChildrenAtPosition`, which walks the residue's own
linkages. And the first version of the test found the bisecting GlcNAc by name and position and picked
up the chitobiose core's GlcNAc, which is also at 4 — it failed without the change, for the wrong
reason.


**#57** (repeat-unit linkage position) — measured: the model holds position 2 as the sequence says,
so the 3 appears between the model and the picture, and the likely candidate is the position
belonging to the *other* end of the repeat. Asked the reporter to confirm which label. Found
separately while measuring: the round trip drops the repeat's own linkage position, `l1` → `l?`,
which is a different fault in the same corner and is not filed.

**#83** (CFG hat diamonds take orientation from the bond) — the angle those two symbols took was
never used, and is gone as of #201, on `develop`. That does not establish the symbol is
orientation-independent; the measurements are on the issue, and the reporter was asked whether it
looks *rotated* or merely *placed differently*.

~~**#183** (a failed WURCS export shows every error twice)~~ — **fixed in PR #208, merged to `develop` on 2026-08-15.**
The export wrote every structure for the file and then wrote them all again to find which had come out
empty, and one failure is already two windows — `LogUtils.report` shows the message and then a
`ReportDialog` — so the second pass produced a second pair. One pass now.

**The reported sequence no longer fails.** `WURCS=2.0/1,1,0/[A111h]/1/` writes back unchanged as of
1.37.0, so those steps produce no dialogs at all and the fix changes nothing a user would see with it.
Fixed on the mechanism rather than on anything watchable, and the reporter has been asked for a structure
that still refuses to export.

#58 (G07957FT layout) · #20 (bracket not symmetric about the reducing end) ·
#91 (Add-structure menu misaligned) · #6 (fragments carrying a bridge)

**#222** (a sulfate cannot be added to a GlcN) — the working spelling is the plain sulfate at position 2,
which the exporter combines with the residue's own `2*N` into `2*NSO/3=O/3=O`. It only works from 1.39.0:
before that the position rules refused it, which is #211's other face. What remains is that the *wrong*
spelling — at position `N` — attaches and then writes nothing, and the better answer is to refuse the
attachment. That is a position-rule change, and those have produced two regressions in two days, so it
waits for the reporter's retest rather than being done in a hurry.

## P4 — not there yet

| # | Title | Note |
|---|---|---|
| #95 | Validate a drawn structure | Before submitting to GlyTouCan. Validation code exists elsewhere and could be called |
| #100 | Substituents and defined residues in the composition builder | Overlaps #7 (which monosaccharides the list should offer); decide them together |
| #181 | No way to add deoxy / en / alditol to a monosaccharide | A regression against the old GlycoWorkbench: a modified residue can be imported from a sequence but not drawn, per R. Ranzinger. #190 (ribitol) is the concrete case to satisfy. #189 is **not** one of these — see P6 |
| #200 | A ring closed through a bridge cannot be represented | The bridge plus the direct bond make a genuine cycle where everything downstream assumes a tree, so it is a change to the structure model, not the renderer. Carried on from #125, which is closed |
| #184 | Multi-format clipboard (bitmap + text + SVG) | |
| #177 | Open a .gws by double-clicking it | Needs file association *and* accepting a path at startup |
| #41 | SNFG with linkage placement notation | CFG has it; SNFG does not |
| #93 | Nested brackets | Rare in papers, currently flattened to a composition |
| ~~#172~~ | ~~Check for updates at startup~~ | **Closed 2026-08-17 as considered and declined.** Help ▸ Check for Updates ships (1.35.0) and is user-initiated, which is what keeps it clear of Store policy; a Store-distributed application volunteering a download link is the part that does not change with the Store being current. Reopen if the Store lags again, or if the deb/rpm/dmg builds turn out to be where most installs are — `UpdateCheck.run()` is already there for it |
| #109 | Compositions with linkage (lactonised sialic acid) | A WURCS question more than a GB2 one |
| #7 | Review the Add-composition monosaccharide list | |
| #16 | A standing list of modifications that were not handled | Fourteen WURCS collected since 2021, almost certainly not one fault — `*OSO`, `*=NO` and the rest fail at different points and some read now. Asked the reporter whether to re-measure each on 1.35.2, close this, and file one issue per modification that still fails. Overlaps #181 |

## P5 — plumbing

| # | Title | Note |
|---|---|---|
| #175 | Get off `org.jdom:jdom` | The reachable path was closed in 1.35.2; the Dependabot alert stays open until the dependency moves. #158 folds into this |
| ~~#180~~ | ~~Microsoft Store still on 1.28.0~~ | **Closed 2026-08-17: the Store has been updated.** It was a release-process problem rather than a code one, and it blocked #185 and gated #172. The `.msix` is still the one step outside CI |

## P6 — answers rather than changes

#117 (what the correct composition is) ·
#182 ("Unknown" is a misleading group name) · #189 (sulfate on a GlcNAc nitrogen) ·
#190 (a KEGG structure with ribitol)

**#190** (a KEGG structure with ribitol) is an instance of #181 — a modified residue can be imported
from a sequence but not drawn, per R. Ranzinger on the issue. Left open as the concrete case #181 has
to satisfy, and the reporter asked for the sequence, which is worth more to whoever takes #181 than a
description of alditols.

**#189** (sulfate on a GlcNAc nitrogen) **is not an instance of #181, and this file said it was for a
day.** The dictionary answers it. `NS` — N-sulfate, `*NSO/3=O/3=O` — is an N-type substituent sitting
right beside `NAc`, and `GlcN` declares `3,4,5,6,N` as its positions while `GlcNAc` declares `3,4,5,6`:

- GlcN with `NS` at N is GlcNS, and it can be drawn today
- GlcNAc has no free nitrogen, and 1.37.0's position rules refuse a second substituent there **on
  purpose** — that is the rule working, not a gap

What is left is the one chemistry question, which is the reporter's: whether they mean GlcNS or a
sulfate on top of an acetylated nitrogen. Asked on the issue.

*The lesson worth keeping: "these two look like the same missing capability" was written from the
titles. Two greps at the dictionary said one of them had an answer already.*

---

## Not a priority band, but do it first

**#123** is someone outside the project offering patches — NPEs turned into exceptions that say what
failed in WURCS terms. **Answered on 2026-08-14** with how to send them, what shape a test wants, and
two recent refusals to imitate. They said about a week, and the patches arrived: **PR #227**, open
since 2026-08-19 and unreviewed as of 2026-09-11. The next move is ours, and a contributor waiting
three weeks on a review is worse than any item in the bands below.

Contribution questions are answered ahead of the queue, whatever band the code would fall in.

---

## The order to take them in

The bands say what a thing costs. This says what to pick up, and it is the bands applied twice: once
for cost, once for what it costs to leave the tracker saying something untrue.

**The list below is a record of 2026-08-15, kept because the reasoning is worth more than the ticks.**
Everything on it is done. What is left to pick up is at the bottom.

1. ~~**One sitting of tracker work.**~~ Nine fixed issues were open and two measured faults were filed
   nowhere — the same failure, and the one this file is most emphatic about: *silence outranks severity*.
   Ten issues closed with their measurements, #203 and #204 filed.
2. ~~**#203, the ion adduct isotope.**~~ The last thing handing someone a wrong number they could not see
   was wrong.
3. ~~**#204, a repeat unit's position lost on a round trip.**~~ Dropped on the way in, not out, and one
   line.
4. ~~**#29, the bisecting GlcNAc.**~~ Reached every picture, so checked in all four orientations.
5. ~~**#183, the doubled export error.**~~ Mechanism real; the reported sequence no longer fails, and that
   was recorded rather than claimed as verified.
6. ~~**A code review, six work packages.**~~ Taken with the test gate first, because it was why the other
   five could ship: 1.36.0, 1.37.0 and 1.38.0 all went out with `-DskipTests` in every installer workflow.
   The XML readers were leaking local files through external entities; save, open and export all reported
   failures as successes.
7. ~~**The save path, three rounds of review.**~~ Transactional replacement, a channel leak, then access
   control — owner, group, ACL and extended attributes, or the file is not replaced at all — then symbolic
   links, then the boundary where a link chain has no end. One of my judgements was overturned in the
   middle of it and the correction is on #214.
8. ~~**#211, a regression 1.37.0 had shipped.**~~ Found by taking glycanbuilder2web forward, not by the
   suite. An acyl could no longer substitute the amine a residue carries, and a copy silently dropped a
   residue.
9. ~~**#219 and #221, composition.**~~ A composition containing sialic acid exported as an empty file
   through four releases, and "Muramic Acid" built a MurNAc.

---

## Before fixing anything: what a safe-to-change test base would take

Asked on 2026-09-11 - build the verification first, so that fixing an issue stops creating the next
one. This is the survey, measured rather than estimated. **No source has been changed.**

### Where the two projects actually stand

Line coverage, measured on 2026-09-11 by running each suite under JaCoCo:

| | tests | line coverage | runtime |
|---|---|---|---|
| **GlycanBuilder2** | 226 | **28.5%** (6,727 / 23,615) | 11.5 s |
| **glycanbuilder2web** | 583 | **81.8%** (5,754 / 7,036) | 45.9 s |

**The library everything is built on is the least tested part of the stack, by a wide margin.** The
web application, which sits on top of it and is a quarter of its size, has nearly three times the
coverage. Both suites run in under a minute, so nothing about the current setup argues against
adding a great deal more.

Where the library's coverage goes:

| package | cov | lines | |
|---|---|---|---|
| `…glycanbuilder` (model, canvas, document) | **14.8%** | 10,234 | the core |
| `…glycanbuilder.util` | **7.4%** | 3,148 | dialogs and helpers |
| `…renderutil` | 41.8% | 3,396 | drawing |
| `…massutil` | 59.8% | 786 | |
| `…util.exchange.importer` / `.exporter` | **84.0% / 84.9%** | 1,593 | the WURCS conversion - the recent work |
| `…dataset` | 86.6% | 373 | |
| `converterGWS` | 74.7% | 257 | |
| `converterKCF`, `converterGlycoMinds` | **0%** | 606 | two formats the application offers and nothing tests |

### The classes where a change is invisible

Ranked by unreached lines - this is the list of places where an edit can go wrong silently:

| class | cov | unreached | |
|---|---|---|---|
| `GlycanCanvas` | 0% | 2,259 | Swing; expensive to test, and the largest single hole |
| `BBoxManager` | 25.1% | 656 | **layout arithmetic, no UI** |
| `Glycan` | 26.5% | 630 | **the structure model** |
| `GlycanDocument` | 17.7% | 549 | **open, save, import, merge** |
| `AbstractGlycanRenderer` | 42.1% | 472 | |
| `Fragmenter` | 0% | 410 | **fragmentation, entirely untested** |
| `Residue` | 44.5% | 383 | **where #238 lives** |
| `KCFParser` | 0% | 351 | |
| the dialogs | 0% | ~2,100 | Swing |

Split by kind: **~6,900 unreached lines are Swing** - dialogs, canvas, menus - where unit tests are
genuinely expensive. The other **~9,900 are model, conversion and layout**: plain Java, no display
needed, testable today with nothing new installed. That second number is the real finding.

**`Residue.addChild` is not untested** - eleven test classes touch it, including
`OnePositionOneBondTest`, which is #34's own test. It still shipped #238. Coverage is necessary and
not sufficient: the line was executed, the case was not.

### What "safe to change" needs, and in what order

Not "cover everything" - that is how a test effort dies. Four stages, each of which makes the next
cheaper, and each worth stopping at if the appetite runs out.

**Stage 1 - a floor under what already works** (a few days). Two corpus tests with committed
baselines: every WURCS the registry holds, read and written back; and every format the application
offers, over the same structures. Records what each input does today, fails when any input changes
category. This is the stage that would have caught #238, #239 and #236, and it needs no new
infrastructure - `tests.yml` already gates every pull request.

**Stage 2 - the model and the layout** (one to two weeks). `Glycan`, `Residue`, `GlycanDocument`,
`BBoxManager`, `Fragmenter`: ~2,600 unreached lines of plain Java. Attaching, detaching, positions,
brackets, repeats, copy, merge, undo. Six of the eighteen new issues are copy-and-paste and
selection, an area with **no tests at all**, and #200 and #230/#234 are structure-model changes that
cannot be attempted safely until this exists.

**Stage 3 - the picture** (one week, then upkeep). Render a fixed corpus in four orientations and
every notation, hash it, compare. #29 and #88 were drawing regressions found by eye; #91, #232, #233
are drawing issues with nothing watching them. The upkeep cost is real: fonts and platforms move, so
this wants a tolerance and a documented way to re-bless.

**Stage 4 - the Swing surface** (open-ended). ~6,900 lines behind dialogs and the canvas. The web
application solves the same problem with Karibu, which builds a UI without a browser and gets it to
81.8%; the desktop equivalent is AssertJ-Swing or FEST, and it is the most expensive stage for the
least return. Worth doing last, or never, if stages 1-3 hold.

**What crosses both projects.** The web application is the better-tested of the two and takes the
library as a dependency, so its 583 tests are already an integration test of the library - that is
how #221 was caught. Making that deliberate is cheap: run the web suite against a release candidate
of the library before tagging, not after. Stage 1's corpus belongs in the library, where both
consumers inherit it.

### Could a suite have caught #238? Yes - and the present one says why it did not

Asked on 2026-09-11, after #238 turned out to be a regression this project shipped in 1.37.0 and
nobody noticed for four releases. This section is the feasibility answer, not a plan of work.

### What exists

**49 test classes, 243 tests** in this repository, and they are not thin - the save path, the XML
readers, position rules, masses, composition encoding and three drawing invariants all have tests,
most written in the last month. glycanbuilder2web has 77 test classes and 583 tests on top.

**And #238 still got through.** The reason is visible in the one test that should have caught it,
`RegisteredGlycanWURCSRoundTripTest`, which says so itself:

> Only structures that survive the round trip today are asserted here. Ambiguous and repeating
> structures (undetermined linkages, `~n` repeats, `u`/`h` skeletons) do not, and are left out
> rather than pinned to their current broken behaviour.

Forty-three sequences, hand-picked, **selected for passing**. A curated allow-list cannot catch a
regression in a structure that was never on the list, and it quietly guarantees that the hardest
inputs - the ones most likely to break - are the ones not watched. #238's sequence was not on it.
Neither was #239's, which is a single residue.

### What would have caught it

A **corpus test**: every WURCS the registry holds, read and written back, with the result compared
against a committed baseline of what each sequence does today.

The baseline is the part that matters, and it is what turns the present approach on its head.
Instead of asserting "these 43 round-trip", it records "these N round-trip, these M do not, and here
is how each one fails", and fails the build when **any sequence changes category** - including a
sequence that starts working, which is how a fix gets noticed and the baseline updated. #238 would
have moved from *round-trips* to *corrupted* in 1.37.0 and stopped the release.

### Is it feasible

| question | answer |
|---|---|
| Is the corpus obtainable? | **Yes - it is a public download, and it has already been fetched.** `https://data.glygen.org/ln2downloads/glycan/others/wurcs.zip`, 22 MB, **98,829 WURCS sequences**, one file per GlyTouCan accession. It is N. Edwards' own group's dump (glygen-glycan-data), which is how #227 was built; the SPARQL behind it is public too, in `PyGly/smw/glycandata/queries/wurcs.sparql2zip`. Nobody needs to be asked |
| Is it fast enough for CI? | Not at full size, on every pull request. A stratified sample committed to the repository - a few thousand chosen to cover skeletons, substituents, repeats, ambiguity, compositions - runs in seconds. The full run belongs on a schedule, or before a release |
| Does it need new infrastructure? | **No.** `tests.yml` already gates every pull request and is called by `release.yml`; a corpus test is another JUnit class. The one new thing is a baseline file in the repository and the discipline of updating it deliberately |
| What else is uncovered? | **GWS names** - the `/` finding above was measured by hand and nothing tests it. **Drawing** has three invariant tests and no image comparison, so #91, #232, #233 have nothing watching them. **Copy and paste** - six open issues, zero tests |

### What the corpus says today

2,000 sequences sampled evenly across the 98,829, read and written back on 1.40.0. **6.4 seconds.**

| outcome | count | share |
|---|---|---|
| round-trips identically | 1,039 | **52.0%** |
| **writes an empty string** | 449 | **22.5%** |
| writes a different sequence | 168 | 8.4% |
| refuses to import | 344 | 17.2% |

**Just over half of registered structures survive a round trip.** The 22.5% that write *nothing* are
the worst of it - the same silence as #219, at scale and unmeasured until now.

Read the caveat before quoting the number: this dump is from 2017, so some failures are the corpus
being old rather than the code being wrong - 9 of the sampled failures are the repeat separator
`:` that WURCS has since replaced with `-`. A current dump would put the honest figure somewhere
above 52%. That is an argument for refreshing the corpus, not for not having one.

Two of the 168 altered sequences are issues already open, which is the point:

```
G01309UP  [a11221h-1a_1-?]  ->  [a11221h-1a_1-?_1-?]      #239, the doubled unknown ring
G03468DF  WURCS=2.0/1,6,7/  ->  WURCS=2.0/1,6,6/          a linkage disappears - not filed
```

The second is new. A ring-closure bond is lost on the way through, which is #200's territory and has
nobody watching it.

**This is the baseline.** Committing these four counts and the per-accession outcome, and failing the
build when any accession changes category, is Stage 1 - and the measurement above took one afternoon
and no new infrastructure.

### The same measurement for GlycoCT and GWS

Asked for on 2026-09-11, and it changed what we know about #34.

**GlycoCT condensed** - the same GlyGen download publishes `glycoct.zip`, 41,553 structures. 2,000
sampled, 6.0 s:

| outcome | count | share |
|---|---|---|
| round-trips identically | 1,521 | **76.1%** |
| writes an empty string | 6 | 0.3% |
| writes a different sequence | 146 | 7.3% |
| refuses to import | 327 | 16.4% |

Better than WURCS, and the refusals are mostly named residues the dictionary does not carry -
`L-gro-a-D-manHepp`, `a-L-N-enx-thrHexA`, `?-Gro-ol`. Worth knowing before the first attempt:
`GlycoCTParser` reads **XML** GlycoCT; condensed is `GlycoCTCondensedParser`, and running the corpus
through the wrong one fails all 2,000 on an XML prolog error.

**GWS** has no published corpus, so it was measured the way it is actually used - as the save format:
WURCS in, GWS out, read the GWS back, WURCS out, compare. 1,631 structures, 9.7 s:

| outcome | count | share |
|---|---|---|
| survives being saved and reopened | 993 | **60.9%** |
| comes back empty | 47 | 2.9% |
| comes back different | 100 | 6.1% |
| **cannot be read back at all** | 491 | **30.1%** |

### #34 is not about a slash

The largest single group of those 491 failures is this, and it reproduces with **one residue**:

```
WURCS in : WURCS=2.0/1,1,0/[a21122h-1a_1-5]/1/
GWS  out : freeEnd--1a1D-D-gro-D-galHep,p$MONO,Und,0,0,freeEnd
read back: invalid format for linkage: -gro-D-galHep,p
```

`D-gro-D-galHep` is a **heptose from this project's own residue dictionary**. Draw it, save the
`.gws`, reopen it: the file is broken. The name carries hyphens, and `-` ends a name in the GWS
grammar - exactly the fault documented above for `Ser/Thr`, reached without anyone typing anything
unusual.

Six of the 134 dictionary residue types have names GWS cannot write:

```
L-gro-D-manHep   D-gro-D-manHep   Tri-P   (S)Lac   (R)Lac   (X)Lac
```

So the character-class question is not a feature request from one user. **The save format cannot
represent six residues the builder offers**, and has not been able to for as long as those residues
have existed. That moves the escape from "nice for aglycon labels" to P2 - work is lost, on reopening
- and it decides the open question in that section: the escape has to cover the dictionary's own
names, not just the characters someone asked for.

One more thing the run turned up, unexplained: some written GWS contains `NaN`
(`--NaNL-L-gro-...`). Not filed, not investigated.


### What a full suite would be, in order of what it buys

1. **WURCS corpus round trip with a baseline.** Catches #238, #239, #236, and the class they belong
   to. The single highest-value piece, and the one that pays for itself the first time it fires.
2. **GWS round trip over an adversarial name set.** Every character the grammar reserves, in a
   residue name, in a reducing end name, inside a repeat. Would have found the `/` fault, and the
   `]`/`^` typo, without a user asking for `Ser/Thr`.
3. **A rendering baseline.** Render a fixed set of structures in all four orientations and all
   notations, hash the image, compare. Cheap to run, awkward to maintain across font changes - but
   it is the only thing that would catch a drawing regression, and #29 and #88 were both drawing
   regressions found by eye.
4. **Copy, paste and selection.** Six open issues say this area has no safety net at all.
5. **A mass baseline.** Every residue and composition the dictionaries offer, weighed, pinned. #221
   (Mur built a MurNAc) changed masses correctly; nothing would have told us if it had changed one
   incorrectly.

### The honest caveat

None of this is free to maintain. A baseline that nobody updates deliberately becomes a file people
regenerate to make the build green, and then it is worse than no test at all, because it looks like
one. The discipline it needs is a sentence in the review checklist: **a changed baseline is a change
to explain, not a change to accept.**

## What `$` means in a GWS string

Written down on 2026-09-11 because it was assumed to be free text, and it is not. Read out of
`GWSParser.toString` / `fromString`, `MassOptions.toString` / `fromString`, and confirmed by running
each case.

### The shape

```
<structure>$<isotope>,<derivatization>,<ion cloud>,<neutral exchanges>[,<reducing end>]
              0          1                2              3                4
```

`freeEnd--?b1D-GalNAc,p$MONO,Und,0,0,freeEnd`

- The split is `str.indexOf('$')` - **the first** `$`, not the last.
- The tail is split on `,` by `TextUtils.tokenize`, which **discards empty tokens**: `a,,b` is two
  tokens, not three. Fields are positional, so an empty one does not hold its place - it shifts
  everything after it left.
- Fields 0-3 are **required**. Field 4 is optional.

### What it accepts, measured

| tail | result |
|---|---|
| `MONO,Und,0,0,freeEnd` | the normal form |
| `MONO,Und,0,0` | fine - reducing end falls back to the default |
| `MONO,Und,0` | **`IndexOutOfBoundsException`** |
| `MONO,Und` / empty | **`IndexOutOfBoundsException`** |
| `MONO,Und,0,0,freeEnd,hello` | **reads, and `hello` is silently dropped when it is written again** |
| `MONO,Und,0,0,freeEnd,label=Ser/Thr,a,b,c` | the same - read, ignored, gone on the next save |
| `MONO,Und,0,0,NoSuchReducingEnd` | **accepted**, and written back unchanged |
| `MONO,Und,0,0,freeEnd$extra` | accepted; the second `$` is ordinary text, and the reducing end is now literally named `freeEnd$extra` |

**So the belief is half right.** Anything *after the fifth field* is tolerated on the way in and
thrown away on the way out. Nothing is free-form before that: four fields must be present, in order,
non-empty.

### Why "accepted" is not the same as "safe"

`ResidueDictionary.findResidueType` never refuses a name. Unknown names go to
`ResidueType.createUnknown`, and a name containing `=` is read as `name=massu` and becomes a custom
reducing end. That is how `Ser_Thr=87.0320u` works - and it is also why a typo in field 4 produces a
residue type rather than an error.

### Using it to carry something

Tempting, for #34: put the display label after the reducing end, where the reader ignores it. It does
not work, and the table says why - **the writer does not carry it**. `MassOptions.toString` rebuilds
the tail from five values, so anything appended survives exactly one read and is gone the moment the
document is saved. A label has to live somewhere the writer writes.

If the tail is ever to carry more, it needs the extension designed rather than discovered: a keyed
field (`k=v`) so position stops mattering, tolerated by old readers because they ignore tokens past
the fifth - which, by the measurement above, they already do.

### Two faults found while writing this down

- **`AVG,perMe,Na1,0,redEnd` reads and comes back as `AVG,perMe,0,0,redEnd`.** The ion cloud is lost.
  `IonCloud.toString` writes `Na` for one sodium and `2Na` for two, so `Na1` is not the spelling it
  writes - but it is the spelling a person would guess, and losing an adduct silently is the same
  class of fault as #203. Whether the correct spelling survives is not yet measured.
- Field 4 is a residue **name**, unquoted, in a comma-separated list. A custom reducing end whose
  name contains a comma cannot be read back, the same way `-` and `/` cannot. Not measured; follows
  from the grammar.

## Settling the GWS specification: what it would take, and what is in the way

The proposal, 2026-09-11: rework `GWSParser.fromString` so structures round-trip, and settle the GWS
notation first, since the reader and the writer currently speak different languages. The sequencing
instinct is right - a format whose writer emits what its reader rejects is a definition problem, not
a local bug - and five things stand in the way of taking it in that order.

**1. There is no specification to settle; the existing files are the specification.** GWS's authority
is `GWSParser` plus twenty years of `.gws` written by GlycoWorkbench and GlycanBuilder. Writing a
grammar from the regex is an afternoon. Knowing whether that grammar describes the files people have
is the hard part, and **there are no files to check against**: zero `.gws` in this repository,
glycanbuilder2web, glycanbuilder2-refactor or glycoworkbench-desktop.

**2. The reader and the writer must both be covered, and choosing between them is not a technical
call.** Six of 134 dictionary residues are writable and unreadable. Widening the reader rescues files
that already exist but leaves new files unopenable by old readers; narrowing the writer protects the
future and abandons what is already written.

**3. Freezing the specification first blocks repairs that need no specification.** Three measured
faults are bugs under any reading: the heptose that cannot be reopened, a `$` inside a name that
empties the structure **with no error at all**, and `NaN` appearing in written output.

**4. There is more than one reader.** GlycanBuilder2, glycanbuilder2-refactor (1.25.6-era, fifteen
releases behind) and GlycoWorkbench. A file written by a new GlycanBuilder2 has to open in software
that will not receive this change for a long time - and whether it ever does is #226's open question.

**5. Rewriting the parser without the baseline hides the result.** The before is measured: 60.9%
survive, 2.9% come back empty, 6.1% come back different, 30.1% cannot be read back. The harness runs
in ten seconds. Without it, a rewrite cannot be told from a reshuffle.

Whatever is written should say what "done" means: a grammar, a conformance corpus of inputs with
expected outcomes, and a statement of which versions are required to read the result. Otherwise
"specification" becomes a document nothing can be tested against.

### Where a GWS corpus could come from - investigated, 2026-09-11

WURCS and GlycoCT both have public corpora. **GWS does not**, and the search is now closed enough to
record.

| source | result |
|---|---|
| `sparql.glygen.org` | **Does not hold GWS.** The store keeps six sequence formats - glycam, glycoct, inchi, iupac, smiles_isomeric, wurcs - and GlycoWorkbench is not among them |
| `sparql.glygen.org/ln2triplestoredata/triples.tar.gz` | 1.94 GB, 2026-05-15. Same store, so the predicate is not in it either; downloading it would not help |
| `data.glygen.org/…/others/gwb.zip`, `gws.zip` | Not published. Both return 3,454 bytes of the site's HTML shell, where `wurcs.zip` returns 22 MB of data |
| PyGly `smw/glycandata/queries/gwb.sparql2zip` | Real, and it proves GWS exists **somewhere**: it selects `glycandata:property "GlycoWorkBench"`. But that vocabulary is `glyomics.org/glycandata#`, an internal store - the same query against the public endpoint returns nothing |
| Generating GWS from the WURCS corpus | Already done, 1,631 structures. **Measures the wrong thing for this purpose** - see below |
| Real `.gws` files: users' saved work, papers' supplements | Unexplored, and the only true source |

Worth knowing for whoever queries GlyGen next: the endpoint is **not** `/sparql`, which 404s. It is
`POST /cgi-bin/get_triples.py` with `injson={"qs":"<query>","format":"JSON"}`.

**The distinction that decides this.** Generating GWS from WURCS answers "can today's GlycanBuilder2
read what today's GlycanBuilder2 writes". The specification needs "can we read what GlycoWorkbench
wrote", and no amount of generating produces that. So the corpus question narrows to one request -
glygen-glycan-data's internal store - with no fallback.

**If it cannot be had**, the specification has to be founded on the current parser plus a stated
compatibility policy, and **must say so on its first page**. An unverifiable premise written as
though it were evidence is worse than an admitted gap.

### It can be had, in part - fourteen files GlycoWorkbench actually wrote

Suggested on 2026-09-11 that MolecularFramework, written around the first GlycanBuilder, might serve
as a reference. **It does not**: 16 io formats - GlycoCT, Glyde, Linucs, OGBI, bcsdb, cabosml,
carbbank, cfg, glycam, glycobase, glycosuite, iupac, kcf, namespace, ncfg, simglycan - and GWS is
not among them. Nor are the names that break GWS from there: `L-gro-D-manHep`, `Tri-P`, `(S)Lac` are
defined in this project's own `conf/residue_types`.

Following the thought to its source did work. **`glycoinfo/eurocarbdb` holds the original
GlycoWorkbench, and its `application/GlycoWorkbench/examples/` carries 14 `.gws` files** - written by
GlycoWorkbench, not generated by us. That is the conformance corpus this section said we did not
have. Small, but authentic, and it is the only sample of the format as it was actually produced.

Run through today's reader, splitting on `;` the way `GlycanDocument` does:

| | count |
|---|---|
| read and written back identically | **0** |
| read, written back differently | **14** |
| cannot be read | **0** |

**All fourteen read.** The difference is the same in every one: the writer states the mass options
the 2008 writer left implicit. `…$MONO,Und,Na,0` comes back as `…$MONO,Und,Na,0,freeEnd`, and a file
with no `$` at all gains `$MONO,Und,0,0,freeEnd`. That is the writer being more explicit, not damage
- but it does mean **no file GlycoWorkbench wrote round-trips unchanged**, which matters for a
baseline: the comparison has to be structural, not textual, or every file is a false alarm.

It also settles the ion cloud spelling: **`Na` is what GlycoWorkbench wrote**, so the earlier `Na1`
that came back as `0` was a guess at the syntax rather than a fault in `IonCloud`.

**A correction.** These fourteen were first reported here as "two cannot be read at all", with
`dermatan_sulfate.gws` and `nglycan_2605.gws` failing on a leading `0;`. That was the measurement's
fault, not the parser's: both files hold **two structures separated by `;`**, and they had been
handed to `GWSParser.readGlycan`, which reads one. The `0;` in the error was the end of the first
structure's mass options meeting the separator. Through `GlycanDocument`'s own path, which splits on
`;`, both read correctly. The claim that GlycanBuilder2 cannot open GlycoWorkbench's own examples was
wrong and is withdrawn.

So the specification has evidence to rest on, and the evidence is better news than the first reading
of it: **today's reader does accept what GlycoWorkbench wrote**, at least across these fourteen. What
it does not do is write it back the same way, which is a fact a baseline has to be built around
rather than a fault to fix.

These 14 files belong in the repository as test data before anything is rewritten. They cost nothing
to add, they are the only witness we have to the format as it was actually produced, and two of them
are multi-structure documents - which is itself worth having under test, since that path is separate
from the one every other test uses.


## What is left

Re-counted 2026-09-11: **47 open**, of which 18 arrived after the last pass. "Nothing is actionable"
was true on 2026-08-15 and is not true now.

| what | how many | which |
|---|---|---|
| **actionable now** | 12 | **#238** (ours, P1), #239, #232, #233, #236 + #95, #237, #229/#240/#241/#242/#244/#245 as one piece |
| waiting on somebody else | 11 | #17, #57, #58, #66, #83, #16, #183, #189, #190, #123, #222 |
| waiting on a decision — chemistry or product | 7 | #220, #7, #100, #109, #117, #182, #235 |
| in somebody else's repository | 3 | #175 (glycoinfo/MolecularFramework#4, glycoinfo/ResourcesDB#1), #226 |
| wishes rather than work | 5 | #41, #93, #177, #181, #184 |
| drawing, P3 | 4 | #6, #20, #58, #91 |
| structure-model changes, neither small | 3 | #200, #181, #230/#231/#234 |

**The nudges are four weeks overdue.** They were held until Monday 2026-08-17 at the maintainer's
request — I. Yamada, 2026-08-15 — and the hold has long since expired: the questions on the eleven
waiting issues went out on 2026-08-14 and 08-15, and it is now 2026-09-11 with no reply on any of
them. #185 came off that list by being answered; the rest are simply waiting.

**There is an open pull request now.** #227, from N. Edwards, turns the conversion's raw
`NullPointerException`s and `StringIndexOutOfBounds` into a named exception carrying the element that
failed — which is #123, and reaches #228 and #236. Reviewed but not commented on: it should use a
field and an accessor rather than concatenating the element into the message, mirroring
wurcsframework's own `WURCSFormatException(message, input)` / `getInputString()`; and the
`RuntimeException` it introduces in `TrivialNameConverter` is the one place it does not follow its own
rule — no named type, no cause, no element.

## Where this stands, for whoever picks it up next

Re-checked against the tracker on **2026-09-11**. The pass below was written on 2026-08-15 after
1.39.0 and is kept because the reasoning still holds; what changed since is at the top of this file -
eighteen new issues, one of them a regression of ours, and a pull request nobody has answered.

**1.39.0 is out**, and is the first release a test gate stood in front of. `origin/develop` and
`origin/master` both read it — checked against the remote, not the local refs, which is the check 1.35.0
was lost by. `v1.39.0` tags the merge commit, `releases/latest` resolves to it, and the run's own log shows
`test` finishing before any installer started.

**On `develop` and not yet released**: #219 (a composition containing sialic acid), #221 (Mur built a
MurNAc), the reason an export came out blank, and the release workflow attaching only installers. 226 tests
pass with all of it.

**Three things worth carrying forward**, each of which cost time to learn:

- **There is a third residue dictionary.** `conf/compositions` defines residue types and they reach
  `ResidueDictionary`, so `Neu5Ac` is already a type of its own — which is why the sialic acid names could
  not be fixed with a synonym, and why trying shadowed a real type and broke the position rules.
- **A test that builds its input the way the code under test likes it will not find a fault on the way in.**
  The composition encoder's tests asked for residues *by the encoder's own names*, so a composition with
  sialic acid failed for four releases with a green suite.
- **Model and IO fixes reach a consumer with the jar; drawing and desktop-UI fixes do not.** Found twice
  taking glycanbuilder2web forward, and the reason to measure rather than assume when a library layout fix
  is supposed to have arrived.

**The release page's label is the last step and it is easy to miss.** The workflow publishes as a
pre-release, and `releases/latest` — which is what `UpdateCheck` asks — skips those, so until somebody marks
it Latest the newest release is invisible to the update check. v1.38.0 was marked by hand; verified through
the API, since the release page and `gh release list` have both misreported this.

Every release carries the same five installers. The Windows `.msix` is not one of them: it is built and
uploaded to Partner Center by hand, which is why the Store sat on 1.28.0 for seven releases (#180,
closed 2026-08-17 once it was updated). It is the one release step outside CI, and the lag it produces
is invisible from here — it took a reporter unable to retest #185 to surface it.

**Still with the maintainer, for every release**: `mvn deploy` from `master` at the versioned commit,
and the Windows `.msix` to Partner Center. Neither is a step to take unasked.

**One open pull request**: #227, from N. Edwards, since 2026-08-19. See "Not a priority band, but do
it first".

**Waiting on somebody else**, and worth a look before starting anything new — several of these may be
closable:

| # | Waiting for |
|---|---|
| #17 | the reporter to confirm the import is what they meant, or close it |
| #57 | which label shows 3 — the round trip dropping `l1` → `l?` is #204 now |
| #58 | which part of the layout is wrong |
| #66 | a retest on 1.36+ and the GWS; it does not reproduce here |
| #83 | whether the symbol looks *rotated* or merely *placed differently* |
| #222 | a retest on 1.39.0 with the sulfate at 2 rather than at N — and the advice that sent them to N was mine, corrected on #189 |
| #183 | a structure that still refuses to export — the one in the report writes fine now |
| #16 | whether to split it per modification |
| #189 | whether they mean GlcNS or a sulfate on an already-acetylated nitrogen — a chemistry question, and the only thing left on it |
| #190 | the WURCS or GlycoCT for G13093, for whoever takes #181 |
| #123 | the contributor said about a week, from 2026-08-14 |

**The hold on the nudges expired on Monday 2026-08-17** and nothing was sent. Four weeks on, none of
the eleven has replied, and the questions themselves are from 2026-08-14 and 08-15. Whatever is sent
now should probably say so rather than pretend the gap did not happen.

**Deliberately not started**: #175 (jdom2) and taking glycanbuilder2web to 1.38.0 are both on hold at
the maintainer's request.

**The reducing-end note in "Before ranking anything, verify it" is the trap most likely to waste your
first hour.** It has caught two measurements so far.
