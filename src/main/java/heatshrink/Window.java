package heatshrink;

import java.util.Arrays;

/**
 * Created by mriley on 12/4/16.
 */
public class Window {

	private final int windowSize;
	private final int lookaheadSize;

	private final int[] search;

	private final int mask;
	private final byte[] buffer;
	private int pos;

	public Window(int windowSize, int lookaheadSize) {
		this.windowSize = windowSize;
		this.lookaheadSize = lookaheadSize;
		this.buffer = new byte[1 << windowSize];
		this.mask = (1 << windowSize) - 1;
		this.search = new int[256];
		Arrays.fill(buffer, (byte) 0xff);
		Arrays.fill(search, (byte) 0xff);
	}

	public int getWindowSize() {
		return windowSize;
	}

	public int getLookaheadSize() {
		return lookaheadSize;
	}

	public void push(byte b) {
		int index = pos++ & mask;
		byte old = buffer[index];
		buffer[index] = b;
		search[old & 0xff] = -1;
		search[b   & 0xff] = index;
	}

	public int get(byte b) {
		return search[b];
	}

	@Override
	public String toString() {
		return "Window{" +
				"search=" + Arrays.toString(search) +
				",\nbuffer=" + Arrays.toString(buffer) +
				'}';
	}
}
