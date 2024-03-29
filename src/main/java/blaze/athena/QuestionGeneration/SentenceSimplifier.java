// Question Generation via Overgenerating Transformations and Ranking
// Copyright (c) 2010 Carnegie Mellon University.  All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Michael Heilman
//	  Carnegie Mellon University
//	  mheilman@cmu.edu
//	  http://www.cs.cmu.edu/~mheilman

package blaze.athena.QuestionGeneration;

import blaze.athena.dto.QuestionDTO;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.ling.Sentence;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Class for extracting simplified factual statements from complex sentences.
 *
 * This class constitutes "stage 1" in the framework described in the following paper
 * (but an earlier prototype was used for stage 1 there):
 * M.  Heilman and N. A. Smith. 2010. Good Question! Statistical Ranking for Question Generation. In Proc. of NAACL/HLT.
 *
 * The following paper discusses the methods implemented here, and provides some experimental evaluation results:
 * M. Heilman and N. A. Smith. 2010. Extracting Simplied Statements for Factual Question Generation. In Proc. of the 3rd Workshop on Question Generation.
 *
 * When creating the output, the system makes various calls to Question.setFeatureValue().
 * These calls track what operations were performed in creating the sentence,
 * and could be used in the later stages of an NLP/NLG system.
 *
 *
 * @author Michael Heilman, Language Technologies Institute, Carnegie Mellon University (mheilman@cmu.edu)
 *
 */
public class SentenceSimplifier {
	public SentenceSimplifier(){
		factory = new LabeledScoredTreeFactory();
	}


	public List<blaze.athena.QuestionGeneration.Question> simplifyHelper(blaze.athena.QuestionGeneration.Question input){
		if(GlobalProperties.getDebug()) System.err.println("simplifyHelper: "+input.getIntermediateTree());
		numSimplifyHelperCalls++;
		List<blaze.athena.QuestionGeneration.Question> treeCollection = new ArrayList<blaze.athena.QuestionGeneration.Question>();


		//move fronted PPs to the end of the sentence
		//if modified, add new tree to results and make a new copy
		moveLeadingPPsAndQuotes(input);

		//remove connecting words, subordinating conjunctions, and adverbs. e.g., "but", "however," "while," etc.
		//also remove all nested elements (e.g., appositives, participials, relative clauses (?), etc.)
		removeNestedElements(input);

		//add the original input as the canonical sentence if none of the above transformations
		//produced a simpler form
		if(!hasBreakableConjunction(input) && hasSubjectAndFiniteMainVerb(input)
				&& mainVerbOK(input))
		{
			addIfNovel(treeCollection, input);
		}else{

			List<blaze.athena.QuestionGeneration.Question> extracted = new ArrayList<blaze.athena.QuestionGeneration.Question>();
			//if there is a conjunction of NPs within this small chunk, also extract separate sentences using each conjunct NP.
			if(breakNPs) extractConjoinedNPs(extracted, input);
			extractConjoinedPhrases(extracted, input);

			for(blaze.athena.QuestionGeneration.Question e: extracted){
				//recur
				addAllIfNovel(treeCollection, simplifyHelper(e));
			}

		}

		return treeCollection;
	}




	/**
	 * This is a simple hack to avoid bad output for a few special cases.
	 * Specifically, we want to avoid extracting
	 * from phrases with "according" and "including",
	 * which syntactically look like participial phrases.
	 *
	 */
	private boolean mainVerbOK(blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		//avoid extracting sentences from "...according to X..."
		tregexOpStr = "ROOT <+(VP|S) (/VB.*/ < /(accord.*|includ.*)/)";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		boolean res = !matcher.find();


		return res;
	}



	private boolean hasBreakableConjunction(blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		//conjoined VPs, clauses, etc.
		tregexOpStr = "CONJP|CC !< either|or|neither|nor > S|SBAR|VP"
			+ " [ $ SBAR|S | !>> SBAR ] "; //we can break conjoined SBARs, but not anything else under an SBAR node
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		if(matcher.find()){
			return true;
		}

		//clauses conjoined by semi-colons
		tregexOpStr = " S < (S=child $ (/:/ < /;/) !$++ (/:/ < /;/) ) ";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		if(matcher.find()){
			return true;
		}

		if(breakNPs){
			tregexOpStr = "CONJP|CC !< either|or|neither|nor > NP !>> SBAR "
				+ " !> (NP < (/^(N.*|SBAR|PRP)$/ !$ /^(N.*|SBAR|PRP)$/))";
			//the latter part is to address special cases of flat NPs in treebank:
			//we allow NPs like "(NP (JJ eastern) (CC and) (JJ western) (NNS coasts))"
			//because we can't easily split them

			matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
			matcher = matchPattern.matcher(input.getIntermediateTree());
			if(matcher.find()){
				return true;
			}
		}

		return false;
	}


	/**
	 * Returns whether the input sentence has a subject and a finite main verb.
	 * If it does not, then we do not want to add it to the output.
	 *
	 * @param input
	 * @return
	 */
	private boolean hasSubjectAndFiniteMainVerb(blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;
		tregexOpStr = "ROOT " + //main clause dominates...
				" <+(S) NP|SBAR  <+(VP|S) VB|VBD|VBP|VBZ  !<+(VP) TO"; //AND also dominates a finite, non-participle verb
				//allowing VBN would allow participial phrases like "founded by Bill Gates"

		//" [ < /^(PRP|N.*|SBAR|PP)$/ " + //either PRP for pronoun, N for NP|NN|NNS...
		//" | < (S < (VP < TO|VBG)) ] " + // or a non-finite verb phrase (e.g., "walking")


		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		boolean res = matcher.find();
		return res;
	}



	private void removeNestedElements(blaze.athena.QuestionGeneration.Question input) {
		removeAppositives(input);
		removeVerbalModifiersAfterCommas(input);
		removeClauseLevelModifiers(input);
		removeNonRestrRelClausesAndParticipials(input);
		removeParentheticals(input);

		if(GlobalProperties.getComputeFeatures()) input.setFeatureValue("removedNestedElements", 1.0); //old feature name
	}




	public List<blaze.athena.QuestionGeneration.Question> simplify(Tree sentence){
		return simplify(sentence, true);
	}


	/**
	 * Primary method for simplifying sentences.
	 * Takes an input sentence in the form of a tree
	 * and returns a list of Question objects, which help
	 * to track what operations were performed.
	 *
	 * @param sentence
	 * @param fixCapitalization
	 * @return
	 */
	public List<blaze.athena.QuestionGeneration.Question> simplify(Tree sentence, boolean fixCapitalization){
		List<blaze.athena.QuestionGeneration.Question> treeList = new ArrayList<blaze.athena.QuestionGeneration.Question>();
		numSimplifyHelperCalls = 0;
		if(GlobalProperties.getDebug()) System.err.println("simplify input:"+ sentence);
		//add original tree
		blaze.athena.QuestionGeneration.Question orig = new blaze.athena.QuestionGeneration.Question();
		orig.setSourceTree(sentence);
		orig.setIntermediateTree(sentence.deepCopy());

		//if the input contains any UCP or other odd nodes, then just return the original sentence
		//such nodes indicate that the parse failed, or at least that our system will likely produce bad output
//		if(uglyParse(sentence)){
//			treeList.add(orig);
//			return treeList;
//		}

		AnalysisUtilities.downcaseFirstToken(orig.getIntermediateTree());
		//treeSet.add(originalWithFeatures);
		blaze.athena.QuestionGeneration.Question current = orig.deeperCopy();

		List<blaze.athena.QuestionGeneration.Question> extracted = new ArrayList<blaze.athena.QuestionGeneration.Question>();

		//for each nested element in the INPUT... (nested elements include finite verbs, non-restrictive relative clauses, appositives, conjunction of VPs, conjunction of clauses, participial phrases)
		//transform the nested element into a declarative sentence (preserving tense), removing conjunctions, etc.
		extracted.add(current);
		extractSubordinateClauses(extracted, orig);
		extractNounParticipialModifiers(extracted, orig);
		extractNonRestrictiveRelativeClauses(extracted, orig);
		extractAppositives(extracted, orig);
		extractVerbParticipialModifiers(extracted, orig);
		//extractWITHPartcipialPhrases(extracted, orig); //too rare to worry about
		if(extractFromVerbComplements) extractComplementClauses(extracted, orig);

		for(blaze.athena.QuestionGeneration.Question q: extracted){
			addAllIfNovel(treeList, simplifyHelper(q));
		}

		//make sure there is at least one output
		if(treeList.size()==0){
			addIfNovel(treeList,current);
		}

		if(fixCapitalization){
			//upcase the first tokens of all output trees.
			for(blaze.athena.QuestionGeneration.Question q: treeList){
				AnalysisUtilities.upcaseFirstToken(q.getIntermediateTree());
			}
		}

		//clean up the output
		for(blaze.athena.QuestionGeneration.Question q: treeList){
			AnalysisUtilities.removeExtraQuotes(q.getIntermediateTree());
		}

		if(GlobalProperties.getComputeFeatures()) treeList.get(0).setFeatureValue("extractedFromLeftMostMainClause", 1.0);
		if(GlobalProperties.getDebug()) System.err.println("simplifyHelperCalls:\t"+numSimplifyHelperCalls);
		return treeList;
	}


