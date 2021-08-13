package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2020
 */
@PipeBitInfo(
      name = "DistinctValueFinder",
      description = "For deepphe.", role = PipeBitInfo.Role.ANNOTATOR
)
final public class DistinctValueFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DistinctValueFinder" );

   // ER	Positive; negative; equivocal; indeterminate	SSDI VALUES: NEGATIVE, POSITIVE, INDETERMINATE
// PR	Positive; negative; equivocal; indeterminate	SSDI VALUES: NEGATIVE, POSITIVE, INDETERMINATE
// HER2	Positive; negative; equivocal; indeterminate	SSDI VALUES: NEGATIVE, POSITIVE, EQUIVOCAL, INDETERMINATE
// Ki67	%; positive	SSDI VALUE:  % POSITIVE
// Msi	High (MSI-high); low (MSI-low) ADDS- STABLE, UNSTABLE NEGATIVE, INTACT NUCLEAR EXPRESSION, NO LOSS OF NUCLEAR EXPRESSION, LOSS OF NUCLEAR EXPRESSION	SSDI VALUES (TERMS): STABLE, NEGATIVE, INTACT NUCLEAR EXPRESSION, NO LOSS OF NUCLEAR EXPRESSION, UNSTABLE, INDETERMINATE, MSI-L, MSI-H, UNSTABLE LOW, UNSTABLE HIGH
// Kras	Positive; negative; present; not present; detected; not detected; mutant; not mutant, ADD WILD TYPE	SSDI VALUES:  NORMAL (WILD TYPE) ABNORMAL (MUTATED)
// PSA	Positive; negative; elevated; numerical	SSDI VALUE: NUMERICAL IS RECORDED

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Processing ..." );
      final Collection<Sentence> sentences = JCasUtil.select( jCas, Sentence.class );
      for ( Sentence sentence : sentences ) {


      }
   }


//   static public void addReceptorStatuses( final JCas jcas, final AnnotationFS lookupWindow ) {
//      final String windowText = lookupWindow.getCoveredText();
//      final List<SimpleStatus> statuses = getReceptorStatuses( windowText );
//      final Collection<SimpleStatus> statuses2 = getReceptorStatuses2( windowText );
//      statuses.addAll( statuses2 );
//      if ( statuses.isEmpty() ) {
//         return;
//      }
//      final int windowStartOffset = lookupWindow.getBegin();
//      for ( SimpleStatus status : statuses ) {
//         UriAnnotationFactory.createIdentifiedAnnotations( jcas,
//               windowStartOffset + status._begin,
//               windowStartOffset + status._end, status._uri, SemanticGroup.LAB, "T034" );
//      }
//   }


}
