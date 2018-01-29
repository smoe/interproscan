package uk.ac.ebi.interpro.scan.business.postprocessing.pfam_A;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.business.postprocessing.pfam_A.model.PfamClan;
import uk.ac.ebi.interpro.scan.business.postprocessing.pfam_A.model.PfamClanData;
import uk.ac.ebi.interpro.scan.business.postprocessing.pfam_A.model.PfamModel;
import uk.ac.ebi.interpro.scan.model.Hmmer3Match;
import uk.ac.ebi.interpro.scan.model.LocationFragment;
import uk.ac.ebi.interpro.scan.model.raw.PfamHmmer3RawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawMatch;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;
import uk.ac.ebi.interpro.scan.util.Utilities;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * This class performs post processing of HMMER3 output for
 * Pfam.
 *
 * @author Phil Jones
 * @version $Id: PfamHMMER3PostProcessing.java,v 1.10 2009/11/09 13:35:50 craigm Exp $
 * @since 1.0
 */
public class PfamHMMER3PostProcessing implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PfamHMMER3PostProcessing.class.getName());

    private PfamClanData clanData;

    private ClanFileParser clanFileParser;

    private SeedAlignmentDataRetriever seedAlignmentDataRetriever;

    @Required
    public void setClanFileParser(ClanFileParser clanFileParser) {
        this.clanFileParser = clanFileParser;
    }

    /**
     * TODO: Will eventually be 'required', but not till after milestone one.
     *
     * @param seedAlignmentDataRetriever to retrieve seed alignment data for
     *                                   a range of proteins.
     */
    public void setSeedAlignmentDataRetriever(SeedAlignmentDataRetriever seedAlignmentDataRetriever) {
        this.seedAlignmentDataRetriever = seedAlignmentDataRetriever;
    }


    /**
     * Post-processes raw results for Pfam HMMER3 in the batch requested.
     *
     * @param proteinIdToRawMatchMap being a Map of protein IDs to a List of raw matches
     * @return a Map of proteinIds to a List of filtered matches.
     */
    public Map<String, RawProtein<PfamHmmer3RawMatch>> process(Map<String, RawProtein<PfamHmmer3RawMatch>> proteinIdToRawMatchMap) throws IOException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Pfam A Post Processing: Number of proteins being considered: " + ((proteinIdToRawMatchMap == null) ? 0 : proteinIdToRawMatchMap.size()));
        }
        if (clanData == null) {
            clanData = clanFileParser.getClanData();
        }
        final Map<String, RawProtein<PfamHmmer3RawMatch>> proteinIdToRawProteinMap = new HashMap<String, RawProtein<PfamHmmer3RawMatch>>();
        if (proteinIdToRawMatchMap == null) {
            return proteinIdToRawProteinMap;
        }
        long startNanos = System.nanoTime();
        // Iterate over UniParc IDs in range and processBatch them
        SeedAlignmentDataRetriever.SeedAlignmentData seedAlignmentData = null;
        if (seedAlignmentDataRetriever != null) {
            seedAlignmentData = seedAlignmentDataRetriever.retrieveSeedAlignmentData(proteinIdToRawMatchMap.keySet());
        }

        for (String proteinId : proteinIdToRawMatchMap.keySet()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Pfam A post processing: processing protein " + proteinId);
            }
            List<SeedAlignment> seedAlignments = null;
            if (seedAlignmentData != null) {
                seedAlignments = seedAlignmentData.getSeedAlignments(proteinId);
            }
            proteinIdToRawProteinMap.put(proteinId, processProtein(proteinIdToRawMatchMap.get(proteinId), seedAlignments));
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(new StringBuilder().append("Batch containing").append(proteinIdToRawMatchMap.size()).append(" proteins took ").append(((double) (System.nanoTime() - startNanos)) / 1.0e9d).append(" s to run.").toString());
        }
        return proteinIdToRawProteinMap;
    }

    /**
     * Implementation of Rob Finn's algorithm for post processing, translated from Perl to Java.
     * <p/>
     * Also includes additional code to ensure seed alignments are included as matches, regardless of
     * score.
     *
     * @param rawProteinUnfiltered being a List of the raw matches to filter
     * @param seedAlignments       being a Collection of SeedAlignment objects, to check for matches to
     *                             methods where this protein was part of the seed alignment.
     * @return a List of filtered matches.
     */
    private RawProtein processProtein(final RawProtein<PfamHmmer3RawMatch> rawProteinUnfiltered, final List<SeedAlignment> seedAlignments) {
        RawProtein<PfamHmmer3RawMatch> filteredMatches = new RawProtein<PfamHmmer3RawMatch>(rawProteinUnfiltered.getProteinIdentifier());
        RawProtein<PfamHmmer3RawMatch> filteredRawProtein = new RawProtein<PfamHmmer3RawMatch>(rawProteinUnfiltered.getProteinIdentifier());

        // First of all, place any rawProteinUnfiltered to methods for which this protein was a seed
        // into the filteredMatches collection.
        final Set<PfamHmmer3RawMatch> seedMatches = new HashSet<PfamHmmer3RawMatch>();

        if (seedAlignments != null) {        // TODO This check can be removed, once the seed alignment stuff has been sorted.
            for (final SeedAlignment seedAlignment : seedAlignments) {
                for (final PfamHmmer3RawMatch candidateMatch : rawProteinUnfiltered.getMatches()) {
                    if (!seedMatches.contains(candidateMatch)) {
                        if (seedAlignment.getModelAccession().equals(candidateMatch.getModelId()) &&
                                seedAlignment.getAlignmentStart() <= candidateMatch.getLocationStart() &&
                                seedAlignment.getAlignmentEnd() >= candidateMatch.getLocationEnd()) {
                            // Found a match to a seed, where the coordinates fall within the seed alignment.
                            // Add it directly to the filtered rawProteinUnfiltered...
                            Utilities.verboseLog("found match to a seed - candidateMatch and seedMatch: " + candidateMatch);
                            filteredMatches.addMatch(candidateMatch);
                            seedMatches.add(candidateMatch);
                        }
                    }
                }
            }
        }

        // Then iterate over the non-seed raw rawProteinUnfiltered, sorted in order ievalue ASC score DESC
        final Set<PfamHmmer3RawMatch> unfilteredByEvalue = new TreeSet<PfamHmmer3RawMatch>(rawProteinUnfiltered.getMatches());

        for (final RawMatch rawMatch : unfilteredByEvalue) {
            final PfamHmmer3RawMatch candidateMatch = (PfamHmmer3RawMatch) rawMatch;
            Utilities.verboseLog("consider match - candidateMatch: " + candidateMatch);
            if (!seedMatches.contains(candidateMatch)) {
                final PfamClan candidateMatchClan = clanData.getClanByModelAccession(candidateMatch.getModelId());

                boolean passes = true;   // Optimistic algorithm!

                Utilities.verboseLog("candidateMatchClan: " + candidateMatchClan);
                if (candidateMatchClan != null) {
                    // Iterate over the filtered rawProteinUnfiltered (so far) to check for passes
                    for (final PfamHmmer3RawMatch match : filteredMatches.getMatches()) {
                        final PfamClan passedMatchClan = clanData.getClanByModelAccession(match.getModelId());
                        // Are both the candidate and the passedMatch in the same clan?
                        if (candidateMatchClan.equals(passedMatchClan)) {
                            // Both in the same clan, so check for overlap.  If they overlap
                            // and are NOT nested, then set passes to false and break out of the inner for loop.
                            if (matchesOverlap(candidateMatch, match)) {
                                if (!matchesAreNested(candidateMatch, match)) {
                                    passes = false;
                                    break;  // out of loop over filtered rawProteinUnfiltered.
                                }else{
                                    Utilities.verboseLog("nested match: candidateMatch - " + candidateMatch
                                            + " other match:- " +   match );
                                }
                            }
                        }
                    }
                }

                if (passes) {
                    // Add filtered match to collection
                    filteredMatches.addMatch(candidateMatch);
                }
            }
        }
        for (PfamHmmer3RawMatch pfamHmmer3RawMatch:filteredMatches.getMatches()){
            if (pfamHmmer3RawMatch.getModelId() == "PF01479"){
                PfamHmmer3RawMatch pfMatch = getTempPfamHmmer3RawMatch(pfamHmmer3RawMatch,280,298);
                filteredMatches.addMatch(pfMatch);
            }
        }
        Map<String,List<String>> nestedModelsMap = new HashMap<>();
        List<String> nestedInModels = new ArrayList<>();
        nestedInModels.add("PF01000");
        //nestedInModels.add("PF01479");

        nestedModelsMap.put("PF01193", nestedInModels);

        for (PfamHmmer3RawMatch pfamHmmer3RawMatch:filteredMatches.getMatches()){
            String modelId = pfamHmmer3RawMatch.getModelId();
            Utilities.verboseLog("ModelId to consider: " + modelId + " region: [" +
                    pfamHmmer3RawMatch.getLocationStart() + "-" + pfamHmmer3RawMatch.getLocationEnd() + "]");

            List<String> nestedModels = nestedModelsMap.get(modelId);
            if (nestedModels != null){
                final UUID splitGroup = UUID.randomUUID();
                pfamHmmer3RawMatch.setSplitGroup(splitGroup);
                //get new regions
                List<Hmmer3Match.Hmmer3Location.Hmmer3LocationFragment> locationFragments = new ArrayList<>();
                for (PfamHmmer3RawMatch rawMatch: filteredMatches.getMatches()){
                    if (nestedModels.contains(rawMatch.getModelId())) {
                        locationFragments.add(new Hmmer3Match.Hmmer3Location.Hmmer3LocationFragment(
                                rawMatch.getLocationStart(), rawMatch.getLocationEnd()));
                    }
                }
                locationFragments.add(new Hmmer3Match.Hmmer3Location.Hmmer3LocationFragment(
                        380, 395));
                //sort these according to the start and stop positions
                Collections.sort(locationFragments);
                int newLocationStart = pfamHmmer3RawMatch.getLocationStart();
                int newLocationEnd = pfamHmmer3RawMatch.getLocationEnd();
                int finalLocationEnd = pfamHmmer3RawMatch.getLocationEnd();
                for (Hmmer3Match.Hmmer3Location.Hmmer3LocationFragment fragment: locationFragments){
                    Utilities.verboseLog("region to consider: " + fragment.toString());
                    newLocationEnd = fragment.getStart() - 1;
                    Utilities.verboseLog("New Region: " + newLocationStart + "-" + newLocationEnd);
                    PfamHmmer3RawMatch pfMatch = getTempPfamHmmer3RawMatch(pfamHmmer3RawMatch,newLocationStart,newLocationEnd);
                    pfMatch.setSplitGroup(splitGroup);
                    filteredRawProtein.addMatch(pfMatch);
                    newLocationStart = fragment.getEnd() + 1;
                }
                //deal with final region
                Utilities.verboseLog("The Last new Region: " + newLocationStart + "-" + finalLocationEnd);
                PfamHmmer3RawMatch pfMatch = getTempPfamHmmer3RawMatch(pfamHmmer3RawMatch,newLocationStart,finalLocationEnd);
                pfMatch.setSplitGroup(splitGroup);
                filteredRawProtein.addMatch(pfMatch);
                //resolve the location frgaments
            }else{
                filteredRawProtein.addMatch(pfamHmmer3RawMatch);
            }
        }
//        return filteredMatches;
        return filteredRawProtein;
    }

    /**
     * Determines if two domains overlap.
     *
     * @param one domain match one.
     * @param two domain match two.
     * @return true if the two domain matches overlap.
     */
    boolean matchesOverlap(PfamHmmer3RawMatch one, PfamHmmer3RawMatch two) {
        return !
                ((one.getLocationStart() > two.getLocationEnd()) ||
                        (two.getLocationStart() > one.getLocationEnd()));
    }

    /**
     * Determines if two Pfam families are nested (in either direction)
     *
     * @param one domain match one.
     * @param two domain match two.
     * @return true if the two domain matches are nested.
     */
    boolean matchesAreNested(PfamHmmer3RawMatch one, PfamHmmer3RawMatch two) {
        PfamModel oneModel = clanData.getModelByModelAccession(one.getModelId());
        PfamModel twoModel = clanData.getModelByModelAccession(two.getModelId());

        return !(oneModel == null || twoModel == null) &&
                (oneModel.isNestedIn(twoModel) || twoModel.isNestedIn(oneModel));

    }

    private PfamHmmer3RawMatch getTempPfamHmmer3RawMatch(PfamHmmer3RawMatch rawMatch, int start, int end){
        final PfamHmmer3RawMatch match = new PfamHmmer3RawMatch(
                rawMatch.getSequenceIdentifier(),
                rawMatch.getModelId(),
                rawMatch.getSignatureLibrary(),
                rawMatch.getSignatureLibraryRelease(),
                start,
                end,
                rawMatch.getEvalue(),
                rawMatch.getScore(),
                rawMatch.getHmmStart(),
                rawMatch.getHmmEnd(),
                rawMatch.getHmmBounds(),
                rawMatch.getScore(),
                rawMatch.getEnvelopeStart(),
                rawMatch.getEnvelopeEnd(),
                rawMatch.getExpectedAccuracy(),
                rawMatch.getFullSequenceBias(),
                rawMatch.getDomainCeValue(),
                rawMatch.getDomainIeValue(),
                rawMatch.getDomainBias()
        );

        return match;
    }
}
