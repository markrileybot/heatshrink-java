package heatshrink;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * OutputStream used to heatshrink encode data.
 *
 * @see <a href="https://github.com/atomicobject/heatshrink">heatshrink on github</a>
 * @see <a href="https://github.com/markrileybot/heatshrink-java">heatshrink-java on github</a>
 */
public class HsOutputStream extends FilterOutputStream {

	private int currentByte;
	private int currentBytePos = 0x80;

	private enum State {
		NOT_FULL,              /* input buffer not full enough */
		FILLED,                /* buffer is full */
		SEARCH,                /* searching for patterns */
		YIELD_TAG_BIT,         /* yield tag bit */
		YIELD_LITERAL,         /* emit literal byte */
		YIELD_BR_INDEX,        /* yielding backref index */
		YIELD_BR_LENGTH,       /* yielding backref length */
		SAVE_BACKLOG,          /* copying buffer to backlog */
		FLUSH_BITS,            /* flush bit buffer */
		DONE,                  /* done */
	}

	private final byte[] outputBuffer;
	private int outputBufferPos;

	private final byte[] window;
	private int windowPos;
	private final int windowSize;
	private final int lookaheadSize;

	private final short[] searchIndex;

	private byte[] tmp = new byte[1];
	private final WriteResult wr = new WriteResult();

	private State state = State.NOT_FULL;

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
		this.outputBuffer = new byte[2 << windowSize];
		this.window = new byte[1 << windowSize];
		this.searchIndex = new short[2 << windowSize];
		this.windowSize = windowSize;
		this.lookaheadSize = lookaheadSize;
	}

	@Override
	public void write(int b) throws IOException {
		super.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		super.write(b);
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
				flushOutputBuffer();
			}
		}
	}

	@Override
	public void flush() throws IOException {
		flushOutputBuffer();
		super.flush();
	}

	private boolean fillOutputBuffer(WriteResult wr) {
		int rem = outputBuffer.length - outputBufferPos;
		if(rem > 0) {
			rem = Math.min(rem, wr.len);
			System.arraycopy(wr.b, wr.off, outputBuffer, outputBufferPos, rem);
			wr.off += rem;
			outputBufferPos += rem;
		}
		return outputBufferPos == outputBuffer.length;
	}

	private State flushOutputBuffer() throws IOException {
		if(outputBufferPos > 0) {
			Map<Byte, Integer> searchIndex = new HashMap<>();
			int matchStartIndex = -1;
			int matchLength = 0;
			int windowBits = 1 << windowSize;
			int lookaheadBits = 1 << lookaheadSize;
			int breakEven = (1 + windowBits + lookaheadBits) / 8;

			for (int i = 0, j = 0; i < outputBufferPos; i++, j++) {
				if (j == lookaheadBits) {
					searchIndex.clear();
					j = 0;
				}

				byte c = outputBuffer[i];
				Integer lastPos = searchIndex.put(c, i);
				if (lastPos == null) {
					if (matchStartIndex != -1 && matchLength > breakEven) {
						writeBackref(matchStartIndex, matchLength);
					} else {
						writeLiteral(c);
					}
				} else {
					if (matchStartIndex == -1) {
						matchStartIndex = lastPos;
					}
					matchLength++;
				}
			}
		}
		outputBufferPos = 0;
		return State.NOT_FULL;
	}

	private void writeLiteral(byte c) throws IOException {
		writeBits(1, 1);
		writeBits(8, c);
	}

	private void writeBackref(int matchStartIndex, int matchLength) throws IOException {
		writeBits(1, 0);
		writeBits(windowSize, matchStartIndex-1);
		writeBits(lookaheadSize, matchLength-1);
	}

	private void writeBits(int numBits, int value) throws IOException {
		for(; numBits > 0; numBits--, currentBytePos >>= 1, value >>= 1) {
			if(numBits >= 8 && currentBytePos == 0x80) {
				write(value & 0xff);
				value >>= 7;
				numBits -= 7;
				currentBytePos = 0x100;
			} else {
				currentByte <<= 1;
				if((value & 1) != 0) {
					currentByte |= 1;
				}

				if (currentBytePos == 0) {
					write(currentByte);
					currentBytePos = 0x80;
					currentByte = 0;
				}
			}
		}
	}

	private static final class WriteResult {
		int off;
		int len;
		int end;
		byte[] b;

		private WriteResult set(byte[] b, int off, int len) {
			this.b = b;
			this.off = off;
			this.len = len;
			this.end = off + len;
			return this;
		}
	}
}
