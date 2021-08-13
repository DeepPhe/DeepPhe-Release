package org.healthnlp.deepphe.summary.engine;

import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.concept.bin.AssertionBin;
import org.healthnlp.deepphe.summary.concept.bin.BinDistributor;
import org.healthnlp.deepphe.util.ConceptAggregateUtil;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Creates one or more neoplasm summaries, as opposed to the SummaryEngine which merges everything into a single
 * summary.
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/17/2020
 */
final public class MultiSummaryEngine {

   static private final Logger LOGGER = Logger.getLogger( "MultiSummaryEngine" );

   static public PatientSummaryXn createPatientSummaryXn( final Patient patient ) {
      LOGGER.info( "Creating Patient Summary for " + patient.getId() + " ..." );
      final String patientId = patient.getId();
      final Map<Mention, String> patientMentionNoteIds = new HashMap<>();
      final Collection<MentionRelation> patientRelations = new ArrayList<>();
      final Collection<Note> patientNotes = patient.getNotes();
      for ( Note note : patientNotes ) {
         final String noteId = note.getId();
         note.getMentions().forEach( m -> patientMentionNoteIds.put( m, noteId ) );
         patientRelations.addAll( note.getRelations() );


//         LOGGER.info( "\n====================== Note " + noteId + " ======================" );
//         LOGGER.info( "We have completed the per-document nlp and trimmed it to information essential for summarization." +
//                      "  Below are the mentions, relations between mentions, and mention coreference chains for the document " + noteId + "." );
//         note.getMentions().forEach( m -> LOGGER.info( "Mention: " + m.getClassUri() + " " + m.getId() ) );
//         note.getRelations().forEach( r -> LOGGER.info( "Relation: " + r.getSourceId() + " " + r.getType() + " " + r.getTargetId() ) );
//         LOGGER.info( "Within doc corefs are no longer being dicovered by NLP pipeline." );
//         note.getCorefs().forEach( c -> LOGGER.info( "Chain: (" + String.join( ",", Arrays.asList( c.getIdChain() ) ) + ")" ) );
      }

      final PatientSummaryXn patientSummary = createPatientSummaryXn( patientId,
                                                                  patientNotes,
                                                                  patientMentionNoteIds,
                                                                  patientRelations );
      patientSummary.setPatient( patient );
      return patientSummary;
   }





