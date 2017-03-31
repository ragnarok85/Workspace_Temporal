package gob.cinvestav.mx.pte.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.mpii.clausie.ClausIE;
import de.mpii.clausie.Proposition;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import gob.cinvestav.mx.pte.clausie.Argument;
import gob.cinvestav.mx.pte.clausie.ClausieTriple;
import gob.cinvestav.mx.pte.clausie.Relation;
import gob.cinvestav.mx.pte.clausie.Subject;
import gob.cinvestav.mx.pte.clausie.TripleElement;
import gob.cinvestav.mx.pte.jena.Utility;
import gob.cinvestav.mx.pte.relation.QueryLOV;
import gob.cinvestav.mx.pte.relation.QueryNell;
import gob.cinvestav.mx.pte.relation.QueryWatson;
import gob.cinvestav.mx.pte.sentence.Word;
import gob.cinvestav.mx.pte.type.QueryWiBi;
import gob.cinvestav.mx.pte.type.WiBiTypes;
import gob.cinvestav.mx.pte.ws.AlchemyEntities;
import gob.cinvestav.mx.pte.ws.BabelfyEntities;
import gob.cinvestav.mx.pte.ws.Entities;
import gob.cinvestav.mx.pte.ws.Entity;
import gob.cinvestav.mx.pte.ws.EntityOpts;

public class Main {

//	static private MasterOfTriples MT = new MasterOfTriples();
	//static private String outputTriple = "triple.rdf";
	public static List<String> lovUris = new ArrayList<String>();
	public static Map<String,List<String>> problematicSentences = new HashMap<String,List<String>>();
	
	public static void main(String args[]) {
		// String sentence = "Disease caused by parasites have plagued humankind
		// for millennia and constitute a major global health problem";
		//String sentence = "This existential question has been raised by a series of experiments conducted recently at the Large Hadron Collider and the Relativistic Heavy Ion Collider that smash various atomic particles together at nearly the speed of light in order to create tiny drops of primordial soup.";
		// String sentence = "Nintendo announces new details on Mario Kart 8";
		// String sentence = "Cows in at least 8 herds have caught salmonella this season, twice as many as last year [2010]";
//		String sentence = "";
		//String sentence = "Bell, a telecommunication company, which is based in Los Angeles, makes and distributes electronic, computer and building products.";
//		String sentences[] = {"Nintendo announces new details on Mario Kart 8","Cows in at least 8 herds have caught salmonella this season, twice as many as last year [2010]"};
		//*****************Processing Sentence with ClausIE *************************//
//		if(args.length > 0){
//			sentence = args[0];
//		}else{
//			sentence = "Disease caused by parasites have plagued humankind for millennia and constitute a major global health problem";
//		}
		/*
		 * args[0] - input folder
		 * args[1] - output folder
		 * args[2] - output seeds folder
		 * args[3] - output text Triples
		 */
		
		if(args == null){
			System.exit(0);
		}else{
			new Main().initialRestrictions(args);
		}
		String problematicSentencesOuput = "problematicSnts.txt";
		System.out.println("Input path: " + args[0]);
		System.out.println("Output path: " + args[1]);
		System.out.println("Output seeds directory: " + args[2]);
		System.out.println("Output text triples: " + args[3]);
		System.out.println("Problematic Sentences file: " + problematicSentencesOuput);
		
		File inputDirectory = new File(args[0]);
		File outputDirectory = new File(args[1]);
		
		List<File> inputFiles = getFiles(inputDirectory);
		List<String> processedFiles = getFilesNames(outputDirectory);
		
		Main main = new Main();
		if(inputFiles.size() > 0){
			List<String>allSeeds = new ArrayList<String>();
			List<String> triples = new ArrayList<String>();
			System.out.println("number of inputFiles = " + inputFiles.size() + " number of processedFiles = " + processedFiles.size());
			for(File inputFile : inputFiles){
				if(processedFiles.contains(inputFile.getName())){
					System.out.println("The File \"" + inputFile.getName() + "\" was previously processed");
					continue;
				}
				List<String> seeds = new ArrayList<String>();
				List<String> sentences = new ArrayList<String>();
				
				System.out.println("Processing file: " + inputFile);
				int counterLines = 0;
				try(BufferedReader br = new BufferedReader(new FileReader(inputFile))){
					String line = "";
					while((line = br.readLine()) != null){
						if(line.split(" ").length > 3){
							sentences.add(line.toLowerCase());
							counterLines++;
						}
						
					}
					System.out.println("\t Number of sentences:\t" + counterLines);
				}catch(IOException e){
					e.printStackTrace();
				}
				if(sentences.size() > 0){
					triples.addAll(main.extractTriples(inputFile.getName(),sentences, args[1]+inputFile.getName()+".rdf", seeds, inputFile.getName()+".rdf", args[3]+inputFile.getName()));
				}
				main.writeSeeds(seeds, args[2]+"/Seeds-"+inputFile.getName());
			}
			Utility.writeProblematicSentences(problematicSentencesOuput, problematicSentences);
			Utility.fuseSeeds(args[2],allSeeds);
			main.writeSeeds(allSeeds, args[2]+"/AllSeeds.txt");
			Utility.printTriples(triples,args[3]+"/AllTriples.txt");
			
			
		}
	}
	
