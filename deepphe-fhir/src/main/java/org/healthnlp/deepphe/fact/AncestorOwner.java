package org.healthnlp.deepphe.fact;


import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/28/2018
 */
public interface AncestorOwner {

   Collection<String> getAncestors();

   default void setAncestors( final Collection<String> ancestors ) {
      getAncestors().addAll( ancestors );
   }

   /**
    * @param ancestor a uri for an ancestor of this fact's uri in the ontology
    */
   default void addAncestor( final String ancestor ) {
      getAncestors().add( ancestor );
   }

}
