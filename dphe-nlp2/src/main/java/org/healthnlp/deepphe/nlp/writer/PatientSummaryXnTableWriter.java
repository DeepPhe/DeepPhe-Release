package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.neo4j.node.xn.*;
import org.healthnlp.deepphe.nlp.patient.PatientSummaryXnStore;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @author SPF , chip-nlp
 * @since {4/26/2021}
 */
final public class PatientSummaryXnTableWriter extends AbstractFileWriter<PatientSummaryXn> {
   static private final Logger LOGGER = Logger.getLogger( "PatientSummaryXnTableWriter" );


   static public final String ZIP_TABLE_PARAM = "ZipTable";
   static public final String ZIP_TABLE_DESC = "Write table file in a zip file.";
   @ConfigurationParameter(
         name = ZIP_TABLE_PARAM,
         description = ZIP_TABLE_DESC,
         mandatory = false,
         defaultValue = "no"
   )
   private String _zipTable;

   static public final String ZIP_MAX_PARAM = "ZipMax";
   static public final String ZIP_MAX_DESC = "The maximum number of files per zip.";
   @ConfigurationParameter(
         name = ZIP_MAX_PARAM,
         description = ZIP_MAX_DESC,
         mandatory = false
   )
   private int _zipMax = 0;

   static public final String PATIENT_MAX_PARAM = "PatientMax";
   static public final String PATIENT_MAX_DESC = "The maximum number of patients per table.";
   @ConfigurationParameter(
         name = PATIENT_MAX_PARAM,
         description = PATIENT_MAX_DESC,
         mandatory = false
   )
   private int _patientMax = 0;

   static private final List<String> CANCER_ATTRIBUTES = Arrays.asList(
         "Location",
         "Topography, major", "Topography, minor", "Laterality",
         "Lymph Involvement", "Metastatic Site", "Histology", "Grade", "Stage",
         "T Stage", "N Stage", "M Stage", "Course", "Test Results",
         "Treatments", "Procedures", "Genes", "Comorbidities"
   );

   static private final List<String> TUMOR_ATTRIBUTES = Arrays.asList(
         "Location",
         "Topography, major", "Topography, minor", "Laterality",
         "Clockface", "Quadrant", "Grade", "Tissue", "Behavior", "Receptor Status",
         "Test Results"
   );

   static private final List<String> BIOMARKERS = Arrays.asList(
         "Estrogen Receptor Status",
         "Progesterone Receptor Status",
         "HER2/Neu Status",
         "Antigen KI-67",
         "Breast Cancer Type 1 Susceptibility Protein",
         "Breast Cancer Type 2 Susceptibility Protein",
         "ALK Tyrosine Kinase Receptor",
         "Epidermal Growth Factor Receptor",
         "Serine/Threonine-Protein Kinase B-Raf",
         "Proto-Oncogene Tyrosine-Protein Kinase ROS",
         "Programmed Cell Death Protein 1",
         "Microsatellite Stable",
         "GTPase KRas",
         "Prostate-Specific Antigen"
//         , "Prostate-Specific Antigen El"
   );


   static private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern( "ddMMyyyykkmmssS" );

