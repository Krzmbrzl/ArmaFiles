package raven.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
	void test() throws IOException {
		preprocess("#define BLA\n#ifdef BLA Hello\n#else Bye\n#endif");
	}

	void preprocess(String input) throws IOException {
		prep.preprocess(new TextReader(new ByteArrayInputStream(input.getBytes())), System.out);
	}

}
