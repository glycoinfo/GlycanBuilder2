# The GWS format

GWS — "GlycoWorkbench sequence" — is the text form GlycanBuilder and GlycoWorkbench save structures
in, and the format of a `.gws` file.

**This document has two levels, and they are two different kinds of statement.**

- **Part 1, GWS core, is normative for GlycanBuilder2 and for glycanbuilder2web**, which uses the
  same library and therefore inherits every clause. It says what this project *must* read and
  write. Two requirements shape it: **every `.gws` that has ever been written must be readable**, and
  **a residue label may carry any character**. Where today's code falls short of that, the document
  states the requirement and the shortfall is a defect, not a clause. What core requires of
  *GlycanCore* is deliberately out of scope: see open question 1.
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

### The name

**A name is:** *(Required.)*

```
name := ( [A-Za-z0-9_#=.] | "-" (?! "-" | [1-9N?]) | "(" [A-Za-z0-9] ")" )+
```

*(Implemented 2026-11-09 in `GWSParser`. The exclusion after `-` is `[-1-9N?]`, not `[-0-9]` as this
clause first said: the pre-2007 linkage form is `-[1-9N?]`, so `N` and `?` must be excluded too or a
name swallows an old-style linkage. Measured: no name in either dictionary has a `-` followed by a
digit, `N` or `?`, so the exclusion costs nothing. The implementation also keeps the historical
`a-zA-z` span rather than narrowing it to `A-Za-z` — see "Why the historical class is not the
specification" — because narrowing would stop files reading that read today, which is the one thing
this format may not do. A reader admitting more than this clause requires is conforming; one
admitting less is not.)*

Wider than the class any implementation uses today, and wider deliberately: this is what makes
**every `.gws` ever written** readable. Two additions over the historical class, each carrying names
that have been written to files and could not be read back — and each narrowed to exactly what is
unambiguous:

- **`-`, when not followed by `-` or a digit.** `--` is a linkage and `-<digit>` the pre-2007 linkage
  form, so the restriction is precisely enough. *(Measured: `D-gro-D-galHep,p` reads as the name
  `gro-D-galHep` after the `D-` configuration prefix; `Tri-P` reads whole;
  `D-gro-D-galHep,p--4b1D-Gal,p` and `D-gro-D-galHep-4b1D-Gal,p` both split where they should.)*
  That split is right for the grammar and was wrong for the residue: see "A configuration that
  belongs to the name" below.
- **A parenthesised single character**, `"(" [A-Za-z0-9] ")"`, for `(S)Lac`, `(R)Lac`, `(X)Lac`.

**Bare `(` and `)` must not be admitted.** Parentheses delimit branches, and a branch opens with `(`
followed immediately by `--`, so the constrained form above cannot collide with one. Admitting them
unconstrained does collide, and only for residues with **no ring form**, which is why it looks safe
until it is not. *(Measured, with an unconstrained class: `b1D-Man(--3a1D-Man,p)` reads the name as
`Man(`, `S(--3a1D-Man,p)` as `S(`, and `GalNAc)--4b1D-Gal,p` as `GalNAc)`. With the constrained form
all three read correctly.)*

### A configuration that belongs to the name

