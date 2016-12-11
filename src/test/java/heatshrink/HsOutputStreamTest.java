package heatshrink;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by mriley on 12/10/16.
 */
public class HsOutputStreamTest {
	@Test
	public void testClear() throws IOException {
		try(HsOutputStream hso = new HsOutputStream(new ByteArrayOutputStream(), 9, 8)) {
			hso.clear();
		}
	}

	@Test(expected = NullPointerException.class)
	public void testNullBuffer() throws IOException {
		try(HsOutputStream hso = new HsOutputStream(new ByteArrayOutputStream(), 9, 8)) {
			hso.write(null, 0, 1);
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testOffGtLen() throws IOException {
		try(HsOutputStream hso = new HsOutputStream(new ByteArrayOutputStream(), 9, 8)) {
			hso.write(new byte[0], 1, 0);
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testNegOff() throws IOException {
		try(HsOutputStream hso = new HsOutputStream(new ByteArrayOutputStream(), 9, 8)) {
			hso.write(new byte[123], -1, 10);
		}
	}
	@Test(expected = IndexOutOfBoundsException.class)
	public void testNegLen() throws IOException {
		try(HsOutputStream hso = new HsOutputStream(new ByteArrayOutputStream(), 9, 8)) {
			hso.write(new byte[123], 1, -5);
		}
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testOffLenGtBuff() throws IOException {
		try(HsOutputStream hso = new HsOutputStream(new ByteArrayOutputStream(), 9, 8)) {
			hso.write(new byte[5], 1, 5);
		}
	}

	@Test
	public void testZeroLen() throws IOException {
		try(HsOutputStream hso = new HsOutputStream(new ByteArrayOutputStream(), 9, 8)) {
			hso.write(new byte[5], 1, 0);
		}
	}
}