	/**
	 * A simple hack to avoid bad output due to syntactic parsing errors.
	 * We check for various rare nonterminal labels
	 * to skip over parses that are likely to be bad.
	 *
	 */
	private boolean uglyParse(Tree t) {
//		if(TregexPatternFactory.getPattern("UCP|FRAG|X|NAC").matcher(t).find()){
//			if(GlobalProperties.getDebug()) System.err.println("Ugly parse");
//		//	return true;
//			//TODO use this or not?
//		}
//		return false;
		System.out.println(t.score());
		return t.score() < -150;
	}



	/**
	 * Method to add a new Question object q to a given set
	 * if the collection treeSet does not already include q.
	 *
	 * @param treeSet
	 * @param q
	 */
	private void addIfNovel(Collection<blaze.athena.QuestionGeneration.Question> treeSet, blaze.athena.QuestionGeneration.Question q) {
		boolean exists = false;
		for(blaze.athena.QuestionGeneration.Question old: treeSet){
			if(old.getIntermediateTree().equals(q.getIntermediateTree())){
				exists = true;
				break;
			}
		}
		if(!exists){
			treeSet.add(q);
		}
	}


	/**
	 * Adds new trees that do not already exist in the treeSet.
	 * We don't use addAll because there may be multiple TreeWithFeatures objects
	 * with the same tree but different features.
	 *
	 * @param treeSet
	 * @param extracted
	 */
	private void addAllIfNovel(Collection<blaze.athena.QuestionGeneration.Question> treeSet, Collection<blaze.athena.QuestionGeneration.Question> extracted) {
		for(blaze.athena.QuestionGeneration.Question q: extracted){
			this.addIfNovel(treeSet, q);
		}
	}


	/**
	 * e.g., John and James like Susan.  ->  John likes Susan.
	 *
	 */
	private void extractConjoinedNPs(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		Tree conjoinedNode;
		Tree parent;

		TregexMatcher matcher;
		blaze.athena.QuestionGeneration.Question newQuestion;

		//only extract conjoined NPs that are arguments or adjuncts of the main verb
		// in the tree, this means the closest S will be the one under the root
		tregexOpStr = "NP=parent < (CONJP|CC !< or|nor [ "
			+ " $+ /^(N.*|PRP|SBAR)$/=child $-- /^(N.*|PRP|SBAR)$/ | " //there must be a noun on each side of the conjunction
			+ " $-- /^(N.*|PRP|SBAR)$/=child $+ /^(N.*|PRP|SBAR)$/ ] ) " //this avoids extracting from flat NPs such as "the smaller and darker form"
			+ " !>> (/.*/ $ (CC|CONJP !< or|nor)) "  //this cannot be nested within a larger conjunction or followed by a conjunction (we recur later to catch this)
			+ " !$ (CC|CONJP !< or|nor)"
			+ " !.. (CC|CONJP !< or|nor > NP|PP|S|SBAR|VP) !>> SBAR ";
			//+ " >> (ROOT !< (S <+(VP) (/^VB.*$/ < are|were|be|seem|appear))) " ; //don't break plural predicate nominatives (e.g., "John and Mary are two of my best friends.")
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		List<Integer> nodeIndexes = new ArrayList<Integer>();
		List<Integer> parentIDs = new ArrayList<Integer>();

		while(matcher.find()){
			//store the parents' IDs (in the tree)
			parent = matcher.getNode("parent");
			parentIDs.add(parent.nodeNumber(input.getIntermediateTree()));

			conjoinedNode = matcher.getNode("child");
			//store the conjoined nodes' index into their parent's list of children
			int idx = parent.objectIndexOf(conjoinedNode);
			if(!nodeIndexes.contains(idx)) nodeIndexes.add(idx);
		}

		//for each of the conjoined children,
		//create a new tree by removing all the nodes they are conjoined with
		Collections.sort(nodeIndexes);//sort, just to keep them in the original order
		for(int i=0; i<nodeIndexes.size(); i++){
			newQuestion = input.deeperCopy();

			Tree t = newQuestion.getIntermediateTree();
			parent = t.getNodeNumber(parentIDs.get(i));
			Tree gparent = parent.parent(t);
			conjoinedNode = parent.getChild(nodeIndexes.get(i));
			String siblingLabel;

			//Remove all the nodes that are conjoined
			//with the selected noun (or are conjunctions, commas).
			//These can have labels NP, NN, ..., PRP for pronouns, CC, "," for commas, ":" for semi-colons
			for(int j=0;j<parent.numChildren(); j++){
				if(parent.getChild(j) == conjoinedNode) continue;
				siblingLabel = parent.getChild(j).label().toString();
				if(siblingLabel.matches("^[NCP,:S].*")){
					parent.removeChild(j);
					j--;
				}
			}

			//if there is an trivial unary "NP -> NP",
			//remove the parent and put the child in its place
			if(parent.numChildren() == 1 && parent.getChild(0).label().equals("NP")){
				int tmpIndex = gparent.objectIndexOf(parent);
				gparent.removeChild(tmpIndex);
				gparent.addChild(tmpIndex, parent.getChild(0));
			}

			correctTense(conjoinedNode, gparent);
			addQuotationMarksIfNeeded(newQuestion.getIntermediateTree());

			if(GlobalProperties.getDebug()) System.err.println("extractConjoinedNPs: "+newQuestion.getIntermediateTree().toString());
			if(GlobalProperties.getComputeFeatures()) newQuestion.setFeatureValue("extractedFromConjoinedPhrases", 1.0); //old feature name
			if(GlobalProperties.getComputeFeatures()) newQuestion.setFeatureValue("extractedFromConjoinedNPs", 1.0);
			extracted.add(newQuestion);
		}
	}



