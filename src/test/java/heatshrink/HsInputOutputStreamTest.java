package heatshrink;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by mriley on 12/10/16.
 */
@RunWith(Parameterized.class)
public class HsInputOutputStreamTest {

	@Parameters(name="{0}")
	public static Iterable<Object[]> generateParameters() {
		return TestData.getTestParameters();
	}

	private final TestFile testFile;

	public HsInputOutputStreamTest(TestFile testFile) {
		this.testFile = testFile;
	}

	@Test
	public void testWriteRead() throws IOException {
		byte[] uncompressed = FileUtils.readFileToByteArray(testFile.getUncompressed());
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try(HsOutputStream hso = new HsOutputStream(output, testFile.getWindowSize(), testFile.getLookaheadSize())) {
			hso.write(uncompressed);
		}
		try(HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(output.toByteArray()), testFile.getWindowSize(), testFile.getLookaheadSize())) {
			byte[] uncompressed2 = new byte[uncompressed.length];
			IOUtils.read(hsi, uncompressed2);
			Assert.assertArrayEquals(uncompressed, uncompressed2);
		}
	}
}
