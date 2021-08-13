package org.healthnlp.deepphe.nlp.writer;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.neo4j.driver.DriverConnection;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.neo4j.node.Patient;
import org.healthnlp.deepphe.neo4j.node.PatientSummary;
import org.healthnlp.deepphe.neo4j.util.JsonUtil;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.node.PatientSummaryNodeStore;
import org.healthnlp.deepphe.summary.engine.SummaryEngine;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/10/2019
 */
@PipeBitInfo(
      name = "NeoplasmNeo4jWriter",
      description = "Write dPhe neoplasm data to neo4j.",
      role = PipeBitInfo.Role.WRITER
)
public class NeoplasmNeo4jWriter extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmNeo4jWriter" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Neo4j Neoplasm Writer ..." );
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );

      final Driver driver = DriverConnection.getInstance().getDriver();
      if ( driver == null ) {
         LOGGER.info( "Empty Driver.  Writing to Neo4j will be skipped." );
         return;
      }
      try ( Session session = driver.session() ) {
         try ( Transaction tx = session.beginTransaction() ) {
            tx.run( "CALL deepphe.initializeDphe()" );
            tx.commit();
         }
      } catch ( Exception e ) {
         LOGGER.error( e.getMessage() );
         throw new ResourceInitializationException( e );
      }
   }


   /**
    * Creates the patient summary with neoplasms before writing to neo4j.
    * Call necessary processing for patient.
    * <p>
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      // Even though the ctakes PatientNoteStore isn't being used to store any patient jcas, it still has note counts.
      final int patientDocCount = PatientNoteStore.getInstance()
                                                  .getWantedDocCount( patientId );
      final Patient patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      LOGGER.info( "Patient " + patientId + " has " + patient.getNotes().size() + " completed out of " + patientDocCount );
      if ( patient.getNotes().size() < patientDocCount ) {
         return;
      }
      writePatient( patient );
   }

   static private final Function<NeoplasmAttribute,NeoplasmAttribute> stripConfidence
         = a -> {  a.setConfidenceFeatures( Collections.emptyList() );
         return a; };

   static private void writePatient( final Patient patient ) throws AnalysisEngineProcessException {
      final String patientId = patient.getId();
//      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      LOGGER.info( "Writing " + patientId + " to Neo4j Database " + DriverConnection.getInstance().getUrl() );
      final Driver driver = DriverConnection.getInstance().getDriver();
      if ( driver == null ) {
         LOGGER.info( "Empty Driver.  Writing to Neo4j will be skipped." );
         return;
      }
      // Somebody else may have already created the patient summary.
      PatientSummary patientSummary = PatientSummaryNodeStore.getInstance().get( patientId );
      if ( patientSummary == null ) {
         // Create PatientSummary
         patientSummary = SummaryEngine.createPatientSummary( patient );
         // Add the summary just in case some other consumer can utilize it.  e.g. eval file writer.
         PatientSummaryNodeStore.getInstance().add( patientId, patientSummary );
      }
      final List<NeoplasmSummary> neoplasms = patientSummary.getNeoplasms();
      for ( NeoplasmSummary neoplasm : neoplasms ) {
         final List<NeoplasmAttribute> attributes
               = neoplasm.getAttributes().stream()
                         .filter( a -> !a.getValue().isEmpty() )
                         .map( stripConfidence )
                         .collect( Collectors.toList() );
         neoplasm.setAttributes( attributes );
      }
      patientSummary.setNeoplasms( neoplasms );
      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      final String summaryJson = gson.toJson( patientSummary );
      final String neo4jOkJson = JsonUtil.packForNeo4j( summaryJson );
      try ( Session session = driver.session() ) {
         try ( Transaction tx = session.beginTransaction() ) {
            tx.run( "CALL deepphe.addPatientSummary(\"" + neo4jOkJson + "\")" );
            tx.commit();
         }
      } catch ( Exception e ) {
         LOGGER.error( e.getMessage() );
         throw new AnalysisEngineProcessException( e );
      }
   }



}
