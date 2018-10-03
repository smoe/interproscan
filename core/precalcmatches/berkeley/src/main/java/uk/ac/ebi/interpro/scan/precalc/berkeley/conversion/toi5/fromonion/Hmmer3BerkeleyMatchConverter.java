package uk.ac.ebi.interpro.scan.precalc.berkeley.conversion.toi5.fromonion;

import org.apache.log4j.Logger;
import uk.ac.ebi.interpro.scan.model.HmmBounds;
import uk.ac.ebi.interpro.scan.model.Hmmer3Match;
import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.DCStatus;
import uk.ac.ebi.interpro.scan.precalc.berkeley.conversion.toi5.BerkeleyMatchConverter;
import uk.ac.ebi.interpro.scan.precalc.berkeley.model.BerkeleyLocation;
import uk.ac.ebi.interpro.scan.precalc.berkeley.model.BerkeleyMatch;

import java.util.HashSet;
import java.util.Set;

/**
 * Converts a BerkeleyMatch to a HMMER3 Match.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class Hmmer3BerkeleyMatchConverter extends BerkeleyMatchConverter<Hmmer3Match> {

    private static final Logger LOG = Logger.getLogger(Hmmer3BerkeleyMatchConverter.class.getName());

    public Hmmer3Match convertMatch(BerkeleyMatch match, Signature signature) {

        final String sln = match.getSignatureLibraryName();
        boolean postProcessed = false;
        if (sln.equalsIgnoreCase("GENE3D") || sln.equalsIgnoreCase("PFAM")) {
            postProcessed = true;
        }

        final Set<Hmmer3Match.Hmmer3Location> locations = new HashSet<>(match.getLocations().size());

        for (BerkeleyLocation location : match.getLocations()) {

            int locationStart = valueOrZero(location.getStart());
            int locationEnd = valueOrZero(location.getEnd());

            int envStart = location.getEnvelopeStart() == null
                    ? locationStart
                    : location.getEnvelopeStart();
            int envEnd =  location.getEnvelopeEnd() == null
                    ? locationEnd
                    : location.getEnvelopeEnd();

            String locationFragmentsStr = location.getLocationFragments();
            if (locationFragmentsStr == null || locationFragmentsStr.isEmpty()) {
                locationFragmentsStr = locationStart + "-" + locationEnd + "-S";
            }
            Set<Hmmer3Match.Hmmer3Location.Hmmer3LocationFragment> locationFragments = new HashSet<>();
            for (String locationFragmentStr : locationFragmentsStr.split(",")) {
                String[] str = locationFragmentStr.trim().split("-");
                if (str.length != 3) {
                    throw new IllegalStateException("Location fragment " + locationFragmentsStr + " not correct format (e.g. '10-20-S,30-40-S'");
                }
                Integer i1 = Integer.parseInt(str[0]);
                Integer i2 = Integer.parseInt(str[1]);
                DCStatus dc = DCStatus.parseSymbol(str[2]);
                locationFragments.add(new Hmmer3Match.Hmmer3Location.Hmmer3LocationFragment(i1, i2, dc));
            }

            final HmmBounds bounds = HmmBounds.parseSymbol(HmmBounds.calculateHmmBounds(envStart, envEnd, locationStart, locationEnd));

            locations.add(new Hmmer3Match.Hmmer3Location(
                    locationStart,
                    locationEnd,
                    valueOrZero(location.getScore()),
                    valueOrZero(location.geteValue()),
                    valueOrZero(location.getHmmStart()),
                    valueOrZero(location.getHmmEnd()),
                    valueOrZero(location.getHmmLength()),
                    bounds,
                    envStart,
                    envEnd,
                    postProcessed,
                    locationFragments
            ));
        }

        return new Hmmer3Match(
                signature,
                match.getSignatureModels(),
                valueOrZero(match.getSequenceScore()),
                valueOrZero(match.getSequenceEValue()),
                locations
        );
    }

}
