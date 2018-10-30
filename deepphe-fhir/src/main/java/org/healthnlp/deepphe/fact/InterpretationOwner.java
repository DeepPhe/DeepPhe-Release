package org.healthnlp.deepphe.fact;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/25/2018
 */
public interface InterpretationOwner {

   Fact getInterpretation();

   void setInterpretation( Fact interpretation );

   default void setOrAppendInterpretation( final Fact interpretation ) {
      if ( getInterpretation() == null ) {
         setInterpretation( interpretation );
      } else if ( interpretation != null ) {
         getInterpretation().append( interpretation );
      }
   }

   default String getInterpretationSnippet() {
      return ( getInterpretation() == null )
            ? ""
            : " | interpretation: " + getInterpretation().getSummaryText();
   }

}
