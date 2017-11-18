package org.apache.ctakes.cancer.ae;

import org.apache.ctakes.cancer.owl.OwlConstants;
import org.apache.ctakes.core.ontology.OwlOntologyConceptUtil;
import org.apache.ctakes.core.util.textspan.DefaultTextSpan;
import org.apache.ctakes.core.util.textspan.TextSpan;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/19/2017
 */
final public class SemanticSubsumer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SemanticSubsumer" );


   static private final Collection<Class<? extends IdentifiedAnnotation>> EVENT_CLASSES = Arrays.asList(
         MedicationMention.class, DiseaseDisorderMention.class,
         SignSymptomMention.class, LabMention.class, ProcedureMention.class );
   // Don't forget AnatomicalSiteMention.class and generic EntityMention.class!

   static private final Function<Annotation, TextSpan> createTextSpan
         = annotation -> new DefaultTextSpan( annotation.getBegin(), annotation.getEnd() );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Subsuming Mentions based upon Ontology ..." );
//      for ( Class<? extends IdentifiedAnnotation> eventClass : EVENT_CLASSES ) {
//         refineForClass( jcas, eventClass );
//      }
//      final Collection<AnatomicalSiteMention> anatomicals = JCasUtil.select( jcas, AnatomicalSiteMention.class );
//      final Collection<EntityMention> entityMentions = new ArrayList<>( JCasUtil.select( jcas, EntityMention.class ) );
//      entityMentions.removeAll( anatomicals );
//      refineForAnnotations( jcas, anatomicals );
//      refineForAnnotations( jcas, entityMentions );
      for ( String uriRoot : OwlConstants.SEMANTIC_ROOT_URIS ) {
         refineForBranch( jcas, uriRoot );
      }
      LOGGER.info( "Finished processing" );
   }


//   static private <T extends IdentifiedAnnotation> void refineForClass( final JCas jcas,
//                                                                        final Class<T> eventClass ) {
//      refineForAnnotations( jcas, JCasUtil.select( jcas, eventClass ) );
//   }

   static private <T extends IdentifiedAnnotation> void refineForBranch( final JCas jcas,
                                                                         final String rootUri ) {
      refineForAnnotations( jcas, OwlOntologyConceptUtil.getAnnotationsByUriBranch( jcas, rootUri ) );
   }


   static private <T extends IdentifiedAnnotation> void refineForAnnotations( final JCas jcas,
                                                                              final Collection<T> annotations ) {
      if ( annotations.isEmpty() ) {
         return;
      }
      final Map<TextSpan, List<T>> textSpanAnnotations =
            annotations.stream().collect( Collectors.groupingBy( createTextSpan ) );
      final Collection<TextSpan> unwantedSpans = getUnwantedSpans( textSpanAnnotations.keySet() );
      unwantedSpans.stream().map( textSpanAnnotations::get )
            .flatMap( Collection::stream )
            .forEach( IdentifiedAnnotation::removeFromIndexes );
   }

   static private Collection<TextSpan> getUnwantedSpans( final Collection<TextSpan> originalTextSpans ) {
      final List<TextSpan> textSpans = new ArrayList<>( originalTextSpans );
      final Collection<TextSpan> discardSpans = new HashSet<>();
      final int count = textSpans.size();
      for ( int i = 0; i < count; i++ ) {
         final TextSpan spanKeyI = textSpans.get( i );
         for ( int j = i + 1; j < count; j++ ) {
            final TextSpan spanKeyJ = textSpans.get( j );
            if ( (spanKeyJ.getBegin() <= spanKeyI.getBegin() && spanKeyI.getEnd() < spanKeyJ.getEnd())
                  || (spanKeyJ.getBegin() < spanKeyI.getBegin() && spanKeyI.getEnd() <= spanKeyJ.getEnd()) ) {
               // J contains I, discard less precise concepts for span I and move on to next span I
               discardSpans.add( spanKeyI );
               break;
            }
            if ( ((spanKeyI.getBegin() <= spanKeyJ.getBegin() && spanKeyJ.getEnd() < spanKeyI.getEnd())
                  || (spanKeyI.getBegin() < spanKeyJ.getBegin() && spanKeyJ.getEnd() <= spanKeyI.getEnd())) ) {
               // I contains J, discard less precise concepts for span J and move on to next span J
               discardSpans.add( spanKeyJ );
            }
         }
      }
      return discardSpans;
   }


}
