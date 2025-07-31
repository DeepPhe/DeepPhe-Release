package org.apache.ctakes.ner.group;

import org.apache.ctakes.ner.term.DetailedTerm;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {11/7/2023}
 */
public interface DetailedTermGroupAccessor<G extends Group<G>> extends GroupAccessor<G> {

   Collection<G> getDetailedTermGroups( final DetailedTerm term );

}
