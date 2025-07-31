package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.DOCUMENT_ID_PREFIX;

/**
 * @author SPF , chip-nlp
 * @since {5/22/2024}
 */
@PipeBitInfo (
      name = "PidListWriter",
      description = "Writes a List of patient IDs to file.",
      role = PipeBitInfo.Role.WRITER,
      usables = { DOCUMENT_ID_PREFIX }
)
public class PidListWriter extends AbstractFileWriter<String> {
   // If you do not need to utilize the entire cas, or need more than the doc cas, consider AbstractFileWriter<T>.
   static private final Logger LOGGER = Logger.getLogger( "PidListWriter" );
   //    static private final DateTimeFormatter DOCTIME_FORMATTER = DateTimeFormatter.ofPattern( "ddMMyyyykkmmssS" );
//   static private final LocalDateTime TODAY_DATE = LocalDateTime.now();

   // TODO - put MaxRows (per table file) in AbstractTableFileWriter
   // TODO - put ZipFiles, ZipMaxFiles in AbstractFileWriter
   static public final String ZIP_LIST_PARAM = "ZipList";
   static public final String ZIP_LIST_DESC = "Write list file in a zip file.";
   @ConfigurationParameter (
         name = ZIP_LIST_PARAM,
         description = ZIP_LIST_DESC,
         mandatory = false,
         defaultValue = "no"
   )
   private String _zipList;

   static public final String ZIP_MAX_PARAM = "ZipMax";
   static public final String ZIP_MAX_DESC = "The maximum number of files per zip.";
   @ConfigurationParameter (
         name = ZIP_MAX_PARAM,
         description = ZIP_MAX_DESC,
         mandatory = false
   )
   private int _zipMax = 1000;

   static public final String PATIENT_MAX_PARAM = "PatientMax";
   static public final String PATIENT_MAX_DESC = "The maximum number of patients per list.";
   @ConfigurationParameter (
         name = PATIENT_MAX_PARAM,
         description = PATIENT_MAX_DESC,
         mandatory = false
   )
   private int _patientMax = 1000;


//    static public final String DATE_PARSE_FORMAT_PARAM = "DateParseFormat";
//    static public final String DATE_PARSE_FORMAT_DESC = "A format to parse dates.  e.g. dd-MM-yyyy_HH:mm:ss";
//    @ConfigurationParameter (
//          name = DATE_PARSE_FORMAT_PARAM,
//          description = DATE_PARSE_FORMAT_DESC,
//          defaultValue = "yyyyMMdd",
//          mandatory = false
//    )
//    private String _dateParseFormat;

   static public final String DATE_WRITE_FORMAT_PARAM = "DateWriteFormat";
   static public final String DATE_WRITE_FORMAT_DESC = "A format to write dates.  e.g. dd-MM-yyyy_HH:mm:ss";
   @ConfigurationParameter (
         name = DATE_WRITE_FORMAT_PARAM,
         description = DATE_WRITE_FORMAT_DESC,
         defaultValue = "ddMMyyyykkmmss",
         mandatory = false
   )
   private String _dateWriteFormat;

   static public final String CAS_DATE_FORMAT_PARAM = "CasDateFormat";
   static public final String CAS_DATE_FORMAT_DESC = "Set a value for parameter CasDateFormat.";
   @ConfigurationParameter (
         name = CAS_DATE_FORMAT_PARAM,
         description = CAS_DATE_FORMAT_DESC,
         defaultValue = "MMddyyyykkmmss",
         mandatory = false
   )
   private String _casDateFormat;


   static private final float MIN_CONFIDENCE = 70f;

   private DateTimeFormatter _dateWriteFormatter;
   private DateTimeFormatter _casDateFormatter;


   private int _patientIndex = 1;
   private int _patientCount = 1;
   static private final String FILE_NAME = "PatientIdList";
   // Prevent asynchronous overwrite by multiple processes
   private String _runStartTime;
   // TODO - if we definitely know that jdk version is 9+, use the pid instead of millis.
//   long pid = ProcessHandle.current().pid();


   private String _pid;

   protected void createData( final JCas jCas ) {
      _pid = SourceMetadataUtil.getPatientIdentifier( jCas );
   }

   protected String getData() {
      return _pid;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final String pid,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      if ( !PatientDocCounter.getInstance().isPatientFull( pid ) ) {
         return;
      }
      final File pidFile = new File( getRootDirectory(), getFileName() );
      try ( Writer writer = new BufferedWriter( new FileWriter( pidFile, true ) ) ) {
         writer.append( pid ).append( "\n" );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected void writeComplete( final String pid ) {
      if ( !PatientDocCounter.getInstance().isPatientFull( pid ) ) {
         return;
      }
      _patientCount++;
      if ( _patientCount > _patientMax ) {
         final File pidFile = new File( getRootDirectory(), getFileName() );
         zipFile( pidFile );
         _patientCount = 1;
         _patientIndex++;
      }
   }



   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      if ( _patientCount > 1 ) {
         final File protocolFile = new File( getRootDirectory(), getFileName() );
         zipFile( protocolFile );
      }
   }


   /**
    * If we are zipping the files returns the root directory.  Otherwise, runs the super method.
    * {@inheritDoc}
    */
   @Override
   protected String getOutputDirectory( final JCas jcas, final String rootPath, final String documentId ) {
      return rootPath;
   }


   private String getFileName() {
      final String filename = FILE_NAME + "_" + _runStartTime;
      if ( _patientMax <= 0 ) {
         return filename + ".bsv";
      }
      return filename + "_" + String.format( "%09d", _patientIndex ) + ".bsv";
   }


   // TODO move to ctakes system utils
   private void zipFile( final File pidFile ) {
      if ( !AeParamUtil.isTrue( _zipList ) ) {
         return;
      }
      LOGGER.info( "Zipping " + pidFile.getAbsolutePath() );
      try ( FileOutputStream fos = new FileOutputStream( pidFile.getPath() + ".zip" );
            ZipOutputStream zipOut = new ZipOutputStream( fos );
            FileInputStream fis = new FileInputStream( pidFile ) ) {
         final ZipEntry zipEntry = new ZipEntry( pidFile.getName() );
         zipOut.putNextEntry( zipEntry );
         byte[] bytes = new byte[ 1024 ];
         int length;
         while ( (length = fis.read( bytes )) >= 0 ) {
            zipOut.write( bytes, 0, length );
         }
      } catch ( IOException ioE ) {
         LOGGER.warn( "Could not zip file " + pidFile.getAbsolutePath() );
         return;
      }
      if ( !pidFile.delete() ) {
         LOGGER.warn( "Could not delete " + pidFile.getAbsolutePath() );
      }
   }


}