package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.healthnlp.deepphe.util.FHIRUtils;

import java.util.*;

import static org.healthnlp.deepphe.util.FHIRConstants.*;

public class ConditionFact extends Fact implements BodySiteOwner, MethodOwner, InterpretationOwner, ValueOwner {

   static private final Object ID_NUM_LOCK = new Object();
   static private long _ID_NUM = 0;

   private FactList bodySite;
   private Set<String> relatedEvidenceIds;

   private Fact interpretation, method, value;

   public ConditionFact( final ConceptInstance conceptInstance ) {
      super( conceptInstance, FHIRConstants.CONDITION );
   }

   public ConditionFact( final ConceptInstance conceptInstance, final String type ) {
      super( conceptInstance, type );
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

   @Override
   public Fact getInterpretation() {
      return interpretation;
   }

   @Override
   public void setInterpretation( final Fact interpretation ) {
      this.interpretation = interpretation;
   }

   @Override
   public FactList getBodySite() {
      if ( bodySite == null ) {
         bodySite = new DefaultFactList();
         bodySite.addType( FHIRConstants.BODY_SITE );
      }
      return bodySite;
   }

   @Override
   public void setBodySite( final FactList bodySite ) {
      this.bodySite = bodySite;
   }

   @Override
   public Fact getMethod() {
      return method;
   }

   @Override
   public void setMethod( final Fact method ) {
      this.method = method;
   }

   @Override
   public Fact getValue() {
      return value;
   }

   @Override
   public void setValue( final Fact value ) {
      if ( value instanceof ValueFact ) {
         StringBuilder sb = new StringBuilder();
         for ( double d : ((ValueFact)value).getValues() ) {
            sb.append( d + "," );
         }
         Logger.getLogger( "ConditionFact" )
               .debug( "Adding " + value.getCategory()
                      + " " + value.getType()
                      + " " + value.getUri()
                      + " " + sb.toString()
                      + " to " + getCategory()
                      + " " + getType()
                      + " " + getUri() );
      }
      this.value = value;
   }

   @Override
   public List<Fact> getContainedFacts() {
      List<Fact> facts = new ArrayList<>();
      if ( method != null ) {
         addContainedFact( facts, method );
      }
      if ( interpretation != null ) {
         addContainedFact( facts, interpretation );
      }
      if ( value != null ) {
         addContainedFact( facts, value );
      }
      for ( Fact fact : getBodySite() ) {
         addContainedFact( facts, fact );
      }
      return facts;
   }

   public String getSummaryText() {
      return getLabel() + getValueSnippet() + getInterpretationSnippet() + getBodySiteSnippet() + getMethodSnippet();
   }

   /**
    * @param property -
    * @return a value for this fact for a given property
    */
   public Fact getValue( final String property ) {
      switch ( property ) {
         case HAS_INTERPRETATION:
            return getInterpretation();
         case HAS_NUM_VALUE:
            return getValue();
         case HAS_METHOD:
            return getMethod();
      }
      return this;
   }

   public void append( final Fact fact ) {
      if ( !ConditionFact.class.isInstance( fact ) ) {
         return;
      }
      super.append( fact );
      if ( fact instanceof InterpretationOwner ) {
         setOrAppendInterpretation( ((InterpretationOwner)fact).getInterpretation() );
      }
      if ( fact instanceof MethodOwner ) {
         setOrAppendMethod( ((MethodOwner)fact).getMethod() );
      }
      if ( fact instanceof ValueOwner ) {
         setOrAppendValue( ((ValueOwner)fact).getValue() );
      }
      appendBodySites( (ConditionFact)fact );
   }

   public void addRelatedEvidnceId( final String id ) {
      getRelatedEvidenceIds().add( id );
   }

   public Set<String> getRelatedEvidenceIds() {
      if ( relatedEvidenceIds == null ) {
         relatedEvidenceIds = new HashSet<>();
      }
      return relatedEvidenceIds;
   }

   public void setRelatedEvidenceIds( final Set<String> relatedEvidenceIds ) {
      this.relatedEvidenceIds = relatedEvidenceIds;
   }

   public Map<String, String> getProperties() {
      final Map<String, String> map = super.getProperties();
      if ( !getRelatedEvidenceIds().isEmpty() ) {
         map.put( FHIRUtils.EVIDENCE, getRelatedEvidenceIds().toString() );
      }
      return map;
   }

   public void setProperties( final Map<String, String> properties ) {
      super.setProperties( properties );
      String evidence = properties.get( FHIRUtils.EVIDENCE );
      if ( evidence == null ) {
         return;
      }
      if ( evidence.startsWith( "[" ) && evidence.endsWith( "]" ) ) {
         evidence = evidence.substring( 1, evidence.length() - 1 );
      }
      for ( String id : evidence.split( "," ) ) {
         addRelatedEvidnceId( id.trim() );
      }
   }

   /**
    * @return "Condition"
    */
   protected String createDefaultType() {
      return FHIRConstants.CONDITION;
   }

   protected String createDefaultCategory() {
      // Default to hasDiagnosis no matter what
      return FHIRConstants.HAS_DIAGNOSIS;
   }

//   /**
//    * This method is used early in the creation of the Fact so getRelatedFacts(relation) isn't able to be used here
//    * @return
//    */
//   private boolean observationHasThisCondition() {
//      final Map<String, Collection<ConceptInstance>> relatedCIs = new HashMap<>( getConceptInstance().getRelatedUris() );
//      for ( Map.Entry<String, Collection<ConceptInstance>> reverse : getConceptInstance().getReverseRelated()
//                                                                                         .entrySet() ) {
//         relatedCIs.computeIfAbsent( reverse.getKey(), n -> new ArrayList<>() ).addAll( reverse.getValue() );
//      }
//      if ( !relatedCIs.isEmpty() ) {
//         relatedCIs.keySet().forEach( System.out::println );
//         if ( relatedCIs.containsKey( FHIRConstants.HAS_DIAGNOSIS )
//              || relatedCIs.containsKey( RelationConstants.DISEASE_MAY_HAVE_FINDING )
//              || relatedCIs.containsKey( RelationConstants.DISEASE_HAS_FINDING ) ) {
//            return true;
//         }
//      }
//      return false; // Do not always force, forcing to always act as if there is an Observation in a relation to this Condition did too much
//   }
}