	public List<String> extractTriples(String inputFile, List<String> sentences, String outputTriple, List<String> seeds,
			String rdfModelFileName, String outputTriples ) {
		Utility utility = new Utility();
		int sentenceCounter = 1;
		List<String> triples = new ArrayList<String>();
		List<String> listProblematicSentences = new ArrayList<String>();
		for (String sentence : sentences) {
			System.out.println(
					"\t Processing sentence (" + sentenceCounter++ + "-" + sentences.size() + "): " + sentence);

			MasterOfTriples MT = new MasterOfTriples();
			ClausIE clausIE = new ClausIE();

			clausIE = processingClausIE(listProblematicSentences,sentence);
			if (clausIE == null) {
				continue;
			}
			MT.setClausieTriples(extractClausieTriples(clausIE.getPropositions()));
			triples.addAll(getClTriples(MT.getClausieTriples()));
			MT.setSntsWrds(extractWords(clausIE, sentence));

			// *****************NE/concept processing
			// ***********************************//
			MT.setAlchemyEntities(EntityOpts.extractAlchemyEntities(sentence));
			MT.setBabelfyEntities(EntityOpts.extractBabelfyEntities(sentence));
			List<Entity> entities = new ArrayList<Entity>();
			EntityOpts.fuseAlchemyUris(MT.getAlchemyEntities(), entities);
			EntityOpts.fuseBabelfyUris(MT.getBabelfyEntities(), entities);
			EntityOpts.printEntities(entities);

			// System.exit(0);

			if (!MT.getAlchemyEntities().isEmpty()) {
				for (AlchemyEntities alchemy : MT.alchemyEntities) {
					alchemy.setListPosition(searchListPosition(alchemy.getText(), MT.sntsWrds));
				}
			}

			if (!MT.getBabelfyEntities().isEmpty()) {
				for (BabelfyEntities babelfy : MT.babelfyEntities) {
					babelfy.setListPosition(searchListPosition(babelfy.getText(), MT.sntsWrds));
				}
			}

			lookTriplesPositionList(MT.clausieTriples, MT.sntsWrds);
			calculateStartEndTriples(MT.clausieTriples, MT.sntsWrds);

			System.out.println("=============reducing entities=======================");
			reduceAlchemyEntities(MT.getAlchemyEntities());
			reduceBabelfyEntities(MT.getBabelfyEntities(), MT.sntsWrds);
			System.out.println("=============remove entities=======================");
			removeAlchemyEntites(MT.getAlchemyEntities());
			removeBabelfyEntites(MT.getBabelfyEntities());
			lookEntities(MT.clausieTriples, MT.alchemyEntities, MT.babelfyEntities);
			System.out.println("=============Create triples=======================");
			createTriple(MT.clausieTriples);
			eliminateDuplicateURL(MT.clausieTriples);
			System.out.println("=============Search relation=======================");
			for (ClausieTriple triple : MT.clausieTriples) {
				String uri = "";
				if (triple.getSubject().getTextNE().length() > 0 && triple.getArgument().getTextNE().length() > 0) {
					uri = lookRelation(triple.getSubject().getTextNE(), triple.getRelation().getText(),
							triple.getArgument().getTextNE());
					triple.getRelation().setUri(uri);
					triple.getTriple().setRelationUri(uri);
				}

			}
			System.out.println("=============Search and create type triples =======================");
			for (ClausieTriple triple : MT.clausieTriples) {
				if (triple.getSubject().getTextNE().length() > 0) {
					List<String> wibiSubjectTypes = queryWiBi(triple.getSubject().getTextNE());
					if (wibiSubjectTypes != null && !wibiSubjectTypes.isEmpty()) {
						WiBiTypes wibi = new WiBiTypes();
						wibi.setUris(wibiSubjectTypes);
						triple.getSubject().setWibi(wibi);
					}
				}

				if (triple.getArgument().getTextNE().length() > 0) {
					List<String> wibiArgTypes = queryWiBi(triple.getArgument().getTextNE());
					if (wibiArgTypes != null && !wibiArgTypes.isEmpty()) {
						WiBiTypes wibi = new WiBiTypes();
						wibi.setUris(wibiArgTypes);
						triple.getArgument().setWibi(wibi);
					}
				}

			}

			for (ClausieTriple triple : MT.clausieTriples) {
				if (!triple.getSubject().getWibi().getUris().isEmpty()) {
					createTypeTriple(triple);
				}
				if (!triple.getArgument().getWibi().getUris().isEmpty()) {
					createTypeTriple(triple);
				}
			}

			System.out.println("=============Create and write model=======================");
			// Utility utility = new Utility();
			for (ClausieTriple triple : MT.getClausieTriples()) {
				utility.publicLovNameSpace(lovUris);
				// utility.addRdfsComment(sentence);
				utility.populateModel(triple, rdfModelFileName);
				utility.populateTypes(triple);
				// utility.writeTriple(outputTriple);
			}

			System.out.println("=============Triples=======================");
			printTriples(MT.clausieTriples);
			// System.out.println("=============Additional Inf.
			// types=======================");
			// System.out.println("\n original sentence = " + sentence);
			// MT.printWords();
			// MT.printClausieTriples();
			// MT.printAlchemyEntities();
			// MT.printBabelfyEntities();
		}
		if(listProblematicSentences.size() > 0){
			problematicSentences.put(inputFile, listProblematicSentences);
		}
		System.out.println("Saving information in: " + outputTriple);
		Utility.printTriples(triples, outputTriples);
		utility.writeTriple(outputTriple);
		utility.getSeeds(seeds);
		return triples;
	}
	
