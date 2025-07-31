package org.healthnlp.deepphe.nlp.writer;

import org.apache.ctakes.core.cc.AbstractTableFileWriter;
import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Element;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.nlp.patient.PatientCasStore;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * @author SPF , chip-nlp
 * @since {3/28/2024}
 */
@PipeBitInfo (
      name = "UAB Patient Summary Table Writer",
      description = "Writes a table of Patient discovery information to file.",
      role = PipeBitInfo.Role.WRITER
)
public class UabPatientTableWriter extends AbstractTableFileWriter {

   // To test uri branches and relations, run the utilities BranchLister and RelationLister
   // As this class extends AbstractTableFileWriter it can use the parameters:
   // OutputDirectory    also specified by -o on the command line
   // SubDirectory       also specified by -s on the command line
   // TableType          with possible values: BSV CSV TAB HTML

   // The Patient Cas is built by the PatientCasCollector.  It must be in your pipeline after the document processors.
   // After the Patient Cas has been used by everything that needs it (such as this table writer),
   // it should be removed from storage, otherwise it takes up a lot of memory space.
   // Place PatientCasCleaner where appropriate in your pipeline to free up memory.

   // Patient Summarization is started in the PatientSummarizer, which calls the PatientCasSummarizer utility.
   // PatientCasSummarizer does a lot of work to create the 'best' summary of the patient as desired by the user,
   // based upon settings such as those in the PatientSummarizer.
   // PatientSummarizer should be in your pipeline after the PatientCasCollector
   // but before everything that needs the patient summary, like this table writer.

   // Strictly speaking, a patient cas can be built using other methods,
   // dphe just happens to use the PatientCasSummarizer, which builds a summary and calls
   //   PatientCasCreator.fillPatientCas( patientCas, goodUriConcepts, conceptRelations, allMentionAnnotationMap );
   // where goodUriConcepts, conceptRelations and allMentionAnnotationMap are essential summary information
   // identified according to what the pipeline discovered per document and the user's summary preferences.
   // PatientCasSummarizer is called by PatientSummarizer and is not added to the pipeline via piper file.


   static private final Logger LOGGER = Logger.getLogger( "UabPatientTableWriter" );

   /**
    * We can add formatted date/time stamps to files.
    */
   static private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern( "ddMMyyyykkmmssS" );
   private String _timeText;

   /**
    * To print a decimal precise to only 2 digits.
    */
   static private final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat( "#.00" );

   /**
    * The table header is constant, it is not based upon any values.
    */
   static private List<String> HEADER = Arrays.asList( " URI ", " CUI ", " TUI ", " Group ", " Type ", " Pref. Text ",
         " Negated ", " Uncertain ", " Historic ", " Mentions ", " Confidence " );

   /**
    * We need to keep a copy of the current patient id to create the file name.
    */
   private String _patientId;

   /**
    * Save the time from the beginning of the run.
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _timeText = TIME_FORMATTER.format( OffsetDateTime.now() );
   }

   /**
    * @param jCas       ignored
    * @param documentId ignored
    * @return the subdirectory set with the PARAM_SUBDIR parameter, for instance "PATIENT_TABLES".
    */
   @Override
   protected String getSubdirectory( final JCas jCas, final String documentId ) {
      return getSimpleSubDirectory();
   }


   /**
    * TableFileWriter normally writes a file per document.  Change slightly to handle Patients.
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final List<List<String>> dataRows,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      if ( dataRows.isEmpty() ) {
         // Patient is not ready or is empty.
         return;
      }
      // Use the Patient ID in the filename.
      final String newDocumentId = getFilename();
      // fileName is ignored in the super method, relying instead on the documentId.
      super.writeFile( dataRows, outputDir, newDocumentId, fileName );
   }

   /**
    * @return a filename for the table made from the patient ID and the startup time.
    */
   protected String getFilename() {
      return _patientId + "_" + _timeText;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return HEADER;
   }

