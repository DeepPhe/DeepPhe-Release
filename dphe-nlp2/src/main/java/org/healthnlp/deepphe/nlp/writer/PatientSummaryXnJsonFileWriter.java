package org.healthnlp.deepphe.nlp.writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.ctakes.core.patient.PatientDocCounter;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.neo4j.node.xn.*;
import org.healthnlp.deepphe.nlp.patient.PatientSummaryXnStore;

import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @author SPF , chip-nlp
 * @since {4/26/2021}
 */
final public class PatientSummaryXnJsonFileWriter extends AbstractFileWriter<PatientSummaryXn> {
   static private final Logger LOGGER = org.apache.log4j.Logger.getLogger( "PatientSummaryXnJsonFileWriter" );

   static public final String PRETTY_PRINT_PARAM = "PrettyPrint";
   static public final String PRETTY_PRINT_DESC = "Write the json with indentations for easier reading.";
   @ConfigurationParameter(
         name = PRETTY_PRINT_PARAM,
         description = PRETTY_PRINT_DESC,
         defaultValue = "no",
         mandatory = false
   )
   private String _prettyPrint;


   static public final String WRITE_NOTE_PARAM = "WriteNote";
   static public final String WRITE_NOTE_DESC = "Write the note text in the json file.";
   @ConfigurationParameter(
         name = WRITE_NOTE_PARAM,
         description = WRITE_NOTE_DESC,
         mandatory = false,
         defaultValue = "yes"
   )
   private String _writeNote;

   static public final String WRITE_SECTIONS_PARAM = "WriteSections";
   static public final String WRITE_SECTIONS_DESC = "Set a value for parameter WriteSections.";
   @ConfigurationParameter(
           name = WRITE_SECTIONS_PARAM,
           description = WRITE_SECTIONS_DESC,
           mandatory = false,
           defaultValue = "yes"
   )
   private String _writeSections;

   static public final String WRITE_CONCEPTS_PARAM = "WriteConcepts";
   static public final String WRITE_CONCEPTS_DESC = "Set a value for parameter WriteConcepts.";
   @ConfigurationParameter(
           name = WRITE_CONCEPTS_PARAM,
           description = WRITE_CONCEPTS_DESC,
           mandatory = false,
           defaultValue = "yes"
   )
   private String _writeConcepts;

   static public final String WRITE_EVIDENCE_PARAM = "WriteEvidence";
   static public final String WRITE_EVIDENCE_DESC = "Set a value for parameter WriteEvidence.";
   @ConfigurationParameter(
           name = WRITE_EVIDENCE_PARAM,
           description = WRITE_EVIDENCE_DESC,
           mandatory = false,
           defaultValue = "yes"
   )
   private String _writeEvidence;

   static public final String WRITE_CONCEPT_RELATIONS_PARAM = "WriteConceptRelations";
   static public final String WRITE_CONCEPT_RELATIONS_DESC = "Set a value for parameter WriteConceptRelations.";
   @ConfigurationParameter(
           name = WRITE_CONCEPT_RELATIONS_PARAM,
           description = WRITE_CONCEPT_RELATIONS_DESC,
           mandatory = false,
           defaultValue = "yes"
   )
   private String _writeConceptRelations;

   static public final String WRITE_MENTIONS_PARAM = "WriteMentions";
   static public final String WRITE_MENTIONS_DESC = "Set a value for parameter WriteMentions.";
   @ConfigurationParameter(
           name = WRITE_MENTIONS_PARAM,
           description = WRITE_MENTIONS_DESC,
           mandatory = false,
           defaultValue = "yes"
   )
   private String _writeMentions;

   static public final String WRITE_MENTION_RELATIONS_PARAM = "WriteMentionRelations";
   static public final String WRITE_MENTION_RELATIONS_DESC = "Set a value for parameter WriteMentionRelations.";
   @ConfigurationParameter(
           name = WRITE_MENTION_RELATIONS_PARAM,
           description = WRITE_MENTION_RELATIONS_DESC,
           mandatory = false,
           defaultValue = "yes"
   )
   private String _writeMentionRelations;

   static public final String SUMMARY_ONLY_PARAM = "SummaryOnly";
   static public final String SUMMARY_ONLY_DESC = "Only write the cancer and tumor summaries without contributing information.";
   @ConfigurationParameter(
         name = SUMMARY_ONLY_PARAM,
         description = SUMMARY_ONLY_DESC,
         mandatory = false,
         defaultValue = "no"
   )
   private String _summaryOnly;


   static public final String ZIP_JSON_PARAM = "ZipJson";
   static public final String ZIP_JSON_DESC = "Write json file in a zip file.";
   @ConfigurationParameter(
         name = ZIP_JSON_PARAM,
         description = ZIP_JSON_DESC,
         mandatory = false,
         defaultValue = "no"
   )
   private String _zipJson;

