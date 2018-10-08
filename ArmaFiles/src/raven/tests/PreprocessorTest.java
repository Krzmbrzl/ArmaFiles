package raven.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
		int amountOfErrorTests = 11;

		for (int i = 1; i <= amountOfNormalTests + amountOfErrorTests; i++) {
			int counter = i;
			String name;

			if (counter <= amountOfNormalTests) {
				name = "Test" + (counter < 10 ? "0" : "") + counter + ".sqf";
			} else {
				counter -= amountOfNormalTests;
				name = "ErrorTest" + (counter < 10 ? "0" : "") + counter + ".sqf";
			}


			TextReader inReader = new TextReader(getResourceStream(name));

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			prep.preprocess(inReader, out);

			inReader.close();
			
			String expected = convertStreamToString(getResourceStream(name.replace("Test", "Result")));
			String actual = out.toString();

			assertEquals(expected, actual, name + " did not match the given result!");
			System.out.println(name + " passed testing...");
		}
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
