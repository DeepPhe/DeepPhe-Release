package org.apache.ctakes.cancer.ae.coref;


import org.apache.ctakes.cancer.uri.UriAnnotationUtil;
import org.apache.ctakes.core.util.DpheEssentialAnnotationUtil;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.healthnlp.deepphe.neo4j.RelationConstants;

import java.util.*;

import static org.apache.ctakes.cancer.uri.UriConstants.LYMPH_NODE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/7/2019
 */
final public class CorefUtil {

   static private final Logger LOGGER = Logger.getLogger( "CorefUtil" );


   /**
    * @param jCas ye olde ...
    * @return a map of markables to indexed chain numbers
    */
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableCorefs( final JCas jCas ) {
      final Collection<CollectionTextRelation> corefs = JCasUtil.select( jCas, CollectionTextRelation.class );
      final Map<Markable, Collection<IdentifiedAnnotation>> markableAnnotations
            = DpheEssentialAnnotationUtil.mapMarkableAnnotations( jCas, corefs );
      return createMarkableCorefs( jCas, corefs, markableAnnotations );
   }


   /**
    * @param corefs coreference chains
    * @return a map of markables to indexed chain numbers
    */
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableCorefs(
         final JCas jCas,
         final Collection<CollectionTextRelation> corefs,
         final Map<Markable, Collection<IdentifiedAnnotation>> markableAnnotations ) {
      if ( corefs == null || corefs.isEmpty() ) {
         return Collections.emptyMap();
      }

      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      final Map<Annotation,Collection<String>> locationUris = new HashMap<>();
      final Map<Annotation,Collection<String>> lateralityUris = new HashMap<>();
      for ( BinaryTextRelation relation : relations ) {
         if ( relation.getCategory().equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE ) ) {
            final String uri = Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)relation.getArg2().getArgument() );
            locationUris.computeIfAbsent( relation.getArg1().getArgument(), a -> new HashSet<>() ).add( uri );
         } else if ( relation.getCategory().equals( RelationConstants.HAS_LATERALITY ) ) {
            final String uri = Neo4jOntologyConceptUtil.getUri( (IdentifiedAnnotation)relation.getArg2().getArgument() );
            lateralityUris.computeIfAbsent( relation.getArg1().getArgument(), a -> new HashSet<>() ).add( uri );
         }
      }

      final List<List<IdentifiedAnnotation>> chains = new ArrayList<>();
      for ( CollectionTextRelation coref : corefs ) {
         collateCoref( chains, coref, markableAnnotations, locationUris, lateralityUris );
     }
      chains.forEach( c -> c.sort( Comparator.comparingInt( Annotation::getBegin ) ) );
      chains.sort( ( l1, l2 ) -> l1.get( 0 ).getBegin() - l2.get( 0 ).getBegin() );

      final Map<IdentifiedAnnotation, Collection<Integer>> corefMarkables = new HashMap<>();
      int index = 1;
      for ( Collection<IdentifiedAnnotation> chain : chains ) {
         for ( IdentifiedAnnotation annotation : chain ) {
            corefMarkables.computeIfAbsent( annotation, a -> new ArrayList<>() ).add( index );
         }
         index++;
      }
      return corefMarkables;
   }


   static private String getAssertion( final IdentifiedAnnotation annotation ) {
      final StringBuilder sb = new StringBuilder();
      if ( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT ) {
         sb.append( "NEGATED" );
      } else {
         sb.append( "AFFIRMED" );
      }
      if ( annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT ) {
         sb.append( "UNCERTAIN" );
      }
      if ( annotation.getGeneric() ) {
         sb.append( "GENERIC" );
      }
      if ( annotation.getConditional() ) {
         sb.append( "CONDITIONAL" );
      }
      return sb.toString();
   }


   static private void mergeChains(
         final List<List<IdentifiedAnnotation>> chains,
         final Map<String,Map<String,Collection<IdentifiedAnnotation>>> siteLateralChains,
         final Map<String, Collection<IdentifiedAnnotation>> lateralOnly ) {
      if ( lateralOnly == null ) {
         return;
      }
      // Only a single location, equate all non-located entities and add each laterality
      for ( Map<String,Collection<IdentifiedAnnotation>> lateralCollatedChains : siteLateralChains.values() ) {
         for ( Map.Entry<String,Collection<IdentifiedAnnotation>> lateralChain : lateralCollatedChains.entrySet() ) {
            if ( lateralOnly.get( lateralChain.getKey() ) != null ) {
               lateralChain.getValue().addAll( lateralOnly.get( lateralChain.getKey() ) );
            }
         }
      }
      addToChains( chains, siteLateralChains );
   }


   static private void addToChains(
         final List<List<IdentifiedAnnotation>> chains,
         final Map<String,Map<String,Collection<IdentifiedAnnotation>>> siteLateralChains ) {
      for ( Map<String,Collection<IdentifiedAnnotation>> map : siteLateralChains.values() ) {
         if ( map == null ) {
            continue;
         }
         map.values().stream()
            .filter( Objects::nonNull )
            .filter( c -> c.size() > 1 )
            .forEach( c -> chains.add( new ArrayList<>( c ) )  );
      }
   }


   static public void collateCoref(
         final List<List<IdentifiedAnnotation>> chains,
         final CollectionTextRelation coref,
         final Map<Markable, Collection<IdentifiedAnnotation>> markableAnnotations,
         final Map<Annotation,Collection<String>> locationUris,
         final Map<Annotation,Collection<String>> lateralityUris ) {
//      LOGGER.info( "collateCoref for " + chains.size() + " chains with " + locationUris.size() + " locations and " + lateralityUris.size() + " lateralities" );
      final Map<String,List<IdentifiedAnnotation>> assertionMap = new HashMap<>();
      final FSList chainHead = coref.getMembers();
      final Collection<Markable> markables = FSCollectionFactory.create( chainHead, Markable.class );
      for ( Markable markable : markables ) {
         for ( IdentifiedAnnotation annotation : markableAnnotations.get( markable ) ) {
            final String assertion = getAssertion( annotation );
            assertionMap.computeIfAbsent( assertion, a -> new ArrayList<>() ).add( annotation );
//            LOGGER.info( "collateCoref added " + annotation.getCoveredText() + " to assertion " + assertion );
         }
      }
      for ( List<IdentifiedAnnotation> asserted : assertionMap.values() ) {
         if ( asserted.size() <= 1 ) {
            continue;
         }
//         LOGGER.info( "collateCoref calling collateAsserted for " + asserted.stream().map( Annotation::getCoveredText ).collect( Collectors.joining( " , " ) ) );
         collateAsserted( chains, asserted, locationUris, lateralityUris );
      }
   }


   static public void collateCoref(
         final List<List<IdentifiedAnnotation>> chains,
         final Collection<IdentifiedAnnotation> coref,
         final Map<Annotation,Collection<String>> locationUris,
         final Map<Annotation,Collection<String>> lateralityUris ) {

      final Map<String,List<IdentifiedAnnotation>> assertionMap = new HashMap<>();
      for ( IdentifiedAnnotation annotation : coref ) {
         final String assertion = getAssertion( annotation );
         assertionMap.computeIfAbsent( assertion, a -> new ArrayList<>() ).add( annotation );
      }
      for ( List<IdentifiedAnnotation> asserted : assertionMap.values() ) {
         if ( asserted.size() <= 1 ) {
            continue;
         }
         collateAsserted( chains, asserted, locationUris, lateralityUris );
      }
   }


   static private void collateAsserted(
         final List<List<IdentifiedAnnotation>> chains,
         final List<IdentifiedAnnotation> asserted,
         final Map<Annotation,Collection<String>> locationUris,
         final Map<Annotation,Collection<String>> lateralityUris ) {
//      LOGGER.info( "collateAsserted calling collateBySiteLateral for " + asserted.stream().map( Annotation::getCoveredText ).collect( Collectors.joining( " , " ) ) );
      // Gather locations and separate chain by those
      final Map<String,Map<String,Collection<IdentifiedAnnotation>>> siteCollatedChains
            = collateBySiteLateral( asserted, locationUris, lateralityUris );
      if ( siteCollatedChains.size() == 1 ) {
//         LOGGER.info( "collateAsserted calling addToChains for 1" );
         // Only a single location.  Add each laterality as a chain
         addToChains( chains, siteCollatedChains );
         return;
      }
      final Map<String, Collection<IdentifiedAnnotation>> siteNeutrals = siteCollatedChains.get( SITE_NEUTRAL );
      if ( siteNeutrals != null ) {
         siteCollatedChains.remove( SITE_NEUTRAL );
         if ( siteCollatedChains.size() == 1 ) {
            // Only a single location, equate all non-located entities and add each laterality
//            LOGGER.info( "collateAsserted calling mergeChains with " + siteNeutrals.size() + " site-neutrals" );
            mergeChains( chains, siteCollatedChains, siteNeutrals );
            return;
         }
      }
      final Map<String, Collection<IdentifiedAnnotation>> lymphNodes = siteCollatedChains.get( LYMPH_NODE );
      if ( lymphNodes != null ) {
         siteCollatedChains.remove( LYMPH_NODE );
         if ( siteCollatedChains.size() == 1 ) {
            // Only a single location, equate all non-located entities and add each laterality
            if ( siteNeutrals != null ) {
//               LOGGER.info( "collateAsserted calling mergeChains with " + siteNeutrals.size() + " site-neutrals" );
               mergeChains( chains, siteCollatedChains, siteNeutrals );
            }
//            LOGGER.info( "collateAsserted calling mergeChains with " + lymphNodes.size() + " lymph nodes" );
            // Only a single location and lymph nodes, equate all lymph node entities and add each laterality
            mergeChains( chains, siteCollatedChains, lymphNodes );
            return;
         }
      }
      if ( siteNeutrals != null ) {
//         LOGGER.info( "collateAsserted calling addToChains for " + siteNeutrals.size() + " site-neutrals, " );
         siteCollatedChains.put( SITE_NEUTRAL, siteNeutrals );
      }
      if ( lymphNodes != null ) {
//         LOGGER.info( "collateAsserted calling addToChains for " + lymphNodes.size() + " lymph nodes, " );
         siteCollatedChains.put( LYMPH_NODE, lymphNodes );
      }
      // At this point we have:
      // siteCollatedInstances with more than 1 site,
      // lymphNodes with lateral lymph nodes,
      // anySite with lateral non-sited
//      LOGGER.info( "collateAsserted calling addToChains for " + siteCollatedChains.size() + " unique sited" );
      addToChains( chains, siteCollatedChains );
   }


   /**
    * @param annotations -
    * @return Map of related tumor concept instances, Uri is Key 1, Laterality is key 2, deep value is annotations.
    * Tumors are related if they have the same laterality and are within the same body site uri branch.
    */
   static private Map<String,Map<String,Collection<IdentifiedAnnotation>>> collateBySiteLateral(
         final Collection<IdentifiedAnnotation> annotations,
         final Map<Annotation, Collection<String>> locationUris,
         final Map<Annotation, Collection<String>> lateralityUris ) {

      // Collection of "same-site" "same-laterality" tumors
      final Map<String,Map<String,Collection<IdentifiedAnnotation>>> lateralSitedAnnotations = new HashMap<>();

//      LOGGER.info( "collateBySiteLateral calling collateBySite with " + annotations.size() + " annotations and " + locationUris.size() + " located annotations" );
      // Collection of "same-site" tumors
      final Map<String,Collection<IdentifiedAnnotation>> sitedAnnotations
            = collateBySite( annotations, locationUris );

//      LOGGER.info( "collateBySiteLateral calling collateByLaterality with " + sitedAnnotations.size() + " sited annotations and " + lateralityUris.size() + " lateralled annotations" );
      // deal with laterality
      for ( Map.Entry<String,Collection<IdentifiedAnnotation>> siteAnnotations : sitedAnnotations.entrySet() ) {
         // Map of laterality uris to tumor concept instances with that laterality
         final Map<String, Collection<IdentifiedAnnotation>> lateralSited
               = collateByLaterality( siteAnnotations.getValue(), lateralityUris );

         lateralSitedAnnotations.put( siteAnnotations.getKey(), lateralSited );
      }
      return lateralSitedAnnotations;
   }


   /**
    * Also collapses body sites
    * @param annotations -
    * @return map of best site uris to all annotations with that best site.
    * annotations are relatable if they have the same laterality and are within the same bodysite uri branch.
    */
   static private Map<String,Collection<IdentifiedAnnotation>> collateBySite(
         final Collection<IdentifiedAnnotation> annotations,
         final Map<Annotation, Collection<String>> locationUris ) {

//      LOGGER.info( "collateBySite calling getUriBodySites with " + annotations.size() + " annotations and " + locationUris.size() + " locations" );
      // map of body site uris and annotations with those uris  -> for all tumors with this laterality
      final Map<String, List<IdentifiedAnnotation>> uriBodySites = getUriBodySites( annotations, locationUris );
      // collate site uris
//      LOGGER.info( "collateBySite calling UriAnnotationUtil.getAssociatedUriMap with " + uriBodySites.size() + " uri body site uris" );
//      return UriAnnotationUtil.collateUriCloseEnough( uriBodySites );
      final Map<String,Collection<String>> associatedSiteUriMap = UriAnnotationUtil.getAssociatedUriMap( uriBodySites );

//      LOGGER.info( "collateBySite associating body sites for " + associatedSiteUriMap.size() + " associated site uris" );
      final Map<String,Collection<IdentifiedAnnotation>> associatedBodySites = new HashMap<>( associatedSiteUriMap.size() );
      for ( Map.Entry<String,Collection<String>> associatedSiteUris : associatedSiteUriMap.entrySet() ) {
         final Collection<IdentifiedAnnotation> associatedSites = new HashSet<>();
         for ( String associatedSiteUri : associatedSiteUris.getValue() ) {
            final Collection<IdentifiedAnnotation> sited = uriBodySites.get( associatedSiteUri );
            if ( sited != null ) {
               associatedSites.addAll( sited );
            }
         }
         associatedBodySites.put( associatedSiteUris.getKey(), associatedSites );
      }
//      LOGGER.info( "collateBySite associated annotations in " + associatedBodySites.size() + " associated site uris" );
      return associatedBodySites;
   }

   static private final String SITE_NEUTRAL = "Site_Neutral";
   static private final String SIDE_NEUTRAL = "Side_Neutral";

   static private Map<String, List<IdentifiedAnnotation>> getUriBodySites(
         final Collection<IdentifiedAnnotation> annotations,
         final Map<Annotation, Collection<String>> locationUris ) {
      final Map<String,List<IdentifiedAnnotation>> uriSites = new HashMap<>();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Collection<String> sites = locationUris.get( annotation );
         if ( sites == null ) {
            uriSites.computeIfAbsent( SITE_NEUTRAL, s -> new ArrayList<>() ).add( annotation );
            continue;
         }
         for ( String site : sites ) {
            uriSites.computeIfAbsent( site, s -> new ArrayList<>() ).add( annotation );
         }
      }
      return uriSites;
   }


