package org.healthnlp.deepphe.summary.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.node.NoteNodeCreator;
import org.healthnlp.deepphe.node.PatientCreator;
import org.healthnlp.deepphe.node.PatientNodeStore;
import org.healthnlp.deepphe.node.PatientSummaryXnNodeStore;
import org.healthnlp.deepphe.summary.engine.FactRelationUtil;
import org.healthnlp.deepphe.summary.engine.MultiSummaryEngine;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_CLOCKFACE;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_QUADRANT;

/**
 * @author SPF , chip-nlp
 * @since {4/30/2021}
 */
public class EvalFileWriterXn extends AbstractFileWriter<Patient> {

   static private final Logger LOGGER = Logger.getLogger( "EvalFileWriterXn" );

   // TODO: Create Cancer and Tumor Node.  Similar to NeoplasmSummary, but contain Fact instead of Mention etc.
   // Refactor Neo4j writer and reader to handle Cancer, Tumor, Fact instead of NeoplasmSummary.  dphe-xn will use.
   // Refactor Neo4j plugin to use Cancer, Tumor, Fact.  dphe-xn will use.  dphe-cr will NOT.
   // Refactor MultiSummaryEngine to create Cancer, Tumor, Fact instead of NeoplasmSummary.  dphe-xn will use.

   // This can keep -cr and -xn separate without too much bother.




   @ConfigurationParameter(
         name = "CancerEvalFile",
         description = "The Path to the File for Cancer Evaluation output.",
         mandatory = true
   )
   private String _cancerEvalPath;

   private Patient _patient;

