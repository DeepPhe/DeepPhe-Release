package org.healthnlp.deepphe.uima.ae;

import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.fhir.Patient;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.fhir.fact.Fact;
import org.healthnlp.deepphe.fhir.fact.FactFactory;
import org.healthnlp.deepphe.fhir.fact.FactList;
import org.healthnlp.deepphe.fhir.summary.*;
import org.healthnlp.deepphe.uima.drools.Domain;
import org.healthnlp.deepphe.uima.drools.DroolsEngine;
import org.healthnlp.deepphe.uima.fhir.PhenotypeResourceFactory;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.OntologyUtils;
import org.kie.api.runtime.KieSession;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.ctakes.core.ae.OwlRegexSectionizer.OWL_FILE_DESC;
import static org.apache.ctakes.core.ae.OwlRegexSectionizer.OWL_FILE_PATH;

/**
 * this is Rule based phenotype cancer summary AE it takes Composition
 * Cancer/Tumor and Patient Summary objects and creates a combined phenotype
 * summary object
 *
 * @author tseytlin
 */

public class PhenotypeSummarizer extends JCasAnnotator_ImplBase {
   static private final Logger LOGGER = Logger.getLogger( "PhenotypeSummarizer" );

   public static final String PARAM_ONTOLOGY_PATH = "ONTOLOGY_PATH";
   private IOntology ontology;
   private EpisodeClassifier episodeClassifier;

   @ConfigurationParameter(
         name = OWL_FILE_PATH,
         description = OWL_FILE_DESC
   )
   private String _owlFilePath;

   public void initialize( UimaContext aContext ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Phenotype Summarizer ..." );
      super.initialize( aContext );
      try {
         ontology = OwlConnectionFactory.getInstance().getOntology( _owlFilePath );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         throw new ResourceInitializationException( multE );
      }

      episodeClassifier = new EpisodeClassifier();
      LOGGER.info( "Initialization finished." );
   }

   public void process( JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Summarizing Phenotype ..." );
      // for now, lets assume there is only one cancer summary
      MedicalRecord record = PhenotypeResourceFactory.loadMedicalRecord( jcas );
      Patient patient = record.getPatient();

      PatientSummary patientSummary = new PatientSummary();
      patientSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );

