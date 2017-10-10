package org.apache.ctakes.cancer.phenotype.receptor;

import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlConnectionFactory;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Arrays;

import static org.apache.ctakes.cancer.phenotype.receptor.StatusType.*;
import static org.apache.ctakes.cancer.phenotype.receptor.StatusValue.*;
import static org.junit.Assert.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/7/2015
 */
public class ReceptorStatusUtilTester {

   static private final Logger LOGGER = Logger.getLogger( "ReceptorStatusUtilTester" );


   @BeforeClass
   static public void setupTestCas() throws UIMAException {
      loadOntology();
      try {
         _testCas = JCasFactory.createJCas();
      } catch ( UIMAException uimaE ) {
         LOGGER.error( "Could not create test CAS " + uimaE.getMessage() );
         throw uimaE;
      }
      int offset = 0;
      PR_PLUS_1 = createReceptorStatus( PR, POSITIVE, offset );
      offset += 40;
      PR_PLUS_2 = createReceptorStatus( PR, POSITIVE, offset );
      offset += 40;
      PR_MINUS_1 = createReceptorStatus( PR, NEGATIVE, offset );
      offset += 40;
      ER_PLUS_1 = createReceptorStatus( ER, POSITIVE, offset );
      offset += 40;
      ER_MINUS_1 = createReceptorStatus( ER, NEGATIVE, offset );
      offset += 40;
      ER_MINUS_2 = createReceptorStatus( ER, NEGATIVE, offset );
      offset += 40;
      HER2_PLUS_1 = createReceptorStatus( HER2, POSITIVE, offset );
      offset += 40;
      HER2_MINUS_1 = createReceptorStatus( HER2, NEGATIVE, offset );
      offset += 40;
      HER2_UNKNOWN_1 = createReceptorStatus( HER2, UNKNOWN, offset );

      offset += 40;
      NOT_RECEPTOR_SS = new SignSymptomMention( _testCas, offset, offset + 10 );
      NOT_RECEPTOR_SS.addToIndexes();

      final char[] noteText = new char[ offset + 51 ];
      Arrays.fill( noteText, 'a' );
      _testCas.setDocumentText( String.valueOf( noteText ) );
   }

   static public IdentifiedAnnotation createReceptorStatus( final StatusType type,
                                                            final StatusValue value,
                                                            final int offset ) {
      final SpannedStatusType spannedType = new SpannedStatusType( type, offset, offset + 10 );
      final SpannedStatusValue spannedValue = new SpannedStatusValue( value, offset + 20, offset + 30 );
      final Status status = new Status( spannedType, spannedValue );
      return StatusPhenotypeFactory.getInstance().createPhenotype( _testCas, 0, status );
   }

   //   @BeforeClass
   static public void loadOntology() {
      try {
         // "data/ontology/breastCancer.owl"
//         OwlConnectionFactory.getInstance().getOntology("http://ontologies.dbmi.pitt.edu/deepphe/nlpBreastCancer.owl");
         OwlConnectionFactory.getInstance().getOntology( "data/ontology/nlpBreastCancer.owl" );
      } catch ( IOntologyException | FileNotFoundException multE ) {
         LOGGER.error( multE.getMessage() );
      }
   }

   static private JCas _testCas;
   static private IdentifiedAnnotation PR_PLUS_1;
   static private IdentifiedAnnotation PR_PLUS_2;
   static private IdentifiedAnnotation PR_MINUS_1;
   static private IdentifiedAnnotation ER_PLUS_1;
   static private IdentifiedAnnotation ER_MINUS_1;
   static private IdentifiedAnnotation ER_MINUS_2;
   static private IdentifiedAnnotation HER2_PLUS_1;
   static private IdentifiedAnnotation HER2_MINUS_1;
   static private IdentifiedAnnotation HER2_UNKNOWN_1;
   static private IdentifiedAnnotation NOT_RECEPTOR_SS;


   @Test
   public void testIsReceptorStatus() {
      final StatusPropertyUtil util = StatusPropertyUtil.getInstance();
      assertTrue( "PR Positive not Receptor Status", util.isCorrectProperty( PR_PLUS_1 ) );
      assertTrue( "PR Positive not Receptor Status", util.isCorrectProperty( PR_PLUS_2 ) );
      assertTrue( "PR Negative not Receptor Status", util.isCorrectProperty( PR_MINUS_1 ) );
      assertTrue( "ER Positive not Receptor Status", util.isCorrectProperty( ER_PLUS_1 ) );
      assertTrue( "ER Negative not Receptor Status", util.isCorrectProperty( ER_MINUS_1 ) );
      assertTrue( "ER Negative not Receptor Status", util.isCorrectProperty( ER_MINUS_2 ) );
      assertTrue( "HER2 Positive not Receptor Status", util.isCorrectProperty( HER2_PLUS_1 ) );
      assertTrue( "HER2 Negative not Receptor Status", util.isCorrectProperty( HER2_MINUS_1 ) );
      assertTrue( "HER2 Unknown not Receptor Status", util.isCorrectProperty( HER2_UNKNOWN_1 ) );
      assertFalse( "Generic Sign Symptom is Receptor Status", util.isCorrectProperty( NOT_RECEPTOR_SS ) );
   }

   @Test
   public void testGetStatusValue() {
      final StatusPropertyUtil util = StatusPropertyUtil.getInstance();
      assertEquals( "PR Positive not Positive", util.getValue( _testCas, PR_PLUS_1 ), POSITIVE );
      assertEquals( "PR Positive not Positive", util.getValue( _testCas, PR_PLUS_2 ), POSITIVE );
      assertEquals( "PR Negative not Negative", util.getValue( _testCas, PR_MINUS_1 ), NEGATIVE );
      assertEquals( "ER Positive not Positive", util.getValue( _testCas, ER_PLUS_1 ), POSITIVE );
      assertEquals( "ER Negative not Negative", util.getValue( _testCas, ER_MINUS_1 ), NEGATIVE );
      assertEquals( "ER Negative not Negative", util.getValue( _testCas, ER_MINUS_2 ), NEGATIVE );
      assertEquals( "HER2 Positive not Positive", util.getValue( _testCas, HER2_PLUS_1 ), POSITIVE );
      assertEquals( "HER2 Negative not Negative", util.getValue( _testCas, HER2_MINUS_1 ), NEGATIVE );
      assertEquals( "HER2 Unknown not Unknown", util.getValue( _testCas, HER2_UNKNOWN_1 ), UNKNOWN );
      assertNull( "Generic Sign Symptom value not Null", util.getValue( _testCas, NOT_RECEPTOR_SS ) );
   }

   // createFullReceptorStatusMention is mostly tested by the setup and other tests


}
