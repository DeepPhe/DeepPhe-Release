//package org.healthnlp.deepphe.uima.ae;
//
//import org.apache.ctakes.cancer.uri.UriConstants;
//import org.apache.ctakes.cancer.uri.UriUtil;
//import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
//import org.healthnlp.deepphe.fact.BodySiteFact;
//import org.healthnlp.deepphe.fact.Fact;
//import org.healthnlp.deepphe.fact.FactFactory;
//import org.healthnlp.deepphe.fact.FactHelper;
//import org.healthnlp.deepphe.summary.*;
//import org.healthnlp.deepphe.uima.drools.Domain;
//import org.healthnlp.deepphe.uima.drools.DroolsEngine;
//import org.healthnlp.deepphe.util.FHIRConstants;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.kie.api.runtime.KieSession;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//public class TestPatientSummarizer {
//	String patientName;
//	MedicalRecord medicalRecord;
//	PatientSummary sourcePatientSummary;
//	CancerSummary sourceCancerSummary, tempSummary;
//
//	String DOMAIN;
//	String factFilePath;
//
//
//
//	@Before
//	public void setUp() throws Exception {
//	      patientName = "PatientX";
//	      //DOMAIN = FHIRConstants.DOMAIN_BREAST;
//	      DOMAIN = FHIRConstants.DOMAIN_MELANOMA;
//	      //DOMAIN = FHIRConstants.DOMAIN_OVARIAN;
//	//HANGS!!!
//	      //long
//	     //factFilePath = "C:/deepPhe_Data/DeepPhe/v2bad/HANGS_patient16_ov_upmc.bsv";
//	     //shorter hang
//	   //  factFilePath = "C:/deepPhe_Data/DeepPhe/v2bad/HANGS_patient31_ov_upmc.bsv";
//
//	 // other
//	      //factFilePath = "C:/deepPhe_Data/DeepPhe/v2bad/patientX.bsv";
//	      //factFilePath = "C:/deepPhe_Data/DeepPhe/v2bad/patient34_upmc_ov_combine_bilat_and_right.bsv";
//
//	 // multi tnm:
//	  //   factFilePath = "C:/deepPhe_Data/DeepPhe/v2bad/patient34_mela_upmc_TNM.bsv";
//
//	  // wrong count
//	      //factFilePath = "C:/deepPhe_Data/DeepPhe/v2bad/patient03_ov_upmc.bsv";
//	  //wrong primary
//	      //factFilePath = "C:/deepPhe_Data/DeepPhe/v2bad/20181174_mela_lsu.bsv";
//	  //should be no tumors if cancer hass no AS
//	      factFilePath = "C:\\private\\@data\\DeepPhe\\V2.subsets\\output.DeepPhe.piper\\V2.UPMC.Train.melanoma.output\\SP.RAD.w.Drools.facts\\patientXX_V2_mm-dd-yyyy.bsv";
//				  //"C:/deepPhe_Data/DeepPhe/v2bad/20062030_mela_lsu.bsv";
//
//	      medicalRecord = new MedicalRecord( patientName );
//	      sourcePatientSummary = medicalRecord.getPatientSummary();
//	      sourcePatientSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );
//
//	      sourceCancerSummary = new CancerSummary( medicalRecord.getPatientName() );
//	      sourceCancerSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );
//
//	      //final TumorSummary sourceTumorSummary = new TumorSummary( medicalRecord.getPatientName() );
//	      //sourceTumorSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );
//
//
//	      tempSummary = new CancerSummary( medicalRecord.getPatientName() );
//	      tempSummary.setAnnotationType( FHIRConstants.ANNOTATION_TYPE_RECORD );
//
//	      // record to load into drools
//		medicalRecord.setPatientSummaryWithoutFacts( sourcePatientSummary );
//		medicalRecord.setCancerSummaryWithoutFacts( sourceCancerSummary );
//		medicalRecord.addTumorSummariesFromNoteSummaries();
//
//		// check ancestors
//		final List<Fact> reportFacts = medicalRecord.getReportLevelFacts(); // aka NoteSummary facts
//		BodySiteFact.makeQuadrantsBodyModifier(reportFacts);
//
//		addMissingAncestors( reportFacts );
//
//	}
//
//	@After
//	public void tearDown() throws Exception {
//	}
//
//	@Test
//	public void test() {
//
//
//		final DroolsEngine de = new DroolsEngine();
//	      KieSession droolsSession = null;
//	      try {
//	         Set<String> dupList = new HashSet<>(); // duplicates care
//			  System.out.println("Using rules for domain: " + DOMAIN);
//	         droolsSession = de.createSessionByKBaseName(DOMAIN);
//	         droolsSession.insert( new Domain(DOMAIN, UriConstants.DPHE_ROOT ) );
//	         droolsSession.insert( medicalRecord );
//
//	         BufferedReader reader = new BufferedReader(new FileReader(factFilePath));
//	         String line;
//	         Fact f;
//	         boolean isHeader = true;
//	         while ((line = reader.readLine()) != null) {
//	        	 if(isHeader) {
//	        		 isHeader = !isHeader;
//	        	 }
//	        	 else {
//					 f = FactHelper.stringToDroolsFact( line );
//		        	 try {
//			               if (dupList.add( f.getInfoDrools(false) ) ) {
//			                  droolsSession.insert( f );
//			               }
//		            } catch ( NullPointerException e ) {}
//	        	 }
//	         }
//	  System.out.println("num non-duplicate: "+dupList.size());
//		     reader.close();
//	         dupList.clear();
//	         dupList = null;
//	         droolsSession.fireAllRules();
//	         droolsSession.dispose();
//
//	         // System.out.println("DROOLS TIME: "+(System.currentTimeMillis() -
//	         // stT)/1000+" sec");
//	      } catch ( Exception e ) {
//	         // TODO Auto-generated catch block
//	         e.printStackTrace();
//	      }
//	  	System.out.println("exiting drools...");
//
//	      // now lets fill out the facts that were omitted by the rules.
//	      mergeCancerSummaries( sourceCancerSummary, tempSummary );
//	      cleanSummaries( sourceCancerSummary );
//
//	      MedicalRecordStore.getInstance().store( medicalRecord );
//
//System.out.println("After Drools CS SUMMARY:"+medicalRecord.getSummaryText());
//	//System.exit(0);
//	   }
//
//	   private void mergeCancerSummaries( final CancerSummary cancerSummary, final CancerSummary tempSummary ) {
//	      addMissingFacts( cancerSummary, tempSummary );
//	      for ( TumorSummary tumorSummary : cancerSummary.getTumors() ) {
//	         TumorSummary tempTumor = getMatchingTumor( tempSummary, tumorSummary );
//	         if ( tempTumor != null ) {
//				 System.out.println("Merging tumors:");
//				 System.out.println(tumorSummary.getSummaryText());
//				 System.out.println(tempTumor.getSummaryText());
//	            addMissingFacts( tumorSummary, tempTumor );
//	         }
//	      }
//	   }
//
//	   private void cleanSummaries( final CancerSummary cancerSummary ) {
//		   cancerSummary.cleanSummary();
//	      /*
//		   //remove all bodysites and diagnosis if it's Lesion only.
//	      final FactList dfl = cancerSummary.getDiagnosis();
//	      if ( dfl.size() == 1 && dfl.get( 0 ).getName().equalsIgnoreCase( "LESION" ) ) {
//	         dfl.clear();
//	         cancerSummary.getBodySite().clear();
//	      }
//	      */
//	   }
//
//	   /**
//	    * find matching tumor
//	    *
//	    * @param tempSummary -
//	    * @param tumorSummary -
//	    * @return -
//	    */
//	   private TumorSummary getMatchingTumor( final CancerSummary tempSummary, final TumorSummary tumorSummary ) {
//	      for ( TumorSummary ts : tempSummary.getTumors() ) {
//	         for ( Fact abs : ts.getBodySite() ) {
//	            for ( Fact bbs : tumorSummary.getBodySite() ) {
//	               if ( bbs.equivalent( abs ) ) {
//	                  return ts;
//	               }
//	            }
//	         }
//	      }
//	      return null;
//	   }
//
//	   /**
//	    * add missing facts to a cancer summary from rules to
//	    *
//	    * @param targetSummary -
//	    * @param tempSummary -
//	    */
//	   private void addMissingFacts( final Summary targetSummary, final Summary tempSummary ) {
//	      for ( String category : tempSummary.getFactCategories() ) {
//	         //target summary doesn't have facts, but other summary does
//	         if ( targetSummary.getFacts( category ).isEmpty() ) {
//	            for ( Fact fact : tempSummary.getFacts( category ) ) {
//	               // clone a fact
//	               Fact newFact = FactFactory.createFact( fact );
//	               newFact.addProvenanceFact( fact );
//	               targetSummary.addFact( category, newFact );
//	            }
//	         }
//	      }
//	   }
//
//	   /**
//	    * @param facts -
//	    */
//	   private void addMissingAncestors( final Collection<Fact> facts ) {
//	      for ( Fact fact : facts ) {
//	         if ( fact.getAncestors().isEmpty() ) {
//	            Neo4jOntologyConceptUtil.getRootUris( fact.getUri() ).stream()
//	                                    .map( UriUtil::getExtension )
//	                                    .filter( n -> !n.equalsIgnoreCase( "Thing" ) )
//	                                    .forEach( fact::addAncestor );
//	         }
//	      }
//	   }
//
//}