**When the name read is no residue, and the configuration and the name together are one, the
configuration is part of the name and the configuration is unknown.** *(Required. Implemented
2026-11-10 in `GWSParser`, #252.)*

Twenty-four names in `non_symbolic_residue_types` begin with a configuration of their own:
`D-gro-D-galHep`, `L-gro-D-manNon`, `D-ery-D-glcDEC` and the like. Their rows give no configuration, so
by default they have none. (`residue_types` has two more, `L-gro-D-manHep` and `D-gro-D-manHep`, whose
rows do give one.) A writer emits the configuration only when it is known, so one of these
with its configuration unknown is written `??1D-gro-D-galHep,p`, and the grammar reads `D-` as the
configuration and `gro-D-galHep` as the name. No dictionary has that name. The residue came back
unknown, not a saccharide and of no mass, **and was written back as the same text**, so a check that
compares text could not see it. *(Measured, before the change: all 24, each a child of a glucose.)*

The rule is narrow on purpose. It applies only when the shorter name is unknown and the longer one is
known, so a file that read as a known residue before reads the same now. `D-D-gro-D-galHep`, with
the configuration known, is unaffected: its name is already the longer one.

### Writing: the same names, unchanged

**A writer emits the name it holds.** *(Required.)* No renaming, no normalisation, no escape for any
name in the dictionaries — the reader above accepts all of them, so the round trip closes on the
identical string.

The escape of §1.3.1 exists for **one purpose only**: a label a user typed that contains a character
the grammar genuinely cannot carry. After the two additions above, that set is small:

```
/  ,  $  =  space   and every non-ASCII character
```

`-`, `(` and `)` are **not** in it. `GalNAc-Ser` and `(S)Lac` are written literally and read back
literally, which is both simpler and what a reader of the file would expect to see.

### Why the historical class is not the specification

`[a-zA-z0-9_#=.]`, which both implementations use, is **not a typo-free character class**: `a-zA-z`
spans ASCII 65–122, so it also admits `[`, `\`, `]`, `^` and `` ` ``. It has been that class since
2007 and was never revisited. *(Read, both versions; GlycanCore corrected the span to
`[a-zA-Z0-9_#=.]` without changing the intended set.)* It carries no design intent worth preserving,
which is why the grammar above was derived from what files contain rather than from what the regex
allows.

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

**Thirty-five rows across the three dictionaries had names this grammar could not write.** *(Read
from `conf/residue_types`, `conf/non_symbolic_residue_types` and `conf/compositions`, 272 rows, on
2026-11-09. Stated here as six, and elsewhere as 33, before it was measured rather than counted by
hand; the first count had scanned only `residue_types`.)*

| offending character | rows | which |
|---|---|---|
| `-` | 30 | the heptoses, the octoses, `Tri-P` |
| `( )` | 3 | `(S)Lac`, `(R)Lac`, `(X)Lac` |
| `-` and `?` | 2 | `?-gro-D-manHep`, `?-gro-D-glcHep` |

Drawing any of them, saving, and reopening lost the file. *(Measured:
`WURCS=2.0/1,1,0/[a21122h-1a_1-5]/1/` writes `freeEnd--1a1D-D-gro-D-galHep,p$…` and read back as
`invalid format for linkage: -gro-D-galHep,p`.)* That is #256, reported by a user who had drawn a
heptose.

This was not a fault in the grammar so much as one nobody guarded: the first dictionary had 81
residue types and **every one fitted the class**. The rest arrived later. *(Measured against the 2007
dictionary.)*

**Resolved 2026-11-09 for 30 of the 35**, by the widening in §1.3 and with no writer change, so the
round trip closes on the identical string. The five that remain are **not grammar problems** and no
reader can be written to accommodate them — each is a row that contradicts itself:

- `araOct` and `manOct` record their anomeric carbon as `-`, where a position or `?` belongs, so the
  writer emits `freeEnd--??-manOct,p` with a dash standing where the carbon should be.
- `L-gro-D-glcOct` records its chirality as `/`, which the writer emits as `/-` and a reader must take
  as the start of a cleavage. The row disagrees with itself three ways: the name says `L-gro-D`, the
  `iupac_name` column `D-gro-D-glcOct`, the description `D-glycero-L-glcoOctose`. *(The `iupac_name`
  column is field 5, per `ResidueType(String)`; stated here as the synonym column, which is field 4
  and holds `-`, until the columns were counted against the parser.)*
- `?-gro-D-manHep` and `?-gro-D-glcHep` *begin* with `?`, which marks an unknown position everywhere
  else in the format.

All five are data to correct, and correcting them is a chemistry decision — **#273**.
`GwsReadsTheNamesItWritesTest` asserts the exact set, so a sixth cannot appear unnoticed and these
five cannot be forgotten.

