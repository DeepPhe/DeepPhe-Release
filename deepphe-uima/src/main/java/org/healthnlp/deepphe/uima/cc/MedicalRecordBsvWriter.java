package org.healthnlp.deepphe.uima.cc;

import org.apache.ctakes.core.note.NoteSpecs;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.fact.*;
import org.healthnlp.deepphe.summary.*;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;


/**
 * Replaces the V1 evaluation writer
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/31/2018
 */
public class MedicalRecordBsvWriter extends MedicalRecordWriter {

   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

   static private final String BREAST_CONFIG_DIR = "data/breast";
   static private final String MELANOMA_CONFIG_DIR = "data/melanoma";
   static private final String OVARIAN_CONFIG_DIR = "data/ovarian";

   @ConfigurationParameter(
         name = "ConfigDirectory",
         mandatory = false
   )
   private String _configDirPath; // used for finding tsv files

   static private final String DOMAIN = "Domain";
   @ConfigurationParameter(
         name = DOMAIN,
         description = "Determines the domain-specific rules to be used.",
         mandatory = false
   )
   private String _domain;

   static private final String I = "|";    // separates fields
   static private final String FS = ";";   // separates values within a field
   static private final String NL = "\n";

   static private final String EVAL_CANCER_FILE = "DeepPhe_Evaluation_Cancer.bsv";
   static private final String EVAL_TUMOR_FILE = "DeepPhe_Evaluation_Tumor.bsv";
   static private final String EVAL_PATIENT_FILE = "DeepPhe_Evaluation_Patient.bsv";
   //static private final String EVAL_EPISODE_FILE = "DeepPhe_Evaluation_Episode.bsv";

   public static final String ENTRY_LABEL = "Label";
   public static final String ENTRY_CLASS = "Class";
   public static final String ENTRY_PROPERTY = "Property";
   public static final String ENTRY_PROVENANCE = "wasDerivedFrom";
   public static final String LABEL_TEMPORALITY = "Temporality";

