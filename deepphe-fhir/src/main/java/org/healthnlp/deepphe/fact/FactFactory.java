package org.healthnlp.deepphe.fact;

import org.apache.ctakes.cancer.concept.instance.ConceptInstance;
import org.apache.ctakes.cancer.summary.CiSummary;
import org.apache.ctakes.cancer.uri.UriConstants;
import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.note.NoteSpecs;
import org.apache.ctakes.core.semantic.SemanticGroup;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.RelationConstants;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.healthnlp.deepphe.util.FHIRConstants;
import org.neo4j.graphdb.GraphDatabaseService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.util.FHIRConstants.*;


/**
 * create different facts
 *
 * @author tseytlin
 */
public class FactFactory {

   static private final Logger LOGGER = Logger.getLogger( "FactFactory" );

   static private final Predicate<ConceptInstance> wantedForFact
         = ci -> !UriConstants.EVENT.equals( ci.getUri() ) && !UriConstants.UNKNOWN.equals( ci.getUri() )
                 && !ci.isNegated() && !ci.isUncertain() && !ci.isGeneric() && !ci.isConditional()
                 && (ci.getRelated().size() + ci.getReverseRelated().size()) != 0;



   static public Map<ConceptInstance, Fact> createCiFacts2( final Map<String, Collection<ConceptInstance>> ciMap,
                                                            final NoteSpecs noteSpecs ) {
      final String docType = noteSpecs.getDocumentType();

      // Create a Fact for each concept instance.
      final Map<ConceptInstance, Fact> ciFactMap
            = ciMap.values()
                   .stream()
                   .flatMap( Collection::stream )
                   .distinct()
                   .collect( Collectors.toMap( Function.identity(), FactFactory::createFact ) );

      // Go through concept instance relations and join / populate facts
      for ( Map.Entry<ConceptInstance, Fact> ciFact : ciFactMap.entrySet() ) {
         final ConceptInstance conceptInstance = ciFact.getKey();
         final Fact fact = ciFact.getValue();
         // set basic doc info for fact
         final Date docDate = conceptInstance.getDocumentDate( null );
         fact.setRecordedDate( docDate );
         fact.setDocumentType( docType );
         fact.autoFillDefaults();
      }
      addRelatedFacts( ciFactMap );
      return ciFactMap;
   }

   static private void addRelatedFacts( final Map<ConceptInstance, Fact> ciFactMap ) {
      for ( Map.Entry<ConceptInstance,Fact> ciFact : ciFactMap.entrySet() ) {
         for ( Map.Entry<String, Collection<ConceptInstance>> ciRelation : ciFact.getKey().getRelated().entrySet() ) {
            final String relationName = ciRelation.getKey();
            if ( relationName.equals( RelationConstants.DISEASE_HAS_NORMAL_TISSUE_ORIGIN )
                 || relationName.equals( RelationConstants.DISEASE_HAS_NORMAL_CELL_ORIGIN ) ) {
               // Facts get overloaded by anatomy concepts and can't tell the difference between e.g. tissues and sites
               continue;
            }
            final Collection<Fact> relatedFacts = ciRelation.getValue()
                                                            .stream()
                                                            .map( ciFactMap::get )
                                                            .filter( Objects::nonNull )
                                                            .sorted( FACT_SORTER )
                                                            .collect( Collectors.toList() );
            for ( Fact relatedFact : relatedFacts ) {
               addRelation( ciFact.getValue(), relationName, relatedFact );
            }
         }
      }
   }

   static public final FactSorter FACT_SORTER = new FactSorter();

   static private class FactSorter implements Comparator<Fact> {
      public int compare( final Fact fact1, final Fact fact2 ) {
         final String uri1 = fact1.getUri();
         final String uri2 = fact2.getUri();
         final int uriDiff = uri1.compareTo( uri2 );
         if ( uriDiff != 0 ) {
            return uriDiff;
         }
         if ( fact1.getConceptInstance() == null || fact2.getConceptInstance() == null ) {
            return fact1.getType().compareTo( fact2.getType() );
         }
         int span1 = fact1.getConceptInstance().getAnnotations().stream().map( a -> a.getEnd() * 10 + a.getBegin() )
                          .mapToInt( Integer::valueOf ).sum();
         int span2 = fact2.getConceptInstance().getAnnotations().stream().map( a -> a.getEnd() * 10 + a.getBegin() )
                          .mapToInt( Integer::valueOf ).sum();
         int diff = span1 - span2;
         if ( diff != 0 ) {
            return diff;
         }
         final String text1 = fact1.getConceptInstance().toText();
         final String text2 = fact2.getConceptInstance().toText();
         int textDiff = text1.compareTo( text2 );
         if ( textDiff != 0 ) {
            return textDiff;
         }
         return fact1.getConceptInstance().getCoveredText().compareTo( fact2.getConceptInstance().getCoveredText() );
      }
   }