	/**
	 * e.g., John and Mary like Bill.  -> John LIKES Bill.  Mary LIKES Bill.
	 * John and I like Bill -> John LIKES Bill.  I LIKE Bill.
	 * John and I are old. -> I IS old. John IS old.
	 */
	private void correctTense(Tree subject, Tree clause) {
		int tmpIndex;
		//correct verb tense when modifying subjects
		for(Tree uncle: clause.getChildrenAsList()){
			String newVerbPOS = null;
			Tree verbPreterminal = null;
			boolean needToModifyVerb = false;
			//if the node is a subject (i.e., its uncle is a VP), then check
			//to see if its tense needs to be changed
			String headPOS = subject.headPreTerminal(AnalysisUtilities.getInstance().getHeadFinder()).label().toString();
			if(uncle.label().toString().equals("VP") && !headPOS.endsWith("S")){
				verbPreterminal = uncle.headPreTerminal(AnalysisUtilities.getInstance().getHeadFinder());
				//original main verb was plural but the conjoined subject word is singular
				//e.g., John (and Mary) like Bill.  -> John like Bill.
				if((verbPreterminal.label().toString().equals("VB") || verbPreterminal.label().toString().equals("VBP"))){ //the parser confuses VBP with VB
					if(getStringFromTree(subject).equals("I") || getStringFromTree(subject).equals("you")){
						newVerbPOS = "VBP";
					}else{
						newVerbPOS = "VBZ";
					}
					needToModifyVerb = true;
				}else if(verbPreterminal.label().toString().equals("VBD")){
					newVerbPOS = "VBD";
					needToModifyVerb = true;
				}
			}
			//if needed, change the tense of the verb
			if(needToModifyVerb){
				String verbLemma = AnalysisUtilities.getInstance().getLemma(verbPreterminal.getChild(0).label().toString(), verbPreterminal.label().toString());
				String newVerb;
				//special cases
				if(verbLemma.equals("be") && newVerbPOS.equals("VBD")){
					if(subject.label().toString().endsWith("S")) newVerb = "were";
					else  newVerb = "was";
				}else if(verbLemma.equals("be") && getStringFromTree(subject).equals("I") && newVerbPOS.equals("VBP")){
					newVerb = "am";
				}else{ //default
					newVerb = AnalysisUtilities.getInstance().getSurfaceForm(verbLemma, newVerbPOS);
				}
				tmpIndex = verbPreterminal.parent(uncle).objectIndexOf(verbPreterminal);
				Tree verbParent = verbPreterminal.parent(uncle);
				verbParent.removeChild(tmpIndex);
				verbParent.addChild(tmpIndex, AnalysisUtilities.getInstance().readTreeFromString("("+newVerbPOS+" "+newVerb+")"));
			}
		}
	}

	/**
	 * e.g., John ran and Bill walked.  -> John ran. Bill walked.
	 *
	 */
	private void extractConjoinedPhrases(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		Tree conjoinedNode;

		TregexMatcher matcher;
		//Tree newTree = copy.getIntermediateTree();
		Tree newTree;
		int nodeindex;

		tregexOpStr = "__ " +
		" [ < (VP < (/VB.*/=child $ (CC|CONJP !< or|nor !$++ CC|CONJP))) "+ //get the first conjunction, to avoid spurious duplicate matches
		" | < (VP < (VP=child $ (CC|CONJP !< or|nor !$++ CC|CONJP))) "+ // verb phrases may be conjoined by commas and adverbs (e.g., "John ran, then walked.")
		" | < (S|SINV < (S|SINV=child $ (CC|CONJP !< or|nor !$++ CC|CONJP))) "+
		" | < (S|SINV < (S|SINV=child $ (/:/ < /;/ !$++ /:/))) "+
		//" | < (ADJP < (JJ|JJR|ADJP=child $ (CC|CONJP !< or|nor !$++ CC|CONJP))) " +
		//" | < (ADVP < (RB|RBR|ADVP=child $ RB|RBR|ADVP=child $ (CC|CONJP !< or|nor !$++ CC|CONJP)))  "+
		//" | < (PP < (PP=child $ (CC|CONJP !< or|nor !$++ CC|CONJP))) " +
		" | < (SBAR < (SBAR=child $ (CC|CONJP !< or|nor !$++ CC|CONJP))) ] "+
		" !$ (CC|CONJP !< or|nor)" + //this cannot be nested within a larger conjunction or followed by a conjunction (we recur later to catch this)
		" !< (CC|CONJP !< or|nor) " +
		" !.. (CC|CONJP !< or|nor > NP|PP|S|SBAR|VP) !>> SBAR";


		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());

