package org.apache.ctakes.ner.detail;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class InMemoryDetailer implements Detailer {

   private final String _name;

   // Map of uri to encodings for the uri.
   private final Map<String, Collection<Details>> _encodingMap;

   /**
    * @param name        unique name for Encoder.
    * @param encodingMap Map with uri as key, and TermEncoding Collection as value.
    */
   public InMemoryDetailer( final String name, final Map<String, Collection<Details>> encodingMap ) {
      _name = name;
      _encodingMap = encodingMap;
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
    */
   @Override
   public Collection<Details> getDetails( final String uri ) {
      return _encodingMap.getOrDefault( uri, Collections.emptyList() );
   }


}
