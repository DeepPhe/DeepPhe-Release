package org.apache.ctakes.cancer.cc;


import org.apache.ctakes.cancer.phenotype.PhenotypeAnnotationUtil;
import org.apache.ctakes.cancer.phenotype.size.SizePropertyUtil;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.core.util.OntologyConceptUtil;
import org.apache.ctakes.core.util.RelationUtil;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.DiagnosesTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/19/2016
 */
public class SemevalBsvWriter2 extends CasConsumer_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SemevalBsvWriter2" );


   /**
    * Name of configuration parameter that must be set to the path of a directory into which the
    * output files will be written.
    * This can be set with cli -o
    */
   @ConfigurationParameter( name = ConfigParameterConstants.PARAM_OUTPUTDIR,
         description = "Root output directory to write semeval pipe files" )
   private File _outputRootDir;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      if ( !_outputRootDir.exists() ) {
         _outputRootDir.mkdirs();
      }
   }

   /**
    * Write the cas into an xmi output file named based upon the document id and located based upon the document id prefix.
    * {@inheritDoc}
    */
   @Override
   public void process( final CAS cas ) throws AnalysisEngineProcessException {
      JCas jcas;
      try {
         jcas = cas.getJCas();
      } catch ( CASException casE ) {
         throw new AnalysisEngineProcessException( casE );
      }
      final Collection<IdentifiedAnnotation> neoplasms = getAllNeoplasms( jcas );
      final String fileName = DocumentIDAnnotationUtil.getDocumentIdForFile( jcas );
      try {
         writeStages( jcas, makeOutputDir( "CancerStage" ), fileName, neoplasms );
         writeReceptors( jcas, makeOutputDir( "ReceptorStatus" ), fileName, neoplasms );
         writeTNMs( jcas, makeOutputDir( "TnmClassification" ), fileName, neoplasms );
         writePrimaries( jcas, makeOutputDir( "Primaries" ), fileName, neoplasms );
         writeMetastases( jcas, makeOutputDir( "Metastasis" ), fileName, neoplasms );
         // Medications and procedures use regular ctakessnorx dictionary
//         writeMedications( jcas, makeOutputDir( "Medication" ), fileName, neoplasms );
//         writeProcedures( jcas, makeOutputDir( "Procedure" ), fileName, neoplasms );
         writeSizes( jcas, makeOutputDir( "TumorSize" ), fileName, neoplasms );
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

   private File makeOutputDir( final String phenotypeName ) {
      final File dir = new File( _outputRootDir, phenotypeName );
      dir.mkdirs();
      return dir;
   }


   static private void writeStages( final JCas jCas,
                                    final File outputDir,
                                    final String fileName,
                                    final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<IdentifiedAnnotation> stages = getStages( jCas );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation stage : stages ) {
            // DocName|
            writer.write( fileName + "|" );
            // Phenotype_Spans|CUI|
            writer.write( getSpanThenCui( stage, "unmarked" ) );
            // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
            writer.write( createAttributeColumns( jCas, stage ) );
            // Value_Spans|CUI|
            writer.write( getSpanThenCui( PhenotypeAnnotationUtil.getPropertyValues( jCas, stage ), "unmarked" ) );
            // Neoplasm_Spans|CUI|
            writer.write( getSpanThenCui( getNeoplasms( jCas, stage, neoplasms ), "unmarked" ) );
            // AssociatedTest_Spans|CUI|
            writer.write( getSpanThenCui( getDiagnosticTests( jCas, stage ), "unmarked" ) );
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private void writeReceptors( final JCas jCas,
                                       final File outputDir,
                                       final String fileName,
                                       final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<IdentifiedAnnotation> receptors = getReceptors( jCas );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation receptor : receptors ) {
            // DocName|
            writer.write( fileName + "|" );
            // Phenotype_Spans|CUI|
            writer.write( getSpanThenCui( receptor, "unmarked" ) );
            // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
            writer.write( createAttributeColumns( jCas, receptor ) );
            // Value_Spans|CUI|
            writer.write( getSpanThenCui( PhenotypeAnnotationUtil.getPropertyValues( jCas, receptor ), "unmarked" ) );
            // Neoplasm_Spans|CUI|
            writer.write( getSpanThenCui( getNeoplasms( jCas, receptor, neoplasms ), "unmarked" ) );
            // AssociatedTest_Spans|CUI|
            writer.write( getSpanThenCui( getDiagnosticTests( jCas, receptor ), "unmarked" ) );
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   /**
    * Only write tnms for which there are values
    */
   static private void writeTNMs( final JCas jCas,
                                  final File outputDir,
                                  final String fileName,
                                  final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<IdentifiedAnnotation> tnms = getTNMs( jCas );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation tnm : tnms ) {
            // We are interested in value annotations, some of which are null for tnm
            // DocName|
            writer.write( fileName + "|" );
            // Phenotype_Spans|CUI|
            writer.write( getSpanThenCui( tnm, "unmarked" ) );
            // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
            writer.write( createAttributeColumns( jCas, tnm ) );
            // Value_Spans|CUI|  -----> null for TNM
            writer.write( "unmarked|NULL|" );
            // Neoplasm_Spans|CUI|
            writer.write( getSpanThenCui( getNeoplasms( jCas, tnm, neoplasms ), "unmarked" ) );
            // AssociatedTest_Spans|CUI|    --> TNM "Diagnostic Test" is actually the prefix (if present)
            writer.write( getSpanThenCui( getDiagnosticTests( jCas, tnm ), "unmarked" ) );
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private void writePrimaries( final JCas jCas,
                                       final File outputDir,
                                       final String fileName,
                                       final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<IdentifiedAnnotation> primaries = new HashSet<>( neoplasms );
      primaries.removeAll( getMetastases( jCas ) );
      final Collection<IdentifiedAnnotation> bodySides = getBodySides( jCas );
      final Collection<IdentifiedAnnotation> quadrants = getQuadrants( jCas );
      final Collection<IdentifiedAnnotation> clocks = getClockwises( jCas );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation primary : primaries ) {
            // DocName|
            writer.write( fileName + "|" );
            // Phenotype_Spans|CUI|
            writer.write( getSpanThenCui( primary, "unmarked" ) );
            // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
            writer.write( createAttributeColumns( jCas, primary ) );
            // Value_Spans|CUI|  --> unmarked for primaries
            writer.write( "unmarked|NULL|" );
            // Neoplasm_Spans|CUI|  --> already marked for primaries
            writer.write( "unmarked|NULL|" );
            // AssociatedTest_Spans|CUI|
            writer.write( getSpanThenCui( getDiagnosticTests( jCas, primary ), "unmarked" ) );
            // BodySide
            writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, primary, bodySides ), "unmarked" ) );
            // Quadrant
            writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, primary, quadrants ), "unmarked" ) );
            // Clockwise positions
            writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, primary, clocks ), "unmarked" ) );
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private void writeMetastases( final JCas jCas,
                                        final File outputDir,
                                        final String fileName,
                                        final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<IdentifiedAnnotation> metastases = getMetastases( jCas );
      final Collection<IdentifiedAnnotation> primaries = new HashSet<>( neoplasms );
      primaries.removeAll( metastases );
      final Collection<IdentifiedAnnotation> bodySides = getBodySides( jCas );
      final Collection<IdentifiedAnnotation> quadrants = getQuadrants( jCas );
      final Collection<IdentifiedAnnotation> clocks = getClockwises( jCas );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation metastasis : metastases ) {
            final Collection<IdentifiedAnnotation> locations = getLocations( jCas, metastasis );
            if ( locations.isEmpty() ) {
               // DocName|
               writer.write( fileName + "|" );
               // Phenotype_Spans|CUI|
               writer.write( getSpanThenCui( metastasis, "unmarked" ) );
               // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
               writer.write( createAttributeColumns( jCas, metastasis ) );
               // Value_Spans|CUI|  --> unmarked for metastases
               writer.write( "unmarked|NULL|" );
               // Neoplasm_Spans|CUI|
               writer.write( getSpanThenCui( getNeoplasms( jCas, metastasis, primaries ), "unmarked" ) );
               // AssociatedTest_Spans|CUI|
               writer.write( getSpanThenCui( getDiagnosticTests( jCas, metastasis ), "unmarked" ) );
               // BodySide
               writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, metastasis, bodySides ), "unmarked" ) );
               // Quadrant
               writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, metastasis, quadrants ), "unmarked" ) );
               // Clockwise positions
               writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, metastasis, clocks ), "unmarked" ) );
               writer.write( "\n" );
            } else {
               // Metastasis can have multiple locations
               for ( IdentifiedAnnotation location : locations ) {
                  // DocName|
                  writer.write( fileName + "|" );
                  // Phenotype_Spans|CUI|
                  writer.write( getSpanThenCui( metastasis, "unmarked" ) );
                  // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
                  writer.write( createLocatedAttributeColumns( jCas, metastasis, location ) );
                  // Value_Spans|CUI|  --> unmarked for metastases
                  writer.write( "unmarked|NULL|" );
                  // Neoplasm_Spans|CUI|
                  writer.write( getSpanThenCui( getNeoplasms( jCas, metastasis, primaries ), "unmarked" ) );
                  // AssociatedTest_Spans|CUI|
                  writer.write( getSpanThenCui( getDiagnosticTests( jCas, metastasis ), "unmarked" ) );
                  // BodySide
                  writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, metastasis, bodySides ), "unmarked" ) );
                  // Quadrant
                  writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, metastasis, quadrants ), "unmarked" ) );
                  // Clockwise positions
                  writer.write( getSpanThenCui( getNeoplasmModifiers( jCas, metastasis, clocks ), "unmarked" ) );
                  writer.write( "\n" );
               }
            }
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private void writeMedications( final JCas jCas,
                                         final File outputDir,
                                         final String fileName,
                                         final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<MedicationMention> medications = JCasUtil.select( jCas, MedicationMention.class );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation medication : medications ) {
            // DocName|
            writer.write( fileName + "|" );
            // Phenotype_Spans|CUI|
            writer.write( getSpanThenCui( medication, "unmarked" ) );
            // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
            writer.write( createAttributeColumns( jCas, medication ) );
            // Value_Spans|CUI|  --> unmarked for medication
            writer.write( "unmarked|NULL|" );
            // Neoplasm_Spans|CUI|
            writer.write( "unmarked|NULL|" );
            // AssociatedTest_Spans|CUI|  --> unmarked for medication
            writer.write( "unmarked|NULL|" );
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private void writeProcedures( final JCas jCas,
                                        final File outputDir,
                                        final String fileName,
                                        final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<ProcedureMention> procedures = JCasUtil.select( jCas, ProcedureMention.class );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation procedure : procedures ) {
            // DocName|
            writer.write( fileName + "|" );
            // Phenotype_Spans|CUI|
            writer.write( getSpanThenCui( procedure, "unmarked" ) );
            // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
            writer.write( createAttributeColumns( jCas, procedure ) );
            // Value_Spans|CUI|  --> unmarked for procedure
            writer.write( "unmarked|NULL|" );
            // Neoplasm_Spans|CUI|
            writer.write( "unmarked|NULL|" );
            // AssociatedTest_Spans|CUI|  --> unmarked for procedure
            writer.write( "unmarked|NULL|" );
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private void writeSizes( final JCas jCas,
                                   final File outputDir,
                                   final String fileName,
                                   final Collection<IdentifiedAnnotation> neoplasms ) throws IOException {
      final File file = new File( outputDir, fileName + ".pipe" );
      final Collection<IdentifiedAnnotation> sizes = getSizes( jCas );
      try ( BufferedWriter writer = new BufferedWriter( new FileWriter( file ) ) ) {
         for ( IdentifiedAnnotation size : sizes ) {
            // DocName|
            writer.write( fileName + "|" );
            // Phenotype_Spans|CUI|
            writer.write( getSpanThenCui( size, "unmarked" ) );
            // All negation, subject, uncertainty, course, severity, conditional, generic, location, doctimerel
            writer.write( createAttributeColumns( jCas, size ) );
            // Value_Spans|CUI|
            writer.write( getSpanThenCui( PhenotypeAnnotationUtil.getPropertyValues( jCas, size ), "unmarked" ) );
            // Neoplasm_Spans|CUI|
            writer.write( getSpanThenCui( getNeoplasms( jCas, size, neoplasms ), "unmarked" ) );
            // AssociatedTest_Spans|CUI|
            writer.write( getSpanThenCui( getDiagnosticTests( jCas, size ), "unmarked" ) );
            writer.write( "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                     Neoplasms
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @param jCas -
    * @return all neoplasms in the cas
    */
   static private Collection<IdentifiedAnnotation> getAllNeoplasms( final JCas jCas ) {
      return new HashSet<>( Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.NEOPLASM ) );
   }

   /**
    * @param jCas -
    * @return all metastases in the cas
    */
   static private Collection<IdentifiedAnnotation> getMetastases( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.METASTASIS );
   }


   static private Collection<IdentifiedAnnotation> getNeoplasms( final JCas jCas,
                                                                 final IdentifiedAnnotation annotation,
                                                                 final Collection<IdentifiedAnnotation> neoplasms ) {
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      if ( relations == null || relations.isEmpty() ) {
         return Collections.emptyList();
      }
      return RelationUtil.getAllRelated( relations, annotation ).stream()
            .filter( neoplasms::contains )
            .collect( Collectors.toSet() );
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                     Locations
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   static private Collection<IdentifiedAnnotation> getLocations( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.ANATOMY );
   }

   static private Collection<IdentifiedAnnotation> getLocations( final JCas jCas,
                                                                 final IdentifiedAnnotation annotation ) {
      final Collection<LocationOfTextRelation> relations = JCasUtil.select( jCas, LocationOfTextRelation.class );
      if ( relations == null || relations.isEmpty() ) {
         return Collections.emptyList();
      }
      return RelationUtil.getSecondArguments( relations, annotation );
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                     Diagnostic Tests
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   static private Collection<IdentifiedAnnotation> getDiagnosticTests( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.TEST );
   }

   static private Collection<IdentifiedAnnotation> getDiagnosticTests( final JCas jCas,
                                                                       final IdentifiedAnnotation annotation ) {
      final Collection<DiagnosesTextRelation> relations = JCasUtil.select( jCas, DiagnosesTextRelation.class );
      if ( relations == null || relations.isEmpty() ) {
         return Collections.emptyList();
      }
      return RelationUtil.getSecondArguments( relations, annotation );
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                   Modifiers : Quadrant, Body Side, Clockwise Position
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   static private Collection<IdentifiedAnnotation> getQuadrants( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.BREAST_PART );
   }

   static private Collection<IdentifiedAnnotation> getBodySides( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.LATERALITY );
   }

   static private Collection<IdentifiedAnnotation> getClockwises( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.CLOCKFACE );
   }

   static private Collection<IdentifiedAnnotation> getNeoplasmModifiers( final JCas jCas,
                                                                         final IdentifiedAnnotation neoplasm,
                                                                         final Collection<IdentifiedAnnotation> modifiers ) {
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      if ( relations == null || relations.isEmpty() ) {
         return Collections.emptyList();
      }
      return RelationUtil.getAllRelated( relations, neoplasm ).stream()
            .filter( modifiers::contains )
            .collect( Collectors.toSet() );
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                    TNM, Stage
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   static private Collection<IdentifiedAnnotation> getTNMs( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.TNM );
   }

   static private Collection<IdentifiedAnnotation> getStages( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.STAGE );
   }

   static private Collection<IdentifiedAnnotation> getReceptors( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.RECEPTOR_STATUS );
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                           Size
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   static private Collection<IdentifiedAnnotation> getSizes( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, SizePropertyUtil.getParentUri() ).stream()
                                     .filter( a -> !a.getCoveredText().toLowerCase().contains( "size" ) ).collect( Collectors.toList() );
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                           Medication
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   static private Collection<IdentifiedAnnotation> getMedications( final JCas jCas ) {
      return Neo4jOntologyConceptUtil.getAnnotationsByUriBranch( jCas, UriConstants.DRUG );
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                           Temporal
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * @return Before, Before/Overlap, Overlap, After
    */
   static private String getDocTimeRel( final IdentifiedAnnotation annotation ) {
      final EventProperties properties = getEventProperties( annotation );
      if ( properties != null ) {
         final String dtr = properties.getDocTimeRel();
         return dtr == null ? "" : dtr;
      }
      return "";
   }

   /**
    * @param annotation -
    * @return type system EventProperties
    */
   static private EventProperties getEventProperties( final IdentifiedAnnotation annotation ) {
      if ( !EventMention.class.isInstance( annotation ) ) {
         return null;
      }
      final Event event = ((EventMention) annotation).getEvent();
      if ( event != null ) {
         return event.getProperties();
      }
      return null;
   }


   /////////////////////////////////////////////////////////////////////////////////////////////////////
   //                                        Utility
   /////////////////////////////////////////////////////////////////////////////////////////////////////

   static private String createAttributeColumns( final JCas jCas, final IdentifiedAnnotation annotation ) {
      final StringBuilder sb = new StringBuilder();
      // Neg_value|Neg_span|
      sb.append( getAttributeYN( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT ) );
      // Subj_value|Subj_span|
      sb.append( getAttributeText( annotation.getSubject(), "patient" ) );
      // Uncertain_value|Uncertain_span|
      sb.append( getAttributeYN( annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT ) );
      // Course_value|Course_span|
      sb.append( "NULL|unmarked|" );
      // Severity_value|Severity_span|
      sb.append( "NULL|unmarked|" );
      // Cond_value|Cond_span|
      sb.append( getAttributeTF( annotation.getConditional() ) );
      // Generic_value|Generic_span|
      sb.append( getAttributeTF( annotation.getGeneric() ) );
      // Bodyloc_value|Bodyloc_span|
      sb.append( getCuiThenSpan( getLocations( jCas, annotation ), "unmarked" ) );
      // DocTimeRel|DocTimeRelSpan|
      sb.append( getAttributeText( getDocTimeRel( annotation ), "unmarked" ) );
      return sb.toString();
   }

   static private String createLocatedAttributeColumns( final JCas jCas,
                                                        final IdentifiedAnnotation annotation,
                                                        final IdentifiedAnnotation location ) {
      final StringBuilder sb = new StringBuilder();
      // Neg_value|Neg_span|
      sb.append( getAttributeYN( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT ) );
      // Subj_value|Subj_span|
      sb.append( getAttributeText( annotation.getSubject(), "patient" ) );
      // Uncertain_value|Uncertain_span|
      sb.append( getAttributeYN( annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT ) );
      // Course_value|Course_span|
      sb.append( "NULL|unmarked|" );
      // Severity_value|Severity_span|
      sb.append( "NULL|unmarked|" );
      // Cond_value|Cond_span|
      sb.append( getAttributeTF( annotation.getConditional() ) );
      // Generic_value|Generic_span|
      sb.append( getAttributeTF( annotation.getGeneric() ) );
      // Bodyloc_value|Bodyloc_span|
      sb.append( getCuiThenSpan( location, "unmarked" ) );
      // DocTimeRel|DocTimeRelSpan|
      sb.append( getAttributeText( getDocTimeRel( annotation ), "unmarked" ) );
      return sb.toString();
   }

   static private String getAttributeYN( final boolean isAttributePositive ) {
      final String value = isAttributePositive ? "yes" : "no";
      return value + "|unmarked|";
   }

   static private String getAttributeTF( final boolean isAttributePositive ) {
      final String value = isAttributePositive ? "true" : "false";
      return value + "|unmarked|";
   }

   static private String getAttributeText( final String attributeValue, final String defaultText ) {
      final String value = (attributeValue == null || attributeValue.isEmpty()) ? defaultText : attributeValue;
      return value + "|unmarked|";
   }


   static private IdentifiedAnnotation getFirstAnnotation( final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream().findFirst().orElse( null );
   }

   static private String getSpanThenCui( final Collection<IdentifiedAnnotation> annotations,
                                         final String defaultText ) {
      return getSpanThenCui( getFirstAnnotation( annotations ), defaultText );
   }

   /**
    * @param annotation  -
    * @param defaultText -
    * @return Span | CUI
    */
   static private String getSpanThenCui( final IdentifiedAnnotation annotation, final String defaultText ) {
      return getSpan( annotation, defaultText ) + '|' + getCui( annotation ) + '|';
   }

   /**
    * @param annotations -
    * @param defaultText -
    * @return CUI | Span
    */
   static private String getCuiThenSpan( final Collection<IdentifiedAnnotation> annotations,
                                         final String defaultText ) {
      return getCuiThenSpan( getFirstAnnotation( annotations ), defaultText );
   }

   /**
    * @param annotation  -
    * @param defaultText -
    * @return CUI | Span
    */
   static private String getCuiThenSpan( final IdentifiedAnnotation annotation,
                                         final String defaultText ) {
      return getCui( annotation ) + '|' + getSpan( annotation, defaultText ) + '|';
   }

   static private String getSpan( final IdentifiedAnnotation annotation, final String defaultSpanText ) {
      return annotation == null ? defaultSpanText : (annotation.getBegin() + "-" + annotation.getEnd());
   }

   static private String getCui( final IdentifiedAnnotation annotation ) {
      final String cuiValue = getCuiValue( annotation );
      return cuiValue == null ? "NULL" : cuiValue;
   }

   static private String getCuiValue( final IdentifiedAnnotation annotation ) {
      if ( annotation == null ) {
         return null;
      }
      final Collection<String> cuis = OntologyConceptUtil.getCuis( annotation );
      return cuis.isEmpty() ? null : cuis.stream().filter( c -> !c.isEmpty() ).findFirst().orElse( null );
   }


   public static AnalysisEngineDescription createAnnotatorDescription( String outputDirectoryPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory
            .createEngineDescription( SemevalBsvWriter2.class, "OutputDirectory", outputDirectoryPath );
   }

}
