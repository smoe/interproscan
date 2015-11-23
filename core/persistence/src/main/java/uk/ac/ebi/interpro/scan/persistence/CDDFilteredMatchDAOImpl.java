package uk.ac.ebi.interpro.scan.persistence;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.interpro.scan.model.CDDMatch;
import uk.ac.ebi.interpro.scan.model.Protein;
import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.SignatureLibraryRelease;
import uk.ac.ebi.interpro.scan.model.raw.CDDRawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CDD CRUD database operations.
 *
 * @author Matthew Fraser, EMBL-EBI, InterPro
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class CDDFilteredMatchDAOImpl extends FilteredMatchDAOImpl<CDDRawMatch, CDDMatch> implements CDDFilteredMatchDAO {

    private final String cddReleaseVersion;

    /**
     * Sets the class of the model that the DAO instance handles.
     * Note that this has been set up to use constructor injection
     * because it makes it easy to sub-class GenericDAOImpl in a robust
     * manner.
     * <p/>
     * Model class specific sub-classes should define a no-argument constructor
     * that calls this constructor with the appropriate class.
     */
    public CDDFilteredMatchDAOImpl(String version) {
        super(CDDMatch.class);
        this.cddReleaseVersion = version;
    }


    /**
     * This is the method that should be implemented by specific FilteredMatchDAOImpl's to
     * persist filtered matches.
     *
     * @param filteredProteins      being the Collection of filtered RawProtein objects to persist
     * @param modelIdToSignatureMap a Map of model IDs to Signature objects.
     * @param proteinIdToProteinMap a Map of Protein IDs to Protein objects
     */
    @Transactional
    public void persist(Collection<RawProtein<CDDRawMatch>> filteredProteins, Map<String, Signature> modelIdToSignatureMap, Map<String, Protein> proteinIdToProteinMap) {
        for (RawProtein<CDDRawMatch> rawProtein : filteredProteins) {
            Protein protein = proteinIdToProteinMap.get(rawProtein.getProteinIdentifier());
            if (protein == null) {
                throw new IllegalStateException("Cannot store match to a protein that is not in database " +
                        "[protein ID= " + rawProtein.getProteinIdentifier() + "]");
            }
            Set<CDDMatch.CDDLocation> locations = null;
            String currentModelId = null;
            Signature currentSignature = null;
            CDDRawMatch lastRawMatch = null;
            CDDMatch match = null;
            for (CDDRawMatch rawMatch : rawProtein.getMatches()) {
                if (rawMatch == null) {
                    continue;
                }

                if (currentModelId == null || !currentModelId.equals(rawMatch.getModelId())) {
                    if (currentModelId != null) {

                        // Not the first (because the currentSignatureAc is not null)
                        if (match != null) {
                            entityManager.persist(match); // Persist the previous one...
                        }
                        match = new CDDMatch(currentSignature, locations);
                        // Not the first...
                        protein.addMatch(match);
                    }
                    // Reset everything
                    locations = new HashSet<CDDMatch.CDDLocation>();
                    currentModelId = rawMatch.getModelId();
                    currentSignature = modelIdToSignatureMap.get(currentModelId);
                    if (currentSignature == null) {
                        throw new IllegalStateException("Cannot find CDD model " + currentModelId + " in the database.");
                    }
                }
                locations.add(
                        new CDDMatch.CDDLocation(
                                rawMatch.getLocationStart(),
                                rawMatch.getLocationEnd(),
                                rawMatch.getScore(),
                                rawMatch.getEvalue()
                        )
                );
                lastRawMatch = rawMatch;
            }
            // Don't forget the last one!
            if (lastRawMatch != null) {
                match = new CDDMatch(currentSignature, locations);
                protein.addMatch(match);
                entityManager.persist(match);
            }
        }
    }
}
