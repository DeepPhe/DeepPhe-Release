package org.healthnlp.deepphe.fact;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/25/2018
 */
public interface ValueOwner {

   Fact getValue();

   void setValue( Fact value );

   default void setOrAppendValue( final Fact value ) {
      if ( getValue() == null ) {
         setValue( value );
      } else if ( value != null ) {
         getValue().append( value );
      }
   }

   default String getValueSnippet() {
      return ( getValue() == null )
            ? ""
            : " | value: " + getValue().getSummaryText();
   }


}