   static private void addRelation( final Fact fact, final String relationName, final Fact relatedFact ) {
      if ( relationName.equals( RelationConstants.HAS_LATERALITY ) ) {
         relatedFact.setType( FHIRConstants.LATERALITY );
         relatedFact.setCategory( FHIRConstants.HAS_BODY_MODIFIER );
      }
      if ( relatedFact.getCategory() == null ) {
         relatedFact.setCategory( relationName );
      }
      if ( fact.getCategory() == null ) {
         final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
         if ( UriConstants.getTumorUris().contains( fact.getUri() ) ) {
            fact.setCategory( RelationConstants.HAS_TUMOR_TYPE );
         } else if ( SearchUtil.getBranchUris( graphDb, UriConstants.DRUG ).contains( fact.getUri() ) ) {
            fact.setCategory( RelationConstants.HAS_TREATMENT );
         }
      }
      if ( relatedFact instanceof BodySiteFact ) {
         if ( fact instanceof BodySiteOwner ) {
            ( (BodySiteOwner)fact ).addBodySite( (BodySiteFact)relatedFact );
         } else if ( fact instanceof BodySiteFact ) {
            ( (BodySiteFact)fact ).addModifier( relatedFact );
         } else {
            LOGGER.error( "Relation " + relationName + " has BodySiteFact target but source "
                                + fact.getClass()
                  .getSimpleName()
                  + " " + fact.getConceptInstance().getSemanticGroup()
                  + " " + fact.getConceptInstance().getCoveredText()
                  + " " + fact.getUri()
                  + " is not a BodySiteFact or BodySiteOwner" );
         }
      } else if ( relatedFact instanceof ValueFact ) {
         if ( fact instanceof ValueOwner ) {
            ( (ValueOwner)fact ).setValue( relatedFact );
         } else {
            LOGGER.error( "Relation " + relationName + " has ValueFact target but source "
                                + fact.getClass()
                                      .getSimpleName()
                          + " " + fact.getConceptInstance().getSemanticGroup()
                          + " " + fact.getConceptInstance().getCoveredText()
                          + " " + fact.getUri()
                          + " is not a ValueOwner" );
         }
      } else if ( relatedFact instanceof ProcedureFact ) {
         if ( fact instanceof MethodOwner ) {
            ( (MethodOwner)fact ).setMethod( relatedFact );
         } else {
            LOGGER.error( "Relation " + relationName + " has ProcedureFact target but source "
                                + fact.getClass()
                                      .getSimpleName()
                          + " " + fact.getConceptInstance().getSemanticGroup()
                          + " " + fact.getConceptInstance().getCoveredText()
                          + " " + fact.getUri()
                          + " is not a MethodOwner" );
         }
      } else if ( fact instanceof BodySiteFact ) {
         ( (BodySiteFact)fact ).addModifier( relatedFact );
      }
      fact.addRelatedFact( relationName, relatedFact );
   }


   static public Fact createFact( final ConceptInstance conceptInstance ) {
      final Fact fact = createSemanticFact( conceptInstance );
      initializeFact( fact, conceptInstance );
      return fact;
   }

   static private void initializeFact( final Fact fact, final ConceptInstance conceptInstance ) {
      fact.setAncestors( getAncestors( fact.getUri() ) );
   }

   static private Collection<String> getAncestors( final String uri ) {
      return Neo4jOntologyConceptUtil.getRootUris( uri ).stream()
                                     .map( UriUtil::getExtension )
                                     .filter( n -> !n.equalsIgnoreCase( "Thing" ) )
                                     .collect( Collectors.toList() );
   }