   static private final Object WRITE_LOCK = new Object();
   private List<Map<String, String>> cancerMapping, tumorMapping, patientMapping, episodeMapping;

   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      // Load the config files once, not per patient.
      // Initialize the header of the output file(s) here so that we get one file (of each type) with
      // one header (per file) for the entire cohort/corpus that is processed.
      // We do not want separate file(s) per patient.
      try {
         if (cancerMapping == null) {
            if ( _configDirPath == null || _configDirPath.isEmpty() ) {
               if ( _domain != null && !_domain.isEmpty() ) {
                  switch ( _domain ) {
                     case "Breast":
                        _configDirPath = BREAST_CONFIG_DIR;
                        break;
                     case "Melanoma":
                        _configDirPath = MELANOMA_CONFIG_DIR;
                        break;
                     case "Ovarian":
                        _configDirPath = OVARIAN_CONFIG_DIR;
                        break;
                     default:
                        _configDirPath = BREAST_CONFIG_DIR;
                  }
               } else {
                  _configDirPath = BREAST_CONFIG_DIR;
               }
            }
               final File configDir = FileLocator.getFile( _configDirPath );
               LOGGER.debug("Using configuration directory: " + configDir);
               final String prefix = configDir + File.separator + "eval" + File.separator + "DeepPhe_Evaluation_";
               cancerMapping = loadEvaluationMap(new File(prefix + "Cancer.tsv"));
               tumorMapping = loadEvaluationMap(new File(prefix + "Tumor.tsv"));
               patientMapping = loadEvaluationMap(new File(prefix + "Patient.tsv"));
               //episodeMapping = loadEvaluationMap(new File(prefix + "Episode.tsv"));
         }
         final String outputDir = getEmrOutputDirectory();
         writeHeader(cancerMapping, new File(outputDir, EVAL_CANCER_FILE));
         writeHeader(tumorMapping, new File(outputDir, EVAL_TUMOR_FILE));
         //writeHeader(patientMapping, new File(outputDir, EVAL_PATIENT_FILE));
         //writeHeader(episodeMapping, new File(outputDir, EVAL_EPISODE_FILE));
      } catch (IOException e) {
         throw new ResourceInitializationException(e);
      }
   }


   /**
    *
    * @param record The MedicalRecord for a single patient
    * @param ignore Was outputDir. Now ignored.
    * @throws IOException -
    */
   protected void writeMedicalRecord( final MedicalRecord record, final String ignore ) throws IOException {
      final String outputDir = getEmrOutputDirectory();

      final CancerSummary sourceCancerSummary = record.getCancerSummary();
      final String patient = record.getPatientName();
      File cancerFile = new File(outputDir, EVAL_CANCER_FILE);

      if ( !sourceCancerSummary.isEmpty() ) {

         writeCancerSummary(cancerMapping, cancerFile, sourceCancerSummary, patient);

      } else {

         LOGGER.info( "No cancer for Patient: " +  patient );
         // We still want some sort of empty entry for patients without cancer to at least show that they exist but nothing was found
         writeEmptyCancer(cancerMapping, cancerFile, sourceCancerSummary, patient);

      }

      File file = new File( outputDir, EVAL_TUMOR_FILE );
      for (TumorSummary tumorSummary : sourceCancerSummary.getTumors()) {
         writeTumorSummary(tumorMapping, file, tumorSummary, patient);
      }


   }


   private String removeEolChars(String s) {
      if (s.contains("\r")) {
         s = s.replace('\r',' ');
         new RuntimeException("Found CR character in '" + s + "'").printStackTrace();
      }
      return s.replace('\n',' ');
   }

   private void appendEndOfLine(File file) throws IOException {
      saveText(NL, file, true);
   }

   private void writeCancerSummary( final List<Map<String, String>> mapping,
                                    final File dataFile,
                                    final CancerSummary cancer,
                                    final String patient) throws IOException {
      final StringBuilder sb = new StringBuilder();
      Map<String, FactList> factsIndexedByCategory = cancer.getSummaryFacts();
      FactList valueFacts = null;
      for ( Map<String, String> map : mapping ) {
         String dataLabel = map.get( ENTRY_LABEL );
         String entryClass = map.get( ENTRY_CLASS );
         String entryProperty = map.get( ENTRY_PROPERTY );

         // special case for some data labels
         String value = "";
         if (dataLabel.contains("atient")) { // skip the P of Patient so we don't rely on if upper or lower case
            value = patient;
         } else if (dataLabel.contains("cancer link") || dataLabel.contains("cancer I")) { // cancer ID, skip the D in ID to ignore its case
            value = cancer.getFullCancerIdentifier();
         } else if ( dataLabel.startsWith( "-" ) && ENTRY_PROVENANCE.equals( entryProperty ) ) {
            value = getProvenance( valueFacts );
         } else if ( LABEL_TEMPORALITY.equals( dataLabel ) ) {
            value = cancer.getTemporality();
         } else if ( "Laterality".equals( entryProperty ) ) {
            value = cancer.getBodySide();
         } else {
            if (FHIRConstants.BODY_SITE.equals(entryClass)) {
               // Laterality is an example of a modifier
               valueFacts = getModifiersWithValue(factsIndexedByCategory, "has"+entryClass, entryProperty, dataLabel);
            } else {
               valueFacts = getFactsWithValue(factsIndexedByCategory, entryClass, entryProperty);
            }
            value = getFactValue( valueFacts, entryClass, entryProperty );
         }
         sb.append( value ).append( I );

      }
      sb.append( NL );

      LOGGER.info( "Writing Cancer Evaluation data to " + dataFile.getAbsolutePath() + " ..." );
      saveText( sb.toString(), dataFile, true );
      LOGGER.info( "Finished." );
   }

   // example: rather than Estrogen_Receptor_Negative, just want Negative
   private String shortenStatusInterpretation(String interpretation) {
      int len = interpretation.length();
      String lcInterp = interpretation.toLowerCase();

      String ending;

      ending = "negative";
      if (lcInterp.endsWith("receptor_"+ending) || lcInterp.endsWith("her2_neu_"+ending)              ) {
         return interpretation.substring(len - (ending.length()));
      }

      ending = "positive";
      if (lcInterp.endsWith("receptor_"+ending) || lcInterp.endsWith("her2_neu_"+ending)              ) {
         return interpretation.substring(len - (ending.length()));
      }


      ending = "unknown";
      if (lcInterp.endsWith("receptor_"+ending) || lcInterp.endsWith("her2_neu_"+ending)              ) {
         return interpretation.substring(len - (ending.length()));
      }

      return interpretation;
   }

   private void writeTumorSummary(List<Map<String, String>> mapping, File dataFile, TumorSummary tumor, String patient) throws IOException {

         StringBuffer buffer = new StringBuffer();
         FactList valueFacts = null;
      Map<String, FactList> factsIndexedByCategory = tumor.getSummaryFacts();

         for ( Map<String, String> map : mapping ) {
            // example values for the next three, for a line that contained "body location\tBreast_Cancer\thasBodySite" ...
            String dataLabel = map.get( ENTRY_LABEL );        // ... would be "body location"
            String entryClass = map.get( ENTRY_CLASS );       // ... would be "Breast_Cancer"
            String entryProperty = map.get( ENTRY_PROPERTY ); // ... would be hasBodySite
            // special case for some data labels
            String value = "";
            if (dataLabel.contains("atient")) {
               value = patient;
            } else if (dataLabel.contains("cancer link") || dataLabel.contains("cancer I")) { // cancer ID, skip the D to ignore its case
               value = tumor.getFullTumorIdentifier();
            } else if (dataLabel.startsWith( "-" ) && ENTRY_PROVENANCE.equals( entryProperty )) {
               value = getProvenance( valueFacts );
            } else if ( isEpisode( dataLabel ) ) { // "Diagnostic", "Pre-Diagnostic", "Treatment", "Follow-up"
               String episodeType = getEpisodeType( dataLabel );
               EpisodeSummary episode = tumor.getEpisode( episodeType );
               if ( episode != null ) {
                  if ( dataLabel.endsWith( "Start Date" ) ) {
                     value = "" + episode.getFirstDate();
                  } else if ( dataLabel.endsWith( "End Date" ) ) {
                     value = "" + episode.getLastDate();
                  } else if ( dataLabel.endsWith( "Documents" ) ) {
                     value = getDocuments( episode.getAllNoteSpecs() );
                  }
               }
            } else {
               if (FHIRConstants.BODY_SITE.equals(entryClass)) {
                  // Quadrant, Clockface, Laterality are examples of modifiers
                  valueFacts = getModifiersWithValue(factsIndexedByCategory, "has"+entryClass, entryProperty, dataLabel);
               } else {
                  valueFacts = getFactsWithValue(factsIndexedByCategory, entryClass, entryProperty);
               }
               value = getFactValue( valueFacts, entryClass, entryProperty );
            }
            buffer.append( value ).append( I );
         }
         buffer.append( NL );

      LOGGER.info( "Writing Tumor Evaluation data to " + dataFile.getAbsolutePath() + " ..." );
      saveText( buffer.toString(), dataFile, true );
      LOGGER.info( "Finished." );
   }


   private void writeEmptyCancer( final List<Map<String, String>> mapping,
                                final File dataFile,
                                final CancerSummary cancer,
                                final String patient) throws IOException {
      final StringBuffer sb = new StringBuffer();
      for (Map<String, String> map : mapping) {
         String dataLabel = map.get(ENTRY_LABEL);
         String entryProperty = map.get(ENTRY_PROPERTY);
         // special case for some data labels
         String value = "";
         if (dataLabel.contains("atient")) {
            value = patient;
         } else if ((dataLabel.startsWith("*") && dataLabel.contains("cancer")) || // for V2 the cancer link and cancer ID are not marked with *, they are not to be counted in scoring, specific attributes are
                 dataLabel.equalsIgnoreCase("cancer id") || dataLabel.equalsIgnoreCase("cancer link") ||
                 dataLabel.substring(1).equalsIgnoreCase("cancer id") || dataLabel.substring(1).equalsIgnoreCase("cancer link")) {
            value = "cancer_" + patient + "__";
         } else if (dataLabel.startsWith("-") && ENTRY_PROVENANCE.equals(entryProperty)) {
            value = "";
         } else if (LABEL_TEMPORALITY.equals(dataLabel)) {
            value = "";
         } else {
            value = "";
         }
         sb.append(value).append(I);
      }
      sb.append(NL);

      LOGGER.info("Writing Cancer Evaluation data for Empty Cancer for " + patient + " to " + dataFile.getAbsolutePath() + " ...");
      saveText(sb.toString(), dataFile, true);
      LOGGER.info("Finished.");
   }


   private String getProvenance( final FactList valueFacts ) {
      if ( valueFacts == null ) {
         return "";
      }
      StringBuffer b = new StringBuffer();
      Collection<String> documents = new HashSet<>();
      Collection<String> mentions = new HashSet<>();
      for ( Fact f : valueFacts ) {
         for ( Fact ff : f.getProvenanceFacts() ) {
            if ( ff.getDocumentName() != null ) {
               documents.add( ff.getDocumentName() );
            }
            for ( TextMention t : ff.getProvenanceText() ) {
               mentions.add( t.getText() );
            }
         }

      }
      for ( String s : documents ) {
         b.append( s + FS );
      }
      for ( String s : mentions ) {
         b.append( s + FS );
      }
      return b.toString();
   }


   /**
    *
    * @param facts
    * @param category for example BodySite
    * @param modifierType for example Laterality, or for quadrant and clockface, BodySite
    * @return All modifiers of the given type, where the modifiers are modifiers of the facts in the given category
    */
   private FactList getModifiersWithValue( Map<String, FactList>  facts, final String category, final String modifierType, final String label) {
      FactList result = new DefaultFactList();
      FactList factsOfGivenCategory = facts.get(category);
      if (factsOfGivenCategory==null || factsOfGivenCategory.isEmpty()) return result;

      for (Fact f: factsOfGivenCategory) {
         BodySiteFact bf = (BodySiteFact) f;
         FactList modifiers = bf.getModifiers();
         for (Fact modifier: modifiers) {
            if (modifierType.equals(modifier.getType())) {
               if (label.contains("clockface")) {
                  if (modifier.getName().toLowerCase().contains("clock")) {
                     result.add(modifier);
                  }
               } else if (label.contains("quadrant")) {
                  if (modifier.getName().toLowerCase().contains("quadrant")) {
                     result.add(modifier);
                  }
               } else { // such as Laterality
                  result.add(modifier);
               }
            }
         }
      }

      return result;
   }
   /**
    * For example, for entryClass="Breast_Cancer" and entryProperty="hasBodySite"
    *   within the facts, look for a Fact for "Breast_Cancer", and get the Fact(s) associated with the property "hasBodySite"
    *
    * @param facts - the facts, indexed by category, to be searched
    * @param entryClass - for example "Breast_Cancer"
    * @param entryProperty - for example "hasBodySite".  could be a category, otherwise we look for Fact of class entryClass
    * @return - if the entryProperty is a category, return the Facts associated with the entryProperty for the fact with the given entryClass,
    * otherwise return the Facts of class entryClass
    */
   private FactList getFactsWithValue( Map<String, FactList> facts, final String entryClass, final String entryProperty ) {
      if (entryClass == null || entryClass.length() == 0) {
         return new DefaultFactList();
      }

      FactList list = facts.get(entryProperty);
      if ( list != null && !list.isEmpty() ) {
         return list;
      }
      // no such category exists in summary, lookup on the class
      list = new DefaultFactList();
      for (FactList factList : facts.values()) {
         for (Fact fact : factList) {
            if (isType( fact, entryClass)) {
               if (!list.contains( fact)) {
                  if (entryClass.equals("Receptor_Status")) {
                     if (fact.getName().startsWith(entryProperty)) {
                        list.add(fact);
                     }
                  } else {
                     list.add(fact);
                  }
               }
            }
         }
      }
      return list;
   }

   /**
    * get fact value for a given property
    *
    * @param entryProperty -
    * @return -
    */
   private String getFactValue( FactList facts, String entryClass, String entryProperty ) {
      if (entryClass.equals("Receptor_Status")) {
         LOGGER.debug("entryClass: " + entryClass +  " entryProperty: " + entryProperty);

      }
      StringBuffer b = new StringBuffer();
      if ( facts == null ) {
         return "";
      }
      // if there is no entryProperty, then this is Present/Absent business
      if ( entryClass.length() > 0 && entryProperty.length() == 0 ) {
         return !facts.isEmpty() ? "Present" : "Absent";
      }

      // go over facts
      HashSet<String> values = new HashSet<String>();
      for ( Fact fact : facts ) {
         Fact value = fact.getValue( entryProperty );
         if ( value == null ) {
            continue;
         }
         String normalized = getNormalizedValue(value);
         if (!values.contains(normalized)) {
            values.add(normalized);
            b.append(normalized).append(FS);
         }
      }

      if ( b.length() > 0 ) {
         // remove last field separator
         return b.substring(0, b.length()-1);
      } else {
         // never added anything to b
         return "";
      }

   }

   private String getNormalizedValue( Fact val ) {
      if ( val instanceof ValueFact ) {
         ValueFact vf = (ValueFact) val;
         return vf.getValueString();
      }
      return shortenStatusInterpretation(val.getName());
   }

   private boolean isType( Fact fact, String entryClass ) {
      if ( fact.getName().equals( entryClass ) ) {
         return true;
      }
      // check sub-classes
      return Neo4jOntologyConceptUtil.getRootUris( fact.getName() ).contains( entryClass );
   }

   private String getDocuments( Collection<NoteSpecs> allNoteSpecs ) {
      StringBuffer docs = new StringBuffer();
      List<String> titles = new ArrayList<String>();
      for ( NoteSpecs noteSpecs : allNoteSpecs ) {
         titles.add( noteSpecs.getDocumentId() );
      }
      Collections.sort( titles );
      for ( String r : titles ) {
         docs.append( r + FS );
      }
      // remove last field separator
      if ( docs.length() > 0 ) {
         docs.replace( docs.length() - 1, docs.length(), "" );
      }
      return docs.toString();
   }


   private String getEpisodeType(String dataLabel ) {
      for ( String name : FHIRConstants.EPISODE_TYPE_MAP.keySet() ) {
         if ( dataLabel.startsWith( name ) ) {
            return FHIRConstants.EPISODE_TYPE_MAP.get( name );
         }
      }
      return null;
   }


   /**
    * Intended for things like "Diagnostic", "Pre-Diagnostic", "Treatment", "Follow-up"
    * See FHIRConstants.EPISODE_TYPE_MAP for the latest.
    * @param dataLabel
    * @return
    */
   private boolean isEpisode( String dataLabel ) {
      for ( String name : FHIRConstants.EPISODE_TYPE_MAP.keySet() ) {
         if ( dataLabel.startsWith( name ) ) {
            return true;
         }
      }
      return false;
   }

   /**
    * load TCGA map from a given file
    *
    * @param file -
    * @return -
    * @throws IOException -
    */
   private List<Map<String, String>> loadEvaluationMap( final File file ) throws IOException {
      final List<Map<String, String>> mapping = new ArrayList<>();
      final List<String> keys = Arrays.asList( ENTRY_LABEL, ENTRY_CLASS, ENTRY_PROPERTY );
      try ( BufferedReader r = new BufferedReader( new FileReader( file ) ) ) {
         for ( String line = r.readLine(); line != null; line = r.readLine() ) {
            if ( line.trim().length() == 0 ) {
               continue;
            }
            final String[] parts = line.split( "\t" );
            final Map<String, String> map = new HashMap<>();
            for ( int i = 0; i < keys.size(); i++ ) {
               String val = (parts.length > i) ? parts[ i ] : "";
               map.put( keys.get( i ).trim(), val );
            }
            mapping.add( map );
         }
      }
      return mapping;
   }

   /**
    * Writes the column headings to a file
    * @param mapList
    * @param file If parent of file doesn't exist, creates directories as needed
    * @throws IOException
    */
   private void writeHeader( List<Map<String, String>> mapList, File file ) throws IOException {
      // has actual columns described above
      StringBuffer buffer = new StringBuffer();
      for ( Map<String, String> map : mapList ) {
         String label = map.get( ENTRY_LABEL );
         buffer.append( label + I );
      }
      buffer.append( NL );
      file.getParentFile().mkdirs();
      saveText( buffer.toString(), file, false );
   }

   /**
    * save to generic text file
    *
    * @param text -
    * @param file -
    * @param append true to append, false to erase existing (if <code>true</code>, then bytes will be written
    *               to the end of the file rather than the beginning)
    * @throws IOException -
    */
   private void saveText( String text, File file, boolean append ) throws IOException {
      synchronized (WRITE_LOCK) {
         try (BufferedWriter w = new BufferedWriter(new FileWriter(file, append))) {
            w.write(text);
         }
      }

   }


}
