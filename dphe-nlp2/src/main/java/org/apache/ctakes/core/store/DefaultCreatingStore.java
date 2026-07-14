package org.apache.ctakes.core.store;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2020
 */
public class DefaultCreatingStore<T> extends DefaultObjectStore<T> implements CreatingObjectStore<T> {


   private final ObjectCreator<T> _creator;

   public DefaultCreatingStore( final ObjectCreator<T> creator ) {
      _creator = creator;
   }

   /**
    *
    * @param id unique object id.
    * @return an object created with the id.
    */
   public T create( String id ) {
      return _creator.create( id );
   }


}
