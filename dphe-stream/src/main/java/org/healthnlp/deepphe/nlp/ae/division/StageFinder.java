package org.healthnlp.deepphe.nlp.ae.division;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.jcas.NoteDivisions;
import org.apache.ctakes.core.util.paragraph.ParagraphProcessor;
import org.apache.ctakes.core.util.section.AbstractSectionProcessor;
import org.apache.ctakes.core.util.section.SectionProcessor;
import org.apache.ctakes.core.util.sentence.SentenceProcessor;
import org.apache.ctakes.core.util.topic.TopicProcessor;
import org.apache.ctakes.core.util.treelist.ListProcessor;
import org.apache.ctakes.typesystem.type.textspan.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {4/15/2022}
 */
@PipeBitInfo(
      name = "StageFinder",
      description = "Finds TNM, Stage and Grade values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class StageFinder extends AbstractSectionProcessor implements SectionProcessor,
                                                                           ListProcessor,
                                                                           SentenceProcessor {

   static private final Logger LOGGER = Logger.getLogger( "StageFinder" );

//*Combined Histologic grade: Intermediate
//   Pathologic staging (pTNM): IIA
//      Primary tumor (pT): pT2
//      Regional lymph nodes (pN): pN0
//Histologic Grade:                    Tubular Formation Score: 2
//                                     Nuclear Pleomorphism Score: 3
//                                     Mitotic Rate Score: 2 (intermediate proliferative rate)
//                                     Combined Histologic Grade: 2
//                                     Greatest dimension of largest focus of invasion over 1 mm: 13 mm
//Primary Tumor (pT):                  pT1c
//Regional Lymph Nodes (pN):           pN0
//Histologic Grade:                Tubular Formation Score: 1
//                                 Nuclear Pleomorphism Score: 2
//                                 Mitotic Rate Score: 1 (low proliferative rate)
//                                 Combined Histologic Grade: 1
//                                 Greatest dimension of largest focus of invasion over 1 mm: 5 mm
//Primary Tumor (pT):              pT1a
//Regional Lymph Nodes (pN):       pN0

//   --> NEGATE::  Associated Ductal Carcinoma in Situ type and grade: cribriform and solid
//   --> NEGATE::  DCIS Nuclear Grade:                  Intermediate
//   DCIS Nuclear Grade:              Low

//   --> Scale/System Hint::   *Elston and Ellis modification of Scarff-Bloom-Richardson Grading System

//   Histologic grade: high grade

//   Pathologic staging (pTNM [FIGO]): Stage IIIB
//     Primary tumor (pT): pT3b
//     Regional lymph nodes (pN): pNx
//     Distant metastases (pM): n/a

//Histologic Grade:  G1: Well differentiated

//PATHOLOGIC STAGE CLASSIFICATION (pTNM, AJCC 8th Edition)
//TNM Descriptors:  y (post-treatment)
//Primary Tumor (pT):  pT1a
//Regional Lymph Nodes (pN):  pNX
//FIGO STAGE
//FIGO Stage:  IA

//Histologic Grade:  FIGO grade 2

//PATHOLOGIC STAGE CLASSIFICATION (pTNM, AJCC 8th Edition)
//TNM Descriptors:  y (post-treatment)
//Primary Tumor (pT):  pT1a
//Regional Lymph Nodes (pN):  pNX
//FIGO STAGE
//FIGO Stage:  IA

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Grade Values ..." );
      super.process( jCas );
   }


   @Override
   public TopicProcessor getTopicProcessor() {
      return null;
   }

   @Override
   public ParagraphProcessor getParagraphProcessor() {
      return null;
   }

   @Override
   public ListProcessor getListProcessor() {
      return this;
   }

   @Override
   public SentenceProcessor getSentenceProcessor() {
      return this;
   }

   @Override
   public Collection<Pair<Integer>> processSentence( final JCas jCas, final Segment section, final Topic topic,
                                                     final Paragraph paragraph,
                                                     final Sentence sentence, final NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      LOGGER.info( "Processed Spans: " + jCas.getDocumentText()
                                             .length() + "\n"
                   + noteDivisions.getProcessedSpans()
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() + " " )
                                  .collect( Collectors.joining( " ; " ) ) );
      LOGGER.info( "Available Spans: " + jCas.getDocumentText()
                                             .length() + "\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );
      final String text = sentence.getCoveredText();

      return Collections.emptyList();
   }

   @Override
   public Collection<Pair<Integer>> processList( final JCas jCas, final Segment section, final Topic topic,
                                                 final Paragraph paragraph,
                                                 final FormattedList list, final NoteDivisions noteDivisions )
         throws AnalysisEngineProcessException {
      LOGGER.info( "List Type " + list.getListType() );
      LOGGER.info( "DocText length: " + jCas.getDocumentText()
                                            .length() + " Processed spans:\n"
                   + noteDivisions.getProcessedSpans()
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );

      LOGGER.info( "DocText length: " + jCas.getDocumentText()
                                            .length() + " Available spans:\n"
                   + noteDivisions.getAvailableSpans( jCas )
                                  .stream()
                                  .map( s -> s.getValue1() + "," + s.getValue2() )
                                  .collect( Collectors.joining( " ; " ) ) );

      return Collections.emptyList();
   }

}