   static private Fact createSemanticFact( final ConceptInstance conceptInstance ) {
      if ( conceptInstance == null ) {
         return new Fact( null );
      }
      final String groupName = conceptInstance.getSemanticGroup();
      final SemanticGroup group = SemanticGroup.getGroup( groupName );
      switch ( group ) {
         case DRUG:
            return new Fact( conceptInstance, FHIRConstants.MEDICATION );
         case DISORDER:
            return new ConditionFact( conceptInstance, FHIRConstants.DIAGNOSIS );
         case FINDING: {
            final Fact tnmFact = createTnmFact( conceptInstance );
            if ( tnmFact != null ) {
               return tnmFact;
            }
            final Fact receptorStatusFact = createReceptorStatusFact( conceptInstance );
            if ( receptorStatusFact != null ) {
               return receptorStatusFact;
            }
            return new ObservationFact( conceptInstance, FHIRConstants.FINDING );
         }
         case PROCEDURE:
            return new ProcedureFact( conceptInstance );
         case ANATOMY: {
            return new BodySiteFact( conceptInstance );
         }
         case CLINICAL_ATTRIBUTE:
            return new ObservationFact( conceptInstance );
         case DEVICE:
            return new Fact( conceptInstance, FHIRConstants.DEVICE );
         case LAB: {
            final Fact erStatusFact = createStatusFact(conceptInstance, FHIRConstants.HAS_ER_STATUS);
            if (erStatusFact != null) {
               return erStatusFact;
            }
            final Fact prStatusFact = createStatusFact(conceptInstance, FHIRConstants.HAS_PR_STATUS);
            if (prStatusFact != null) {
               return prStatusFact;
            }
            final Fact her2StatusFact = createStatusFact(conceptInstance, FHIRConstants.HAS_HER2_STATUS);
            if (her2StatusFact != null) {
               return her2StatusFact;
            }
            return new ObservationFact(conceptInstance, QUANTITY);
         }
         case PHENOMENON:
            return new Fact( conceptInstance );
         case SUBJECT:
            return new Fact( conceptInstance );
         case TITLE:
            return new Fact( conceptInstance );
         case EVENT:
            return new Fact( conceptInstance );
         case ENTITY:
            return new Fact( conceptInstance );
         case TIME:
            return new Fact( conceptInstance );
         case MODIFIER:
            return new Fact( conceptInstance );
         case LAB_MODIFIER: {
            final Fact sizeFact = createSizeFact( conceptInstance );
            if ( sizeFact != null ) {
               return sizeFact;
            }
            return new Fact( conceptInstance );
         }
         case UNKNOWN: {
            final Fact erStatusFact = createStatusFact( conceptInstance, FHIRConstants.HAS_ER_STATUS );
            if ( erStatusFact != null ) {
               return erStatusFact;
            }
            final Fact prStatusFact = createStatusFact( conceptInstance, FHIRConstants.HAS_PR_STATUS );
            if ( prStatusFact != null ) {
               return prStatusFact;
            }
            final Fact her2StatusFact = createStatusFact( conceptInstance, FHIRConstants.HAS_HER2_STATUS );
            if ( her2StatusFact != null ) {
               return her2StatusFact;
            }
            return new Fact( conceptInstance );
         }
      }
      return new Fact( conceptInstance );
   }

   static public Fact createTypeFact( final String type ) {
      switch ( type ) {
         case BODY_SITE:
            return new BodySiteFact( null );
         case OBSERVATION:
            return new ObservationFact( null );
         case CONDITION:
            return new ConditionFact( null );
         case PROCEDURE:
            return new ProcedureFact( null );
         case QUANTITY:
            return new ValueFact( null );
      }
      return new Fact( null, type );
   }

   static private Fact createSemanticFact( final String uri, final ConceptInstance conceptInstance ) {
      final SemanticGroup group = Neo4jOntologyConceptUtil.getSemanticGroup( uri );
      switch ( group ) {
         case DRUG:
            return new Fact( conceptInstance, FHIRConstants.MEDICATION );
         case DISORDER:
            return new ConditionFact( conceptInstance, FHIRConstants.DIAGNOSIS );
         case FINDING:
            return new ObservationFact( conceptInstance, FHIRConstants.FINDING );
         case PROCEDURE:
            return new ProcedureFact( conceptInstance );
         case ANATOMY:
            return new BodySiteFact( conceptInstance );
         case CLINICAL_ATTRIBUTE:
            return new ObservationFact( conceptInstance );
         case DEVICE:
            return new Fact( conceptInstance, FHIRConstants.DEVICE );
         case LAB:
            return new ObservationFact( conceptInstance, QUANTITY );
         case PHENOMENON:
            return new Fact( conceptInstance );
         case SUBJECT:
            return new Fact( conceptInstance );
         case TITLE:
            return new Fact( conceptInstance );
         case EVENT:
            return new Fact( conceptInstance );
         case ENTITY:
            return new Fact( conceptInstance );
         case TIME:
            return new Fact( conceptInstance );
         case MODIFIER:
            return new Fact( conceptInstance );
         case LAB_MODIFIER:
            return new Fact( conceptInstance );
         case UNKNOWN:
            return new Fact( conceptInstance );
      }
      return new Fact( conceptInstance );
   }


