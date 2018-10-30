package org.apache.ctakes.cancer.summary;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/13/2018
 */
@PipeBitInfo(
      name = "CiSummaryInitializer",
      description = "For deepphe.", role = PipeBitInfo.Role.SPECIAL
)
final public class CiSummaryInitializer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "CiSummaryInitializer" );

   static public final String USE_THRESHOLD_PARAMETERS = "UseThresholdParameters";
   static public final String ALL_CANCER_SITE_THRESHOLD = "AllCancerSiteThreshold";
   static public final String CANCER_SITE_THRESHOLD = "CancerSiteThreshold";
   static public final String PRIMARY_SITE_THRESHOLD = "PrimarySiteThreshold";
   static public final String METASTASIS_SITE_THRESHOLD = "MetastasisSiteThreshold";
   static public final String GENERIC_SITE_THRESHOLD = "GenericSiteThreshold";
   static public final String ALL_CANCER_TUMOR_THRESHOLD = "AllCancerTumorThreshold";
   static public final String CANCER_TUMOR_THRESHOLD = "CancerTumorThreshold";
   static public final String PRIMARY_TUMOR_THRESHOLD = "PrimaryTumorThreshold";
   static public final String METASTASIS_TUMOR_THRESHOLD = "MetastasisTumorThreshold";
   static public final String GENERIC_TUMOR_THRESHOLD = "GenericTumorThreshold";

   @ConfigurationParameter(
           name = USE_THRESHOLD_PARAMETERS,
           description = "Switches on or off the use of the other threshold parameters defined for this annotator.",
           mandatory = false
   )
   private boolean _useThresholdParameters = false;

   @ConfigurationParameter(
         name = ALL_CANCER_SITE_THRESHOLD
   )
   private String _allCancerSiteThreshold = "0";

   @ConfigurationParameter(
         name = CANCER_SITE_THRESHOLD
   )
   private String _cancerSiteThreshold = "0";

   @ConfigurationParameter(
         name = PRIMARY_SITE_THRESHOLD
   )
   private String _primarySiteThreshold = "0";

   @ConfigurationParameter(
         name = METASTASIS_SITE_THRESHOLD
   )
   private String _metastasisSiteThreshold = "0";

   @ConfigurationParameter(
         name = GENERIC_SITE_THRESHOLD
   )
   private String _genericSiteThreshold = "0";

   @ConfigurationParameter(
         name = ALL_CANCER_TUMOR_THRESHOLD
   )
   private String _allCancerTumorThreshold = "0";

   @ConfigurationParameter(
         name = CANCER_TUMOR_THRESHOLD
   )
   private String _cancerTumorThreshold = "0";

   @ConfigurationParameter(
         name = PRIMARY_TUMOR_THRESHOLD
   )
   private String _primaryTumorThreshold = "0";

   @ConfigurationParameter(
         name = METASTASIS_TUMOR_THRESHOLD
   )
   private String _metastasisTumorThreshold = "0";

   @ConfigurationParameter(
         name = GENERIC_TUMOR_THRESHOLD
   )
   private String _genericTumorThreshold = "0";




   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      // Always call the super first
      super.initialize( context );
      // do nothing
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      // do nothing
   }


}
