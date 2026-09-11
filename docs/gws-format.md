# The GWS format

GWS — "GlycoWorkbench sequence" — is the text form GlycanBuilder and GlycoWorkbench save structures
in, and the format of a `.gws` file.

**This document has two levels, and they are two different kinds of statement.**

- **Part 1, GWS core, is normative for GlycanBuilder2.** It says what this project *must* read and
  write. Two requirements shape it: **every `.gws` that has ever been written must be readable**, and
  **a residue label may carry any character**. Where today's code falls short of that, the document
  states the requirement and the shortfall is a defect, not a clause.
- **Part 2, GWS extended, is descriptive.** It records the superset implemented in
  [GlycanCore](https://gitlab.com/glycoinfo/glycanbuilder-module/glycancore). Core does not include
  it, and this project is not to grow it: anything beyond Part 1 belongs to the extension, and the
  extension belongs to GlycanCore.

That division is the point. Core is fixed here so that existing files stop being lost; the extension
evolves there, without the two being merged.

## Status, and how to read it

Written 2026-09-11. No specification existed, so the material came from reading two parsers, from the
original 2007 sources, and from running structures through them. Each clause is tagged:

- **Measured** — a structure was written, read back, and the result observed.
- **Read** — taken from the grammar in the source, not separately exercised.
- **Required** — a normative clause in Part 1. Where today's code does not meet it, that is said
  plainly and the gap is listed in §1.7.
- **Open** — a question left undecided. Listed rather than settled.

The tags are kept even for normative clauses: knowing whether a rule rests on a measurement or on a
reading of somebody's regex is worth as much as the rule.

Sources: `GWSParser` in `org.eurocarbdb.application.glycanbuilder.converterGWS` (this project);
`GWSParser` / `GWSBuilder` in `org.glycoinfo.glycanbuilder.io` (GlycanCore 0.13.10, 2026-08-25); the
original 2007 parser, from
`storage.googleapis.com/google-code-archive-source/v2/code.google.com/glycanbuilder/source-archive.zip`;
and the 14 `.gws` examples shipped with GlycoWorkbench, in `glycoinfo/eurocarbdb` under
`application/GlycoWorkbench/examples/`.

## The rule the rest of this follows

**Read liberally, write conservatively.** *(Required.)*

| | rule |
|---|---|
| **reading** | accept every spelling that has ever been written — old hyphenated names, parenthesised names, escaped names, and structures with no `$` section |
| **writing** | emit only what another implementation can read: safe characters, or the escape of §1.3 |

The two halves are not the same grammar, and that is deliberate. A reader that accepts only what it
writes loses files; a writer that emits everything its reader accepts makes files nobody else can
open. Every clause below that differs between reading and writing does so for this reason.

---

# Part 1 — GWS core

## 1.1 A document

A `.gws` file holds one or more structures, **separated by `;`**. Splitting is the caller's job:
`GlycanDocument.parseString` does it, `GWSParser.readGlycan` does not. *(Measured: two of
GlycoWorkbench's own examples — `dermatan_sulfate.gws`, `nglycan_2605.gws` — hold two structures, and
handing a whole file to `readGlycan` fails on the separator.)*

```
document := structure ( ";" structure )*
```

There is no header, no version marker and no character-set declaration. A file is one line in
practice, though nothing requires it.

## 1.2 A structure

```
structure := subtree bracket? ( "$" mass-options )?
```

The `$` and everything after it are optional; a structure without them takes the reader's defaults.
*(Measured: 4 of the 14 GlycoWorkbench examples have no `$` at all.)*

**The split is on the first `$` in the string**, not the last. A `$` anywhere earlier — inside a
residue name, for instance — is taken as the separator. *(Measured, and the failure is silent: a
reducing end named `freeEnd$S/T` parses without error and yields an empty structure.)*

## 1.3 A residue

```
residue := anomer? configuration? name ring-form? cleavage? placement? bounding-box?

anomer        := [abo?] [1-9N?]
configuration := ("D" | "L") "-"
ring-form     := "," [?opfa]
cleavage      := "/" [a-zA-z0-9_#]+
placement     := "@" "-"? [0-9]+ "s"?
bounding-box  := "<bounding_box>" int "," int "," int "," int "</bounding_box>"
```

*(Read, from `GWSParser`'s `residue_pattern`.)*

Examples: `?b1D-GalNAc,p` · `freeEnd` · `--3S` (a sulfate at 3) · `?b1D-GlcNAc,p<bounding_box>0,0,30,30</bounding_box>`

### The name, reading and writing

**A name is read as:** *(Required.)*

```
name := ( [A-Za-z0-9_#=.] | "-" (?! "-" | [0-9]) | "(" | ")" )+
```

That is wider than the class any implementation uses today, and wider deliberately: it is what makes
**every `.gws` ever written** readable. Two additions over the historical class, each needed by names
that have been written to files and could not be read back:

- **`-`, when not followed by `-` or a digit.** `--` is a linkage and `-<digit>` is the pre-2007
  linkage form, so the restriction is exactly enough to keep both unambiguous. *(Measured: with this
  rule `D-gro-D-galHep,p` reads as the name `gro-D-galHep` after the `D-` configuration prefix,
  `Tri-P` reads whole, and `D-gro-D-galHep,p--4b1D-Gal,p` and `D-gro-D-galHep-4b1D-Gal,p` both split
  at the right place.)*
- **`(` and `)`**, for `(S)Lac`, `(R)Lac`, `(X)Lac`.

**A name is written as:** *(Required.)*

```
safe := [A-Za-z0-9_#.]        // note: no "=", see §1.3.1
```

Anything else is escaped, per §1.3.1. So a reader accepts `D-gro-D-manHep` and `(S)Lac`; a writer
emits neither, choosing a safe spelling or the escape. **This is the asymmetry of the opening rule,
and it is what lets old files be rescued without making new files unreadable elsewhere.**

### Why the historical class is not the specification

`[a-zA-z0-9_#=.]`, which both implementations use, is **not a typo-free character class**: `a-zA-z`
spans ASCII 65–122, so it also admits `[`, `\`, `]`, `^` and `` ` ``. It has been that class since
2007 and was never revisited. *(Read, both versions; GlycanCore corrected the span to `[a-zA-Z0-9_#=.]`
without changing the intended set.)* It carries no design intent worth preserving, which is why the
normative class above was derived from what files contain rather than from what the regex allows.

### 1.3.1 What a name may contain today

`[a-zA-z0-9_#=.]` is **not a typo-free character class**. `a-zA-z` spans ASCII 65–122, so it also
admits `[`, `\`, `]`, `^` and `` ` ``. This has been the class since the first version in 2007 and
has never been revisited. *(Read, both versions; GlycanCore corrected it to `[a-zA-Z0-9_#=.]`
without changing the intended set.)*

