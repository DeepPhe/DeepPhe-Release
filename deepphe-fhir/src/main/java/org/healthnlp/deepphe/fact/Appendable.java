package org.healthnlp.deepphe.fact;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/28/2018
 */
@FunctionalInterface
public interface Appendable<T> {

   /**
    * @param appendable -
    * @return true if the summary class is equal to the Runtime instance of this appendable.
    */
   default boolean isAppendable( final T appendable ) {
      return getClass().isInstance( appendable );
   }

   /**
    * inserts all facts in the given summary into this one
    *
    * @param appendable -
    */
   void append( final T appendable );

}
