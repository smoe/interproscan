package uk.ac.ebi.interpro.scan.io.match.coils;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.junit.Test;
import uk.ac.ebi.interpro.scan.model.raw.CoilsRawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Tests the CoilsMatchParser, specifically looking at memory usage.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0
 */
public class CoilsMatchParserTest extends TestCase {

    private static final Logger LOGGER = Logger.getLogger(CoilsMatchParserTest.class.getName());

    private static final String TEST_FILE_PATH = "data/coils/10k.coils.out";

    /**
     * Parses a (largish) file and outputs memory usage at the end of the parse.
     */
    @Test
    public void testParserEfficiency() throws IOException {
        logMemUsage("Before parse: ");
        CoilsMatchParser parser = new CoilsMatchParser();
        try(InputStream is = CoilsMatchParserTest.class.getClassLoader().getResourceAsStream(TEST_FILE_PATH)) {
            Set<RawProtein<CoilsRawMatch>> results = parser.parse(is);
            logMemUsage("After parse: ");
            assertNotNull(results);
            assertEquals(1146, results.size());
            LOGGER.debug("Result count: " + results.size());
        }
    }

    private void logMemUsage(String prefix) {
        if (LOGGER.isDebugEnabled()) {
            System.gc();
            LOGGER.debug(prefix + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + " MB.");
        }
    }
}
