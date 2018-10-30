package org.apache.ctakes.cancer.concept.instance;

import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/9/2016
 */
public interface ConceptInstance extends IdOwner {

   /**
    * @return the url of the instance
    */
   String getUri();

   /**
    * @return all represented Identified Annotations
    */
   Collection<IdentifiedAnnotation> getAnnotations();

   /**
    * @return preferred text
    */
   default String getPreferredText() {
      return Neo4jOntologyConceptUtil.getPreferredText( getUri() );
   }

   /**
    * @return name of the semantic group
    */
   default String getSemanticGroup() {
      return Neo4jOntologyConceptUtil.getSemanticGroup( getUri() )
                                     .getName();
   }

   /**
    * @return id of the patient with concept
    */
   String getPatientId();

   /**
    *
    * @return document identifiers as single text string.
    */
   String getJoinedDocumentId();

   /**
    * @return document identifiers for all annotations.
    */
   Collection<String> getDocumentIds();

   /**
       * @param annotation annotation within Concept
       * @return id of the document with annotation
       */
   String getDocumentId( IdentifiedAnnotation annotation );

   /**
    * @param annotation -
    * @return the date for the document in which the given annotation is found
    */
   Date getDocumentDate( IdentifiedAnnotation annotation );

   /**
    * @return true if the instance is negated: "not stage 2"
    */
   default boolean isNegated() {
      return getAnnotations().stream().anyMatch( a -> a.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT );
   }

   /**
    * @return true if the instance is uncertain "might be stage 2"
    */
   default boolean isUncertain() {
      return getAnnotations().stream().anyMatch( a -> a.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT );
   }

   /**
    * @return true if the instance is hypothetical "testing may indicate stage 2"
    */
   default boolean isGeneric() {
      return getAnnotations().stream().anyMatch( IdentifiedAnnotation::getGeneric );
   }

   /**
    * @return true if the instance is conditional "if positive then stage 2"
    */
   default boolean isConditional() {
      return getAnnotations().stream().anyMatch( IdentifiedAnnotation::getConditional );
   }

   /**
    * @return true if the instance is in patient history
    */
   default boolean inPatientHistory() {
      return getAnnotations().stream().anyMatch( a -> a.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT );
   }

   /**
    * @return string representation of subject
    */
   default String getSubject() {
      return getAnnotations().stream().map( IdentifiedAnnotation::getSubject )
                         .filter( Objects::nonNull ).filter( s -> s.length() > 0 )
                         .findFirst().orElse( "" );
   }

   /**
    *
    * @return Before, Before/Overlap, Overlap, After
    */
   default String getDocTimeRel() {
      return getAnnotations().stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
                         .map( EventProperties::getDocTimeRel ).filter( Objects::nonNull )
                         .min( CorefConceptInstance.DtrComparator.INSTANCE ).orElse( "" );
   }

   /**
    * @return Actual, hypothetical, hedged, generic
    */
   default String getModality() {
      return getAnnotations().stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
                         .map( EventProperties::getContextualModality ).filter( Objects::nonNull )
                         .findFirst().orElse( "" );
   }

   /**
    * @return true if is an intermittent event
    */
   default boolean isIntermittent() {
      return getAnnotations().stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
                         .map( EventProperties::getContextualAspect ).filter( Objects::nonNull )
                         .anyMatch( a -> a.length() > 0 );
   }

   default boolean isPermanent() {
      return getAnnotations().stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
                         .map( EventProperties::getPermanence ).filter( Objects::nonNull )
                         .anyMatch( p -> !p.equalsIgnoreCase( "Finite" ) );
   }

   /**
    * @return text in note
    */
   default String getCoveredText() {
      return getAnnotations().stream()
                         .sorted( Comparator.comparingInt( Annotation::getBegin ) )
                         .map( IdentifiedAnnotation::getCoveredText )
                         .collect( Collectors.joining( ",", "[", "]" ) );
   }



