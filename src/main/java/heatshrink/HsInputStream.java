package heatshrink;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;

/**
 * InputStream used to decode heatshrink'd data.
 *
 * @see <a href="https://github.com/atomicobject/heatshrink">heatshrink on github</a>
 * @see <a href="https://github.com/markrileybot/heatshrink-java">heatshrink-java on github</a>
 */
public class HsInputStream extends FilterInputStream {

	private static final double LN2 = Math.log(2);

	/**
	 * State machine states.  Don't really need this but it's nice
	 * to match the c code.
	 */
	enum State {
		TAG_BIT,                /* tag bit */
		YIELD_LITERAL,          /* ready to yield literal byte */
		BACKREF_BOUNDS,          /* ready to yield backref index/count */
		YIELD_BACKREF,          /* ready to yield back-reference */
		BUFFER_EMPTY,           /* Not enough data to continue */
	}

	/**
	 * buffer of compressed input bytes
	 */
	private final byte[] inputBuffer;
	/**
	 * window of data used for backref lookup
	 */
	private final byte[] window;
	/**
	 * number of bits in the window
	 */
	private final int windowSize;
	/**
	 * number of bits to lookahead
	 */
	private final int lookaheadSize;

	/**
	 * State machine state
	 */
	private State state;

	/**
	 * Input buffer bounds
	 */
	private int inputBufferPos;
	private int inputBufferLen;
	private boolean inputExhausted;

	/**
	 * Current byte bit reader bounds
	 */
	private int currentBytePos;
	private int currentByte;

	/**
	 * Current backref position
	 */
	private int windowPos;

	/**
	 * backref read index and count
	 */
	private int outputCount;
	private int outputIndex;

	/**
	 * read() tmps
	 */
	private final byte[] tmp = new byte[1];
	private final ReadResult rr = new ReadResult();

	/**
	 * Creates a <code>FilterInputStream</code>
	 * by assigning the  argument <code>in</code>
	 * to the field <code>this.in</code> so as
	 * to remember it for later use.
	 *
	 * @param in the underlying input stream, or <code>null</code> if
	 *           this instance is to be created without an underlying stream.
	 */
	public HsInputStream(java.io.InputStream in) {
		this(in, 2 << 11, 11, 4);
	}


	public HsInputStream(java.io.InputStream in, int windowSize, int lookaheadSize) {
		this(in, 2 << windowSize, windowSize, lookaheadSize);
	}

	public HsInputStream(java.io.InputStream in, int bufferSize, int windowSize, int lookaheadSize) {
		super(in);
		this.inputBuffer = new byte[Math.max(2 << windowSize, bufferSize)];
		this.window = new byte[1 << windowSize];
		this.windowSize = windowSize;
		this.lookaheadSize = lookaheadSize;
		clear();
	}

	/**
	 * Reads the next byte of data from this input stream. The value
	 * byte is returned as an <code>int</code> in the range
	 * <code>0</code> to <code>255</code>. If no byte is available
	 * because the end of the stream has been reached, the value
	 * <code>-1</code> is returned. This method blocks until input data
	 * is available, the end of the stream is detected, or an exception
	 * is thrown.
	 * <p>
	 * This method
	 * simply performs <code>in.read()</code> and returns the result.
	 *
	 * @return     the next byte of data, or <code>-1</code> if the end of the
	 *             stream is reached.
	 * @exception IOException  if an I/O error occurs.
	 * @see        java.io.FilterInputStream#in
	 */
	public int read() throws IOException {
		return read(tmp) <= 0 ? -1 : tmp[0] & 0xff;
	}

