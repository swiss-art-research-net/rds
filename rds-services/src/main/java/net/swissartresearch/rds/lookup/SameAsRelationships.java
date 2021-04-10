package net.swissartresearch.rds.lookup;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Holder for a set of resources considered identical (same-as)
 * 
 * @author Wolfgang Schell <ws@metaphacts.com>
 */
public class SameAsRelationships {
    private final String primaryId;
    private final Set<String> sameAsIDs;

    public SameAsRelationships(String primaryId) {
        this(primaryId, Collections.singleton(primaryId));
    }

    public SameAsRelationships(String primaryId, Set<String> sameAsIRIs) {
        this.primaryId = primaryId;
        this.sameAsIDs = new TreeSet<>(sameAsIRIs);
        this.sameAsIDs.add(primaryId);
    }

    public String getPrimaryId() {
        return primaryId;
    }

    public SameAsRelationships withCandidate(String sameAs) {
        sameAsIDs.add(sameAs);
        return this;
    }

    public SameAsRelationships withCandidates(Set<String> sameAs) {
        sameAsIDs.addAll(sameAs);
        return this;
    }

    public SameAsRelationships merge(SameAsRelationships other) {
        sameAsIDs.add(other.getPrimaryId());
        sameAsIDs.addAll(other.getSameAsIDs());
        return this;
    }

    public boolean hasSameAs() {
        return !sameAsIDs.isEmpty();
    }

    public boolean isSameAs(String id) {
        return sameAsIDs.contains(id);
    }

    public boolean hasOthersThan(String id) {
        return sameAsIDs.contains(id) && sameAsIDs.size() > 1;
    }

    public Set<String> getSameAsIDs() {
        return sameAsIDs;
    }
}