	/*
	 ************Phase 0 ClausIE *************** 
	 * (i) Processing sentence with ClausIE
	 * (ii) extract words from sentence 
	 * (iii) extract ClausIE triples
	*/
	
	//(i)
	public static ClausIE processingClausIE(List<String> listProblematicSentences, String sentence){
		ClausIE clausIE = new ClausIE();
		clausIE.initParser();
		
		
		try{
			clausIE.parse(sentence);
			clausIE.detectClauses();
			clausIE.generatePropositions();
		}catch(StackOverflowError s){
			listProblematicSentences.add(sentence + "********StackOverflowError");
			System.out.println("StackOverflowError.... in sentence: " + sentence);
			return null;
		}catch(NullPointerException e){
			listProblematicSentences.add(sentence + "********NullPointerException");
			System.out.println("sentence :" + sentence + "--- give no clause");
			return null;
		}
		
		
		return clausIE;
	}
	
	//(ii)
	public static List<Word> extractWords(ClausIE clausIE, String sentence) {
		List<Word> words = new ArrayList<Word>();

		for (CoreLabel token : clausIE.getDepTree().taggedLabeledYield()) {
			Word wrd = new Word();
			wrd.setWord(token.get(TextAnnotation.class));
			wrd.setPosTag(token.get(PartOfSpeechAnnotation.class));
			words.add(wrd);
		}
		// Extract start-end of each word
		CoreLabelTokenFactory ctf = new CoreLabelTokenFactory();
		PTBTokenizer ptb = new PTBTokenizer(new StringReader(sentence), ctf, "invertible=true");
		while (ptb.hasNext()) {
			CoreLabel label = (CoreLabel) ptb.next();
			for (Word word : words) {
				if (word.getWord().equalsIgnoreCase(label.originalText()) && !(word.getEnd() > 0)) {
					word.setStart(label.beginPosition());
					word.setEnd(label.endPosition());
					break;
				}
			}

		}
		return words;
	}

