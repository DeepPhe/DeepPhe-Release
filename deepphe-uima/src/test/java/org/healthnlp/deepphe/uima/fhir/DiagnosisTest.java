package org.healthnlp.deepphe.uima.fhir;

import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.ontology.UriAnnotationFactory;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.fhir.Disease;
import org.healthnlp.deepphe.uima.ae.DocumentSummarizerAE;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.fail;


public class DiagnosisTest {

   static private final Logger LOGGER = Logger.getLogger( "DiagnosisTest" );

   /**
    * create mention
    *
    * @param dd
    * @param st
    * @param en
    * @param code
    * @return
    */
   private IdentifiedAnnotation createMention( IdentifiedAnnotation dd, int st, int en, String code ) {
      JCas jcas = null;
      try {
         jcas = dd.getCAS().getJCas();
      } catch ( CASException e ) {
         e.printStackTrace();
         fail( "Unable to init AE" + e.getMessage() );
      }
      OntologyConcept o = new OntologyConcept( jcas );
      o.setCode( code );
      o.setCodingScheme( "UMLS" );
      dd.setBegin( 23 );
      dd.setEnd( 36 );
      dd.setOntologyConceptArr( new FSArray( jcas, 1 ) );
      dd.setOntologyConceptArr( 0, o );
      dd.addToIndexes();
      return dd;
   }

   private RelationArgument createArgument( IdentifiedAnnotation a ) {
      JCas jcas = null;
      try {
         jcas = a.getCAS().getJCas();
      } catch ( CASException e ) {
         e.printStackTrace();
         fail( "Unable to init AE" + e.getMessage() );
      }
      RelationArgument ra = new RelationArgument( jcas );
      ra.setArgument( a );
      ra.addToIndexes();
      return ra;
   }


   @Test
   public void testFHIRexport() {
      // specify the default ontology
      try {
         OwlConnectionFactory.getInstance().getOntology( "data/ontology/nlpBreastCancer.owl" );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         LOGGER.error( multE.getMessage() );
      }

      File outputDirectory = new File( System.getProperty( "user.home" ) + File.separator + "Output" );
      if ( !outputDirectory.exists() )
         outputDirectory.mkdirs();
      String ontologyPath = "http://ontologies.dbmi.pitt.edu/deepphe/breastCancer.owl";
      AnalysisEngine ae;
      try {
         ae = AnalysisEngineFactory.createEngine(
//					DocumentSummarizerAE.class, ConfigParameterConstants.PARAM_OUTPUTDIR,outputDirectory,DocumentSummarizerAE.PARAM_ONTOLOGY_PATH,ontologyPath);
               DocumentSummarizerAE.class, ConfigParameterConstants.PARAM_OUTPUTDIR, outputDirectory );

         // init new cas
         JCas jcas = ae.newJCas();
         jcas.setDocumentText( "history of T2N0M0 left breast cancer" );

         // setup CAS
         DocumentID id = new DocumentID( jcas );
         id.setDocumentID( "dx_doc" );
         id.addToIndexes();
//			DiseaseDisorderMention dd =  (DiseaseDisorderMention) createMention(new DiseaseDisorderMention(jcas),23,36,"http://ontologies.dbmi.pitt.edu/deepphe/breastCancer.owl#Malignant_Breast_Neoplasm");
//			AnatomicalSiteMention am = (AnatomicalSiteMention) createMention(new AnatomicalSiteMention(jcas),18,9, "http://ontologies.dbmi.pitt.edu/deepphe/breastCancer.owl#Left_Breast");
//			TnmClassification tc = (TnmClassification) createMention(new TnmClassification(jcas),11,19, "http://ontologies.dbmi.pitt.edu/deepphe/breastCancer.owl#Left_Breast");
         IdentifiedAnnotation dd = UriAnnotationFactory
               .createIdentifiedAnnotation( jcas, 23, 36, "http://ontologies.dbmi.pitt.edu/deepphe/breastCancer.owl#Malignant_Breast_Neoplasm" );
         IdentifiedAnnotation am = UriAnnotationFactory
               .createIdentifiedAnnotation( jcas, 18, 9, "http://ontologies.dbmi.pitt.edu/deepphe/breastCancer.owl#Left_Breast" );
         // TODO -- tnm classification with an anatomical site uri?   After the tnm in model issue is fixed, change to #TNMClassification
         IdentifiedAnnotation tc = UriAnnotationFactory
               .createIdentifiedAnnotation( jcas, 11, 19, "http://ontologies.dbmi.pitt.edu/deepphe/breastCancer.owl#Left_Breast" );
//			NeoplasmRelation nr = new NeoplasmRelation(jcas);
//			nr.setCategory("TNM_of");
//			nr.setArg1(createArgument(tc));
//			nr.setArg2(createArgument(dd));
//			nr.addToIndexes();
//			NeoplasmRelation nr = NeoplasmRelationFactory.createNeoplasmRelation( jcas, tc, dd, "TNM_of" );
//			LocationOfTextRelation lt = new LocationOfTextRelation(jcas);
//			lt.setArg1(createArgument(dd));
//			lt.setArg2(createArgument(am));
//			lt.addToIndexes();
         RelationUtil.createRelation( jcas, tc, dd, "TNM_of" );
         RelationUtil.createRelation( jcas, new LocationOfTextRelation( jcas ), dd, am, "hasBodySite" );
         // process AE
         for ( Disease dx : DocumentResourceFactory.getDiagnoses( jcas ) ) {
            LOGGER.info( dx.getSummaryText() );
         }

      } catch ( ResourceInitializationException e ) {
         e.printStackTrace();
         fail( "Unable to init AE" + e.getMessage() );
      } /*catch (AnalysisEngineProcessException e) {
         e.printStackTrace();
			fail(e.getMessage());
		}*/

      //fail("Not yet implemented");
   }

}
