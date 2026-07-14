package org.healthnlp.deepphe.nlp.concept;


import org.healthnlp.deepphe.nlp.confidence.ConfidenceGroup;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author SPF , chip-nlp
 * @since {9/9/2023}
 */
public interface RelationGroup {


    /**
     *
     * @return relations
     */
    Map<String,Collection<UriConceptRelation>> getAllRelations();

    /**
     *
     * @return best relations
     */
    default Map<String,Collection<UriConceptRelation>> getAllBestRelations() {
        final Map<String,Collection<UriConceptRelation>> relationsMap = getAllRelations();
        final Map<String,Collection<UriConceptRelation>> bestRelations = new HashMap<>( relationsMap.size() );
        for ( Map.Entry<String,Collection<UriConceptRelation>> typeRelations : relationsMap.entrySet() ) {
            bestRelations.put( typeRelations.getKey(), getBestRelations( typeRelations.getValue() ) );
        }
        return bestRelations;
    }

    /**
     *
     * @param type -
     * @return -
     */
    default Collection<UriConceptRelation> getRelations( final String type ) {
        return getAllRelations().getOrDefault( type, Collections.emptyList() );
    }

    default Collection<UriConceptRelation> getUriConceptRelations( final String... types ) {
        return Stream.of( types )
                .map( this::getRelations )
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
    }

    void addRelation( final UriConceptRelation relation );

    /**
     * @return concept instances related to this concept instance and the name of the relation
     */
    default Map<String, Collection<UriConcept>> getRelatedConceptMap() {
//        final Map<String,Collection<UriConceptRelation>> bestRelations = getAllBestRelations();
        final Map<String,Collection<UriConceptRelation>> bestRelations = getAllRelations();
        final Map<String,Collection<UriConcept>> bestRelatedConcepts = new HashMap<>();
        for ( Map.Entry<String,Collection<UriConceptRelation>> typeRelations : bestRelations.entrySet() ) {
            bestRelatedConcepts.put( typeRelations.getKey(), getRelationTargets( typeRelations.getValue() ) );
        }
        return bestRelatedConcepts;
    }

    static Collection<UriConcept> getRelationTargets( final Collection<UriConceptRelation> relations ) {
        return relations.stream()
                .map( UriConceptRelation::getTarget )
                .collect( Collectors.toSet() );
    }

    /**
     *
     * @param relations -
     * @return The Relations with the top 2 confidence rankings.
     */
    static Collection<UriConceptRelation> getBestRelations( final Collection<UriConceptRelation> relations ) {
        if ( relations.size() <= 1 ) {
            return relations;
        }
        return new ConfidenceGroup<>( relations ).getBest();
    }

    /**
     *
     * @param type -
     * @return -
     */
    default Collection<UriConcept> getRelatedConcepts(final String type ) {
        return getRelatedConceptMap().getOrDefault( type, Collections.emptyList() );
    }

    /**
     *
     * @param types -
     * @return -
     */
    default Collection<UriConcept> getRelatedConcepts(final String... types ) {
        return Arrays.stream( types )
                .map( this::getRelatedConcepts )
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
    }


    void addRelatedConcept( final String type, final UriConcept related );

    default void addRelatedConcepts( final String type, final Collection<UriConcept> related ) {
        related.forEach( r -> addRelatedConcept( type, r ) );
    }

    /**
     * @param relationMap new replacement relations for the concept instance.
     */
    default void setRelatedConcepts( final Map<String, Collection<UriConcept>> relationMap ) {
        clearRelations();
        relationMap.forEach( (k,v) -> v.forEach( c -> addRelatedConcept( k, c ) ) );
//        for ( Map.Entry<String, Collection<Concept>> entry : relationMap.entrySet() ) {
//            final String name = entry.getKey();
//            entry.getValue().forEach( c -> addRelatedConcept( name, c ) );
//        }
    }

//    /**
//     * As much as I hated to do it, I removed the standard of immutable CIs in order to better create ci relations
//     * @param related concept instances related to this concept instance and the name of the relation
//     */
//    void addRelatedConcept( final String type, UriConcept related );
//
//    /**
//     * As much as I hated to do it, I removed the standard of immutable CIs in order to better create ci relations
//     * @param related concept instances related to this concept instance and the name of the relation
//     */
//    void addRelatedConcepts( final String type, Collection<UriConcept> related );

    /**
     * clear the forward relations.
     */
    void clearRelations();

    /**
     *
     * @param type -
     * @return -
     */
    default Collection<String> getRelatedUris( final String type ) {
        return getRelatedConcepts( type ).stream()
                .map( UriConcept::getUri )
                .collect( Collectors.toSet() );
    }

}