   static private String getReceptorUri( final String uri ) {
      if ( uri.contains( "Estrogen" ) ) {
         return UriConstants.ER_STATUS;
      } else if ( uri.contains( "Progesterone" ) ) {
         return UriConstants.PR_STATUS;
      } else if ( uri.contains( "HER2" ) ) {
         return UriConstants.HER2_STATUS;
      }
      return UriConstants.RECEPTOR_STATUS;
   }

   static private Fact createTnmFact( final ConceptInstance conceptInstance ) {
      final String uri = conceptInstance.getUri();
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Collection<String> tnmBranch = SearchUtil.getBranchUris( graphDb, UriConstants.TNM );
      if ( !tnmBranch.contains( uri ) ) {
         return null;
      }
      final TNMFact fact = new TNMFact( conceptInstance );
      final Fact prefixFact = createTnmPrefixFact( conceptInstance );
      final char letter = getTnmLetter( conceptInstance, prefixFact != null );
      if ( letter == ' ' ) {
         return null;
      }
      if ( prefixFact == null ) {
         switch ( letter ) {
            case 'T': {
               fact.setCategory( HAS_CLINICAL_T_CLASSIFICATION );
               break;
            }
            case 'N': {
               fact.setCategory( HAS_CLINICAL_N_CLASSIFICATION );
               break;
            }
            case 'M': {
               fact.setCategory( HAS_CLINICAL_M_CLASSIFICATION );
               break;
            }
         }
      } else {
         fact.setPrefix( prefixFact );
         if (prefixFact.getUri().equals(UriConstants.P_TNM)) {
            switch ( letter ) {
               case 'T': {
                  fact.setCategory( HAS_PATHOLOGIC_T_CLASSIFICATION );
                  break;
               }
               case 'N': {
                  fact.setCategory( HAS_PATHOLOGIC_N_CLASSIFICATION );
                  break;
               }
               case 'M': {
                  fact.setCategory( HAS_PATHOLOGIC_M_CLASSIFICATION );
                  break;
               }
            }
         } else if (prefixFact.getUri().equals(UriConstants.C_TNM)) {
            switch ( letter ) {
               case 'T': {
                  fact.setCategory( HAS_CLINICAL_T_CLASSIFICATION );
                  break;
               }
               case 'N': {
                  fact.setCategory( HAS_CLINICAL_N_CLASSIFICATION );
                  break;
               }
               case 'M': {
                  fact.setCategory( HAS_CLINICAL_M_CLASSIFICATION );
                  break;
               }
            }
         } else {
            LOGGER.warn("prefixFact.getUri()  " + prefixFact.getUri() + "  fact.getPrefix() " + fact.getPrefix());
         }
      }
      return fact;
   }

   static private char getTnmLetter( final ConceptInstance conceptInstance, final boolean hasPrefix ) {
      final Predicate<Character> isTnm = c -> c == 'T' || c == 'N' || c == 'M';
      final int letterIndex = hasPrefix ? 1 : 0;
      return conceptInstance.getAnnotations()
                            .stream()
                            .map( Annotation::getCoveredText )
                            .filter( t -> t.length() > letterIndex )
                            .map( String::toUpperCase )
                            .map( t -> t.charAt( letterIndex ) )
                            .filter( isTnm )
                            .findAny()
                            .orElse( ' ' );
   }


