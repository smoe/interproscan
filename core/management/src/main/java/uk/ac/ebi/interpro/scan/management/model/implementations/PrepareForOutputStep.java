package uk.ac.ebi.interpro.scan.management.model.implementations;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import uk.ac.ebi.interpro.scan.io.FileOutputFormat;
import uk.ac.ebi.interpro.scan.io.match.writer.ProteinMatchesJSONResultWriter;
import uk.ac.ebi.interpro.scan.io.match.writer.ProteinMatchesWithNucleotidesXMLJAXBFragmentsResultWriter;
import uk.ac.ebi.interpro.scan.io.match.writer.ProteinMatchesXMLJAXBFragmentsResultWriter;
import uk.ac.ebi.interpro.scan.management.model.Step;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;
import uk.ac.ebi.interpro.scan.management.model.implementations.writer.TarArchiveBuilder;
import uk.ac.ebi.interpro.scan.model.*;
import uk.ac.ebi.interpro.scan.persistence.MatchDAO;
import uk.ac.ebi.interpro.scan.persistence.NucleotideSequenceDAO;
import uk.ac.ebi.interpro.scan.persistence.ProteinDAO;
import uk.ac.ebi.interpro.scan.precalc.berkeley.conversion.toi5.SignatureLibraryLookup;
import uk.ac.ebi.interpro.scan.util.Utilities;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PrepareForOutputStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(PrepareForOutputStep.class.getName());

    //DAOs
    private ProteinDAO proteinDAO;
    private MatchDAO matchDAO;

    private NucleotideSequenceDAO nucleotideSequenceDAO;

    public void setProteinDAO(ProteinDAO proteinDAO) {
        this.proteinDAO = proteinDAO;
    }

    public void setMatchDAO(MatchDAO matchDAO) {
        this.matchDAO = matchDAO;
    }

    public void setNucleotideSequenceDAO(NucleotideSequenceDAO nucleotideSequenceDAO) {
        this.nucleotideSequenceDAO = nucleotideSequenceDAO;
    }

    @Override
    public void execute(StepInstance stepInstance, String temporaryFileDirectory) {
        final Set<NucleotideSequence> nucleotideSequences = new HashSet<>();

        final Set<Long> nucleotideSequenceIds = new HashSet<>();

        String proteinRange = "[" + stepInstance.getBottomProtein() + "-" + stepInstance.getTopProtein() + "]";
        Utilities.verboseLog("starting PrepareForOutputStep :" + proteinRange);
        Long bottomProteinId = stepInstance.getBottomProtein();
        Long topProteinId = stepInstance.getTopProtein();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting step with Id " + this.getId());
        }

        int proteinCount = 0;
        int matchCount = 0;

        Set<String> signatureLibraryNames = new HashSet<>();

        for (SignatureLibrary sig: SignatureLibrary.values() ){
            signatureLibraryNames.add(sig.getName());
        }

       // Map<String, Protein> keyToProteinMap = proteinDAO.getKeyToProteinMap();
        //Iterator it = keyToProteinMap.keySet().iterator();

