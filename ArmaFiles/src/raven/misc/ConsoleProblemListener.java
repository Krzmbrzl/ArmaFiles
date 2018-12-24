package raven.misc;

public class ConsoleProblemListener implements IProblemListener {

	public ConsoleProblemListener() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void error(String msg, int start, int length) {
		System.out.println("\n\n[ERROR]:\t" + msg + " (start: " + start + " - length: " + length + ")\n");
	}

	@Override
	public void warning(String msg, int start, int length) {
		System.out.println("\n\n[WARNING]:\t" + msg + " (start: " + start + " - length: " + length + ")\n");
	}

}
