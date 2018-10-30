package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 6/10/2016
 */
final public class DistancedRemover extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DistancedRemover" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Removing misidentified Locations ..." );
      final String documentText = jcas.getDocumentText();
      final Collection<LocationOfTextRelation> locationOfs = JCasUtil.select( jcas, LocationOfTextRelation.class );
      if ( locationOfs == null || locationOfs.isEmpty() ) {
         LOGGER.info( "Finished Processing" );
         return;
      }
      final Collection<String> neoplasmUris = Arrays.asList( UriConstants.NEOPLASM );
      final Collection<IdentifiedAnnotation> neoplasms = neoplasmUris.stream()
                                                                     .map( u -> Neo4jOntologyConceptUtil
                                                                           .getAnnotationsByUriBranch( jcas, u ) )
                                                                     .flatMap( Collection::stream )
                                                                     .collect( Collectors.toSet() );
      if ( neoplasms.isEmpty() ) {
         LOGGER.info( "Finished Processing" );
         return;
      }
      final Collection<LocationOfTextRelation> removals = new HashSet<>();
      for ( IdentifiedAnnotation neoplasm : neoplasms ) {
         final Collection<LocationOfTextRelation> neoplasmLocationOfs = RelationUtil.getRelationsAsFirst( locationOfs, neoplasm );
         if ( neoplasmLocationOfs == null || neoplasmLocationOfs.isEmpty() ) {
            continue;
         }
         for ( LocationOfTextRelation locationOf : neoplasmLocationOfs ) {
            final Annotation location = locationOf.getArg2().getArgument();
            if ( location.getBegin() > neoplasm.getEnd() ) {
               final String between = documentText.substring( neoplasm.getEnd(), location.getBegin() ).toLowerCase();
               if ( between.contains( "from" )
                     || between.contains( "superior" ) || between.contains( "inferior" )
                     || between.contains( "anterior" ) || between.contains( "posterior" ) ) {
                  removals.add( locationOf );
               }
            }
         }
      }
      removals.forEach( LocationOfTextRelation::removeFromIndexes );
      LOGGER.info( "Finished Processing" );
   }


}