   /**
    * After every document is processed (at this point in the pipeline) the document cas is sent here.
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final String patientId = SourceMetadataUtil.getPatientIdentifier( jCas );
       if (!PatientDocCounter.getInstance().isPatientFull(patientId)) {
           return Collections.emptyList();
       }
      _patientId = patientId;
      final JCas patientCas = PatientCasStore.getInstance().getOrCreate( patientId );
      final List<List<String>> rows = new ArrayList<>();
      final Collection<Element> elements = JCasUtil.select( patientCas, Element.class );
      for ( Element element : elements ) {
         rows.add( createRow( element ) );
      }
      rows.sort( ROW_COMPARATOR );
      return rows;
   }

   /**
    * @param element A Disease/Disorder, Procedure, Anatomic Site, etc.
    * @return a list of strings representing a row for the table, one string per column.
    */
   static private List<String> createRow( final Element element ) {
//      final int id = element.getId();
      final String confidence = DECIMAL_FORMATTER.format( element.getConfidence() );
      final String count = String.valueOf( element.getMentions().size() );
      final String negated = CONST.NE_POLARITY_NEGATION_PRESENT == element.getPolarity() ? "True" : "False";
      final String uncertain = CONST.NE_UNCERTAINTY_PRESENT == element.getUncertainty() ? "True" : "False";
      final String historic = CONST.NE_HISTORY_OF_PRESENT == element.getHistoryOf() ? "True" : "False";
      final OntologyConcept concept = element.getOntologyConcept();
      String cui = "";
      SemanticTui type = SemanticTui.UNKNOWN;
      String prefText = "";
      if ( concept instanceof UmlsConcept ) {
         cui = ((UmlsConcept)concept).getCui();
         type = SemanticTui.getTuiFromCode( ((UmlsConcept)concept).getTui() );
         prefText = ((UmlsConcept)concept).getPreferredText();
      }
      final String uri = concept.getCode();

      for ( int i = 0; i < element.getMentions().size(); i++ ) {
         final IdentifiedAnnotation annotation = element.getMentions( i );
         final int annotationBegin = annotation.getBegin();
         final int annotationEnd = annotation.getEnd();
         LOGGER.info( "Annotation span: " + annotationBegin + "," + annotationEnd );
         LOGGER.info( "Annotation text: " + annotation.getCoveredText() );
         try {
            final JCas annotationCas = annotation.getCAS().getJCas();
            LOGGER.info( "Annotation view: " + annotationCas.getViewName() );
            final String docText = annotationCas.getDocumentText();
            final int docLength = docText.length();
            LOGGER.info( "Annotation Document length: " + docLength );
            if ( annotationBegin > annotationEnd || annotationBegin < 0 || annotationEnd > docLength ) {
               LOGGER.error( "Annotation span outside document text." );
               continue;
            }
            final int snipBegin = Math.max( 0, annotationBegin - 50 );
            final int snipEnd = Math.min( docLength, annotationEnd + 50 );
            LOGGER.info( "Annotation snippet " + docText.substring( snipBegin, snipEnd ) );
         } catch ( CASException casE ) {
            LOGGER.error( "Could not find JCas for annotation " + annotation.getCoveredText() + " , element "
                  + prefText );
         }
      }


      return Arrays.asList( uri, cui, type.name(), type.getGroupName(), type.getSemanticType(),
            prefText, negated, uncertain, historic, count, confidence );
   }


   /**
    * Compares rows by semantic group, type, and uri.  Used to sort the table rows.
    */
   static private final Comparator<List<String>> ROW_COMPARATOR = ( row1, row2 ) -> {
      final int group = String.CASE_INSENSITIVE_ORDER.compare( row1.get( 3 ), row2.get( 3 ) );
      if ( group != 0 ) {
         return group;
      }
      final int type = String.CASE_INSENSITIVE_ORDER.compare( row1.get( 4 ), row2.get( 4 ) );
      if ( type != 0 ) {
         return type;
      }
      // Reverse confidence
      return String.CASE_INSENSITIVE_ORDER.compare( row2.get( 10 ), row1.get( 10 ) );
   };


}
