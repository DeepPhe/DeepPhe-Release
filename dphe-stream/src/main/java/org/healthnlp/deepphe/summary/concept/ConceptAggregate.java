package org.healthnlp.deepphe.summary.concept;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.util.KeyValue;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;
import static org.healthnlp.deepphe.neo4j.constant.UriConstants.LYMPH_NODE;
import static org.healthnlp.deepphe.summary.concept.ConceptAggregate.NeoplasmType.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/17/2020
 */
public interface ConceptAggregate {

   enum NeoplasmType {
      CANCER,
      PRIMARY,
      SECONDARY,
      NON_CANCER,
      UNKNOWN
   }

   /**
    * @return unique id.
    */
   String getId();

   /**
    * @return the url of the instance
    */
   String getUri();

   /**
    * @return the urls of all Mentions of the instance
    */
   default Collection<String> getAllUris() {
      return getUriRootsMap().keySet();
   }

   /**
    *
    * @return map of uris for all mentions and the roots for those uris
    */
   Map<String,Collection<String>> getUriRootsMap();

   /**
    *
    * @return a score used to find the "best" uri. 0-1, can be used for confidence.
    */
   double getUriScore();

   /**
    *
    * @return the Uris and how they score against each other.
    */
   List<KeyValue<String, Double>> getUriQuotients();

   /**
    * @return all represented Identified Annotations
    */
   default Collection<Mention> getMentions() {
      return getNoteIdMap().keySet();
//                           .stream()
//                           .sorted( Comparator.comparing( Mention::getBegin ).thenComparing( Mention::getEnd ) )
//                           .collect( Collectors.toList() );
   }

   /**
    *
    * @return a map of class uris and mentions with those uris
    */
   default Map<String,List<Mention>> getUriMentions() {
      return getMentions().stream()
                          .collect( Collectors.groupingBy( Mention::getClassUri ) );
   }

   /**
    * @return preferred text
    */
   default String getPreferredText() {
      return Neo4jOntologyConceptUtil.getPreferredText( getUri() );
   }

//   /**
//    * @return name of the semantic group
//    */
//   default String getSemanticGroup() {
//      return Neo4jOntologyConceptUtil.getSemanticGroup( getUri() )
//                                     .getName();
//   }

   /**
    * @return id of the patient with concept
    */
   String getPatientId();

   /**
    *
    * @return document identifiers as single text string.
    */
   default String getJoinedNoteId() {
      return getNoteIdMap().values()
                           .stream()
                           .distinct()
                           .collect( Collectors.joining( "_") );
   }

   /**
    * @return document identifiers for all annotations.
    */
   default Collection<String> getNoteIds() {
      return getNoteIdMap().values();
   }

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
    * @param mention -
    * @return the date for the document in which the given mention is found
    */
   Date getNoteDate( Mention mention );

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
    * @return true if the instance is hypothetical "testing may indicate stage 2"
    */
   default boolean isGeneric() {
      return getMentions().stream().anyMatch( Mention::isGeneric );
   }

