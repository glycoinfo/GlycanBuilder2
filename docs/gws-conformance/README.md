# GWS conformance data

Sample data for verifying an implementation against [`../gws-format.md`](../gws-format.md), for
GlycanBuilder2 and for glycanbuilder2web, which shares the library.

**Every "status" column was measured on 1.40.0 on 2026-09-11, before any of the specification was
implemented.** So each file is two things at once: the cases a conformant implementation must pass,
and a baseline of what the current one actually does. A row whose status says FAILS is a defect the
specification names; a row that passes must keep passing.

Nothing here was written by hand except the column headings. The GWS strings came from the library
itself — built as structures and written out — or from files other software produced.

## The files

| file | size | what it checks | spec |
|---|---|---|---|
| `c1-glycoworkbench/` | 14 `.gws` | **every file GlycoWorkbench ever shipped reads, and writes back as the identical string.** Real files, from `glycoinfo/eurocarbdb`, `application/GlycoWorkbench/examples/`. Two of them (`dermatan_sulfate`, `nglycan_2605`) hold two structures separated by `;` and must go through the multi-structure path | §1.7 C1 |
| `c2c3-dictionary-names.tsv` | 33 rows | **every residue name in the dictionaries can be written and read back.** Column 4 is the GWS this project writes today for a structure containing that residue; **all 33 are unreadable on 1.40.0**. 29 carry `-`, 3 carry parentheses, and the remaining one is `Tri-P` | §1.7 C2, C3 |
| `c4-glycancore-strings.tsv` | 48 rows | **the core/extended boundary.** GlycanCore's own test strings, each marked by measurement: 26 read here and must keep reading; 22 use an extension and must **not** | §1.7 C4, Part 2 |
| `c5-label-escapes.tsv` | 9 rows | **a label may carry any character.** Column 1 is what a user types, column 2 what the file must contain. Column 3 records that each stored form already round-trips and renders on 1.40.0 — so the escape needs no format change, only an encoder and a decoder | §1.3.1 |
| `c6-dollar-section.tsv` | 6 rows | **the `$` section is reproduced, and what core does not interpret is not destroyed.** Five of the six fail today: the writer adds a section that was absent, adds a fifth field that was absent, and drops everything past the fifth | §1.6, §1.6.1 |
| `c7-must-be-refused.tsv` | 2 rows | **a raw `$` inside a name is refused.** Both fail today, and fail in the worst way: they read with no error at all and produce an empty structure | §1.2 |

## Running them

There is no runner yet — deliberately, since the specification is not implemented. Each file is
tab-separated with a `#` heading, so a JUnit test can read it directly. Three shapes of assertion
cover everything here:

```java
// c1: read, write, compare strings
assertEquals(fileContents, write(read(fileContents)));

// c2, c3, c5, c6: read the given string, write it, compare with the required output
assertEquals(required, write(read(input)));

// c4 (extended rows), c7: the input must not produce a structure
assertThrows(Exception.class, () -> read(input));
```

`c4`'s extended rows and `c7` are the only negative cases. Note the difference between them: an
extended string is *valid GWS that core does not implement*, while `c7` is a malformed name. Both are
refused, for different reasons.

## Two cautions

**`c1` is a string comparison, and that is a choice this data justifies.** An earlier draft of the
specification required a structural comparison, because the writer normalised the `$` section and no
GlycoWorkbench file came back unchanged. The structure part of all 14 is already byte-identical
*(measured)*, and `writeGlycan` passes `ordered=false` so branch order survives — so once §1.6's
reproduction rule is in, string equality is both correct and much easier to read when it fails.

**A baseline nobody updates deliberately is worse than no test.** These status columns will start
going green as the work lands. Each change to them is a change to explain in review, not a file to
regenerate until the build passes.

## Where this came from

- `c1` — `glycoinfo/eurocarbdb`; the same 14 files are in the original GlycoWorkbench source archive
  at `storage.googleapis.com/google-code-archive-source/v2/code.google.com/glycoworkbench/source-archive.zip`
- `c2c3` — generated from `conf/residue_types` and `conf/non_symbolic_residue_types` by building each
  residue and asking `GWSParser` to write it
- `c4` — `gitlab.com/glycoinfo/glycanbuilder-module/glycancore`,
  `src/test/java/org/glycoinfo/glycanbuilder/io/TestGWSIO.java`
- `c5`, `c6`, `c7` — each stored form was created as a reducing-end name or a `$` tail, written, read
  back and rendered, and the observed result is what column 3 says
