package org.apache.ctakes.core.store;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public interface ObjectCreator<T> {

   /**
    *
    * @param id unique object id.
    * @return an object created with the id.
    */
   T create( String id );

}
