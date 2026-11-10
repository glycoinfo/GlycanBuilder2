# The test corpora

Data the tests read, kept apart from the tests that read it, so that adding a case is editing a
table rather than writing Java.

## Where things are

| Directory | What is in it |
|---|---|
| `gws/` | GWS strings and the fourteen files GlycoWorkbench itself wrote. Pins `docs/gws-format.md`. |
| `wurcs/` | WURCS strings taken from the issues, by defect: skeleton codes, repeats, rings. |
| `residues/` | What the three residue dictionaries claim, and what the application does with it. |

## Three rules, each of which was learned the hard way

**1. The input comes from the reporter, not from us.**

> *A test that builds its input the way the code under test likes it will not find a fault on the
> way in.*

The composition encoder's tests asked for residues **by the encoder's own names**, so a composition
containing sialic acid was broken for four releases with a green suite. Every row in these tables is
therefore the string as it was reported or as another program wrote it — GlycoWorkbench's `KDO` and
`Hept`, GlyTouCan's WURCS, the reporter's paste — never a string this project's own writer produced.
Where a row *is* our own output, it says so, because that is a different test: it asks whether we can
read ourselves back, which is the weaker question.

**2. A row carries the verdict it gets today, not the verdict it should get.**

Each table has a `today` column: `pass`, or the failure as it actually comes out. That is what lets
the corpus be committed *before* the fixes, and lets a fix be measured by the rows that change rather
than by an opinion. A row that goes from `fail` to `pass` is the evidence an issue can be closed; a
row that goes the other way is a regression found the same hour it was written. #238 is a regression
we shipped, and this column is what would have caught it.

**3. What the dictionaries say is read, never transcribed.**

The count of residue names the GWS writer can emit and the reader cannot read back has been stated
as 6, then 33, and is 35. All three were hand-counts, and the first two were wrong — the first had
scanned only `residue_types`, missing `non_symbolic_residue_types`. So `residues/` holds no list of
names. The test walks `conf/residue_types`, `conf/non_symbolic_residue_types` and
`conf/compositions` itself and asserts the property, which keeps it true when somebody adds a row.

Nothing in these tables is a fixture to be regenerated when it disagrees with the code. If a row
disagrees with the code, one of the two is wrong and that is the finding.
