package org.healthnlp.deepphe.summary.concept.bin;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.MentionRelation;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.DefaultConceptAggregate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {5/24/2021}
 */
final public class BinDistributor {

   static private Logger LOGGER = Logger.getLogger( "BinDistributor" );

   public enum MentionType {
      CANCER, TUMOR, OTHER;
   }


   static public Map<String,Mention> mapIdToMention( final Collection<Mention> mentions ) {
      return mentions.stream()
                     .collect( Collectors.toMap( Mention::getId,
                                                 Function.identity() ) );
   }

   static public Map<Mention,Map<String,Collection<Mention>>> mapRelations( final Collection<Mention> mentions,
                                                                                    final Map<String,Mention> idToMentionMap,
                                                                                    final Collection<MentionRelation> mentionRelations ) {
      final Map<Mention,Map<String,Collection<Mention>>> neoplasmRelationsMap = new HashMap<>();
      for ( MentionRelation relation : mentionRelations ) {
         final Mention source = idToMentionMap.get( relation.getSourceId() );
         if ( source == null || !mentions.contains( source ) ) {
            continue;
         }
         final Mention target = idToMentionMap.get( relation.getTargetId() );
         if ( target == null || target.equals( source ) ) {
            continue;
         }
         neoplasmRelationsMap.computeIfAbsent( source, s -> new HashMap<>() )
                             .computeIfAbsent( relation.getType(), t -> new HashSet<>() )
                             .add( target );
      }
      return neoplasmRelationsMap;
   }


   static public Map<Mention,Collection<ConceptAggregate>> createNonNeoplasms(
          final Collection<Mention> nonNeoplasms,
         final String patientId,
          final Map<Mention, String> patientMentionNoteIds,
          final Map<String,Collection<String>> allUriRoots ) {
      final Map<Mention,Collection<ConceptAggregate>> mentionConceptsMap = new HashMap<>();
      final Map<String,Set<Mention>> uriMentionsMap
            = nonNeoplasms.stream()
                          .collect( Collectors.groupingBy( Mention::getClassUri, Collectors.toSet() ) );
      final Map<String,Collection<String>> uriChainsMap = UriUtil.getAllAssociatedUriMap( uriMentionsMap.keySet() );
      for ( Collection<String> uriChain : uriChainsMap.values() ) {
         final Map<String,Set<Mention>> uriMentions = new HashMap<>( uriMentionsMap );
         uriMentions.keySet().retainAll( uriChain );
         final ConceptAggregate conceptAggregate = createConceptAggregate( uriMentions, patientId,
                                                                           patientMentionNoteIds, allUriRoots );
         uriMentions.values()
                    .stream()
                    .flatMap( Collection::stream )
                    .forEach( m -> mentionConceptsMap.computeIfAbsent( m, c -> new HashSet<>() ).add( conceptAggregate ) );
      }
      return mentionConceptsMap;
   }




//   static public Map<ConceptAggregate,Collection<ConceptAggregate>> createCancerTumors(
//         final Collection<SiteNeoplasmBin> siteNeoplasmBins,
//         final Map<Mention, Map<String, Collection<Mention>>> mentionRelations,
//         final String patientId,
//         final Map<Mention, String> patientMentionNoteIds,
//         final Map<String, Collection<String>> allUriRoots ) {
//      final Map<NeoplasmChain,Collection<NeoplasmChain>> cancerTumorsMap = new HashMap<>();
//
//      final Collection<NeoplasmChain> allCancers = siteNeoplasmBins.stream()
//                                                                .map( b -> b.getNeoplasmChains(NeoplasmType.CANCER ) )
//                                                                .flatMap( Collection::stream )
//                                                                .collect( Collectors.toSet() );
//      for ( SiteNeoplasmBin siteNeoplasmBin : siteNeoplasmBins ) {
//         final Collection<NeoplasmChain> cancers = siteNeoplasmBin.getNeoplasmChains( NeoplasmType.CANCER );
//         cancers.forEach( c -> cancerTumorsMap.computeIfAbsent( c, t -> new HashSet<>() )
//                                              .addAll( siteNeoplasmBin.getNeoplasmChains( NeoplasmType.TUMOR ) ) );
//         LOGGER.info( "createCancerTumors, NeoplasmBin:" + siteNeoplasmBin.toString() );
//      }
//      final Map<ConceptAggregate,Collection<ConceptAggregate>> diagnosisMap = new HashMap<>();
//      for ( Map.Entry<NeoplasmChain,Collection<NeoplasmChain>> cancerTumors : cancerTumorsMap.entrySet() ) {
//         final ConceptAggregate cancer = cancerTumors.getKey()
//                                               .createConceptAggregate( patientId, patientMentionNoteIds, allUriRoots );
//         final Collection<ConceptAggregate> tumors =
//               cancerTumors.getValue()
//                           .stream()
//                           .map( c -> c.createConceptAggregate( patientId, patientMentionNoteIds, allUriRoots ) )
//                           .collect( Collectors.toSet() );
//         diagnosisMap.put( cancer, tumors );
//      }
//      return diagnosisMap;
//   }





