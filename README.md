# GlycanBuilder2
<!--
書いておくこと（書き終わったものは文頭に"*"を書いておく）

* 実行ファイルへのリンク（RINGSで公開されているもの, GLICで公開されているとのこと）
  64bitOSで実行することを想定して良さそう、32bitは考慮しない

* ビルドのやり方
* 実行ファイルの実行方法
* 論文の書誌情報
  マニュアルはどこかにあっただろうか？

* GUIの使用する場合の操作方法（必要最低限の操作は書いておいたほうがいいかもしれない）
* Import/Exportのやり方だけでも書いておく方がユーザビリティに富むと思われるが
-->

## Requirement
* Java 8 (or later)
* maven 3.6 (or later)

## Compile
Clone this repository in the local repository.
```
$ git clone https://github.com/glycoinfo/GlycanBuilder2.git
```

Move to the cloned local repository and compile the source files
```
$ cd ~/Directory_of_local_repository/GlycanBuilder2
$ mvn clean compile
```

## JAR file
Runable JAR (Java Archive) file is generate the below command.
```
$ mvn clean package
```

When compilation in finished, jar file is created in the target folder.
>[INFO] Building jar: /../../Directory_of_local_repository/GlycanBuilder2/target/wurcsextend-1.0.4-SNAPSHOT-jar-with-dependencies.jar

```
java -XstartOnFirstThread -jar /../../Directory_of_local_repository/GlycanBuilder2/target/wurcsextend-1.0.4-SNAPSHOT-jar-with-dependencies.jar
```

## Example

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

## Publications
* [Shinichiro Tsuchiya, Nobuyuki P. Aoki, Daisuke Shinmachi, Masaaki Matsubara, Issaku Yamada, Kiyoko F. Aoki-Kinoshita, Hisashi Narimatsu,
Implementation of GlycanBuilder to draw a wide variety of ambiguous glycans, Carbohydrate Research, Volume 445, 2017, Pages 104-116](https://www.sciencedirect.com/science/article/pii/S0008621516305316)

## Downloads

### Standalone executable GlycanBuilder2
* [GlycanBuilder2](http://www.rings.t.soka.ac.jp/downloads.html)
* Supported OS : 
  * Windowds (64bit)
  * Mac OS X (64bit)