   static private PatientSummaryXn createPatientSummaryXn( final String patientId,
                                                           final Collection<Note> patientNotes,
                                                           final Map<Mention, String> patientMentionNoteIds,
                                                           final Collection<MentionRelation> patientRelations ) {
//      LOGGER.info( "\n====================== Creating Concept Aggregates for " + patientId + " ======================" );
//      LOGGER.info( "Concept Aggregates are basically unique concepts that are created by aggregating all mentions that are correferent." +
//                   "  While coreference chains are within single documents, Concept Aggregates span across all documents." );
//      +
//                   "  Concept Aggregates do not only aggregate cross-document mentions, but will also both aggregate and separate" +
//                   " mentions in within-document coreference chains." +
//                   "  So, yes, we could logically remove the coreference annotator from the nlp pipeline." +
//                   "   I will experiment when I have time.   - 10/14/2020 Done." );
//      LOGGER.info( "For the patient we have " + patientNotes.size() + " notes, "
//                   + patientMentionNoteIds.size() + " mentions, "
//                   + patientRelations.size() + " relations" );
//                   + patientCorefs.size() + " coref chains." );

      final Collection<Mention> affirmedMentions = AssertionBin.getAffirmedMentions( patientMentionNoteIds.keySet() );
      final Map<String,Mention> idToMentionMap = BinDistributor.mapIdToMention( affirmedMentions );
      final Map<Mention,Map<String,Collection<Mention>>> relationsMap
            = BinDistributor.mapRelations( affirmedMentions, idToMentionMap, patientRelations );

      final Map<String,Collection<String>> allUriRoots = BinDistributor.mapUriRoots( affirmedMentions );
      final Map<String,Collection<String>> allUriBranches = BinDistributor.mapUriBranches( allUriRoots.keySet() );

      final AssertionBin affirmedBin = new AssertionBin();
      // creates cancer and tumor bins, returns non neoplasm mentions.  This sets the neoplasms
      final Collection<Mention> nonNeoplasms = affirmedBin.splitMentions( affirmedMentions, relationsMap, allUriRoots );

      affirmedBin.distributeSites( allUriRoots, allUriBranches );
      affirmedBin.mergeExtents( relationsMap );
      affirmedBin.mergeSiteNeoplasmChains( allUriBranches );


      // TODO : Send hasTumorExtent to all SiteNeoplasmBins.
      //  For all Mass neoplasmChains that have some other mass chain in the same siteneoplasmbin as a tumor extent,
      //  Join the two together into 1 mass.


      // TODO Before making cancers within a siteneoplasmbin, cull by the lowest counted mentions.


      final AssertionBin.DiagnosisConcepts diagnosisConcepts = affirmedBin.createDiagnosisConcepts(
            patientId, patientMentionNoteIds, allUriRoots );

      final Map<Mention,Collection<ConceptAggregate>> siteConcepts = affirmedBin.mapSiteConcepts(
            patientId, patientMentionNoteIds, allUriRoots );

      nonNeoplasms.removeAll( siteConcepts.keySet() );

      final Map<Mention,Collection<ConceptAggregate>> mentionConceptsMap
            = BinDistributor.createNonNeoplasms( nonNeoplasms, patientId, patientMentionNoteIds,
                                                 allUriRoots );

      mentionConceptsMap.putAll(
            BinDistributor.mapMentionToConcepts( diagnosisConcepts._cancerSiteUrisMap.keySet() ) );
      mentionConceptsMap.putAll(
            BinDistributor.mapMentionToConcepts( diagnosisConcepts._tumorSiteUrisMap.keySet() ) );

      mentionConceptsMap.putAll( siteConcepts );

      BinDistributor.assignConceptRelations( diagnosisConcepts, mentionConceptsMap, relationsMap );


      final Collection<ConceptAggregate> allConcepts = mentionConceptsMap.values()
                                                                         .stream()
                                                                         .flatMap( Collection::stream )
                                                                         .collect( Collectors.toSet() );

      return createPatientSummaryXn( patientId,
                                     diagnosisConcepts._cancerTumorsMap,
                                     diagnosisConcepts.getLoneTumors(),
                                     allConcepts );
   }





//   static private PatientSummaryXn createPatientSummaryXn( final String patientId,
//                                                       final Collection<Note> patientNotes,
//                                                       final Map<Mention, String> patientMentionNoteIds,
//                                                       final Collection<MentionRelation> patientRelations ) {
////      LOGGER.info( "\n====================== Creating Concept Aggregates for " + patientId + " ======================" );
////      LOGGER.info( "Concept Aggregates are basically unique concepts that are created by aggregating all mentions that are correferent." +
////                   "  While coreference chains are within single documents, Concept Aggregates span across all documents." );
////      +
////                   "  Concept Aggregates do not only aggregate cross-document mentions, but will also both aggregate and separate" +
////                   " mentions in within-document coreference chains." +
////                   "  So, yes, we could logically remove the coreference annotator from the nlp pipeline." +
////                   "   I will experiment when I have time.   - 10/14/2020 Done." );
////      LOGGER.info( "For the patient we have " + patientNotes.size() + " notes, "
////                   + patientMentionNoteIds.size() + " mentions, "
////                   + patientRelations.size() + " relations" );
////                   + patientCorefs.size() + " coref chains." );
////      final Map<String, Collection<ConceptAggregate>> uriConceptAggregateMap
////            = createUriConceptAggregateMap( patientId, patientNotes, patientMentionNoteIds, patientRelations, patientCorefs );
////            = ConceptAggregateHandler.createUriConceptAggregateMap( patientId, patientMentionNoteIds, patientRelations );
//
//
//      final Collection<ConceptAggregate> neoplasmConcepts = BinDistributor.createConceptAggregates( patientId,
//                                                                                            patientMentionNoteIds,
//                                                                                            patientRelations );
//      // TODO - convert everything that isn't a neoplasm into ConceptAggregates.
//      // TODO - Map<Uri,Collection<Mention>> , UriUtil.getAssociatedUris(..), -> ConceptAggregates.
//      // TODO - set relations on all of the ConceptAggregates.  First Map<Mention,Collection<ConceptAggregate>>
//      final Map<String,List<ConceptAggregate>> uriConceptsMap
//            = neoplasmConcepts.stream()
//                      .collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
//
//      return createPatientSummaryXn( patientId, uriConceptsMap );
//   }


//   static private PatientSummaryXn createPatientSummaryXn( final String patientId,
////                                                       final Map<String, Collection<ConceptAggregate>> uriConceptAggregates ) {
//                                                           final Map<String, List<ConceptAggregate>> uriConceptsMap ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      final Collection<String> massNeoplasms = UriConstants.getMassNeoplasmUris( graphDb );
//
//
////      LOGGER.info( "\n====================== Summarizing " + patientId + " ======================" );
////      LOGGER.info( "We are now using the ConceptAggregates for the patient to create Cancer Summaries." +
////                   "  For KCR we force this down to a single summary, but that is only for the simplicity of the dataset." +
////                   "  dPhe Classic does not do this.  I think that this will not be done at some future date for CR." );
//
//
//      final Collection<ConceptAggregate> allAggregates = new HashSet<>();
//      final Collection<ConceptAggregate> neoplasmAggregates = new HashSet<>();
//
//      for ( Map.Entry<String, List<ConceptAggregate>> uriConcepts : uriConceptsMap.entrySet() ) {
//         allAggregates.addAll( uriConcepts.getValue() );
//         final String uri = uriConcepts.getKey();
//         if ( !massNeoplasms.contains( uri ) ) {
//            continue;
//         }
//         for ( ConceptAggregate concept : uriConcepts.getValue() ) {
//            if ( concept.isWantedForSummary() && concept.hasWantedRelations() ) {
//               neoplasmAggregates.add( concept );
//
//
////               LOGGER.info( "ConceptAggregate " + concept.getUri() + " " + concept.getId() + " is a neoplasm and
////               will be used for a Summary." );
//
//
//            }
//         }
//      }
//      return createPatientSummaryXn( patientId, neoplasmAggregates, allAggregates );
//   }


//   static private PatientSummaryXn createPatientSummaryXn( final String patientId,
//                                                       final Collection<ConceptAggregate> neoplasmConcepts,
//                                                       final Collection<ConceptAggregate> allConcepts ) {
//
//      // TODO
//      // TODO
//      // TODO
//      // TODO   --->>   This is redoing locations.  Need to merge without doing that.
//
//      neoplasmConcepts.forEach( LOGGER::info );
//
//
//
//      final Map<ConceptAggregate, Collection<ConceptAggregate>> diagnosisMap
//            = ConceptAggregateMerger.mergeNeoplasms( neoplasmConcepts, allConcepts );
//
//      // TODO Remove keys (Cancers) with mentions < max mention count / 4 (or some settable number)
//      final int maxMentions = diagnosisMap.keySet().stream().mapToInt( c -> c.getMentions().size() ).max().orElse( 0 );
//      final Collection<ConceptAggregate> tooSmall =
//            diagnosisMap.keySet().stream().filter( c -> c.getMentions().size() < maxMentions/3 ).collect(
//                  Collectors.toList() );
//      LOGGER.info( "Removing Too Small "
//                   + tooSmall.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining( ";" ) ) );
//      diagnosisMap.keySet().removeAll( tooSmall );
//
//      return summarizeConceptAggregateMap( patientId, diagnosisMap, allConcepts );
//   }

