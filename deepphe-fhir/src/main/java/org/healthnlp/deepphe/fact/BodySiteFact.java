package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.neo4j.graphdb.GraphDatabaseService;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.ctakes.cancer.uri.UriConstants.LYMPH_NODE;
import static org.healthnlp.deepphe.util.FHIRConstants.*;

public class BodySiteFact extends Fact {

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   static private final Logger LOGGER = Logger.getLogger( MethodHandles.lookup().lookupClass().getSimpleName() );

   private FactList modifiers;
   private Fact side;

   public BodySiteFact( final ConceptInstance conceptInstance ) {
      super( conceptInstance, FHIRConstants.BODY_SITE );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected long createUniqueIdNum() {
      synchronized ( ID_NUM_LOCK ) {
         _ID_NUM++;
         return _ID_NUM;
      }
   }

   public String getUri() {
      String uri = super.getUri();
      if ( uri.startsWith( "Left_" ) ) {
         return uri.substring( 5 );
      } else if ( uri.startsWith( "Right_" ) ) {
         return uri.substring( 6 );
      } else if ( uri.startsWith( "Bilateral_" ) ) {
         return uri.substring( 10 );
      }
      return uri;
   }

   // Kludge because a Fact can be created by drools that sets the label to be a uri
   public String getLabel() {
      String label = super.getLabel();
 	  if(label == null) {
 		  return getUri();
 	  }
 	  else if ( label.startsWith( "Left_" ) ) {
         return label.substring( 5 );
      } else if ( label.startsWith( "Right_" ) ) {
         return label.substring( 6 );
      } else if ( label.startsWith( "Bilateral_" ) ) {
         return label.substring( 10 );
      }
      return label;
   }

   /**
    * Note this checks more URIs than laterality so check laterality {@code isBodySide} first for performance
    *
    * @param modifier
    * @return
    */
   static public boolean isBodyModifer(final Fact modifier) {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      Collection<String> bodyModifierUris = SearchUtil.getBranchUris( graphDb, UriConstants.BODY_MODIFIER );
      LOGGER.debug("bodyModifierUris.size() = " + (bodyModifierUris==null? 0 : bodyModifierUris.size()) + " using UriConstants.BODY_MODIFIER");

      if (bodyModifierUris==null) {
         return false;
      }
      return UriUtil.containsUri(modifier.getUri(), bodyModifierUris);

   }

   static Collection<String> breastSiteUris = null;
   static Collection<String> lymphSiteUris = null;

   static public boolean hasBodySiteSomeBreast(Fact fact) {
      if (breastSiteUris==null) {
         final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
         breastSiteUris
               = SearchUtil.getBranchUris( graphDb, BREAST );
         if (breastSiteUris.contains("Organ") || breastSiteUris.contains("Anatomic_Structure_System_or_Substance")) {
            LOGGER.error("getBranchUris(UriConstants.BREAST) returned 'Organ' or 'Anatomic_Structure_System_or_Substance'");
            breastSiteUris = new ArrayList<>();
         }
         // Ensure we have expected values regardless of what getBranchUris returns until get update and verify it
         if (!breastSiteUris.contains("Nipple")) breastSiteUris.add("Nipple");
         if (!breastSiteUris.contains("Breast")) breastSiteUris.add("Breast");
         if (!breastSiteUris.contains("Female_Breast")) breastSiteUris.add("Female_Breast");
         if (!breastSiteUris.contains("Male_Breast")) breastSiteUris.add("Male_Breast");
         if (!breastSiteUris.contains("Left_Breast")) breastSiteUris.add("Left_Breast");
         if (!breastSiteUris.contains("Right_Breast")) breastSiteUris.add("Right_Breast");
      }
      if (fact instanceof BodySiteOwner) {
         BodySiteOwner bodySiteOwner = (BodySiteOwner) fact;
         for (Fact cancerBodySiteFact : bodySiteOwner.getBodySite()) {
            if (breastSiteUris.contains(cancerBodySiteFact.getLabel())) {
               LOGGER.debug("BodySiteOwner Fact " + fact + " hasBodySiteSomeBreast when checking cancerBodySiteFact.getLabel() " + cancerBodySiteFact.getLabel());
               return true;
            }
         }

      }
      if (HAS_BODY_SITE.equals(fact.getCategory())) {
         if (breastSiteUris.contains(fact.getLabel())) {
            return true;
         }
         if (fact.getLabel()!=null && fact.getLabel().contains("Breast")) {
            return true;
         }
      }
      if (fact.getRelatedFacts(HAS_BODY_SITE)==null) return false;
      for (Fact f: fact.getRelatedFacts(HAS_BODY_SITE)) {
         if (breastSiteUris.contains(f.getName())) {
            LOGGER.debug("BodySiteOwner Fact " + fact + " hasBodySiteSomeBreast when checking f.getName() " + f.getName());
            return true;
         }
      }
      return false;
   }

   static public boolean hasBodySiteSomeLymph(Fact fact) {
      if (lymphSiteUris==null) {
         final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
         lymphSiteUris = SearchUtil.getBranchUris( graphDb, LYMPH_NODE );
         if ( lymphSiteUris.contains( "Organ" ) || lymphSiteUris.contains( UriConstants.ANATOMY ) ) {
            LOGGER.error("getBranchUris(UriConstants.LYMPH_NODE) returned 'Organ' or 'Anatomic_Structure_System_or_Substance'");
            lymphSiteUris = new ArrayList<>();
         }
         // Ensure we have expected values regardless of what getBranchUris returns until get updated ontology and verify it
         if (!lymphSiteUris.contains("Lymphatic_Vessel")) lymphSiteUris.add("Lymphatic_Vessel");
         if (!lymphSiteUris.contains("Lymph_Node")) lymphSiteUris.add("Lymph_Node");
         if (!lymphSiteUris.contains("Axillary_Lymph_Node")) lymphSiteUris.add("Axillary_Lymph_Node");
         if (!lymphSiteUris.contains("Axillary_Lymph_Node_Level_II")) lymphSiteUris.add("Axillary_Lymph_Node_Level_II");
         if (!lymphSiteUris.contains("Brachial_Lymph_Node")) lymphSiteUris.add("Brachial_Lymph_Node");
         if (!lymphSiteUris.contains("Axillary_Lymph_Node_Level_I")) lymphSiteUris.add("Axillary_Lymph_Node_Level_I");
         if (!lymphSiteUris.contains("Pectoral_Lymph_Node")) lymphSiteUris.add("Pectoral_Lymph_Node");
         if (!lymphSiteUris.contains("Axillary_Lymph_Node_Level_III")) lymphSiteUris.add("Axillary_Lymph_Node_Level_III");
      }
      if (HAS_BODY_SITE.equals(fact.getCategory())) {
         if (lymphSiteUris.contains(fact.getLabel())) {
            return true;
         }
      }
      if (fact.getRelatedFacts(HAS_BODY_SITE)==null) return false;
      for (Fact f: fact.getRelatedFacts(HAS_BODY_SITE)) {
         if (lymphSiteUris.contains(f.getName())) {
            LOGGER.debug("Fact " + fact + " hasBodySiteSomeLymph when checking f.getName() " + f.getName());
            return true;
         }
      }
      return false;
   }



   static public boolean isBodySide( final Fact modifier ) {
      return UriUtil.containsUri( modifier.getUri(), UriConstants.LEFT, UriConstants.RIGHT, UriConstants.BILATERAL );
   }

   public FactList getModifiers() {
      if ( modifiers == null ) {
         modifiers = new DefaultFactList();
      }
      return modifiers;
   }

   public void setModifiers( final FactList modifiers ) {
      this.modifiers = modifiers;
   }

   public void addModifier( final Fact fact ) {
      getModifiers().add( fact );

      fact.setCategory( FHIRConstants.HAS_BODY_MODIFIER );
      if ( isBodySide( fact ) || fact.getType().equals(FHIRConstants.LATERALITY)) {
         fact.setType( FHIRConstants.LATERALITY );
         side = fact;
      } else {
         fact.setType( FHIRConstants.BODY_MODIFIER );
      }
   }

   private Fact getModifier( final String uri ) {
      return getModifiers().stream()
                           .filter( f -> f.getUri()
                                          .equals( uri ) )
                           .findAny()
                           .orElse( null );
   }

   public List<Fact> getContainedFacts() {
      final List<Fact> facts = new ArrayList<>();
      getModifiers().forEach( f -> addContainedFact( facts, f ) );
      return facts;
   }

   public String getFullName() {
      return ( side == null ? "" : side.getName() + '_' ) + getName();
   }

   static public void makeQuadrantsBodyModifier(List<Fact> facts) {
      for (Fact f: facts) {
         if (f.getCategory()==null || f.getCategory().equals(HAS_BODY_SITE)) {
            if ( f.getAncestors().contains( UriConstants.BODY_MODIFIER ) ) {
               f.setCategory(HAS_BODY_MODIFIER);
               f.setType( BODY_MODIFIER );
            }
         }
      }
   }

   /**
    * @param property -
    * @return a value for this fact for a given property
    */
   public Fact getValue( final String property ) {
      switch ( property ) {
         case HAS_LATERALITY:
            return getBodySide();
         case HAS_QUADRANT:
            return getQuadrant();
         case HAS_CLOCKFACE:
            return getClockfacePosition();
      }
      return this;
   }

   public String getSummaryText() {
      return getLabel()
            + " | modifier: "
            + getModifiers().stream()
                            .map( Fact::getName )
                            .collect( Collectors.joining( " | modifier: " ) );
   }

   public Fact getClockfacePosition() {
      return getModifier( UriConstants.CLOCKFACE );
   }

   public Fact getQuadrant() {
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Collection<String> breastParts = SearchUtil.getBranchUris( graphDb, UriConstants.BREAST_PART );
      return getModifiers().stream()
                           .filter( f -> f.getUri()
                                          .contains( "Quadrant" ) )
                           .filter( f -> breastParts.contains( f.getUri() ) )
                           .findAny()
                           .orElse( null );
   }

   public Fact getBodySide() {
      return side;
   }

   // Used by Drools rules
   public void setBodySide( final Fact fact ) {
      side = fact;
      if ( fact != null ) {
         addModifier( fact );
      }
   }

   public boolean equivalent( final BodySiteFact fact ) {
      if ( !super.equivalent( fact ) ) {
         return false;
      }
      final Fact bodyside = getBodySide();
      final Fact otherSide = fact.getBodySide();
      if ( bodyside == null && otherSide == null ) {
         return true;
      }
      return bodyside != null && bodyside.equivalent( otherSide );
   }

   public void append( final Fact fact ) {
      if ( fact instanceof BodySiteFact && equivalent( (BodySiteFact)fact ) ) {
         super.append( fact );
         final List<Fact> myModifiers = getModifiers();
         final BodySiteFact otherBodySite = (BodySiteFact)fact;
         otherBodySite.getModifiers()
                      .stream()
                      .filter( m -> FactHelper.isMissingFact( myModifiers, m ) )
                      .forEach( this::addModifier );
      }
   }

   public String getInfo() {
      String info = super.getInfo();
      return info.replace( "\n", "|" ) + "side: " + side + "\n";
   }

   protected String createDefaultCategory() {
      return FHIRConstants.HAS_BODY_SITE;
   }


   /**
    * @return "BodySite"
    */
   protected String createDefaultType() {
      return FHIRConstants.BODY_SITE;
   }

}

