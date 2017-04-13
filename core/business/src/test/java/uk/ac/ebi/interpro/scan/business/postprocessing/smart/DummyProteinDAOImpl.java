package uk.ac.ebi.interpro.scan.business.postprocessing.smart;

import uk.ac.ebi.interpro.scan.model.Protein;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Phil Jones
 */
public class DummyProteinDAOImpl implements uk.ac.ebi.interpro.scan.persistence.ProteinDAO {
    @Override
    public Protein getProteinAndCrossReferencesByProteinId(Long id) {
        return null;
    }

    @Override
    public List<Protein> getProteinsBetweenIds(long bottom, long top) {
        return null;
    }

    @Override
    public List<Protein> getProteinsByIds(Set<Long> proteinIds) {
        return null;
    }

    @Override
    public PersistedProteins insertNewProteins(Collection<Protein> newProteins) {
        return null;
    }

    @Override
    public List<Protein> getProteinsAndMatchesAndCrossReferencesBetweenIds(long bottom, long top) {
        return null;
    }

    @Override
    public Protein insert(Protein newInstance) {
        return null;
    }

    @Override
    public Collection<Protein> insert(Collection<Protein> newInstances) {
        return null;
    }

    @Override
    public void update(Protein modifiedInstance) {
        // Do nothing
    }

    @Override
    public Protein read(Long id) {
        return null;
    }

    @Override
    public Protein readDeep(Long id, String... deepFields) {
        return null;
    }

    @Override
    public void delete(Protein persistentObject) {
      
    }

    @Override
    public Long count() {
        return null;
    }

    @Override
    public List<Protein> retrieveAll() {
        return null;
    }

    @Override
    public int deleteAll() {
        return 0;
    }

    @Override
    public Long getMaximumPrimaryKey() {
        return null;
    }

    @Override
    public void flush() {
        // Do nothing
    }
}
