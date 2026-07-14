package org.apache.ctakes.ner.detail;


import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public enum DetailerStore {
   INSTANCE;

   static public DetailerStore getInstance() {
      return INSTANCE;
   }


   private final Collection<Detailer> _detailers = new ArrayList<>();

   public boolean addDetailer( final Detailer detailer ) {
      final String name = detailer.getName();
      synchronized ( _detailers ) {
         final boolean present = _detailers.stream()
                                           .map( Detailer::getName )
                                           .anyMatch( name::equals );
         if ( present ) {
            // Encoder with given name already exists.
            return false;
         }
         _detailers.add( detailer );
         return true;
      }
   }


   public Collection<Detailer> getDetailers() {
      return _detailers;
   }

}