   static private ConceptAggregate createConceptAggregate( final Map<String,Set<Mention>> uriMentions,
                                            final String patientId,
                                            final Map<Mention, String> patientMentionNoteIds,
                                            final Map<String,Collection<String>> allUriRoots ) {
      final Map<String, Collection<Mention>> noteIdMentionsMap
            = uriMentions.values()
                          .stream()
                          .flatMap( Collection::stream )
                          .collect( Collectors.groupingBy( patientMentionNoteIds::get,
                                                           Collectors.toCollection( HashSet::new ) ) );
      // smaller map of uris to roots that only contains pertinent uris.
      final Map<String,Collection<String>> uriRoots = new HashMap<>();
      uriMentions.keySet().forEach( u -> uriRoots.put( u, allUriRoots.get( u ) ) );
      return new DefaultConceptAggregate( patientId, uriRoots, noteIdMentionsMap ) ;
   }


   static public Map<String,Collection<String>> mapUriRoots( final Collection<Mention> mentions ) {
      final Collection<String> allUris = mentions.stream()
                                                   .map( Mention::getClassUri )
                                                   .collect( Collectors.toSet() );
      return UriUtil.mapUriRoots( allUris );
   }

   static public Map<String,Collection<String>> mapUriBranches( final Collection<String> uris ) {
      return uris.stream()
                 .collect( Collectors.toMap( Function.identity(), Neo4jOntologyConceptUtil::getBranchUris ) );
   }


   static public Map<Mention,Collection<ConceptAggregate>> mapMentionToConcepts( final Collection<ConceptAggregate> concepts ) {
      final Map<Mention,Collection<ConceptAggregate>> mentionAggregates = new HashMap<>();
         for ( ConceptAggregate concept : concepts ) {
            concept.getMentions()
                   .forEach( m -> mentionAggregates.computeIfAbsent( m, c -> new HashSet<>() )
                                                   .add( concept ) );
         }
      return mentionAggregates;
   }

//   static public void assignConceptRelations( final Map<Mention,Collection<ConceptAggregate>> mentionConceptsMap,
//                                       final Map<Mention,Map<String,Collection<Mention>>> mentionRelationsMap ) {
//      for ( Map.Entry<Mention,Map<String,Collection<Mention>>> mentionRelations : mentionRelationsMap.entrySet() ) {
//         final Collection<ConceptAggregate> sources = mentionConceptsMap.get( mentionRelations.getKey() );
//         if ( sources == null || sources.isEmpty() ) {
//            continue;
//         }
//         final Map<String,Collection<Mention>> relations = mentionRelations.getValue();
//         for ( Map.Entry<String,Collection<Mention>> relation : relations.entrySet() ) {
//            final Collection<ConceptAggregate> targets = relation.getValue()
//                                                                 .stream()
//                                                                 .map( mentionConceptsMap::get )
//                                                                 .filter( Objects::nonNull )
//                                                                 .flatMap( Collection::stream )
//                                                                 .collect( Collectors.toSet() );
//            for ( ConceptAggregate target : targets ) {
//               for ( ConceptAggregate source : sources ) {
//                  if ( !source.equals( target ) ) {
//                     source.addRelated( relation.getKey(), target );
//                  }
//               }
//            }
//         }
//      }
//   }


   static public void assignConceptRelations(
         final AssertionBin.DiagnosisConcepts diagnosisConcepts,
         final Map<Mention,Collection<ConceptAggregate>> mentionConceptsMap,
         final Map<Mention,Map<String,Collection<Mention>>> mentionRelationsMap ) {
      for ( Map.Entry<Mention,Map<String,Collection<Mention>>> mentionRelations : mentionRelationsMap.entrySet() ) {
         final Collection<ConceptAggregate> sources = mentionConceptsMap.get( mentionRelations.getKey() );
         if ( sources == null || sources.isEmpty() ) {
            continue;
         }
         final Map<String,Collection<Mention>> relations = mentionRelations.getValue();
         for ( Map.Entry<String,Collection<Mention>> relation : relations.entrySet() ) {
            final boolean isHasSite = ( RelationConstants.isHasSiteRelation( relation.getKey() )
                 || RelationConstants.HAS_LATERALITY.equals( relation.getKey() ) );
            final Collection<ConceptAggregate> targets = relation.getValue()
                                                                 .stream()
                                                                 .map( mentionConceptsMap::get )
                                                                 .filter( Objects::nonNull )
                                                                 .flatMap( Collection::stream )
                                                                 .collect( Collectors.toSet() );
            for ( ConceptAggregate target : targets ) {
               for ( ConceptAggregate source : sources ) {
                  if ( !source.equals( target )
                       && (!isHasSite || isValidSite( diagnosisConcepts, source, target ) ) ) {
                     source.addRelated( relation.getKey(), target );
                  }
               }
            }
         }
      }
   }

   static private boolean isValidSite(
         final AssertionBin.DiagnosisConcepts diagnosisConcepts,
         final ConceptAggregate source,
         final ConceptAggregate target ) {
      final Collection<String> validUris
            = diagnosisConcepts._cancerSiteUrisMap.getOrDefault( source, new HashSet<>() );
      validUris.addAll( diagnosisConcepts._tumorSiteUrisMap.getOrDefault( source, new HashSet<>() ) );
      return target.getAllUris()
                   .stream()
                   .anyMatch( validUris::contains );
   }


}