	//(iii)
	public static List<ClausieTriple> extractClausieTriples(List<Proposition> propositions) {
		List<ClausieTriple> clTriples = new ArrayList<ClausieTriple>();
		System.out.println("ClausIE triples: ");
		for (Proposition proposition : propositions) {
			if (proposition.noArguments() > 0) {
				ClausieTriple clTriple = new ClausieTriple();
				Subject subject = new Subject();
				Relation relation = new Relation();
				Argument argument = new Argument();
				subject.setText(proposition.subject());
				relation.setText(proposition.relation());
				argument.setText(proposition.argument(0));
				
				clTriple.setTriple(subject, relation, argument);
				System.out.println("\t" + subject.getText() + "," + relation.getText() + "," + argument.getText());
				clTriples.add(clTriple);
			}
		}
		return clTriples;

	}

	// **************Phase 1  NEs/Concepts**********************//
	/*
	 * (i) 		extract AlchemyEntities 
	 * (ii) 	extract BabelfyEntities 
	 * (iii)	look for elements in list (List<Word>) 
	 * (iv) 	Calculate start and end of each element
	 * 
	 */
	
	

	public static void lookTriplesPositionList(List<ClausieTriple> clTriple, List<Word> words) {
		for (ClausieTriple triple : clTriple) {
			triple.getSubject().setListPosition(searchListPosition(triple.getSubject().getText(), words));
			triple.getRelation().setListPosition(searchListPosition(triple.getRelation().getText(), words));
			triple.getArgument().setListPosition(searchListPosition(triple.getArgument().getText(), words));
		}
	}

	public static void calculateStartEndTriples(List<ClausieTriple> clTriple, List<Word> words) {
		for (ClausieTriple triple : clTriple) {
			if (!triple.getSubject().getListPosition().isEmpty())
				setStartEndTripleElement((TripleElement) triple.getSubject(), words);
			if (!triple.getRelation().getListPosition().isEmpty())
				setStartEndTripleElement((TripleElement) triple.getRelation(), words);
			if (!triple.getArgument().getListPosition().isEmpty())
				setStartEndTripleElement((TripleElement) triple.getArgument(), words);
		}
	}

	private static void setStartEndTripleElement(TripleElement element, List<Word> words) {
		int start = element.getListPosition().get(0);
		int end = element.getListPosition().get(element.getListPosition().size() - 1);
		element.setStart(words.get(start).getStart());
		element.setEnd(words.get(end).getEnd());
	}

