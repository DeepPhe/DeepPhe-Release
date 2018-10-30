package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/9/2016
 */
final class CorefConceptInstance extends AbstractConceptInstance {

   static private final Logger LOGGER = Logger.getLogger( "CorefConceptInstance" );


   CorefConceptInstance( final String patientId, final String uri,
                         final Map<String, Collection<IdentifiedAnnotation>> annotations ) {
      super( patientId, uri );
      for ( Map.Entry<String, Collection<IdentifiedAnnotation>> docAnnotations : annotations.entrySet() ) {
         final String documentId = docAnnotations.getKey();
         docAnnotations.getValue().forEach( a -> addAnnotation( a, documentId, null ) );
      }
   }


}
