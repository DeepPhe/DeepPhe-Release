package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.concept.instance.IdOwner;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.healthnlp.deepphe.util.FHIRUtils;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.util.FHIRConstants.*;
import static org.healthnlp.deepphe.util.FHIRUtils.LANGUAGE_ASPECT_DOC_TIME_REL_URL;


/**
 * Fact representing a piece of information in Ontology
 *
 * @author tseytlin
 */
public class Fact implements IdOwner, PropertyOwner, ProvenanceOwner, AncestorOwner, Appendable<Fact> {


   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private final long _unique_id_num;


   private String name, uri, identifier, label, category, type = getClass().getSimpleName();
   private String summaryType, summaryId;
   private Collection<String> ancestors;
   private Collection<String> rulesApplied;
   private List<Fact> provenanceFacts;
   private List<TextMention> provenanceText;
   private Date recordedDate = null;
   private int temporalOrder;

   private String documentIdentifier, patientIdentifier, documentType;
   private transient Collection<String> containerIdentifier;
   private Map<String, String> properties;

   private Map<String, Collection<Fact>> _relatedFacts;

   private final ConceptInstance _conceptInstance;
   
   public Fact() {
      this( null, null );
   }

   public Fact( final ConceptInstance conceptInstance ) {
      this( conceptInstance, null );
   }

   public Fact( final ConceptInstance conceptInstance, final String type ) {
      _unique_id_num = createUniqueIdNum();
      _conceptInstance = conceptInstance;
      if ( type != null && !type.isEmpty() ) {
         setType( type );
      } else if ( conceptInstance != null ) {
         setType( conceptInstance.getSemanticGroup() );
      }
      if ( conceptInstance != null ) {
         setIdentifier( getClassDash() + conceptInstance.getId() );
         setUri( conceptInstance.getUri() );
         setPatientIdentifier( conceptInstance.getPatientId() );
         setDocumentIdentifier( conceptInstance.getJoinedDocumentId() );
         setDocumentTitle( conceptInstance.getJoinedDocumentId() );
         final Date docDate = conceptInstance.getDocumentDate( null );
         setRecordedDate( docDate );
         setLabel( conceptInstance.getUri() );
         conceptInstance.getAnnotations()
                        .stream()
                        .map( a -> Fact.createTextMention( conceptInstance.getDocumentId( a ), a ) )
                        .forEach( this::addProvenanceText );
         addProperty( HAS_POLARITY, conceptInstance.isNegated() ? POLARITY_NEGATIVE : POLARITY_POSITIVE );
         // cheat to insert FHIRConstants temporality constants
         addProperty( HAS_TEMPORALITY, conceptInstance.getDocTimeRel() + "_DocTimeRel" );
         addProperty( "hasUncertainty", conceptInstance.isUncertain() ? "Uncertain" : "Certain" );
         addProperty( "hasGeneric", conceptInstance.isGeneric() ? "Generic" : "Actual" );
         addProperty( "hasHistoricity", conceptInstance.inPatientHistory() ? "Historical" : "Current" );
      }
   }

   /**
    * ANY summary can have a patient_site_etc id in common with another.
    * For instance, patientA has two current tumors on the left breast.  One is 1mm upper inner, one 5mm lower outer.
    */
   protected long createUniqueIdNum() {
      synchronized ( ID_NUM_LOCK ) {
         _ID_NUM++;
         return _ID_NUM;
      }
   }

   final public ConceptInstance getConceptInstance() {
      return _conceptInstance;
   }

   /**
    * ANY fact can have a patient_site_etc id in common with another.
    * For instance, patientA and patientB have current tumors on the left breast.
    *
    * @return an index number unique to this summary
    */
   final public long getUniqueIdNum() {
      return _unique_id_num;
   }


