# heatshrink-java

Java library used to encode/decode [heatshrink] compressed data.

## Building

```bash
./gradlew build
```

## Gradle dependency

build.gradle:

```groovy

repositories {
	maven { url 'https://dl.bintray.com/mrileybot/markrileybot/' }
}
    
dependencies {
	compile 'com.github.markrileybot.heatshrink:heatshrink-java:0.4.0'
}

```

## Usage

### Java library

```java

int windowSize = 9;
int lookaheadSize = 8;
ByteArrayOutputStream baos = new ByteArrayOutputStream();

try(HsOutputStream out = new HsOutputStream(baos, windowSize, lookaheadSize)) {
	out.write("ABCABCABCABCABCABC".getBytes());
}

try(HsInputStream hsi = new HsInputStream(new ByteArrayInutStream(baos.toByteArray()), windowSize, lookaheadSize)) {
	byte[] res = new byte[512];
	int len = hsi.read(res);
	System.out.println(new String(res, 0, len));
}

```

### CLI

```bash

$ java -jar heatshrink-java-exe.jar [-h] [-e|-d] [-v] [-w SIZE] [-l BITS] [IN_FILE] [OUT_FILE]

```

## Status
[![Build Status](https://github.com/markrileybot/heatshrink-java/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/markrileybot/heatshrink-java/actions/workflows/gradle-publish.yml)

[heatshrink]: https://github.com/atomicobject/heatshrink