   static private Fact createTnmPrefixFact( final ConceptInstance conceptInstance ) {
      final String uri = conceptInstance.getUri();
      boolean isPathologic = uri.contains( "Pathologic" ) || uri.startsWith( "p" );
      boolean isClinical = uri.contains( "Clinical" ) || uri.startsWith( "c" );
      if ( !isPathologic && !isClinical ) {
         isPathologic = conceptInstance.getAnnotations()
                                       .stream()
                                       .map( Annotation::getCoveredText )
                                       .anyMatch( t -> t.startsWith( "p" ) );
      }
      if ( !isPathologic ) {
         isClinical = conceptInstance.getAnnotations()
                                     .stream()
                                     .map( Annotation::getCoveredText )
                                     .anyMatch( t -> t.startsWith( "c" ) );
      }
      if ( !isPathologic && !isClinical ) {
         return null;
      }
      final Fact fact = new ObservationFact( conceptInstance, FHIRConstants.TNM_MODIFIER );
      fact.setUri( isPathologic ? UriConstants.P_TNM : UriConstants.C_TNM );
      fact.setCategory( FHIRConstants.HAS_TNM_PREFIX );
      fact.autoFillDefaults();
      LOGGER.debug( "FactFactory.TnmPrefixFact " + fact.getUri() + " " + fact.getCategory() );
      return fact;
   }

   /**
    *
    * @param conceptInstance
    * @param category parent of what we are looking for. If we want the statuses (_Negative, _Positive, and any
    *                 others) that under FHIRConstants.HAS_HER2_STATUS, use FHIRConstants.HAS_HER2_STATUS
    * @return
    */
   static private Fact createStatusFact(final ConceptInstance conceptInstance,  String category) {
      final String uri = conceptInstance.getUri();
      final String receptorUri = getReceptorUri( uri );
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Collection<String> branch = SearchUtil.getBranchUris( graphDb, receptorUri );
      // branch includes receptorUri, but we only wanted the children of receptorUri,
      // so skip making it a fact with HAS_INTERPRETATION if it is exactly receptorUri.
      // For example, want HER2_Neu_Status to be ignored by Drools, so don't want to
      // assign category HAS_INTERPRETATION (or want it just to not be a Fact),
      // but we want HER2_Neu_Negative to have category HAS_INTERPRETATION
      if ( !branch.contains(uri) || (receptorUri!=null && receptorUri.equals(uri)) ) {
         return null;
      }
      final ObservationFact statusFact = new ObservationFact( conceptInstance, FHIRConstants.ORDINAL_INTERPRETATION );
      initializeFact( statusFact, conceptInstance );
      statusFact.setCategory( FHIRConstants.HAS_INTERPRETATION );
      statusFact.autoFillDefaults();
      LOGGER.debug( "FactFactory.createStatusFact " + statusFact.getUri() + " " + statusFact.getCategory() );
      return statusFact;
   }

   static private Fact createReceptorStatusFact( final ConceptInstance conceptInstance ) {
      final String uri = conceptInstance.getUri();
      final String receptorUri = getReceptorUri( uri );
      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Collection<String> receptorStatusBranch = SearchUtil
            .getBranchUris( graphDb, receptorUri );
      if ( !receptorStatusBranch.contains( uri ) ) {
         return null;
      }
      // RECEPTOR
      final ObservationFact receptorFact = new ObservationFact( conceptInstance );
      receptorFact.setUri( receptorUri );
      receptorFact.setLabel( receptorUri );
      receptorFact.setIdentifier( conceptInstance.getId() + "_r" );
      receptorFact.setAncestors( getAncestors( receptorFact.getUri() ) );
      receptorFact.setCategory( FHIRConstants.HAS_RECEPTOR_STATUS );
      // STATUS
      final ObservationFact statusFact = new ObservationFact( conceptInstance, FHIRConstants.ORDINAL_INTERPRETATION );
      initializeFact( statusFact, conceptInstance );
      statusFact.setCategory( FHIRConstants.HAS_INTERPRETATION );
      receptorFact.setInterpretation( statusFact );
      statusFact.autoFillDefaults();
      LOGGER.debug( "FactFactory.createReceptorStatusFact " + receptorFact.getUri() + " " + receptorFact.getCategory() +
                   " has " + statusFact.getUri() + " " + statusFact.getCategory() );
      return receptorFact;
   }


   static private ValueFact createSizeFact( final ConceptInstance conceptInstance ) {
      if ( !conceptInstance.getUri()
                           .equals( UriConstants.SIZE ) ) {
         return null;
      }
      // hopefully there is just one annotation, but we may get something like "2x3mm" , "2x3mm"
      final List<String> texts = conceptInstance.getAnnotations()
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
      return createSizeFact( conceptInstance, values, unitSb.toString().trim() );
   }

