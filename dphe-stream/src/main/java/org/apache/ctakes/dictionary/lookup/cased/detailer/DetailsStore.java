package org.apache.ctakes.dictionary.lookup.cased.detailer;


import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/17/2020
 */
public enum DetailsStore {
   INSTANCE;

   static public DetailsStore getInstance() {
      return INSTANCE;
   }


   private final Collection<TermDetailer> _detailers = new ArrayList<>();

   public boolean addDetailer( final TermDetailer detailer ) {
      final String name = detailer.getName();
      synchronized ( _detailers ) {
         final boolean present = _detailers.stream()
                                           .map( TermDetailer::getName )
                                           .anyMatch( name::equals );
         if ( present ) {
            // Encoder with given name already exists.
            return false;
         }
         _detailers.add( detailer );
         return true;
      }
   }


   public Collection<TermDetailer> getDetailers() {
      return _detailers;
   }

}