   private PatientSummaryXn _patientSummary;

//   private int _zipIndex = 1;
//   private int _zipCount = 0;
   private int _patientIndex = 1;
   private int _patientCount = 1;
   static private final String FILE_NAME = "DeepPhe_Table";
   // Prevent asynchronous overwrite by multiple processes
   private String _timeText;
   // TODO - if we definitely know that jdk version is 9+, use the pid instead of millis.
//   long pid = ProcessHandle.current().pid();


   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _timeText = TIME_FORMATTER.format( OffsetDateTime.now() );
   }

   /**
    * Sets data to be written to the jcas.
    *
    * @param jCas ye olde
    */
   @Override
   protected void createData( final JCas jCas ) {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
       if (!PatientDocCounter.getInstance().isPatientFull(patientId)) {
           _patientSummary = null;
           return;
       }
      _patientSummary = PatientSummaryXnStore.getInstance().get( patientId );
   }

   /**
    * @return the JCas.
    */
   @Override
   protected PatientSummaryXn getData() {
      if ( _patientSummary == null ) {
         return null;
      }
      return _patientSummary;
   }

   /**
    * called after writing is complete
    *
    * @param patientSummary -
    */
   @Override
   protected void writeComplete( final PatientSummaryXn patientSummary ) {
   }

   /**
    * If we are zipping the files returns the root directory.  Otherwise, runs the super method.
    * {@inheritDoc}
    */
   @Override
   protected String getOutputDirectory( final JCas jcas, final String rootPath, final String documentId ) {
      return rootPath;
   }

   /**
    * Write information into a file named based upon the document id and located based upon the document id prefix.
    * This will write one file per patient, named after the patient, with each row containing columns of cuis.
    *
    * @param patientSummary data to be written
    * @param outputDir  output directory
    * @param documentId -- not used --
    * @param fileName   -- not used --
    * @throws IOException if anything goes wrong
    */
   public void writeFile( final PatientSummaryXn patientSummary,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      if ( patientSummary == null ) {
         return;
      }
      final String patientId = patientSummary.getId();
       if (!PatientDocCounter.getInstance().isPatientFull(patientId)) {
           return;
       }
      final String cancerRows = compileCancers( patientSummary );
      final String tumorRows = compileTumors( patientSummary );
      // Not working with append
//      if ( AeParamUtil.isTrue( _zipTable ) ) {
//         writeFullZip( "Cancer", cancerRows, outputDir );
//         writeFullZip( "Tumor", tumorRows, outputDir );
//         return;
//      }
      final File cancerFile = new File( outputDir, getFileName("Cancer" ) );
      final File tumorFile = new File( outputDir, getFileName("Tumor" ) );
      try ( Writer writer = new BufferedWriter( new FileWriter( cancerFile, true ) ) ) {
         writer.append( cancerRows );
      }
      try ( Writer writer = new BufferedWriter( new FileWriter( tumorFile, true ) ) ) {
         writer.append( tumorRows );
      }
      incrementCount( cancerFile, tumorFile );
   }
   
   static private String compileCancers( final PatientSummaryXn patient ) {
      final StringBuilder sb = new StringBuilder();
      final String patientId = patient.getId();
      final List<CancerSummaryXn> cancers = new ArrayList<>( patient.getCancers() );
      if ( cancers.isEmpty() ) {
         sb.append( patientId );
         for ( int i = 0; i < 3 + CANCER_ATTRIBUTES.size(); i++ ) {
            sb.append( '|' );
         }
         LOGGER.info( "No Cancer for " + patientId );
         return sb.append( "\n" ).toString();
      }
      cancers.sort( Comparator.comparingDouble( CancerSummaryXn::getdConfidence ).reversed() );
      for ( CancerSummaryXn cancer : cancers ) {
         final List<AttributeXn> attributes = cancer.getAttributes();
         final String attributesText = compileCancerAttributes( attributes );
         if ( emptyAttributes( attributesText ) ) {
            continue;
         }
         sb.append( patientId ).append( '|' ).append( cancer.getId() ).append( '|' )
           .append( UriInfoCache.getInstance().getPrefText( cancer.getClassUri() ) )
//           .append( UriInfoCache.getInstance().getCui( cancer.getClassUri() ) )
           .append( ':' ).append( cancer.getConfidence() ).append( '|' );
         sb.append( attributesText ).append( "\n" );
      }
      return sb.toString();
   }

   static private boolean emptyAttributes( final String attributesText ) {
      if ( String.join( "", StringUtil.fastSplit( attributesText, '|' ) ).isEmpty() ) {
         LogFileWriter.add( "No Attributes");
         return true;
      }
      return false;
   }

   static private String compileTumors( final PatientSummaryXn patient ) {
      final StringBuilder sb = new StringBuilder();
      final String patientId = patient.getId();
      final Map<String, TumorSummaryXn> tumorMap = new HashMap<>();
      patient.getCancers()
             .stream()
             .map( CancerSummaryXn::getTumors )
             .flatMap( Collection::stream )
             .forEach( t -> tumorMap.putIfAbsent( t.getId(), t ) );
      if ( tumorMap.isEmpty() ) {
         sb.append( patientId );
         for ( int i = 0; i < 3 + TUMOR_ATTRIBUTES.size(); i++ ) {
            sb.append( '|' );
         }
         LOGGER.info( "No Tumor for " + patientId );
         return sb.append( "\n" ).toString();
      }
      final List<TumorSummaryXn> tumors = new ArrayList<>( tumorMap.values() );
      tumors.sort( Comparator.comparingDouble( TumorSummaryXn::getdConfidence ).reversed() );
      for ( TumorSummaryXn tumor : tumors ) {
         final List<AttributeXn> attributes = tumor.getAttributes();
         final String attributesText = compileTumorAttributes( attributes );
         if ( emptyAttributes( attributesText ) ) {
            continue;
         }
         sb.append( patientId ).append( '|' ).append( tumor.getId() ).append( '|' )
           .append( UriInfoCache.getInstance().getPrefText( tumor.getClassUri() ) )
//           .append( UriInfoCache.getInstance().getCui( tumor.getClassUri() ) )
           .append( ':' ).append( tumor.getConfidence() ).append( '|' );
         sb.append( attributesText ).append( "\n" );
      }
      return sb.toString();
   }


   static private String compileCancerAttributes( final List<AttributeXn> attributes ) {
      return compileAttributes( CANCER_ATTRIBUTES, attributes );
   }

   static private String compileTumorAttributes( final List<AttributeXn> attributes ) {
      return compileAttributes( TUMOR_ATTRIBUTES, attributes )
            + "|" + compileAttributes( BIOMARKERS, attributes );
   }


   static private String compileAttributes( final List<String> names, final List<AttributeXn> attributes ) {
      final StringBuilder sb = new StringBuilder();
      // How are attributes with empty names being created?
      final Map<String,AttributeXn> attributeXnMap
            = attributes.stream()
                        .filter( a -> !a.getName().isEmpty() )
                        .collect( Collectors.toMap( AttributeXn::getName, Function.identity() ) );
      for ( String name : names ) {
         AttributeXn attribute = attributeXnMap.get( name );
         if ( attribute != null ) {
            final List<AttributeValue> values = attribute.getValues();
            if ( values != null && !values.isEmpty() ) {
               values.sort( Comparator.comparing( AttributeValue::getdConfidence ).reversed() );
               for ( AttributeValue value : values ) {
                  final String uri = value.getClassUri();
                  if ( uri == null || uri.isEmpty() ) {
                     continue;
                  }
                  final String prefText = UriInfoCache.getInstance().getPrefText( uri );
                  if ( prefText.isEmpty() ) {
                     LogFileWriter.add( "PatientSummaryXnTableWriter empty prefText for " + uri );
                     continue;
                  }
                  String val = value.getValue();
                  if ( val.equals( prefText ) ) {
                     val = "";
                  }
                  sb
                        .append( prefText )
//                    .append( UriInfoCache.getInstance().getCui( uri ) )
                    .append( '=' ).append( val )
                    .append( ':' ).append( value.getConfidence() ).append( ';' );
               }
            }
         }
         sb.append( '|' );
      }
      return sb.toString();
   }


