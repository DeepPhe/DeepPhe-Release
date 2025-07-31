package org.healthnlp.deepphe.nlp.concept;

import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.nlp.confidence.ConfidenceOwner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {9/9/2023}
 */
public interface UriConcept extends MentionGroup, RelationGroup, ConfidenceOwner {

    // TODO - in uriconcept make a siteRelations group and a non-siteRelations group.
    //  Have a getSitesGroup() and getNonSitesGroup()

//    double getGroupedConfidence();

   void setConfidence( final double confidence );

      /**
       * @return some value for the concept
       */
   String getValue();

    /**
     * @return unique id.
     */
    String getId();

    /**
     * @return the url of the instance
     */
    String getUri();

    /**
     *
     * @return not a SemanticGroup, but one of a group of important SemanticTuis.
     */
    DpheGroup getDpheGroup();

    /**
     * @return the urls of all Mentions of the instance
     */
    default Collection<String> getAllUris() {
        return getUriRootsMap().keySet();
    }

    /**
     * TODO Return roots from some central location?
     * TODO getAllUris().stream().collect( Collectors.toMap( Function.identity(), UriStore::getUriRoots );
     * @return map of uris for all mentions and the roots for those uris
     */
    Map<String,Collection<String>> getUriRootsMap();

    /**
     *
     * @return all Uris, including duplicates
     */
    default List<String> getAllUrisList() {
        return getMentions().stream()
                .map( Mention::getClassUri )
                .collect( Collectors.toList() );
    }

    /**
     * @return id of the patient with concept
     */
    String getPatientId();

    default boolean isWantedForSummary() {
        return !isNegated();
    }

    default boolean hasWantedRelations() {
        return true;
    }

    Map<String,Collection<String>> getCodifications();

    /**
     * @return the uri and covered text
     */
    default String toShortText() {
        return getDpheGroup() + " " + getUri();
    }

    /**
     * @return text indicating state of negated, uncertain, etc.
     */
    default String toLongText() {
        final StringBuilder sb = new StringBuilder();
        sb.append( getUri() ).append( "  " )
                .append( getId() ).append( "  " )
          .append( getConfidence() ).append( "  " )
//          .append( getGroupedConfidence() )
          .append( " :\n" );
        getUriMentions().forEach( (k,v) -> sb.append( " " )
                .append( k )
                .append( " mentions=" )
                .append( v.size() )
//                                             .append( " " ).append( v.stream().map( Mention::getText ).collect( Collectors.joining(";")) )
        );
        for ( Map.Entry<String,Collection<UriConcept>> related : getRelatedConceptMap().entrySet() ) {
            sb.append( "\n  " ).append( related.getKey() );
            related.getValue().forEach( ci -> sb.append( "\n    " )
                    .append( ci.getUri() ).append( " " )
                    .append( ci.getId() ).append( " : "  )
                    .append( ci.getUriMentions()
                            .entrySet()
                            .stream()
                            .map( e -> e.getKey() + "=" + e.getValue().size() )
                            .collect( Collectors.joining( " " ) ) ) );
        }
        return sb.toString();
    }



    UriConcept NULL_URI_CONCEPT = new UriConcept() {
//        @Override
//        public double getGroupedConfidence() {
//            return 0;
//        }
         public void setConfidence( final double confidence ) {
         }

         @Override
         public String getValue() {
            return UriConstants.UNKNOWN;
         }

        @Override
        public String getUri() {
            return UriConstants.UNKNOWN;
        }

        @Override
        public DpheGroup getDpheGroup() {
            return DpheGroup.FINDING;
        }

        @Override
        public Map<String,Collection<String>> getUriRootsMap() {
            return Collections.emptyMap();
        }

        @Override
        public Map<Mention, String> getNoteIdMap() {
            return Collections.emptyMap();
        }

        @Override
        public void addMention(Mention mention, String documentId, Date date) {
        }

        @Override
        public Map<String,Collection<String>> getCodifications() {
            return Collections.emptyMap();
        }

        @Override
        public String getPatientId() {
            return "";
        }

        @Override
        public Map<String, Collection<UriConceptRelation>> getAllRelations() {
            return Collections.emptyMap();
        }

        @Override
        public void addRelation( final UriConceptRelation relation ) {

        }

        @Override
        public void addRelatedConcept( final String type, final UriConcept related ) {
        }

        @Override
        public void clearRelations() {
        }

        @Override
        public String getId() {
            return "NULL_URI_CONCEPT";
        }
    };

}