	private static List<Integer> searchListPosition(String text, List<Word> words) {
		String[] splitText = text.split(" ");
		List<Integer> positions = new ArrayList<Integer>();
		for (int i = 0; i < splitText.length; i++) {
			for (Word word : words) {
				if (word.getWord().equalsIgnoreCase(splitText[i])) {
					positions.add(words.indexOf(word));
				}
			}
		}
		deleteDiscontinuousElements(positions);
		return positions;
	}

	public static void deleteDiscontinuousElements(List<Integer> positions) {
		// delete discontinuous elements from beginning and ending
		Collections.sort(positions);
		List<Integer> delete = new ArrayList<Integer>();
		for (int i = positions.size() - 1; i >= 0; i--) {
			if (i - 1 > 0) {
				int op = positions.get(i) - positions.get(i - 1);
				if (op > 1) {
					delete.add(Math.max(positions.get(i), positions.get(i - 1)));
				}
			} else {
				int op = positions.get(i) - positions.get(0);
				if (op > 1) {
					delete.add(Math.min(positions.get(i), positions.get(0)));
				}
			}

		}
		for (Integer del : delete) {
			positions.remove(del);
		}
	}

	// ************************ Phase 2 **********************************//
	/*
	 * (i) Concepts inside other concepts - discard the "small" concept (ii)
	 * NE/Concepts which are not in List<Word> - discard (iii)Look for subject's
	 * NE/Cs (ClausIE) (iv) Look for argument's NE/Cs (ClausIE)
	 * 
	 */

	public static void reduceAlchemyEntities(List<AlchemyEntities> entities) {
		List<Entities> toDiscard = new ArrayList<Entities>();
		for (Entities entity : entities) {
			String entityText = entity.getText();
			for (Entities innerEntity : entities) {
				String iEntityText = innerEntity.getText();
				if (!entityText.equalsIgnoreCase(iEntityText) && entityText.contains(iEntityText)) {
//					System.out.println(
//							"\"" + entityText + "\"" + " contains " + "\"" + iEntityText + "\"" + " (discarted)");
					toDiscard.add(innerEntity);
				}
			}
		}
		for (Entities discard : toDiscard) {
			entities.remove(discard);
		}

	}

	public static void reduceBabelfyEntities(List<BabelfyEntities> entities, List<Word> words) {
		List<BabelfyEntities> toDiscard = new ArrayList<BabelfyEntities>();
		for (BabelfyEntities entity : entities) {
			String entityText = entity.getText();

			// the concept is not a noun
			if (entity.getListPosition().size() == 1) {
				if (!words.get(entity.getListPosition().get(0)).getPosTag().contains("NN")) {
					toDiscard.add(entity);
				}
			}
			// score is equal to 0
			if (entity.getScore() == 0.0d) {
//				System.out.println("\"" + entityText + "\"" + " have score = 0.0 " + " (discarted)");
				toDiscard.add(entity);
			}
			// An entity is contained inside another entity
			for (BabelfyEntities innerEntity : entities) {
				String iEntityText = innerEntity.getText();
				if (!entityText.equalsIgnoreCase(iEntityText) && entityText.contains(iEntityText)) {
//					System.out.println(
//							"\"" + entityText + "\"" + " contains " + "\"" + iEntityText + "\"" + " (discarted)");
					toDiscard.add(innerEntity);
				}

			}
		}
		for (Entities discard : toDiscard) {
			entities.remove(discard);
		}

	}

	//Discard entities which are not part of the sentence
	//that mean, Alchemy return an URI which main word are not in the sentence
	public static void removeAlchemyEntites(List<AlchemyEntities> entities) {
		List<Entities> toDiscard = new ArrayList<Entities>();
		for (Entities entity : entities) {
			if (entity.getListPosition().isEmpty()) {
//				System.out.println("\"" + entity.getText() + "\"" + " -  didn't match with any element in the list");
				toDiscard.add(entity);
			}
		}
		for (Entities discard : toDiscard) {
			entities.remove(discard);
		}
	}