   /**
    * @deprecated use getId()
    * Not removed yet because used by drools rules (.drl files)
    */
   final public String getIdentifier() {
      return getId();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public String getId() {
      if ( identifier == null ) {
    	  identifier = createDefaultId();
      }
      return identifier;
   }

   final protected String getClassDash() {
      return getClass().getSimpleName() + "-";
   }

   final public Map<String, Collection<Fact>> getAllRelatedFacts() {
      if ( _relatedFacts == null ) {
         _relatedFacts = new HashMap<>();
      }
      return _relatedFacts;
   }

   final public Collection<Fact> getRelatedFacts( final String relationName ) {
      return getAllRelatedFacts().computeIfAbsent( relationName, r -> new ArrayList<>() );
   }

   final public void addRelatedFact( final String relationName, final Fact fact ) {
      getRelatedFacts( relationName ).add( fact );
   }

   @Override
   public Map<String, String> getProperties() {
      if ( properties == null ) {
         properties = new HashMap<>();
      }
      return properties;
   }

   @Override
   public void setProperties( final Map<String, String> properties ) {
      this.properties = properties;
   }

   final public String getDocumentTitle() {
      return getProperties().get( FHIRUtils.DOCUMENT_TITLE_URL );
   }

   final public void setDocumentTitle( final String documentTitle ) {
      getProperties().put( FHIRUtils.DOCUMENT_TITLE_URL, documentTitle );
   }

   final public String getDocumentName() {
      return documentIdentifier;
   }

   final public void setDocumentName( final String documentName ) {
      documentIdentifier = documentName;
   }

   /**
    * usually a single-word name for this Fact
    * @return usually the uri extension, possibly the label with ' ' replaced with '_', etc.
    */
   final public String getName() {
      return name;
   }

   public String getFullName() {
      return getName();
   }

   final public void setName( final String name ) {
      this.name = name;
   }

   public String getUri() {
      return uri;
   }

   final public void setUri( final String uri ) {
      this.uri = uri;
   }

   final public boolean isSameClass( final Fact fact ) {
      return getUri().equals( fact.getUri() );
   }

   final public boolean isParentClassOf( final Fact fact ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      return SearchUtil.getBranchUris( graphDb, getUri() ).contains( fact.getUri() );
   }

   final public boolean isChildClassOf( final Fact fact ) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      return SearchUtil.getBranchUris( graphDb, fact.getUri() ).contains( getUri() );
   }

   final public String getType() {
      return type;
   }

   final public void setType( final String type ) {
      this.type = type;
   }


   final public void setIdentifier( final String identifier ) {
      this.identifier = identifier;
   }

   @Override
   final public List<Fact> getProvenanceFacts() {
      if ( provenanceFacts == null ) {
         provenanceFacts = new ArrayList<>();
      }
      return provenanceFacts;
   }

   @Override
   final public List<TextMention> getProvenanceText() {
      if ( provenanceText == null ) {
         provenanceText = new ArrayList<>();
      }
      return provenanceText;
   }

   /**
    *
    * @return typically some preferred text for this Fact
    */
   public String getLabel() {
      if ( label == null ) {
         createDefaultLabel();
      }
      return label;
   }

   public void setLabel( final String label ) {
      this.label = label;
   }

   public String getSummaryText() {
	   return getName();
   }

   @Override
   public Collection<String> getAncestors() {
      if ( ancestors == null ) {
         ancestors = new HashSet<>();
      }
      return ancestors;
   }

   public String getDocumentType() {
      return documentType;
   }

   public void setDocumentType( final String docType ) {
      documentType = docType;
   }

   public void setDocumentIdentifier( final String docId ) {
      documentIdentifier = docId;
   }

   final public String getCategory() {
      return category;
   }

   final public void setCategory( final String category ) {
      this.category = category;
   }

   final public String getDocumentIdentifier() {
      return documentIdentifier;
   }

   final public String getPatientIdentifier() {
      return patientIdentifier;
   }

   final public void setPatientIdentifier( final String patientIdentifier ) {
      this.patientIdentifier = patientIdentifier;
   }

   final public Collection<String> getContainerIdentifier() {
      if ( containerIdentifier == null ) {
         containerIdentifier = new ArrayList<>();
      }
      return containerIdentifier;
   }

   final public void addContainerIdentifier( final String containerIdentifier ) {
      final Collection<String> containerIds = getContainerIdentifier();
      if (containerIdentifier == null) {
          throw new RuntimeException("null passed for containerIdentifier");
      }
      if ( !containerIds.contains( containerIdentifier ) ) {
         getContainerIdentifier().add( containerIdentifier );
      }
   }

