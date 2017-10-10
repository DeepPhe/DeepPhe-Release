package org.apache.ctakes.cancer.concept.instance;

import org.apache.ctakes.cancer.owl.OwlUriUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/17/2016
 */
@Immutable
final class SimpleConceptInstance implements ConceptInstance {

   static private final Logger LOGGER = Logger.getLogger( "SimpleConceptInstance" );

   private final IdentifiedAnnotation _annotation;

   SimpleConceptInstance( final IdentifiedAnnotation annotation ) {
      _annotation = annotation;
   }

   /**
    * @return the url of the instance
    */
   @Override
   public String getUri() {
      return OwlUriUtil.getSpecificUri( _annotation );
   }

   /**
    * @return all represented Identified Annotations
    */
   @Override
   public Collection<IdentifiedAnnotation> getAnnotations() {
      return Collections.singletonList( _annotation );
   }

   /**
    * @return true if the instance is negated: "not stage 2"
    */
   @Override
   public boolean isNegated() {
      return _annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT;
   }

   /**
    * @return true if the instance is uncertain "might be stage 2"
    */
   @Override
   public boolean isUncertain() {
      return _annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT;
   }

   /**
    * @return true if the instance is hypothetical "testing may indicate stage 2"
    */
   @Override
   public boolean isGeneric() {
      return _annotation.getGeneric();
   }

   /**
    * @return true if the instance is conditional "if positive then stage 2"
    */
   @Override
   public boolean isConditional() {
      return _annotation.getConditional();
   }

   /**
    * @return true if the instance is in patient history
    */
   @Override
   public boolean inPatientHistory() {
      return _annotation.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT;
   }

   /**
    * @return string representation of subject
    */
   @Override
   public String getSubject() {
      final String subject = _annotation.getSubject();
      return subject == null ? "" : subject;
   }

   @Override
   public String getDocTimeRel() {
      final EventProperties properties = getEventProperties();
      if ( properties != null ) {
         final String dtr = properties.getDocTimeRel();
         return dtr == null ? "" : dtr;
      }
      return "";
   }

   /**
    * @return Actual, hypothetical, hedged, generic
    */
   @Override
   public String getModality() {
      final EventProperties properties = getEventProperties();
      if ( properties != null ) {
         final String cm = properties.getContextualModality();
         return cm == null ? "" : cm;
      }
      return "";
   }

   /**
    * @return true if is an intermittent event
    */
   @Override
   public boolean isIntermittent() {
      final EventProperties properties = getEventProperties();
      if ( properties != null ) {
         final String intermittent = properties.getContextualAspect();
         return intermittent != null && !intermittent.isEmpty();
      }
      return false;
   }

   @Override
   public boolean isPermanent() {
      final EventProperties properties = getEventProperties();
      if ( properties != null ) {
         final String permanence = properties.getPermanence();
         return permanence != null && !permanence.equalsIgnoreCase( "Finite" );
      }
      return false;
   }

   @Override
   public String getCoveredText() {
      return _annotation.getCoveredText();
   }

   @Override
   public Collection<ConceptInstance> getValues() {
      return ConceptInstanceUtil.getPropertyValues( this );
   }


   public boolean equals( final Object other ) {
      return other instanceof SimpleConceptInstance
            && ((SimpleConceptInstance) other)._annotation.equals( _annotation );
   }

   public int hashCode() {
      return _annotation.hashCode();
   }

   private EventProperties getEventProperties() {
      return ConceptInstance.getEventProperties( _annotation );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return toText();
   }

}