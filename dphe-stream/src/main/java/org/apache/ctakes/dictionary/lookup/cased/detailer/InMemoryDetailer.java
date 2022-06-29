package org.apache.ctakes.dictionary.lookup.cased.detailer;


import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class InMemoryDetailer implements TermDetailer {

   private final String _name;

   // Map of rare tokens to terms that contain those tokens.  Used like "First Word Token Lookup" but faster
   private final Map<String, Details> _detailsMap;

   /**
    * @param name        unique name for dictionary
    * @param detailsMap Map with a cui code as key, and TermEncoding Collection as value
    */
   public InMemoryDetailer( final String name, final Map<String, Details> detailsMap ) {
      _name = name;
      _detailsMap = detailsMap;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * {@inheritDoc}
    * @param cui
    * @return
    */
   @Override
   public Details getDetails( final String cui ) {
      return _detailsMap.getOrDefault( cui, Details.EMPTY_DETAILS );
   }


}