   /**
    * @return true if the instance is conditional "if positive then stage 2"
    */
   default boolean isConditional() {
      return getMentions().stream().anyMatch( Mention::isConditional );
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

   /**
    *
    * @return Before, Before/Overlap, Overlap, After
    */
   default String getDocTimeRel() {
      return getMentions().stream()
                          .map( Mention::getTemporality )
                          .filter( Objects::nonNull )
                          .min( DtrComparator.INSTANCE )
                          .orElse( "" );
   }

//   /**
//    * @return Actual, hypothetical, hedged, generic
//    */
//   default String getModality() {
//      return getMentions().stream().map( ConceptAggregate::getEventProperties ).filter( Objects::nonNull )
//                          .map( EventProperties::getContextualModality ).filter( Objects::nonNull )
//                          .findFirst().orElse( "" );
//   }
//
//   /**
//    * @return true if is an intermittent event
//    */
//   default boolean isIntermittent() {
//      return getMentions().stream().map( ConceptAggregate::getEventProperties ).filter( Objects::nonNull )
//                          .map( EventProperties::getContextualAspect ).filter( Objects::nonNull )
//                          .anyMatch( a -> a.length() > 0 );
//   }
//
//   default boolean isPermanent() {
//      return getMentions().stream().map( ConceptAggregate::getEventProperties ).filter( Objects::nonNull )
//                          .map( EventProperties::getPermanence ).filter( Objects::nonNull )
//                          .anyMatch( p -> !p.equalsIgnoreCase( "Finite" ) );
//   }

   default String getCoveredText( final Mention mention ) {
      final String noteId = getNoteId( mention );
      if ( noteId.isEmpty() ) {
         return "";
      }
      final Note note = NoteNodeStore.getInstance().get( noteId );
      if ( note == null ) {
         return "";
      }
      final String text = note.getText();
      if ( text.length()  < mention.getEnd() ) {
         return "";
      }
      return text.substring( mention.getBegin(), mention.getEnd() );
   }

   /**
    * @return text in note
    */
   default String getCoveredText() {
      return getMentions().stream()
                          .sorted( Comparator.comparingInt( Mention::getBegin ).thenComparing( Mention::getEnd ) )
                          .map( this::getCoveredText )
                          .collect( Collectors.joining( ",", "[", "]" ) );
   }


   /**
    * @return concept instances related to this concept instance and the name of the relation
    */
   Map<String,Collection<ConceptAggregate>> getRelatedConceptMap();


   /**
    * @param relationMap new replacement relations for the concept instance.
    */
   default void setRelated( final Map<String, Collection<ConceptAggregate>> relationMap ) {
      clearRelations();
      for ( Map.Entry<String, Collection<ConceptAggregate>> entry : relationMap.entrySet() ) {
         final String name = entry.getKey();
         entry.getValue().forEach( ci -> addRelated( name, ci ) );
      }
   }


   /**
    * clear the forward relations.
    */
   void clearRelations();

   /**
    *
    * @return map of mentions and their note ids
    */
   Map<Mention, String> getNoteIdMap();


   /**
    * As much as I hated to do it, I removed the standard of immutable CIs in order to better create ci relations
    * @param related concept instances related to this concept instance and the name of the relation
    */
   void addRelated( final String type, ConceptAggregate related );

   /**
    * As much as I hated to do it, I removed the standard of immutable CIs in order to better create ci relations
    * @param related concept instances related to this concept instance and the name of the relation
    */
   void addRelated( final String type, Collection<ConceptAggregate> related );


   default Collection<ConceptAggregate> getRelated( final String type ) {
      final Collection<ConceptAggregate> related = getRelatedConceptMap().get( type );
      if ( related != null ) {
         return related;
      }
      return Collections.emptyList();
   }

   default Collection<ConceptAggregate> getRelated( final String... types ) {
      final Collection<ConceptAggregate> relateds = new HashSet<>();
      for ( String type : types ) {
         final Collection<ConceptAggregate> related = getRelated( type );
         if ( related != null ) {
            relateds.addAll( related );
         }
      }
      return relateds;
   }


   default Collection<String> getRelatedUris( final String type ) {
      return getRelated( type ).stream()
                               .map( ConceptAggregate::getUri )
                               .collect( Collectors.toSet() );
   }

   default Collection<String> getRelatedUris( final String... types ) {
      final Collection<String> relateds = new HashSet<>();
      for ( String type : types ) {
         final Collection<String> relatedUris = getRelatedUris( type );
         if ( relatedUris != null ) {
            relateds.addAll( relatedUris );
         }
      }
      return relateds;
   }

   default Collection<ConceptAggregate> getRelatedSites() {
      return getRelatedConceptMap().entrySet()
                       .stream()
                       .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
                       .map( Map.Entry::getValue)
                       .flatMap( Collection::stream )
                       .collect( Collectors.toSet() );
   }


   default Collection<String> getRelatedSiteMainUris() {
      return getRelatedSites().stream()
                               .map( ConceptAggregate::getUri )
                               .collect( Collectors.toSet() );
   }

   default Collection<String> getRelatedSiteAllUris() {
      return getRelatedSites().stream()
                              .map( ConceptAggregate::getAllUris )
                              .flatMap( Collection::stream )
                              .collect( Collectors.toSet() );
   }



   default Map<String,Collection<ConceptAggregate>> getNonLocationRelationMap() {
      return  getRelatedConceptMap().entrySet()
                      .stream()
                      .filter( e -> !RelationConstants.isLocationRelation( e.getKey() ) )
                      .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
   }


   default boolean isWantedForSummary() {
      return !isNegated();
   }

   default boolean hasWantedRelations() {
      return true;
   }


   /**
    *
    * @return Secondary : metastasis uri or Invasive_Lesion; Primary : not INVASIVE_LESION, Cancer : no tumor extent
    */
   default ConceptAggregate.NeoplasmType getNeoplasmType() {
//      final Logger LOGGER = Logger.getLogger( "ConceptAggregate" );
      if ( getUri().contains( "In_Situ" ) ) {
         // An in situ has to be primary.
//         LOGGER.info( "neoplasm " + getUri() + " " + getId() + " is Primary by \"in_situ\" in URI." );
         return PRIMARY;
      }
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> stages = UriConstants.getCancerStages( graphDb );
      if ( getRelatedUris( HAS_STAGE ).stream().anyMatch( stages::contains ) ) {
//         LOGGER.info( "neoplasm " + getUri() + " "  + getId() + " is Primary by having a Stage." );
         return PRIMARY;
      }

      final Collection<String> relations = getRelatedConceptMap().keySet();
      if ( relations.contains( HAS_CLINICAL_T )
           || relations.contains( HAS_CLINICAL_N )
           || relations.contains( HAS_CLINICAL_M )
           || relations.contains( HAS_PATHOLOGIC_T )
           || relations.contains( HAS_PATHOLOGIC_N )
           || relations.contains( HAS_PATHOLOGIC_M )
      ) {
//         LOGGER.info( "neoplasm " + getUri() + " "+ getId() + " is Primary by having a TNM." );
         return PRIMARY;
      }
      final Collection<String> diagnoses = new ArrayList<>();
      diagnoses.add( getUri() );
      getRelated( HAS_DIAGNOSIS ).stream()
              .map( ConceptAggregate::getUri )
              .forEach( diagnoses::add );
      final boolean benignUri = UriConstants.getBenignTumorUris( graphDb ).stream()
                                            .anyMatch( diagnoses::contains );
      if ( benignUri ) {
//         LOGGER.info( "neoplasm " + getUri() + " " + getId() + " is Not a Cancer by being or having a Benign Tumor URI according to Ontology in a Diagnosis" );
         return ConceptAggregate.NeoplasmType.NON_CANCER;
      }
      // Check metastases first so that we don't get something like historic In_Situ in a lymph node
      final boolean metastasisUri = UriConstants.getMetastasisUris( graphDb ).stream()
                                                .anyMatch( diagnoses::contains );
      if ( metastasisUri ) {
//         LOGGER.info( "neoplasm " + getUri() + " " + getId() +  " is Secondary by being or having a Secondary Tumor URI according to Ontology in a Diagnosis." );
         return SECONDARY;
      }
      // TODO - check for no diagnosis ?  diagnoses size > 1.  If no diagnosis maybe hold off on being primary?
      final boolean primaryUri = UriConstants.getPrimaryUris( graphDb ).stream()
                                             .anyMatch( diagnoses::contains );
      if ( primaryUri ) {
//         LOGGER.info( "neoplasm " + getUri() + " " + getId() + " is Primary by being or having a Primary URI according to Ontology in a Diagnosis." );
         return PRIMARY;
      }
      final Collection<String> extentUris = getRelatedUris( HAS_TUMOR_EXTENT );
      if ( extentUris.contains( "In_Situ_Lesion" ) ) {
//         LOGGER.info( "neoplasm " + getUri() + " " + getId() + " is Primary by having an in situ lesion as an extent." );
         return PRIMARY;
      } else if ( extentUris.contains( "Invasive_Lesion" )
                  || extentUris.contains( "Metastatic_Lesion" ) ) {
//         LOGGER.info( "neoplasm " + getUri() + " " + getId() + " is Secondary by having an invasive or metastatic lesion as an extent." );
         return SECONDARY;
      }
      final Collection<ConceptAggregate> metastasesOf = getRelated( METASTASIS_OF );
      if ( !metastasesOf.isEmpty() ) {
//         LOGGER.info( "neoplasm " + getUri() + " " + getId() + " is Secondary by having a metastasis_of relation." );
         return SECONDARY;
      }
      final Collection<String> primarySiteUris = getRelatedSiteMainUris();
      if ( !primarySiteUris.isEmpty() ) {
         // This won't work for lymphoma
         final Collection<String> lymphSiteUris = Neo4jOntologyConceptUtil.getBranchUris( LYMPH_NODE );
         if ( primarySiteUris.stream().anyMatch( lymphSiteUris::contains ) ) {
//            LOGGER.info( "neoplasm " + getUri() + " " + getId() + " is Secondary by having a lymph node site." );
            return SECONDARY;
         }
      }
      return UNKNOWN;
   }



   /**
    * @return the uri and covered text
    */
   default String toShortText() {
      return getUri() + " = " + getCoveredText();
   }


   /**
    * @return text indicating state of negated, uncertain, etc.
    */
   default String toText() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n" )
//            .append( getClass().getSimpleName() ).append( ": " )
//        .append( getPatientId() ).append( "  " )
//        .append( getMentions().stream()
//                              .map( this::getNoteId )
//                              .distinct()
//                              .collect( Collectors.joining( "_") ) ).append( "\n" )
//        .append( getPreferredText() ).append( "\n" )
        .append( getUri() ).append( "  " )
        .append( getId() ).append( " :\n" );
      getUriQuotients().stream()
                  .map( kv -> " " + kv.getKey() + "=" + kv.getValue() + " "  )
                  .forEach( sb::append );
      sb.append( "\n" );
//        .append( getMentions().size() ).append( " mentions,   all uris: " );
      getUriMentions().forEach( (k,v) -> sb.append( " " )
                                           .append( k )
                                           .append( "=" )
                                           .append( v.size() ) );
//         .append( String.join( " ", getAllUris() ) );
//        .append( getCoveredText() )
//        .append( " " )
//        .append( isNegated() ? "\tnegated" : "" )
//        .append( isUncertain() ? "\tuncertain" : "" )
//        .append( isConditional() ? "\tconditional" : "" )
//        .append( isGeneric() ? "\thypothetical" : "" )
////        .append( isPermanent() ? "\tpermanent" : "" )
////        .append( isIntermittent() ? "\tintermittent" : "" )
//        .append( inPatientHistory() ? "\tpatient history" : "" )
//        .append( getSubject().isEmpty() ? "" : "\t" + getSubject() )
//        .append( getDocTimeRel().isEmpty() ? "" : "\t" + getDocTimeRel() );
////        .append( getModality().isEmpty() ? "" : "\t" + getModality() );
      for ( Map.Entry<String,Collection<ConceptAggregate>> related : getRelatedConceptMap().entrySet() ) {
         sb.append( "\n  " ).append( related.getKey() );
//         related.getValue().forEach( ci -> sb.append( "\n   " ).append( ci.getPreferredText() ).append( "  " ).append( ci.getId() ) );
         related.getValue().forEach( ci -> sb.append( "\n    " )
                                             .append( ci.getUri() ).append( " " )
                                             .append( ci.getId() ).append( " : "  )
//                                             .append( ci.getMentions().size() ).append( " mentions,   all uris: " )
//                                             .append( String.join( " ", ci.getAllUris() ) ) );
               .append( ci.getUriMentions().entrySet().stream().map( e -> e.getKey() + "=" + e.getValue().size() ).collect(
                     Collectors.joining( " " ) ) ) );
      }
      return sb.toString();
   }