		while(matcher.find()){
			conjoinedNode = matcher.getNode("child");
			nodeindex = conjoinedNode.nodeNumber(input.getIntermediateTree());

			//make a copy of the input for this iteration
			newTree = input.getIntermediateTree().deepCopy();
			removeConjoinedSiblingsHelper(newTree, nodeindex);

			//for conjoined main clauses, add punctuation if necessary
			AnalysisUtilities.addPeriodIfNeeded(newTree);

			//make a new Question object and add it
			addQuotationMarksIfNeeded(newTree);

			blaze.athena.QuestionGeneration.Question newTreeWithFeatures = input.deeperCopy();
			newTreeWithFeatures.setIntermediateTree(newTree);
			if(GlobalProperties.getDebug()) System.err.println("extractConjoinedPhrases: "+newTree.toString());
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromConjoinedPhrases", 1.0); //old feature name
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromConjoined", 1.0);
			addIfNovel(extracted, newTreeWithFeatures);
		}
	}



	private void removeConjoinedSiblingsHelper(Tree copy, int childindex) {
		if(GlobalProperties.getDebug()) System.err.println("removeConjoinedSiblingsHelper: "+copy.toString());
		Tree child = copy.getNodeNumber(childindex);
		Tree parent = child.parent(copy);
		Tree gparent = parent.parent(copy);

		int parentIdx = gparent.objectIndexOf(parent);

		//By an annoying PTB convention, some verb phrase conjunctions
		//can conjoin two verb preterminals under a VP,
		//rather than only allowing VP nodes to be conjoined.
		//e.g., John walked and played.
		//So, we add an extra VP node in between if necessary
		if(child.label().toString().startsWith("VB")){
			gparent.removeChild(parentIdx);
			Tree newTree = factory.newTreeNode("VP", new ArrayList<Tree>());
			newTree.addChild(child);
			gparent.addChild(parentIdx, newTree);
		}else{
			gparent.setChild(parentIdx, child);
		}
	}



	/**
	 *
	 * e.g., However, John did not study. -> John did not study.
	 *
	 * @param q
	 * @return
	 */
	private boolean removeClauseLevelModifiers(blaze.athena.QuestionGeneration.Question q) {
		List<Pair<TregexPattern, TsurgeonPattern>> ops;
		String tregexOpStr;
		TregexPattern matchPattern;
		TsurgeonPattern p;
		List<TsurgeonPattern> ps;

		boolean modified = false;

		//remove subordinate clauses and various phrases
		//leave conditional antecedents (i.e., with "if" or "unless" as complementizers.  punt on "even if")
		tregexOpStr = "ROOT=root < (S=mainclause < (/SBAR|ADVP|ADJP|CC|PP|S|NP/=fronted !< (IN < if|unless) !$ ``  $++ NP=subject))";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		TregexMatcher matcher = matchPattern.matcher(q.getIntermediateTree());
		if(matcher.find()){
			ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
			ps = new ArrayList<TsurgeonPattern>();
			tregexOpStr = "ROOT=root < (S=mainclause < (/[,:]/=comma $ (/SBAR|ADVP|ADJP|CC|PP|S|NP/=fronted !< (IN < if|unless) $++ NP=subject)))";
			matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
			ps.add(Tsurgeon.parseOperation("prune comma"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());

			ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
			ps = new ArrayList<TsurgeonPattern>();
			tregexOpStr = "ROOT=root < (S=mainclause < (/SBAR|ADVP|ADJP|CC|PP|S|NP/=fronted !< (IN < if|unless) $++ NP=subject))";
			matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
			ps.add(Tsurgeon.parseOperation("prune fronted"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());

			addQuotationMarksIfNeeded(q.getIntermediateTree());
			if(GlobalProperties.getComputeFeatures()) q.setFeatureValue("removedClauseLevelModifiers", 1.0);
			modified = true;
		}

		return modified;
	}


	/**
	 *
	 * e.g., John studied, hoping to get a good grade. -> John studied.
	 *
	 * @return whether or not a change was made
	 */
	private boolean removeVerbalModifiersAfterCommas(blaze.athena.QuestionGeneration.Question q) {
		List<Pair<TregexPattern, TsurgeonPattern>> ops;
		String tregexOpStr;
		TregexPattern matchPattern;
		TsurgeonPattern p;
		List<TsurgeonPattern> ps;

		ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();

		tregexOpStr = "ROOT=root << (VP !< VP < (/,/=comma $+ /[^`].*/=modifier))";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);

		//remove modifiers
		ps = new ArrayList<TsurgeonPattern>();
		if(matchPattern.matcher(q.getIntermediateTree()).find()){
			ps.add(Tsurgeon.parseOperation("prune modifier"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());

			//now remove the comma
			ops.clear();
			ps.clear();
			tregexOpStr = "ROOT=root << (VP !< VP < /,/=comma)";
			matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
			ps.add(Tsurgeon.parseOperation("prune comma"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());
			addQuotationMarksIfNeeded(q.getIntermediateTree());

			if(GlobalProperties.getComputeFeatures()) q.setFeatureValue("removedVerbalModifiersAfterCommas", 1.0);
			return true;
		}else{
			return false;
		}
	}


	/**
	 *
	 * John studied, hoping to get a good grade. -> John hoped to get a good grade.
	 *
	 * @param extracted
	 * @param input
	 */
	private void extractVerbParticipialModifiers(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		tregexOpStr =
			"S=sub $- /,/ !< NP < (VP=participial < VBG=verb) " +
			" >+(VP) (S|SINV < NP=subj) " +
			" >> (ROOT <<# /VB.*/=tense) "; //tense determined by top-most verb

		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		while(matcher.find()){
			String verbPOS = findTense(matcher.getNode("tense"));
			Tree p = matcher.getNode("participial").deepCopy();
			Tree verb = matcher.getNode("verb");
//			String verbLemma =  AnalysisUtilities.getInstance().getLemma(verb.getChild(0).label().toString(), verb.label().toString());
			String verbLemma =  AnalysisUtilities.getInstance().getLemma(verb.yieldWords().get(0).toString(), verb.label().toString());
			String newVerb = AnalysisUtilities.getInstance().getSurfaceForm(verbLemma, verbPOS);
			int verbIndex = p.objectIndexOf(verb);
			if (verbIndex < 0) { continue; }
			p.removeChild(verbIndex);
			p.addChild(verbIndex, AnalysisUtilities.getInstance().readTreeFromString("("+verbPOS+" "+newVerb+")"));
			String treeStr = "(ROOT (S "+matcher.getNode("subj").toString()+" "+p.toString()+" (. .)))";
			Tree newTree = AnalysisUtilities.getInstance().readTreeFromString(treeStr);
			correctTense(newTree.getChild(0).getChild(0), newTree.getChild(0));

			addQuotationMarksIfNeeded(newTree);
			blaze.athena.QuestionGeneration.Question newTreeWithFeatures = input.deeperCopy();
			newTreeWithFeatures.setIntermediateTree(newTree);
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromParticipial", 1.0);
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromVerbParticipial", 1.0);
			if(GlobalProperties.getDebug()) System.err.println("extractVerbParticipialModifiers: "+newTree.toString());
			addIfNovel(extracted, newTreeWithFeatures);
		}
	}



	private String findTense(Tree node) {
		if(node.label().equals("MD")){
			if(getStringFromTree(node).matches("^(would|could)$")){
				return "VBD";
			}
		}
		return node.label().toString();
	}





	/**
	 * e.g., As John slept, I studied. ->  John slept.
	 *
	 */
	private void extractSubordinateClauses(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		Tree subord;
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		tregexOpStr = " SBAR [ > VP < IN | > S|SINV ]  " + //not a complement
			" !< (IN < if|unless|that)" + //not a conditional antecedent
			" < (S=sub !< (VP < VBG)) "+//+ //not a participial phrase
			" >S|SINV|VP "; //not part of a noun phrase or PP (other methods for those)

		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		while(matcher.find()){
			Tree newTree = factory.newTreeNode("ROOT", new ArrayList<Tree>());
			subord = matcher.getNode("sub");
			newTree.addChild(subord.deepCopy());

			AnalysisUtilities.addPeriodIfNeeded(newTree);
			addQuotationMarksIfNeeded(newTree);
			blaze.athena.QuestionGeneration.Question newTreeWithFeatures = input.deeperCopy();
			newTreeWithFeatures.setIntermediateTree(newTree);
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromFiniteClause", 1.0); //old feature name
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromSubordinateClause", 1.0);
			if(GlobalProperties.getDebug()) System.err.println("extractSubordinateClauses: "+newTree.toString());
			addIfNovel(extracted, newTreeWithFeatures);
		}
	}


	private void extractComplementClauses(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		Tree subord;
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		//TODO should also address infinitive complements
		tregexOpStr = "SBAR "+
			" < (S=sub !< (VP < VBG)) "+//+ //not a participial phrase
			" !> NP|PP "+ //not part of a noun phrase or PP (other methods for those)
			" [ $- /^VB.*/=verb | >+(SBAR) (SBAR $- /^VB.*/=verb) ] "; //complement of a VP (follows the verb)

		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		while(matcher.find()){
			Tree newTree = factory.newTreeNode("ROOT", new ArrayList<Tree>());
			subord = matcher.getNode("sub");
			Tree verb = matcher.getNode("verb");
			String verbLemma = AnalysisUtilities.getInstance().getLemma(getStringFromTree(verb), verb.label().toString());

			if(!verbImpliesComplement(verbLemma)){
				continue;
			}
			newTree.addChild(subord.deepCopy());

			AnalysisUtilities.addPeriodIfNeeded(newTree);
			addQuotationMarksIfNeeded(newTree);
			blaze.athena.QuestionGeneration.Question newTreeWithFeatures = input.deeperCopy();
			newTreeWithFeatures.setIntermediateTree(newTree);
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromFiniteClause", 1.0); //old feature name
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromComplementClause", 1.0);
			if(GlobalProperties.getDebug()) System.err.println("extractComplementClauses: "+newTree.toString());
			addIfNovel(extracted, newTreeWithFeatures);
		}
	}


	/**
	 * Identifies whether the given verb implies its complement
	 * e.g., true for "forget", false for "believe"
	 *
	 * @param verbLemma
	 * @return
	 */
	private boolean verbImpliesComplement(String verbLemma) {
		if(verbsThatImplyComplements == null){
			verbsThatImplyComplements = new HashSet<String>();
			verbsThatImplyComplements.add("know");
			verbsThatImplyComplements.add("forget");
			verbsThatImplyComplements.add("discover");
			//TODO
		}

		return verbsThatImplyComplements.contains(verbLemma);

	}


	/**
	 * e.g., Lincoln, the 16th president, was tall. -> Lincoln was the 16th president.
	 * The meeting, in 1984, was important. -> The meeting was in 1984.
	 */
	private void extractAppositives(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		tregexOpStr = "NP < (NP=noun !$-- NP $+ (/,/ $++ NP|PP=appositive !$ CC|CONJP)) " +
				" >> (ROOT <<# /^VB.*/=mainverb) "; //extract the main verb to capture the verb tense
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		while(matcher.find()){
			Tree verbtree = matcher.getNode("mainverb");
			Tree nountree = matcher.getNode("noun").deepCopy();
			Tree appositivetree = matcher.getNode("appositive");

			makeDeterminerDefinite(nountree);

			//if both are proper nouns, do not extract because this is not an appositive(e.g., "Pittsburgh, PA")
			/*if(nountree.headPreTerminal(AnalysisUtilities.getInstance().getHeadFinder()).label().toString().equals("NNP")
					&& appositivetree.headPreTerminal(AnalysisUtilities.getInstance().getHeadFinder()).label().toString().equals("NNP"))
			{
				continue;
			}*/

			//make a new tree for a copula sentence with the noun and appositive
			String pos = verbtree.label().toString();
			String copula;
			if(pos.equals("VBD")){
				if(isPlural(nountree)){
					copula = "(VBD were)";
				}else{
					copula = "(VBD was)";
				}
			}else{
				if(isPlural(nountree)){
					copula = "(VBD are)";
				}else{
					copula = "(VBD is)";
				}
			}
			Tree newTree = AnalysisUtilities.getInstance().readTreeFromString("(ROOT (S "+nountree+" (VP "+copula+" "+appositivetree+") (. .)))");

			addQuotationMarksIfNeeded(newTree);
			if(GlobalProperties.getDebug()) System.err.println("extractAppositives: "+ newTree.toString());
			blaze.athena.QuestionGeneration.Question newTreeWithFeatures = input.deeperCopy();
			newTreeWithFeatures.setIntermediateTree(newTree);
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromAppositive", 1.0);

			addIfNovel(extracted, newTreeWithFeatures);

		}
	}


	private void addQuotationMarksIfNeeded(Tree input){
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		tregexOpStr = "__=parent < (/`/ !.. /'/)";

		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input);

		if(matcher.find()){
			TsurgeonPattern p;
			List<TsurgeonPattern> ps = new ArrayList<TsurgeonPattern>();
			List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();

			ps.add(Tsurgeon.parseOperation("insert ('' '') >-1 parent"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, input);
		}
	}





	/**
	 * e.g., John, who is a friend of mine, likes Susan. -> John is a friend of mine.
	 *
	 */
	private void extractNonRestrictiveRelativeClauses(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;
		TregexMatcher matcherclause;

		tregexOpStr = "NP=np < (SBAR=sbar [ < (WHADVP=wherecomp < (WRB < where)) "
					+ " | < (WHNP !< /WP\\$/) "
					+ " | < (WHNP=possessive < /WP\\$/)"  //John, whose car was
					+ " | < (WHPP < IN|TO=preposition) ] $-- NP $- /,/ "
					+ " < S=relclause  !< WHADJP)";

		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());

		//iterate over all the relative clauses in the input
		//and create an output sentence for each one.
		while(matcher.find()){
			Tree missingArgumentTree = matcher.getNode("np");
			Tree relclause = matcher.getNode("relclause");
			if(missingArgumentTree == null || relclause == null) continue;
			missingArgumentTree = missingArgumentTree.deepCopy();
			relclause = relclause.deepCopy();
			Tree possessive = matcher.getNode("possessive");
			Tree sbar = matcher.getNode("sbar").deepCopy();

			makeDeterminerDefinite(missingArgumentTree);

			if(possessive != null){
				possessive = possessive.deepCopy();
				possessive.removeChild(0);
				String newTree = "(NP (NP "+missingArgumentTree.toString()+ " (POS 's))";
				for(int i=0; i<possessive.numChildren(); i++) newTree += possessive.getChild(i).toString() + " ";
				newTree += ")";
				missingArgumentTree = AnalysisUtilities.getInstance().readTreeFromString(newTree);
			}

			//remove the relative clause and the commas surrounding it from the missing argument tree
			for(int i=0; i<missingArgumentTree.numChildren(); i++){
				if(missingArgumentTree.getChild(i).equals(sbar)){
					//remove the relative clause
					missingArgumentTree.removeChild(i);
					//remove the comma after the relative clause
					if(i<missingArgumentTree.numChildren() && missingArgumentTree.getChild(i).label().toString().equals(",")){
						missingArgumentTree.removeChild(i);
					}
					//remove the comma before the relative clause
					if(i>0 && missingArgumentTree.getChild(i-1).label().toString().equals(",")){
						missingArgumentTree.removeChild(i-1);
						i--;
					}
					i--;
				}
			}

			//put the noun in the clause at the topmost place with an opening for a noun.
			//Note that this may mess up if there are noun phrase adjuncts like "The man I met Tuesday".

			//specifically:
			//the parent of the noun can be either a clause (S) as in "The man who met me"
			//or a verb phrase as in "The man who I met".
			//for verb phrases, add the noun to the end since it will be an object.
			//for clauses, add the noun to the beginning since it will be the subject.
			tregexOpStr = "S|VP=newparent !< NP < (VP=verb !< TO !$ TO)";
			matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
			matcherclause = matchPattern.matcher(relclause);
			boolean subjectMovement = true;
			if(!matcherclause.find()){
				tregexOpStr = "VP=newparent !< VP < /VB.*/=verb !>> (S !< NP) !<< (VP !< VP !< NP)";
				matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
				matcherclause = matchPattern.matcher(relclause);
				subjectMovement = false;
			}

			//reset (so the first match isn't skipped)
			matcherclause = matchPattern.matcher(relclause);

			if(matcherclause.find()){
				Tree newparenttree = matcherclause.getNode("newparent");
				Tree verbtree = matcherclause.getNode("verb");
				boolean ppRelativeClause = false;

				if(matcher.getNode("wherecomp") != null){
					String tmp = "(PP (IN at) "+missingArgumentTree.toString()+")";
					missingArgumentTree = AnalysisUtilities.getInstance().readTreeFromString(tmp);
					ppRelativeClause = true;
					subjectMovement = false;
				}else if(matcher.getNode("preposition") != null){
					String tmp = "(PP (IN "+getStringFromTree(matcher.getNode("preposition"))+") "+missingArgumentTree.toString()+")";
					missingArgumentTree = AnalysisUtilities.getInstance().readTreeFromString(tmp);
					ppRelativeClause = true;
				}

				if(subjectMovement){	//subject
					newparenttree.addChild(newparenttree.objectIndexOf(verbtree), missingArgumentTree);
				}else{ // newparentlabel is VP
					if(ppRelativeClause) newparenttree.addChild(newparenttree.numChildren(), missingArgumentTree);
					else newparenttree.addChild(newparenttree.objectIndexOf(verbtree)+1, missingArgumentTree);
				}


				//create a new tree with punctuation
				Tree newTree = factory.newTreeNode("ROOT", new ArrayList<Tree>());
				newTree.addChild(relclause);
				AnalysisUtilities.addPeriodIfNeeded(newTree);

				if(GlobalProperties.getDebug()) System.err.println("extractRelativeClauses: "+ newTree.toString());
				addQuotationMarksIfNeeded(newTree);
				blaze.athena.QuestionGeneration.Question newTreeWithFeatures = input.deeperCopy();
				newTreeWithFeatures.setIntermediateTree(newTree);
				if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromRelativeClause", 1.0);
				addIfNovel(extracted, newTreeWithFeatures);
			}
		}
	}






	/**
	 * e.g., In January, John wore his winter coat. -> John wore his winter coat in January.
	 *
	 * @param input
	 * @return
	 */
	private void moveLeadingPPsAndQuotes(blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;
		Tree mainvp = null;
		Tree subj = null;

		tregexOpStr = "ROOT < (S|SINV=mainclause < (NP|SBAR=subj !$++ /,/) < VP=mainvp "
			+ " [ < (PP=modifier < NP) " //must be a PP with an NP object
			+ "| < (S=modifier < SBAR|NP <<# VB|VBD|VBP|VBZ) ] ) "; //OR: a quote, which is an S clause with a subject and finite main verb
		//the modifiers to move must be immediately followed by commas

		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());

		List<Tree> modifiers = new ArrayList<Tree>();
		while(matcher.find()){
			if(mainvp == null){
				mainvp = matcher.getNode("mainvp").deepCopy();
				subj = matcher.getNode("subj").deepCopy();
			}
			Tree mainclause = matcher.getNode("mainclause");
			Tree modifier = matcher.getNode("modifier").deepCopy();
			int idx = mainclause.objectIndexOf(modifier);
			if(modifiers.contains(modifier)) continue; //just in case the tregex expression catches duplicates
			//add commas and quotation marks if they appeared in the original
			if(idx > 0 && mainclause.getChild(idx-1).label().toString().equals("``")){
				modifiers.add(AnalysisUtilities.getInstance().readTreeFromString("(, ,)"));
				modifiers.add(AnalysisUtilities.getInstance().readTreeFromString("(`` ``)"));
				Tree sbar = factory.newTreeNode("SBAR", new ArrayList<Tree>());
				sbar.addChild(modifier);
				modifiers.add(sbar);
				modifiers.add(AnalysisUtilities.getInstance().readTreeFromString("('' '')"));
			}else{
				modifiers.add(modifier);
			}
		}

		if(mainvp != null){ //any matches?
			for(Tree m: modifiers){
				mainvp.addChild(m);
			}

			Tree newTree = factory.newTreeNode("ROOT", new ArrayList<Tree>());
			Tree clause = factory.newTreeNode("S", new ArrayList<Tree>());
			newTree.addChild(clause);
			clause.addChild(subj);
			clause.addChild(mainvp);

			AnalysisUtilities.addPeriodIfNeeded(newTree);
			addQuotationMarksIfNeeded(newTree);
			if(GlobalProperties.getDebug()) System.err.println("moveLeadingModifiers: "+ newTree.toString());
			input.setIntermediateTree(newTree);
			if(GlobalProperties.getComputeFeatures()) input.setFeatureValue("movedLeadingPPs", 1.0);
		}

	}


	/**
	 * e.g., John, hoping to get a good grade, studied. -> John hoped to get a good grade.
	 *   Walking to the store, John saw Susan -> John was walking to the store.
	 *
	 *   NOTE: This method produces false positives for sentences like,
	 *   			"Broadly speaking, the project was successful."
	 *   		where the participial phrase does not modify the subject.
	 *
	 * @param extracted
	 * @param input
	 */
	private void extractNounParticipialModifiers(Collection<blaze.athena.QuestionGeneration.Question> extracted, blaze.athena.QuestionGeneration.Question input) {
		String tregexOpStr;
		TregexPattern matchPattern;
		TregexMatcher matcher;

		tregexOpStr = "ROOT < (S "
			+ " [ << (NP < (NP=subj  $++ (/,/ $+ (VP=modifier <# VBN|VBG|VP=tense )))) " 	//modifiers that appear after nouns
			+ " | < (S !< NP|SBAR < (VP=modifier <# VBN|VBG|VP=tense) $+ (/,/ $+ NP=subj)) " 	//modifiers before the subject. e.g., Founded by John, the company...
			+ " | < (SBAR < (S !< NP|SBAR < (VP=modifier <# VBN|VBG=tense)) $+ (/,/ $+ NP=subj)) " //e.g., While walking to the store, John saw Susan.
			+ " | < (PP=modifier !< NP <# VBG=tense $+ (/,/ $+ NP=subj)) ] ) " // e.g., Walking to the store, John saw Susan.
			+ " <<# /^VB.*$/=maintense ";	//tense determined by top-most verb

		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(input.getIntermediateTree());
		while(matcher.find()){
			Tree nountree = matcher.getNode("subj").deepCopy();
			Tree vptree = matcher.getNode("modifier");
			Tree verb = matcher.getNode("tense");
			makeDeterminerDefinite(nountree);

			if(vptree.label().toString().equals("PP")) vptree.label().setValue("VP");
			String verbPOS = findTense(matcher.getNode("maintense"));
			if(vptree == null || nountree == null) return;

			String newTreeStr;
			if(verb.label().toString().equals("VBG")){
				//for present partcipials, change the tense to the tense of the main verb
				//e.g., walking to the store -> walked to the store
				String verbLemma =  AnalysisUtilities.getInstance().getLemma(verb.getChild(0).label().toString(), verb.label().toString());
				String newVerb = AnalysisUtilities.getInstance().getSurfaceForm(verbLemma, verbPOS);
				int verbIndex = vptree.objectIndexOf(verb);
				vptree = vptree.deepCopy();
				vptree.removeChild(verbIndex);
				vptree.addChild(verbIndex, AnalysisUtilities.getInstance().readTreeFromString("("+verbPOS+" "+newVerb+")"));
				newTreeStr = "(ROOT (S "+matcher.getNode("subj").toString()+" "+vptree.toString()+" (. .)))";
			}else{
				//for past participials, add a copula
				//e.g., John, exhausted, -> John was exhausted
				//(or for conjunctions, just add the copula---kind of a hack to make the moby dick sentence work out)
				String auxiliary;
				if(verbPOS.equals("VBP") || verbPOS.equals("VBD")){
					if(isPlural(nountree)) auxiliary = "(VBD were)";
					else auxiliary = "(VBD was)";
				}else{
					if(isPlural(nountree)) auxiliary = "(VB are)";
					else auxiliary = "(VBZ is)";
				}

				newTreeStr = "(ROOT (S "+nountree+" (VP "+auxiliary+" "+vptree+") (. .)))";
			}

			Tree newTree = AnalysisUtilities.getInstance().readTreeFromString(newTreeStr);
			correctTense(newTree.getChild(0).getChild(0), newTree.getChild(0));
			addQuotationMarksIfNeeded(newTree);

			if(GlobalProperties.getDebug()) System.err.println("extractNounParticipialModifiers: "+ newTree.toString());
			blaze.athena.QuestionGeneration.Question newTreeWithFeatures = input.deeperCopy();
			newTreeWithFeatures.setIntermediateTree(newTree);
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromParticipial", 1.0); //old feature name
			if(GlobalProperties.getComputeFeatures()) newTreeWithFeatures.setFeatureValue("extractedFromNounParticipial", 1.0);
			extracted.add(newTreeWithFeatures);
		}


	}



	/**
	 * Convert a non-definite determiner to "the".
	 * Used when extracting from noun modifiers such as relative clauses.
	 * E.g., "A tall man, who was named Bob, entered the store."
	 * -> "A tall man was named Bob."
	 * -> "THE tall man was named Bob."
	 *
	 * @param np
	 */
	private void makeDeterminerDefinite(Tree np) {
		String tregexOpStr = "NP !> __ <+(NP) (DT=det !< the)";
		TregexPattern matchPattern = TregexPatternFactory.getPattern(tregexOpStr);

		TsurgeonPattern p;
		List<TsurgeonPattern> ps = new ArrayList<TsurgeonPattern>();
		List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();

		ps.add(Tsurgeon.parseOperation("replace det (DT the)"));
		p = Tsurgeon.collectOperations(ps);
		ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
		Tsurgeon.processPatternsOnTree(ops, np);
	}






	protected boolean isPlural(Tree nountree){
		String headTerminalLabel = nountree.headPreTerminal(AnalysisUtilities.getInstance().getHeadFinder()).label().toString();
		return (headTerminalLabel.equals("NNS") || headTerminalLabel.equals("NPS"));
	}


	/**
	 * e.g., John, who hoped to get a good grade, studied. -> John studied.
	 *
	 */
	private boolean removeNonRestrRelClausesAndParticipials(blaze.athena.QuestionGeneration.Question q) {
		List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
		String tregexOpStr;
		TregexPattern matchPattern;
		TsurgeonPattern p;
		List<TsurgeonPattern> ps;

		ps = new ArrayList<TsurgeonPattern>();
		tregexOpStr = "NP < (VP|SBAR=mod $- /,/=punc !$+ /,/ !$ CC|CONJP)";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		boolean modified = false;
		if(matchPattern.matcher(q.getIntermediateTree()).find()){
			ps.add(Tsurgeon.parseOperation("prune punc"));
			ps.add(Tsurgeon.parseOperation("prune mod"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());
			if(GlobalProperties.getComputeFeatures()) q.setFeatureValue("removedNonRestrRelClausesAndParticipials", 1.0);
			modified = true;
		}

		ps = new ArrayList<TsurgeonPattern>();
		tregexOpStr = "NP < (VP|SBAR=mod $- /,/=punc $+ /,/=punc2 !$ CC|CONJP)";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		if(matchPattern.matcher(q.getIntermediateTree()).find()){
			ps.add(Tsurgeon.parseOperation("prune punc"));
			ps.add(Tsurgeon.parseOperation("prune mod"));
			ps.add(Tsurgeon.parseOperation("prune punc2"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());
			if(GlobalProperties.getComputeFeatures()) q.setFeatureValue("removedNonRestrRelClausesAndParticipials", 1.0);
			modified = true;
		}

		return modified;
	}


	/**
	 *
	 * e.g., John Smith (1931-1992) was a fireman. -> John Smith was a Fireman.
	 *
	 * @return whether or not a change was made
	 */
	private boolean removeParentheticals(blaze.athena.QuestionGeneration.Question q) {
		List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
		String tregexOpStr;
		TregexPattern matchPattern;
		TsurgeonPattern p;
		List<TsurgeonPattern> ps;
		boolean res = false;

		ps = new ArrayList<TsurgeonPattern>();
		tregexOpStr = "__=parenthetical [ $- /-LRB-/=leadingpunc $+ /-RRB-/=trailingpunc " +
				" | $+ /,/=leadingpunc $- /,/=trailingpunc !$ CC|CONJP "+
				" | $+ (/:/=leadingpunc < --) $- (/:/=trailingpunc < /--/) ]";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		if(matchPattern.matcher(q.getIntermediateTree()).find()){
			ps.add(Tsurgeon.parseOperation("prune leadingpunc"));
			ps.add(Tsurgeon.parseOperation("prune parenthetical"));
			ps.add(Tsurgeon.parseOperation("prune trailingpunc"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());

			if(res)	addQuotationMarksIfNeeded(q.getIntermediateTree());
			if(GlobalProperties.getComputeFeatures()) q.setFeatureValue("removedParentheticals", 1.0);
			res = true;
		}

		ps = new ArrayList<TsurgeonPattern>();
		tregexOpStr = "PRN=parenthetical";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		if(matchPattern.matcher(q.getIntermediateTree()).find()){
			ps.add(Tsurgeon.parseOperation("prune parenthetical"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));
			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());
			if(GlobalProperties.getComputeFeatures()) q.setFeatureValue("removedParentheticals", 1.0);
			res = true;
		}

		return res;
	}




	public long getNumSimplifyHelperCalls() {
		return numSimplifyHelperCalls;
	}

	public void setNumSimplifyHelperCalls(long numSimplifyHelperCalls) {
		this.numSimplifyHelperCalls = numSimplifyHelperCalls;
	}



	/**
	 *
	 * e.g., John, the painter, knew Susan.  -> John knew Susan.
	 *
	 * @param q
	 * @return whether or not a change was made
	 */
	private boolean removeAppositives(blaze.athena.QuestionGeneration.Question q) {
		List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
		String tregexOpStr;
		TregexPattern matchPattern;
		TsurgeonPattern p;
		List<TsurgeonPattern> ps;

		ps = new ArrayList<TsurgeonPattern>();
		tregexOpStr = "NP=parent < (NP=child $++ (/,/ $++ NP|PP=appositive) !$-- /,/) !< CC|CONJP";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		if(matchPattern.matcher(q.getIntermediateTree()).find()){

			ps.add(Tsurgeon.parseOperation("move child $+ parent"));
			ps.add(Tsurgeon.parseOperation("prune parent"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern,TsurgeonPattern>(matchPattern,p));

			Tsurgeon.processPatternsOnTree(ops, q.getIntermediateTree());
			addQuotationMarksIfNeeded(q.getIntermediateTree());

			if(GlobalProperties.getComputeFeatures()) q.setFeatureValue("removedAppositives", 1.0);
			return true;
		}else{
			return false;
		}
	}





	public void setBreakNPs(boolean breakNPs) {
		this.breakNPs = breakNPs;
	}





	public boolean getBreakNPs() {
		return breakNPs;
	}



	public void setExtractFromVerbComplements(boolean extractFromVerbComplements) {
		this.extractFromVerbComplements = extractFromVerbComplements;
	}





	public boolean getExtractFromVerbComplements() {
		return extractFromVerbComplements;
	}


	private static String simplificationFeatureString(blaze.athena.QuestionGeneration.Question q) {
		String res = "";

		res += q.getFeatureValue("extractedFromAppositive");
		res += "\t" + q.getFeatureValue("extractedFromComplementClause");
		res += "\t" + q.getFeatureValue("extractedFromConjoined");
		res += "\t" + q.getFeatureValue("extractedFromConjoinedNPs");
		res += "\t" + q.getFeatureValue("extractedFromNounParticipial");
		res += "\t" + q.getFeatureValue("extractedFromRelativeClause");
		res += "\t" + q.getFeatureValue("extractedFromSubordinateClause");
		res += "\t" + q.getFeatureValue("extractedFromVerbParticipial");
		//res += "\t" + q.getFeatureValue("extractedFromWithParticipial");
		res += "\t" + q.getFeatureValue("movedLeadingPPs");
		res += "\t" + q.getFeatureValue("removedAppositives");
		res += "\t" + q.getFeatureValue("removedClauseLevelModifiers");
		res += "\t" + q.getFeatureValue("removedNonRestrRelClausesAndParticipials");
		res += "\t" + q.getFeatureValue("removedParentheticals");
		res += "\t" + q.getFeatureValue("removedVerbalModifiersAfterCommas");
		res += "\t" + q.getFeatureValue("extractedFromLeftMostMainClause");

		return res;
	}


	/**
	 */
	public List<QuestionDTO> run(String doc) {
		StanfordParserServer server = StanfordParserServer.getInstance();

		boolean verbose = false;
		String propertiesFile = "config"+File.separator+"factual-statement-extractor.properties";

		GlobalProperties.loadProperties(propertiesFile);
		Tree parsed;

		//pre-load
		if(GlobalProperties.getDebug()) System.err.println("Enter sentence: ");
		try{
			String topic = "";

			long startTime = System.currentTimeMillis();
			List<String> sentences = AnalysisUtilities.getSentences(doc);

			//iterate over each segmented sentence and generate questions
			List<blaze.athena.QuestionGeneration.Question> output = new ArrayList<blaze.athena.QuestionGeneration.Question>();

			List<String> sentencesList = new ArrayList<String>();
			List<Tree> parsedList = new ArrayList<Tree>();
			List<String> topicList = new ArrayList<String>();
			String oldSentence = "INITIAL SENTENCE IS EMPTY";

			for(String sentence: sentences){
				int numWords = sentence.split(" ").length;
				String copy = sentence;
				if (copy.replaceAll("[^a-zA-Z ]", "").split(" ").length <= 5 &&
						(oldSentence.equals("INITIAL SENTENCE IS EMPTY") || oldSentence.replace(" ", "").equals("==========."))) {
					topic = sentence.replace(".", "").trim();
					System.out.println("topic is: " + sentence);
					oldSentence = sentence;
					continue;
				} else if (oldSentence.replace(" ", "").equals("==========.")) {
                    topic = ""; //reset topic
                }
				oldSentence = sentence;

				parsed = server.parse(sentence);

				if (numWords < 5) {
					if (Character.isUpperCase(sentence.trim().charAt(0))/* && parsed.firstChild().label().value().equals("NP")*/) {
						topic = sentence.replace(".", "").trim();
					}
					continue; //don't include sentences that are fewer than 5 words
				}

//				//not a proper sentence structure, so continue
				if (!parsed.firstChild().label().value().equals("S")) {
					continue;
				}

				if(GlobalProperties.getDebug()) System.err.println("input: "+getStringFromTree(parsed));
				if(GlobalProperties.getDebug()) System.err.println("parse: "+sentence);
				//if no parse, print the original sentence
				if(getStringFromTree(parsed).equals(".")){
					System.out.print(sentence);
					if(verbose) System.out.print("\t"+sentence);
					System.out.println();
					continue;
				}

				sentencesList.add(sentence);
				parsedList.add(parsed);
				topicList.add(topic);
				oldSentence = sentence;
			}

			List<String> questionList = new ArrayList<String>();
			List<Tree> answerList = new ArrayList<Tree>();

			for (int i = 0; i < sentencesList.size(); i++) {
				Tree taggedWords = parsedList.get(i);
				boolean nounPhrasePresent = false;
				Tree[] children = taggedWords.firstChild().children();

				for (Tree phrase : children) {
					if (phrase.value().equals("NP")) {
						nounPhrasePresent = true;
						// there should be some noun phrase before the verb phrase for a valid sentence
						// Learning objectives tend to start with "Understand" - these are not questionable sentences
					} else if (phrase.value().equals("VP") && nounPhrasePresent && !sentencesList.get(i).startsWith("Understand")) {
						List<Tree> nounPhrases = getNounPhrase(phrase);
						for (Tree nounPhrase : nounPhrases) {
							String nounPhraseString = getStringFromTree(nounPhrase);
//							// we need to remove "the" if it appears at the start of a noun phrase
//							if (nounPhraseString.startsWith("the ")){
//								nounPhraseString = nounPhraseString.replaceFirst("the ", "");
//							}
							if (!filterQuestions(nounPhraseString, topicList.get(i), sentencesList.get(i))) {
								continue;
							}
							questionList.add(topicList.get(i) + ":::" + sentencesList.get(i).replaceFirst("\\b" + Pattern.quote(nounPhraseString) + "\\b", " __________________"));
							//answerList.add(nounPhraseString.replace("-LRB- ", "(").replace(" -RRB-", ")"));
							answerList.add(nounPhrase);
						}
					}
				}
			}

			int size = questionList.size();
			List<QuestionDTO> questionDTOList = new ArrayList<>();

			for (int i = 0; i < size; i++) {
				//double check to ensure there is a blank to fill
				if (!questionList.get(i).contains("__________________")) {
					continue;
				}

				String topicString = questionList.get(i).split(":::")[0].replaceAll("[^\\x00-\\x7F]", "");
				String questionString = questionList.get(i).split(":::", 2)[1].replaceAll("[^\\x00-\\x7F]", "");
				QuestionDTO q = new QuestionDTO(topicString, questionString);
				List<String> answers = getAnswerSet(answerList, i);
				String correctAnswer = getStringFromTree(answerList.get(i));
				//String correctAnswer = getStringFromTree(answerList.get(i)) + " --- " + answerList.get(i).taggedLabeledYield();
				q.addAnswer(answers.get(0));
				q.addAnswer(answers.get(1));
				q.addAnswer(answers.get(2));
				q.addAnswer(answers.get(3));
				q.setAnswer(answers.indexOf(correctAnswer));
				questionDTOList.add(q);

				System.out.println(questionList.get(i));
				System.out.println("a) " + answers.get(0));
				System.out.println("b) " + answers.get(1));
				System.out.println("c) " + answers.get(2));
				System.out.println("d) " + answers.get(3));
				System.out.println();
			}
			Collections.shuffle(questionDTOList);
			System.err.println("Seconds Elapsed:\t"+((System.currentTimeMillis()-startTime)/1000.0));

			return questionDTOList;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	private boolean filterQuestions(String nounPhraseString, String topic, String sentence) {
		if (nounPhraseString.length() < 3 || nounPhraseString.matches("them|they|you") || nounPhraseString.startsWith("http")) { //filter bad answers
			return false;
		}
		if (sentence.matches("[\\s\\S]*\\([\\s\\S]*" + Pattern.quote(nounPhraseString.trim()) + "[\\s\\S]*\\)[\\s\\S]*")) { // ignore if answer in brackets
			return false;
		}
		if (StringUtils.containsIgnoreCase(topic, "figure") || StringUtils.containsIgnoreCase(topic, "table")
				|| StringUtils.containsIgnoreCase(nounPhraseString, "figure") || StringUtils.containsIgnoreCase(nounPhraseString, "table")
				|| StringUtils.containsIgnoreCase(sentence, "figure") || StringUtils.containsIgnoreCase(sentence, "table")) { //ignore questions related to a figure or table
			return false;
		}
		return true;
	}

	private List<String> getAnswerSet(List<Tree> answersList, int answerIndex) {
		Tree currentAnswer = answersList.get(answerIndex);
		List<Answer> answers = new ArrayList<>();
		answers.add(new Answer(getStringFromTree(currentAnswer)/* + " --- " + currentAnswer.taggedLabeledYield()*/, -1.00)); //give correct answer the lowest value (lowest is best)
		int indexOfHighest = 0;
		double highestValue = -1;

		Set<String> answersSet = new HashSet<>();
		answersSet.add(getStringFromTree(currentAnswer));

		//generate distractors
		for (Tree t : answersList) {
			Answer newAnswer = new Answer(getStringFromTree(t)/* + " --- " + t.taggedLabeledYield()*/, getSimilarity(t,currentAnswer));
			if (answers.size() < 4 && !answers.contains(newAnswer)) {
				answers.add(newAnswer); //if there's not already 4 answers, then add this and check if it is the newest worst answer
				if (newAnswer.getValue() > highestValue) {
					indexOfHighest = answers.size() - 1;
					highestValue = newAnswer.getValue();
				}
			} else {
				if (newAnswer.getValue() < highestValue && !answers.contains(newAnswer)) { //only add if this one is better than the current worse
					answers.set(indexOfHighest, newAnswer);
					indexOfHighest = answers.size() - 1; //assume the one we added is the worst
					highestValue = newAnswer.getValue();
					for (int i = 0; i < answers.size(); i++) {
						if (answers.get(i).getValue() > highestValue) {
							indexOfHighest = i;
							highestValue = answers.get(i).getValue(); //but if any are worse, we set that as highest instead
						}
					}
				}
			}
//			if (getSimilarity(t, answersList.get(answerIndex))) {
//				System.out.println("match! " + getStringFromTree(t) + " --- " + getStringFromTree(answersList.get(answerIndex)));
//			}
		}
		if (answers.size() < 4) {
			answers.add(new Answer("Your input was too short, so not enough answers can be generated", 99));
			answers.add(new Answer("Your input was too short, so not enough answers can be generated", 99));
			answers.add(new Answer("Your input was too short, so not enough answers can be generated", 99));
		}
		Collections.shuffle(answers);
		List<String> stringAnswers = new ArrayList<>();
		for (Answer a : answers) {
			stringAnswers.add(a.getAnswerText());
		}
		return stringAnswers;
	}

	/**
	 * gets the similarity between answer structure
	 * (lower is better!)
	 */
	private double getSimilarity(Tree t, Tree t2) {
		if (t.equals(t2)) {
			return 999999; //same answer!
		}
		double difference = StringUtils.getLevenshteinDistance(t.taggedLabeledYield().toString(), t2.taggedLabeledYield().toString());
		return difference + Math.abs(getStringFromTree(t).length() - getStringFromTree(t2).length())/10.0; //add on the length difference as a fraction, for tiebreakers
	}

	private static List<Tree> getNounPhrase(Tree phrase) {
		List<Tree> nouns = new ArrayList<>();
		if (phrase.isLeaf()) {
			return nouns;
		}
		for (Tree phrase2 : phrase.children()) {
			List<Tree> s;
			String phrase2String = getStringFromTree(phrase2);
			if (phrase2.value().equals("NP") && phrase2String.split(" ").length < 5) {
			//	System.out.println("the noun phrase is: " + Sentence.listToString(phrase2.yieldWords()));
				nouns.add(phrase2);
			} else if ((s = getNounPhrase(phrase2)) != null && s.size() != 0) {
				nouns.addAll(s);
			}
		}
		return nouns;
	}

	private static String getStringFromTree(Tree tree) {
		return Sentence.listToString(tree.yieldWords()).trim();
	}

	private long numSimplifyHelperCalls; //for debugging, counts the number of call to simplifyHelper to check that duplicate derivations are avoided

	private boolean breakNPs = false; //whether to break conjunctions of noun phrases (e.g., John and I are friends.)
	private boolean extractFromVerbComplements = false; //whether to extract from complements (e.g., John thought that I studied -> I studied)

	private TreeFactory factory;
	private Set<String> verbsThatImplyComplements = null; //not yet fully implemented, can be ignored
}