An escape using only characters the class already accepts — `_` plus hex digits, so `/` becomes
`_2F_` — round-trips today on an unmodified parser, and older readers show the escaped form rather
than failing. Not implemented; see [Open questions](#open).

### Names with any character in them: the escape

Decided 2026-09-11, and narrowed on 2026-09-11 once the reader was widened. **This applies to labels
a user types, not to names in the dictionaries** — those are read and written literally, per "Writing"
above. A label may contain any character at all; the format does not change to allow it, because the
label is encoded into characters the grammar already accepts.

The characters that need encoding, after `-` and `(x)` became readable:

```
/  ,  $  =  space   and every non-ASCII character
```

#### The rules

1. **Safe characters pass through unchanged**: `A-Z a-z 0-9 _ # .`, and — since the reader accepts
   them — `-` and a parenthesised single character. `GalNAc-Ser` is stored as it was typed.
2. **Every other character is written `_XX_`**, two upper-case hex digits. `/` → `_2F_`, `,` →
   `_2C_`, `$` → `_24_`, `=` → `_3D_`, space → `_20_`.
3. **A label that had anything encoded is prefixed `_e_`.** A label that needed no encoding is stored
   as it is, with no prefix.
4. **A literal label beginning `_e_` is encoded**, if only its leading underscore (`_5F_`), so that
   it too carries the prefix. This is the only reason a label with no otherwise-unsafe character
   gets encoded.
5. **Non-ASCII is encoded as its UTF-8 bytes**, one `_XX_` each.

#### Worked examples

| the user types | stored as | why |
|---|---|---|
| `Ser_Thr` | `Ser_Thr` | nothing unsafe - **no prefix, no change** |
| `Ser_2F_Thr` | `Ser_2F_Thr` | nothing unsafe either; rule 4 is what keeps this distinct from the next row |
| `Ser/Thr` | `_e_Ser_2F_Thr` | `/` encoded |
| `GalNAc-Ser/Thr` | `_e_GalNAc-Ser_2F_Thr` | only `/` — `-` is readable, so it stays |
| `/-` | `_e__2F__2D_` | consecutive escapes run their underscores together; still unambiguous, since a decoder reads `_`, two hex, `_` |
| `セリン` | `_e__E3__82__BB__E3__83__AA__E3__83__B3_` | UTF-8, three bytes per character |
| `_e_Ser` | `_e__5F_e_5F_Ser` | rule 5 |
| `Ser=Thr` | `_e_Ser_3D_Thr` | `=` separates a custom reducing end's label from its mass, so it must be encoded — without that, the label truncates and the mass parse throws *(measured: `For input string: "Th"`)* |

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

#### Revised for an aglycon: written as typed, 2026-11-10 (#305)

**The root is now read by a rule of its own, and an aglycon's name is written as the user typed it.**
Decided by the maintainer on 2026-11-10, after the rules above: they encode what the residue grammar
cannot carry, and a reducing end no longer goes through that grammar.

**Reading the root.** It runs up to the first `--`, or to the `(` of a `(--` before it. A
composition's root, which has nothing after it, runs to the bracket.

- If all of it reads as a residue that a dictionary has (`freeEnd`, `redEnd`, `2AB`, a cleaved
  residue), it is that residue, as before.
- Anything else is an **aglycon named by the whole of it**. With a mass, as in
  `Ser/Thr=87.0320u`, the last `=` separates the name from the mass. Without one, as in `Ser`, it is
  an aglycon whose mass is not known.
- This is how GlycanCore reads a root it does not know, with or without `=`. GB2 additionally reads
  `=<mass>u` as the mass.
- A file with no `--` at all, which is a pre-2007 file or text that is not GWS, is read as before.

**Writing.** Only what splits the text before the root is looked at is encoded:

```
;  $  }  ,  --   and control characters
```

`;` separates the structures of a document, `$` opens the mass options, `}` closes a composition's
bracket, `,` separates the mass-option fields (field 4 holds the name again), and `--` begins a
linkage. The prefix rules 3 and 4 above are kept, and **only a name prefixed `_e_` is decoded**, so a
name written before, such as `Ser_2F_Thr`, still means what it meant. Within a prefixed name, a `_`
that would read as an escape is encoded too.

| the user types | stored as |
|---|---|
| `Ser/Thr` | `Ser/Thr` |
| `Cer(d18:1/16:0)` | `Cer(d18:1/16:0)` |
| `D-Ser`, `β-Ala`, `Ser Thr`, `Ser=Thr` | unchanged |
| `Ser,Thr` | `_e_Ser_2C_Thr` |
| `_e_Ser` | `_e__5F_e_Ser` |

Every stored form in the first table above still decodes to what was typed. *(Tested:
`AglyconNamesRoundTripTest` reads `label-escapes.tsv`.)*

**A mass not known** (Ser/Thr is one of two masses) is written `=NaNu`, which an older reader also
parses, as NaN. Such a structure weighs as its glycan alone, and its m/z is shown as "— (the
aglycon's mass is not known)" rather than as a figure.

**The cost: an older reader cannot open a name written this way.** GB2 1.40.0 and earlier,
GlycoWorkbench, and the web application before it takes this up all stop at the `/`. This is the
same failure the desktop application already produced, by writing `Ser/Thr=…u` unescaped. Names that
need nothing encoded, such as `Ser` or `2AB`, read everywhere as before.

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

**The pre-2007 form means exactly what the same position written with `--` means.** *(Required, and
measured since 2026-11-09.)* `freeEnd-1b1D-GlcNAc,p` is `freeEnd--1b1D-GlcNAc,p`. A writer never emits
it. Until #274 it was accepted by the grammar and then failed to load, so a file in this form opened as
empty with no error.

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
mass-options := isotope "," derivatization "," ion-cloud "," neutral-exchanges
                ( "," reducing-end )? ( "," anything )*
```

It is `MassOptions.toString()`, positional, comma-separated. `freeEnd--?b1D-GalNAc,p$MONO,Und,0,0,freeEnd`

| field | meaning | written as |
|---|---|---|
| 0 | isotope | `MONO`, `AVG` |
| 1 | derivatization | `Und` (none), `perMe`, … |
| 2 | ion cloud | `0` when empty, else `Na`, `2Na`, `Na+K`, … — **one ion is `Na`, not `Na1`** |
| 3 | neutral exchanges | same spelling as the ion cloud |
| 4 | reducing end | a residue name, or `name=<mass>u` for a custom one, `=NaNu` for a mass not known |
| 5+ | **not core's business** | see below |

- Fields 0–3 are **required**; a tail of three fields throws `IndexOutOfBoundsException`.
- Field 4 is optional. *(4 of the 14 GlycoWorkbench examples have no `$` section at all.)*
- **A writer reproduces what it read: no `$` section stays absent, and a four-field tail stays four
  fields.** *(Required.)* Today the writer always emits five, which is why no file GlycoWorkbench
  wrote comes back unchanged — `…$MONO,Und,Na,0` gains `,freeEnd` and a structure with no `$` gains
  a whole section *(measured)*. Neither is damage, and both destroy textual identity for no gain.
  Held the same way as §1.6.1: remember the shape that arrived, reproduce it.
- **Empty fields do not hold their place.** The tokenizer discards them, so `a,,b` is two tokens and
  everything after a blank shifts left.

*(All measured. The ion-cloud spelling twice over: `IonCloud.toString` writes it that way, and 6 of
the 14 GlycoWorkbench examples carry `Na`.)*

**Field 4 is not validated.** No name is refused. Since 2026-11-10 (#305), a name no dictionary
has is an aglycon whose mass is not known, the same as at the root. A name ending in `=<mass>u` is
an aglycon of that mass, split at the last `=`. A name holding an `=` that is not followed by a
mass is a name, and no longer throws. A typo here produces an aglycon, not an error. *(Measured:
`…,NoSuchReducingEnd` round-trips unchanged.)*

### 1.6.1 Fields past the fifth: interpret nothing, preserve everything

**Core interprets the five fields and nothing else. It must also destroy nothing else.** *(Required.)*

```
reading : parse fields 0-4. Keep the remainder of the tail verbatim, uninterpreted.
writing : emit the five fields, then append the remainder unchanged.
```

The five fields correspond one-to-one with what GlycanBuilder2 can do — isotope, derivatization, ion
cloud, neutral exchanges, reducing end — so interpreting more would mean taking the extension into
core, which is the opposite of this document's arrangement. But **not interpreting is not the same as
deleting**, and today core deletes:

| a file containing | GlycanBuilder2 writes back | GlycanCore writes back |
|---|---|---|
| `$MONO,Und,0,0,freeEnd,writer=x` | `$MONO,Und,0,0,freeEnd` — **dropped** | `$MONO,Und,0,0,freeEnd,writer=x` — **kept** |
| `$MONO,Und,0,0,freeEnd,writer=x,ver=1` | dropped | **kept** |

*(Measured, both implementations.)* So a file that survives a GlycanCore round trip is degraded by a
GlycanBuilder2 one: **opening a document and saving it loses information nobody asked core to
understand.** That is the same class of fault as a save that cannot be reopened, and it is why the
rule above is normative rather than a nicety.

Nothing occupies those fields today, so nothing is being lost yet. The rule exists so that the day
the extension puts something there, core is already safe — and so that whatever convention the
extension chooses is **none of core's business**. Core needs no keyed-field scheme, no registry and no
agreement about what the fields mean; it needs only to carry them.

Where it lands in the implementation: a string held on `MassOptions`, set by `fromString`, appended by
`toString`, copied by `clone()`. **Deliberately excluded from `equals()`** — a difference core cannot
interpret must not make a document count as modified, or opening an extended file would prompt to
save it.

## 1.7 Core conformance

**An implementation conforms to GWS core when all four pass.** *(Required.)* Each corpus exists
today; none is in this repository yet, and all four should be.

| # | corpus | requirement | where it stands |
|---|---|---|---|
| C1 | the **14 `.gws` written by GlycoWorkbench** (`glycoinfo/eurocarbdb`, `application/GlycoWorkbench/examples/`) | **each file reads and writes back as the identical string** | **14 read, 0 fail**, and the **structure part of all 14 is already byte-identical** *(measured)*. Only the `$` section differs, which §1.6 now requires be reproduced — so identity is reachable |
| C2 | the **29 dictionary names containing `-`** (3 in `residue_types`, 26 in `non_symbolic_residue_types`) | each name reads, resolves to the residue the dictionary defines, and writes back identically | **0 of 29 read today** *(measured)*. The `-` rule of §1.3 fixes this with no change to the writer |
| C3 | the **3 dictionary names containing parentheses** (`(S)Lac`, `(R)Lac`, `(X)Lac`) | as C2 | **0 of 3 read today** *(measured)*. The `"(" [A-Za-z0-9] ")"` rule fixes this, also with no writer change |
| C4 | the **25 core-level strings** among GlycanCore's 47 test strings | each reads | **25 read** *(measured)*; the other 22 are Part 2 and must **not** read |

**C1 is a string comparison.** That is a change from this document's first draft, which required a
structural one because the writer normalised the `$` section. With §1.6's reproduction rule the
normalisation goes away, and a test on string equality becomes both correct and far easier to read
when it fails.

Three things make textual identity achievable rather than aspirational, all measured:

- the structure part of all 14 GlycoWorkbench files already matches exactly;
- `writeGlycan` is `toString(structure, false, true)` — **`ordered=false`, so the writer does not sort
  branches**, and the order in the file survives;
- with the reader widened, the writer needs no change at all for the 32 dictionary names — it already
  emits exactly what the file contained.

### Where the current implementation stands against its own requirements

| requirement | status | what fixes it |
|---|---|---|
| read every `.gws` ever written | **met for the names** as of 2026-11-09 — 30 of the 35 unreadable names fixed by §1.3, reader only; the remaining five are self-contradicting dictionary rows, not grammar (§1.3.1, #273). **Met for the pre-2007 linkage form** `-[1-9N?]` as of 2026-11-09: it loads as the same structure as its `--` equivalent (#274, §1.4) | #273, five dictionary rows |
| read and write the identical string | **not met** — the `$` section is normalised on write | §1.6's reproduction rule |
| a label may carry any character | **met for an aglycon** as of 2026-11-10 (#305): written as typed, with `;` `$` `}` `,` `--` encoded (§1.3.1, "Revised for an aglycon"). Not yet for a user-defined residue elsewhere in the tree | the escape in §1.3.1 |
| destroy nothing it does not interpret | **not met** — tail fields past the fifth are dropped | §1.6.1 |

And for scale, measured across 1,631 structures taken through WURCS → GWS → WURCS: 60.9% survive,
2.9% come back empty, 6.1% come back different, **30.1% cannot be read back**. The largest single
group of those failures is C2.

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
*(measured)* — as this project did until 2026-11-09, and no longer does.

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

One remains.

1. **Do the two dialects converge — and does core's reading grammar reach GlycanCore?** Part 1 fixes
   core and Part 2 fences the extension, which is a working arrangement rather than an answer. The
   question has a deadline: **GlycanBuilder2's successor is to be built on GlycanCore**, and
   GlycanCore's reading grammar is *narrower* than core's — it refuses `D-gro-D-galHep` and `(S)Lac`
   exactly as this project does today *(measured)*. So the reading widened by §1.3, which is what
   rescues every existing file, **would be lost at that migration** unless one of these is chosen:

   | | what it means | cost |
   |---|---|---|
   | **converge upward** | GlycanCore adopts core's reading grammar — `type_str` at `GWSParser.java:67` | one line, and the same rule is already measured unambiguous |
   | **stay separate** | GlycanBuilder2 keeps a reader GlycanCore lacks | a one-off conversion of every existing file before the migration; anything missed is lost |
   | **do not widen at all** | give up on rescuing old files | contradicts §1.7 C2/C3 |

   The first is a line of code and the third is a retreat, so the real content of the question is
   governance rather than engineering: **converging upward means a core requirement reaching into
   GlycanCore**, which is the first time the arrangement in this document would be crossed. That sits
   beside the fork question in glycoinfo/GlycanBuilder2#226 and is for people to decide.

Settled by Part 1, and recorded here so the history is legible:

- ~~*Escape or widen the name class?*~~ **Both, asymmetrically** — widen the reader, escape the
  writer (§1.3, "The rule the rest of this follows").
- ~~*Correct the `a-zA-z` span?*~~ **Moot.** The normative class is derived from what files contain,
  not from that regex; the span's accidental members (`[`, `\`, `]`, `^`, `` ` ``) are simply not in
  it, and a reader that still accepts them is harmlessly liberal.
