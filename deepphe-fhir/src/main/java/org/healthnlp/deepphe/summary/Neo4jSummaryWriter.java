package org.healthnlp.deepphe.summary;


import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.note.NoteSpecs;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.fact.*;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.Neo4jConstants.*;
import static org.healthnlp.deepphe.neo4j.SearchUtil.getObjectNode;



/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/3/2018
 */
final public class Neo4jSummaryWriter extends JCasAnnotator_ImplBase implements NamedEngine {

   static private final Logger LOGGER = Logger.getLogger( "Neo4jSummaryWriter" );

   static private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddhhmm");



   static private final Map<String,RelationshipType> _relationshipTypes = new HashMap<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      LOGGER.info( "Initializing Neo4j Summary Writer ..." );
      // The super.initialize call will automatically assign user-specified values for to ConfigurationParameters.
      super.initialize( uimaContext );
      // MedicalRecordStore keeps a cache of Medical Records that are ready to be consumed.
      // In order to prevent a Medical Record from being disposed from the cache before it has been used,
      // register this class with the MedicalRecordStore.
      MedicalRecordStore.getInstance().registerEngine( getEngineName() );

      createAllPatientsNode();
      createAllDocumentsNode();
      createAllSummariesNode();
      // Don't want an "All Facts" node.  Adds mud with too many instance_of relations.
      // The label "Object" should be enough.
      createAllDatumNode();

   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      writeMedicalRecord();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      writeMedicalRecord();
   }

   private void writeMedicalRecord() {
      final MedicalRecord medicalRecord = MedicalRecordStore.getInstance().popMedicalRecord( getEngineName() );
      if ( medicalRecord == null ) {
         return;
      }
      LOGGER.info( "Writing Summary to Neo4j ..." );

      final Collection<NoteSummary> noteSummaries = medicalRecord.getNoteSummaries();

      // PatientSummary is not yet populated with data
      PatientSummary patientSummary = medicalRecord.getPatientSummary();
      if ( patientSummary == null ) {
         patientSummary = new PatientSummary( medicalRecord.getPatientIdentifier() );
         noteSummaries.stream()
                      .map( NoteSummary::getNoteSpecs )
                      .distinct()
                      .forEach( patientSummary::addNoteSpecs );
      }

      final CancerSummary cancer = medicalRecord.getCancerSummary();
      if ( !cancer.isEmpty() ) {
         writeCancerSummary( patientSummary, noteSummaries, cancer, medicalRecord.getSimplePatientName() );
      }
   }

   private void writeCancerSummary( final PatientSummary patient,
                                    final Collection<NoteSummary> noteSummaries,
                                    final CancerSummary cancer,
                                    final String patientName ) {

      final String patientId = getUniqueId( patient, patient.getPatientIdentifier() );

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );
      try ( Transaction tx = graphDb.beginTx() ) {

         final Node patientNode = createPatientNode( patientId, patient, patientName, graphDb, nodeFactory );

         noteSummaries.forEach( n -> createDocumentNode( patientId, patientNode, n, graphDb, nodeFactory ) );

         final Node cancerNode = createCancerNode( patientId, patientNode, cancer, graphDb, nodeFactory );

         tx.success();
         } catch ( MultipleFoundException mfE ) {
            LOGGER.error( mfE.getMessage(), mfE );
         }
   }


   static private Node createFactNode( final String patientId,
                                       final Node containerNode,
                                       final String category,
                                       final RelationshipType containerRelation,
                                       final Fact fact ) {

      final Collection<TextMention> textMentions = fact.getProvenanceText();
      final Collection<Mention> mentions = new ArrayList<>( textMentions.size() );
      for ( TextMention textMention : textMentions ) {
         mentions.add( new Mention( textMention.getStart(), textMention.getEnd(),
               createPatientDocId( patientId, textMention.getDocumentTitle() ) ) );
      }
      return createFactNodeG( containerNode, category, containerRelation, fact.getUri(),
            getNiceId( fact ), "", mentions );
   }


   static private void createRelations( final Fact fact, final GraphDatabaseService graphDb,
                                        final UniqueFactory.UniqueNodeFactory nodeFactory  ) {
      final String factId = getNiceId( fact );
      final Map<String, Collection<Fact>> relatedFacts = fact.getAllRelatedFacts();
      for ( Map.Entry<String, Collection<Fact>> related : relatedFacts.entrySet() ) {
         final String relationName = related.getKey();
         final Collection<String> relatedIds = related.getValue().stream()
                                                      .map( Neo4jSummaryWriter::getNiceId )
                                                      .collect( Collectors.toList() );

         createRelationsG( factId, relationName, relatedIds, graphDb, nodeFactory );
      }
   }

   static private String getDisplayValue( final ConceptInstance instance ) {
      final String uri = instance.getUri();
      if ( uri.equals( UriConstants.SIZE ) ) {
         return getSizeText( instance );
      }
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      if ( SearchUtil.getBranchUris( graphDb, UriConstants.STAGE ).contains( uri ) ) {
         return getStageText( instance.getPreferredText() );
      }
      if ( SearchUtil.getBranchUris( graphDb, UriConstants.RECEPTOR_STATUS ).contains( uri ) ) {
         return getReceptorStatusText( uri );
      }
      return "";
   }


   static private String getSizeText( final ConceptInstance instance ) {
      final List<String> texts = instance.getAnnotations()
                                                .stream()
                                                .map( Annotation::getCoveredText )
                                                .map( String::toLowerCase )
                                                .distinct()
                                                .sorted( Comparator.comparingInt( String::length ) )
                                                .collect( Collectors.toList() );
      final String size = texts.get( texts.size() - 1 );
      final List<Double> values = new ArrayList<>();
      final StringBuilder numberSb = new StringBuilder();
      final StringBuilder unitSb = new StringBuilder();
      for ( char c : size.toCharArray() ) {
         if ( Character.isDigit( c ) || c == '.' ) {
            numberSb.append( c );
            unitSb.setLength( 0 );
         } else {
            try {
               if ( !numberSb.toString().isEmpty() ) {
                  values.add( Double.valueOf( numberSb.toString() ) );
               }
               numberSb.setLength( 0 );
            } catch ( NumberFormatException nfE ) {
               LOGGER.error( "Could not parse number " + numberSb.toString() + " for size " + size );
            }
            unitSb.append( c );
         }
      }

      final UnitConverter.UnitInfo unitInfo = UnitConverter.UnitInfo.getByText( unitSb.toString() );
      if ( unitInfo == UnitConverter.UnitInfo.UNKNOWN ) {
         return values.stream()
                      .map( v -> v.toString() )
                      .collect( Collectors.joining( "x" ) );
      }
      final UnitConverter.Converter converter = unitInfo.getConverter();
      if ( converter.getSourceUrl() == null || converter.getTargetUrl() == null ) {
         return values.stream()
                      .map( v -> v.toString() )
                      .collect( Collectors.joining( "x" ) );
      }
      final String newValue = values.stream()
                                    .map( converter::convert )
                                    .map( v -> v.toString() )
                                    .collect( Collectors.joining( "x" ) );
      final UnitConverter.UnitInfo newUnitInfo = UnitConverter.UnitInfo.getByUrl( converter.getTargetUrl() );
      return newValue + " " + newUnitInfo.getPrefText();
   }

   static private String getStageText( final String fullStage ) {
      if ( fullStage.contains( "IV" ) ) {
         return "Stage IV";
      } else if ( fullStage.contains( "III" ) ) {
         return "Stage III";
      } else if ( fullStage.contains( "II" ) ) {
         return "Stage II";
      } else if ( fullStage.contains( "I" ) ) {
         return "Stage I";
      } else if ( fullStage.contains( "0" ) ) {
         return "Stage 0";
      } else if ( fullStage.contains( "4" ) ) {
         return "Stage IV";
      } else if ( fullStage.contains( "3" ) ) {
         return "Stage III";
      } else if ( fullStage.contains( "2" ) ) {
         return "Stage II";
      } else if ( fullStage.contains( "1" ) ) {
         return "Stage I";
      }
      return fullStage;
   }

   static private String getReceptorStatusText( final String uri ) {
      if ( uri.contains( "Positive" ) ) {
         return "Positive";
      } else if ( uri.contains( "Negative" ) ) {
         return "Negative";
      } else if ( uri.contains( "Unknown" ) ) {
         return "Unknown";
      }
      return "";
   }








   /////////////////////////////////////////////////////////////////////////////
   //
   //       Copied from MedicalRecordBsvWriter.  All of this is temporary.
   //
   /////////////////////////////////////////////////////////////////////////////

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
         LOGGER.info("entryClass: " + entryClass +  " entryProperty: " + entryProperty);

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


   static private UniqueFactory.UniqueNodeFactory createNodeFactory( final GraphDatabaseService graphDb ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         final UniqueFactory.UniqueNodeFactory factory = new UniqueFactory.UniqueNodeFactory( graphDb, NAME_INDEX ) {
            @Override
            protected void initialize( final Node created, final Map<String, Object> properties ) {
               // set a name for the node
               created.setProperty( NAME_KEY, properties.get( NAME_KEY ) );
            }
         };
         tx.success();
         return factory;
      }
   }


   static private Node createAllPatientsNode() {
      final Node thingNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.THING );
      if ( thingNode == null ) {
         LOGGER.error( "No " + UriConstants.THING + " node!  Cannot create put " + SUBJECT_URI + " in graph." );
         return null;
      }
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );

      try ( Transaction tx = graphDb.beginTx() ) {
         final Node subjectNode = nodeFactory.getOrCreate( NAME_KEY, SUBJECT_URI );
         subjectNode.addLabel( CLASS_LABEL );
         subjectNode.addLabel( SUBJECT_LABEL );
         subjectNode.createRelationshipTo( thingNode, IS_A_RELATION );
         final Node allPatientsNode = nodeFactory.getOrCreate( NAME_KEY, PATIENT_URI );
         allPatientsNode.addLabel( CLASS_LABEL );
         allPatientsNode.addLabel( SUBJECT_LABEL );
         allPatientsNode.createRelationshipTo( subjectNode, IS_A_RELATION );
         tx.success();
         return allPatientsNode;
      }
   }

   static private Node createAllDocumentsNode() {
      final Node thingNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.THING );
      if ( thingNode == null ) {
         LOGGER.error( "No Thing node!  Cannot create put " + EMR_NOTE_URI + " in graph." );
         return null;
      }

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );
      try ( Transaction tx = graphDb.beginTx() ) {

         final Node allDocumentsNode = nodeFactory.getOrCreate( NAME_KEY, EMR_NOTE_URI );
         allDocumentsNode.addLabel( CLASS_LABEL );
         allDocumentsNode.addLabel( FINDING_LABEL );
         allDocumentsNode.createRelationshipTo( thingNode, IS_A_RELATION );

         tx.success();
         return allDocumentsNode;
      }
   }

   static private Node createAllSummariesNode() {
      final Node thingNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.THING );
      if ( thingNode == null ) {
         LOGGER.error( "No Thing node!  Cannot create put " + SUMMARY_URI + " in graph." );
         return null;
      }

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );
      try ( Transaction tx = graphDb.beginTx() ) {

         final Node allSummariesNode = nodeFactory.getOrCreate( NAME_KEY, SUMMARY_URI );
         allSummariesNode.addLabel( CLASS_LABEL );
         allSummariesNode.addLabel( FINDING_LABEL );
         allSummariesNode.createRelationshipTo( thingNode, IS_A_RELATION );

         createSubClassNode( nodeFactory, allSummariesNode, CANCER_SUMMARY_URI, FINDING_LABEL, "Cancer Summaries" );
         createSubClassNode( nodeFactory, allSummariesNode, TUMOR_SUMMARY_URI, FINDING_LABEL, "Tumor Summaries" );

         tx.success();
         return allSummariesNode;
      }
   }

   static private Node createSubClassNode( final UniqueFactory.UniqueNodeFactory nodeFactory,
                                           final Node allParentNode,
                                           final String typeUri,
                                           final Label label,
                                           final String prefText ) {
      final Node subTypeNode = nodeFactory.getOrCreate( NAME_KEY, typeUri );
      subTypeNode.addLabel( CLASS_LABEL );
      subTypeNode.addLabel( label );
      subTypeNode.setProperty( PREF_TEXT_KEY, prefText );
      subTypeNode.createRelationshipTo( allParentNode, IS_A_RELATION );
      return subTypeNode;
   }


   static private Node createAllDatumNode() {
      final Node thingNode = Neo4jOntologyConceptUtil.getClassNode( UriConstants.THING );
      if ( thingNode == null ) {
         LOGGER.error( "No " + UriConstants.THING + " node!  Cannot create put " + SOURCE_DATUM_URI + " in graph." );
         return null;
      }

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );
      try ( Transaction tx = graphDb.beginTx() ) {

         final Node sourceDatumNode = nodeFactory.getOrCreate( NAME_KEY, SOURCE_DATUM_URI );
         sourceDatumNode.addLabel( CLASS_LABEL );
         sourceDatumNode.addLabel( FINDING_LABEL );
         sourceDatumNode.createRelationshipTo( thingNode, IS_A_RELATION );

         final Node structuredEntryNode = nodeFactory.getOrCreate( NAME_KEY, STRUCTURED_ENTRY_URI );
         structuredEntryNode.addLabel( CLASS_LABEL );
         structuredEntryNode.addLabel( FINDING_LABEL );
         structuredEntryNode.createRelationshipTo( sourceDatumNode, IS_A_RELATION );

         tx.success();
         return sourceDatumNode;
      }
   }


   static private Node createPatientNode( final String patientId,
                                          final PatientSummary patientSummary,
                                          final String patientName,
                                          final GraphDatabaseService graphDb,
                                          final UniqueFactory.UniqueNodeFactory nodeFactory  ) {
      final Node allPatientsNode = Neo4jOntologyConceptUtil.getClassNode( PATIENT_URI );
      if ( allPatientsNode == null ) {
         LOGGER.error( "No class for uri " + PATIENT_URI + ".  Cannot create put patient in graph." );
         return null;
      }
      try ( Transaction tx = graphDb.beginTx() ) {
         final Node patientNode = nodeFactory.getOrCreate( NAME_KEY, patientId );
         patientNode.addLabel( OBJECT_LABEL );
         patientNode.addLabel( SUBJECT_LABEL );
         patientNode.createRelationshipTo( allPatientsNode, INSTANCE_OF_RELATION );
         patientNode.setProperty( PATIENT_NAME, patientName );
         patientNode.setProperty( PATIENT_GENDER, patientSummary.getFilledGender() );
         patientNode.setProperty( PATIENT_BIRTH_DATE, patientSummary.getBirthday() );
         patientNode.setProperty( PATIENT_DEATH_DATE, patientSummary.getDeathday() );
         patientNode.setProperty( PATIENT_FIRST_ENCOUNTER, patientSummary.getFirstDateSlashText() );
         patientNode.setProperty( PATIENT_LAST_ENCOUNTER, patientSummary.getLastDateSlashText() );
         tx.success();
         return patientNode;
      }
   }

   static private Node createCancerNode( final String patientId,
                                         final Node patientNode,
                                         final CancerSummary cancerSummary,
                                         final GraphDatabaseService graphDb,
                                         final UniqueFactory.UniqueNodeFactory nodeFactory ) {
      final Node allCancersNode = Neo4jOntologyConceptUtil.getClassNode( CANCER_SUMMARY_URI );

      try ( Transaction tx = graphDb.beginTx() ) {
         final String cancerId = getUniqueId( cancerSummary, cancerSummary.getFullCancerIdentifier() );
         final Node cancerNode = nodeFactory.getOrCreate( NAME_KEY, cancerId );
         cancerNode.addLabel( OBJECT_LABEL );
         cancerNode.addLabel( FINDING_LABEL );
         patientNode.createRelationshipTo( cancerNode, SUBJECT_HAS_CANCER_RELATION );
         createInstanceOf( graphDb, cancerNode, allCancersNode );

         cancerNode.setProperty( "TEMPORALITY", cancerSummary.getTemporality() );
         cancerNode.setProperty( "LATERALITY", cancerSummary.getBodySide() );
         boolean wroteStage = false;
         final Map<String, FactList> factsIndexedByCategory = cancerSummary.getSummaryFacts();
         for ( Map.Entry<String, FactList> entry : factsIndexedByCategory.entrySet() ) {
            final String category = entry.getKey();
            wroteStage |= category.equals( RelationConstants.HAS_STAGE );
            for ( Fact fact : entry.getValue() ) {
               final ConceptInstance instance = fact.getConceptInstance();
               if ( instance != null ) {
                  createFactNode( patientId, cancerNode, category, CANCER_HAS_FACT_RELATION, instance );
               } else {
                  createFactNode( patientId, cancerNode, category, CANCER_HAS_FACT_RELATION, fact );
               }
            }
         }

         for ( Map.Entry<String, FactList> entry : factsIndexedByCategory.entrySet() ) {
            for ( Fact fact : entry.getValue() ) {
               final ConceptInstance instance = fact.getConceptInstance();
               if ( instance != null ) {
                  createRelations( instance, graphDb, nodeFactory );
               } else {
                  createRelations( fact, graphDb, nodeFactory );
               }
            }
         }

         if ( !wroteStage ) {
            // Viz tool requires a stage for cohort display.  Add an explicit fact for unknown stage.
            createFactNode( patientId, cancerNode, RelationConstants.HAS_STAGE, CANCER_HAS_FACT_RELATION, createUnknownStage( patientId ) );
         }

         for ( TumorSummary tumorSummary : cancerSummary.getTumors() ) {
            createTumorNode( patientId, cancerNode, tumorSummary, graphDb, nodeFactory );
         }

         tx.success();

         return cancerNode;
      }
   }


   static private Fact createUnknownStage( final String patientId ) {
      final Fact fact = FactFactory.createTypeFact( FHIRConstants.CONDITION );
      fact.setCategory( RelationConstants.HAS_STAGE );
      fact.setUri( UriConstants.STAGE_UNKNOWN );
      fact.autoFillDefaults();
      return fact;
   }

   static private Node createTumorNode( final String patientId,
                                        final Node cancerNode,
                                        final TumorSummary tumorSummary,
                                        final GraphDatabaseService graphDb,
                                        final UniqueFactory.UniqueNodeFactory nodeFactory ) {
      final Node allTumorsNode = Neo4jOntologyConceptUtil.getClassNode( TUMOR_SUMMARY_URI );

      try ( Transaction tx = graphDb.beginTx() ) {
         // For some reason tumor identifier starts with "cancer_" instead of "tumor_".
         // This leads to incorrect graph nodes.
         String tumorId = tumorSummary.getFullTumorIdentifier();
         if ( tumorId.startsWith( "cancer_" ) ) {
            tumorId = "tumor_" + tumorId.substring( 7 );
         }
         tumorId = getUniqueId( tumorSummary, tumorId );
         final Node tumorNode = nodeFactory.getOrCreate( NAME_KEY, tumorId );
         tumorNode.addLabel( OBJECT_LABEL );
         tumorNode.addLabel( FINDING_LABEL );
         cancerNode.createRelationshipTo( tumorNode, CANCER_HAS_TUMOR_RELATION );
         createInstanceOf( graphDb, tumorNode, allTumorsNode );

         tumorNode.setProperty( "TEMPORALITY", tumorSummary.getTemporality() );

         final Map<String, FactList> factsIndexedByCategory = tumorSummary.getSummaryFacts();
         boolean wroteHasTumorType = false;
         for ( Map.Entry<String, FactList> entry : factsIndexedByCategory.entrySet() ) {
            final String category = entry.getKey();
            for ( Fact fact : entry.getValue() ) {
               final ConceptInstance instance = fact.getConceptInstance();
               wroteHasTumorType |= category.equals( RelationConstants.HAS_TUMOR_TYPE );
               if ( instance != null ) {
                  createFactNode( patientId, tumorNode, category, TUMOR_HAS_FACT_RELATION, instance );
               } else {
                  createFactNode( patientId, tumorNode, category, TUMOR_HAS_FACT_RELATION, fact );
               }
            }
         }
         if ( !wroteHasTumorType ) {
            LOGGER.error( "No tumor type for Tumor " + tumorId );
         }

         for ( Map.Entry<String, FactList> entry : factsIndexedByCategory.entrySet() ) {
            for ( Fact fact : entry.getValue() ) {
               final ConceptInstance instance = fact.getConceptInstance();
               if ( instance != null ) {
                  createRelations( instance, graphDb, nodeFactory );
               } else {
                  createRelations( fact, graphDb, nodeFactory );
               }
            }
         }

         tx.success();

         return cancerNode;
      }
   }

   static private Node createDocumentNode( final String patientId,
                                           final Node patientNode,
                                           final NoteSummary noteSummary,
                                           final GraphDatabaseService graphDb,
                                           final UniqueFactory.UniqueNodeFactory nodeFactory ) {
      final NoteSpecs noteSpecs = noteSummary.getNoteSpecs();

      final String patientDocId = createPatientDocId( patientId, noteSpecs.getDocumentId() );
      Node documentNode = getObjectNode( graphDb, patientDocId );
      if ( documentNode != null ) {
         return documentNode;
      }

      final String docType = noteSpecs.getDocumentType();
      final String docTypeUri = getDocTypeUri( docType );
      Node documentTypeNode = Neo4jOntologyConceptUtil.getClassNode( docTypeUri );
      if ( documentTypeNode == null ) {
         final Node allDocumentsNode = Neo4jOntologyConceptUtil.getClassNode( EMR_NOTE_URI );
         if ( allDocumentsNode == null ) {
            LOGGER.error( "No class for uri " + EMR_NOTE_URI +
                          ".  Cannot create document type " + docTypeUri +
                          " for document " + patientDocId + " in graph." );
            return null;
         }
         documentTypeNode
               = createSubClassNode( nodeFactory, allDocumentsNode, docTypeUri, FINDING_LABEL, getDocTypePrefText( docType ) );
      }
      documentNode = nodeFactory.getOrCreate( NAME_KEY, patientDocId );
      documentNode.addLabel( OBJECT_LABEL );
      documentNode.addLabel( FINDING_LABEL );
      documentNode.createRelationshipTo( documentTypeNode, INSTANCE_OF_RELATION );
      patientNode.createRelationshipTo( documentNode, SUBJECT_HAS_NOTE_RELATION );
      documentNode.setProperty( NOTE_NAME, noteSpecs.getDocumentId() );
      documentNode.setProperty( NOTE_EPISODE, noteSummary.getEpisodeType() );
      final String docText = noteSpecs.getDocumentText();
      if ( docText != null ) {
         documentNode.setProperty( NOTE_TEXT, docText );
      } else {
         LOGGER.error( "No document text for " + patientDocId );
      }
      // Writes note date / time in format yyyyMMddhhmm
      documentNode.setProperty( NOTE_DATE, noteSpecs.getNoteTime() );
      return documentNode;
   }

   static private Node createFactNode( final String patientId,
                                       final Node containerNode,
                                       final String category,
                                       final RelationshipType containerRelation,
                                       final ConceptInstance instance ) {
      final Collection<IdentifiedAnnotation> annotations = instance.getAnnotations();
      final Collection<Mention> mentions = new ArrayList<>( annotations.size() );
      for ( IdentifiedAnnotation annotation : annotations ) {
         mentions.add( new Mention( annotation.getBegin(), annotation.getEnd(),
               createPatientDocId( patientId, instance.getDocumentId( annotation ) ) ) );
      }
      return createFactNodeG( containerNode, category, containerRelation, instance.getUri(),
            getNiceId( instance ), getDisplayValue( instance ), mentions );
   }


   static private Node createFactNodeG( final Node containerNode,
                                        final String category,
                                        final RelationshipType containerRelation,
                                        String uri,
                                        final String factId,
                                        final String valueText,
                                        final Collection<Mention> mentions ) {
      uri = getTrimUri( uri );
      final Node classNode = Neo4jOntologyConceptUtil.getClassNode( uri );
      if ( classNode == null ) {
         LOGGER.error( "No class for uri " + uri + ".  Cannot create put fact in graph." );
         return null;
      }
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );
      try ( Transaction tx = graphDb.beginTx() ) {

         final Node factNode = nodeFactory.getOrCreate( NAME_KEY, factId );
         factNode.addLabel( OBJECT_LABEL );
         factNode.addLabel( Label.label( Neo4jOntologyConceptUtil.getSemanticGroup( uri ).getName() ) );

         createInstanceOf( graphDb, factNode, classNode );

         if ( valueText != null && !valueText.isEmpty() ) {
            factNode.setProperty( VALUE_TEXT, valueText );
         }

         for ( Mention mention : mentions ) {
            final Node textMentionNode = createTextMentionNodeG( mention, factNode );
         }

         final Relationship relationship = containerNode.createRelationshipTo( factNode, containerRelation );
         relationship.setProperty( SUMMARY_FACT_RELATION_TYPE, category );
         final Relationship explicitR
               = containerNode.createRelationshipTo( factNode,
               _relationshipTypes.computeIfAbsent( category, c -> RelationshipType.withName( category ) ) );

         tx.success();
         return factNode;
      }
   }

   static private final class Mention {
      private final int _begin;
      private final int _end;
      private final String _patientDocId;

      private Mention( final int begin, final int end, final String patientDocId ) {
         _begin = begin;
         _end = end;
         _patientDocId = patientDocId;
      }
   }


   static private void createRelations( final ConceptInstance instance,
                                        final GraphDatabaseService graphDb,
                                        final UniqueFactory.UniqueNodeFactory nodeFactory  ) {
      final String factId = getNiceId( instance );
      for ( Map.Entry<String, Collection<ConceptInstance>> ciRelation : instance.getRelated().entrySet() ) {
         final String relationName = ciRelation.getKey();
         final Collection<String> relatedIds = ciRelation.getValue().stream()
                                                         .map( Neo4jSummaryWriter::getNiceId )
                                                         .collect( Collectors.toList() );
         createRelationsG( factId, relationName, relatedIds, graphDb, nodeFactory );
      }
   }

   static private void createRelationsG( final String factId,
                                         final String relationName,
                                         final Collection<String> factIds2,
                                         final GraphDatabaseService graphDb,
                                         final UniqueFactory.UniqueNodeFactory nodeFactory ) {
      final Node factNode = getObjectNode( graphDb, factId );
      if ( factNode == null ) {
         LOGGER.error( "No node for " + factId + " in graph." );
         return;
      }

      final RelationshipType relationshipType
            = _relationshipTypes.computeIfAbsent( relationName, RelationshipType::withName );
      for ( String relatedId : factIds2 ) {
         final Node relatedNode = getObjectNode( graphDb, relatedId );
         if ( relatedNode == null ) {
            LOGGER.error(
                  "No related node " + relatedId + " for " + relationName + " for node " + factId + " in graph." );
            continue;
         }
         factNode.createRelationshipTo( relatedNode, relationshipType );
         // create generic relation for easy consumer traversal
         final Relationship hasFactRelation
               = factNode.createRelationshipTo( relatedNode, FACT_HAS_RELATED_FACT_RELATION );
         hasFactRelation.setProperty( FACT_RELATION_TYPE, relationName );
         final Relationship explicitR
               = factNode.createRelationshipTo( relatedNode,
               _relationshipTypes.computeIfAbsent( relationName, c -> RelationshipType.withName( relationName ) ) );

      }
   }


   static private Node createTextMentionNodeG( final Mention mention,
                                               final Node factNode ) {
      final String patientDocId = mention._patientDocId;
      final int spanBegin = mention._begin;
      final int spanEnd = mention._end;
      final String mentionId = patientDocId + '_' + spanBegin + '_' + spanEnd;
      Node mentionNode = getObjectNode( Neo4jConnectionFactory.getInstance().getGraph(), mentionId );
      if ( mentionNode != null ) {
         return mentionNode;
      }

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Node documentNode = getObjectNode( graphDb, patientDocId );
      if ( documentNode == null ) {
         LOGGER.error(
               "No document " + patientDocId + " for annotation " + mentionId + " in graph." );
      }
      final UniqueFactory.UniqueNodeFactory nodeFactory = createNodeFactory( graphDb );
      try ( Transaction tx = graphDb.beginTx() ) {
         mentionNode = nodeFactory.getOrCreate( NAME_KEY, mentionId );
         mentionNode.addLabel( OBJECT_LABEL );
         mentionNode.addLabel( FINDING_LABEL );
         factNode.createRelationshipTo( mentionNode, FACT_HAS_TEXT_MENTION_RELATION );

         mentionNode.setProperty( TEXT_SPAN_BEGIN, spanBegin );
         mentionNode.setProperty( TEXT_SPAN_END, spanEnd );

         if ( documentNode != null ) {
            documentNode.createRelationshipTo( mentionNode, NOTE_HAS_TEXT_MENTION_RELATION );
         }

         tx.success();
         return mentionNode;
      }
   }


   static private void createInstanceOf( final GraphDatabaseService graphDb, final Node instanceNode,
                                         final Node classNode ) {
      try ( Transaction tx = graphDb.beginTx() ) {
         for ( Relationship existing : instanceNode.getRelationships( INSTANCE_OF_RELATION, Direction.OUTGOING ) ) {
            if ( existing.getOtherNode( instanceNode ).equals( classNode ) ) {
               tx.success();
               return;
            }
         }
         instanceNode.createRelationshipTo( classNode, INSTANCE_OF_RELATION );
         tx.success();
      } catch ( MultipleFoundException mfE ) {
//         throw new RuntimeException( mfE );
      }
   }

   static private String getDocTypeUri( final String docType ) {
      switch ( docType ) {
         case "RAD":
            return "Radiology_Report";
         case "PATH":
            return "Pathology_Report";
         case "SP":
            return "Surgical_Pathology_Report";
         case "DS":
            return "Discharge_Summary";
         case "PGN":
            return "Progress_Note";
         case "NOTE":
            return "Clinical_Note";
         case NoteSpecs.ID_NAME_CLINICAL_NOTE:
            return "Clinical_Note";
      }
      return docType.replace( ' ', '_' );
   }

   static private String getDocTypePrefText( final String docType ) {
      switch ( docType ) {
         case "RAD":
            return "Radiology Report";
         case "PATH":
            return "Pathology Report";
         case "SP":
            return "Surgical Pathology Report";
         case "DS":
            return "Discharge Summary";
         case "PGN":
            return "Progress Note";
         case "NOTE":
            return "Clinical Note";
         case NoteSpecs.ID_NAME_CLINICAL_NOTE:
            return "Clinical Note";
      }
      return docType.replace( '_', ' ' );
   }


   static private String getUniqueId( final Summary summary, final String currentId ) {
      return currentId + "_" + summary.getUniqueIdNum();
   }

   static private String getTrimUri( String uri ) {
      final int hash = uri.indexOf( '#' );
      if ( hash >= 0 ) {
         uri = uri.substring( hash + 1 );
      }
      if ( uri.equals( "PrimaryTumor" ) ) {
         return "Primary_Neoplasm";
      }
      return uri;
   }

   /**
    * @return [PatientId]_[DocId]_[SemanticGroup]_[HashCode]
    */
   static private String getNiceId( final ConceptInstance instance ) {
      return getTrimUri( instance.getUri() ) + '_' + instance.getId();
   }

   /**
    * @return [uri]_[PatientId]_[DocId]_[SemanticGroup]_[HashCode]
    */
   static private String getNiceId( final Fact fact ) {
      return getTrimUri( fact.getUri() ) + '_' + fact.getPatientIdentifier() + '_' + fact.getDocumentIdentifier() +
             '_' + fact.getType() + '_' + fact.hashCode();
   }

   static private String createPatientDocId( final String patientId, final String docId ) {
      return patientId + '_' + docId;
   }

}