   static private PatientSummaryXn createPatientSummaryXn( final String patientId,
                                                           final Collection<ConceptAggregate> cancerConcepts,
                                                           final Collection<ConceptAggregate> tumorConcepts,
                                                           final Collection<ConceptAggregate> allConcepts ) {
      // TODO
      // TODO
      // TODO
      // TODO   --->>   This is redoing locations.  Need to merge without doing that.
//      LOGGER.info( "!!!  Cancers  !!!" );
//      cancerConcepts.stream()
//                    .sorted( Comparator.comparing( ConceptAggregate::getUri )
//                                       .thenComparing( ConceptAggregate::getId ) )
//                    .forEach( LOGGER::info );
//      LOGGER.info( "!!!  Tumors  !!!" );
//      tumorConcepts.stream()
//                   .sorted( Comparator.comparing( ConceptAggregate::getUri )
//                                      .thenComparing( ConceptAggregate::getId ) )
//                   .forEach( LOGGER::info );

//
//
//      final Map<ConceptAggregate, Collection<ConceptAggregate>> diagnosisMap
//            = ConceptAggregateMerger.mergeNeoplasms( neoplasmConcepts, allConcepts );
//
//      // TODO Remove keys (Cancers) with mentions < max mention count / 4 (or some settable number)
//      final int maxMentions = diagnosisMap.keySet().stream().mapToInt( c -> c.getMentions().size() ).max().orElse( 0 );
//      final Collection<ConceptAggregate> tooSmall =
//            diagnosisMap.keySet().stream().filter( c -> c.getMentions().size() < maxMentions/3 ).collect(
//                  Collectors.toList() );
//      LOGGER.info( "Removing Too Small "
//                   + tooSmall.stream().map( ConceptAggregate::getUri ).collect( Collectors.joining( ";" ) ) );
//      diagnosisMap.keySet().removeAll( tooSmall );
//
//      return summarizeConceptAggregateMap( patientId, diagnosisMap, allConcepts );
      return null;
   }