What a name may therefore contain, **measured** by writing a reducing end so named, saving and
reopening:

| character | survives | why |
|---|---|---|
| letters, digits, `_`, `.`, `#` | yes | in the class |
| `^`, `]` | yes — by accident | admitted by the `a-zA-z` span; both are reserved elsewhere in the grammar |
| `-` | **no** | `--` begins a linkage, so a single `-` ends the name |
| `/` | **no** | begins a cleavage |
| `,` | **no** | begins the ring form |
| space | **no** | not in the class |
| `%` | **no** | not in the class — so percent-encoding is unavailable |
| `$` | **no**, and silently | taken as the mass-options separator |

**Six of the 134 residue types in `conf/residue_types` have names this grammar cannot write**:
`L-gro-D-manHep`, `D-gro-D-manHep`, `Tri-P`, `(S)Lac`, `(R)Lac`, `(X)Lac`. Drawing any of them,
saving, and reopening loses the file. *(Measured: `WURCS=2.0/1,1,0/[a21122h-1a_1-5]/1/` writes
`freeEnd--1a1D-D-gro-D-galHep,p$…` and reads back as `invalid format for linkage:
-gro-D-galHep,p`.)*

This is not a fault in the grammar so much as one nobody guarded: the first dictionary had 81 residue
types and **every one fitted the class**. The six arrived later. *(Measured against the 2007
dictionary.)*

