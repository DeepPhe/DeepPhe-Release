package org.apache.ctakes.cancer.concept.instance;

import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/17/2016
 */
final class SimpleConceptInstance extends AbstractConceptInstance {

   static private final Logger LOGGER = Logger.getLogger( "SimpleConceptInstance" );


   SimpleConceptInstance( final String patientId, final String documentId, final IdentifiedAnnotation annotation ) {
      super( patientId, Neo4jOntologyConceptUtil.getUri( annotation ) );
      addAnnotation( annotation, documentId, null );
   }


}