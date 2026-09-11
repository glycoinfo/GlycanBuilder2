# The GWS format

GWS — "GlycoWorkbench sequence" — is the text form GlycanBuilder and GlycoWorkbench save structures
in, and the format of a `.gws` file.

**This document describes two levels.** *GWS core* is what this project reads and writes, and what
GlycoWorkbench wrote before it. *GWS extended* is the superset implemented by
[GlycanCore](https://gitlab.com/glycoinfo/glycanbuilder-module/glycancore), which this project does
not read. The split is deliberate: the extensions are newer work and there is no need to bring them
here to fix what is broken in the core.

## Status of this document, and how to read it

**This is a description, not a standard.** It was written on 2026-09-11 by reading two parsers and
running structures through them, because no specification existed. Each clause is one of:

- **Measured** — stated because a structure was written, read back, and the result observed.
- **Read** — taken from the grammar in the source and not separately exercised.
- **Open** — a question the implementations do not answer consistently. Listed rather than decided.

Where core and extended disagree, both are given. Nothing here is a proposal: it says what the
software does today, so that changing it becomes a decision rather than an accident.

Sources: `GWSParser` in `org.eurocarbdb.application.glycanbuilder.converterGWS` (this project);
`GWSParser` / `GWSBuilder` in `org.glycoinfo.glycanbuilder.io` (GlycanCore 0.13.10, 2026-08-25); the
original 2007 parser, from
`storage.googleapis.com/google-code-archive-source/v2/code.google.com/glycanbuilder/source-archive.zip`;
and the 14 `.gws` examples shipped with GlycoWorkbench, in `glycoinfo/eurocarbdb` under
`application/GlycoWorkbench/examples/`.

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
name          := [a-zA-z0-9_#=.]+
ring-form     := "," [?opfa]
cleavage      := "/" [a-zA-z0-9_#]+
placement     := "@" "-"? [0-9]+ "s"?
bounding-box  := "<bounding_box>" int "," int "," int "," int "</bounding_box>"
```

*(Read, from `GWSParser`'s `residue_pattern`.)*

Examples: `?b1D-GalNAc,p` · `freeEnd` · `--3S` (a sulfate at 3) · `?b1D-GlcNAc,p<bounding_box>0,0,30,30</bounding_box>`

### The name class, and its consequences

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

### An unrestricted name is reachable, including `$`

*(Measured 2026-09-11, by naming a reducing end each way, writing, reading back and rendering.)*

| name as typed | today | as `_XX_` escape | escaped result |
|---|---|---|---|
| `Ser/Thr` | `invalid format for linkage: =87.0320u…` | `Ser_2F_Thr` | **reads, renders 436×111** |
| `Ser(x)Thr` | `invalid format for linkage: x` | `Ser_28_x_29_Thr` | **reads, renders 478×111** |
| `Ser$Thr` | reads with **no error** and yields an empty structure | `Ser_24_Thr` | **reads, renders 437×111** |
| `Ser=Thr` | `For input string: "Th"` | `Ser_3D_Thr` | **reads, renders 438×111** |
| `Ser_Thr` | reads — but would be ambiguous once `_` introduces an escape | `Ser_5F_Thr` | **reads, renders 436×111** |
| `セリン` | `Invalid format for string: セリン=87.0320u…` | needs a defined form — see below | — |

So **no character needs to be forbidden, `$` included.** Once the writer escapes, no raw `$` reaches
the string and the first-`$` split stays safe. Giving up `$` is a concession that does not have to be
made.

Four things the escape has to cover, and only the first is obvious:

1. **Characters outside the name class** — `/`, `-`, `,`, space, `(`, `)`, `$`, and the rest.
2. **`=`, which is inside the class and still unsafe.** See the next section: escaping it changes
   how a custom reducing end is stored.
3. **`_` itself**, as the escape introducer: `Ser_Thr` has to be written `Ser_5F_Thr`, or the two
   cannot be told apart. **No dictionary residue name contains `_`** *(measured: 0 of 134)*, so this
   costs nothing for the six unwritable names and only affects user-typed labels.
4. **Non-ASCII.** `セリン` does not match the name pattern at all today. Two hex digits cannot carry
   it; the form has to be decided — UTF-8 bytes as consecutive `_XX_`, or a wider `_uXXXX_`. Unless
   Japanese labels are ruled out, this needs choosing rather than discovering.


### What escaping `=` changes about a custom reducing end

A custom reducing end has no dictionary entry. It is stored **as its own name**, and that name is a
two-field record with `=` as the separator:

```java
// ResidueType.createOtherReducingEnd(label, mass)
ret.name = label + "=" + new DecimalFormat("0.0000").format(mass) + "u";

// ResidueDictionary.findResidueType(type_name)  - the way back
String[] tokens = type_name.split("=");
String name = tokens[0];
double mass = Double.valueOf(tokens[1].substring(0, tokens[1].length()-1));   // strips the "u"
```

So the label shares one string with the mass, and `=` is what tells them apart.

| the user types | stored as | `split("=")` gives | read back as |
|---|---|---|---|
| `Ser_Thr` | `Ser_Thr=87.0320u` | `["Ser_Thr", "87.0320u"]` | label `Ser_Thr`, 87.0320 ✓ |
| `Ser=Thr` — **today** | `Ser=Thr=87.0320u` | `["Ser", "Thr", "87.0320u"]` | label **truncated to `Ser`**, and the mass parsed from `"Th"` — *(measured: `For input string: "Th"`)* |
| `Ser=Thr` — **escaped** | `Ser_3D_Thr=87.0320u` | `["Ser_3D_Thr", "87.0320u"]` | decode → `Ser=Thr`, 87.0320 ✓ |

The record's **shape** does not change. What changes is that **the label field stops being literal**,
and three things follow from that:

**The mass, not just the label, depends on it.** The failure in row two is a `NumberFormatException`
about the mass. Escaping `=` is not cosmetic - it is what makes `tokens.length` reliably 2, so the
mass can be found at all. Today it can be 3 or more.

**Everything that displays a type name has to decode.** `getTypeName()` returns the stored form, and
the canvas draws it: *(measured)* a reducing end round-trips as `Ser_2F_Thr=87.0320u` and renders at
436×111, the width tracking the label's length. Without a decode step in the renderer and in the
dialog, the user is shown `Ser_3D_Thr` where they typed `Ser=Thr`.

**Files written before the change become ambiguous, and only those.** A label typed literally as
`Ser_3D_Thr` is legal today *(measured: reads, renders 438×111)*. A new reader would decode it to
`Ser=Thr` - silently changing a label somebody chose. The new **writer** is unambiguous, because it
escapes its own introducer and would store that label as `Ser_5F_3D_5F_Thr`; the collision is
strictly between the new reader and old files. Nothing in the format distinguishes the two, so this
is a decision to take openly: accept it as rare, or add a marker that says "this label is encoded",
which is a format change and belongs with [open question 4](#open).

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

## 1.7 What core does not round-trip

Measured on 1.40.0, for anyone writing a conformance test:

| corpus | result |
|---|---|
| 14 `.gws` written by GlycoWorkbench | **14 read, 0 fail, 0 textually identical** — the writer states mass options the 2008 writer left implicit |
| 1,631 structures via WURCS → GWS → WURCS | 60.9% survive · 2.9% come back empty · 6.1% come back different · **30.1% cannot be read back** |

The first line matters for test design: **a GWS baseline must compare structures, not text**, or
every file GlycoWorkbench ever wrote reads as a failure.

---

# Part 2 — GWS extended (GlycanCore)

GlycanCore implements everything in Part 1 with three corrections and five additions. This project
reads **none** of the additions. *(Measured: of 47 GWS strings in GlycanCore's own test file, this
project reads 25 and fails on 22.)*

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

Listed because a specification that answers them silently is worse than one that admits them.

1. **Do the two dialects converge?** If they are meant to, this document becomes the migration plan;
   if not, it should say which software writes which, so that a `.gws` file carries an expectation.
   This sits beside the fork question in #226 and is a decision for people, not parsers.
2. **Should the name class be escaped or widened?** Escaping (`_2F_`) needs no format change and no
   flag day; widening splits files into old and new. Whatever is chosen must cover the dictionary's
   own names, not only the characters a user asked for — the six above are the requirement. If the
   answer is escaping, three sub-decisions come with it and none is technical: the escape form, the
   treatment of non-ASCII, and whether `=` is escaped (it must be, but that changes how a custom
   reducing end is stored).
3. **Is the `a-zA-z` span to be corrected here?** It admits `]` and `^`, both reserved. Narrowing it
   could reject a file somebody already holds, which is why it has not been done quietly.
4. **Does the `$` tail get a keyed form?** Tokens past the fifth are already ignored by every reader
   measured, so `k=v` fields would be backward compatible — but the writer would have to carry them,
   which it does not today.
5. **What is authoritative when the writer and the reader disagree?** Six residue names can be
   written and not read. Widening the reader rescues existing files; narrowing the writer protects
   future ones. Both are defensible and they are not the same choice.

# Conformance corpus

Neither collection is in this repository yet; both should be.

| set | size | source |
|---|---|---|
| GlycoWorkbench examples | 14 | `glycoinfo/eurocarbdb`, `application/GlycoWorkbench/examples/` — the only witness to the format as originally produced, and two exercise the multi-structure path |
| GlycanCore test strings | 47 | `glycancore`, `src/test/java/org/glycoinfo/glycanbuilder/io/TestGWSIO.java` — 25 core, 22 extended, which is what makes the boundary in this document measurable rather than asserted |
| Generated | any | WURCS corpus → GWS → back. Tests whether today's writer and reader agree; **does not** test whether we read what GlycoWorkbench wrote |
