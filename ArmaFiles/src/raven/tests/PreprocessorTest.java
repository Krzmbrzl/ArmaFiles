package raven.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import raven.misc.IProblemListener;
import raven.misc.TextReader;
import raven.preprocessor.DefaultPreprocessorPathResolver;
import raven.preprocessor.Preprocessor;
import raven.preprocessor.PreprocessorBugReproduction;
import raven.preprocessor.PreprocessorCommentHandling;
import raven.preprocessor.PreprocessorWhitespaceHandling;

class PreprocessorTest {

	static class Problem {
		public String message;
		public int start;
		public int length;
		public boolean isError;

		public Problem(String message, int start, int length, boolean isError) {
			this.message = message;
			this.start = start;
			this.length = length;
			this.isError = isError;
		}
	}

	/**
	 * A little helper class that makes sure errors and warnings only occur in the
	 * expected test cases and if they occur it will keep track of them
	 * 
	 * @author Raven
	 *
	 */
	static class ProblemGuard implements IProblemListener {

		private boolean errorsAllowed;
		private boolean warningsAllowed;

		public List<Problem> problems;

		public ProblemGuard() {
			problems = new ArrayList<>();
		}

		@Override
		public void error(String msg, int start, int length) {
			assertTrue("Got unexpected error: " + msg, errorsAllowed);
			problems.add(new Problem(msg, start, length, true));
		}

		@Override
		public void warning(String msg, int start, int length) {
			assertTrue("Got unexpected warning: " + msg, warningsAllowed);
			problems.add(new Problem(msg, start, length, false));
		}

		public void allowErrors(boolean allowed) {
			this.errorsAllowed = allowed;
		}

		public void allowWarnings(boolean allowed) {
			this.warningsAllowed = allowed;
		}

		public void allowProblems(boolean allowed) {
			this.errorsAllowed = allowed;
			this.warningsAllowed = allowed;
		}

		public void reset() {
			problems.clear();
		}

	}

	static Preprocessor prep;
	static ProblemGuard guard;

	@BeforeAll
	static void setUp() throws Exception {
		prep = new Preprocessor(PreprocessorWhitespaceHandling.TOLERANT, PreprocessorBugReproduction.ARMA,
				PreprocessorCommentHandling.REMOVE, new DefaultPreprocessorPathResolver(Paths.get("/")));

		guard = new ProblemGuard();
		prep.addProblemListener(guard);
	}

	@Test
	public void fileTests() throws IOException {
		guard.allowProblems(false);
		guard.reset();
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
		guard.allowProblems(true);
		guard.reset();
		prep.setCommentHandling(PreprocessorCommentHandling.REMOVE);

		System.out.println("\n\nTesting invalid files...\n");
		int amountOfErrorTests = 17;

		for (int i = 1; i <= amountOfErrorTests; i++) {
			String name = "ErrorTest" + (i < 10 ? "0" : "") + i + ".sqf";
			String problemMessageFileName = name.replace(".sqf", "_ExpectedProblemMessages.sqf");

			fileTest(name);

			processProblemMessages(name, problemMessageFileName);
		}
	}

	@Test
	public void commentTest() throws IOException {
		guard.allowProblems(false);
		guard.reset();

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
		assertEquals("\nBLUBB/*\nAnd so\nam I*/BLA", outStream.toString());
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
		assertEquals("\nBLUBB\n\nBLA", outStream.toString());
		inReader.close();

		outStream.close();
	}

	@Test
	public void includeTest() throws IOException {
		guard.allowProblems(false);
		guard.reset();
		prep.setCommentHandling(PreprocessorCommentHandling.KEEP);

		System.out.println("Testing include statements\n");
		int amountOfTests = 6;

		String root = getRoot();

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

		// test sub-folders
		String testName = "subfolder" + File.separator + "SubfolderIncludeTest.sqf";
		String resultName = "IncludeResult02.sqf";

		System.out.print("Testing " + testName);

		prep.preprocess(new TextReader(getResourceStream(testName)), out,
				root + File.separator + "subfolder" + File.separator);

		String expected = convertStreamToString(getResourceStream(resultName));
		String actual = out.toString();

		assertEquals(expected, actual);

		out.reset();

		System.out.println(" - passed");
	}
	
	@Test
	public void includeErrorTest() throws IOException {
		guard.allowProblems(true);
		guard.reset();
		prep.setCommentHandling(PreprocessorCommentHandling.KEEP);

		System.out.println("Testing invalid include statements\n");
		int amountOfTests = 6;

		String root = getRoot();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i = 1; i <= amountOfTests; i++) {
			String number = (i < 10 ? "0" : "") + String.valueOf(i);
			String testName = "IncludeErrorTest" + number + ".sqf";
			String resultName = "IncludeErrorResult.sqf";
			String problemMessageFileName = testName.replace(".sqf", "_ExpectedProblemMessages.sqf");

			System.out.print("Testing " + testName);

			prep.preprocess(new TextReader(getResourceStream(testName)), out, root);

			String expected = convertStreamToString(getResourceStream(resultName));
			String actual = out.toString();

			assertEquals(expected, actual);

			out.reset();

			System.out.println(" - passed");
			
			processProblemMessages(testName, problemMessageFileName);
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

	/**
	 * Processes the problem messages that are currently contained inside
	 * {@link #guard} by printing the out in a pretty format alongside with the
	 * offending text area and by comparing the obtained problem messages with the
	 * expected ones.
	 * 
	 * @param fileName
	 *            The name of the file that has been processed. It is used to get
	 *            the original file content via {@link #getResourceStream(String)}
	 * @param problemMessageFileName
	 *            The name of the file that contains the expected error messages. It
	 *            is used with {@link #getResourceStream(String)}
	 */
	protected void processProblemMessages(String fileName, String problemMessageFileName) {
		String fileContent = convertStreamToString(getResourceStream(fileName));

		// check problem-messages
		StringBuilder problemMessages = new StringBuilder();
		StringBuilder detailedProblemMessages = new StringBuilder();
		for (Problem currentProblem : guard.problems) {
			String line = (currentProblem.isError ? "[ERROR]:" : "[WARNING]:") + " \"" + currentProblem.message
					+ "\" | start: " + currentProblem.start + " - length: " + currentProblem.length + "\n";

			problemMessages.append(line);

			// Add offending region to detailed message
			detailedProblemMessages.append(line);
			if(currentProblem.start + currentProblem.length <= fileContent.length()) {
			detailedProblemMessages.append("\t Offending region: -"
					+ fileContent.substring(currentProblem.start, currentProblem.start + currentProblem.length).replace("\n", "\\n")
					+ "-\n");
			} else {
				detailedProblemMessages.append("\t [Index out of Bounds]\n");
				System.out.println(detailedProblemMessages.toString());
				fail("Error index is out of bounds!");
			}
		}

		// print out expected problem messages
		System.out.println("-----------------------------Expected Problem Messages-----------------------------");
		System.out.println(detailedProblemMessages.toString().trim());
		System.out.println(
				"-----------------------------------------------------------------------------------------------\n\n");


		// compare problem messages
		assertEquals(convertStreamToString(getResourceStream(problemMessageFileName)),
				problemMessages.toString().trim(), "Error messages differed");


		// clear error messages
		guard.reset();
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