	/**
	 * Reads up to <code>byte.length</code> bytes of data from this
	 * input stream into an array of bytes. This method blocks until some
	 * input is available.
	 * <p>
	 * This method simply performs the call
	 * <code>read(b, 0, b.length)</code> and returns
	 * the  result. It is important that it does
	 * <i>not</i> do <code>in.read(b)</code> instead;
	 * certain subclasses of  <code>FilterInputStream</code>
	 * depend on the implementation strategy actually
	 * used.
	 *
	 * @param      b   the buffer into which the data is read.
	 * @return     the total number of bytes read into the buffer, or
	 *             <code>-1</code> if there is no more data because the end of
	 *             the stream has been reached.
	 * @exception  IOException  if an I/O error occurs.
	 * @exception  EOFException if the end of the input was reached before processing completes
	 * @see        java.io.FilterInputStream#read(byte[], int, int)
	 */
	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * Reads up to <code>len</code> bytes of data from this input stream
	 * into an array of bytes. If <code>len</code> is not zero, the method
	 * blocks until some input is available; otherwise, no
	 * bytes are read and <code>0</code> is returned.
	 * <p>
	 * This method simply performs <code>in.read(b, off, len)</code>
	 * and returns the result.
	 *
	 * @param      b     the buffer into which the data is read.
	 * @param      off   the start offset in the destination array <code>b</code>
	 * @param      len   the maximum number of bytes read.
	 * @return     the total number of bytes read into the buffer, or
	 *             <code>-1</code> if there is no more data because the end of
	 *             the stream has been reached.
	 * @exception  NullPointerException If <code>b</code> is <code>null</code>.
	 * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
	 * <code>len</code> is negative, or <code>len</code> is greater than
	 * <code>b.length - off</code>
	 * @exception  IOException  if an I/O error occurs.
	 * @exception  EOFException if the end of the input was reached before processing completes
	 * @see        java.io.FilterInputStream#in
	 */
	public int read(byte b[], int off, int len) throws IOException {
		rr.set(b, off, len);
		while(rr.off < rr.end) {
			State lastState = state;

			switch (lastState) {
				case TAG_BIT:
					state = readTagBit();
					break;
				case YIELD_LITERAL:
					state = readLiteral(rr);
					break;
				case BACKREF_BOUNDS:
					state = readBackrefBounds();
					break;
				case YIELD_BACKREF:
					state = readBackref(rr);
					break;
				case BUFFER_EMPTY:
					break;
			}

			if(state == State.BUFFER_EMPTY) {
				state = lastState;
				break;
			}
		}

		int numRead = rr.off - off;
		return numRead > 0 ? numRead : inputExhausted ? -1 : 0;
	}

	private State readTagBit() throws IOException {
		int bits = getBits(1);  // get tag bit
		if(bits == -1) {
			return State.BUFFER_EMPTY;
		} else if (bits != 0) {
			return State.YIELD_LITERAL;
		}
		outputCount = outputIndex = 0;
		return State.BACKREF_BOUNDS;
	}

	private State readBackrefBounds() throws IOException {
		int bits = getBits(windowSize);
		if(bits == -1) return State.BUFFER_EMPTY;
		outputIndex = bits + 1;

		bits = getBits(lookaheadSize);
		if(bits == -1) return State.BUFFER_EMPTY;
		outputCount = bits + 1;

		System.err.println("BR: o=" + outputIndex + ", l=" + outputCount + ", cwp=" + windowPos);
		int mask = (1 << windowSize) - 1;
		for(int i = 0; i < outputCount; i++) {
			if(i > 0) System.err.print(", ");
			char c = (char) window[((windowPos + i) - outputIndex) & mask];
			System.err.print(c);
		}
		System.err.println();
		return State.YIELD_BACKREF;
	}

	private State readBackref(ReadResult rr) {
		int count = Math.min(rr.end - rr.off, outputCount);
		if(count > 0) {
			int mask = (1 << windowSize) - 1;
			for (int i = 0; i < count; i++) {
				byte c = window[(windowPos - outputIndex) & mask];
				rr.b[rr.off++] = c;
				window[windowPos++ & mask] = c;
			}
			outputCount -= count;
			if (outputCount == 0) {
				return State.TAG_BIT;
			}
		}
		return State.YIELD_BACKREF;
	}

	private State readLiteral(ReadResult rr) throws IOException {
		if(rr.off < rr.end) {
			int bits = getBits(8);
			if(bits == -1) return State.BUFFER_EMPTY;
			int mask = (1 << windowSize)  - 1;
			byte c = (byte) (bits & 0xff);
			window[windowPos++ & mask] = c;
			rr.b[rr.off++] = c;
			return State.TAG_BIT;
		}
		return State.YIELD_LITERAL;
	}

	/**
	 * Skips over and discards <code>n</code> bytes of data from the
	 * input stream. The <code>skip</code> method may, for a variety of
	 * reasons, end up skipping over some smaller number of bytes,
	 * possibly <code>0</code>. The actual number of bytes skipped is
	 * returned.
	 * <p>
	 * This method simply performs <code>in.skip(n)</code>.
	 *
	 * @param      n   the number of bytes to be skipped.
	 * @return     the actual number of bytes skipped.
	 * @exception  IOException  if the stream does not support seek,
	 *                          or if some other I/O error occurs.
	 */
	public long skip(long n) throws IOException {
		long r;
		for(r = 0; r < n; r++) {
			if(read(tmp) <= 0) break;
		}
		return r;
	}

	/**
	 * Returns an estimate of the number of bytes that can be read (or
	 * skipped over) from this input stream without blocking by the next
	 * caller of a method for this input stream. The next caller might be
	 * the same thread or another thread.  A single read or skip of this
	 * many bytes will not block, but may read or skip fewer bytes.
	 * <p>
	 * This method returns the result of {@link #in in}.available().
	 *
	 * @return     an estimate of the number of bytes that can be read (or skipped
	 *             over) from this input stream without blocking.
	 * @exception  IOException  if an I/O error occurs.
	 */
	public int available() throws IOException {
		return in.available();
	}

