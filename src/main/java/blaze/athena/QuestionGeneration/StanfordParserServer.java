package blaze.athena.QuestionGeneration;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.Options;
import edu.stanford.nlp.trees.Tree;

/**
 * Wrapper class to run the Stanford Parser as a socket server so the grammar need not
 * be loaded for every new sentence.
 *
 * @author mheilman@cmu.edu
 *
 */
public class StanfordParserServer {
	private LexicalizedParser lp;

	public Tree parse(String doc) {
		Tree bestParse = lp.parse(doc);
		System.err.println(bestParse);
		return bestParse;
	}

	public StanfordParserServer() {
		String[] args = new String[]{};
		String serializedInputFileOrUrl = null;
		int maxLength = 40;

		// variables needed to process the files to be parse
		String sentenceDelimiter = null;
		int argIndex = 0;
		if (args.length < 1) {
			args = new String[]{"config/englishFactored.ser.gz", "-port", "5556"};
		}

		Options op = new Options();
		// while loop through option arguments
		while (argIndex < args.length && args[argIndex].charAt(0) == '-') {
			if (args[argIndex].equalsIgnoreCase("-sentences")) {
				sentenceDelimiter = args[argIndex + 1];
				if (sentenceDelimiter.equalsIgnoreCase("newline")) {
					sentenceDelimiter = "\n";
				}
				argIndex += 2;
			} else if (args[argIndex].equalsIgnoreCase("-loadFromSerializedFile")) {
				// load the parser from a binary serialized file
				// the next argument must be the path to the parser file
				serializedInputFileOrUrl = args[argIndex + 1];
				argIndex += 2;
			} else if (args[argIndex].equalsIgnoreCase("-maxLength")) {
				maxLength = new Integer(args[argIndex + 1]);
				argIndex += 2;
			} else if (args[argIndex].equalsIgnoreCase("-port")) {
	//			port = new Integer(args[argIndex + 1]);
				argIndex += 2;
			} else {
				argIndex = op.setOptionOrWarn(args, argIndex);
			}
		} // end while loop through arguments

		// so we load a serialized parser
		if (serializedInputFileOrUrl == null && argIndex < args.length) {
			// the next argument must be the path to the serialized parser
			serializedInputFileOrUrl = args[argIndex];
//			argIndex++;
		}
		if (serializedInputFileOrUrl == null) {
			System.err.println("No grammar specified, exiting...");
			System.exit(0);
		}
		try {
//			lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishFactored.ser.gz");
			lp = LexicalizedParser.loadModel("config/englishFactored.ser.gz");
		} catch (IllegalArgumentException e) {
			System.err.println("Error loading parser, exiting...");
			System.exit(0);
		}

		lp.setOptionFlags("-outputFormat", "oneline", "-maxLength", maxLength + "");
	}

}