- ~~*Writer or reader authoritative where they disagree?*~~ **Neither** — they are different
  grammars on purpose.
- ~~*Does the `$` tail get a keyed form?*~~ **Not core's question.** §1.6.1 has core carry the tail
  without interpreting it, so the extension may adopt any convention it likes without core needing to
  know or agree.

# The corpora, and where they are

**They are in this repository now: [`src/test/resources/gws/`](../src/test/resources/gws/)**, on the
test classpath, alongside the corpus `ConverterReachTest` already uses. §1.7 makes four of them
a requirement; the other three cover the clauses §1.7 does not.

| file | rows | requirement |
|---|---|---|
| `glycoworkbench/` | 14 `.gws` | C1 — read, and write back the identical string |
| `dictionary-names.tsv` | 33 | C2, C3 — every dictionary name writable and readable. **33 of 33 unreadable today** |
| `glycancore-strings.tsv` | 48 | C4 — 26 core rows must read, 22 extended rows must not |
| `label-escapes.tsv` | 9 | §1.3.1 — a label may carry any character |
| `dollar-section.tsv` | 6 | §1.6, §1.6.1 — reproduce the section's shape, carry what is not interpreted. **5 of 6 fail today** |
| `must-be-refused.tsv` | 2 | §1.2 — a raw `$` in a name is refused. **Both fail today, silently** |

Every status column was measured on 1.40.0 before any of this was implemented, so the data is both
the requirement and the baseline. Nothing in it was written by hand: the GWS strings came from the
library itself, built as structures and written out, or from files other software produced. There is
no runner yet, deliberately — `src/test/resources/gws/README.md` gives the three assertion shapes
that cover all of it, and the `getResourceAsStream` paths.

The originals remain downloadable, should the data ever need regenerating: GlycoWorkbench and
GlycanBuilder both survive on the Google Code archive
(`storage.googleapis.com/google-code-archive-source/v2/code.google.com/{glycoworkbench,glycanbuilder}/source-archive.zip`),
and the WURCS and GlycoCT corpora at `data.glygen.org/ln2downloads/glycan/others/`.
