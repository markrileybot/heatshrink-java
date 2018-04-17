package heatshrink;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

public class HsInputOutputStreamNumberArraysTest {

	@Test
	public void testShortArrayReadWrite() throws IOException {
		testArrayReadWrite(Short::parseShort, (out, v) -> {
			try {out.writeShort(v);} catch (Exception ignored) {}
		}, in -> {
			try {return in.readShort();} catch (Exception ignored) {throw new RuntimeException(ignored);}
		});
	}

	@Test
	public void testByteArrayReadWrite() throws IOException {
		testArrayReadWrite(Byte::parseByte, (out, v) -> {
			try {out.writeByte(v);} catch (Exception ignored) {}
		}, in -> {
			try {return in.readByte();} catch (Exception ignored) {throw new RuntimeException(ignored);}
		});
	}

	@Test
	public void testIntArrayReadWrite() throws IOException {
		testArrayReadWrite(Integer::parseInt, (out, v) -> {
			try {out.writeInt(v);} catch (Exception ignored) {}
		}, in -> {
			try {return in.readInt();} catch (Exception ignored) {throw new RuntimeException(ignored);}
		});
	}

	@Test
	public void testLongArrayReadWrite() throws IOException {
		testArrayReadWrite(Long::parseLong, (out, v) -> {
			try {out.writeLong(v);} catch (Exception ignored) {}
		}, in -> {
			try {return in.readLong();} catch (Exception ignored) {throw new RuntimeException(ignored);}
		});
	}

	@Test
	public void testDoubleArrayReadWrite() throws IOException {
		testArrayReadWrite(Double::parseDouble, (out, v) -> {
			try {out.writeDouble(v);} catch (Exception ignored) {}
		}, in -> {
			try {return in.readDouble();} catch (Exception ignored) {throw new RuntimeException(ignored);}
		});
	}

	public <T> void testArrayReadWrite(Function<String, T> stringMapper,
	                                   BiConsumer<DataOutputStream, T> writer,
	                                   Function<DataInputStream, T> reader) throws IOException {
		List<T> written = new ArrayList<>();
		List<T> read = new ArrayList<>();

		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(getClass().getResourceAsStream("/testdata/arrays.csv.gz"))))) {
			String l;
			ByteArrayOutputStream output = new ByteArrayOutputStream();

			while ((l = r.readLine()) != null) {
				written.clear();
				read.clear();
				output.reset();

				try (DataOutputStream out = new DataOutputStream(new HsOutputStream(output, 9, 8))) {
					for (String s : l.split(",")) {
						T v = stringMapper.apply(s);
						written.add(v);
						writer.accept(out, v);
					}
				}

				try (DataInputStream in = new DataInputStream(new HsInputStream(new ByteArrayInputStream(output.toByteArray()), 9, 8))) {
					for (int i = 0; i < written.size(); i++) {
						read.add(reader.apply(in));
					}
				}

				Assert.assertEquals(written, read);
			}
		}
	}
}
