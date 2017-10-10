package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.cancer.owl.OwlRelationUtil;
import org.apache.ctakes.cancer.owl.UriAnnotationCache;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/6/2017
 */
final public class OwlRelationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "OwlRelationFinder" );

   /**
    * Adds relation types registered in the ontology for neoplasms
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Relations defined in the Ontology ..." );
      final Map<String, Collection<IdentifiedAnnotation>> uriAnnotationMap = new HashMap<>();
      // iterate over paragraphs
      final Collection<Paragraph> paragraphs = JCasUtil.select( jcas, Paragraph.class );
      for ( Paragraph paragraph : paragraphs ) {
         uriAnnotationMap.putAll( OwlOntologyConceptUtil.getUriAnnotationsByUriBranch( jcas, paragraph, OwlConstants.BODY_SITE_URI ) );
         uriAnnotationMap.putAll( OwlOntologyConceptUtil.getUriAnnotationsByUriBranch( jcas, paragraph, OwlConstants.CONDITION_URI ) );
         uriAnnotationMap.putAll( OwlOntologyConceptUtil.getUriAnnotationsByUriBranch( jcas, paragraph, OwlConstants.PROCEDURE_URI ) );
         uriAnnotationMap.putAll( OwlOntologyConceptUtil.getUriAnnotationsByUriBranch( jcas, paragraph, OwlConstants.MEDICATION_URI ) );
         uriAnnotationMap.putAll( OwlOntologyConceptUtil.getUriAnnotationsByUriBranch( jcas, paragraph, OwlConstants.BODY_MODIFIER_URI ) );
         uriAnnotationMap.putAll( OwlOntologyConceptUtil.getUriAnnotationsByUriBranch( jcas, paragraph, OwlConstants.OBSERVATION_URI ) );
         uriAnnotationMap.putAll( OwlOntologyConceptUtil.getUriAnnotationsByUriBranch( jcas, paragraph, OwlConstants.SIZE_URI ) );
         if ( uriAnnotationMap.isEmpty() ) {
            continue;
         }
         for ( Map.Entry<String, Collection<IdentifiedAnnotation>> uriAnnotations : uriAnnotationMap.entrySet() ) {
            final String uri = uriAnnotations.getKey();
            final Map<String, Set<String>> uriRelations = UriAnnotationCache.getInstance().getUriRelations( uri );
            if ( uriRelations.isEmpty() ) {
               continue;
            }
            for ( Map.Entry<String, Set<String>> relation : uriRelations.entrySet() ) {
               final String relationName = relation.getKey();
               final Collection<String> relatedUris = relation.getValue();
               final Collection<String> relatableUris = OwlRelationUtil.getRelatableUris( uriAnnotationMap.keySet(), relatedUris );
               if ( relatableUris.isEmpty() ) {
                  continue;
               }
               final List<IdentifiedAnnotation> candidates = relatableUris.stream()
                     .map( uriAnnotationMap::get )
                     .flatMap( Collection::stream )
                     .sorted( ( a1, a2 ) -> a1.getBegin() - a2.getBegin() )
                     .collect( Collectors.toList() );
               final List<IdentifiedAnnotation> annotations = new ArrayList<>( uriAnnotations.getValue() );
               annotations.sort( ( a1, a2 ) -> a1.getBegin() - a2.getBegin() );
               final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> candidateMap
                     = RelationUtil.createCandidateMap( jcas, candidates, annotations );
               for ( Map.Entry<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> entry : candidateMap.entrySet() ) {
                  final IdentifiedAnnotation related = entry.getKey();
                  entry.getValue().forEach( n -> RelationUtil.createRelation( jcas, n, related, relationName ) );
//                  entry.getValue().forEach( arg -> LOGGER.info( arg.getCoveredText() + " " + relationName + " " + related.getCoveredText() ) );
               }
            }
         }
         uriAnnotationMap.clear();
      }
      LOGGER.info( "Finished Processing" );
   }


}