   static private PatientSummaryXn createPatientSummaryXn(
         final String patientId,
         final Map<ConceptAggregate, Collection<ConceptAggregate>> diagnosisMap,
         final Collection<ConceptAggregate> loneTumors,
         final Collection<ConceptAggregate> allConcepts ) {

//      LOGGER.info( "!!!  Cancers , Tumors !!!" );
//      final Collection<ConceptAggregate> cancerList = diagnosisMap.keySet()
//                                                                  .stream()
//                                                                  .sorted( Comparator.comparing( ConceptAggregate::getUri ) )
//                                                                  .collect( Collectors.toList() );
//      for ( ConceptAggregate cancer : cancerList ) {
//         LOGGER.info( "\n\n!!! Cancer !!!\n" + cancer.toString() );
//         diagnosisMap.get( cancer )
//                     .stream()
//                     .sorted( Comparator.comparing( ConceptAggregate::getUri ) )
//                     .forEach( t -> LOGGER.info( "\n  !! Tumor !!\n" + t.toString() ) );
//      }
//      loneTumors.forEach( t -> LOGGER.info( "\n!! Lone Tumor !!\n" + t.toString() ) );
      // TODO Remove keys (Cancers) with mentions < max mention count / 4 (or some settable number)
//      final int maxMentions = diagnosisMap.keySet()
//                                          .stream()
//                                          .mapToInt( c -> c.getMentions().size() )
//                                          .max()
//                                          .orElse( 0 );
//      final Collection<ConceptAggregate> tooSmall = diagnosisMap.keySet()
//                                                                .stream()
//                                                                // TODO for old and busted upmc brca
//                                                                .filter( c -> c.getMentions().size()*3 < maxMentions )
////                                                                .filter( c -> c.getMentions().size() < maxMentions )
//                                                                .collect( Collectors.toSet() );
//
//      final int maxSites = diagnosisMap.keySet()
//                                          .stream()
//                                       .map( ConceptAggregate::getRelatedSites )
//                                       .flatMap( Collection::stream )
//                                          .mapToInt( c -> c.getMentions().size() )
//                                          .max()
//                                          .orElse( 0 );
//      final Collection<ConceptAggregate> tooFewSites
//            = diagnosisMap.keySet()
//                         .stream()
//                         .filter( c -> c.getRelatedSites()
//                                        .stream()
//                                        .mapToInt( s -> s.getMentions().size() )
//                                        // TODO for old and busted upmc brca
//                                        .sum()*3 < maxSites )
////                                        .sum() < maxSites )
//                          .collect( Collectors.toSet() );
//      LOGGER.info( "Site Mention Counts ..." );
      diagnosisMap.keySet()
                  .forEach( c -> LOGGER.info(
                        c.getUri() + " " + String.join( ",", c.getAllUris() )
                        + "  " + c.getRelatedSites()
                                   .stream()
                                   .mapToInt( s -> s.getMentions().size() )
                                   .sum()
                        + " :\n" + c.getRelatedSites()
                                  .stream()
                                  .map( s -> String.join( ",", s.getAllUris() )
                                             + " " + s.getMentions().size() )
                                  .collect( Collectors.joining("\n") )
                         ) );

//      LOGGER.info( "Removing Too Few Sites Cancers from max " + maxSites + "/3 :\n"
//                   + tooFewSites.stream()
//                             .map( c -> c.getUri() + " : " + c.getMentions().size() )
//                             .collect( Collectors.joining( "\n" ) ) );
//      diagnosisMap.keySet().removeAll( tooFewSites );
//
//      LOGGER.info( "Removing Too Small Cancers from max " + maxMentions + "/3 :\n"
//                   + tooSmall.stream()
//                             .map( c -> c.getUri() + " : " + c.getMentions().size() )
//                             .collect( Collectors.joining( "\n" ) ) );
//      diagnosisMap.keySet().removeAll( tooSmall );

//      final Collection<ConceptAggregate> tooSmallLoners = loneTumors.stream()
//                                                                    // TODO for old and busted upmc brca
//                                                                .filter( c -> c.getMentions().size()*3 < maxMentions )
////                                                                    .filter( c -> c.getMentions().size() < maxMentions )
//                                                                    .collect( Collectors.toSet() );
//      LOGGER.info( "Removing Too Small Lone Tumors from max " + maxMentions + "/3 :\n"
//                   + tooSmallLoners.stream()
//                             .map( c -> c.getUri() + " : " + c.getMentions().size() )
//                             .collect( Collectors.joining( "\n" ) ) );
//      loneTumors.removeAll( tooSmallLoners );

//      LOGGER.info( "!!!  Cancers , Tumors !!!" );
      final Collection<ConceptAggregate> cancerList2 = diagnosisMap.keySet()
                                                                  .stream()
                                                                  .sorted( Comparator.comparing( ConceptAggregate::getUri ) )
                                                                  .collect( Collectors.toSet() );
      for ( ConceptAggregate cancer : cancerList2 ) {
         LOGGER.info( "\n\n!!! Cancer !!!\n" + cancer.toString() );
         diagnosisMap.get( cancer )
                     .stream()
                     .sorted( Comparator.comparing( ConceptAggregate::getUri ) )
                     .forEach( t -> LOGGER.info( "\n  !! Tumor !!\n" + t.toString() ) );
      }
      loneTumors.forEach( t -> LOGGER.info( "\n!! Lone Tumor !!\n" + t.toString() ) );

      LOGGER.info( "Remaining Cancers:\n"
                   + diagnosisMap.keySet().stream()
                             .map( c -> c.getUri() + " : " + c.getMentions().size() )
                             .collect( Collectors.joining( "\n" ) ) );

//      System.exit( 0 );
      return summarizeConceptAggregateMap( patientId, diagnosisMap, loneTumors, allConcepts );
   }


