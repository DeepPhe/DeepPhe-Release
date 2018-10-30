package org.apache.ctakes.cancer.ae.section;


import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/28/2016
 */
final public class NonCancerSectionRemover extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "NonCancerSectionRemover" );

//  After Drools CS SUMMARY:CancerSummary (CancerSummary-Breast):
//	Body Site:
//		Breast | modifier: Left | modifier: Upper_Outer_Quadrant_of_the_Breast | modifier: 12_30_o_clock_position
//	Cancer Cell Line:
//		Carcinoma
//	Diagnosis:
//_______________________________________________________
//TumorSummary (TumorSummary-MergedTumor-1451218267):
//	Tumor Type:
//		PrimaryTumor
//	Histologic Type:
//		Ductal
//	Body Site:
//		Breast | modifier: Left | modifier: Upper_Outer_Quadrant_of_the_Breast | modifier: 12_30_o_clock_position
//	Cancer Cell Line:
//		Carcinoma
//	Receptor Status:
//		Progesterone_Receptor_Negative | method: Modified_Radical_Mastectomy
//		Estrogen_Receptor_Negative | method: Modified_Radical_Mastectomy
//		HER2_Neu_Negative | method: Modified_Radical_Mastectomy
//	Diagnosis:
//		Ductal_Breast_Carcinoma
//	Tumor_Size:
//		1.2 cm
//
//_______________________________________________________
//TumorSummary (TumorSummary-MergedTumor-1930237496):
//	Tumor Type:
//		Regional_Metastasis
//	Histologic Type:
//		Epithelial_Stromal
//	Body Site:
//		Axillary_Lymph_Node | modifier: Left
//	Calcification:
//		Calcification
//	Cancer Cell Line:
//		Carcinoma
//	Diagnosis:
//		Metastatic_Carcinoma
//	Tumor_Size:
//		2.5 cm

   /**
    * Where Sentences are in certain unwanted sections, they are removed.
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Temporarily removing non-cancer related Sections ..." );
      final Collection<Segment> removalSections = getNonCancerSections( jcas );
      if ( removalSections == null || removalSections.isEmpty() ) {
         LOGGER.info( "Finished Processing" );
         return;
      }
      // TODO add removed annotations to an "AnnotationHolder" for later replacement?
//      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jcas );
      final Collection<Annotation> removalAnnotations = new HashSet<>();
      for ( Segment section : removalSections ) {
//         SectionHolder.getInstance().addHiddenSection( documentId, section );
         LOGGER.info( "Clearing non-cancer section " + section.getTagText() );
         removalAnnotations.add( section );
         removalAnnotations.addAll( JCasUtil.selectCovered( jcas, Annotation.class, section ) );
      }
      removalAnnotations.forEach( Annotation::removeFromIndexes );
      LOGGER.info( "Finished Processing" );
   }


   static private Collection<Segment> getNonCancerSections( final JCas jCas ) {
      final Collection<Segment> removalSections = new ArrayList<>();

      final Collection<String> tumorCancerUris = new ArrayList<>( UriConstants.getTumorUris() );
      tumorCancerUris.addAll( UriConstants.getCancerUris() );

      final Map<Segment, Collection<IdentifiedAnnotation>> sectionAnnotations
            = JCasUtil.indexCovered( jCas, Segment.class, IdentifiedAnnotation.class );

      for ( Map.Entry<Segment, Collection<IdentifiedAnnotation>> entry : sectionAnnotations.entrySet() ) {
         boolean cancerSection = false;
         for ( IdentifiedAnnotation annotation : entry.getValue() ) {
            for ( String uri : Neo4jOntologyConceptUtil.getUris( annotation ) ) {
               if ( tumorCancerUris.contains( uri ) ) {
                  cancerSection = true;
                  break;
               }
            }
            if ( cancerSection ) {
               break;
            }
         }
         if ( !cancerSection ) {
            removalSections.add( entry.getKey() );
         }
      }
      return removalSections;
   }


}
