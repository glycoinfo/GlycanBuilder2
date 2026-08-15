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

## P1 — a wrong answer nobody can see is wrong

| # | Title | What is known |
|---|---|---|
| ~~#132~~ | ~~`v_stripes`/`h_stripes` never painted~~ | **Done.** Three bars, clipped to the outline. The test asserts the drawn image and, separately, that the stripes exist — Tal is green and All is blue, so comparing the pair passes on colour alone |
| ~~#107~~ | ~~Composition export to WURCS fails~~ | **Done.** `org.glycoinfo.application.glycanbuilder.composition` builds the composition as unlinked nodes and lets `WURCSFactory` canonicalize, which is what glycompconverter does. No new dependency; output pinned against that implementation's, residue by residue |
| ~~#219~~ | ~~A composition with sialic acid exports as an empty file~~ | **Fixed, on `develop`.** The residue type is `NeuAc` and composition WURCS calls it `Neu5Ac`; nothing translated. Not fixable by renaming — `Neu5Ac` is already a type, from `conf/compositions`. The table lives in the encoder now, where glycanbuilder2web has always kept its own |
| ~~#127~~ | ~~`MassOptions.ISOTOPE` has no effect~~ | **Done for the neutral mass**, 1.36.0, and closed. Residues, water, hydrogen and the derivatization all follow the choice; measured against literature (Glc 180.156, Man₃GlcNAc₂ 910.82) |
| ~~#203~~ | ~~An m/z pairs an average neutral mass with a monoisotopic adduct~~ | **Fixed in PR #205, merged to `develop` on 2026-08-15.** `IonCloud` captured an adduct's mass when the ion was set rather than when it was computed, so the choice could not reach it. `computeMZ`/`computeMass` take the flag now and `getIonsMass(boolean)` recomputes the total; a charge given an explicit mass keeps it. The test is on potassium and chloride, with sodium as the control — one stable isotope, so a sodiated m/z was right by accident and a test on the default adduct would have passed before the fix |
| **#185** | EPS/PS/PDF export writes 0 bytes silently | **Half done.** A failed transcode is now refused by name instead of being written as an empty file — it had been logged and returned as null, and the null was written. The underlying Windows failure does not reproduce here: all five formats write on macOS with the pinned batik 1.19 / fop 2.11. Waiting on a retest from the reporter |
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
| #172 | Check for updates at startup | Waiting on the Microsoft Store question |
| #109 | Compositions with linkage (lactonised sialic acid) | A WURCS question more than a GB2 one |
| #7 | Review the Add-composition monosaccharide list | |
| #16 | A standing list of modifications that were not handled | Fourteen WURCS collected since 2021, almost certainly not one fault — `*OSO`, `*=NO` and the rest fail at different points and some read now. Asked the reporter whether to re-measure each on 1.35.2, close this, and file one issue per modification that still fails. Overlaps #181 |

## P5 — plumbing

| # | Title | Note |
|---|---|---|
| #175 | Get off `org.jdom:jdom` | The reachable path was closed in 1.35.2; the Dependabot alert stays open until the dependency moves. #158 folds into this |
| #180 | Microsoft Store still on 1.28.0 | A release-process problem, not a code one |

## P6 — answers rather than changes

#94 (which classes read a WURCS) · #117 (what the correct composition is) ·
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
two recent refusals to imitate. They said about a week, so nothing is owed here until roughly the 21st;
the next move is theirs.

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

## What is left, and why none of it can be picked up today

Thirty issues are open and **none is actionable without either a reply or a decision.** That is worth
stating plainly rather than leaving the list to imply there is work going begging.

| what | how many | which |
|---|---|---|
| waiting on somebody else | 12 | #17, #57, #58, #66, #83, #185, #16, #183, #189, #190, #123, #222 |
| waiting on a decision — chemistry or product | 6 | #220, #7, #100, #109, #117, #182 |
| on hold at the maintainer's request | 2 | #175, #180 |
| wishes rather than work | 8 | #41, #93, #94, #95, #172, #177, #181, #184 |
| drawing, P3 | 4 | #6, #20, #58, #91 |
| structure-model changes, neither small | 2 | #200, #181 |

**The nudges are held until Monday 2026-08-17** — I. Yamada, 2026-08-15. They were written and ready on
the Saturday; holding them is deliberate, so a quiet weekend on this list is not a stall.

**#222 is the one that looks actionable and is not.** A sulfate attaches at a GlcN's `N` and then writes
nothing, and refusing the attachment is the better answer — but that is a change to the position rules,
and changes of exactly that shape have produced two regressions in two days: 1.37.0's #211, and a synonym
that shadowed a real residue type on the 15th. It waits for the reporter's retest, which costs nothing.

---

## Where this stands, for whoever picks it up next

Re-checked against the tracker on 2026-08-15, after 1.39.0.

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

Every release carries the same five installers. What is missing on all of them is the Windows `.msix`,
which is uploaded by hand and is what #180 is about.

**Still with the maintainer, for every release**: `mvn deploy` from `master` at the versioned commit,
and the Windows `.msix` to Partner Center. Neither is a step to take unasked.

**No open pull requests.**

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
| #185 | a retest on Windows; all five formats write here |
| #183 | a structure that still refuses to export — the one in the report writes fine now |
| #16 | whether to split it per modification |
| #189 | whether they mean GlcNS or a sulfate on an already-acetylated nitrogen — a chemistry question, and the only thing left on it |
| #190 | the WURCS or GlycoCT for G13093, for whoever takes #181 |
| #123 | the contributor said about a week, from 2026-08-14 |

**Held until Monday 2026-08-17**: the nudges in the table above. Written and ready on Saturday the 15th, at
the maintainer's request — see step 6 of "The order to take them in". A quiet weekend on this list is
deliberate.

**Deliberately not started**: #175 (jdom2) and taking glycanbuilder2web to 1.38.0 are both on hold at
the maintainer's request.

**The reducing-end note in "Before ranking anything, verify it" is the trap most likely to waste your
first hour.** It has caught two measurements so far.