   private boolean _needHeader;


   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _needHeader = true;
   }


   /**
    * Sets data to be written to the jcas.
    *
    * @param jCas ye olde
    */
   @Override
   protected void createData( final JCas jCas ) {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
      _patient = PatientNodeStore.getInstance().getOrCreate( patientId );
      final Note note = NoteNodeCreator.createNote( jCas );
      PatientCreator.addNote( _patient, note );
   }

   /**
    * @return the JCas.
    */
   @Override
   protected Patient getData() {
      return _patient;
   }

   /**
    * called after writing is complete
    *
    * @param data -
    */
   @Override
   protected void writeComplete( final Patient data ) {
   }

   /**
    * Don't use the document / patient for subdirectories
    * {@inheritDoc}
    */
   @Override
   protected String getSubdirectory(JCas jCas, String documentId) {
      return this.getSimpleSubDirectory();
   }

   /**
    * Write information into a file named based upon the document id and located based upon the document id prefix.
    *
    * This will write one file per patient, named after the patient, with each row containing columns of cuis.
    *
    * @param patient       data to be written
    * @param outputDir  output directory
    * @param documentId -- not used --
    * @param fileName   -- not used --
    * @throws IOException if anything goes wrong
    */
   public void writeFile( final Patient patient,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final File cancerFile = new File( outputDir, _cancerEvalPath + "_cancer.bsv" );
      final File tumorFile = new File( outputDir, _cancerEvalPath + "_tumor.bsv" );
      if ( _needHeader ) {
         try ( Writer cancerWriter = new BufferedWriter( new FileWriter( cancerFile ) ) ) {
            cancerWriter.write( CANCER_HEADER );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
         try ( Writer tumorWriter = new BufferedWriter( new FileWriter( tumorFile ) ) ) {
            tumorWriter.write( TUMOR_HEADER );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
         _needHeader = false;
      }
      final String patientId = patient.getId();
      // Even though the ctakes PatientNoteStore isn't being used to store any patient jcas, it still has note counts.
      final int patientDocCount = PatientNoteStore.getInstance()
                                                  .getWantedDocCount( patientId );
      if ( patient.getNotes().size() < patientDocCount ) {
//         LOGGER.info( patientId + " " + patient.getNotes().size() + " of " + patientDocCount );
         return;
      }
      // Somebody else may have already created the patient summary.
      PatientSummaryXn patientSummary = PatientSummaryXnNodeStore.getInstance().get( patientId );
      if ( patientSummary == null ) {
         // Create PatientSummary
         patientSummary = MultiSummaryEngine.createPatientSummaryXn( patient );
         // Add the summary just in case some other consumer can utilize it.  e.g. eval file writer.
         PatientSummaryXnNodeStore.getInstance().add( patientId, patientSummary );
      }

      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      final String summaryJson = gson.toJson( patientSummary );
      final File file = new File( outputDir, patientId + ".json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( summaryJson );
      }

      final Map<String,Fact> idFactMap =
            patientSummary.getFacts()
                          .stream()
                          .collect( Collectors.toMap( Fact::getId, Function.identity() ) );
      final List<Cancer> cancers = patientSummary.getCancers();
         final Collection<String> cancerLines = new HashSet<>();
         final Collection<String> tumorLines = new HashSet<>();
         for ( Cancer cancer : cancers ) {
            final Collection<NeoplasmAttribute> cancerAttributes = cancer.getAttributes();
            final Map<String,List<String>> cancerIdRelatedIdMap = cancer.getRelatedFactIds();
            final Map<String,List<Fact>> cancerRelated = FactRelationUtil.getRelatedFactsMap( cancerIdRelatedIdMap,
                                                                                             idFactMap );
            cancerLines.add( createCancerLine( patientId, cancer, cancerAttributes, cancerRelated ) );
            final String cancerId = cancer.getId();

            for ( Tumor tumor : cancer.getTumors() ) {
               final Collection<NeoplasmAttribute> tumorAttributes = tumor.getAttributes();
               final Map<String,List<String>> tumorIdRelatedIdMap = tumor.getRelatedFactIds();
               final Map<String,List<Fact>> tumorRelated = FactRelationUtil.getRelatedFactsMap( tumorIdRelatedIdMap,
                                                                                             idFactMap );

               tumorLines.add( createTumorLine( patientId, cancerId, tumor, tumorAttributes, tumorRelated,
                                                cancerAttributes, cancerRelated ) );
            }
         }
      try ( Writer cancerWriter = new BufferedWriter( new FileWriter( cancerFile, true ) );
            Writer tumorWriter = new BufferedWriter( new FileWriter( tumorFile, true ) ) ) {
         for ( String cancerLine : cancerLines ) {
            cancerWriter.write( cancerLine );
         }
         for ( String tumorLine : tumorLines ) {
            tumorWriter.write( tumorLine );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   static private final String CANCER_HEADER = "*Patient_ID|"
                                               + "-Summary_Type|"
                                               + "-Summary_ID|"
                                               + "-Summary_URI|"
                                               + "-hasCancerType|"
                                               + "-hasHistologicType|"
                                               + "-hasHistoricity|"
                                               + "*hasBodySite|"
                                               + "hasLaterality|"
                                               + "hasCancerStage|"
                                               + "has_Clinical_T|"
                                               + "has_Clinical_N|"
                                               + "has_Clinical_M|"
                                               + "-has_Pathologic_T|"
                                               + "-has_Pathologic_N|"
                                               + "-has_Pathologic_M|"
                                               + "-hasMethod|"
                                               + "-hasTreatment|"
                                               + "-Regimen_Has_Accepted_Use_For_Disease|"
                                               + "-Disease_Has_Normal_Tissue_Origin|"
                                               + "-Disease_Has_Normal_Cell_Origin|"
                                               + "-Disease_Has_Finding|"
                                               + "-Disease_May_Have_Finding|"
                                               + "-has_PSA_Level|"
                                               + "-has_Gleason_Score|\n";



   static private String createCancerLine( final String patientId,
                                           final Cancer cancer,
                                           final Collection<NeoplasmAttribute> cancerAttributes,
                                           final Map<String,List<Fact>> cancerRelated ) {
      if ( cancer.getClassUri().equals( UriConstants.UNKNOWN ) ) {
         return "";
      }
      if ( getSite( cancerAttributes, cancerRelated ).isEmpty() ) {
         return "";
      }
      return patientId + "|" + "Cancer|" + cancer.getId() + "|" + cancer.getClassUri() + "|"
            + getRelatedUri( cancerRelated, "hasCancerType" ) + "|"
             + getAttributeValue( cancerAttributes, "histology" ) + "|"
             + getRelatedUri( cancerRelated, "hasHistoricity" ) + "|"
             + getSite( cancerAttributes, cancerRelated ) + "|"
             + getLaterality( cancerAttributes ) + "|"
             + getStage( cancerAttributes, cancerRelated, cancer ) + "|"
             + getAttributeValue( cancerAttributes, "t" ) + "|"
             + getAttributeValue( cancerAttributes, "n" ) + "|"
             + getAttributeValue( cancerAttributes, "m" ) + "|"
             + getAttributeValue( cancerAttributes, "t" ) + "|"
             + getAttributeValue( cancerAttributes, "n" ) + "|"
             + getAttributeValue( cancerAttributes, "m" ) + "|"
             + getRelatedUri( cancerRelated, "hasMethod" ) + "|"
             + getRelatedUri( cancerRelated, "hasTreatment" ) + "|"
             + getRelatedUri( cancerRelated, "Regimen_Has_Accepted_Use_For_Disease" ) + "|"
             + getRelatedUri( cancerRelated, "Disease_Has_Normal_Tissue_Origin" ) + "|"
             + getRelatedUri( cancerRelated, "Disease_Has_Normal_Cell_Origin" ) + "|"
             + getRelatedUri( cancerRelated, "Disease_Has_Finding" ) + "|"
             + getRelatedUri( cancerRelated, "Disease_May_Have_Finding" ) + "|"
             + getAttributeValue( cancerAttributes, "PSA" ) + "|"
             + getRelatedUri( cancerRelated, "hasGleasonScore" ) + "|\n";
   }


   static private final String TUMOR_HEADER = "*Patient_ID|"
                                              + "-Cancer_ID|"
                                              + "-Summary_Type|"
                                              + "-Summary_ID|"
                                              + "-Summary_URI|"
                                              + "-hasCancerType|"
                                              + "-hasHistologicType|"
                                              + "-hasHistoricity|"
                                              + "*hasBodySite|"
                                              + "hasLaterality|"
                                              + "hasDiagnosis|"
                                              + "-isMetastasisOf|"
                                              + "-hasTumorType|"
                                              + "-hasTumorExtent|"
                                              + "-hasMethod|"
                                              + "-hasTreatment|"
                                              + "-Disease_Has_Normal_Tissue_Origin|"
                                              + "-Disease_Has_Normal_Cell_Origin|"
                                              + "-hasSize|"
                                              + "-hasCalcification|"
                                              + "-has_Ulceration|"
                                              + "-has_Breslow_Depth|"
                                              + "hasQuadrant|"
                                              + "hasClockface|"
                                              + "has_ER_Status|"
                                              + "has_PR_Status|"
                                              + "has_HER2_Status|"
                                              + "hasGrade|\n";

//               tumorLines.add( createTumorLine( patientId, cancerId, tumor, tumorAttributes, tumorRelated,
//                                                cancerSite, cancerAttributes, cancerRelated ) );
   static private String createTumorLine( final String patientId,
                                          final String cancerId,
                                          final Tumor tumor,
                                          final Collection<NeoplasmAttribute> tumorAttributes,
                                          final Map<String,List<Fact>> tumorRelated,
                                          final Collection<NeoplasmAttribute> cancerAttributes,
                                          final Map<String,List<Fact>> cancerRelated ) {
      if ( takeFirst( getSite( tumorAttributes, tumorRelated ), getSite( cancerAttributes, tumorRelated ) ).isEmpty() ) {
         return "";
      }
      return patientId + "|" + cancerId + "|" + "Tumor|" + tumor.getId() + "|" + tumor.getClassUri() + "|"
             + tumor.getClassUri() + "|"
             + takeFirst( getAttributeValue( tumorAttributes, "histology" ),
                          getAttributeValue( cancerAttributes, "histology" ) )+ "|"
             + getRelatedUri( tumorRelated, "hasHistoricity" ) + "|"
             + takeFirst( getSite( tumorAttributes, tumorRelated ), getSite( cancerAttributes, tumorRelated ) ) + "|"
             + takeFirst(  getLaterality( tumorAttributes ),
                           getLaterality( cancerAttributes ) ) + "|"
             + takeFirst( getRelatedUri( tumorRelated, "hasDiagnosis" ),
                          getRelatedUri( cancerRelated, "hasDiagnosis" ) ) + "|"
             + getRelatedUri( tumorRelated, "isMetastasisOf" ) + "|"
             + getRelatedUri( tumorRelated, "hasTumorType" ) + "|"
             + getRelatedUri( tumorRelated, "hasTumorExtent" ) + "|"
             + getRelatedUri( tumorRelated, "hasMethod" ) + "|"
             + getRelatedUri( tumorRelated, "hasTreatment" ) + "|"
             + getRelatedUri( tumorRelated, "Disease_Has_Normal_Tissue_Origin" ) + "|"
             + getRelatedUri( tumorRelated, "Disease_Has_Normal_Cell_Origin" ) + "|"
             + getRelatedValue( tumorRelated, "hasSize" ) + "|"
             + getRelatedUri( tumorRelated, "hasCalcification" ) + "|"
             + getRelatedUri( tumorRelated, "has_Ulceration" ) + "|"
             + getRelatedUri( tumorRelated, "has_Breslow_Depth" ) + "|"
             + takeFirst( getQuadrant( tumorAttributes, tumorRelated ),
                          getQuadrant( cancerAttributes,  cancerRelated ) ) + "|"
             + takeFirst( getClockface( tumorAttributes, tumorRelated ),
                          getClockface( cancerAttributes, cancerRelated ) ) + "|"
             + takeFirst( getStatus( tumorAttributes, "ER_", tumorRelated, RelationConstants.has_Biomarker, "estro" ),
                          getStatus( cancerAttributes, "ER_", cancerRelated, RelationConstants.has_Biomarker, "estro" ) )+ "|"
             + takeFirst( getStatus( tumorAttributes, "PR_", tumorRelated, RelationConstants.has_Biomarker, "progest" ),
                          getStatus( cancerAttributes, "PR_", cancerRelated, RelationConstants.has_Biomarker, "progest" ) ) + "|"
             + takeFirst( getStatus( tumorAttributes, "HER2", tumorRelated, RelationConstants.has_Biomarker, "her2" ) ,
                          getStatus( cancerAttributes, "HER2", cancerRelated, RelationConstants.has_Biomarker,
                                     "her2" ) ) + "|"
             + takeFirst( getAttributeUri( tumorAttributes, "grade" ),
                          getAttributeUri( cancerAttributes, "grade" ) )
                          + "|\n";
   }

   static private String takeFirst( final String one, final String two ) {
      if ( !one.isEmpty() ) {
         return one;
      }
      return two;
   }

   static private final String[] SITE_RELATIONS = { RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                                                    RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
                                                    RelationConstants.DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
                                                    RelationConstants.Disease_Has_Associated_Region,
                                                    RelationConstants.Disease_Has_Associated_Cavity,
                                                    RelationConstants.Finding_Has_Associated_Site,
                                                    RelationConstants.Finding_Has_Associated_Region,
                                                    RelationConstants.Finding_Has_Associated_Cavity };

   static private String getSite( final Collection<NeoplasmAttribute> attributes,
                                  final Map<String,List<Fact>> relatedFacts ) {
      final String topography_major = getAttributeUri( attributes, "topography_major" ).replace( "DeepPhe", "" );
      if ( !topography_major.isEmpty() ) {
         return topography_major;
      }
      for ( String siteRelation : SITE_RELATIONS ) {
         final String siteUri = getRelatedUri( relatedFacts, siteRelation );
         if ( !siteUri.isEmpty() ) {
            return siteUri;
         }
      }
      return "";
   }

   static private String getStage( final Collection<NeoplasmAttribute> attributes,
                                   final Map<String,List<Fact>> relatedFacts,
                                  final Cancer cancer ) {
      final String stage = getAttributeValue( attributes, "stage" );
      if ( !stage.isEmpty() ) {
         return stage;
      }
      final String uriStage = getDiagnosisStage( cancer.getClassUri() );
      if ( !uriStage.isEmpty() ) {
         return uriStage;
      }
      final String diagnoses = getRelatedUri( relatedFacts, RelationConstants.HAS_DIAGNOSIS );
      if ( !diagnoses.isEmpty() ) {
         final String[] splits = StringUtil.fastSplit( diagnoses, ';' );
         return Arrays.stream( splits )
               .map( EvalFileWriterXn::getDiagnosisStage )
               .filter( s -> !s.isEmpty() ).collect( Collectors.joining(";") );
      }
      return "";
   }

   static private String getDiagnosisStage( final String diagnosis ) {
      if ( diagnosis.startsWith( "Stage_" ) ) {
         final int nextScore = diagnosis.indexOf( '_', 6 );
         if ( nextScore > 6 ) {
            return diagnosis.substring( 6, nextScore );
         }
      }
      return "";
   }

   static private String getFacts( final Map<String,List<Fact>> relatedFacts, final String rootUri ) {
      final Collection<String> uris = Neo4jOntologyConceptUtil.getBranchUris( rootUri );
      return relatedFacts.values()
                         .stream()
                         .flatMap( Collection::stream )
                         .map( Fact::getClassUri )
                         .filter( uris::contains )
                         .distinct().collect( Collectors.joining(";") );
   }

   static private String getQuadrant( final Collection<NeoplasmAttribute> attributes,
                                       final Map<String,List<Fact>> relatedFacts ) {
      final Collection<String> quadrants
            = new HashSet<>( Arrays.asList( getAttributeUri( attributes, "topography_minor", UriConstants.QUADRANT )
                                   .split( ";" ) ) );
      quadrants.addAll( Arrays.asList( getRelatedUri( relatedFacts, HAS_QUADRANT )
                                          .split( ";" ) ) );
//      quadrants.addAll( Arrays.asList( getFacts( relatedFacts, UriConstants.QUADRANT ).split( ";" ) ) );
      return String.join( ";", quadrants );
   }

   static private String getClockface( final Collection<NeoplasmAttribute> attributes,
                                       final Map<String,List<Fact>> relatedFacts ) {
      final Collection<String> clocks
            = new HashSet<>( Arrays.asList( getAttributeUri( attributes, "topography_minor", UriConstants.CLOCKFACE )
                                   .split( ";" ) ) );
      clocks.addAll( Arrays.asList( getRelatedUri( relatedFacts, HAS_CLOCKFACE )
                                          .split( ";" ) ) );
      return clocks.stream()
                   .map( c -> c.startsWith( "_" ) ? c.substring( 1 ) : c )
                   .collect( Collectors.joining(";") );
   }


   static private String getStatus(  final Collection<NeoplasmAttribute> attributes,
                                     final String attributeName,
                                     final Map<String,List<Fact>> relatedFacts,
                                     final String relationName,
                                     final String relationUri ) {
      final Collection<String> status = attributes.stream()
                                                  .filter( a -> a.getName()
                                                                 .equalsIgnoreCase( attributeName ) )
                                                  .map( NeoplasmAttribute::getValue )
                                                  .collect( Collectors.toSet() );
      status.addAll( relatedFacts.getOrDefault( relationName, Collections.emptyList() )
                                                          .stream()
                                                          .map( Fact::getClassUri )
                                 .filter( u -> u.toLowerCase().contains( relationUri ) )
                                                          .collect( Collectors.toSet() ) );
      return String.join( ";", status );
   }

   static private String getLaterality( final Collection<NeoplasmAttribute> attributes ) {
      final String site = getAttributeUri( attributes, "topography_major" );
      if ( site.startsWith( "Right_" ) ) {
         return "Right";
      } else if ( site.startsWith( "Left_" ) ) {
         return "Left";
      } else if ( site.startsWith( "Bilateral_" ) ) {
         return "Bilateral";
      }
      return getAttributeUri( attributes, "laterality" );
   }

   static private String getAttributeValue( final Collection<NeoplasmAttribute> attributes,
                                            final String attributeName ) {
      return attributes.stream()
                .filter( a -> a.getName().equalsIgnoreCase( attributeName ) )
                .map( NeoplasmAttribute::getValue )
                       .distinct()
                .collect( Collectors.joining( ";" ) );
   }

   static private String getAttributeUri( final Collection<NeoplasmAttribute> attributes,
                                          final String attributeName ) {
      return attributes.stream()
                       .filter( a -> a.getName().equalsIgnoreCase( attributeName ) )
                       .map( NeoplasmAttribute::getClassUri )
                       .distinct()
                       .collect( Collectors.joining( ";" ) );
   }

   static private String getAttributeUri( final Collection<NeoplasmAttribute> attributes,
                                          final String attributeName, final String rootUri ) {
      final Collection<String> uris = Neo4jOntologyConceptUtil.getBranchUris( rootUri );
      return attributes.stream()
                       .filter( a -> a.getName().equalsIgnoreCase( attributeName ) )
                       .map( NeoplasmAttribute::getClassUri )
                       .filter( uris::contains )
                       .distinct()
                       .collect( Collectors.joining( ";" ) );
   }

   static private String getRelatedUri( final Map<String,List<Fact>> relatedFacts,
                                        final String relationName ) {
      return relatedFacts.getOrDefault( relationName, Collections.emptyList() )
                         .stream()
                       .map( Fact::getClassUri )
                       .distinct()
                       .collect( Collectors.joining( ";" ) );
   }

   static private String getRelatedUri( final Map<String,List<Fact>> relatedFacts,
                                        final String relationName,
                                        final String rootUri ) {
      final Collection<String> uris = Neo4jOntologyConceptUtil.getBranchUris( rootUri );
      return relatedFacts.getOrDefault( relationName, Collections.emptyList() )
                         .stream()
                         .map( Fact::getClassUri )
                         .filter( uris::contains )
                         .distinct()
                         .collect( Collectors.joining( ";" ) );
   }

   static private String getRelatedValue( final Map<String,List<Fact>> relatedFacts,
                                          final String relationName ) {
      return relatedFacts.getOrDefault( relationName, Collections.emptyList() )
                         .stream()
                         .map( Fact::getValue )
                         .distinct()
                         .collect( Collectors.joining( ";" ) );
   }


}
