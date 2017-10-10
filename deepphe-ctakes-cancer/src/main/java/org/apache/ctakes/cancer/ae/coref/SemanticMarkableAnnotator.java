package org.apache.ctakes.cancer.ae.coref;

import org.apache.ctakes.cancer.util.MarkableHolder;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/18/2016
 */
// TODO swap this out for owl uri roots
final public class SemanticMarkableAnnotator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SemanticMarkableAnnotator" );

   static public final String PARAM_ANNOTATION_CLASS = "AnnotationClass";

   @ConfigurationParameter(
         name = PARAM_ANNOTATION_CLASS,
         description = "Class of Identified Annotation to use",
         defaultValue = { "org.apache.ctakes.typesystem.type.textsem.EventMention" }
   )
   private String _annotationClassName;

   private Class<? extends IdentifiedAnnotation> _annotationClass;

   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         final Class<?> annotationClass = Class.forName( _annotationClassName );
         _annotationClass = annotationClass.asSubclass( IdentifiedAnnotation.class );
      } catch ( ClassNotFoundException | ClassCastException cnfE ) {
         LOGGER.error( "Could not get class for name " + _annotationClassName );
         throw new ResourceInitializationException( cnfE );
      }
   }

   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String documentId = DocumentIDAnnotationUtil.getDocumentID( jCas );
      final Map<Markable, IdentifiedAnnotation> markables
            = JCasUtil.select( jCas, _annotationClass ).stream()
            .filter( a -> a.getCoveredText().length() > 1 )
            .collect( Collectors.toMap( a -> new Markable( jCas, a.getBegin(), a.getEnd() ), Function.identity() ) );
      // add markables to cas, add them to MarkableHolder
      markables.keySet().forEach( Markable::addToIndexes );
      MarkableHolder.addMarkables( documentId, markables );
   }

}
