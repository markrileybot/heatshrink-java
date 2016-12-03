package heatshrink;

import java.io.File;

/**
 * Created by mriley on 12/2/16.
 */
public class TestFile {

	private final File compressed;
	private final File uncompressed;
	private final int windowSize;
	private final int lookaheadSize;

	public TestFile(File compressed) {
		this.compressed = compressed;
		String name = compressed.getName();
		int didx = -1;
		this.lookaheadSize = Integer.parseInt(name.substring((didx = getDotIndex(name)) + 1));
		name = name.substring(0, didx);
		this.windowSize = Integer.parseInt(name.substring((didx = getDotIndex(name)) + 1));
		name = name.substring(0, didx);
		didx = getDotIndex(name);
		name = name.substring(0, didx);
		this.uncompressed = new File(compressed.getParent(), name);
		if(!this.uncompressed.exists()) throw new IllegalArgumentException("Invalid file name");
	}

	private int getDotIndex(String name) {
		int didx;
		didx = name.lastIndexOf('.');
		if(didx <= 0) throw new IllegalArgumentException("Invalid file name");
		return didx;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public int getLookaheadSize() {
		return lookaheadSize;
	}

	public File getCompressed() {
		return compressed;
	}

	public File getUncompressed() {
		return uncompressed;
	}

	@Override
	public String toString() {
		return "TestFile{" +
				"compressed=" + compressed.getName() +
				", uncompressed=" + uncompressed.getName() +
				", windowSize=" + windowSize +
				", lookaheadSize=" + lookaheadSize +
				'}';
	}
}