An escape using only characters the class already accepts — `_` plus hex digits, so `/` becomes
`_2F_` — round-trips today on an unmodified parser, and older readers show the escaped form rather
than failing. Not implemented; see [Open questions](#open).

### Names with any character in them: the escape

Decided 2026-09-11. A residue label may contain **any character except `=`**, and the format does not
change to allow it: the label is encoded into characters the existing grammar already accepts, so
every reader that exists today - this project, GlycanCore, GlycoWorkbench, 1.28.0 - reads the file.

#### The rules

1. **`=` is forbidden in a label.** It is the separator inside the stored name of a custom reducing
   end (`label=<mass>u`), and forbidding it is fail-safe where escaping it would be fail-open: a
   single path that forgot to encode would truncate the label *and* throw on the mass. Nothing in
   chemistry notation wants it, and **no dictionary name contains one** *(measured: 0 of 134)*.
2. **Safe characters pass through unchanged**: `A-Z a-z 0-9 _ # .`
3. **Every other character is written `_XX_`**, two upper-case hex digits. `/` → `_2F_`, `-` →
   `_2D_`, `(` → `_28_`.
4. **A label that had anything encoded is prefixed `_e_`.** A label that needed no encoding is stored
   as it is, with no prefix.
5. **A literal label beginning `_e_` is encoded**, if only its leading underscore (`_5F_`), so that
   it too carries the prefix. This is the only reason a label with no otherwise-unsafe character
   gets encoded.
6. **Non-ASCII is encoded as its UTF-8 bytes**, one `_XX_` each.

#### Worked examples

| the user types | stored as | why |
|---|---|---|
| `Ser_Thr` | `Ser_Thr` | nothing unsafe - **no prefix, no change** |
| `Ser_2F_Thr` | `Ser_2F_Thr` | nothing unsafe either; rule 4 is what keeps this distinct from the next row |
| `Ser/Thr` | `_e_Ser_2F_Thr` | `/` encoded |
| `GalNAc-Ser/Thr` | `_e_GalNAc_2D_Ser_2F_Thr` | both `-` and `/` |
| `/-` | `_e__2F__2D_` | consecutive escapes run their underscores together; still unambiguous, since a decoder reads `_`, two hex, `_` |
| `セリン` | `_e__E3__82__BB__E3__83__AA__E3__83__B3_` | UTF-8, three bytes per character |
| `_e_Ser` | `_e__5F_e_5F_Ser` | rule 5 |
| `Ser=Thr` | — | refused at input, rule 1 |

*(Every stored form above was written as a reducing-end name, read back and rendered on 1.40.0: all
eight round-trip. `_e_GalNAc_2D_Ser_2F_Thr` renders at 540×111, the UTF-8 one at 665×111.)*

#### Why the prefix, rather than escaping `_`

Without it the encoding is not reversible. `Ser/Thr` and a label typed literally as `Ser_2F_Thr`
would both be stored as `Ser_2F_Thr`, and a reader holding that string could not tell which was
meant. The alternative - escaping `_` itself everywhere - also restores reversibility, but it
rewrites every label containing an underscore, and it leaves **files written before the change**
ambiguous: they contain `_2F_` sequences that were always literal, and a decoder would silently turn
them into `/`.

The prefix solves both. **A file written before this change carries no `_e_`, so it is read
literally**, which is what it was. Nothing already saved changes meaning.

#### Where the escape applies

Residue names, and field 4 of the `$` section. **Not** to a cleavage name - see below. Decoding
belongs wherever a name is shown: `getTypeName()` returns the stored form, and the canvas draws it,
so without a decode step in the renderer and in the dialog the user is shown `_e_Ser_2F_Thr` where
they typed `Ser/Thr`.

#### What this fixes

The six dictionary residue names that cannot be written today are the reason this is not merely a
convenience: `L-gro-D-manHep`, `D-gro-D-manHep`, `Tri-P` carry `-`, and `(S)Lac`, `(R)Lac`, `(X)Lac`
carry parentheses *(measured)*. None carries `=`, so rule 1 costs them nothing and rules 2-4 make
all six writable.

### One place the escape must **not** be applied

```java
String cleavage_typename = m.group(7);
if( cleavage_typename.indexOf('_') != -1 )
    cleavage = CrossRingFragmentDictionary.newFragment(cleavage_typename, ret);
else
    cleavage = ResidueDictionary.newResidue(cleavage_typename);
```

In a **cleavage** name, `_` already decides whether the name is a cross-ring fragment or an ordinary
residue. Escaping cleavage names would break that test. *(Read.)* The escape belongs to residue
names and to field 4 of the `$` section — not to the cleavage name after `/`.

## 1.4 A linkage

```
linkage := "--" ( additional "," )* glycosidic
         | "-" [1-9N?]                       // pre-2007 form, still read

glycosidic := ( [1-9N?] "/" )* [1-9N?] ( "=" [1-9N?] )?
```

`--4b1D-Gal,p` is a bond at position 4. `--3=2,6/4b1D-Glc,p` carries an additional bond and an
ambiguous position set. The one-dash form is read and never written. *(Read.)*

## 1.5 Repeats and brackets

```
repeat-start := "["
repeat-end   := "]" ( "_" "-"? int )? ( "^" "-"? int )?
bracket      := "}"
```

`--4[--4b1D-GlcNAc,p--6b1D-GlcNAc,p--4]_1^3--4b1D-GlcNAc,p` is a repeat of between 1 and 3 units.
*(Measured: reads in this project.)* `}` introduces the bracket that carries undetermined-linkage
residues. Counts on `]` accept negative numbers; the 2007 parser did not. *(Read.)*

## 1.6 The `$` section

```
mass-options := isotope "," derivatization "," ion-cloud "," neutral-exchanges ( "," reducing-end )?
```

It is `MassOptions.toString()`, positional, comma-separated. `freeEnd--?b1D-GalNAc,p$MONO,Und,0,0,freeEnd`

- Fields 0–3 are **required**; a tail with three fields throws `IndexOutOfBoundsException`.
- Field 4 is optional.
- **Empty fields do not hold their place.** The tokenizer discards them, so `a,,b` is two tokens and
  everything after a blank shifts left.
- **Tokens past the fifth are read and ignored** — and **discarded when the structure is written
  again**, because the writer rebuilds the tail from five values. The tail is therefore not a place
  to carry anything.

*(All measured.)*

| field | meaning | written as |
|---|---|---|
| 0 | isotope | `MONO`, `AVG` |
| 1 | derivatization | `Und` (none), `perMe`, … |
| 2 | ion cloud | `0` when empty, else `Na`, `2Na`, `Na+K`, … — **one ion is `Na`, not `Na1`** |
| 3 | neutral exchanges | same spelling as the ion cloud |
| 4 | reducing end | a residue name, or `name=<mass>u` for a custom one |

*(The ion-cloud spelling is measured twice over: `IonCloud.toString` writes it that way, and 6 of the
14 GlycoWorkbench examples carry `Na`.)*

**Field 4 is not validated.** `ResidueDictionary.findResidueType` never refuses a name: an unknown
one becomes a residue type via `createUnknown`, and one containing `=` is read as `name=massu` and
becomes a custom reducing end. A typo in this field produces a residue, not an error. *(Measured:
`…,NoSuchReducingEnd` round-trips unchanged.)*

## 1.7 Core conformance

**An implementation conforms to GWS core when all four of these pass.** *(Required.)* Each is a
corpus that exists today; none is in this repository yet, and all four should be.

| # | corpus | requirement | where it stands |
|---|---|---|---|
| C1 | the **14 `.gws` written by GlycoWorkbench** (`glycoinfo/eurocarbdb`, `application/GlycoWorkbench/examples/`) | every file reads, and the structure read is the structure written | **14 read, 0 fail** *(measured)*. None is textually identical on rewrite, because the writer states mass options the 2008 writer left implicit - see below |
| C2 | the **29 dictionary names containing `-`** (3 in `residue_types`, 26 in `non_symbolic_residue_types`) | each name reads, and the residue is the one the dictionary defines | **0 of 29 read today** *(measured)*. The widened class of §1.3 is what fixes this |
| C3 | the **3 dictionary names containing parentheses** (`(S)Lac`, `(R)Lac`, `(X)Lac`) | as C2 | **0 of 3 read today** *(measured)* |
| C4 | the **25 core-level strings** among GlycanCore's 47 test strings | each reads | **25 read** *(measured)*; the other 22 are Part 2 and must **not** read |

**C1 requires a structural comparison, not a textual one.** No file GlycoWorkbench wrote round-trips
as text - `…$MONO,Und,Na,0` comes back as `…$MONO,Und,Na,0,freeEnd`, and a file with no `$` gains one.
That is the writer being more explicit, not damage, and a conformance test written on string equality
would fail all 14 *(measured)*.

### Where the current implementation stands against its own requirements

| requirement | status |
|---|---|
| read every `.gws` ever written | **not met** - C2 and C3 fail: 32 dictionary names can be written and not read |
| a label may carry any character | **not met** - `/`, `-`, `,`, space, `(`, `)` are refused, and `$` empties the structure with no error |
| `=` refused in a label | **not met** - accepted, then truncates the label and throws on the mass |
| write only what others can read | **not met** - the 32 names above are written and are unreadable by this project, GlycanCore and GlycoWorkbench alike |

And for the wider picture, measured across 1,631 structures taken through WURCS → GWS → WURCS:
60.9% survive, 2.9% come back empty, 6.1% come back different, **30.1% cannot be read back**. The
largest single group of those failures is C2.

# Part 2 — GWS extended (GlycanCore)

**This part is descriptive, and it is also a boundary.** Everything here is outside GWS core:
GlycanBuilder2 is not to implement it, and an implementation that reads only Part 1 is conformant.
The extension is GlycanCore's, and it evolves there.

The boundary was drawn by measurement rather than by preference: of the 47 GWS strings in
GlycanCore's own test file, **25 are core and 22 use an addition below** *(measured)*. That is what
makes "core" a line somebody can test against rather than a matter of taste.

Note that core and extended are **not** ordered by which is better. Core has the wider *name* - it
has to, to read every file ever written - while extended has the wider *structure* syntax. An
extended reader is not automatically a core reader: GlycanCore refuses `D-gro-D-galHep` and `(S)Lac`
exactly as this project does *(measured)*.

## 2.1 Corrections to the core

| | core | extended |
|---|---|---|
| name class | `[a-zA-z0-9_#=.]` | `[a-zA-Z0-9_#=.]` — the ASCII span corrected, the intended set unchanged |
| multi-structure | the caller splits on `;` | `fromString` splits and returns a list |
| normalisation | none | `setNormalizeOnRead(true)` normalises on the way in |

**The name class was corrected without being widened.** `-` and `/` are excluded in the extended
dialect too, so the six unwritable residue names fail there as well. Two implementations have now
met this limit and neither chose to widen the set. *(Read.)*

## 2.2 Additions

| addition | syntax | example |
|---|---|---|
| anomer symbols | `[abudo?!]` adds `u`, `d`, `!` | `--4u1D-Glc,p` |
| ring forms | `[?pfoa!]` adds `!` | |
| configuration | `[DL?]-` adds `?` | |
| **linkage probability** | `%[_^0-9]+` after a position | `--6=6%_0^100,4%_0^100b1D-GlcNAc,p` |
| **core modification** | `[…]` after the ring form, `[a-zA-Z0-9_=/\-,]+` | |
| **attachment ids** | `#n` on brackets and repeats | `--4{#2)…--4{#1}#2_0^2` |
| **counts on brackets** | `}` takes `_n^m`, and `^?` | `…}_1^3--…`, `…]^?--…` |
| **definition block** | a second `$`: `$$NAME=definition` | `freeEnd--?b1D-Hex=A,?…$$A=alpha-D-gluco/manno-pyranose` |

The definition block is the notable one: it is how the extended dialect carries meaning the core has
nowhere to put, and it is placed **after** the mass options rather than inside them. *(Read, and
measured only in that this project rejects it.)*

## 2.3 Compatibility, as it stands

- **Extended → core**: fails wherever an addition appears. 22 of 47 measured.
- **Core → extended**: expected to read, since extended is a superset of the grammar. **Not
  measured** — GlycanCore was not run here.

---

# Open questions {#open}

Three of the five this document opened are settled by Part 1 being normative. What remains:

1. **Do the two dialects converge?** Part 1 fixes core and Part 2 fences the extension, which is a
   working arrangement rather than an answer: it does not say whether GlycanCore will one day read
   core's widened names, or whether GlycanBuilder2's successor simply becomes an extended reader.
   Sits beside the fork question in #226, and is a decision for people rather than parsers.
2. **Does the `$` tail get a keyed form?** Tokens past the fifth are already ignored by every reader
   measured, so `k=v` fields would be backward compatible — but the writer would have to carry them,
   which it does not today. Needed only if something must travel with a structure that the five
   fields cannot hold.

Settled by Part 1, and recorded here so the history is legible:

- ~~*Escape or widen the name class?*~~ **Both, asymmetrically** — widen the reader, escape the
  writer (§1.3, "The rule the rest of this follows").
- ~~*Correct the `a-zA-z` span?*~~ **Moot.** The normative class is derived from what files contain,
  not from that regex; the span's accidental members (`[`, `\`, `]`, `^`, `` ` ``) are simply not in
  it, and a reader that still accepts them is harmlessly liberal.
- ~~*Writer or reader authoritative where they disagree?*~~ **Neither** — they are different
  grammars on purpose.

# The corpora, and where they are

§1.7 makes four of these a requirement. None is in this repository yet; all four should be, and the
first two cost nothing but a copy.

| set | size | source |
|---|---|---|
| **C1** GlycoWorkbench examples | 14 | `glycoinfo/eurocarbdb`, `application/GlycoWorkbench/examples/` — the only witness to the format as originally produced, and two of them exercise the multi-structure path that nothing else tests |
| **C2/C3** dictionary names | 29 + 3 | this repository: `conf/residue_types`, `conf/non_symbolic_residue_types`. The test writes each name, reads it back, and checks the residue is the one defined |
| **C4** GlycanCore test strings | 47 | `glycancore`, `src/test/java/org/glycoinfo/glycanbuilder/io/TestGWSIO.java` — 25 core, 22 extended |
| *(not a requirement)* generated | any | WURCS corpus → GWS → back. Tests whether today's writer and reader agree; **does not** test whether we read what GlycoWorkbench wrote, which is why C1 exists |

The originals are downloadable: GlycoWorkbench and GlycanBuilder both survive on the Google Code
archive (`storage.googleapis.com/google-code-archive-source/v2/code.google.com/{glycoworkbench,glycanbuilder}/source-archive.zip`),
and the WURCS and GlycoCT corpora at `data.glygen.org/ln2downloads/glycan/others/`.