//        List<Protein> proteinsinRange = proteinDAO.getProteinsBetweenIds(stepInstance.getBottomProtein(), stepInstance.getTopProtein());
//        List<Protein> proteinsinRange = getProteinsBetweenIds(stepInstance.getBottomProtein(), stepInstance.getTopProtein());
        /*
        List<String> proteinsinRange = getProteinsBetweenIds(stepInstance.getBottomProtein(), stepInstance.getTopProtein());

        for (String proteinKey: proteinsinRange) {
            proteinCount ++;

            for(String signatureLibraryName: signatureLibraryNames){

                final String dbKey = proteinKey + signatureLibraryName;
                Set<Match> matches = matchDAO.getMatchSet(dbKey);
                if (matches != null){
                    //Utilities.verboseLog("Get matches for protein  id: " + protein.getId() +  " dbKey (matchKey): " + dbKey);
                    for(Match match: matches){
                        //protein.addMatch(match);
                        matchCount ++;
                    }
                }
            }
            //keyToProteinMap.put(key, protein);
        }
        */
        int proteinRawCount = 0;
        Protein exampleProtein = null;

        try {
            simulateMarshalling(stepInstance, "p");
            //if()
            processNucleotideSequences(stepInstance);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

//        if (! sequenceType.equalsIgnoreCase("p")){
//            return;
//        }

        for (Long proteinIndex= bottomProteinId;proteinIndex <= topProteinId; proteinIndex ++){
            proteinRawCount ++;
            String proteinKey = Long.toString(proteinIndex);
            Protein protein = proteinDAO.getProtein(proteinKey);


            if(protein != null){
                proteinCount ++;
            }

            for(String signatureLibraryName: signatureLibraryNames){
                final String dbKey = proteinKey + signatureLibraryName;
                Set<Match> matches = matchDAO.getMatchSet(dbKey);
                if (matches != null){
                    //Utilities.verboseLog("Get matches for protein  id: " + protein.getId() +  " dbKey (matchKey): " + dbKey);
                    for(Match match: matches){
                        match.getSignature().getCrossReferences();
                        match.getSignature().getEntry();
                        protein.addMatch(match);
                        matchCount ++;
                    }
                }
            }
            if (exampleProtein == null && protein.getMatches().size() > 1) {
                exampleProtein = protein;
            }
            if (proteinIndex % 4000 == 0){
                Utilities.verboseLog(proteinRange + " - Of possible  " + proteinRawCount + " proteins, processed  " + proteinCount + " with  total matches : " + matchCount);
            }

            Protein proteinWithXref = proteinDAO.getProteinAndCrossReferencesByProteinId(proteinIndex);
            //Protein protein = proteinWithXref;

            //Utilities.verboseLog("proteinWithXref: \n" +  proteinWithXref.toString());
            for (OpenReadingFrame orf : proteinWithXref.getOpenReadingFrames()) {
                //Utilities.verboseLog("OpenReadingFrame: \n" +  orf.toString());
                NucleotideSequence seq = orf.getNucleotideSequence();
                //Utilities.verboseLog("NucleotideSequence: \n" +  seq.toString());
                if (seq != null) {
                    nucleotideSequences.add(seq);
                    nucleotideSequenceIds.add(seq.getId()); //store the Id
//                    Hibernate.initialize(seq);
//                    nucleotideSequenceDAO.persist(key, seq);
                }
            }

            //store protein back in kv store
            proteinDAO.persist(proteinKey, protein);



            //keyToProteinMap.put(key, protein);
        }


        if(nucleotideSequences.size() > 0) {
            //Utilities.verboseLog("nucleotideSequences : \n" + nucleotideSequences.iterator().next());
            try {
                //outputNTToXML(stepInstance, "n", nucleotideSequences);
                outputToXML(stepInstance, "n", nucleotideSequenceIds);
                //outputToJSON(stepInstance, "n", nucleotideSequences);
            } catch (IOException e) {
                LOGGER.error("Error writing to xml");
                e.printStackTrace();
            }
        }



        Utilities.verboseLog("nucleotideSequences size: " +  nucleotideSequences.size());


       // Map<String, Set<Match>> matchesForEachProtein = matchDAO.getMatchesForEachProtein();

        //Utilities.verboseLog("matchesForEachProtein: " + matchesForEachProtein.size());

        //for (String matchKey : matchesForEachProtein.keySet()){
        //    Set<Match> matches = matchesForEachProtein.get(matchKey);
            //Utilities.verboseLog("matchKey: " + matchKey + " match count: " + matches.size());
        //}


        //Set<Match> allMatches = matchDAO.getMatches();
        //matchCount = allMatches.size();

        //        Match testMatch = (Match) matchDAO.get("test1");


        Utilities.verboseLog("Total proteins in range " + proteinRange + " with matches :  " + proteinCount + " Total matches : " + matchCount);
        //Protein exampleProtein = proteinsinRange.get(1);
        if(exampleProtein != null) {
            //dont prin the example protein for now
//            Utilities.verboseLog("exampleProtein: " + exampleProtein.toString());
            int matchsize = 0;
            if (! exampleProtein.getMatches().isEmpty()){
                matchsize =  exampleProtein.getMatches().size();
            }
            Utilities.verboseLog("exampleProtein: " + exampleProtein.getId() + " matches: " + matchsize);
        }
    }

    List<String>   getProteinsBetweenIds(Long bottom, Long top){
        Long timestart = System.currentTimeMillis();
        List<String> proteinsinRange = new ArrayList<>();
        for (Long index= bottom;index <= top; index ++){
            String proteinKey = Long.toString(index);
            Protein protein = proteinDAO.getProtein(proteinKey);
            if(protein != null){
                proteinsinRange.add(proteinKey);
            }
        }
        Long timeTaken = System.currentTimeMillis() - timestart;
        int timeTakenSeconds = timeTaken.intValue() / 1000;
        Utilities.verboseLog("timeTakenSeconds to get range: [" + bottom + "-" + top + "] = " + timeTakenSeconds + " seconds ");

        return proteinsinRange;
    }


    private void processNucleotideSequences( StepInstance stepInstance){
        //
        //should we deal with nucleotides here
        //Utilities.verboseLog("proteinWithXref: \n" +  proteinWithXref.toString());
        final Set<NucleotideSequence> nucleotideSequences = new HashSet<>();

        final Set<Long> nucleotideSequenceIds = new HashSet<>();

        Long bottomProteinId = stepInstance.getBottomProtein();
        Long topProteinId = stepInstance.getTopProtein();

        int proteinCount = 0;


            Set<String> signatureLibraryNames = new HashSet<>();

            for (SignatureLibrary sig: SignatureLibrary.values() ){
                signatureLibraryNames.add(sig.getName());
            }

            for (Long proteinIndex= bottomProteinId;proteinIndex <= topProteinId; proteinIndex ++) {
                String proteinKey = Long.toString(proteinIndex);
                Protein protein = proteinDAO.getProteinAndCrossReferencesByProteinId(proteinIndex);
                //Protein protein = proteinDAO.getProtein(proteinKey);
                if (protein != null) {
                    proteinCount++;
                }
                for (OpenReadingFrame orf : protein.getOpenReadingFrames()) {
                    //Utilities.verboseLog("OpenReadingFrame: \n" +  orf.toString());
                    NucleotideSequence seq = orf.getNucleotideSequence();
                    //Utilities.verboseLog("NucleotideSequence: \n" +  seq.toString());
                    if (seq != null) {
                        nucleotideSequences.add(seq);
                        nucleotideSequenceIds.add(seq.getId()); //store the Id
                        //                    Hibernate.initialize(seq);
                        //                    nucleotideSequenceDAO.persist(key, seq);
                    }
                }
            }

            if(nucleotideSequences.size() > 0) {
                //Utilities.verboseLog("nucleotideSequences : \n" + nucleotideSequences.iterator().next());
                try {
                    //outputNTToXML(stepInstance, "n", nucleotideSequences);
                    outputToXML(stepInstance, "n", nucleotideSequenceIds);
                    //outputToJSON(stepInstance, "n", nucleotideSequences);
                } catch (IOException e) {
                    LOGGER.error("Error writing to xml");
                    e.printStackTrace();
                }
            }



            Utilities.verboseLog("nucleotideSequences size: " +  nucleotideSequences.size());
    }


    private void simulateMarshalling( StepInstance stepInstance, String sequenceType) throws IOException {
        if (! sequenceType.equalsIgnoreCase("p")){
            return;
        }
        final boolean isSlimOutput = false;
        final String interProScanVersion = "5-34";

        Path outputPath = getFinalPath(stepInstance, FileOutputFormat.XML);
        Utilities.verboseLog(10, " Prepare For OutputStep - output proteins to XML: " + outputPath );

        Long bottomProteinId = stepInstance.getBottomProtein();
        Long topProteinId = stepInstance.getTopProtein();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Load " + topProteinId + " proteins from the db.");
        }
        Utilities.verboseLog(10, " WriteOutputStep - proteins XML new " + " There are " + topProteinId + " proteins.");
        int count = 0;

        //final Set<NucleotideSequence> nucleotideSequences = new HashSet<>();


        int proteinCount = 0;
        int matchCount = 0;

        try (ProteinMatchesXMLJAXBFragmentsResultWriter writer = new ProteinMatchesXMLJAXBFragmentsResultWriter(outputPath, Protein.class, isSlimOutput)) {
            //writer.header(interProScanVersion);
            writer.header(interProScanVersion,   "protein-matches");
            Set<String> signatureLibraryNames = new HashSet<>();

            for (SignatureLibrary sig: SignatureLibrary.values() ){
                signatureLibraryNames.add(sig.getName());
            }

            for (Long proteinIndex= bottomProteinId;proteinIndex <= topProteinId; proteinIndex ++){
                String proteinKey = Long.toString(proteinIndex);
                Protein protein  = proteinDAO.getProteinAndCrossReferencesByProteinId(proteinIndex);
                //Protein protein = proteinDAO.getProtein(proteinKey);
                if(protein != null){
                    proteinCount ++;
                }

                for(String signatureLibraryName: signatureLibraryNames){
                    final String dbKey = proteinKey + signatureLibraryName;
                    Set<Match> matches = matchDAO.getMatchSet(dbKey);
                    if (matches != null){
                        //Utilities.verboseLog("Get matches for protein  id: " + protein.getId() +  " dbKey (matchKey): " + dbKey);
                        for(Match match: matches){
                            match.getSignature().getCrossReferences();
                            //match.getSignature().getEntry();
                            //try update with cross refs etc
                            //updateMatch(match);
                            protein.addMatch(match);
                            matchCount ++;
                        }
                    }
                }

                String xmlProtein = writer.marshal(protein);
                protein.getOpenReadingFrames().size();

                for(Match i5Match: protein.getMatches()){
                    //try update with cross refs etc
                    updateMatch(i5Match);
                }

                proteinDAO.persist(proteinKey, protein);

                //Utilities.verboseLog(10, " xmlProtein:" + xmlProtein);

            }
            writer.close();
        }catch (JAXBException e){
            e.printStackTrace();
        }catch (XMLStreamException e) {
            e.printStackTrace();
        }

        //remove the temp xmls file
        deleteTmpMarshallingFile(outputPath);
    }


    private void outputToXML( StepInstance stepInstance, String sequenceType, Set<Long> nucleotideSequenceIds) throws IOException {
        if (! sequenceType.equalsIgnoreCase("n")){
            return;
        }
        final boolean isSlimOutput = false;
        final String interProScanVersion = "5-34";

        Path outputPath = getFinalPath(stepInstance, FileOutputFormat.XML);
        Utilities.verboseLog(10, " Prepare For OutputStep - outputNTToXML: " + outputPath );

        Long bottomProteinId = stepInstance.getBottomProtein();
        Long topProteinId = stepInstance.getTopProtein();

        try (ProteinMatchesXMLJAXBFragmentsResultWriter writer = new ProteinMatchesXMLJAXBFragmentsResultWriter(outputPath, NucleotideSequence.class, isSlimOutput)) {
            //writer.header(interProScanVersion);
            if (! nucleotideSequenceIds.isEmpty()) {

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Load " + topProteinId + " proteins from the db.");
                }
                Utilities.verboseLog(10, " WriteOutputStep -NT XML new " + " There are " + topProteinId + " proteins.");
                int count = 0;
                writer.header(interProScanVersion,   "nucleotide-sequence-matches");
                //final Set<NucleotideSequence> nucleotideSequences = new HashSet<>();
                for (Long nucleotideSequenceId :nucleotideSequenceIds){
                    NucleotideSequence  nucleotideSequence = nucleotideSequenceDAO.getNucleotideSequence(nucleotideSequenceId);
                    String xmlNucleotideSequence = writer.marshal(nucleotideSequence);
                    String key = nucleotideSequence.getMd5();
                    nucleotideSequenceDAO.persist(key, nucleotideSequence);
                    //Utilities.verboseLog("Prepae OutPut xmlNucleotideSequence : " + nucleotideSequenceId + " -- "); // +  xmlNucleotideSequence);
                }

                Utilities.verboseLog("WriteOutPut nucleotideSequenceIds size: " +  nucleotideSequenceIds.size());
            }
            writer.close();

        }catch (JAXBException e){
            e.printStackTrace();
        }catch (XMLStreamException e) {
            e.printStackTrace();
        }
        //delete the tmp xml file
        deleteTmpMarshallingFile(outputPath);
    }



    private void outputNTToXML( StepInstance stepInstance, String sequenceType, Set<NucleotideSequence> nucleotideSequences) throws IOException {
        if (! sequenceType.equalsIgnoreCase("n")){
            return;
        }
        final boolean isSlimOutput = false;
        final String interProScanVersion = "5-34";

        Path outputPath = getFinalPath(stepInstance, FileOutputFormat.XML);
        Utilities.verboseLog(10, " Prepare For OutputStep - outputNTToXML: " + outputPath );

        Long bottomProteinId = stepInstance.getBottomProtein();
        Long topProteinId = stepInstance.getTopProtein();

        try (ProteinMatchesWithNucleotidesXMLJAXBFragmentsResultWriter writer = new ProteinMatchesWithNucleotidesXMLJAXBFragmentsResultWriter(outputPath, isSlimOutput)) {
            //writer.header(interProScanVersion);
            if (bottomProteinId != null && topProteinId != null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Load " + topProteinId + " proteins from the db.");
                }
                Utilities.verboseLog(10, " WriteOutputStep -XML new " + " There are " + topProteinId + " proteins.");
                int count = 0;
                writer.header(interProScanVersion);
                //final Set<NucleotideSequence> nucleotideSequences = new HashSet<>();
                for (NucleotideSequence nucleotideSequence :nucleotideSequences){
                    //writer.write(nucleotideSequence, sequenceType, isSlimOutput);
                    String xmlNucleotideSequence = writer.marshal(nucleotideSequence);
                    //writer.write(",");

                    String key = nucleotideSequence.getMd5();
                    Hibernate.initialize(nucleotideSequence);
                    nucleotideSequenceDAO.getMaximumPrimaryKey();

                    nucleotideSequenceDAO.persist(key, nucleotideSequence);
                    Utilities.verboseLog("Prepae OutPut xmlNucleotideSequence : " +  xmlNucleotideSequence);
                }

                Utilities.verboseLog("WriteOutPut nucleotideSequences size: " +  nucleotideSequences.size());

            }
            writer.close();
        }catch (JAXBException e){
            e.printStackTrace();
        }catch (XMLStreamException e) {
            e.printStackTrace();
        }

    }



    private void outputToJSON(StepInstance stepInstance, String sequenceType, Set<NucleotideSequence> nucleotideSequences) throws IOException {
        Utilities.verboseLog(10, " WriteOutputStep - outputToJSON " );

        boolean isSlimOutput = false;

        Utilities.verboseLog(10, " WriteOutputStep - outputToJSON json-slim? " + isSlimOutput);
//        try (ProteinMatchesJSONResultWriter writer = new ProteinMatchesJSONResultWriter(outputPath, isSlimOutput)) {
//            //old way??
//            //writer.write(matchesHolder, proteinDAO, sequenceType, isSlimOutput);
//
//        }
        //Try writing to JSOn from this module

        Long bottomProteinId = stepInstance.getBottomProtein();
        Long topProteinId = stepInstance.getTopProtein();

        Path outputPath = getFinalPath(stepInstance, FileOutputFormat.JSON);

        final Map<String, String> parameters = stepInstance.getParameters();
        //final boolean mapToPathway = Boolean.TRUE.toString().equals(parameters.get(MAP_TO_PATHWAY));
        //final boolean mapToGO = Boolean.TRUE.toString().equals(parameters.get(MAP_TO_GO));
        //final boolean mapToInterProEntries = mapToPathway || mapToGO || Boolean.TRUE.toString().equals(parameters.get(MAP_TO_INTERPRO_ENTRIES));
        //writer.setMapToInterProEntries(mapToInterProEntries);
        //writer.setMapToGO(mapToGO);
        // writer.setMapToPathway(mapToPathway);
        try (ProteinMatchesJSONResultWriter writer = new ProteinMatchesJSONResultWriter(outputPath, isSlimOutput)) {
            final String interProScanVersion = "5-34";
            writer.header(interProScanVersion);
            if (bottomProteinId != null && topProteinId != null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Load " + topProteinId + " proteins from the db.");
                }
                Utilities.verboseLog(10, " WriteOutputStep -JSON new " + " There are " + topProteinId + " proteins.");
                int count = 0;
                for (NucleotideSequence nucleotideSequence :nucleotideSequences){

                    writer.write(nucleotideSequence);
                    count++;
                    if (count < nucleotideSequences.size()) {
                        writer.write(","); // More proteins/nucleotide sequences to follow
                    }

                }
            }
            writer.footer();
        }

    }

    Path getFinalPath(StepInstance stepInstance, FileOutputFormat fileOutputFormat){
        final String OUTPUT_EXPLICIT_FILE_PATH_KEY = "EXPLICIT_OUTPUT_FILE_PATH";
        final Map<String, String> parameters = stepInstance.getParameters();
        final boolean explicitPath = parameters.containsKey(OUTPUT_EXPLICIT_FILE_PATH_KEY);

        final String OUTPUT_FILE_FORMATS = "OUTPUT_FORMATS";

        final String outputFormatStr = parameters.get(OUTPUT_FILE_FORMATS);
        final Set<FileOutputFormat> outputFormats = FileOutputFormat.stringToFileOutputFormats(outputFormatStr);

        final String OUTPUT_FILE_PATH_KEY = "OUTPUT_PATH";
        String filePathName = (explicitPath)
                ? parameters.get(OUTPUT_EXPLICIT_FILE_PATH_KEY)
                : parameters.get(OUTPUT_FILE_PATH_KEY);

        filePathName = filePathName + ".initial.tmp";

        Path outputPath = getPathName(explicitPath, filePathName, fileOutputFormat);
        return outputPath;
    }

    private Path getPathName(final boolean explicitPath,
                             final String filePathName,
                             final FileOutputFormat outputFormat) {
        // E.g. for "-b OUT" filePathName = "~/Projects/github-i5/interproscan/core/jms-implementation/target/interproscan-5-dist/OUT"
        Path outputPath = null;
        boolean archiveSVGOutput = false;  //TEMP

        if (explicitPath) {
            outputPath = Paths.get(filePathName);
            if (Files.exists(outputPath)) {
                try {
                    Files.delete(outputPath);
                } catch (IOException e) {
                    final String p = outputPath.toAbsolutePath().toString();
                    System.out.println("Unable to overwrite file " + p + ".  Please check file permissions.");
                    System.exit(101);
                }
            }
        } else {
            // Try to use the file name provided. If the file already exists, append a bracketed number (Chrome style).
            // but using an underscore rather than a space (pah!)
            Integer counter = null;
            int ioCounter = 0;
            boolean pathAvailable = false;
            while (!pathAvailable) {
                final StringBuilder candidateFileName = new StringBuilder(filePathName);
                if (counter == null) {
                    counter = 1;
                } else {
                    // E.g. Output file name could become "test_proteins.fasta_1.tsv"
                    candidateFileName
                            .append('_')
                            .append(counter++);
                }
                candidateFileName
                        .append('.')
                        .append(outputFormat.getFileExtension());
                //Extend file name by tar (tar.gz) extension if HTML or SVG
                if (outputFormat.equals(FileOutputFormat.HTML) || outputFormat.equals(FileOutputFormat.SVG)) {
                    //outputPath = Paths.get(TarArchiveBuilder.buildTarArchiveName(candidateFileName.toString(), archiveSVGOutput, compressHtmlAndSVGOutput, outputFormat));
                } else {
                    outputPath = Paths.get(candidateFileName.toString());
                }
                pathAvailable = !Files.exists(outputPath);
                if (pathAvailable) {
                    try {
                        // Start creating the empty output file now, while the path is still available
                        if (outputFormat.equals(FileOutputFormat.SVG) && !archiveSVGOutput) {
                            outputPath = Files.createDirectories(outputPath);
                        }
                        else {
                            outputPath = Files.createFile(outputPath);
                        }
                    } catch (IOException e) {
                        pathAvailable = false; // Nope, that path has probably just been taken (e.g. by another copy of InterProScan writing to the same output directory)
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Path " + candidateFileName.toString() + " was available for writing to, but I/O exception thrown");
                        }
                        ioCounter++;
                        if (ioCounter > 2000) {
                            // Stop possible infinite loop!
                            throw new IllegalStateException("Path " + candidateFileName.toString() + " was available, but I/O exception thrown on file creation");
                        }
                    }
                }
            }

        }
        return outputPath;
    }

    public void updateMatch(Match match){
        Entry matchEntry = match.getSignature().getEntry();
        if(matchEntry!= null) {
            //check goterms
            //check pathways
            matchEntry.getGoXRefs();
            if (matchEntry.getGoXRefs() != null) {
                matchEntry.getGoXRefs().size();
            }
            matchEntry.getPathwayXRefs();
            if (matchEntry.getPathwayXRefs() != null) {
                matchEntry.getPathwayXRefs().size();
            }
        }
    }

    private void deleteTmpMarshallingFile(Path outputPath){
        final String outputFilePathName = outputPath.toAbsolutePath().toString();
        LOGGER.warn("Deleting temp xml file:  " + outputFilePathName);
        File file = new File(outputFilePathName);
        if (file.exists()) {
            if (!file.delete()) {
                LOGGER.error("Unable to delete the file located at " + outputFilePathName);
                throw new IllegalStateException("Unable to delete the file located at " + outputFilePathName);
            }
        }else{
            LOGGER.warn("File not found, file located at " + outputFilePathName);
        }
    }


}