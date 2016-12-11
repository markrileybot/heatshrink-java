package heatshrink;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mriley on 12/2/16.
 */
public final class TestData {

	public static Iterable<Object[]> getTestParameters() {
		List<Object[]> result = new ArrayList<>();
		for(TestFile testFile : getTestFiles()) {
			result.add(new Object[]{testFile});
		}
		if(result.isEmpty()) {
			throw new RuntimeException("No test files found!");
		}
		return result;
	}

	public static List<TestFile> getTestFiles() {
		List<TestFile> testFiles = new ArrayList<>();
		File file = new File(TestData.class.getResource("/testfiles").getFile()).getAbsoluteFile();
		File[] files = file.listFiles();
		if(files == null || files.length == 0) {
			throw new RuntimeException("No test files found in " + file.getAbsolutePath());
		}
		for (File f : files) {
			try {
				testFiles.add(new TestFile(f));
			} catch (Exception ignored) {}
		}
		return testFiles;
	}

	private TestData() {}
}