      CancerSummary cancerSummary = new CancerSummary( patient.getPatientName() );
      cancerSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );

      CancerSummary tempSummary = new CancerSummary( patient.getPatientName() );
      tempSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );


      // record to load into drools
      record.setPatientSummary( patientSummary );
      record.setCancerSummary( cancerSummary );

      // merge stuff around
      for ( Report report : record.getReports() ) {
         PatientSummary p = report.getPatientSummary();
         if ( p != null && patientSummary.isAppendable( p ) ) {
            patientSummary.append( p );
         }
         // append everything to tempSummary instead of real cancer summary
         for ( CancerSummary c : report.getCancerSummaries() ) {
            if ( tempSummary.isAppendable( c ) ) {
               tempSummary.append( c );
            }
         }
      }

      // check ancestors
      List<Fact> reportFacts = record.getReportLevelFacts();
      checkAncestors( reportFacts );

      System.out.println( "PROCESSING phenotype for " + cancerSummary.getResourceIdentifier() + " .." );
      System.out.println( "loading " + reportFacts.size() + " facts into Rules Engine ..." );

		/*for(Fact f: record.getReportLevelFacts()){
         if(f.getName().contains("clock"))
				System.out.println(f.getInfo()); 
		}*/
      // figure out the domain
      String domain = FHIRConstants.DOMAIN_BREAST;
      for ( String d : FHIRConstants.DOMAINS ) {
         if ( ontology.getName().contains( d ) )
            domain = d;
      }


      // insert record into drools
      DroolsEngine de = new DroolsEngine();
      KieSession droolsSession = null;
      try {
         // duplicates care
         List<String> dupList = new ArrayList<String>();
         droolsSession = de.getSession();
         // droolsSession.addEventListener( new DebugAgendaEventListener() );
         // insert new medical record
         droolsSession.insert( new Domain( domain, ontology.getURI().toString() ) );
         droolsSession.insert( record );
         boolean doWrite = false;
         FileWriter fw = null;
         if ( doWrite )
            fw = new FileWriter( "/home/opm1/devSrc/deepPhe_Data/DeepPhe/droolsInput/GOLD_Patient21.txt" );
         for ( Fact f : reportFacts ) {
            try {
               if ( !f.getCategory().equalsIgnoreCase( "wasDerivedFrom" ) && !dupList.contains( f.getInfoDrools() ) ) {
                  dupList.add( f.getInfoDrools() );
                  if ( doWrite )
                     fw.write( f.getInfoDrools() + "\n" );

                  // System.out.println(f.getInfo());
                  droolsSession.insert( f );
               }
            } catch ( NullPointerException e ) {
               // System.err.println("NO Category for F: "+f.getInfo());
            }
         }
         if ( fw != null )
            fw.close();
         dupList.clear();
         dupList = null;
         droolsSession.fireAllRules();
         droolsSession.dispose();

         // System.out.println("DROOLS TIME: "+(System.currentTimeMillis() -
         // stT)/1000+" sec");
      } catch ( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      // now lets fill out the facts that were omitted by the rules
      //Olga comm
      mergeCancerSummaries( cancerSummary, tempSummary );
      // end Olga

      cleanSummaries( cancerSummary );


      // lets create episode objects to medical record summary
      //episodeClassifier.addEpisodes(record);


      // do clean up. Remove all tumor containers that only have Lesion as Dx
      /*List<TumorSummary> torem = new ArrayList<TumorSummary>();
      for(TumorSummary ts: cancerSummary.getTumors()){
			FactList dx = ts.getDiagnosis();
			if(dx.size() == 1 && FHIRConstants.LESION.equals(dx.get(0).getName())){
				torem.add(ts);
			}else{
				// remove lesion as Dx
				for(Fact f: new ArrayList<Fact>(dx)){
					if(FHIRConstants.LESION.equals(f.getName())){
						dx.remove(f);
					}
				}
			}
			
			
			
		}*/
      //remove junk tumors
      //cancerSummary.getTumors().removeAll(torem);


      // debug system out
      System.out.println( "\n**************************" );
      System.out.println( "RECORD Summary: " + record.getSummaryText() );
      System.out.println( "**************************" );

      // reset fact level resource IDs that rules have created
      for ( Fact f : record.getRecordLevelFacts() ) {
         //if(!f.getIdentifier().matches(".*\\d+.*")){
         f.setIdentifier( record.getClass().getSimpleName() + "_" + f.getName() + "_" + Math.round( 1000 * Math.random() ) );
         //}
         // System.out.println(f.getInfo());
      }

      // this is where you save your work back to CAS
      PhenotypeResourceFactory.saveMedicalRecord( record, jcas );
      LOGGER.info( "Summarization finished." );
   }

   private void mergeCancerSummaries( CancerSummary cancerSummary, CancerSummary tempSummary ) {
      addMissingFacts( cancerSummary, tempSummary );
      addMissingFacts( cancerSummary.getPhenotype(), tempSummary.getPhenotype() );
      for ( TumorSummary tumorSummary : cancerSummary.getTumors() ) {
         TumorSummary tempTumor = getMatchingTumor( tempSummary, tumorSummary );
         if ( tempTumor != null ) {
            addMissingFacts( tumorSummary, tempTumor );
            addMissingFacts( tumorSummary.getPhenotype(), tempTumor.getPhenotype() );
         }
      }
   }

   private void cleanSummaries( CancerSummary cancerSummary ) {
      //remove all bodysites and diagnosis if it's Lesion only.
      FactList dfl = cancerSummary.getDiagnosis();
      if ( dfl.size() == 1 && dfl.get( 0 ).getName().equalsIgnoreCase( "LESION" ) ) {
         dfl.clear();
         cancerSummary.getBodySite().clear();
      }
   }

   /**
    * find matching tumor
    *
    * @param tempSummary
    * @param tumorSummary
    * @return
    */
   private TumorSummary getMatchingTumor( CancerSummary tempSummary, TumorSummary tumorSummary ) {
      for ( TumorSummary ts : tempSummary.getTumors() ) {
         for ( Fact abs : ts.getBodySite() ) {
            for ( Fact bbs : tumorSummary.getBodySite() ) {
               if ( bbs.equivalent( abs ) )
                  return ts;
            }
         }
      }
      return null;
   }

   /**
    * add missing facts to a cancer summary from rules to
    *
    * @param targetSummary
    * @param tempSummary
    */
   private void addMissingFacts( Summary targetSummary, Summary tempSummary ) {
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

   public void checkAncestors( Collection<Fact> facts ) {
      for ( Fact f : facts ) {
         if ( f.getAncestors().isEmpty() )
            OntologyUtils.getInstance().addAncestors( f );
      }
   }
}