//   private String getZipName( final String type ) {
//      final String zipName = FILE_NAME + "_" + type + "_" + MILLIS;
//      if ( _zipMax <= 0 ) {
//         return zipName + ".zip";
//      }
//      _zipCount++;
//      if ( _zipCount > _zipMax ) {
//         _zipCount = 1;
//         _zipIndex++;
//      }
//      return zipName + "_" + String.format( "%09d", _zipIndex ) + ".zip";
//   }

   private String getFileName( final String type ) {
      final String filename = FILE_NAME + "_" + type + "_" + _timeText;
      if ( _patientMax <= 0 ) {
         return filename + ".bsv";
      }
      return filename + "_" + String.format( "%09d", _patientIndex ) + ".bsv";
   }

   private void incrementCount( final File cancerFile, final File tumorFile ) {
      _patientCount++;
      if ( _patientCount > _patientMax ) {
         if ( AeParamUtil.isTrue( _zipTable ) ) {
            zipFile( cancerFile );
            zipFile( tumorFile );
         }
         _patientCount = 1;
         _patientIndex++;
      }
   }

   private void zipFile( final File sourceFile ) {
      try ( FileOutputStream fos = new FileOutputStream(sourceFile.getPath() + ".zip" );
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            FileInputStream fis = new FileInputStream( sourceFile ) ) {
         final ZipEntry zipEntry = new ZipEntry( sourceFile.getName() );
         zipOut.putNextEntry( zipEntry );
         byte[] bytes = new byte[ 1024 ];
         int length;
         while( ( length = fis.read( bytes ) ) >= 0 ) {
            zipOut.write( bytes, 0, length );
         }
      } catch ( IOException ioE ) {
         LOGGER.warn( "Could not zip file " + sourceFile.getAbsolutePath() );
         return;
      }
      if ( !sourceFile.delete() ) {
         LOGGER.warn( "Could not delete " + sourceFile.getAbsolutePath() );
      }
   }

//   /**
//    * Write all patients to a single zip.
//    * @param rows -
//    * @param rootPath -
//    * @throws IOException -
//    */
//   private void writeFullZip( final String type, final String rows, final String rootPath )
//         throws IOException {
//      final String zipName = getZipName( type );
//      final String zipFilePath = rootPath + "/" + zipName + ".bsv.zip";
//      final String filename = getFileName( type );
//      zipAppend( zipFilePath, filename, rows.getBytes() );
//   }
//
//   static private void zipAppend( final String zipFilePath, final String filename, final byte[] contents )
//         throws IOException {
//      LOGGER.info( "Writing " + filename + " in " + zipFilePath + " ..." );
//      final Map<String,String> systemSettings = new HashMap<>();
//      systemSettings.put( "create", "true" );
//      final Path zipPath = Paths.get( zipFilePath );
//      final URI systemUri = URI.create( "jar:" + zipPath.toUri() );
//      try ( FileSystem fs = FileSystems.newFileSystem( systemUri, systemSettings ) ) {
//         final Path newFile = fs.getPath( filename );
//         Files.write( newFile, contents, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC );
//      }
//   }


}
