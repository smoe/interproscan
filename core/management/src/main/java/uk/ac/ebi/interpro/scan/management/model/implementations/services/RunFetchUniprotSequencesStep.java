package uk.ac.ebi.interpro.scan.management.model.implementations.services;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;
import uk.ac.ebi.interpro.scan.management.model.implementations.RunBinaryStep;
import uk.ac.ebi.interpro.scan.util.Utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the MobiDB binary on the fasta file provided to the output file provided.
 *
 * @author Gift Nuka
 * @version $Id$
 * @since 1.0
 */

public class RunFetchUniprotSequencesStep extends RunBinaryStep {

    private static final Logger LOGGER = Logger.getLogger(RunFetchUniprotSequencesStep.class.getName());

    private String fullPathToPython;

    private String fullPathToBinary;


    private String fastaFileNameTemplate;

    public static final String ID_FILE_PATH_KEY = "id.file.path";

    @Required
    public void setFullPathToBinary(String fullPathToBinary) {
        this.fullPathToBinary = fullPathToBinary;
    }

    @Required
    public void setFastaFileNameTemplate(String fastaFileNameTemplate) {
        this.fastaFileNameTemplate = fastaFileNameTemplate;
    }

    public String getFullPathToPython() {
        return fullPathToPython;
    }

    public void setFullPathToPython(String fullPathToPython) {
        this.fullPathToPython = fullPathToPython;
    }

    public String getFastaFileNameTemplate() {
        return fastaFileNameTemplate;
    }

    @Override
    protected List<String> createCommand(StepInstance stepInstance, String temporaryFileDirectory) {
        final Map<String, String> parameters = stepInstance.getParameters();
        final List<String> command = new ArrayList<String>();
        final String fastaFilePath
                = stepInstance.buildFullyQualifiedFilePath(temporaryFileDirectory, this.getFastaFileNameTemplate());
        final String outputFileName = stepInstance.buildFullyQualifiedFilePath(temporaryFileDirectory, getOutputFileNameTemplate());

        final String idFilePath = parameters.get(ID_FILE_PATH_KEY);


        if(getFullPathToPython() != null  && this.getFullPathToPython().trim().isEmpty()){
            command.add("python3");
        }else{
            command.add(this.getFullPathToPython());
        }

        command.add(fullPathToBinary);
        command.add(idFilePath);
        command.add(outputFileName);

        Utilities.verboseLog("panther command is :" + command.toString() + " isUsesFileOutputSwitch(): " + isUsesFileOutputSwitch());
        return command;
    }
}
