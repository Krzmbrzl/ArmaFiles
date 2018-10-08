package raven.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raven.misc.TextReader;
import raven.preprocessor.Preprocessor;

class PreprocessorTest {

	static Preprocessor prep;

	@BeforeAll
	static void setUp() throws Exception {
		prep = new Preprocessor();
	}

	@Test
	public void fileTests() throws IOException {
		int amountOfNormalTests = 16;

		for (int i = 1; i <= amountOfNormalTests; i++) {
			String name = "Test" + (i < 10 ? "0" : "") + i + ".sqf";

			doTest(name);
		}
	}

	@Test
	public void errorFileTests() throws IOException {
		int amountOfErrorTests = 11;

		for (int i = 1; i <= amountOfErrorTests; i++) {
			String name = "ErrorTest" + (i < 10 ? "0" : "") + i + ".sqf";

			doTest(name);
		}
	}

	/**
	 * Performs the actual testing on the file with the given name. The name is
	 * expected to contain the substring "Test". Furthermore another file is
	 * expected to be present having the same name except that "Test" has been
	 * replaced with "Result". This second file is used to determine the expected to
	 * represent the expected output of the preprocessing of the given file.
	 * 
	 * @param name
	 *            The name of the file to test
	 * @throws IOException
	 */
	protected void doTest(String name) throws IOException {
		TextReader inReader = new TextReader(getResourceStream(name));

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		prep.preprocess(inReader, out);

		inReader.close();

		String expected = convertStreamToString(getResourceStream(name.replace("Test", "Result")));
		String actual = out.toString();

		assertEquals(expected, actual, name + " did not match the given result!");
		System.out.println(name + " passed testing...");
	}

	@Test
	void test() throws IOException {
		preprocess("#define BLA\n#ifdef BLA Hello\n#else Bye\n#endif");
	}

	void preprocess(String input) throws IOException {
		prep.preprocess(new TextReader(getResourceStream("Test01.sqf")), System.out);
	}

	static InputStream getResourceStream(String name) {
		InputStream in = PreprocessorTest.class.getClassLoader().getResourceAsStream(
				PreprocessorTest.class.getPackage().getName().replace(".", "/") + "/resources/preprocessor/" + name);

		if (in == null) {
			throw new IllegalArgumentException("Can't find resource " + name);
		}

		return in;
	}

	static String convertStreamToString(InputStream is) {
		Scanner scanner = new Scanner(is);

		java.util.Scanner s = scanner.useDelimiter("\\A");

		String content = s.hasNext() ? s.next() : "";

		scanner.close();

		return content;
	}

}
