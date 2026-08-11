# GlycanBuilder2

A tool for drawing and editing glycans intuitively using [SNFG](https://www.ncbi.nlm.nih.gov/glycans/snfg.html) symbols.

## Downloads

### Windows
Install from the Microsoft Store:

[https://apps.microsoft.com/detail/9pp6bsnx71jl](https://apps.microsoft.com/detail/9pp6bsnx71jl)

### macOS / Linux
Download the installer from [GitHub Releases](https://github.com/glycoinfo/GlycanBuilder2/releases).

| File | Platform |
|---|---|
| `GlycanBuilder2-*.ARM64.dmg` | macOS (Apple Silicon) |
| `GlycanBuilder2-*.X86_64.dmg` | macOS (Intel) |
| `glycanbuilder2_*_amd64.deb` | Linux (Debian/Ubuntu) |
| `glycanbuilder2-*.alma9.x86_64.rpm` | Linux (AlmaLinux 9) |
| `glycanbuilder2-*.alma10.x86_64.rpm` | Linux (AlmaLinux 10) |

## Requirements

### Using the installer
No Java installation required. The Java runtime is bundled.

### Building from source
- Java 8 (or later)
- Maven 3.6 (or later)

## Build from Source

Clone this repository:
```
git clone https://github.com/glycoinfo/GlycanBuilder2.git
cd GlycanBuilder2
```

Compile:
```
mvn clean compile
```

If a certificate error such as "PKIX path validation failed" occurs, try:
```
mvn clean compile -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true
```

Build a runnable JAR:
```
mvn clean -P make-fat-jar package
```

Run the JAR:
```
java -jar ./target/glycanbuilder2-jar-with-dependencies.jar
```

## Usage

### Import WURCS string

![Imgur](https://i.imgur.com/6RcNetX.png)
1. Click **Add structure from string** (Red marked).
2. Paste WURCS string into the text area, and select **WURCS2** in the **input sequence format** (Red marked).
3. When click **import** button, represent glycan image on the canvas.

### Export WURCS string

![Import](https://i.imgur.com/6eQ1qkb.png)
1. Drag and select a glycan image on the canvas.
2. Click **Get string from structure** (Red marked).
3. Select **WURCS2** in the **String encoded**.
4. WURCS2 string is output.

### Export images

![Image](https://i.imgur.com/XXmnrdg.png)
1. Drag and select a glycan image on the canvas.
2. Click **Export to graphical formats** on the **File** tab and select the image format.
3. Select a directory to save the image.

## Release Notes

Please see [CHANGELOG.md](CHANGELOG.md) for details.

## Releasing

The steps in order. The tag is the one to get right: the installer workflow matches on it, and a tag
it cannot match is a release with no installers.

**1. Branches.** `master` is never committed to directly and takes pull requests **from `develop`
only**. Work is done on branches off `develop` and merged into `develop`.

**2. Version.** On `develop`, in one commit, immediately before opening the pull request to `master`:
raise the version in `pom.xml` and add the `CHANGELOG.md` entry together. **A branch merged into
`develop` does not raise the version** — leave `pom.xml` alone there, or two branches in flight both
claim the same number.

**3. Merge** `develop` into `master` by pull request.

**4. Tag** that merge commit `vMAJOR.MINOR.PATCH` — e.g. `v1.27.0`. The leading `v` and all three
parts are required, because the GitHub Actions workflow reads them. GitHub or local, either is fine.

**5. Deploy**, locally, from `master` at the versioned commit:

```
mvn deploy
```

This needs a GitHub PAT. Afterwards check the version is listed under
[MavenRepository](https://github.com/glycoinfo/MavenRepository/tree/master/org/eurocarbdb/glycanbuilder/glycanbuilder2).

**6. Installers.** Run [Release GlycanBuilder2](https://github.com/glycoinfo/GlycanBuilder2/actions/workflows/release.yml)
from "Run workflow", giving it the tag from step 4.

**7. Windows (manual).** The MSIX has to be submitted to the Microsoft Store by hand:

1. Download the `glycanbuilder2-installer-windows` artifact from that workflow run and unzip it to
   get `GlycanBuilder2.msix`.
2. Sign in to Microsoft Partner Center with the project's store account. This needs two-factor
   authentication, so it also needs whoever holds the authenticator — plan for that, since it is the
   usual reason this step waits.
3. Apps and games → **GlycanBuilder** → product update → **Packages**.
4. Drag `GlycanBuilder2.msix` in and Save. The previous version's package is removed automatically.
5. **Submit for certification**, then wait: the Store page updates itself once certification passes.

**8. Release label.** The workflow leaves the release as a Pre-Release. On the
[releases page](https://github.com/glycoinfo/GlycanBuilder2/releases), Edit it, choose **Latest**
under "Release label", and Update release.

## Publications

* [Shinichiro Tsuchiya, Nobuyuki P. Aoki, Daisuke Shinmachi, Masaaki Matsubara, Issaku Yamada, Kiyoko F. Aoki-Kinoshita, Hisashi Narimatsu,
Implementation of GlycanBuilder to draw a wide variety of ambiguous glycans, Carbohydrate Research, Volume 445, 2017, Pages 104-116](https://www.sciencedirect.com/science/article/pii/S0008621516305316)
