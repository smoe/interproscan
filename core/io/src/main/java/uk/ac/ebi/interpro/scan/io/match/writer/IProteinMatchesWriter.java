package uk.ac.ebi.interpro.scan.io.match.writer;

import uk.ac.ebi.interpro.scan.model.Protein;

import java.io.Closeable;
import java.io.IOException;

/**
 * Write proteins to output
 */
public interface IProteinMatchesWriter extends Closeable {
    public int write(Protein protein) throws IOException;
}