	public static void removeBabelfyEntites(List<BabelfyEntities> entities) {
		List<Entities> toDiscard = new ArrayList<Entities>();
		for (Entities entity : entities) {
			if (entity.getListPosition().isEmpty()) {
//				System.out.println("\"" + entity.getText() + "\"" + " -  didn't match with any element in the list");
				toDiscard.add(entity);
			}
		}
		for (Entities discard : toDiscard) {
			entities.remove(discard);
		}
	}

	public static void lookEntities(List<ClausieTriple> triples, List<AlchemyEntities> alchemyEntities,
			List<BabelfyEntities> babelfyEntities) {
		for (ClausieTriple triple : triples) {
//			System.out.println("Look entities for: " + triple.toString());

			for (AlchemyEntities alchemy : alchemyEntities) {
				if (triple.getSubject().getText().contains(alchemy.getText())) {
					triple.getSubject().getAlchemy().add(alchemy);
				}
				if (triple.getArgument().getText().contains(alchemy.getText())) {
					triple.getArgument().getAlchemy().add(alchemy);
				}
			}

			for (BabelfyEntities babelfy : babelfyEntities) {
				if (triple.getSubject().getText().contains(babelfy.getText())) {
					triple.getSubject().getBabelfy().add(babelfy);
				}
				if (triple.getArgument().getText().contains(babelfy.getText())) {
					triple.getArgument().getBabelfy().add(babelfy);
				}
			}
		}
	}

	/*
	 * (i) create triple (ii) delete duplicated uris
	 */

	public static void createTriple(List<ClausieTriple> clTriple) {
		for (ClausieTriple trip : clTriple) {
			Triple triple = new Triple();

			if (!trip.getSubject().getAlchemy().isEmpty()) {
				for (AlchemyEntities alchemy : trip.getSubject().getAlchemy()) {
					trip.getSubject().setTextNE(alchemy.getText());
					if (!alchemy.getDbpediaURL().isEmpty()) {
						triple.getSubjectUris().add(alchemy.getDbpediaURL());
					}
				}
			}
			if (!trip.getArgument().getAlchemy().isEmpty()) {
				for (AlchemyEntities alchemy : trip.getArgument().getAlchemy()) {
					trip.getArgument().setTextNE(alchemy.getText());
					if (!alchemy.getDbpediaURL().isEmpty()) {
						triple.getArgumentUris().add(alchemy.getDbpediaURL());
					}
				}
			}

			if (!trip.getSubject().getBabelfy().isEmpty()) {
				for (BabelfyEntities babelfy : trip.getSubject().getBabelfy()) {
					trip.getSubject().setTextNE(babelfy.getText());
					if (babelfy.getDbpediaURL() != null && !babelfy.getDbpediaURL().isEmpty()) {
						triple.getSubjectUris().add(babelfy.getDbpediaURL());
					}
				}
			}
			if (!trip.getArgument().getBabelfy().isEmpty()) {
				for (BabelfyEntities babelfy : trip.getArgument().getBabelfy()) {
					trip.getArgument().setTextNE(babelfy.getText());
					if (babelfy.getDbpediaURL() != null && !babelfy.getDbpediaURL().isEmpty()) {
						triple.getArgumentUris().add(babelfy.getDbpediaURL());
					}
				}
			}

			triple.setRelation(trip.getRelation().getText());
			if (!triple.getArgumentUris().isEmpty() && !triple.getSubjectUris().isEmpty()) {
				trip.setTriple(triple);
			}

		}
	}

