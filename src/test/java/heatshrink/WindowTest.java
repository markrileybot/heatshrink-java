package heatshrink;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mriley on 12/4/16.
 */
public class WindowTest {

	@Test
	public void testCreate() {
		Window window = new Window(11, 4);
		Assert.assertEquals(11, window.getWindowSize());
		Assert.assertEquals(4, window.getLookaheadSize());
	}

	@Test
	public void testSimpleSearch() {
		Window window = new Window(8, 4);
		window.push((byte) 'a');
		window.push((byte) 'b');
		window.push((byte) 'c');
		Assert.assertEquals(0, window.get((byte) 'a'));
		Assert.assertEquals(1, window.get((byte) 'b'));
		Assert.assertEquals(2, window.get((byte) 'c'));
		Assert.assertEquals(-1, window.get((byte) 'z'));
	}

	@Test
	public void testFillWindowThenSearch() {
		Window window = new Window(4, 3);
		int size = 8 << 2 - 1;
		for(int i = 0; i < size; i++) {
			window.push((byte) 'a');
			window.push((byte) 'b');
			window.push((byte) 'c');
			System.out.println(window);
		}

		Assert.assertEquals(0, window.get((byte) 'a'));
		Assert.assertEquals(1, window.get((byte) 'b'));
		Assert.assertEquals(2, window.get((byte) 'c'));
		Assert.assertEquals(-1, window.get((byte) 'z'));
	}
}