   final public Collection<String> getRulesApplied() {
      if ( rulesApplied == null ) {
         rulesApplied = new LinkedHashSet<>();
      }
      return rulesApplied;
   }

   final public void addRulesApplied( final String ruleName ) {
      getRulesApplied().add( ruleName );
   }

   public String getInfo() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "name: " )
        .append( getName() )
        .append( "|" );
      sb.append( "uri: " )
        .append( getUri() )
        .append( "|" );
      sb.append( "category: " )
        .append( getCategory() )
        .append( "|" );
      sb.append( "type: " )
        .append( getType() )
        .append( "|" );
      sb.append( "id: " )
        .append( getId() )
        .append( "|" );
      sb.append( "patient id: " )
        .append( getPatientIdentifier() )
        .append( "|" );
      sb.append( "document id: " )
        .append( getDocumentIdentifier() )
        .append( "|" );
      sb.append( "document type: " )
        .append( getDocumentType() )
        .append( "|" );
      sb.append( "document title: " )
        .append( getDocumentTitle() )
        .append( "|" );
      sb.append( "summary type: " )
        .append( getSummaryType() )
        .append( "|" );
      sb.append( "summary id: " )
        .append( getSummaryId() )
        .append( "|" );
      sb.append( "container ids: " )
        .append( getContainerIdentifier() )
        .append( "|" );
      sb.append( "recordDate: " )
        .append( getRecordedDate() )
        .append( "|" );
      sb.append( "ancestors: " )
        .append( getAncestors() )
        .append( "|" );

      for ( String key : getProperties().keySet() ) {
         sb.append( FHIRUtils.getLanguageAspectLabel( key ) )
           .append( ": " )
           .append( getProperty( key ) )
           .append( "|" );
      }

      return sb.toString();
   }

   
   public String getInfoDrools(boolean withId) {
	      final StringBuilder sb = new StringBuilder();
	      sb.append( getName() )
	        .append( "|" );
	      if(withId) {
	    	  sb.append( getUri() ) .append( "|" );
	    	  sb.append( getIdentifier() ).append( "|" );
	      }
	      sb.append( getCategory() )
	        .append( "|" );
	      sb.append( getType() )
	        .append( "|" );
	      if(withId) {
		      sb.append( getPatientIdentifier() )
		        .append( "|" );
		      sb.append( getDocumentIdentifier() )
		        .append( "|" );
		      sb.append( getDocumentType() )
		        .append( "|" );
		      sb.append( getDocumentTitle() )
		        .append( "|" );
	    	  sb.append( getSummaryType() )
	        .append( "|" );
	      }
	      sb.append( getSummaryId() )
	        .append( "|" );
	      if(withId) {
		      sb.append( getContainerIdentifier().stream().collect( Collectors.joining( " , " ) ) )
		        .append( "|" );
	      
		      sb.append( getRecordedDate() )
		        .append( "|" );
		      sb.append( getAncestors().stream().collect( Collectors.joining( " , " ) ) );
	      }
	      return sb.toString();
	   }


   public Date getRecordedDate() {
      return recordedDate;
   }

   public void setRecordedDate( final Date recordedDate ) {
      this.recordedDate = recordedDate;
   }

   public int getTemporalOrder() {
      return temporalOrder;
   }

   public void setTemporalOrder( final int temporalOrder ) {
      this.temporalOrder = temporalOrder;
   }

   /**
    * return all facts that are contained within this fact
    * This creates a new list.
    * @return all facts from {@code getAllRelatedFacts} but in a single list
    */
   public List<Fact> getContainedFacts() {
      final ArrayList<Fact> allFacts = new ArrayList<>();

      final Map<String, Collection<Fact>> relations = getAllRelatedFacts();
      final Collection<String> relationNames = relations.keySet().stream().sorted().collect( Collectors.toList() );
      for ( String relationName : relationNames ) {
         final Collection<Fact> relatedFacts = relations.get( relationName );
         allFacts.addAll(relatedFacts);

      }
      return allFacts;
   }

   /**
    * convenience method used for creating list of contained facts and updating their container Id list
    *
    * @param facts -
    * @param fact  -
    * @return -
    */
   protected List<Fact> addContainedFact( final List<Fact> facts, final Fact fact ) {
      if ( this.equals( fact ) ) {
         // Don't want to "add a fact to itself".  Comes about because of the phenotype summary
         return facts;
      }
      if (this.getSummaryId()!=null) {
         fact.addContainerIdentifier(this.getSummaryId());
      }
      facts.add( fact );
      return facts;
   }

   /**
    * get a value for this fact for a given property
    *
    * @param property ignored
    * @return this fact
    */
   public Fact getValue( final String property ) {
      return this;
   }

   public String getSummaryType() {
      return summaryType;
   }

   public void setSummaryType( final String summaryType ) {
      this.summaryType = summaryType;
   }

   public String getSummaryId() {
      return summaryId;
   }

   public void setSummaryId( final String summaryId ) {
      this.summaryId = summaryId;
   }

   public boolean equivalent( final Fact fact ) {
      return fact != null && isSameClass( fact );
   }

   public String getDocTimeRel() {
      return getProperty( LANGUAGE_ASPECT_DOC_TIME_REL_URL );
   }

   @Override
   public boolean isAppendable( final Fact fact ) {
      return equivalent( fact );
   }

   /**
    * append information from an existing fact
    *
    * @param fact -
    */
   @Override
   public void append( final Fact fact ) {
      if ( !equivalent( fact ) ) {
         return;
      }
      // add this fact as provenance
      getAncestors().addAll( fact.getAncestors() );
      getProvenanceText().addAll( fact.getProvenanceText() );
      getProvenanceFacts().add( fact );
      getProvenanceFacts().addAll( fact.getProvenanceFacts() );
   }

   public String getDocumentSection() {
      return getProperties().get( FHIRUtils.SECTION_URL );
   }

   /**
    * @return a flat list of all provenance text objects related to this fact
    */
   public Set<TextMention> getContainedProvenanceText() {
      return getContainedProvenanceText( this, new ArrayList<>() );
   }

   /**
    * @return a flat list of all provenance text objects related to this fact
    */
   public Set<String> getContainedProvenanceRules() {
      return getContainedProvenanceRules( this, new HashSet<>() );
   }

   /**
    * @return a flat list of all provenance text objects related to this fact
    */
   private Set<String> getContainedProvenanceRules( final Fact fact, final Set<Fact> visitedFacts ) {
      if ( visitedFacts.contains( fact ) ) {
         return Collections.emptySet();
      }
      final Set<String> list = new HashSet<>( fact.getRulesApplied() );
      visitedFacts.add( fact );
      for ( Fact ff : fact.getContainedFacts() ) {
         list.addAll( getContainedProvenanceRules( ff, visitedFacts ) );
      }
      for ( Fact ff : fact.getProvenanceFacts() ) {
         list.addAll( getContainedProvenanceRules( ff, visitedFacts ) );
      }
      return list;
   }


   /**
    * @return a flat list of all provenance text objects related to this fact
    */
   private Set<TextMention> getContainedProvenanceText( final Fact fact, final List<Fact> visitedFacts ) {
      final Set<TextMention> list = new HashSet<>();
      if ( visitedFacts.contains( fact ) ) {
         return list;
      }
      visitedFacts.add( fact );

      list.addAll( fact.getProvenanceText() );

      for ( Fact ff : fact.getContainedFacts() ) {
         if ( ff != fact ) {
            list.addAll( getContainedProvenanceText( ff, visitedFacts ) );
         }
      }
      for ( Fact ff : fact.getProvenanceFacts() ) {
         if ( ff != fact ) {
            list.addAll( getContainedProvenanceText( ff, visitedFacts ) );
         }
      }
      // reset document level info
      if ( fact.getDocumentTitle() != null ) {
         for ( TextMention tm : list ) {
            tm.setDocumentTitle( fact.getDocumentTitle() );
            tm.setDocumentIdentifier( fact.getDocumentIdentifier() );
            tm.setDocumentType( fact.getDocumentType() );
            tm.setDocumentSection( fact.getDocumentSection() );
         }
      }
      return list;
   }

   public void copyIdInfo( final Fact sourceFact ) {
      setUri( sourceFact.getUri() );
      setLabel( sourceFact.getLabel() );
      setName( sourceFact.getName() );
      setIdentifier( sourceFact.getId() );
   }

   /**
    * Document information should not be lost as it is used for identification.
    *
    * @param sourceFact - any fact corresponding to the same document that has populated doc info.
    */
   public void copyDocInfo( final Fact sourceFact ) {
      if ( getPatientIdentifier() == null ) {
         setPatientIdentifier( sourceFact.getPatientIdentifier() );
      }
      if ( getDocumentIdentifier() == null ) {
         setDocumentIdentifier( sourceFact.getDocumentIdentifier() );
      }
      if ( getDocumentType() == null ) {
         setDocumentType( sourceFact.getDocumentType() );
      }
      if ( getDocumentTitle() == null && sourceFact.getDocumentTitle() != null ) {
         setDocumentTitle( sourceFact.getDocumentTitle() );
      }
      if ( getRecordedDate() == null ) {
         setRecordedDate( sourceFact.getRecordedDate() );
      }
   }


   /**
    * Fact creation is called from many places, often repeating code.
    * The autoFillDefaults() method allows a Fact to be populated based upon
    * minimum preset information.  Url should always be set explicitly.
    */
   public void autoFillDefaults() {
      if ( getType() == null ) {
         setType( createDefaultType() );
      }
      if ( getLabel() == null ) {
         setLabel( createDefaultLabel() );
      }
      if ( getName() == null ) {
         setName( createDefaultName() );
      }
      if ( getCategory() == null ) {
         setCategory( createDefaultCategory() );
      }
      if ( getId() == null ) {
         setIdentifier( createDefaultId() );
      }
   }

   /**
    * @return a Type created from the name of the class.  The suffix "Fact" is stripped if present.
    */
   protected String createDefaultType() {
      final String className = getClass().getSimpleName();
      if ( className.length() > 4 && className.endsWith( "Fact" ) ) {
         return className.substring( 0, className.length() - 4 );
      }
      return className;
   }

   protected String createDefaultLabel() {
      if ( getUri() != null ) {
         return getUri();
      }
      if ( getName() != null ) {
         return getName();
      }
      if ( getId() != null ) {
         return getId();
      }
      return "Unknown";
   }

   protected String createDefaultName() {
      if ( getUri() != null ) {
         return UriUtil.getExtension( getUri() );
      }
      if ( getLabel() != null ) {
         return getLabel().replace( ' ', '_' );
      }
      if ( getId() != null ) {
         return getId();
      }
      return "Fact";
   }

   protected String createDefaultCategory() {
      return null;
   }

   /**
    * @return identifier made from patient, doc, type, label and hashcode
    */
   protected String createDefaultId() {
      final String patientId = getPatientIdentifier();
      final String docId = getDocumentIdentifier();
      return (patientId == null ? "" : patientId + '_')
             + (docId == null ? "" : docId + '_')
             + getType() + '_'
             + getLabel().replace( ' ', '_' ) + '_'
             + Math.abs( hashCode() );
   }

   public String toString() {
      if (getName()!=null) return getName();
      return "name-is-null";   // differentiate between null object and null name
   }

   static private TextMention createTextMention( final Annotation annotation ) {
      final TextMention textMention = new TextMention();
      textMention.setText( annotation.getCoveredText() );
      textMention.setStart( annotation.getBegin() );
      textMention.setEnd( annotation.getEnd() );
      return textMention;
   }

   static private TextMention createTextMention( final String docId, final Annotation annotation ) {
      final TextMention textMention = new TextMention();
      textMention.setText( annotation.getCoveredText() );
      textMention.setStart( annotation.getBegin() );
      textMention.setEnd( annotation.getEnd() );
      textMention.setDocumentTitle( docId );
      return textMention;
   }

}
