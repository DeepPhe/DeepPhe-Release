package org.apache.ctakes.cancer.pipeline;


import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/18/2017
 */
public class PatientXtester {

   static private final Logger LOGGER = Logger.getLogger( "PatientXtester" );

   static private final String PIPER_PATH = "org/apache/ctakes/cancer/pipeline/DeepPhe.piper";
   static private final String FILE_PATH_1 = "data/sample/reports/patientX/patientX_doc1_RAD.txt";

   private PipelineBuilder _builder;


   /**
    * This is a unit testing anti-pattern, but it serves as an end-to-end
    */
   @Test
   public void testPatientX_1() {
      String text = "";
      try ( final BufferedReader reader = new BufferedReader( new InputStreamReader( FileLocator
            .getAsStream( FILE_PATH_1 ) ) ) ) {
         text = reader.lines().collect( Collectors.joining( "\n" ) );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         fail();
      }
      try {
         final PiperFileReader reader = new PiperFileReader( PIPER_PATH );
         reader.getBuilder().run( text );
      } catch ( UIMAException | IOException multE ) {
         LOGGER.error( multE.getMessage() );
      }

   }


   static private final class JCasProvider extends JCasAnnotator_ImplBase {
      @Override
      public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      }
   }


}
