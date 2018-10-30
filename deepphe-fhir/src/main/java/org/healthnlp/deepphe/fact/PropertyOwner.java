package org.healthnlp.deepphe.fact;


import org.healthnlp.deepphe.util.FHIRUtils;

import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/28/2018
 */
public interface PropertyOwner {

   Map<String, String> getProperties();

   void setProperties( final Map<String, String> properties );

   default void addProperty( final String key, final String value ) {
      getProperties().put( key, value );
   }

   default String getProperty( final String key ) {
      final String value = getProperties().get( key );
      if ( value != null ) {
         return value;
      }
      return getProperties().get( FHIRUtils.FHIR_VALUE_PREFIX + key );
   }

   default void addProperties( final Map<String, String> properties ) {
      getProperties().putAll( properties );
   }

}
