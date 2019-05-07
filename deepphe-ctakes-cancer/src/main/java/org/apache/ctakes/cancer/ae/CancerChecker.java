package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.note.NoteSpecs;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.SourceMetadataUtil;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/31/2018
 */
@PipeBitInfo(
      name = "CancerChecker",
      description = "For deepphe.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class CancerChecker extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "CancerChecker" );

   // TODO  Add cancer-to-disease threshold in docType enums - or even SectType enums
   // TODO  Add cancer-weight to sectType enums, maybe even DocType enums


   /**
    * Checks note content to see if tumors are over half of the mentioned diseases.
    * If tumors are not in the majority then annotations are cleared from the jcas to prevent following annotators from processing it.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Verifying note is Cancer-centric ..." );

      if ( !isCancerNote( jCas ) ) {
         LOGGER.info( DocumentIDAnnotationUtil.getDocumentID( jCas ) + " is not a cancer note." );
         final Collection<Annotation> annotations = JCasUtil.select( jCas, Annotation.class );
         annotations.forEach( Annotation::removeFromIndexes );
      }
   }


   static private boolean isCancerNote( final JCas jCas ) {
      final Collection<String> tumorCancerUris = new ArrayList<>(UriConstants.getTumorUris() );
      final Map<String, Collection<IdentifiedAnnotation>> allTumors
            = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, tumorCancerUris );
      if ( allTumors.isEmpty() ) {
         return false;
      }
      final int tumorTotal = allTumors.values().stream().mapToInt( Collection::size ).sum();
      if ( tumorTotal <= 2 ) {
         return false;
      }

      final Map<String, Collection<IdentifiedAnnotation>> allDiseases
            = Neo4jOntologyConceptUtil.getUriAnnotationsByUriBranch( jCas, UriConstants.DISEASE );
      final Map<String,Collection<String>> collatedDiseases = ByUriRelationFinder.collateUris( allDiseases.keySet() );

      int frequentDisease = 0;
      for ( Collection<String> diseaseUris : collatedDiseases.values() ) {
         final int diseaseCount = diseaseUris.stream().map( allDiseases::get ).mapToInt( Collection::size ).sum();
         frequentDisease = Math.max( frequentDisease, diseaseCount );
      }

      return tumorTotal > frequentDisease/3;
   }


   private static String getDocumentType( final SourceData sourceData ) {
      if ( sourceData == null ) {
         return NoteSpecs.ID_NAME_CLINICAL_NOTE;
      } else {
         String sourceType = sourceData.getNoteTypeCode();
         return sourceType == null ? NoteSpecs.ID_NAME_CLINICAL_NOTE : sourceType;
      }
   }

   /**
    *
    * @param jCas ye olde ...
    * @return map of diseases to counts for the existing branch of related diseases.
    */
   static private Map<IdentifiedAnnotation,Integer> getDiseaseCounts( final JCas jCas ) {
      final Map<String, Collection<IdentifiedAnnotation>> allDiseases
            = Neo4jOntologyConceptUtil.getUriAnnotationsByUriBranch( jCas, UriConstants.DISEASE );
      final Map<String,Collection<String>> collatedDiseases = ByUriRelationFinder.collateUris( allDiseases.keySet() );
      final Map<IdentifiedAnnotation,Integer> diseaseCounts = new HashMap<>();
      for ( Collection<String> relatedDiseases : collatedDiseases.values() ) {
         final Collection<IdentifiedAnnotation> allRelated
               = relatedDiseases.stream()
                                .map( allDiseases::get )
                                .flatMap( Collection::stream )
                                .collect( Collectors.toList() );
         allRelated.forEach( s -> diseaseCounts.put( s, allRelated.size() ) );
      }
      return diseaseCounts;
   }

   /**
    *
    * @param jCas ye olde ...
    * @return map of tumors and cancers to counts for the existing branch of related tumors.
    */
   static private Map<IdentifiedAnnotation,Integer> getTumorCounts( final JCas jCas ) {
      final Collection<String> tumorCancerUris = new ArrayList<>(UriConstants.getTumorUris() );
      final Map<String, Collection<IdentifiedAnnotation>> allTumors
            = Neo4jOntologyConceptUtil.getUriAnnotationsByUris( jCas, tumorCancerUris );
      final Map<String,Collection<String>> collatedTumors = ByUriRelationFinder.collateUris( allTumors.keySet() );
      final Map<IdentifiedAnnotation,Integer> tumorCounts = new HashMap<>();
      for ( Collection<String> relatedTumors : collatedTumors.values() ) {
         final Collection<IdentifiedAnnotation> allRelated
               = relatedTumors.stream()
                              .map( allTumors::get )
                              .flatMap( Collection::stream )
                              .collect( Collectors.toList() );
         allRelated.forEach( s -> tumorCounts.put( s, allRelated.size() ) );
      }
      return tumorCounts;
   }


}
