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
}