   ///////////////////////////////////////////////////////////////////////////////////////////////////////////


   /**
    * !!!  The Neoplasms are merged Here  !!!
    * <p>
    * Create only a single cancer summary that contains the same neoplasm for its cancer and tumor(s).
    * This is where it diverges from dPhe classic (wrt neoplasm summary).
    *
    * @param patientId    -
    * @param diagnosisMap map of cancer concepts to related tumor concepts.
    * @param allConcepts -
    * @return -
    */
   static private PatientSummaryXn summarizeConceptAggregateMap( final String patientId,
                                                               final Map<ConceptAggregate, Collection<ConceptAggregate>> diagnosisMap,
                                                                 final Collection<ConceptAggregate> loneTumors,
                                                                 final Collection<ConceptAggregate> allConcepts ) {

      final Map<ConceptAggregate,Fact> factMap = FactCreator.createFactMap( allConcepts );

      final List<Cancer> cancers = new ArrayList<>( diagnosisMap.size() );
      for ( Map.Entry<ConceptAggregate,Collection<ConceptAggregate>> diagnosisEntry : diagnosisMap.entrySet() ) {
         final Cancer cancer = CancerCreator.createCancer( diagnosisEntry.getKey(), factMap );
         final List<Tumor> tumors = new ArrayList<>( diagnosisEntry.getValue().size() );
         for ( ConceptAggregate cancerTumor : diagnosisEntry.getValue() ) {
            if ( cancerTumor == null ) {
               continue;
            }
            final Map<String,Collection<ConceptAggregate>> related = cancerTumor.getRelatedConceptMap();
            final Collection<String> removals = related.keySet().stream()
                                                       .filter( ConceptAggregateUtil::isCancerOnlyFact )
                                                       .collect( Collectors.toList() );
            LOGGER.info( "Cancer Only Removals\n" + String.join( " ; ", removals ) );
            related.keySet().removeAll( removals );
            tumors.add( TumorCreator.createTumor( cancerTumor, factMap ) );
         }
         cancer.setTumors( tumors.stream()
                                 .sorted( Comparator.comparing( Tumor::getId ) )
                                 .collect( Collectors.toList() ) );
         cancers.add( cancer );
      }

      if ( !loneTumors.isEmpty() ) {
         final Collection<Tumor> tumors = new ArrayList<>( loneTumors.size() );
         for ( ConceptAggregate loneTumor : loneTumors ) {
            final Map<String, Collection<ConceptAggregate>> related = loneTumor.getRelatedConceptMap();
            final Collection<String> removals = related.keySet()
                                                       .stream()
                                                       .filter( ConceptAggregateUtil::isCancerOnlyFact )
                                                       .collect( Collectors.toList() );
            LOGGER.info( "Cancer Only Removals\n" + String.join( " ; ", removals ) );
            related.keySet()
                   .removeAll( removals );
            tumors.add( TumorCreator.createTumor( loneTumor, factMap ) );
         }
         final Cancer unknownCancer = new Cancer();
         unknownCancer.setId( UriConstants.UNKNOWN + "_Cancer_" + System.currentTimeMillis() );
         unknownCancer.setClassUri( UriConstants.UNKNOWN );
         unknownCancer.setTumors( tumors.stream()
                                         .sorted( Comparator.comparing( Tumor::getId ) )
                                         .collect( Collectors.toList() ) );
         unknownCancer.setFactIds( Collections.emptyList() );
         unknownCancer.setRelatedFactIds( Collections.emptyMap() );
         unknownCancer.setAttributes( Collections.emptyList() );
         cancers.add( unknownCancer );
      }

//      LOGGER.info( "\n===================== NAACCR Cancer =============================" );
//      LOGGER.info( naaccrCancer );

//      final NeoplasmSummary neoplasmSummary = NeoplasmSummaryCreator.createNeoplasmSummary( naaccrCancer );
//      final NeoplasmSummary neoplasmSummary = NeoplasmSummaryCreator.createNeoplasmSummary( naaccrCancer, allConcepts );

      final PatientSummaryXn patientSummary = new PatientSummaryXn();
      patientSummary.setId( patientId );
      patientSummary.setCancers( cancers.stream()
                                       .sorted( Comparator.comparing( Cancer::getId ) )
                                       .collect( Collectors.toList() ) );
      patientSummary.setFacts( new ArrayList<>( factMap.values()
                                                       .stream()
                                                       .sorted( Comparator.comparing( Fact::getId ) )
                                                       .collect( Collectors.toList() ) ) );
      return patientSummary;
   }






//   static private Map<String, Collection<String>> mapChainedUris( final Collection<MentionCoref> patientCorefs,
//                                                                  final Map<String,Mention> mentionIdMap ) {
//      LOGGER.info( "\nMapping unique URIs to all connected URIs from all in Doc Coreference Chains.  Beginning xDoc Coref." );
//
//
//      // Map of unique URIs to all connected URIs from any coreference chain.
//      final Map<String, Collection<String>> chainedUrisMap = new HashMap<>();
//      for ( MentionCoref coref : patientCorefs ) {
//         final String[] mentionIdChain = coref.getIdChain();
//         // All unique URIs in the coreference chain.
//         final Collection<String> corefUris = Arrays.stream( mentionIdChain )
//                                                    .map( mentionIdMap::get )
//                                                    .map( Mention::getClassUri )
//                                                    .collect( Collectors.toSet() );
//         for ( String uri : corefUris ) {
//            chainedUrisMap.computeIfAbsent( uri, u -> new HashSet<>() ).addAll( corefUris );
//         }
//
//
//         LOGGER.info( "in Doc Coref Chain: (" + String.join( "," + corefUris ) + ")" );
//
//
//      }
//
//
//      chainedUrisMap.forEach( (k,v) -> LOGGER.info( "URI " + k + " in full xDoc chain (" + String.join( ",", v ) + ")" ) );
//
//
//      return chainedUrisMap;
//   }



