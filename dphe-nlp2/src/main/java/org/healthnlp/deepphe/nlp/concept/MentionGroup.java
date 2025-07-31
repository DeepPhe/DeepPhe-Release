package org.healthnlp.deepphe.nlp.concept;

import org.healthnlp.deepphe.neo4j.node.Mention;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {9/9/2023}
 */
public interface MentionGroup {

    /**
     * @return all represented Identified Annotations
     */
    default Collection<Mention> getMentions() {
        return getNoteIdMap().keySet();
    }

    /**
     *
     * @return map of mentions and their note ids
     */
    Map<Mention, String> getNoteIdMap();

    /**
     * @param mention annotation within Concept
     * @return id of the document with annotation
     */
    default String getNoteId( final Mention mention ) {
        return getNoteIdMap().getOrDefault( mention, "No_Mention_No_Doc" );
    }

    /**
     *
     * @return map of documentId to collection of annotations in document
     */
    default Map<String,Collection<Mention>> getNoteMentions() {
        final Map<Mention, String> noteIdMap = getNoteIdMap();
        if ( noteIdMap == null ) {
            return Collections.emptyMap();
        }
        final Map<String,Collection<Mention>> docMentions = new HashMap<>();
        for ( Map.Entry<Mention,String> mentionDoc : noteIdMap.entrySet() ) {
            docMentions.computeIfAbsent( mentionDoc.getValue(), d -> new ArrayList<>() )
                    .add( mentionDoc.getKey() );
        }
        return docMentions;
    }

    /**
     *
     * @return a map of class uris and mentions with those uris
     */
    default Map<String, List<Mention>> getUriMentions() {
        return getMentions().stream()
                .collect( Collectors.groupingBy( Mention::getClassUri ) );
    }

    /**
     *
     * @param mention annotation within Concept
     * @param documentId id of the document with annotation
     * @param date       the date for the document in which the given annotation is found
     */
    void addMention( final Mention mention, final String documentId, final Date date );

    /**
         * @return true if the instance is negated: "not stage 2"
         */
    default boolean isNegated() {
        return getMentions().stream().anyMatch( Mention::isNegated );
    }

    /**
     * @return true if the instance is uncertain "might be stage 2"
     */
    default boolean isUncertain() {
        return getMentions().stream().anyMatch( Mention::isUncertain );
    }


    /**
     * @return true if the instance is in patient history
     */
    default boolean inPatientHistory() {
        return getMentions().stream().anyMatch( Mention::isHistoric );
    }

    /**
     * @return string representation of subject
     */
    default String getSubject() {
        return "";
//      return getMentions().stream().map( Mention::getSubject )
//                          .filter( Objects::nonNull ).filter( s -> s.length() > 0 )
//                          .findFirst().orElse( "" );
    }

//    /**
//     *
//     * @return Before, Before/Overlap, Overlap, After
//     */
//    default String getDocTimeRel() {
//        return getMentions().stream()
//                .map( Mention::getTemporality )
//                .filter( Objects::nonNull )
//                .min( DtrComparator.INSTANCE )
//                .orElse( "" );
//    }
//
//    enum DtrComparator implements Comparator<String> {
//        INSTANCE;
//
//        static private int getValue( final String dtr ) {
//            switch ( dtr.toUpperCase() ) {
//                case ("BEFORE/OVERLAP"):
//                    return 1000;
//                case ("AFTER"):
//                    return 100;
//                case ("BEFORE"):
//                    return 10;
//                case ("OVERLAP"):
//                    return 1;
//            }
//            return 0;
//        }
//
//        public int compare( final String dtr1, final String dtr2 ) {
//            return getValue( dtr2 ) - getValue( dtr1 );
//        }
//    }

}