   static public final String ZIP_MAX_PARAM = "ZipMax";
   static public final String ZIP_MAX_DESC = "The maximum number of patients per zip.";
   @ConfigurationParameter(
         name = ZIP_MAX_PARAM,
         description = ZIP_MAX_DESC,
         mandatory = false
   )
   private int _zipMax = 0;

   static public final String SPLIT_JSON_PARAM = "SplitJson";
   static public final String SPLIT_JSON_DESC = "Split the output into multiple json files.";
   @ConfigurationParameter(
         name = SPLIT_JSON_PARAM,
         description = SPLIT_JSON_DESC,
         mandatory = false,
         defaultValue = "no"
   )
   private String _splitJson;


   private PatientSummaryXn _patientSummary;

   private int _zipIndex = 1;
   private int _zipCount = 0;

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
      final PatientSummaryXn patientSummary = new PatientSummaryXn();
      patientSummary.setId( _patientSummary.getId() );
      patientSummary.setName( _patientSummary.getName() );
      patientSummary.setDocuments( cloneDocuments( _patientSummary.getDocuments() ) );
      patientSummary.setConcepts( cloneConcepts( _patientSummary.getConcepts() ) );
      patientSummary.setConceptRelations( cloneConceptRelations( _patientSummary.getConceptRelations() ) );
      patientSummary.setCancers( cloneCancers( _patientSummary.getCancers() ) );
      return patientSummary;
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
      if ( AeParamUtil.isTrue( _zipJson ) ) {
         return rootPath;
      }
      return super.getOutputDirectory( jcas, rootPath, documentId );
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
       if (_zipMax > 0) {
           // Increase the zip count here, not at the file level!
           _zipCount++;
           if (_zipCount > _zipMax) {
               _zipCount = 1;
               _zipIndex++;
           }
       }
      final GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
      if ( AeParamUtil.isTrue( _prettyPrint ) ) {
         gsonBuilder.setPrettyPrinting();
      }
      final Gson gson = gsonBuilder.create();
      if ( AeParamUtil.isTrue( _splitJson ) ) {
         writeSplitJson( gson, patientSummary, outputDir, patientId );
         return;
      }
      final String summaryJson = gson.toJson( patientSummary );
      if ( AeParamUtil.isTrue( _zipJson ) ) {
         writeFullZip( summaryJson, outputDir, super.getSimpleSubDirectory(), patientId, patientId );
         return;
      }
      final File file = new File( outputDir, patientId + ".json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( summaryJson );
      }
   }

   private void writeSplitJson( final Gson gson,
                                final PatientSummaryXn patientSummary,
                                final String outputDir, final String patientId ) throws IOException {
      writePatient( gson, patientSummary, outputDir, patientId );
      for ( DocumentXn document : patientSummary.getDocuments() ) {
         writeDocument( gson, document, outputDir, patientId );
      }
      writeCancers( gson, patientSummary, outputDir, patientId );
      writeConcepts( gson, patientSummary, outputDir, patientId );
   }

   private void writePatient( final Gson gson,
                              final PatientSummaryXn patientSummary,
                              final String outputDir, final String patientId ) throws IOException {
      final BasicPatient patient = new BasicPatient( patientId,
            patientSummary.getName(), patientSummary.getGender(),
            patientSummary.getBirth(), patientSummary.getDeath() );
      final String json = gson.toJson( patient );
      if ( AeParamUtil.isTrue( _zipJson ) ) {
         writeFullZip( json, outputDir, super.getSimpleSubDirectory(), patientId, patientId );
         return;
      }
      final File file = new File( outputDir, patientId + ".json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( json );
      }
   }

   private void writeDocument( final Gson gson,
                               final DocumentXn document,
                               final String outputDir, final String patientId ) throws IOException {
      final String json = gson.toJson( document );
      final String documentId = document.getId();
      if ( AeParamUtil.isTrue( _zipJson ) ) {
         writeFullZip( json, outputDir, super.getSimpleSubDirectory(), patientId, documentId + "_Doc" );
         return;
      }
      final File file = new File( outputDir, documentId + "_Doc.json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( json );
      }
   }

   private void writeCancers( final Gson gson,
                              final PatientSummaryXn patientSummary,
                              final String outputDir, final String patientId ) throws IOException {
      final List<CancerSummaryXn> cancers = patientSummary.getCancers();
      final String json = gson.toJson( cancers );
      if ( AeParamUtil.isTrue( _zipJson ) ) {
         writeFullZip( json, outputDir, super.getSimpleSubDirectory(), patientId, patientId + "_Cancers" );
         return;
      }
      final File file = new File( outputDir, patientId + "_Cancers.json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( json );
      }
   }

   private void writeConcepts( final Gson gson,
                               final PatientSummaryXn patientSummary,
                               final String outputDir, final String patientId ) throws IOException {
      final Concepts concepts = new Concepts( patientSummary.getConcepts(), patientSummary.getConceptRelations() );
      final String json = gson.toJson( concepts );
      if ( AeParamUtil.isTrue( _zipJson ) ) {
         writeFullZip( json, outputDir, super.getSimpleSubDirectory(), patientId, patientId + "_Concepts" );
         return;
      }
      final File file = new File( outputDir, patientId + "_Concepts.json" );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( json );
      }
   }

   /**
    * Writes a zip file for each patient.
    * @param summaryJson -
    * @param outputDir -
    * @param patientId -
    * @throws IOException -
    */
   static private void writeZip( final String summaryJson, final String outputDir, final String patientId )
         throws IOException {
      final File zipFile = new File( outputDir, patientId + ".json.zip" );
      final ZipOutputStream out = new ZipOutputStream( Files.newOutputStream( zipFile.toPath() ) );
      final ZipEntry jsonEntry = new ZipEntry( patientId + ".json" );
      out.putNextEntry( jsonEntry );
      final byte[] data = summaryJson.getBytes();
      out.write( data, 0, data.length );
      out.closeEntry();
      out.close();
   }

   private String getZipName( final String subDirectory, final String patientId ) {
      if ( _zipMax == 1 ) {
//         LOGGER.info( "ZipMax is 1, returning PatientId " + patientId );
         return patientId;
      }
      final String zipName = subDirectory.isEmpty() ? "AllZip" : subDirectory;
      if ( _zipMax <= 0 ) {
//         LOGGER.info( "ZipMax is <= 0, returning zipName " + zipName );
         return zipName;
      }
//      LOGGER.info( "ZipMax is > 1, returning zipName index " + zipName + "_" + String.format( "%09d", _zipIndex ) );
      return zipName + "_" + String.format( "%09d", _zipIndex );
   }

   /**
    * Write all patients to a single zip.
    * @param summaryJson -
    * @param rootPath -
    * @param subDirectory -
    * @param patientId -
    * @throws IOException -
    */
   private void writeFullZip( final String summaryJson, final String rootPath,
                              final String subDirectory, final String patientId,
                              final String filePrefix )
         throws IOException {
      final String zipName = getZipName( subDirectory, patientId );
      final String zipFilePath = rootPath + "/" + zipName + ".json.zip";
      final String filename = filePrefix + ".json";
      zipAppend( zipFilePath, filename, summaryJson.getBytes() );
   }

   static private void zipAppend( final String zipFilePath, final String filename, final byte[] contents )
         throws IOException {
      LOGGER.info( "Writing " + filename + " in " + zipFilePath + " ..." );
      final Map<String,String> systemSettings = new HashMap<>();
      systemSettings.put( "create", "true" );
      final Path zipPath = Paths.get( zipFilePath );
      final URI systemUri = URI.create( "jar:" + zipPath.toUri() );
      try ( FileSystem fs = FileSystems.newFileSystem( systemUri, systemSettings ) ) {
         final Path newFile = fs.getPath( filename );
         Files.write( newFile, contents, StandardOpenOption.CREATE );
      }
   }

   private List<DocumentXn> cloneDocuments(final List<DocumentXn> documents ) {
      final List<DocumentXn> docs = new ArrayList<>( documents.size() );
      for ( DocumentXn document : documents ) {
         final DocumentXn doc = new DocumentXn();
         doc.setId( document.getId() );
         doc.setName( document.getName() );
         doc.setDate( document.getDate() );
         doc.setType( document.getType() );
         doc.setEpisode( document.getEpisode() );
         if ( AeParamUtil.isTrue( _writeNote ) && !AeParamUtil.isTrue( _summaryOnly ) ) {
            doc.setText( document.getText() );
         }
         if ( AeParamUtil.isTrue( _writeSections ) && !AeParamUtil.isTrue( _summaryOnly ) ) {
            doc.setSections( document.getSections() );
         }
         if ( AeParamUtil.isTrue( _writeMentions ) && !AeParamUtil.isTrue( _summaryOnly ) ) {
            doc.setMentions( document.getMentions() );
         }
         if ( AeParamUtil.isTrue( _writeMentionRelations ) && !AeParamUtil.isTrue( _summaryOnly ) ) {
            doc.setMentionRelations( document.getMentionRelations() );
         }
         docs.add( doc );
      }
      return docs;
   }

   private List<Concept> cloneConcepts( final List<Concept> concepts ) {
      if ( AeParamUtil.isTrue( _writeConcepts )
              && !AeParamUtil.isTrue( _summaryOnly ) ) {
         final List<Concept> clones = new ArrayList<>( concepts.size() );
         for ( Concept concept : concepts ) {
            final Concept clone = new Concept();
            clone.setDpheGroup( concept.getDpheGroup() );
            clone.setPreferredText( concept.getPreferredText() );
            cloneInfoNode( concept, clone );
            if ( AeParamUtil.isTrue( _writeEvidence )
                    && AeParamUtil.isTrue( _writeMentions )
                    && !AeParamUtil.isTrue( _summaryOnly ) ) {
               clone.setMentionIds( concept.getMentionIds() );
            }
            if ( !AeParamUtil.isTrue( _summaryOnly ) ) {
               clone.setCodifications( concept.getCodifications() );
            }
            clones.add( clone );
         }
         return clones;
      } else {
         return Collections.emptyList();
      }
   }

   private List<ConceptRelation> cloneConceptRelations( final List<ConceptRelation> conceptRelations ) {
      if ( AeParamUtil.isTrue( _writeConceptRelations )
              && AeParamUtil.isTrue( _writeConcepts )
              && !AeParamUtil.isTrue( _summaryOnly ) ) {
         return conceptRelations;
      } else {
         return Collections.emptyList();
      }
   }

   private List<CancerSummaryXn> cloneCancers( final List<CancerSummaryXn> cancerSummaries ) {
      final List<CancerSummaryXn> cancers = new ArrayList<>( cancerSummaries.size() );
      for ( CancerSummaryXn cancerSummary : cancerSummaries ) {
         final CancerSummaryXn cancer = new CancerSummaryXn();
         cloneInfoNode( cancerSummary, cancer );
         if ( AeParamUtil.isTrue( _writeEvidence )
                 && AeParamUtil.isTrue( _writeConcepts )
                 && !AeParamUtil.isTrue( _summaryOnly ) ) {
            cancer.setConceptIds( cancerSummary.getConceptIds() );
         }
         cancer.setAttributes( cloneAttributes( cancerSummary.getAttributes() ) );
         cancer.setTumors( cloneTumors( cancerSummary.getTumors() ) );
         cancers.add( cancer );
      }
      return cancers;
   }

   private List<TumorSummaryXn> cloneTumors( final List<TumorSummaryXn> tumorSummaries ) {
      final List<TumorSummaryXn> tumors = new ArrayList<>( tumorSummaries.size() );
      for ( TumorSummaryXn tumorSummary : tumorSummaries ) {
         final TumorSummaryXn tumor = new TumorSummaryXn();
         cloneInfoNode( tumorSummary, tumor );
         if ( AeParamUtil.isTrue( _writeEvidence )
                 && AeParamUtil.isTrue( _writeConcepts )
                 && !AeParamUtil.isTrue( _summaryOnly ) ) {
            tumor.setConceptIds( tumorSummary.getConceptIds() );
         }
         tumor.setAttributes( cloneAttributes( tumorSummary.getAttributes() ) );
         tumors.add( tumor );
      }
      return tumors;

   }

   private List<AttributeXn> cloneAttributes( final List<AttributeXn> attributeXns ) {
      final List<AttributeXn> attributes = new ArrayList<>( attributeXns.size() );
      for ( AttributeXn attributeXn : attributeXns ) {
         final AttributeXn attribute = new AttributeXn();
         attribute.setId( attributeXn.getId() );
         attribute.setName( attributeXn.getName() );
         attribute.setValues( cloneAttributeValues( attributeXn.getValues() ) );
         attributes.add( attribute );
      }
      return attributes;
   }

   private List<AttributeValue> cloneAttributeValues( final List<AttributeValue> attributeValues ) {
      final List<AttributeValue> values = new ArrayList<>( attributeValues.size() );
      for ( AttributeValue attributeValue : attributeValues ) {
         final AttributeValue value = new AttributeValue();
         cloneInfoNode( attributeValue, value );
         value.setValue( attributeValue.getValue() );
         if ( AeParamUtil.isTrue( _writeEvidence )
                 && AeParamUtil.isTrue( _writeConcepts )
                 && !AeParamUtil.isTrue( _summaryOnly ) ) {
            value.setConceptIds( attributeValue.getConceptIds() );
         }
         values.add( value );
      }
      return values;
   }


   private void cloneInfoNode( final InfoNode source, final InfoNode target ) {
      target.setId( source.getId() );
//      target.setDpheGroup( source.getDpheGroup() );
//      target.setPreferredText( source.getPreferredText() );
      target.setClassUri( source.getClassUri() );
      target.setdConfidence( source.getdConfidence() );
      target.setNegated( source.isNegated() );
      target.setUncertain( source.isUncertain() );
      target.setHistoric( source.isHistoric() );
   }


   static private final class Concepts {
      private List<Concept> concepts;
      private List<ConceptRelation> conceptRelations;
      private Concepts( final List<Concept> concepts, final List<ConceptRelation> conceptRelations ) {
         this.concepts = concepts;
         this.conceptRelations = conceptRelations;
      }
   }

}