   // NOT NECESSARY W/O inDoc Coref
//   static private Map<String, Collection<String>> mapBestRoots( final Map<String, Collection<String>> associatedUrisMap,
//                                                                final Map<String, Collection<String>> chainedUrisMap ) {
//      // Map of uri branches to their best root according to previously determined coreference chains.
//      final Map<String, Collection<String>> bestRootsMap = new HashMap<>( associatedUrisMap.size() );
//
//      for ( Map.Entry<String, Collection<String>> associatedUris : associatedUrisMap.entrySet() ) {
//         final String root = associatedUris.getKey();
//         for ( String uri : associatedUris.getValue() ) {
//            final Collection<String> chainedUris = chainedUrisMap.get( uri );
//            if ( chainedUris != null ) {
//               for ( String corefUri : chainedUris ) {
//                  for ( Map.Entry<String, Collection<String>> associated2 : associatedUrisMap.entrySet() ) {
//                     if ( associated2.getValue().stream().anyMatch( corefUri::equals ) ) {
//                        bestRootsMap.computeIfAbsent( root, u -> new HashSet<>() )
//                                    .add( associated2.getKey() );
//                        bestRootsMap.computeIfAbsent( associated2.getKey(), u -> new HashSet<>() )
//                                    .add( root );
//                     }
//                  }
//               }
//            }
//         }
//      }
//      return bestRootsMap;
//   }


//   static private Collection<Collection<String>> collectFinalBranches( final Map<String, Collection<String>> associatedUrisMap,
//                                                                       final Map<String, Collection<String>> bestRootsMap ) {
//      final Collection<Collection<String>> finalBranches = new ArrayList<>( associatedUrisMap.size() );
//      final Collection<String> usedRoots = new HashSet<>( bestRootsMap.size() );
//      for ( Map.Entry<String, Collection<String>> associated : associatedUrisMap.entrySet() ) {
//         if ( usedRoots.contains( associated.getKey() ) ) {
//            continue;
//         }
//         final Collection<String> corefRoots = bestRootsMap.get( associated.getKey() );
//         if ( corefRoots == null || corefRoots.isEmpty() ) {
//            finalBranches.add( associated.getValue() );
//         } else {
//            final Collection<String> finalCoref = new HashSet<>( associated.getValue() );
//            for ( String corefRoot : corefRoots ) {
//               finalCoref.addAll( associatedUrisMap.get( corefRoot ) );
//               usedRoots.add( corefRoot );
//            }
//            finalBranches.add( finalCoref );
//         }
//      }
//      return finalBranches;
//   }




