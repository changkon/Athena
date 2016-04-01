package blaze.athena.QuestionGeneration;

import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
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
	private MaxentTagger tagger;
	private static StanfordParserServer server;

	public Tree parse(String doc) {
		//DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(doc));
		//for (List<HasWord> sentence : tokenizer) {
		//	List<TaggedWord> tagged = tagger.tagSentence(sentence);
			
		//	Tree bestParse = lp.apply(tagged);
		//	GrammaticalStructure bestParse = lp.predict(tagged);
			//Tree bestParse = lp.parse(doc);
			Tree bestParse = lp.parse(doc);
			System.err.println(bestParse);
		//	System.err.println(tagged);
			return bestParse;
		//}
		//return null;
	}

	public static StanfordParserServer getInstance() {
		if (server == null) {
			server = new StanfordParserServer();
		}
		return server;
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
			lp = LexicalizedParser.loadModel("config/englishPCFG.ser.gz");
			//String taggerPath = "config/english-left3words-distsim.tagger";
			//tagger = new MaxentTagger(taggerPath);

		} catch (IllegalArgumentException e) {
			System.err.println("Error loading parser, exiting...");
			System.exit(0);
		}

		lp.setOptionFlags("-outputFormat", "oneline", "-maxLength", maxLength + "");
	}
}

