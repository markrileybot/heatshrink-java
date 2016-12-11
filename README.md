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
	compile 'com.github.markrileybot.heatshrink:heatshrink-java:0.0.3'
}

```

## Usage

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

## Status
[![Build Status](https://travis-ci.org/markrileybot/heatshrink-java.png)](http://travis-ci.org/markrileybot/heatshrink-java)
[![Coverage Status](https://coveralls.io/repos/github/markrileybot/heatshrink-java/badge.svg?branch=master)](https://coveralls.io/github/markrileybot/heatshrink-java?branch=master)
[![Download](https://api.bintray.com/packages/mrileybot/markrileybot/heatshrink-java/images/download.svg) ](https://bintray.com/mrileybot/markrileybot/heatshrink-java/_latestVersion)

[heatshrink]: https://github.com/atomicobject/heatshrink

