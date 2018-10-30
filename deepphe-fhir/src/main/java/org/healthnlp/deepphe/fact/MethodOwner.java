package org.healthnlp.deepphe.fact;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/25/2018
 */
public interface MethodOwner {

   Fact getMethod();

   void setMethod( Fact method );

   default void setOrAppendMethod( final Fact method ) {
      if ( getMethod() == null ) {
         setMethod( method );
      } else if ( method != null ) {
         getMethod().append( method );
      }
   }

   default String getMethodSnippet() {
      return ( getMethod() == null )
            ? ""
            : " | method: " + getMethod().getSummaryText();
   }

}
