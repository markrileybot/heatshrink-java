package heatshrink;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Main implements AutoCloseable {

	private InputStream input;
	private OutputStream output;

	private Main(String[] args) throws FileNotFoundException {
		int windowSize = 9;
		int lookaheadBits = 8;
		boolean encode = false;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			switch (arg) {
				case "-w":
					windowSize = Integer.parseInt(args[++i]);
					break;
				case "-l":
					lookaheadBits = Integer.parseInt(args[++i]);
					break;
				case "-e":
					encode = true;
					break;
				case "-d":
					encode = false;
					break;
				case "-h":
					throw new RuntimeException("Exit");
				case "-":
					if (input == null) {
						input = System.in;
					} else if (output == null) {
						output = System.out;
					}
					break;
				default:
					if (input == null) {
						input = new FileInputStream(arg);
					} else if (output == null) {
						output = new FileOutputStream(arg);
					}
					break;
			}
		}

		if (input == null) {
			input = System.in;
		}
		if (output == null) {
			output = System.out;
		}
		if (encode) {
			output = new HsOutputStream(output, windowSize, lookaheadBits);
		} else {
			input = new HsInputStream(input, windowSize, lookaheadBits);
		}
	}

	private void run() throws IOException {
		byte[] buf = new byte[10240];
		int r;
		while ((r = input.read(buf)) != -1) {
			output.write(buf, 0, r);
		}
	}

	@Override
	public void close() {
		if (input != null) {
			try {
				input.close();
			} catch (Exception ignored) {}
		}
		if (output != null) {
			try {
				output.close();
			} catch (Exception ignored) {}
		}
	}

	public static void main(String[] args) throws Exception {
		try (Main m = new Main(args)) {
			m.run();
		} catch (Exception e) {
			System.err.println("Usage: java -jar <jarname> [-h] [-e|-d] [-v] [-w SIZE] [-l BITS] [IN_FILE] [OUT_FILE]");
			throw e;
		}
	}
}
