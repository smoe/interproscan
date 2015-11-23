package uk.ac.ebi.interpro.scan.persistence;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.interpro.scan.model.CDDMatch;
import uk.ac.ebi.interpro.scan.model.Protein;
import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.raw.CDDRawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;

import java.util.Collection;
import java.util.Map;

/**
* CDD CRUD database operations.
*
* @author Matthew Fraser, EMBL-EBI, InterPro
* @version $Id$
* @since 1.0-SNAPSHOT
*/
public interface CDDFilteredMatchDAO extends FilteredMatchDAO<CDDRawMatch, CDDMatch> {

    /**
     * Persists a set of parsed CDD match objects as filtered matches.
     * There is no filtering step required.
     *
     *
     */
    @Transactional
    void persist(Collection<RawProtein<CDDRawMatch>> filteredProteins, Map<String, Signature> modelIdToSignatureMap, Map<String, Protein> proteinIdToProteinMap);

}
