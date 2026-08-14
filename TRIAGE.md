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

## Verified, and can be closed

Measured on 1.35.2. Each needs a comment carrying the measurement, not a bare close.

| # | Title | What was measured |
|---|---|---|
| #67 | Composition m/z is incorrect | Hex₃HexNAc₂ as a composition weighs 910.3278, as does the same glycan linked. Fixed by the 1.34.1–1.34.3 composition work |
| #150 | Ambiguous linkage read as the parent's position | Fixed in 1.34.0, `AmbiguousLinkageKeepsUnknownPositionsTest` holds it |
| #158 | org.jdom has no fixed version | Duplicate of #175, which carries the fuller investigation |
| #17 | AUdxxxxxh no longer supported | G00771KP reads and draws. Ask the reporter to confirm before closing — it is their report |

**#125** (redrawing G11127BT) is half done: the structure no longer takes the JVM down, it is refused
with a sentence saying a ring through a bridge cannot be represented. Being unable to draw it is what
remains, and the issue should say so rather than being closed.

---

## P1 — a wrong answer nobody can see is wrong

| # | Title | What is known |
|---|---|---|
| ~~#132~~ | ~~`v_stripes`/`h_stripes` never painted~~ | **Done.** Three bars, clipped to the outline. The test asserts the drawn image and, separately, that the stripes exist — Tal is green and All is blue, so comparing the pair passes on colour alone |
| ~~#107~~ | ~~Composition export to WURCS fails~~ | **Done.** `org.glycoinfo.application.glycanbuilder.composition` builds the composition as unlinked nodes and lets `WURCSFactory` canonicalize, which is what glycompconverter does. No new dependency; output pinned against that implementation's, residue by residue |
| ~~#127~~ | ~~`MassOptions.ISOTOPE` has no effect~~ | **Done for the neutral mass.** Residues, water, hydrogen and the derivatization all follow the choice; measured against literature (Glc 180.156, Man₃GlcNAc₂ 910.82). **Still monoisotopic: the ion adducts.** `IonCloud` captures an ion's mass when the ion is set, not when the mass is computed, so an m/z carries an average neutral mass and a monoisotopic adduct. Worth its own issue |
| **#185** | EPS/PS/PDF export writes 0 bytes silently | No error, no file worth having |
| **#66** | WURCS export fails with `"_map" is null` on a heavily modified Fuc | |
| **#34** | Duplicated linkage position in a branched glycan | The position list is built without asking what the parent already carries, so an impossible structure can be drawn |

## P2 — work is lost, or the output cannot be used

| # | Title | Note |
|---|---|---|
| **#186** | PNG background not transparent | These three are one problem — "I cannot put this in a paper" — |
| **#188** | SVG gives every character its own white background | and are worth doing together rather than |
| **#187** | Ungrouping in PowerPoint destroys Fuc and Man | one at a time |
| **#106** | Saving on close offers Save As for an already-saved file | |
| **#178** | "Open additional document" leaves the document counted as unchanged, so it closes without warning | |
| **#179** | "Remember files after restarting" carries a customised reducing end into newly imported structures | |

## P3 — visibly wrong

#29 (bisecting GlcNAc position) · #57 (repeat-unit linkage position) · #58 (G07957FT layout) ·
#20 (bracket not symmetric about the reducing end) · #88 (no right margin on a bridge) ·
#83 (CFG hat diamonds take orientation from the bond) · #91 (Add-structure menu misaligned) ·
#6 (fragments carrying a bridge)

## P4 — not there yet

| # | Title | Note |
|---|---|---|
| #95 | Validate a drawn structure | Before submitting to GlyTouCan. Validation code exists elsewhere and could be called |
| #100 | Substituents and defined residues in the composition builder | Overlaps #7 (which monosaccharides the list should offer); decide them together |
| #181 | No way to add deoxy / en / alditol to a monosaccharide | A regression against the old GlycoWorkbench |
| #184 | Multi-format clipboard (bitmap + text + SVG) | |
| #177 | Open a .gws by double-clicking it | Needs file association *and* accepting a path at startup |
| #41 | SNFG with linkage placement notation | CFG has it; SNFG does not |
| #93 | Nested brackets | Rare in papers, currently flattened to a composition |
| #172 | Check for updates at startup | Waiting on the Microsoft Store question |
| #109 | Compositions with linkage (lactonised sialic acid) | A WURCS question more than a GB2 one |
| #7 | Review the Add-composition monosaccharide list | |

## P5 — plumbing

| # | Title | Note |
|---|---|---|
| #175 | Get off `org.jdom:jdom` | The reachable path was closed in 1.35.2; the Dependabot alert stays open until the dependency moves. #158 folds into this |
| #180 | Microsoft Store still on 1.28.0 | A release-process problem, not a code one |

## P6 — answers rather than changes

#94 (which classes read a WURCS) · #117 (what the correct composition is) ·
#182 ("Unknown" is a misleading group name) · #189 (sulfate on a GlcNAc nitrogen) ·
#190 (a KEGG structure with ribitol)

#189 and #190 are asked as "how do I input this?" and may each turn out to be a missing capability
rather than a missing instruction. Answer them by trying it, and re-file what does not work.

---

## Not a priority band, but do it first

**#123** is someone outside the project offering patches — NPEs turned into exceptions that say what
failed in WURCS terms — and asking how to submit them. The last word is theirs: they will be back in
about a week. Replying with how to send it costs a paragraph, and not replying costs the patches.

Contribution questions are answered ahead of the queue, whatever band the code would fall in.