   static private ValueFact createSizeFact( final ConceptInstance conceptInstance, final List<Double> values, final String units ) {
      final double[] valueArray = new double[ values.size() ];
      for ( int i = 0; i < values.size(); i++ ) {
         valueArray[ i ] = values.get( i );
      }
      final ValueFact fact = new ValueFact( conceptInstance, FHIRConstants.TUMOR_SIZE );
      fact.setUri( UriConstants.SIZE );
      fact.setValues( valueArray );
      fact.setUnit( units );
      fact.autoFillDefaults();
      final ValueFact normalizedFact = UnitConverter.createNormalizedFact( fact );
      if ( !fact.equals( normalizedFact ) ) {
         fact.setNormalizedFact( normalizedFact );
      }
      return fact;
   }

	static public ValueFact createValueFact( final Fact sourceFact, final double[] values, final String units ) {
      final ValueFact fact = new ValueFact( sourceFact.getConceptInstance() );
      fact.copyIdInfo( sourceFact );
      fact.copyDocInfo( sourceFact );
		fact.setValues( values );
		fact.setUnit( units );
      fact.autoFillDefaults();
		return fact;
	}


   static public Fact createHistologyFact( final CiSummary ciSummary ) {
      final String histology = ciSummary.getHistology();
      if ( histology == null || histology.isEmpty() ) {
         return null;
      }
      final Fact fact = createTypeFact( CONDITION );
      fact.setCategory( RelationConstants.HAS_HISTOLOGY );
      fact.setUri( histology );
      fact.autoFillDefaults();
      return fact;
   }

   static public Fact createCancerTypeFact( final CiSummary ciSummary ) {
      final String cancerType = ciSummary.getCancerType();
      if ( cancerType == null || cancerType.isEmpty() ) {
         return null;
      }
      final Fact fact = createTypeFact( CONDITION );
      fact.setCategory( FHIRConstants.HAS_CANCER_TYPE );
      fact.setUri( cancerType );
      fact.autoFillDefaults();
      return fact;
   }


   /**
    * !!! Used in Drools !!!
    * create empty fact of a given type
    * @param type -
    * @return -
    */
   public static Fact createFact( String type, String uri ) {
      final Fact fact = createTypeFact( type );
      fact.setUri( uri );
      fact.autoFillDefaults();
      return fact;
   }

   /**
    * !!! Used in Drools !!!
    * <p>
    * create fact and copy most of the parameters from the old one
    *
    * @param sourceFact    -
    * @param uri           -
    * @param containerType either a source doc type or one of CancerSummary, TumorSummary, RecordSummary (from drools)
    * @return -
    */
   public static Fact createFact( final Fact sourceFact, final String uri, final String containerType ) {
      return createFact( sourceFact, sourceFact.getType(), uri, containerType );
   }

   /**
    * !!! Used in Drools !!!  Though that can be refactored to leave out uri, type
    *
    * create fact and copy most of the parameters from the old one
    * @param oldFact -
    * @param type -
    * @param uri -
    * @param containerType either a source doc type or one of CancerSummary, TumorSummary, RecordSummary (from drools)
    * @return -
    */
   public static Fact createFact( final Fact oldFact, final String type, final String uri,
                                  final String containerType ) {
      final Fact newFact = createTypeFact( type );
      newFact.setUri( uri );
      newFact.setType( type );
      newFact.copyIdInfo( oldFact );
      newFact.copyDocInfo( oldFact );
      newFact.setAncestors( oldFact.getAncestors() );
      newFact.setCategory( oldFact.getCategory() );
      newFact.setPatientIdentifier( oldFact.getPatientIdentifier() );
      newFact.setSummaryType( oldFact.getSummaryType() );
      newFact.setSummaryId( oldFact.getSummaryType() + "_" + oldFact.getName() );
      newFact.setDocumentType( containerType );
      newFact.autoFillDefaults();

      newFact.addProperties( oldFact.getProperties() );
      oldFact.getProvenanceText().forEach( newFact::addProvenanceText );

      return newFact;
   }

