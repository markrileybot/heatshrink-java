package heatshrink;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by mriley on 12/2/16.
 */
public class HsInputStreamTest {

	@Test
	public void testMarkSupported() throws IOException {
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {}))) {
			Assert.assertFalse(hsi.markSupported());
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testMark() throws IOException {
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {}))) {
			hsi.mark(123);
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testReset() throws IOException {
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {}))) {
			hsi.reset();
		}
	}

	@Test
	public void testAvailable() throws IOException {
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {}))) {
			Assert.assertEquals(0, hsi.available());
		}
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {1,2,3}))) {
			Assert.assertEquals(3, hsi.available());
		}
	}

	@Test
	public void testSkip() throws IOException {
		try (HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[]{1,2,3}))) {
			Assert.assertEquals(3, hsi.skip(10));
		}
	}

	@Test
	public void testSkipRaw() throws IOException {
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {}))) {
			hsi.ensureAvailable(1);
			Assert.assertEquals(0, hsi.skipRaw(10));
		}
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {1,2,3}))) {
			hsi.ensureAvailable(1);
			Assert.assertEquals(3, hsi.skipRaw(10));
		}
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {0,1,2,3,4,5,6,7,8,9}))) {
			hsi.ensureAvailable(1);
			Assert.assertEquals(10, hsi.skipRaw(10));
		}
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[] {0,1,2,3,4,5,6,7,8,9}))) {
			hsi.ensureAvailable(1);
			Assert.assertEquals(5, hsi.skipRaw(5));
		}
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(new byte[1024]), 5, 4)) {
			hsi.ensureAvailable(1);
			Assert.assertEquals(513, hsi.skipRaw(513));
			Assert.assertEquals(511, hsi.skipRaw(513));
		}
	}
}