   enum DtrComparator implements Comparator<String> {
      INSTANCE;

      static private int getValue( final String dtr ) {
         switch ( dtr.toUpperCase() ) {
            case ("BEFORE/OVERLAP"):
               return 1000;
            case ("AFTER"):
               return 100;
            case ("BEFORE"):
               return 10;
            case ("OVERLAP"):
               return 1;
         }
         return 0;
      }

      public int compare( final String dtr1, final String dtr2 ) {
         return getValue( dtr2 ) - getValue( dtr1 );
      }
   }



   @Immutable
   ConceptAggregate NULL_AGGREGATE = new ConceptAggregate() {
      @Override
      public String getUri() {
         return UriConstants.UNKNOWN;
      }

      @Override
      public Map<String,Collection<String>> getUriRootsMap() {
         return Collections.emptyMap();
      }

      @Override
      public double getUriScore() {
         return 0.0;
      }
      @Override
      public List<KeyValue<String, Double>> getUriQuotients() {
         return Collections.emptyList();
      }
      @Override
      public Map<Mention, String> getNoteIdMap() {
         return Collections.emptyMap();
      }

      @Override
      public String getPreferredText() {
         return "";
      }

      @Override
      public String getPatientId() {
         return "";
      }

      @Override
      public String getJoinedNoteId() {
         return "";
      }

      @Override
      public Date getNoteDate( final Mention mention ) {
         return null;
      }

      @Override
      public String getCoveredText() {
         return "";
      }

      @Override
      public Map<String, Collection<ConceptAggregate>> getRelatedConceptMap() {
         return null;
      }

      @Override
      public void clearRelations() {
      }

      @Override
      public void addRelated( final String type, final ConceptAggregate related ) {
      }

      @Override
      public void addRelated( final String type, final Collection<ConceptAggregate> related ) {
      }

      @Override
      public String getId() {
         return "NULL_AGGREGATE";
      }
   };

}
