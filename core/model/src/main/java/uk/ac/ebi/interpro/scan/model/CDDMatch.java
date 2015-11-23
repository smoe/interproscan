/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ebi.interpro.scan.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.Set;

/**
 * CDD filtered match.
 *
 * @author Antony Quinn
 * @version $Id$
 * @since 1.0
 */
@Entity
@Table(name = "cdd_match")
@XmlType(name = "CDDMatchType")
public class CDDMatch extends Match<CDDMatch.CDDLocation> {

    protected CDDMatch() {
    }

    public CDDMatch(Signature signature, Set<CDDLocation> locations) {
        super(signature, locations);
    }

    public Object clone() throws CloneNotSupportedException {
        final Set<CDDLocation> clonedLocations = new HashSet<CDDLocation>(this.getLocations().size());
        for (CDDLocation location : this.getLocations()) {
            clonedLocations.add((CDDLocation) location.clone());
        }
        return new CDDMatch(this.getSignature(), clonedLocations);
    }

    /**
     * Location(s) of match on protein sequence
     *
     * @author Antony Quinn
     */
    @Entity
    @Table(name = "cdd_location")
    @XmlType(name = "CDDLocationType")
    public static class CDDLocation extends Location {

        @Column(nullable = false)
        private double score;

        @Column(nullable = false)
        private double evalue;

        /**
         * protected no-arg constructor required by JPA - DO NOT USE DIRECTLY.
         */
        protected CDDLocation() {
        }

        public CDDLocation(int start, int end, double score, double evalue) {
            super(start, end);
            setScore(score);
            setEvalue(evalue);
        }

        @XmlAttribute(required = true)
        public double getScore() {
            return score;
        }

        private void setScore(double score) {
            this.score = score;
        }

        @XmlAttribute(required = true)
        public double getEvalue() {
            return evalue;
        }

        private void setEvalue(double evalue) {
            this.evalue = evalue;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CDDLocation))
                return false;
            final CDDLocation f = (CDDLocation) o;
            return new EqualsBuilder()
                    .appendSuper(super.equals(o))
                    .append(score, f.score)
                    .append(evalue, f.evalue)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(19, 21)
                    .appendSuper(super.hashCode())
                    .append(score)
                    .append(evalue)
                    .toHashCode();
        }

        public Object clone() throws CloneNotSupportedException {
            return new CDDLocation(this.getStart(), this.getEnd(), this.getScore(), this.getEvalue());
        }
    }
}
