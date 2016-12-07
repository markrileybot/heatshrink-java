package heatshrink;

import java.util.Arrays;

/**
 * Created by mriley on 12/4/16.
 */
public class Window {

	private final int windowBits;
	private final int lookaheadBits;
	private final int windowSize;
	private final int lookaheadSize;

	private final int[] search;

	private final int mask;
	private final byte[] buffer;
	private final int breakEven;
	private int pos;

	public Window(int windowBits, int lookaheadBits) {
		this.windowBits = windowBits;
		this.lookaheadBits = lookaheadBits;
		this.windowSize = 1 << windowBits;
		this.lookaheadSize = 1 << lookaheadBits;
		this.breakEven = (1 + windowBits + lookaheadBits) / 8;

		this.buffer = new byte[2 << windowBits];
		this.mask = windowSize - 1;
		this.search = new int[256];
		Arrays.fill(buffer, (byte) 0xff);
		Arrays.fill(search, (byte) 0xff);
	}

	public int getWindowBits() {
		return windowBits;
	}

	public int getLookaheadBits() {
		return lookaheadBits;
	}

	public void add(byte b) {
		int index = pos++ & mask;
		buffer[index] = b;
		search[b & 0xff] = index;
	}

	public int get(byte b) {
		return search[b & 0xff];
	}

	public boolean fill(byte[] b, int off, int len) {
		int fillLen = Math.min(len, windowBits - pos);
		System.arraycopy(b, off, buffer, windowBits, fillLen);
		return (pos += fillLen) == windowBits;
	}

	public void write() {

	}

	public int findBestMatch(int start, int end) {
		int bestMatch = 0;


		for(int i = end - 1; i >= start; i--) {

		}
	}

	@Override
	public String toString() {
		return "Window{" +
				"search=" + Arrays.toString(search) +
				",\nbuffer=" + Arrays.toString(buffer) +
				'}';
	}
}
