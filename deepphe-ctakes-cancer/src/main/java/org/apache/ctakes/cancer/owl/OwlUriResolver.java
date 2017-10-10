package org.apache.ctakes.cancer.owl;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.logging.Logger;

import static org.apache.ctakes.cancer.owl.OwlConstants.BREAST_CANCER_OWL;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/1/2017
 */
final public class OwlUriResolver extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "OwlUriResolver" );

   static public final String ONTOLOGY_URI = "ontologyUri";
   static public final String DEFAULT_OWL_URI = BREAST_CANCER_OWL;

   static private String ontologyURI = DEFAULT_OWL_URI;

   @ConfigurationParameter(
         name = ONTOLOGY_URI,
         description = "URI of root ontology.",
         defaultValue = { DEFAULT_OWL_URI }
   )
   private String _ontologyUri;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      ontologyURI = _ontologyUri;
   }

   /**
    * does nothing
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
   }

   static public String getBaseUri() {
      return ontologyURI;
   }

   static public String resolveUri( final String extension ) {
      return getBaseUri() + "#" + extension;
   }


}
