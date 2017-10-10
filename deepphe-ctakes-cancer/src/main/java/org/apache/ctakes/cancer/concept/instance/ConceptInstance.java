package org.apache.ctakes.cancer.concept.instance;

import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/9/2016
 */
public interface ConceptInstance {

   /**
    * @return the url of the instance
    */
   String getUri();

   /**
    * @return all represented Identified Annotations
    */
   Collection<IdentifiedAnnotation> getAnnotations();

   /**
    * @return true if the instance is negated: "not stage 2"
    */
   boolean isNegated();

   /**
    * @return true if the instance is uncertain "might be stage 2"
    */
   boolean isUncertain();

   /**
    * @return true if the instance is hypothetical "testing may indicate stage 2"
    */
   boolean isGeneric();

   /**
    * @return true if the instance is conditional "if positive then stage 2"
    */
   boolean isConditional();

   /**
    * @return true if the instance is in patient history
    */
   boolean inPatientHistory();

   /**
    * @return string representation of subject
    */
   String getSubject();

   /**
    * @return Before, Before/Overlap, Overlap, After
    */
   String getDocTimeRel();

   /**
    * @return Actual, hypothetical, hedged, generic
    */
   String getModality();

   /**
    * @return true if is an intermittent event
    */
   boolean isIntermittent();

   /**
    * @return true if it is a permanent event
    */
   boolean isPermanent();

   /**
    * @return text in note
    */
   String getCoveredText();

   /**
    * @return values for the concept.  4 mm for size, positive for ER, 3A for stage
    */
   Collection<ConceptInstance> getValues();

   /**
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
    * @return text indicating state of negated, uncertain, etc.
    */
   default String toText() {
      final StringBuilder sb = new StringBuilder();
      sb.append( getClass().getSimpleName() ).append( ": " )
            .append( getCoveredText() )
            .append( " " ).append( getUri() )
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
      return sb.toString();
   }

}
