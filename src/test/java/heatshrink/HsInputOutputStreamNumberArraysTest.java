package heatshrink;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
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
			ChunkyInputStream chunks = new ChunkyInputStream();
			HsInputStream input = new HsInputStream(chunks, 9, 8);
			DataInputStream in = new DataInputStream(input);

			while ((l = r.readLine()) != null) {
				written.clear();
				read.clear();
				output.reset();
				input.clear();

				try (DataOutputStream out = new DataOutputStream(new HsOutputStream(output, 9, 8))) {
					for (String s : l.split(",")) {
						T v = stringMapper.apply(s);
						written.add(v);
						writer.accept(out, v);
					}
				}

				chunks.setNext(output.toByteArray());
				for (int i = 0; i < written.size(); i++) {
					read.add(reader.apply(in));
				}

				Assert.assertEquals(written, read);
			}
		}
	}

	private static final class ChunkyInputStream extends InputStream {
		private ByteBuffer next;

		public void setNext(byte[] b) {
			this.next = ByteBuffer.wrap(b);
		}

		@Override
		public int read(byte[] b) throws IOException {
			int r = Math.min(next.remaining(), b.length);
			next.get(b, 0, r);
			return r;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int r = Math.min(next.remaining(), len);
			next.get(b, off, r);
			return r;
		}

		@Override
		public int read() throws IOException {
			if (!next.hasRemaining()) return -1;
			return next.get() & 0xff;
		}
	}
}