//   static public Map<String,List<IdentifiedAnnotation>> createUriOnlyCorefs(
//         final Collection<IdentifiedAnnotation> annotations ) {
//      final Map<String,List<IdentifiedAnnotation>> uriAnnotations = UriAnnotationUtil.mapUriAnnotations( annotations );
//      return createUriOnlyCorefs( uriAnnotations );
//   }
//
//
//   static private Map<String,List<IdentifiedAnnotation>> createUriOnlyCorefs(
//         final Map<String,List<IdentifiedAnnotation>> uriAnnotations ) {
//      return UriAnnotationUtil.collateUriCloseEnough( uriAnnotations );
//   }


//   static public Map<String,List<IdentifiedAnnotation>> createMarkedUriCorefs(
//         final Map<String,List<IdentifiedAnnotation>> uriOnlyCorefs,
//         final Collection<CollectionTextRelation> corefs,
//         final Map<Markable, Collection<IdentifiedAnnotation>> markableAnnotations ) {
//      if ( corefs == null || corefs.isEmpty() ) {
//         return uriOnlyCorefs;
//      }
//
//
//      final List<List<IdentifiedAnnotation>> chains = new ArrayList<>(); /// uriOnlyCorefs.value()
////      for ( CollectionTextRelation coref : corefs ) {
////         collateCoref( chains, coref, markableAnnotations, locationUris, lateralityUris );
////      }
////      chains.forEach( c -> c.sort( Comparator.comparingInt( Annotation::getBegin ) ) );
////      chains.sort( ( l1, l2 ) -> l1.get( 0 ).getBegin() - l2.get( 0 ).getBegin() );
//
//      for ( CollectionTextRelation coref : corefs ) {
//         final FSList chainHead = coref.getMembers();
//         final Collection<Markable> markables = FSCollectionFactory.create( chainHead, Markable.class );
//         for ( Markable markable : markables ) {
//            for ( IdentifiedAnnotation annotation : markableAnnotations.get( markable ) ) {
////               final String assertion = getAssertion( annotation );
////               assertionMap.computeIfAbsent( assertion, a -> new ArrayList<>() ).add( annotation );
////               LOGGER.info( "collateCoref added " + annotation.getCoveredText() + " to assertion " + assertion );
//            }
//         }
//      }
//
//
//
//      final Map<IdentifiedAnnotation, Collection<Integer>> corefMarkables = new HashMap<>();
//      int index = 1;
//      for ( Collection<IdentifiedAnnotation> chain : chains ) {
//         for ( IdentifiedAnnotation annotation : chain ) {
//            corefMarkables.computeIfAbsent( annotation, a -> new ArrayList<>() ).add( index );
//         }
//         index++;
//      }
//
//      for ( Collection<IdentifiedAnnotation> chain : chains ) {
//         for ( IdentifiedAnnotation annotation : chain ) {
//            corefMarkables.computeIfAbsent( annotation, a -> new ArrayList<>() ).add( index );
//         }
//         index++;
//      }
//
//
//
//
//      return corefMarkables;
//
//
//   }
//

