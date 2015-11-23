package uk.ac.ebi.interpro.scan.management.model.implementations.cdd;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.io.cdd.match.CDDMatchParser;
import uk.ac.ebi.interpro.scan.management.model.Step;
import uk.ac.ebi.interpro.scan.management.model.StepInstance;
import uk.ac.ebi.interpro.scan.model.raw.CDDRawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;
import uk.ac.ebi.interpro.scan.persistence.CDDFilteredMatchDAO;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * This step parses the output from the CDD Perl script and then persists the matches.
 * No post processing (match filtering) required.
 *
 * @author Matthew Fraser
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class ParseAndPersistCDDOutputStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(ParseAndPersistCDDOutputStep.class.getName());

    private String cddBinaryOutputFileName;

    private CDDMatchParser parser;

    private CDDFilteredMatchDAO rawMatchDAO;

    @Required
    public void setCDDBinaryOutputFileName(String cddBinaryOutputFileName) {
        this.cddBinaryOutputFileName = cddBinaryOutputFileName;
    }

    @Required
    public void setParser(CDDMatchParser parser) {
        this.parser = parser;
    }

    @Required
    public void setRawMatchDAO(CDDFilteredMatchDAO rawMatchDAO) {
        this.rawMatchDAO = rawMatchDAO;
    }

    /**
     * Parse the output file from the CDD binary and persist the results in the database.
     *
     * @param stepInstance           containing the parameters for executing. Provides utility methods as described
     * above.
     * @param temporaryFileDirectory which can be passed into the
     * stepInstance.buildFullyQualifiedFilePath(String temporaryFileDirectory, String fileNameTemplate) method
     */
    public void execute(StepInstance stepInstance, String temporaryFileDirectory) {

        // Retrieve raw matches from the CDD binary output file
        InputStream is = null;
        final String fileName = stepInstance.buildFullyQualifiedFilePath(temporaryFileDirectory, cddBinaryOutputFileName);
        Set<RawProtein<CDDRawMatch>> rawProteins;
        try {
            is = new FileInputStream(fileName);
            rawProteins = parser.parse(is);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parsed out " + rawProteins.size() + " proteins with matches from file " + fileName);
                int count = 0;
                for (RawProtein<CDDRawMatch> rawProtein : rawProteins) {
                    count += rawProtein.getMatches().size();
                }
                LOGGER.debug("A total of " + count + " matches from file " + fileName);
            }
            // NOTE: No post processing therefore no need to store the raw results here - we will just persist them to
            // the database later on...
        } catch (IOException e) {
            throw new IllegalStateException("IOException thrown when attempting to parse " + fileName, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error closing input stream", e);
            }
        }

        if (rawProteins != null && rawProteins.size() > 0) {
            // Persist the matches
            rawMatchDAO.persist(rawProteins);
        }
        else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No CDD matches were persisted as none were found in the CDD binary output file: " + fileName);
            }
        }


    }
}