	/**
	 * Closes this input stream and releases any system resources
	 * associated with the stream.
	 * This
	 * method simply performs <code>in.close()</code>.
	 *
	 * @exception  IOException  if an I/O error occurs.
	 * @see        java.io.FilterInputStream#in
	 */
	public void close() throws IOException {
		in.close();
	}

	/**
	 * Marks the current position in this input stream. A subsequent
	 * call to the <code>reset</code> method repositions this stream at
	 * the last marked position so that subsequent reads re-read the same bytes.
	 * <p>
	 * The <code>readlimit</code> argument tells this input stream to
	 * allow that many bytes to be read before the mark position gets
	 * invalidated.
	 * <p>
	 * This method simply performs <code>in.mark(readlimit)</code>.
	 *
	 * @param   readlimit   the maximum limit of bytes that can be read before
	 *                      the mark position becomes invalid.
	 * @see     java.io.FilterInputStream#in
	 * @see     java.io.FilterInputStream#reset()
	 */
	public void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Repositions this stream to the position at the time the
	 * <code>mark</code> method was last called on this input stream.
	 * <p>
	 * This method
	 * simply performs <code>in.reset()</code>.
	 * <p>
	 * Stream marks are intended to be used in
	 * situations where you need to read ahead a little to see what's in
	 * the stream. Often this is most easily done by invoking some
	 * general parser. If the stream is of the type handled by the
	 * parse, it just chugs along happily. If the stream is not of
	 * that type, the parser should toss an exception when it fails.
	 * If this happens within readlimit bytes, it allows the outer
	 * code to reset the stream and try another parser.
	 *
	 * @exception  IOException  if the stream has not been marked or if the
	 *               mark has been invalidated.
	 * @see        java.io.FilterInputStream#in
	 * @see        java.io.FilterInputStream#mark(int)
	 */
	public void reset() throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Tests if this input stream supports the <code>mark</code>
	 * and <code>reset</code> methods.
	 * This method
	 * simply performs <code>in.markSupported()</code>.
	 *
	 * @return  <code>true</code> if this stream type supports the
	 *          <code>mark</code> and <code>reset</code> method;
	 *          <code>false</code> otherwise.
	 * @see     java.io.FilterInputStream#in
	 * @see     java.io.InputStream#mark(int)
	 * @see     java.io.InputStream#reset()
	 */
	public boolean markSupported() {
		return false;
	}

	/**
	 * Prepare this for reuse
	 */
	private void clear() {
		state = State.TAG_BIT;
		outputCount = outputIndex = 0;
		inputBufferPos = inputBufferLen = 0;
		currentBytePos = 0;
		windowPos = 0;
		inputExhausted = false;
	}

	private boolean ensureAvailable(int bitsRequired) throws IOException {
		int bytesRemaining = inputBufferLen - inputBufferPos;
		int bitsAvailable = bytesRemaining * 8;

		bitsRequired -= (currentBytePos > 0 ? 8 - ((Math.log(currentBytePos) / LN2) + 1) : 0);
		if(bitsRequired > bitsAvailable) {
			if(bytesRemaining > 0) {
				// lame buffer shift won't happen often
				System.arraycopy(inputBuffer, inputBufferPos, inputBuffer, 0, bytesRemaining);
			}
			inputBufferPos = 0;
			inputBufferLen = bytesRemaining;
			int numRead = in.read(inputBuffer, bytesRemaining, inputBuffer.length - bytesRemaining);
			if(numRead > -1) {
				inputBufferLen += numRead;
			} else {
				inputExhausted = true;
			}
			bitsAvailable = inputBufferLen * 8;
		}

		return bitsAvailable >= bitsRequired;
	}

	private int getBits(int numBits) throws IOException {
		int ret = 0;
		if(!ensureAvailable(numBits)) {
			return -1;
		}
		for(; numBits > 0; numBits--, currentBytePos >>= 1) {
			if(currentBytePos == 0) {
				currentByte = inputBuffer[inputBufferPos++];
				currentBytePos = 0x80;
			}

			ret <<= 1;
			if (currentBytePos == 0x80 && numBits >= 8) {
				ret <<= 7;
				ret |= currentByte & 0xff;
				numBits -= 7;
				currentBytePos = 0;
			} else if ((currentByte & currentBytePos) != 0) {
				ret |= 0x01;
			}
		}

		return ret;
	}

	private static final class ReadResult {
		int off;
		int len;
		int end;
		byte[] b;

		private ReadResult set(byte[] b, int off, int len) {
			this.b = b;
			this.off = off;
			this.len = len;
			this.end = off + len;
			return this;
		}
	}
}
