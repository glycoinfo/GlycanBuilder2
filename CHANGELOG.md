## Change log
### 1.34.0  (20260810)
* Gave an antenna one parent when reading WURCS, so it is no longer drawn outside its own
  picture (#71)
  * An antenna names the residues it may hang from and is drawn from the bracket; G42735RP names
    three and the reader, having no way to choose, linked the residue to the first and then made
    it a child of the bracket as well - two parents at once, which no tree can hold
  * Everything that walks the structure then met it twice: the renderer laid the sialic acid out
    under the bracket and translated it a second time along with the other parent's subtree,
    leaving it outside the box the renderer had reported and cut off by the edge of the image
  * The writers said it twice too - G42735RP went in as 7,13,12 and came back out of WURCS as
    8,14,13+, with a NeuAc carrying its N-acetyl twice
  * Measured on the 107 WURCS strings in the test sources: only G42735RP differs
* Read an antenna written the other way round by role (#150)
  * WURCS writes the two sides of a linkage in whichever order puts the residue linking through
    its anomeric carbon on the donor side; G42735RP says m1-f?|i?|k?, position 1 on a residue
    whose anomeric carbon is 2, and is parsed the other way round
  * Read as though the sides meant the usual thing, the antenna's own 1 was taken for the
    position on each candidate: the structure was drawn as 1-linked to its galactoses and written
    back as m2-f1|i1|k1, stating a definite position where the sequence said it was unknown
  * The same reading left a candidate residue with two parents, dropping it from the structure,
    and could hand LinkageConnector a null acceptor - G00955WX written that way did not import
  * G42735RP now draws byte for byte the picture its well-formed twin draws
* Drew a composition whether or not the reducing-end marker is shown (#153)
  * A composition has no residue privileged as the reducing end, and the renderer laid it out
    from the residue past that marker - nothing at all - so asking for one without the marker
    gave a blank 1x1 image, or a NullPointerException from the legend measuring a box that had
    never been computed
  * Whether the marker is drawn is a display preference; whether the composition is drawn no
    longer follows it
* Moved batik to 1.19 and fop to 2.11, together (#144)
  * batik 1.9 carries the SSRF and remote-class-loading run fixed later in the 1.x line -
    CVE-2019-17566, CVE-2020-11987 and the 2022 group including CVE-2022-44729 - and both
    libraries travel to every consumer
  * They cannot move apart: fop is built against one batik family and mixing them fails at
    runtime rather than at build time, so the pair comes from fop-parent's own batik.version
  * Verified past the test suite: the desktop application starts on the new pair, and
    glycanbuilder2web builds, runs and exports every format the library offers - PDF, PS, EPS,
    PNG, JPG, BMP, SVG
* Declared each plugin once, and each version
  * maven-deploy-plugin was declared twice and exec-maven-plugin had no version, both of which
    Maven warned about on every build, the first adding that future versions might no longer
    accept such a build; jdom was asked for by its pre-relocation coordinates
  * The effective pom and the resolved dependency list are unchanged - the build simply stopped
    warning
### 1.33.0  (20260810)
* Wrote the configuration where the user may write, so the application starts on Windows (#10)
  * A first run saved it to the path it had just tried to read it from - normally the bundled
    resource "/config.xml" - so the write went to whatever that name resolves to on disk: the
    working directory of whatever launched the application, which from the Windows start menu is
    C:\WINDOWS\system32, where it is denied and nothing starts
  * It saves to the per-user location the workspace already knew about, creating the directory
  * Opening a configuration that is neither a file nor a resource now answers no, instead of
    falling back to src/main/resources/config.xml - a build-tree path absent from every
    distributed jar, so the fallback could only ever throw on a first run
* Gave a glycosidic bond its linkage types, on every route in (#4)
  * The pass that works them out had no branch for an ordinary sugar-sugar bond at all, only for
    substituents and bridges, so every glycosidic linkage stayed UNVALIDATED: the donor gives up
    the OH at its anomeric centre (DEOXY), the acceptor keeps the oxygen (H_AT_OH)
  * And it ran in one place, the WURCS writer, so a structure carried placeholders until the
    moment it was written back - all three readers run it now, and GWS, WURCS and GlycoCT agree
  * Nothing written out changes: seven structures were exported to all three formats before and
    after, byte-identical, which is what stating a type in the model had to agree with
* Kept an antenna's parents when reading GlycoCT, so a glycan is drawn the same whichever
  sequence it arrived as (#62)
  * GlycoCT states them in the UND section's ParentIDs and the reader dropped them, so an antenna
    knew of no parents; the renderer asks exactly that when it decides whether to draw a link
    towards the bracket, and G00955WX came out with the link from WURCS and without it from
    GlycoCT
  * Measured on G00955WX: eleven parents on both routes now, and the two SVGs identical
* Said what has to be true of any layout, before changing one (groundwork for #58 and #71)
  * Every residue lies within the bounding box the renderer reports, and no two share a spot -
    properties that hold whatever the layout looks like, so an improvement passes them and a
    mistake does not, where a frozen SVG would fail on both
  * Nine structures hold them; G42735RP of #71 does not, and its test says so, failing the day
    #71 is fixed
### 1.32.0  (20260809)
* Wrote structures with bridges to GlycoCT, which had exported as an empty string
  * The bridge went to the namescheme converter decorated like a sugar - "?-P", which nothing
    could resolve - and undecorated it resolved to a substituent whose exchange table only knows
    the single-attachment form
  * Bridges are now typed substituent nodes in GlycoCT's own vocabulary, both attachments at 1,
    and the sugar's side of each bond typed by the atom the bridge attaches through - oxygen
    keeps the sugar's OH (o), nitrogen and sulfur replace it (d)
  * P, S, SH, N, Suc and PyrP round-trip through the GlycoCT reader; NS, PEtn and PPEtn attach
    through two different atoms whose sides the model does not record, so they still refuse
    rather than guess
* Refused a cyclic structure graph with a sentence, not a StackOverflowError
  * A WURCS with two connections between the same residues - G11127BT's bridge plus a direct
    bond - became a genuine cycle, and the first tree walk to touch it descended forever
  * The importer refuses the cycle before the document sees it, and the GWS writer guards
    itself against any cyclic graph arriving another way
* Failed in WURCS terms, not in Java's
  * A conversion failure was a raw NullPointerException or StringIndexOutOfBounds, naming
    nothing; it is now a WURCSToGlycanException saying what failed on which sequence, with the
    original chained underneath - failures that already speak pass through untouched
  * Fourteen printStackTrace calls in the conversion and model classes go through LogUtils now,
    so they answer to the logging configuration
* Moved FOP off CVE-2017-5661 (an XXE in its readers) to 2.2, with the batik 1.9 family it was
  built against - bumping fop alone fails at runtime on the first PDF export, so PDF, PS and EPS
  are now each transcoded in a test and checked for the magic bytes of the format they claim to be
### 1.31.0  (20260809)
* Read the alditol back from the reducing end's type when loading GWS
  * Every .gws file saved before 1.30.0 records a reduced end with the ring letter still "p", and
    loading one turned the alditol into a ring again: the WURCS reverted from h2122h to a cyclic
    residue, two hydrogens lighter, with nothing said
  * The rule is setReducingEndType's own, applied on the way in; files that already say "o", free
    reducing ends and structures with no sugar under the root are left exactly as they are
* Gave the generic deoxy-HexNAc its missing oxygen
  * The dictionary said C8H15NO4 where the deoxy form of HexNAc is C8H15NO5, so the generic weighed
    189.1001 against FucNAc, RhaNAc and QuiNAc at 205.0950 - one oxygen, subtracted twice
  * A structure drawn with the generic was quietly 15.9949 lighter than the same structure drawn
    with any specific residue it stands for
* Attached a substituent's side of its bond at 1 in GlycoCT
  * The exporter wrote whatever the bond recorded, which for the GAG templates was unknown, so
    gagheparin exported lines like 1:1d(2+-1)2n and GlyTouCan's graphic search rejected the
    structure: "for this substituent sulfate linkage pos must be 1"
  * Substituents only, and only where the bond says unknown - a sugar's attachment really can be
    unknown, and every core template is now held by test to export no "-1" substituent attachment
### 1.30.0  (20260808)
* Wrote a labelled reducing end as itself, where every label wrote what a free reducing end wrote
  * PA, 2AB, AA and the other eight are reductive aminations, so each leaves its sugar acyclic -
    only the alditol marker was recorded as doing so
  * A 2AB glycan weighed 120 Da more than a free one and produced the same WURCS, so two
    structures registered as one
  * Which reducing ends reduce is asked of the residue type rather than listed
* Kept the ring form and the alditol and aldehyde flags saying one thing
  * Setting the ring form now sets the flags the exporters actually read, so a caller that is not
    the desktop canvas no longer leaves a residue that says it is acyclic and writes as a ring
  * A saved alditol came back a ring, and undo, which goes through GWS, quietly un-reduced
    whatever it touched
* Stored the open-chain form in GWS, which its ring codes did not include
  * A structure drawn as an open chain could be saved and then not opened
* Drew a labelled reducing end on an acyclic sugar
  * An acyclic sugar suppressed the reducing-end symbol whatever it was, which hid every label
    once labels were correctly recorded as making their sugar acyclic
  * A plain alditol still draws none: the sugar's own marker says it
* Fixed Ctrl+Left navigating down, which it has done since the first commit
* Fixed cloning a structure built through the API rather than parsed
  * It has no bracket, which the clone reached through regardless - taking computeMass(String)
    with it, since that clones before changing the isotope

### 1.29.0  (20260808)
* Wrote eleven residues that no WURCS string could be produced for
  * Kdo, Mur, MurNAc, MurNGc, Bac, Dha, Api, and both manno-heptoses
  * Residues that could not be written now say which residue and why, instead of failing silently
  * dHexA is still refused: its definition records no carbon for the deoxy the name claims
* Added TalA, the one hexuronate SNFG names that had a symbol here but no residue
* Wrote monovalent pyruvate, using the MAP the WURCS 2.0 specification defines for it
* Changed the default configuration of Lyx from L to D
  * SNFG and PubChem both give the symbol as D-Lyxose, and the symbol drawn for it already meant D
  * A structure saved before this keeps its meaning: GWS records the configuration explicitly
* Corrected Dha, which had no anomeric carbon, configuration or ring recorded, and Api, which was
  recorded as a pyranose where it is the one furanose SNFG makes an exception of
* Read WURCS through this project's own dictionaries rather than a converter's tables
  * A residue is now recognised in every form it is written in, not only with a ring - an
    undetermined anomeric carbon, an alditol and an open chain were all going unrecognised
  * A residue drawn in the configuration that is not its default is read back as itself
  * Substituents on a residue written without a ring are no longer lost
* Fixed the legend under a residue drawn without a symbol overwriting its residue type
  * Reading one structure changed what another said, and the type's own description - the one the
    menus show - was replaced for the rest of the session
* Fixed the application failing to start on Linux distributions that no longer ship GTK2
* Removed the DJNativeSwing and SWT dependencies, which the application never used
  * The native interface was opened and pumped without any native component being created
  * Opening it forked a second JVM and pulled in a platform SWT build and its matching GTK
* Removed the platform SWT profiles that existed only to supply those dependencies
* Stopped passing -XstartOnFirstThread on macOS, which SWT needed and Swing cannot start under
  * The macOS installer built an app that opened no window

### 1.28.0  (20260807)
* Fixed bridge substituents within one monosaccharide disappearing from WURCS export
  * 4,6-pyruvate, anhydro, and "Both" type bridges used within a single residue
  * An ether bridge is written as its linkage positions alone, without "*O*"
* Fixed (S)/(R)/(X)-pyruvate collapsing to the same stereo notation on WURCS export
* Held substituent and bridge MAP codes in residue_types and cross_linked_substituent_types
  as an optional last column
* Normalized MAP codes on WURCS export, accepting either spelling on import
* Removed the GlyContainer conversion path, which the WURCS parser no longer used
* Fixed Mur and MurNGc molecular compositions
* Added warning when WURCS Composition is exported

### 1.27.0  (20260806)
* Updated README.md to add installer info
* Fixed error when root is alditol or open-chain
* Fixed onBorder check not to have linkage to substituent with placement
* Restored feature to remember last directory and recent files
* Trimmed spaces on the head of description
* Set empty icons on GAGs in Add structure
* Fixed composition representation
* Updated sentences for remembering recent files
* Fixed image for rotation button icon
* Removed action and function for root of fragment and composition
* Fixed to show linkage when terminals are added to bracket
* Changed not to rotate substituent
* Organized substituents and their categories
  * O-type,N-type,Organic,Inorganic -> O-type,N-type,Deoxy-type,P/S-type
  * Removed unused substituents and "unknown"
* Updated handling for bridge substituents
* Updated WURCS conversion for substitutents
* Removed Modification category
* Fixed other reducing end input
* Updated collision check for linkage positions

### 1.26.1  (20260728)
* Fixed anomeric symbol representation when aglycon is hidden
* Fixed not to rotate symbol
* Updated SVG export to match to other image export

### 1.26.0  (20260708)
* Added methyl-phosphate (PMe) as a new organic substituent
* Handled multi-line and selected WURCS export
* Set file extension to “gws” when saving
* Handled open-chain (aldehyde/keto) form
* Updated read process from GWS to handle parents of fragment and bridging substituent
* Updated lib: glycanformatconverter 2.10.5
* Update lib: wurcsframework 1.3.3

### 1.25.6  (20260205)
* Removed incorrect template structures

### 1.25.5  (20251125)
* Updated dependency location for artifacts in org.glycoinfo.eurocarbdb.depends

### 1.25.4  (20241008)
* Fixed wrong MAP encoding for O-linked substituents

### 1.25.3  (20240826)
* Update lib: glycanformatconverter 2.10.4

### 1.25.2  (20240520)
* Fixing the High Mannose Structure Template
* Update lib: wurcsframework 1.3.1
* Update lib: glycanformatconverter 2.10.3

### 1.25.1  (20240321)
* Update pom.xml: Add dependency of org.eclipse.swt.cocoa.macosx.aarch64
* Update lib: WURCSFramework 1.2.15

### 1.25.0  (20231031)
* changed not to export to WURCS from Composition structure.

### 1.24.0 (20230915)
* Update lib: WURCSFramework 1.2.14

### 1.23.1 (20230628)
* typo in about_builder.html. version 1.23.0-SNAPSHOT to 1.23.1

### 1.23.0 (20230628)
* Update lib: glycanformatconverter 2.9.1
* Update lib: MolecularFramework 1.0.0
* Update lib: wurcsframework 1.2.13

### 1.22.0 (20230113)
* Update lib: glycanformatconverter 2.8.2

### 1.21.0 (20230113)
* Fixed URL (https:/nexus.glycoinfo.org/content/groups/public).
* Update lib: glycanformatconverter 2.8.0
* Update lib: wurcsframework 1.2.9

### 1.20.1 (20221228)
* Changed the dependency of logger from log4j to log4j-over-slf4j.

### 1.20.0 (20221118)
* Fix a stack overflow error when drawing glycan had repeat units.

### 1.19.0 (20220829)
* Blue RGB value of the SNFG was changed `0,144,188` to `0,114,188`.
* A clearance of bracket for glycan fragments was modified. 

### 1.18.1 (20220419)
* Change register API url `https://api.gtc.beta.glycosmos.org/glycan/register` to `https://api.beta.glytoucan.org/glycan/register`

### 1.18.0 (20220418)
* Change API url `https://gtc.beta.glycosmos.org/` to `https://beta.glytoucan.org/`

### 1.17.1 (20220325)
* Change anomeric state of the reducing end of Glc3Man9GlcNAc2 (α -> β) 

### 1.17.0 (20220208)
* Add "Write" function (for GlycoWorkbench)
* Change usable undo count 50 from 20
* Change configuration of N-glycan high mannose (Man6 -> Man9)

### 1.16.0 (20220204)
* Implemented function and user-interface of GlyTouCan registration for GlycoWorkbench
  * Send Structure Data
  * GlyTouCanID List
  * Change User

### 1.15.0 (20220111)
* Bug fix (#30, #35, #36)
* GIC develop phase-2
* Add N-Glycan templates: Complex, Hybrid, Oligosaccharide
* Display an available file format in file select dialog
* Fixed reducing end of O-glycan (core1~8)

### 1.14.0 (20211201)
* Update maven configurations

### 1.13.0 (20210805)
* 


### 1.12.0 ()

### 1.11.0 ()

### 1.10.0 ()

### 1.0.91 ()

### 1.0.9 ()

### 1.0.8 ()

### 1.0.7 ()

### 1.0.6 ()

### 1.0.5-snapshot ()

### 1.0.4-snapshot ()
