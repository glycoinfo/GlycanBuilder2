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
# Windows / Linux
java -jar ./target/glycanbuilder2-jar-with-dependencies.jar

# macOS
java -XstartOnFirstThread -jar ./target/glycanbuilder2-jar-with-dependencies.jar
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

## Publications

* [Shinichiro Tsuchiya, Nobuyuki P. Aoki, Daisuke Shinmachi, Masaaki Matsubara, Issaku Yamada, Kiyoko F. Aoki-Kinoshita, Hisashi Narimatsu,
Implementation of GlycanBuilder to draw a wide variety of ambiguous glycans, Carbohydrate Research, Volume 445, 2017, Pages 104-116](https://www.sciencedirect.com/science/article/pii/S0008621516305316)
