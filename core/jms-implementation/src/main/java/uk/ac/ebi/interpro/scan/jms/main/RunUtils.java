package uk.ac.ebi.interpro.scan.jms.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import uk.ac.ebi.interpro.scan.io.FileOutputFormat;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that provides static methods to assist the {@link uk.ac.ebi.interpro.scan.jms.main.Run} class.
 */
public final class RunUtils {

    private static final Logger LOGGER = Logger.getLogger(RunUtils.class.getName());

    private static final Mode DEFAULT_MODE = Mode.STANDALONE;

    private RunUtils() {}

    /**
     * Get the mode supplied on the command line, or return the default if nothing specified.
     * @param parsedCommandLine Command line
     * @return The mode
     * @throws org.apache.commons.cli.ParseException If the mode could not be parsed
     */
    public static Mode getMode(final CommandLine parsedCommandLine) throws ParseException {

        String modeArgument = parsedCommandLine.getOptionValue(I5Option.MODE.getLongOpt());
        Mode mode = null;
        try {
            mode = (modeArgument != null)
                    ? Mode.valueOf(modeArgument.toUpperCase())
                    : DEFAULT_MODE;
        } catch (IllegalArgumentException iae) {
            LOGGER.fatal("The mode '" + modeArgument + "' is not handled.  Should be one of: " + Mode.getCommaSepModeList());
            System.exit(1);
        }
        return mode;
    }

    // TODO Return SequenceType enum?
    public static String getSequenceType(final CommandLine parsedCommandLine) {
        String sequenceType = "p"; // default
        if (parsedCommandLine.hasOption(I5Option.SEQUENCE_TYPE.getLongOpt())) {
            sequenceType = parsedCommandLine.getOptionValue(I5Option.SEQUENCE_TYPE.getLongOpt()).toLowerCase();

            // Check the sequence type is "n" or "p"
            final Set<String> sequenceTypes = SequenceType.getSequenceTypes();
            if (sequenceTypes != null && !sequenceTypes.contains(sequenceType)) {
                System.out.print("\n\nThe specified sequence type " + sequenceType + " was not recognised, expected: ");
                StringBuilder expectedSeqTypes = new StringBuilder();
                for (String seqType : sequenceTypes) {
                    if (expectedSeqTypes.length() > 0) {
                        expectedSeqTypes.append(",");
                    }
                    expectedSeqTypes.append(seqType);
                }
                System.out.println(expectedSeqTypes + "\n\n");
                System.exit(1);
            }
        }
        return sequenceType;
    }

    public static String[] getOutputFormats(final CommandLine parsedCommandLine,
                                            final Mode mode) {
        String[] parsedOutputFormats = null;
        if (parsedCommandLine.hasOption(I5Option.OUTPUT_FORMATS.getLongOpt())) {
            parsedOutputFormats = parsedCommandLine.getOptionValues(I5Option.OUTPUT_FORMATS.getLongOpt());
            parsedOutputFormats = tidyOptionsArray(parsedOutputFormats);

            // Validate the output formats supplied
            // TODO With org.apache.commons.cli v2 could use EnumValidator instead, but currently we use cli v1.2
            if (parsedOutputFormats != null && parsedOutputFormats.length > 0) {
                // The user manually specified at least one output format, now check it's OK
                for (String outputFormat : parsedOutputFormats) {
                    if (!FileOutputFormat.isExtensionValid(outputFormat)) {
                        System.out.println("\n\n" + "The specified output file format " + outputFormat + " was not recognised." + "\n\n");
                        System.exit(1);
                    } else if (!mode.equals(Mode.CONVERT) && outputFormat.equalsIgnoreCase("raw")) {
                        // RAW output (InterProScan 4 TSV output) is only allowed in CONVERT mode
                        System.out.println("\n\n" + "The specified output file format " + outputFormat + " is only supported in " + Mode.CONVERT.name() + " mode." + "\n\n");
                        System.exit(1);
                    }
                }
            }
        }
        return parsedOutputFormats;
    }




    /**
     * Tidy an array of options for a command line option that takes multiple values.
     * For example { "Pfam,", "Gene3d,SMART", ",", ",test" } becomes { "Pfam", "Gene3d", "SMART", "test" }.
     * The validity of the options are not checked here.
     *
     * @param options Un-tidy array of options
     * @return Array of options after tidying.
     */
    public static String[] tidyOptionsArray(String[] options) {
        if (options == null || options.length < 1) {
            return options;
        }

        Set<String> parsedOptions = new HashSet<String>();

        // Examples of un-tidy options arrays:
        // 1. Commons.cli stores "-appl Pfam -appl Gene3d" as an array with 2 items { "Pfam", "Gene3d" }
        // 2. Commons.cli stores "-appl Pfam,Gene3d" as array with 1 item { "Pfam,Gene3d" }
        // 3. The I5 code below also allows something like "-appl Pfam, Gene3d,SMART, , ,test" which comes through as an
        //    array with 4 items { "Pfam,", "Gene3d,SMART", ",", ",test" } and needs to be tidied.
        for (String optionsArrayItem : options) {
            String[] optionsArrayItems = optionsArrayItem.split("[,\\s+]+");
            for (String option : optionsArrayItems) {
                if (option != null && !option.equals("")) {
                    parsedOptions.add(option);
                }
            }
        }

        return parsedOptions.toArray(new String[parsedOptions.size()]);
    }

}
