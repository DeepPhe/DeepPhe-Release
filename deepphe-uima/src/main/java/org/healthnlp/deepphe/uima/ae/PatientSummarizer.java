package org.healthnlp.deepphe.uima.ae;

import org.apache.ctakes.cancer.summary.*;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.patient.AbstractPatientConsumer;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fact.Fact;
import org.healthnlp.deepphe.fact.FactFactory;
import org.healthnlp.deepphe.fact.FactHelper;
import org.healthnlp.deepphe.summary.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Summarize a patient from the note jcas views stored in a patient-level jcas.
 * Replaces old PhenotypeCancerSummaryAE / PhenotypeSummarizer
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2018
 */
@PipeBitInfo(
      name = "PatientSummarizer",
      description = "Summarize using Dphe Fhir resources at a patient level instead of document level.",
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class PatientSummarizer extends AbstractPatientConsumer {


   static private final boolean USE_DROOLS = false; //true;


   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

   public static final String DOMAIN = "Domain";

   static private final String DEBUG_DIR = ""; //"../../";
   private static boolean WRITE_DEBUG_BSVS = false;
   static {
      if (WRITE_DEBUG_BSVS) {
         LOGGER.info("WRITE_DEBUG_BSVS=" + WRITE_DEBUG_BSVS);
      }
   }
   @ConfigurationParameter(
           name = DOMAIN,
           description = "Determines the domain-specific rules to be used.",
           mandatory = false
   )
   private String domain;  // See FHIRConstants.DOMAINS

   public PatientSummarizer() {
      super( MethodHandles.lookup().lookupClass().getSimpleName(), "Summarizing Facts" );
   }

   /**
    * Call necessary processing for patient
    * <p>
    * {@inheritDoc}
    */
   @Override
   protected void processPatientCas( final JCas patientCas ) throws AnalysisEngineProcessException {

//      final String patientName = SourceMetadataUtil.getPatientIdentifier( patientCas );

//      final Map<CiSummary,Collection<CiSummary>> ciSummaries = CiSummaryFactory.createPatientDiagnoses2( patientCas );
//      final Collection<CancerCiSummary> cancers = CiSummaryFactory.createPatientDiagnoses2( patientCas );
//      CancerCiStore.getInstance().store( cancers );
      final PatientCiContainer patient = CiContainerFactory.createPatientSummary( patientCas );
      PatientCiContainerStore.getInstance().store( patient );

      patient.getCancers().forEach( LOGGER::info );

      ///////////////////////////////   Debug for Drools to Java-only comparison   ///////////////////////////
//      final Collection<CancerSummary> cancerSummaries
//            = SummaryFactory.createPatientDiagnosedSummaries( patientCas, new NoteSpecs( patientCas ) );
//      LOGGER.info( "Non-Drools Cancer Summaries for Patient: " + patientName );
//      cancerSummaries.forEach( s ->
//            LOGGER.info( "\n=============================================================\n"
//                         + s.getSummaryText() ) );
//      try {
//         AbstractCiSummariesWriter.INSTANCE.writeCancerSummaries( patientName, cancerSummaries );
//      } catch ( IOException ioE ) {
//         LOGGER.error( ioE.getMessage() );
//      }
//      return;

//      final MedicalRecord medicalRecord = new MedicalRecord( patientName );
//      final Collection<JCas> docJcases = PatientViewUtil.getAllViews( patientCas );
//
//      docJcases.stream()
//               .filter( Objects::nonNull )
//               .filter( j -> j.getDocumentText() != null )
//               .filter( j -> !j.getDocumentText().isEmpty() )
//               .filter( j -> !PatientViewUtil.getDefaultViewName().equals( j.getViewName() ) )
//               .forEach( n -> medicalRecord.addNote( n, USE_DROOLS ) );
//
//      final PatientSummary sourcePatientSummary = medicalRecord.getPatientSummary();
//      sourcePatientSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );
//      medicalRecord.setPatientSummaryWithoutFacts( sourcePatientSummary );
//
//      if ( !USE_DROOLS ) {
////         final Collection<CancerSummary> cancerSummaries
////               = SummaryFactory.createCancerSummaries( patientCas, new NoteSpecs( patientCas ) );
////         LOGGER.info( "Non-Drools Cancer Summaries for Patient: " + patientName );
////         cancerSummaries.forEach( s ->
////               LOGGER.info( "\n=============================================================\n"
////                            + s.getSummaryText() ) );
////         cancerSummaries.forEach( s -> s.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD ) );
////         cancerSummaries.forEach( medicalRecord::addCancerSummaryWithoutFacts );
//         final Collection<CancerSummary> cancerSummaries
//               = SummaryFactory.createPatientDiagnosedSummaries( patientCas, new NoteSpecs( patientCas ) );
//         LOGGER.info( "Non-Drools Cancer Summaries for Patient: " + patientName );
//         cancerSummaries.forEach( s ->
//               LOGGER.info( "\n=============================================================\n"
//                            + s.getSummaryText() ) );
//         cancerSummaries.forEach( s -> s.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD ) );
//         cancerSummaries.forEach( medicalRecord::addCancerSummaryWithoutFacts );
//
//      } else {
//
//         final CancerSummary sourceCancerSummary = new CancerSummary( medicalRecord.getPatientName() );
//         sourceCancerSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );
//
//         final CancerSummary tempSummary = new CancerSummary( medicalRecord.getPatientName() );
//         tempSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );
//
//         // Record to load into drools
//         medicalRecord.setCancerSummaryWithoutFacts( sourceCancerSummary );
//
//         // Each NoteSummary potentially has TumorSummary(s), add each of those to the medicalRecord and set summaryType for those facts since Drools is being driven by TumorSummary's
//         medicalRecord.addTumorSummariesFromNoteSummaries();
//
//         // check ancestors
//         final List<Fact> reportFacts = medicalRecord.getReportLevelFacts(); // aka NoteSummary facts
//         BodySiteFact.makeQuadrantsBodyModifier( reportFacts );
//
//         addMissingAncestors( reportFacts );
//
//         LOGGER.info( "PROCESSING phenotype ..." + sourcePatientSummary.getId() );
//
//         if ( WRITE_DEBUG_BSVS )
//            writeFactDebug( patientName, reportFacts );
//
//         // Insert record into drools
//         final DroolsEngine de = new DroolsEngine();
//         KieSession droolsSession = null;
//         try {
//            // duplicates care
//            Set<String> dupList = new HashSet<String>();
//
//            String rules;
//            if ( domain == null || domain.trim().isEmpty() ) {
//               rules = FHIRConstants.DOMAIN_BREAST;
//            } else {
//               if ( !FHIRConstants.DOMAINS.contains( domain ) ) {
//                  Exception rte = new RuntimeException( "Domain '" + domain + "' is unrecognized." );
//                  rte.printStackTrace();
//                  rules = FHIRConstants.DOMAIN_BREAST;
//               } else {
//                  rules = domain;
//               }
//            }
//            LOGGER.debug( "Using rules for domain: " + rules );
//            droolsSession = de.createSessionByKBaseName( rules.toUpperCase() );
//
//            // Insert new medical record
//            droolsSession.insert( new Domain( rules, UriConstants.DPHE_ROOT ) );
//            droolsSession.insert( medicalRecord );
//
//            for ( Fact f : reportFacts ) {
//               try {
//                  if ( dupList.add( f.getInfoDrools( false ) ) ) {
//                     droolsSession.insert( f );
//                  }
//               } catch ( NullPointerException e ) {
//               }
//            }
//            dupList.clear();
//            dupList = null;
//            droolsSession.fireAllRules();
//            droolsSession.dispose();
//
//         } catch ( Exception e ) {
//            e.printStackTrace();
//         }
//
//         // Fill out the facts that were omitted by the rules.
//         mergeCancerSummaries( sourceCancerSummary, tempSummary );
//         cleanSummaries( sourceCancerSummary );
//
//         if ( WRITE_DEBUG_BSVS ) {
//            final List<Fact> factsAfterDrools = medicalRecord.getReportLevelFacts(); // aka NoteSummary facts
//            writeFactDebug( patientName+"_after", factsAfterDrools );
//         }
//         LOGGER.info("\nDrools CS SUMMARY: " + sourceCancerSummary.getSummaryText());
//      }
//      // Store the medical record
//      // It is better to write custom datatypes directly as they are now at an end state and further translation
//      // requires processing time and introduces potential errors and data loss.
//      MedicalRecordStore.getInstance().storeMedicalRecord( medicalRecord );
//
////      ///////////////////////////////   Debug for Drools to Java-only comparison   ///////////////////////////
////      final Collection<CancerSummary> cancerSummaries
////            = SummaryFactory.createPatientDiagnosedSummaries( patientCas, new NoteSpecs( patientCas ) );
////      LOGGER.info( "Non-Drools Cancer Summaries for Patient: " + patientName );
////      cancerSummaries.forEach( s ->
////            LOGGER.info( "\n=============================================================\n"
////                         + s.getSummaryText() ) );
////      try {
////         DirectMedicalRecordBsvWriter.INSTANCE.writeCancerSummaries( patientName, cancerSummaries );
////      } catch ( IOException ioE ) {
////         LOGGER.error( ioE.getMessage() );
////      }
//
//      LOGGER.debug( "Done with summary text" );
   }

   private void mergeCancerSummaries( final CancerSummary cancerSummary, final CancerSummary tempSummary ) {
      addMissingFacts( cancerSummary, tempSummary );
      for ( TumorSummary tumorSummary : cancerSummary.getTumors() ) {
         TumorSummary tempTumor = getMatchingTumor( tempSummary, tumorSummary );
         if ( tempTumor != null ) {
            LOGGER.debug("Merging tumors:");
            LOGGER.debug(tumorSummary.getSummaryText());
            LOGGER.debug(tempTumor.getSummaryText());
            addMissingFacts( tumorSummary, tempTumor );
         }
      }
   }

   private void cleanSummaries( final CancerSummary cancerSummary ) {
	   cancerSummary.cleanSummary();
   }

   /**
    * find matching tumor
    *
    * @param tempSummary -
    * @param tumorSummary -
    * @return -
    */
   private TumorSummary getMatchingTumor( final CancerSummary tempSummary, final TumorSummary tumorSummary ) {
      for ( TumorSummary ts : tempSummary.getTumors() ) {
         for ( Fact abs : ts.getBodySite() ) {
            for ( Fact bbs : tumorSummary.getBodySite() ) {
               if ( bbs.equivalent( abs ) ) {
                  return ts;
               }
            }
         }
      }
      return null;
   }

   /**
    * add missing facts to a cancer summary from rules to
    *
    * @param targetSummary -
    * @param tempSummary -
    */
   private void addMissingFacts( final Summary targetSummary, final Summary tempSummary ) {
      for ( String category : tempSummary.getFactCategories() ) {
         //target summary doesn't have facts, but other summary does
         if ( targetSummary.getFacts( category ).isEmpty() ) {
            for ( Fact fact : tempSummary.getFacts( category ) ) {
               // clone a fact
               Fact newFact = FactFactory.createFact( fact );
               newFact.addProvenanceFact( fact );
               targetSummary.addFact( category, newFact );
            }
         }
      }
   }

   /**
    * @param facts -
    */
   private void addMissingAncestors( final Collection<Fact> facts ) {
      for ( Fact fact : facts ) {
         if ( fact.getAncestors().isEmpty() ) {
            Neo4jOntologyConceptUtil.getRootUris( fact.getUri() ).stream()
                                    .map( UriUtil::getExtension )
                                    .filter( n -> !n.equalsIgnoreCase( "Thing" ) )
                                    .forEach( fact::addAncestor );
         }
      }
   }

   /**
    * @param filenameBase - suggest use patient ID or something that includes patient ID
    * @return a file writer that can be used to store debug info for a patient
    * @throws IOException -
    */
   private Writer createDebugWriter( final String filenameBase ) throws IOException {
      final String fileDate = new SimpleDateFormat( ("MM-dd-yyyy_hh") ).format( new Date() );
      final String username = System.getProperty( "user.name" );
      String filename = DEBUG_DIR + filenameBase + "_V2_" + fileDate + "_" + username + ".bsv";
      final File file = new File( filename );
//      LOGGER.info( "Writing facts to a debug file in: " + file.getParentFile().getAbsolutePath() );
      LOGGER.info( "Writing facts to a debug file using filename: " + filename );
      return new FileWriter( filename );
   }

   /**
    * this debug file can used to run drools via TestPatientSummarizer without pipeline and requires all the fields in the Fact.getInfoDrools(true) method.
    *
    * @param filenameBase - suggest use patient ID or something that includes patient ID
    * @param reportFacts -
    */
   private void writeFactDebug( final String filenameBase, final List<Fact> reportFacts ) {
      try ( Writer debug = createDebugWriter( filenameBase ) ) {
         Collection<String> dupList = new HashSet<>();
         debug.write( FactHelper.DROOLS_INFO_HEADER + "\n" );
         for ( Fact fact : reportFacts ) {
            if ( dupList.add( fact.getInfoDrools( false ) ) ) {
               debug.write( fact.getInfoDrools( true ) + "\n" );
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Bad Debug", ioE );
      }
   }

   /**
    * this debug file is used to run drools without pipeline and requires all the fields in the Fact.getInfoDrools(true) method.
    *
    * @param patientName -
    * @param reportFacts -
    */
   private void writeShortFactDebug( final String patientName, final List<Fact> reportFacts ) {
      try ( Writer debug = createDebugWriter( "Short_" + patientName ) ) {
         for ( Fact fact : reportFacts ) {
            debug.write( getInfoDrools( fact ) );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Bad Debug", ioE );
      }
   }


   /**
    * @param fact -
    * @return short line of fact debug info that makes manual viewing and automatic diffing easy.
    */
   private String getInfoDrools( final Fact fact ) {
      return fact.getUri() + "|" + fact.getCategory()
             + "|" + fact.getType() + "|" + fact.getDocumentIdentifier()
             + "|" + fact.getDocumentTitle() + "|" + fact.getSummaryType()
             + "|" + (fact.getSummaryId() == null ? "NULL" : fact.getSummaryId().replaceAll( "[0-9]", "" )) + "\n";
   }


}