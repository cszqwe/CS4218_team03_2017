import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import sg.edu.nus.comp.cs4218.exception.AbstractApplicationException;
import sg.edu.nus.comp.cs4218.impl.app.HeadApplication;
import sg.edu.nus.comp.cs4218.impl.app.TailApplication;

public class TailApplicationTest {
	static TailApplication tailApp;
	static InputStream is;
	static OutputStream os;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tailApp = new TailApplication();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
	}

	@Before
	public void setUp() throws Exception {
		is = null;
		os = new ByteArrayOutputStream();
	}

	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void testTailOptionAndFile() {
		// 12 lines in test2.txt, should print lines line 8 to 12
		String args[] = "-n 5 test2.txt".split(" ");
		String expected ="line 8\nline 9\nline 10\nline 11\nline 12\n";
		try {
			tailApp.run(args, is, os);
			String output = os.toString();
			assertEquals(expected, output);
		} catch (AbstractApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void testTailOptionExceedFileLines() {
		// 4 lines in test.txt, should all 4 lines
		String args[] = "-n 999 test.txt".split(" ");
		String expected ="line 1\nline 2\nline 3\nline 4\n";
		try {
			tailApp.run(args, is, os);
			String output = os.toString();
			assertEquals(expected, output);
		} catch (AbstractApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void testTailFileOnly() {
		// 12 lines in test2.txt, should print 10 lines
		String args[] = "test2.txt".split(" ");
		String expected ="line 3\nline 4\nline 5\nline 6\nline 7\nline 8\nline 9\nline 10\nline 11\nline 12\n";
		try {
			tailApp.run(args, is, os);
			String output = os.toString();
			assertEquals(expected, output);
		} catch (AbstractApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTailEmptyInput() {
		String args[] = "".split(" ");	
		try {
			tailApp.run(args, is, os);

		} catch (AbstractApplicationException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			assertEquals("tail: File not found", e.getMessage());
		}
	}

}
