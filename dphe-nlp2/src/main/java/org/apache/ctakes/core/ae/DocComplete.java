package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * @author SPF , chip-nlp
 * @since {6/7/2024}
 */
@PipeBitInfo(
        name = "DocComplete",
        description = "Informs cTAKES that the document processing is complete.  Important for AEs relying upon doc or patient completion.",
        role = PipeBitInfo.Role.SPECIAL
)
public class DocComplete extends JCasAnnotator_ImplBase {
   static private final Logger LOGGER = Logger.getLogger("DocComplete");

   static public final String CACHE_DOC_PARAM = "CacheDoc";
   static public final String CACHE_DOC_DESC = "Cache the document CAS in a patient CAS.  Only useful for full patient processing.";
   @ConfigurationParameter(
           name = CACHE_DOC_PARAM,
           description = CACHE_DOC_DESC,
           defaultValue = "no",
           mandatory = false
   )
   private String _cacheDoc;

   private boolean _storeNote = false;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize(final UimaContext context) throws ResourceInitializationException {
      super.initialize(context);
      _storeNote = AeParamUtil.isTrue(_cacheDoc);
   }

   /**
    * Adds the primary view of this cas to a cache of views for patients.
    * See {@link PatientNoteStore}
    * {@inheritDoc}
    */
   @Override
   public void process(final JCas jCas) throws AnalysisEngineProcessException {
      final String patientId = PatientNoteStore.getDefaultPatientId(jCas);
      if (_storeNote) {
         LOGGER.info("Caching Document " + PatientNoteStore.getDefaultDocumentId(jCas)
                 + " into Patient " + patientId + " ...");
         PatientNoteStore.getInstance().storeAllViews(jCas);
      }
      final int count = PatientDocCounter.getInstance().incrementProcessedDocCount(patientId);
      LOGGER.info("Documents processed for " + patientId + ": " + count + " / "
              + PatientNoteStore.getInstance().getWantedDocCount(patientId));
   }


}