//   /**
//    *
//    * @param annotations -
//    * @return map of uris to lists of annotations (coref chains) for that uri
//    */
//   static public Map<String,List<IdentifiedAnnotation>> createUriOnlyCorefs(
//         final Collection<IdentifiedAnnotation> annotations ) {
//      LOGGER.info( "createUriOnlyCorefs calling UriAnnotationUtil.mapUriAnnotations with " + annotations.size() + " annotations" );
//      final Map<String,List<IdentifiedAnnotation>> uriAnnotations = UriAnnotationUtil.mapUriAnnotations( annotations );
//      LOGGER.info( "createUriOnlyCorefs have " + uriAnnotations.size() + " uris with chains.  Calling createUriOnlyCorefs" );
//      return createUriOnlyCorefs( uriAnnotations );
//   }
//
//
//   static public Map<String,List<IdentifiedAnnotation>> createUriOnlyCorefs(
//         final Map<String,List<IdentifiedAnnotation>> uriAnnotations ) {
//      LOGGER.info( "createUriOnlyCorefs calling UriAnnotationUtil.getAssociatedUriAnnotations with " + uriAnnotations.size() + " uris with chains." );
//      return UriAnnotationUtil.getAssociatedUriAnnotations( uriAnnotations );
//   }


   static private Map<String, Collection<IdentifiedAnnotation>> collateByLaterality(
         final Collection<IdentifiedAnnotation> annotations,
         final Map<Annotation, Collection<String>> lateralityUris ) {
      final Map<String, Collection<IdentifiedAnnotation>> lateralityMap = new HashMap<>( 3 );
      final Collection<IdentifiedAnnotation> sideNeutral = new HashSet<>();
//      LOGGER.info( "collateByLaterality filling laterality map for " + annotations.size() + " annotations" );
      for ( IdentifiedAnnotation annotation : annotations ) {
         final Collection<String> lateralities = lateralityUris.get( annotation );
         if ( lateralities == null || lateralities.isEmpty() ) {
            sideNeutral.add( annotation );
         } else {
            for ( String laterality : lateralities ) {
               lateralityMap.computeIfAbsent( laterality, s -> new HashSet<>() ).add( annotation );
            }
         }
      }
      if ( !sideNeutral.isEmpty() ) {
         if ( lateralityMap.size() == 1 ) {
//            LOGGER.info( "collateByLaterality only a single unique laterality, merging " + sideNeutral.size() + " non-sided." );
            lateralityMap.values().forEach( s -> s.addAll( sideNeutral ) );
         } else {
//            LOGGER.info( "collateByLaterality " + lateralityMap.size() + " unique lateralities, adding " + sideNeutral.size() + " non-sided." );
            lateralityMap.put( SIDE_NEUTRAL, sideNeutral );
         }
//      } else { LOGGER.info( "collateByLaterality " + lateralityMap.size() + " unique lateralities" ); }
      }
      return lateralityMap;
   }



}