   /**
    * !!! Used in Drools !!!  Though that can be refactored to leave out uri, type
    * <p>
    * create fact and copy most of the parameters from the old one
    *
    * @param oldFact       -
    * @param containerType either a source doc type or one of CancerSummary, TumorSummary, RecordSummary (from drools)
    * @return -
    */
   public static Fact createFact( final Fact oldFact, final String containerType ) {
      final Fact newFact = createClassedFact( oldFact );
      newFact.setType( oldFact.getType() );
      newFact.copyIdInfo( oldFact );
      newFact.copyDocInfo( oldFact );
      newFact.setAncestors( oldFact.getAncestors() );
      newFact.setPatientIdentifier( oldFact.getPatientIdentifier() );
      newFact.setDocumentType( containerType );
      newFact.autoFillDefaults();

      newFact.addProperties( oldFact.getProperties() );
      oldFact.getProvenanceText().forEach( newFact::addProvenanceText );

      return newFact;
   }

   static private Fact createClassedFact( final Fact oldFact ) {
      try {
         final Constructor<? extends Fact> constructor = oldFact.getClass().getConstructor();
         return constructor.newInstance();
      } catch ( NoSuchMethodException | SecurityException
            | InstantiationException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException multE ) {
         LOGGER.error( "Could not create new Fact of class " + oldFact.getClass().getSimpleName(), multE );
      }
      return createSemanticFact( oldFact.getConceptInstance() );
   }

	/**
	 * create fact from information of a different fact
	 * (without cloning provenance or attributes)
	 * @param oldFact -
	 * @return -
	 */
	public static Fact createFact( final Fact oldFact ) {
		final Fact newFact = createClassedFact( oldFact );
      newFact.copyIdInfo( oldFact );
      newFact.copyDocInfo( oldFact );
      newFact.setIdentifier( "Fact_" + newFact.getName() + "_" + System.currentTimeMillis() );
		newFact.addProperties( oldFact.getProperties() );
      oldFact.getProvenanceText().forEach( newFact::addProvenanceText );
      return newFact;
	}

   /**
    *  !!! Used by Drools MergedTumor.java
    * @param modifierFact -
    * @param tumorSummary -
    * @param cancerSummary -
    * @param summaryType -
    * @param category -
    * @param documentType -
    * @param type -
    * @return -
    */
   public static Fact createTumorFactModifier( final Fact modifierFact, final Fact tumorSummary,
                                               final Fact cancerSummary, final String summaryType,
                                               final String category, final String documentType, final String type ) {
      final Fact fact = createSemanticFact( modifierFact.getConceptInstance() );
      fact.autoFillDefaults();

      if (cancerSummary != null) {
         fact.addContainerIdentifier(cancerSummary.getSummaryId());
      }

      fact.addContainerIdentifier( tumorSummary.getSummaryId() );


      fact.setCategory( category );
      fact.setPatientIdentifier( tumorSummary.getPatientIdentifier() );
      fact.setSummaryType( summaryType );
      fact.setType( type );

      String name = fact.getName();
      fact.setSummaryId( tumorSummary.getSummaryId());
      fact.setIdentifier( tumorSummary.getId() + "-" + name );
      fact.setDocumentType( documentType );

      return fact;
   }

   /**
    * !!! Used by Drools MergedTumor.java
    *
    * @param uri          -
    * @param tumorSummary    -
    * @param cSummaryF    -
    * @param summaryType  -
    * @param category     -
    * @param documentType -
    * @param type         -
    * @return -
    */
   public static Fact createTumorFactModifier( final String uri, final Fact tumorSummary,
                                               final Fact cSummaryF, final String summaryType,
                                               final String category, final String documentType, final String type ) {
      Fact fact = FactFactory.createFact( type, uri );

      fact.addProvenanceFact( cSummaryF );
      fact.addProvenanceFact( tumorSummary );

      fact.addContainerIdentifier( cSummaryF.getSummaryId() );
      fact.addContainerIdentifier( tumorSummary.getSummaryId() );

      fact.setCategory( category );
      fact.setPatientIdentifier( tumorSummary.getPatientIdentifier() );
      fact.setSummaryType( summaryType );
      fact.setType( type );

      String name = fact.getName();
      fact.setSummaryId( tumorSummary.getSummaryId());
      fact.setIdentifier( tumorSummary.getId() + "-" + name );
      fact.setDocumentType( documentType );

      return fact;
   }

}