package raven.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raven.misc.ConsoleProblemListener;
import raven.misc.TextReader;
import raven.preprocessor.DefaultPreprocessorPathResolver;
import raven.preprocessor.Preprocessor;
import raven.preprocessor.PreprocessorBugReproduction;
import raven.preprocessor.PreprocessorCommentHandling;
import raven.preprocessor.PreprocessorWhitespaceHandling;

class PreprocessorTest {

	static Preprocessor prep;

	@BeforeAll
	static void setUp() throws Exception {
		prep = new Preprocessor(PreprocessorWhitespaceHandling.TOLERANT, PreprocessorBugReproduction.ARMA,
				PreprocessorCommentHandling.REMOVE, new DefaultPreprocessorPathResolver(Paths.get("/")));
		prep.addProblemListener(new ConsoleProblemListener());
	}

	@Test
	public void fileTests() throws IOException {
		prep.setCommentHandling(PreprocessorCommentHandling.REMOVE);
		
		System.out.println("\n\nTesting valid files...\n");

		int amountOfNormalTests = 21;

		for (int i = 1; i <= amountOfNormalTests; i++) {
			String name = "Test" + (i < 10 ? "0" : "") + i + ".sqf";

			fileTest(name);
		}
	}

	@Test
	public void errorFileTests() throws IOException {
		prep.setCommentHandling(PreprocessorCommentHandling.REMOVE);
		
		System.out.println("\n\nTesting invalid files...\n");
		int amountOfErrorTests = 17;

		for (int i = 1; i <= amountOfErrorTests; i++) {
			String name = "ErrorTest" + (i < 10 ? "0" : "") + i + ".sqf";

			fileTest(name);
		}
	}

	@Test
	public void commentTest() throws IOException {
		String input = "// I am a test\nBLUBB/*\nAnd so\nam I*/BLA";
		TextReader inReader = new TextReader(new ByteArrayInputStream(input.getBytes()));
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();

		prep.setCommentHandling(PreprocessorCommentHandling.KEEP);
		prep.preprocess(inReader, outStream, getRoot());
		assertEquals(input, outStream.toString());
		inReader.close();

		inReader = new TextReader(new ByteArrayInputStream(input.getBytes()));
		outStream.reset();
		prep.setCommentHandling(PreprocessorCommentHandling.KEEP_BLOCK);
		prep.preprocess(inReader, outStream, getRoot());
		assertEquals("BLUBB/*\nAnd so\nam I*/BLA", outStream.toString());
		inReader.close();

		inReader = new TextReader(new ByteArrayInputStream(input.getBytes()));
		outStream.reset();
		prep.setCommentHandling(PreprocessorCommentHandling.KEEP_INLINE);
		prep.preprocess(inReader, outStream, getRoot());
		assertEquals("// I am a test\nBLUBB\n\nBLA", outStream.toString());
		inReader.close();

		inReader = new TextReader(new ByteArrayInputStream(input.getBytes()));
		outStream.reset();
		prep.setCommentHandling(PreprocessorCommentHandling.REMOVE);
		prep.preprocess(inReader, outStream, getRoot());
		assertEquals("BLUBB\n\nBLA", outStream.toString());
		inReader.close();

		outStream.close();
	}

	@Test
	public void includeTest() throws IOException {
		System.out.println("Testing include statements\n");
		int amountOfTests = 3;

		String root = getRoot();
		
		prep.setCommentHandling(PreprocessorCommentHandling.KEEP);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i = 1; i <= amountOfTests; i++) {
			String number = (i < 10 ? "0" : "") + String.valueOf(i);
			String testName = "IncludeTest" + number + ".sqf";
			String resultName = "IncludeResult" + number + ".sqf";

			System.out.print("Testing " + testName);

			prep.preprocess(new TextReader(getResourceStream(testName)), out, root);

			String expected = convertStreamToString(getResourceStream(resultName));
			String actual = out.toString();

			assertEquals(expected, actual);

			out.reset();

			System.out.println(" - passed");
		}

		out.close();
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
	protected void fileTest(String name) throws IOException {
		System.out.print("Testing \"" + name + "\"...");

		TextReader inReader = new TextReader(getResourceStream(name));

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		prep.preprocess(inReader, out, getRoot());

		inReader.close();

		String expected = convertStreamToString(getResourceStream(name.replace("Test", "Result")));
		String actual = out.toString();

		assertEquals(expected, actual, name + " did not match the given result!");
		System.out.println(" - passed");
	}

	static InputStream getResourceStream(String name) {
		InputStream in = PreprocessorTest.class.getClassLoader()
				.getResourceAsStream(PreprocessorTest.class.getPackage().getName().replace(".", File.separator)
						+ File.separator + "resources" + File.separator + "preprocessor" + File.separator + name);

		if (in == null) {
			throw new IllegalArgumentException("Can't find resource " + name);
		}

		return in;
	}

	static String getRoot() {
		return new File("src").getAbsolutePath() + File.separator
				+ PreprocessorTest.class.getPackage().getName().replace(".", File.separator) + File.separator
				+ "resources" + File.separator + "preprocessor" + File.separator;
	}

	static String convertStreamToString(InputStream is) {
		Scanner scanner = new Scanner(is);

		java.util.Scanner s = scanner.useDelimiter("\\A");

		String content = s.hasNext() ? s.next() : "";

		scanner.close();

		return content;
	}

}
