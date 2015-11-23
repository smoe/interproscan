package uk.ac.ebi.interpro.scan.io.cdd;

import org.apache.log4j.Logger;
import uk.ac.ebi.interpro.scan.io.tmhmm.TMHMMProtein;
import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.SignatureLibraryRelease;
import uk.ac.ebi.interpro.scan.model.TMHMMMatch;
import uk.ac.ebi.interpro.scan.model.TMHMMSignature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for TMHMM 2.0c (Prediction of transmembrane helices in proteins) raw result output.
 * <br/>If you run TMHMM 2.0c binary file (decodeanhmm) with the following parameters:
 * -N 1
 * -PrintNumbers
 * -background '0.081 0.015 0.054 ...'
 * <p/>
 * you get the following result output for 2 specified protein sequences:
 * >1
 * %pred N0: O 1 199, M 200 222, i 223 233, M 234 256, o 257 270
 * ?0 OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO
 * <p/>
 * ?0 OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO OOOOOOOOOO
 * <p/>
 * >2
 * %pred N0: o 1 19, M 20 42, i 43 61, M 62 84, o 85 200
 * ?0 oooooooooo oooooooooM MMMMMMMMMM MMMMMMMMMM MMiiiiiiii iiiiiiiiii
 * <p/>
 * ?0 iMMMMMMMMM MMMMMMMMMM MMMMoooooo oooooooooo oooooooooo oooooooooo
 * <p/>
 * The main method of this parser returns a set of protein matches, which represent trans membrane regions.
 *
 * @author Maxim Scheremetjew, EMBL-EBI, InterPro
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public final class CDDRawResultParser {





}
