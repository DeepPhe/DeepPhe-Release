package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.tcas.Annotation;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/9/2016
 */
@Immutable
final class CorefConceptInstance implements ConceptInstance {

   static private final Logger LOGGER = Logger.getLogger( "CorefConceptInstance" );

   private final String _uri;
   private final Collection<IdentifiedAnnotation> _annotations;
   private final int _hashCode;

   CorefConceptInstance( final String uri, final Collection<IdentifiedAnnotation> annotations ) {
      _uri = uri;
      _annotations = Collections.unmodifiableCollection( annotations );
      _hashCode = (int) _annotations.stream()
            .sorted( (Comparator.comparing( IdentifiedAnnotation::getBegin )) )
            .mapToLong( Object::hashCode ).sum();
   }


   /**
    * @return the url of the instance
    */
   @Override
   public String getUri() {
      return _uri;
   }

   /**
    * @return all represented Identified Annotations
    */
   @Override
   public Collection<IdentifiedAnnotation> getAnnotations() {
      return _annotations;
   }

   /**
    * @return true if the instance is negated: "not stage 2"
    */
   @Override
   public boolean isNegated() {
      return _annotations.stream().anyMatch( a -> a.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT );
   }

   /**
    * @return true if the instance is uncertain "might be stage 2"
    */
   @Override
   public boolean isUncertain() {
      return _annotations.stream().anyMatch( a -> a.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT );
   }

   /**
    * @return true if the instance is hypothetical "testing may indicate stage 2"
    */
   @Override
   public boolean isGeneric() {
      return _annotations.stream().anyMatch( IdentifiedAnnotation::getGeneric );
   }

   /**
    * @return true if the instance is conditional "if positive then stage 2"
    */
   @Override
   public boolean isConditional() {
      return _annotations.stream().anyMatch( IdentifiedAnnotation::getConditional );
   }

   /**
    * @return true if the instance is in patient history
    */
   @Override
   public boolean inPatientHistory() {
      return _annotations.stream().anyMatch( a -> a.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT );
   }

   /**
    * @return string representation of subject
    */
   @Override
   public String getSubject() {
      return _annotations.stream().map( IdentifiedAnnotation::getSubject )
            .filter( Objects::nonNull ).filter( s -> s.length() > 0 )
            .findFirst().orElse( "" );
   }

   @Override
   public String getDocTimeRel() {
      return _annotations.stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
            .map( EventProperties::getDocTimeRel ).filter( Objects::nonNull )
            .sorted( DtrComparator.INSTANCE ).findFirst().orElse( "" );
   }

   /**
    * @return Actual, hypothetical, hedged, generic
    */
   @Override
   public String getModality() {
      return _annotations.stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
            .map( EventProperties::getContextualModality ).filter( Objects::nonNull )
            .findFirst().orElse( "" );
   }

   /**
    * @return true if is an intermittent event
    */
   @Override
   public boolean isIntermittent() {
      return _annotations.stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
            .map( EventProperties::getContextualAspect ).filter( Objects::nonNull )
            .anyMatch( a -> a.length() > 0 );
   }

   @Override
   public boolean isPermanent() {
      return _annotations.stream().map( ConceptInstance::getEventProperties ).filter( Objects::nonNull )
            .map( EventProperties::getPermanence ).filter( Objects::nonNull )
            .anyMatch( p -> !p.equalsIgnoreCase( "Finite" ) );
   }


   @Override
   public String getCoveredText() {
      return _annotations.stream()
            .sorted( Comparator.comparingInt( Annotation::getBegin ) )
            .map( IdentifiedAnnotation::getCoveredText )
            .collect( Collectors.joining( ",", "[", "]" ) );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<ConceptInstance> getValues() {
      return ConceptInstanceUtil.getPropertyValues( this );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return toText();
   }


   public boolean equals( final Object other ) {
      return other instanceof CorefConceptInstance
            && ((CorefConceptInstance) other)._annotations.size() == _annotations.size()
            && ((CorefConceptInstance) other)._annotations.containsAll( _annotations );
   }

   public int hashCode() {
      return _hashCode;
   }

   private enum DtrComparator implements Comparator<String> {
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
