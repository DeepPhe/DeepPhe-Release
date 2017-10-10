package org.healthnlp.deepphe.uima.cr;

import org.apache.ctakes.cancer.cr.AbstractFileTreeReader;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.fhir.Report;
import org.healthnlp.deepphe.summarization.drools.kb.KbEncounter;
import org.healthnlp.deepphe.summarization.drools.kb.KbIdentified;
import org.healthnlp.deepphe.summarization.drools.kb.KbPatient;
import org.healthnlp.deepphe.summarization.drools.kb.KbSummary;
import org.healthnlp.deepphe.uima.ae.DocumentSummarizerAE;
import org.healthnlp.deepphe.uima.fhir.DocumentResourceFactory;
import org.healthnlp.deepphe.uima.fhir.PhenotypeResourceFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FhirReader extends AbstractFileTreeReader {
   static private final Logger LOGGER = Logger.getLogger( "FhirReader" );

   protected File getRootDir() {
      final File superRoot = super.getRootDir();
      if ( superRoot == null ) {
         LOGGER.error( "Not yet initialized" );
         return null;
      }
      final File fhirSubDir = FileLocator.getFileQuiet( new File( superRoot, DocumentSummarizerAE.FHIR_TYPE ).getPath() );
      if ( fhirSubDir != null && fhirSubDir.isDirectory() ) {
         return fhirSubDir;
      }
      return superRoot;
   }

   /**
    * @param parentDir       -
    * @param validExtensions collection of valid extensions or empty collection if all extensions are valid
    * @return List of files descending from the parent directory
    */
   protected List<File> getDescendentFiles( final File parentDir, final Collection<String> validExtensions ) {
      // check if there is a FHIR subdirectory, otherwiswe assume that that is it
      final File[] children = getRootDir().listFiles();
      if ( children == null || children.length == 0 ) {
         return Collections.emptyList();
      }
      final List<File> descendentFiles = new ArrayList<>();
      for ( File child : children ) {
         if ( child.isDirectory() ) {
            descendentFiles.add( child );
         }
      }
      return descendentFiles;
   }

   /**
    * Using OBX Text (TX) and Formatted Text (FT).  Keep an eye out for valid String Data (ST).
    * {@inheritDoc}
    */
   @Override
   protected void readFile( final JCas jCas, final File file ) throws IOException {
      try {
         // now we have a subject directory, it should contain report directories.
         //ArrayList<Report> reports = new ArrayList<Report>();
         //Patient patient = new Patient();
         //p.setPath(f.getAbsolutePath());
         jCas.reset();

         StringBuffer reportText = new StringBuffer();

         List<Report> reports = new ArrayList<Report>();
         for ( File f : file.listFiles() ) {
            if ( f.isDirectory() ) {
               Report r = DocumentResourceFactory.loadReport( f );
               if ( r != null ) {
                  reports.add( r );
               }
            }
         }
         // sort by date
         Collections.sort( reports );

         // add reports to TS
         int offset = 0;
         for ( Report r : reports ) {
            // add report to an uber report
            String text = r.getReportText() + "\n";
            reportText.append( text );
            r.setOffset( offset );
            offset += text.length();

            // persist into CAS
            //System.out.println(r.getSummaryText());
            PhenotypeResourceFactory.saveReport( r, jCas );
         }

         // we don't have text (for now)
         //jcas.setDocumentText(Base64.getEncoder().encodeToString(SerializationUtils.serialize(patient)));
         jCas.setDocumentText( reportText.toString() );
      } catch ( Exception e ) {

      }
   }

   private void reIdentifyDAG( KbPatient p ) {
      p.setId( KbIdentified.idGenerator++ );
      p.setSequence( p.getId() );
      for ( KbEncounter e : p.getEncounters() ) {
         e.setId( KbIdentified.idGenerator++ );
         e.setSequence( e.getId() );
         e.setPatientId( p.getId() );
      }
      for ( KbSummary s : p.getSummaries() ) {
         s.setId( KbIdentified.idGenerator++ );
         s.setSummarizableId( p.getId() );
      }
      for ( KbEncounter e : p.getEncounters() ) {
         for ( KbSummary s : e.getSummaries() ) {
            s.setId( KbIdentified.idGenerator++ );
            s.setSummarizableId( e.getId() );
         }
      }
   }

   /**
    * {@inheritDoc}
    *
    * @return the file name without the extension removed
    */
   @Override
   protected String createDocumentID( final File file, final Collection<String> validExtensions ) {
      final String parentPath = getRootPath();
      if ( file.getPath().startsWith( parentPath ) ) {
         return file.getPath().substring( parentPath.length() + 1 );
      }
      return file.getPath();
   }


   public static void main( String[] args ) throws Exception {
      File file = new File( "/home/tseytlin/Work/DeepPhe/data/sample/output/FHIR/Jane Doe" );
      for ( File f : file.listFiles() ) {
         if ( f.isDirectory() ) {
            System.out.println( f.getName() );
            Report r = DocumentResourceFactory.loadReport( f );
            if ( r != null ) {
               System.out.println( r.getSummaryText() );
               r.save( new File( "/home/tseytlin/Output/FHIR/" ) );
            }
         }
      }

   }
}
