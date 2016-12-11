package heatshrink;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream used to heatshrink encode data.
 *
 * @see <a href="https://github.com/atomicobject/heatshrink">heatshrink on github</a>
 * @see <a href="https://github.com/markrileybot/heatshrink-java">heatshrink-java on github</a>
 */
public class HsOutputStream extends FilterOutputStream {

	/**
	 * Window size (in bytes)
	 */
	private final int windowSize;
	/**
	 * Backref size (in bits)
	 */
	private final int lookaheadSize;
	/**
	 * Window size (in bits available)
	 */
	private final int windowBits;
	/**
	 * Backref size (in bits available)
	 */
	private final int lookaheadBits;

	/**
	 * Current byte I'm writting to
	 */
	private int currentByte;
	/**
	 * Current position I'm writing to
	 */
	private int currentBytePos = 0x80;

	/**
	 * window and position
	 */
	private final byte[] window;
	private int windowPos;

	/**
	 * write() tmps
	 */
	private final byte[] tmp = new byte[1];
	private final Result wr = new Result();

	/**
	 * Creates an output stream filter built on top of the specified
	 * underlying output stream.
	 *
	 * @param out the underlying output stream to be assigned to
	 *            the field <tt>this.out</tt> for later use, or
	 *            <code>null</code> if this instance is to be
	 *            created without an underlying stream.
	 */
	public HsOutputStream(OutputStream out, int windowSize, int lookaheadSize) {
		super(out);
		this.window = new byte[2 << windowSize];
		this.windowBits = windowSize;
		this.lookaheadBits = lookaheadSize;
		this.windowSize = 1 << windowBits;
		this.lookaheadSize = 1 << lookaheadBits;
	}

	@Override
	public void write(int b) throws IOException {
		tmp[0] = (byte) b;
		super.write(tmp);
	}

	@Override
	public void write(byte[] b) throws IOException {
		super.write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) ||
				((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}

		wr.set(b, off, len);
		while(wr.off < wr.end) {
			if(fillOutputBuffer(wr)) {
				flushOutputBuffer(false);
			}
		}
	}

	@Override
	public void flush() throws IOException {
		flushOutputBuffer(true);
		super.flush();
	}

	private boolean fillOutputBuffer(Result wr) {
		int rem = windowSize - windowPos;
		if(rem > 0) {
			rem = Math.min(rem, wr.len);
			System.arraycopy(wr.b, wr.off, window, windowPos + windowSize, rem);
			wr.off += rem;
			wr.len -= rem;
			windowPos += rem;
		}
		return windowPos == windowSize;
	}

	private void flushOutputBuffer(boolean finish) throws IOException {
		if(windowPos > 0) {
			int scanPos = 0;
			int breakEven = (1 + windowBits + lookaheadBits) / 8;

			for(; scanPos <= windowPos - (finish ? 1 : lookaheadSize); scanPos++) {
				scanPos = writeNext(scanPos, breakEven);
			}

			shiftWindow(finish, scanPos);
		}
		if(finish) {
			if(currentBytePos != 0x80) {
				flushCurrentByte();
			}
		}
	}

	private void shiftWindow(boolean finish, int scanPos) {
		// Shift window down to prepare for more datas
		if(!finish && scanPos < windowPos){
			int rem = windowSize - scanPos;
			System.arraycopy(window, windowPos - rem, window, 0, windowSize + rem);
			windowPos = rem;
		} else {
			windowPos = 0;
		}
	}

	private int writeNext(int scanPos, int breakEven) throws IOException {
		int bestMatchLen = 0;
		int bestMatchOff = 0;
		int maxMatchLen = Math.min(lookaheadSize, windowPos - scanPos);
		int end = windowSize + scanPos;
		int start = end - windowSize;

		for(int i = end - 1; i >= start; i--) {
			if(window[i + bestMatchLen] == window[end + bestMatchLen]
					&& window[i] == window[end]) {
				int l = 1;
				for(; l < maxMatchLen; l++) {
					if(window[i + l] != window[end + l]) {
						break;
					}
				}
				if(l > bestMatchLen) {
					bestMatchLen = l;
					bestMatchOff = i;
					if(bestMatchLen == maxMatchLen) {
						break;
					}
				}
			}
		}

		if(bestMatchLen > breakEven) {
			writeBackref(end - bestMatchOff, bestMatchLen);
			scanPos += bestMatchLen - 1;
		} else {
			writeLiteral(window[windowSize + scanPos]);
		}
		return scanPos;
	}

	private void writeLiteral(byte c) throws IOException {
		writeBits(1, 1);
		writeBits(8, c);
	}

	private void writeBackref(int matchStartIndex, int matchLength) throws IOException {
		writeBits(1, 0);
		writeBits(windowBits, matchStartIndex-1);
		writeBits(lookaheadBits, matchLength-1);
	}

	private void writeBits(int numBits, int value) throws IOException {
		if(numBits == 8 && currentBytePos == 0x80) {
			currentByte = value & 0xff;
			flushCurrentByte();
		} else {
			for (int i = numBits - 1; i >= 0; i--) {
				if ((value & (1 << i)) != 0) {
					currentByte |= currentBytePos;
				}
				if ((currentBytePos >>= 1) == 0) {
					flushCurrentByte();
				}
			}
		}
	}

	private void flushCurrentByte() throws IOException {
		out.write(currentByte);
		currentBytePos = 0x80;
		currentByte = 0;
	}

	public void clear() {
		currentBytePos = 0x80;
		currentByte = 0;
		windowPos = 0;
	}
}
