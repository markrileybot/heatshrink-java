package heatshrink;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class MainTest {

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Test(expected = RuntimeException.class)
	public void testHelp() throws Exception {
		Main.main(new String[] {"-h"});
	}

	@Test
	public void testEncodeFiles() throws Exception {
		for (TestFile testFile : TestData.getTestFiles()) {
			File out = tmpFolder.newFile(testFile.getUncompressed().getName());
			Main.main(new String[] {"-e"
					, "-w", String.valueOf(testFile.getWindowSize())
					, "-l", String.valueOf(testFile.getLookaheadSize())
					, testFile.getUncompressed().getAbsolutePath()
					, out.getAbsolutePath()});
			Assert.assertArrayEquals(FileUtils.readFileToByteArray(testFile.getCompressed())
					, FileUtils.readFileToByteArray(out));
			out.delete();
		}
	}


	@Test
	public void testDecodeFiles() throws Exception {
		for (TestFile testFile : TestData.getTestFiles()) {
			File out = tmpFolder.newFile(testFile.getCompressed().getName());
			Main.main(new String[] {"-d"
					, "-w", String.valueOf(testFile.getWindowSize())
					, "-l", String.valueOf(testFile.getLookaheadSize())
					, testFile.getCompressed().getAbsolutePath()
					, out.getAbsolutePath()});
			Assert.assertArrayEquals(FileUtils.readFileToByteArray(testFile.getUncompressed())
					, FileUtils.readFileToByteArray(out));
			out.delete();
		}
	}
}