   // TODO
   // TODO
   // TODO                    For annotationConceptInstances run the byUriRelationFinder
   //  and NonGraphedRelationFinder  (modified)
   // TODO
   // TODO
//   static private void addRelations( final Collection<JCas> docJcases,
//                                     final Collection<Collection<ConceptAggregate>> conceptAggregateSet,
//                                     final Collection<MentionRelation> patientRelations ) {
//      final Map<Mention, ConceptAggregate> mentionConceptAggregateMap
//            = mapMentionConceptAggregates( conceptAggregateSet );
//
//      // TODO  --- Do we need to add the xdoc relations?  Relations within same section type?  9-17-2020
////      CiUriRelationFinder.addRelations( docJcases, conceptAggregateSet );
//
//      for ( Collection<ConceptAggregate> conceptAggregates : conceptAggregateSet ) {
//         for ( ConceptAggregate conceptAggregate : conceptAggregates ) {
//            final Map<String, Collection<ConceptAggregate>> relatedCis
//                  = getCategoryTargetConceptMap( conceptAggregate, mentionConceptAggregateMap, patientRelations );
//            for ( Map.Entry<String, Collection<ConceptAggregate>> related : relatedCis.entrySet() ) {
//               final String type = related.getKey();
//               for ( ConceptAggregate target : related.getValue() ) {
//                  conceptAggregate.addRelated( type, target );
//               }
//            }
//         }
//      }
//   }






































//
//      !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//      !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//      !!
//      !!                ONTOLOGY CAN MESS WITH US
//      !!
//      !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//      !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//
//   The branching of the ontology and the inconsisten manner of neo4j traversal
//   can give multiple answers (non-deterministic by graph, not by rules).
//   Note that rerunning gave the same primary,
//   but the favored URI was Invasive_Carcinoma and not Non_Small_Cell_Carcinoma !
//
//
//   Invasive_Carcinoma  PatientSimple_70
//[Lung cancer,mass,INVASIVE CARCINOMA,CARCINOMA, NON-SMALL-CELL,MALIGNANT NEOPLASM,MALIGNANT NEOPLASM] 	uncertain	patient history
//         Disease_Has_Associated_Cavity
//   Lung  PatientSimple_60
//   Finding_Has_Associated_Site
//   Lung  PatientSimple_60
//   Disease_Has_Associated_Region
//   Lung  PatientSimple_60
//   Disease_Has_Primary_Anatomic_Site
//   Lung  PatientSimple_60
//   hasTumorExtent
//   Invasive Carcinoma  PatientSimple_70
//         hasDiagnosis
//   Invasive Carcinoma  PatientSimple_70
//
//
//3.A.2  MORPHOLOGY  (HISTOLOGY and BEHAVIOR)
//   Morphology seems to have a little bit of human favoratism involved ...
//   All Ontology Morphology codes for Invasive_Carcinoma: 8010/3,8000/3
//   Getting Best Histology from Morphology codes 8010/3,8000/3
//   The preferred histology is the first of the following OR the first in numerically sorted order:
//         8071 8070 8520 8575 8500 8503 8260 8250 8140 8480 8046 8041 8240 8012 8000 8010
//   This ordering came from the best overall fit to gold annotations.
//
//
// ===================== Neoplasm Invasive_Carcinoma =====================
//   Site: Lung Lung
//   Topography: C34 , 9
//   Histology: 8000
//   Behavior: 3
//   Laterality:
//   Grade: 9
//   TNM:


}