	public static void eliminateDuplicateURL(List<ClausieTriple> clTriple) {
		for (ClausieTriple trip : clTriple) {
			List<Integer> deleteSubject = new ArrayList<Integer>();
			List<Integer> deleteArgument = new ArrayList<Integer>();
			Collections.sort(trip.getTriple().getSubjectUris());
			if (trip.getTriple().getSubjectUris().size() > 1) {
				for (int i = 0; i < trip.getTriple().getSubjectUris().size(); i++) {
					if (i + 1 < trip.getTriple().getSubjectUris().size()) {
						if (trip.getTriple().getSubjectUris().get(i)
								.equalsIgnoreCase(trip.getTriple().getSubjectUris().get(i + 1))) {
							deleteSubject.add(i + 1);
						}
					}
				}
			}
			Collections.sort(trip.getTriple().getArgumentUris());
			if (trip.getTriple().getArgumentUris().size() > 1) {
				for (int i = 0; i < trip.getTriple().getArgumentUris().size(); i++) {
					if (i + 1 < trip.getTriple().getArgumentUris().size()) {
						if (trip.getTriple().getArgumentUris().get(i)
								.equalsIgnoreCase(trip.getTriple().getArgumentUris().get(i + 1))) {
							deleteArgument.add(i + 1);
						}
					}
				}
			}
			Collections.sort(deleteSubject, Collections.reverseOrder());
			Collections.sort(deleteArgument, Collections.reverseOrder());
			for (int delSub : deleteSubject) {
				trip.getTriple().getSubjectUris().remove(delSub);
			}
			for (int delArg : deleteArgument) {
				trip.getTriple().getArgumentUris().remove(delArg);
			}

		}
	}

	public static void printTriples(List<ClausieTriple> clTriple) {
		for (ClausieTriple triple : clTriple) {
			System.out.println(triple.printTriple());
		}

	}
	
	public static boolean predicateRestrictions(String predicate){
		boolean isAlpha = true;
		char[] charSet = predicate.toCharArray();
		
		for(Character aChar : charSet){
			if( !aChar.equals("_") && !Character.isLetter(aChar)){
				isAlpha = false;
				break;
			}
		}
//		if(!predicate.contains("?")){
//			if(!Character.isLetter(predicate.charAt(0))){
//				isAlpha = false;
//			}
//		}else{
//			isAlpha = false;
//		}
		
		return isAlpha;
	}

	public List<String> getClTriples(List<ClausieTriple> clTriple){
		List<String> triples = new ArrayList<String>();
		System.out.println("saving ClausIE triple: ");
		for(ClausieTriple triple : clTriple){
			if (triple.getSubject().getText().length() > 0 && triple.getArgument().getText().length() > 0){
				String trip = "";
				trip = triple.getSubject().getText() + "\t" + triple.getRelation().getText() + "\t" + triple.getArgument().getText();
				System.out.println("\t" + trip);
				triples.add(trip);
			}
			
		}
		return triples;
	}
	/*
	 * Look for predicates (i) consult NELL (ii) consult LOV
	 * 
	 */

	public static String lookRelation(String subject, String predicate, String argument) {
		String uri = "";

		uri = queryNELL(subject, argument);
//		if(uri.contains("http"))
//			System.out.println("NELL result = " + uri);
//		else{
//			List<String> luri = queryLOV(predicate);
//			if(luri != null && !luri.isEmpty()){
//				//luri.get(1) = http://....
//				uri =luri.get(1);
//				if(uri.contains("http")){
//					System.out.println("LOV result = " + uri);
//					//luri.get(0) = nms:id
//					String[] parts = luri.get(0).split(":");
//					lovUris.add(parts[0]+";"+uri.substring(0,uri.length()-parts[1].length()));
//				}
//			}
//			else if(luri == null || luri.isEmpty()){
		if (uri.contains("http"))
			System.out.println("NELL result = " + uri);

		if (uri.length() == 0) {
			List<String> luri = new ArrayList<String>();
			//luri = queryWatson(predicate); // watson's web service is not working
			if (luri.size() > 0) {
				Iterator<String> iter = luri.iterator();
				uri = iter.next();
				if (uri.contains("http")) {
					System.out.println("Watson's result = " + uri);
					// luri.get(0) = nms:id
				}
			}
		}
		if (uri.length() == 0 && predicateRestrictions(predicate)) {
			predicate = predicate.replaceAll(" ", "_");
			uri = "http://www.cinvestav.com.mx/#" + predicate;
			System.out.println("The default uri will be = " + uri);
		}
		
		if(uri.length() == 0){
			uri = "http://www.cinvestav.com.mx/#RelatedWith";
			System.out.println("The default uri will be = " + uri);
		}

		return uri;
	}

