package org.healthnlp.deepphe.nlp.util;

import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;


/**
 * @author SPF , chip-nlp
 * @since {5/23/2024}
 */
@PipeBitInfo (
      name = "UriInfoCacheCleaner",
      description = "Cleans patient uri information from the UriInfoCache.",
      role = PipeBitInfo.Role.SPECIAL
)
public class UriInfoCacheCleaner extends JCasAnnotator_ImplBase {
   static private final Logger LOGGER = Logger.getLogger( "UriInfoCacheCleaner" );

   static public final String CLEAN_AFTER_DOC_PARAM = "CleanAfterDoc";
   static public final String CLEAN_AFTER_DOC_DESC =
         "Set to Yes to clean the URI cache after processing each document.";
   @ConfigurationParameter (
         name = CLEAN_AFTER_DOC_PARAM,
         description = CLEAN_AFTER_DOC_DESC,
         defaultValue = "no",
         mandatory = false
   )
   private String _cleanAtDoc;

   private boolean _cleanAfterDoc;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _cleanAfterDoc = AeParamUtil.isTrue( _cleanAtDoc );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      if ( _cleanAfterDoc ) {
         UriInfoCache.getInstance().clear();
         return;
      }
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
       if (PatientDocCounter.getInstance().isPatientFull(patientId)) {
           UriInfoCache.getInstance().clear();
       }
   }


}
