package org.healthnlp.deepphe.summary;

import org.apache.ctakes.cancer.concept.instance.IdOwner;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.fact.Appendable;
import org.healthnlp.deepphe.fact.*;
import org.healthnlp.deepphe.util.FHIRConstants;

import java.lang.invoke.MethodHandles;
import java.util.*;


public abstract class Summary
      implements IdOwner, DpheElement, PropertyOwner, Appendable<Summary> {

   static private final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());


   private final long _unique_id_num;

   private String annotationType = FHIRConstants.ANNOTATION_TYPE_DOCUMENT;
   protected Map<String, FactList> content;
   private Map<String, String> properties;
   protected String resourceIdentifier;
   protected String conceptURI;
   private String _patientId;


   public Summary(final String id) {
      _unique_id_num = createUniqueIdNum();
      if (id == null)
         setResourceIdentifier("" + Math.abs(hashCode()));
      else
         setResourceIdentifier(id);
   }

   /**
    * ANY summary can have a patient_site_etc id in common with another.
    * For instance, patientA has two current tumors on the left breast.  One is 1mm upper inner, one 5mm lower outer.
    */
   abstract protected long createUniqueIdNum();

   /**
    * ANY summary can have a patient_site_etc id in common with another.
    * For instance, patientA has two current tumors on the left breast.  One is 1mm upper inner, one 5mm lower outer.
    *
    * @return an index number unique to this summary
    */
   public long getUniqueIdNum() {
      return _unique_id_num;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public String getId() {
      final String n = getClass().getSimpleName();
      // Drools or Java ? can already add simpleName. This check helps to avoid duplicates in id.
      if (resourceIdentifier.startsWith(n))
         return resourceIdentifier;
      else
         return getClass().getSimpleName() + "-" + resourceIdentifier;
   }

   final public String getObjectId() {
      return resourceIdentifier;
   }

   final public String getPatientIdentifier() {
      return _patientId;
   }

   public void setPatientIdentifier(final String patientId) {
      _patientId = patientId;
   }

   /**
    * @return some default uri for the summary type
    */
   abstract protected String getDefaultUri();

   final public Map<String, FactList> getContent() {
      if (content == null) {
         content = new HashMap<>();
      }
      return content;
   }

   /*
    * Used after Drools
    */
   abstract public void cleanSummary();

   @Override
   final public Map<String, String> getProperties() {
      if (properties == null) {
         properties = new HashMap<>();
      }
      return properties;
   }

   @Override
   final public void setProperties(final Map<String, String> properties) {
      this.properties = properties;
   }


   /**
    * get facts of a given category
    *
    * @param category ?
    * @return ?
    */
   final public FactList getFacts(final String category) {
      final FactList list = getContent().get(category);
      return list == null ? DefaultFactList.emptyList() : list;
   }

   /**
    * get facts of a given category
    *
    * @param category ?
    * @return ?
    */
   final protected FactList getOrCreateFacts(final String category) {
      return getContent().computeIfAbsent(category, c -> new DefaultFactList(c));
   }

   /**
    * clear list for drools testing
    *
    * @param category ?
    */
   final public void clearFactList(final String category) {
      final FactList list = getContent().get(category);
      if (list != null) {
         list.clear();
      }
   }


   final public void setResourceIdentifier(String resourceIdentifier) {
      this.resourceIdentifier = resourceIdentifier;
   }

   /**
    * cannot deprecate since it's a bean
    */
   final public String getResourceIdentifier() {
      return getId();
   }

   public String getDisplayText() {
      return getClass().getSimpleName() + " (" + resourceIdentifier + ")";
   }


   protected String getClassDash() {
      return getClass().getSimpleName() + "-";
   }

   /**
    * @return fact categories
    */
   public Set<String> getFactCategories() {
      return getContent().keySet();
   }

   public boolean addFact(final Fact fact) {
      return addFact(fact.getCategory(), fact);
   }

   public boolean addFact(final String category, final Fact fact) {
      final FactList list = getContent().computeIfAbsent(category, c -> new DefaultFactList(c));
      // add IDs from this container and documents
      addIdentifiersToFact(fact, category);
      fact.getContainedFacts().forEach(f -> addIdentifiersToFact(f, null));
      addFact(fact, list);
      return true;
   }

   /**
    * add fact to a list
    *
    * @param fact -
    * @param list -
    */
   private void addFact(final Fact fact, final FactList list) {
      // Drools rules need to be able to add duplicate facts s.a. Breast, Left and Breast, Right
      list.add(fact);
   }


   public Fact getFactByCategoryID(final String category, final String identifier) {
      return getFacts(category).stream()
              .filter(f -> f.getId()
                      .equals(identifier))
              .findAny()
              .orElse(null);
   }

   public String getSummaryType() {
      return getClass().getSimpleName();
   }

   // like getSummaryText, but just the facts not the text
   public Map<String, FactList> getSummaryFacts() {
      Map<String, FactList> indexedFacts = new HashMap<>();

      Set<String> duplCategory = new HashSet<>();
      for (String category : getFactCategories()) {
         FactList facts = getFacts(category);
         FactList noDupes = new DefaultFactList(category);
         duplCategory.clear();
         for (Fact c : facts) {
            if (duplCategory.add(c.getSummaryText())) {
               noDupes.add(c);
            }
         }
         indexedFacts.put(category, noDupes);
      }
      return indexedFacts;
   }

   public String getSummaryText() {
      final StringBuilder sb = new StringBuilder();
      sb.append( getDisplayText() )
        .append( ":\n" );
      Set<String> duplCategory = new HashSet<String>();
      for ( String category : getFactCategories() ) {
         sb.append( "\t" )
           .append( getPropertyDisplayLabel( category ) )
           .append( ":\n" );
         duplCategory.clear();
         for ( Fact c : getFacts( category ) ) {
        	 if(duplCategory.add(c.getSummaryText())) {
	            sb.append( "\t\t" )
	              .append( c.getSummaryText() )
	              .append( "\n" );
        	 }
         }
      }
      
      duplCategory.clear();
      duplCategory = null;
      return sb.toString();
   }

   public String getPropertyDisplayLabel( final String text ) {
		if ( text == null ) {
         return "Unknown";
      }
      String display = text;
		if ( display.toLowerCase().startsWith( "has" ) ) {
         display = text.substring( 3 );
      }
		// insert space into camel back
		return display.replaceAll("([a-z])([A-Z])","$1 $2" );
	}



   public String getConceptURI() {
      return conceptURI != null ? conceptURI : getDefaultUri();
   }

   public void setConceptURI( String conceptURI ) {
      this.conceptURI = conceptURI;
   }


   /**
    * do a very simple append of data
    *
    * @param summary
    */
   @Override
   public void append( final Summary summary ) {
      for ( String category : summary.getFactCategories() ) {
         summary.getFacts( category ).forEach( f -> addFact( category, f ) );
      }
   }

   public String getAnnotationType() {
      return annotationType;
   }

   public void setAnnotationType( final String annotationType ) {
      this.annotationType = annotationType;
   }

   protected void addIdentifiersToFact( final Fact fact, final String category ) {
      if ( category != null ) {
         fact.setCategory( category );
      }
      fact.addContainerIdentifier( getId() );
      if (TumorSummary.class.getSimpleName().equals(fact.getSummaryType())) {
         // already is assigned to a tumor summary, don't reassign. Drools is driven by tumor summary
      } else {
         fact.setSummaryType(getSummaryType());
         fact.setSummaryId( getId() );
      }
      final String episodeType = getProperty( FHIRConstants.HAS_EPISODE_TYPE );
      if ( episodeType != null ) {
         fact.addProperty( FHIRConstants.HAS_EPISODE_TYPE, episodeType );
      }
   }


   /**
    * return all facts that are contained within this fact
    *
    * @return -
    */
   public List<Fact> getContainedFacts() {
      final ArrayList<Fact> list = new ArrayList<>();
      for ( FactList factList : getContent().values() ) {
         for ( Fact fact : factList ) {
            list.add( fact );
            // recurse into facts
            list.addAll( fact.getContainedFacts() );
         }
      }
      return list;
   }


   public static String createLocationIdentifier( final ConditionFact tumor, final BodySiteFact site ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( tumor.getName() )
        .append( "_" );
      if ( site.getBodySide() != null ) {
         sb.append( site.getBodySide().getName() )
           .append( "_" );
      }
      sb.append( site.getName() );
      final List<String> modifiers = new ArrayList<>();
      for ( Fact mod : site.getModifiers() ) {
         if ( !BodySiteFact.isBodySide( mod ) ) {
            modifiers.add( mod.getName() );
         }
      }
      modifiers.sort( Collections.reverseOrder() );
      for ( String modifier : modifiers ) {
         sb.append( "_" )
           .append( modifier );
      }
      return sb.toString();
   }

   public String getTemporality() {
      for ( Fact fact : getContainedFacts() ) {
         for ( Map.Entry<String, String> property : fact.getProperties().entrySet() ) {
            if ( property.getKey().equals( FHIRConstants.HAS_TEMPORALITY ) && !property.getValue().equalsIgnoreCase( "Before" ) ) {
               return "Current";
            }
         }
      }
      return "Historic";
   }



   @Override
   public String toString() {
      return UriUtil.getExtension( getConceptURI() ) + " : " + content;
   }

}