	public static String queryNELL(String subject, String argument) {
		String pred = "";
		try {
			pred = QueryNell.sendGet(subject, argument);
			if(pred.length() > 0)
				pred = "http://www.cinvestav.com.mx/#" + pred;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pred;
	}

	public static List<String> queryLOV(String predicate) {
		List<String> uri = null;
		try {
			uri = QueryLOV.sendGet(predicate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uri;
	}
	
	public static List<String> queryWatson(String predicate) {
		List<String> uri = null;
		try {
			uri = QueryWatson.asearch(predicate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uri;
	}
	
	public static List<String> queryWiBi(String concept){
		List<String> wibiTypes = new ArrayList<String>();
		
		try {
			wibiTypes = QueryWiBi.sendGet(concept);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(wibiTypes.isEmpty()){
			System.out.println("WiBi does not return something for: " + concept);
		}
		
		return wibiTypes;
	}
	
	public static TypeTriple createTypeTriple(ClausieTriple triple){
		TypeTriple tTriple = new TypeTriple();
		
		if(!triple.getTriple().getSubjectUris().isEmpty()){
			tTriple.setSubject(triple.getTriple().getSubjectUris().get(0));
			List<String> objects = new ArrayList<String>();
			for(String object : triple.getSubject().getWibi().getUris())
				objects.add(object);
			if(!objects.isEmpty()){
				tTriple.setObject(objects);
			}
		}
		
		if(!triple.getTriple().getArgumentUris().isEmpty()){
			tTriple.setSubject(triple.getTriple().getArgumentUris().get(0));
			List<String> objects = new ArrayList<String>();
			for(String object : triple.getArgument().getWibi().getUris())
				objects.add(object);
			if(!objects.isEmpty()){
				tTriple.setObject(objects);
			}
		}
		return tTriple;
		
	}
	
	public void initialRestrictions(String[] args){
		File file = new File(args[0]);
		File output = new File(args[1]);
		File outputSeeds = new File(args[2]);
		File outputTriples = new File(args[3]);
		System.out.println("args size = " + args.length + "["+args[0]+","+args[1]+","+args[2]+"]");
		if(!file.exists()){
			System.out.println("the input path doesn't exist");
			System.exit(0);
		}else if(!file.isDirectory()){
			System.out.println("the input path isn't a directory");
			System.exit(0);
		}
		if(!output.exists()){
			output.mkdir();
		}
		if(!outputSeeds.exists()){
			outputSeeds.mkdir();
		}
		if(!outputTriples.exists()){
			outputTriples.mkdir();
		}
	}
	
	public static List<File> getFiles(File inputDirectory){
		List<File> inputFiles = new ArrayList<File>();
		for(File file : inputDirectory.listFiles()){
			if(file.isFile() && file.getName().endsWith("txt")){
				inputFiles.add(file);
			}
		}
		return inputFiles;
		
	}
	
	public static List<String> getFilesNames(File inputDirectory){
		List<String> inputFiles = new ArrayList<String>();
		for(File file : inputDirectory.listFiles()){
			if(file.isFile() && file.getName().endsWith("rdf")){
				inputFiles.add(file.getName().replace(".rdf", ""));
			}
		}
		return inputFiles;
		
	}
	
	public void writeSeeds(List<String> seeds, String output){
		try(PrintWriter pw = new PrintWriter(new FileWriter(output))){
			for(String seed : seeds){
				pw.write(seed+"\n");
			}
			pw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