   /**
    * @return concept instances related to this concept instance and the name of the relation, recursive
    */
   default Map<String, Collection<ConceptInstance>> getForwardRelated() {
      return ConceptInstanceUtil.getRelations( this, ConceptInstanceUtil.RelationDirection.FORWARD );
   }

   /**
    * @return concept instances related to this concept instance and the name of the relation
    */
   Map<String,Collection<ConceptInstance>> getRelated();

   /**
    * @param relationMap new replacement relations for the concept instance.
    */
   default void setRelated( final Map<String, Collection<ConceptInstance>> relationMap ) {
      clearRelations();
      for ( Map.Entry<String, Collection<ConceptInstance>> entry : relationMap.entrySet() ) {
         final String name = entry.getKey();
         entry.getValue().forEach( ci -> addRelated( name, ci ) );
      }
   }

   /**
    * clear the forward relations.
    */
   void clearRelations();

   /**
    * As much as I hated to do it, I removed the standard of immutable CIs in order to better create ci relations
    * @param related concept instances related to this concept instance and the name of the relation
    */
   void addRelated( final String type, ConceptInstance related );

   /**
    * Reverse relations, as in a relation in which this concept instance is the target, not the source.
    * @return concept instances related to this concept instance and the name of the relation
    */
   Map<String,Collection<ConceptInstance>> getReverseRelated();

   /**
    * @param relationMap new replacement relations for the concept instance.
    */
   default void setReverseRelated( final Map<String, Collection<ConceptInstance>> relationMap ) {
      clearReverseRelations();
      for ( Map.Entry<String, Collection<ConceptInstance>> entry : relationMap.entrySet() ) {
         final String name = entry.getKey();
         entry.getValue().forEach( ci -> addReverseRelated( name, ci ) );
      }
   }

   /**
    * clear the reverse relations.
    */
   void clearReverseRelations();

   /**
    * Reverse relations, as in a relation in which this concept instance is the target, not the source.
    * As much as I hated to do it, I removed the standard of immutable CIs in order to better create ci relations
    * @param related concept instances related to this concept instance and the name of the relation
    */
   void addReverseRelated( final String type, ConceptInstance related );

   /**
    *
    * @param annotation -
    * @return type system EventProperties
    */
   static EventProperties getEventProperties( final IdentifiedAnnotation annotation ) {
      if ( !EventMention.class.isInstance( annotation ) ) {
         return null;
      }
      final Event event = ((EventMention) annotation).getEvent();
      if ( event != null ) {
         return event.getProperties();
      }
      return null;
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
      sb.append( getClass().getSimpleName() ).append( ": " )
        .append( getPatientId() ).append( "\n" )
        .append( getAnnotations().stream()
                                 .map( this::getDocumentId )
                                 .distinct()
                                 .collect( Collectors.joining( "_") ) ).append( "\n" )
        .append( getPreferredText() ).append( "\n" )
        .append( getUri() ).append( "\n" )
        .append( getCoveredText() )
        .append( " " )
        .append( isNegated() ? "\tnegated" : "" )
        .append( isUncertain() ? "\tuncertain" : "" )
        .append( isConditional() ? "\tconditional" : "" )
        .append( isGeneric() ? "\thypothetical" : "" )
        .append( isPermanent() ? "\tpermanent" : "" )
        .append( isIntermittent() ? "\tintermittent" : "" )
        .append( inPatientHistory() ? "\tpatient history" : "" )
        .append( getSubject().isEmpty() ? "" : "\t" + getSubject() )
        .append( getDocTimeRel().isEmpty() ? "" : "\t" + getDocTimeRel() )
        .append( getModality().isEmpty() ? "" : "\t" + getModality() );
      for ( Map.Entry<String,Collection<ConceptInstance>> related : getRelated().entrySet() ) {
         sb.append( "\n" ).append( related.getKey() );
         related.getValue().forEach( ci -> sb.append( "\n   " ).append( ci.getPreferredText() ) );
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

}
