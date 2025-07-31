package org.apache.ctakes.ner.dictionary;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/13/2020
 */
public enum DictionaryStore {
   INSTANCE;

   static public DictionaryStore getInstance() {
      return INSTANCE;
   }


   private final Collection<Dictionary> _dictionaries = new ArrayList<>();

   public boolean addDictionary( final Dictionary dictionary ) {
      final String name = dictionary.getName();
      synchronized ( _dictionaries ) {
         final boolean present = _dictionaries.stream()
                                              .map( Dictionary::getName )
                                              .anyMatch( name::equals );
         if ( present ) {
            // Dictionary with given name already exists.
            return false;
         }
         _dictionaries.add( dictionary );
         return true;
      }
   }


   public Collection<Dictionary> getDictionaries() {
      return _dictionaries;
   }


}
