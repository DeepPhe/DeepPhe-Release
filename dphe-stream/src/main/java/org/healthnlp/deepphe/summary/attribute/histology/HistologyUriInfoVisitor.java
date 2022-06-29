package org.healthnlp.deepphe.summary.attribute.histology;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_DIAGNOSIS;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_TUMOR_EXTENT;

final public class HistologyUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _histologyConcepts;
   final private Collection<String> _exactHistologyUris = new HashSet<>();

   static private final int HISTOLOGY_WINDOW = 30;
   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _histologyConcepts == null ) {
         _exactHistologyUris.clear();
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                .getGraph();
         final Collection<String> neoplasmUris = UriConstants.getNeoplasmUris( graphDb );
         final Collection<ConceptAggregate> certainNeoplasm = neoplasms.stream()
//                                                                         .filter( c -> !c.isNegated() )
                                                                         .filter( c -> c.getAllUris()
                                                                                        .stream()
                                                                                        .anyMatch( neoplasmUris::contains ) )
                                                                         .collect( Collectors.toSet() );
         _histologyConcepts = new HashSet<>( certainNeoplasm );
         neoplasms.stream()
                    .map( c -> c.getRelated( HAS_DIAGNOSIS, HAS_TUMOR_EXTENT ) )
                    .flatMap( Collection::stream )
//                    .filter( c -> !c.isNegated() )
//                   .filter( c -> !c.isUncertain() )
                   .forEach( _histologyConcepts::add );
//         if ( !_histologyConcepts.isEmpty() ) {
//            return _histologyConcepts;
//         }
//         neoplasms.stream()
//                 .filter( c -> !c.isNegated() )
//                  .filter( c -> c.getAllUris()
//                                 .stream()
//                                 .anyMatch( neoplasmUris::contains ) )
//                  .forEach( _histologyConcepts::add );
//         _histologyConcepts = new HashSet<>( certainNeoplasm );
//         neoplasms.stream()
//                  .map( c -> c.getRelated( HAS_DIAGNOSIS, HAS_TUMOR_EXTENT ) )
//                  .flatMap( Collection::stream )
//                  .filter( c -> !c.isNegated() )
//                  .forEach( _histologyConcepts::add );


         //  Added 3/31/2022
         //  If text contains "histologic type: [type]" for any detected aggregates only those are returned.
         final Collection<ConceptAggregate> histologies = new HashSet<>();
         for ( ConceptAggregate aggregate : _histologyConcepts ) {
            for ( Mention mention : aggregate.getMentions() ) {
               final int mentionBegin = mention.getBegin();
               if ( mentionBegin <= HISTOLOGY_WINDOW ) {
                  continue;
               }
               final Note note = NoteNodeStore.getInstance().get( mention.getNoteId() );
               if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
                  continue;
               }
               final String preText = note.getText()
                                          .substring( mentionBegin-HISTOLOGY_WINDOW, mentionBegin )
                                          .toLowerCase();
//               NeoplasmSummaryCreator.addDebug( "Histology Candidate and pretext "
//                                                       + note.getText().substring( mentionBegin-HISTOLOGY_WINDOW, mention.getEnd() )
//                                                       + "\n" );
               //  "Preop diagnosis"?   "positive for"?
               if ( preText.contains( "histologic type:" )
                    || preText.contains( "diagnosis:" )
                    || preText.contains( "consistent with" ) ) {
                  NeoplasmSummaryCreator.addDebug( "Trimming to histology candidate "
                                                          + aggregate.getCoveredText() + "\n" );
                  histologies.add( aggregate );
                  _exactHistologyUris.add( mention.getClassUri() );
               }
            }
         }
         if ( !histologies.isEmpty() ) {
            _histologyConcepts.retainAll( histologies );
         }
      }
      return _histologyConcepts;
   }


//   /**
//    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
//    * @param neoplasms -
//    * @return the histology score as it may be increased by text surrounding a mention.
//    */
//   @Override
//   public Map<String,Integer> getAttributeUriStrengths1( final Collection<ConceptAggregate> neoplasms ) {
//      final Map<String,Integer> uriStrengths = UriInfoVisitor.super.getAttributeUriStrengths( neoplasms );
//      if ( _exactHistologyUris.isEmpty() ) {
//         return uriStrengths;
//      }
//      for ( String uri : _exactHistologyUris ) {
//         final int strength = uriStrengths.get( uri );
//         NeoplasmSummaryCreator.addDebug( "Adding 10% strength to Histology Candidate " + uri
//                                                 + " strength " + strength + "\n" );
//         uriStrengths.put( uri, strength + 10 );
//      }
//      return uriStrengths;
//   }

   /**
    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
    * @param neoplasms -
    * @return the histology score as it may be increased by text surrounding a mention.
    */
   @Override
   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> attributes = getAttributeConcepts( neoplasms );
      if ( attributes.isEmpty() ) {
         return Collections.emptyMap();
      }
      // Switched from getAllUris to uris for affirmed mentions 3/23/2021
      final Collection<Mention> allMentions = attributes.stream()
                                                        .map( ConceptAggregate::getMentions )
                                                        .flatMap( Collection::stream )
                                                        .filter( m -> ( !m.isNegated()
                                                                        || _exactHistologyUris.contains( m.getClassUri() ) ) )
                                                        .collect( Collectors.toSet() );
//      final Collection<String> allUris = attributes.stream()
////                                                 .map( ConceptAggregate::getAllUris )
//                                                   .map( ConceptAggregate::getMentions )
//                                                   .flatMap( Collection::stream )
//                                                   .filter( m -> !m.isNegated() )
//                                                   .map( Mention::getClassUri )
//                                                   .collect( Collectors.toSet() );
      final Collection<String> allUris = allMentions.stream()
                                                    .map( Mention::getClassUri )
                                                    .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allUriRoots = new HashMap<>();
      for ( ConceptAggregate attribute : attributes ) {
         allUriRoots.putAll( attribute.getUriRootsMap() );
      }
//      final Map<String,Collection<String>> allUriRoots = attributes.stream()
//                                                                   .map( ConceptAggregate::getUriRootsMap )
//                                                                   .map( Map::entrySet )
//                                                                   .flatMap( Collection::stream )
//                                                                   .distinct()
//                                                                   .collect( Collectors.toMap( Map.Entry::getKey,
//                                                                                               Map.Entry::getValue ) );
      // Switched from getAllUris to uris for affirmed mentions 3/23/2021
//      final Collection<Mention> allMentions = attributes.stream()
//                                                        .map( ConceptAggregate::getMentions )
//                                                        .flatMap( Collection::stream )
//                                                        .filter( m -> !m.isNegated() )
//                                                        .collect( Collectors.toSet() );
      final List<KeyValue<String, Double>> uriQuotients = UriScoreUtil.mapUriQuotients( allUris,
                                                                                        allUriRoots,
                                                                                        allMentions );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         int strength = (int)Math.ceil( quotients.getValue() * 100 );
         if ( _exactHistologyUris.contains( quotients.getKey() ) ) {
            strength += 25;
         }
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      UriInfoVisitor.applySectionAttributeUriStrengths( attributes, uriStrengths );
      UriInfoVisitor.applyHistoryAttributeUriStrengths( attributes, uriStrengths );
      return uriStrengths;
   }

//   @Override
//   public boolean applySectionStrengths() {
//      return true;
//   }

//   /**
//    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
//    * @param neoplasms -
//    * @return the grade score (0-5) * 20.
//    */
//   @Override
//   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
//      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
//      return concepts.stream()
//                     .map( ConceptAggregate::getAllUris )
//                     .flatMap( Collection::stream )
//                     .distinct()
//                     .collect( Collectors.toMap( Function.identity(),
//                                                 HistologyUriInfoVisitor::getHistologyStrength ) );
//   }
//
//   static private int getHistologyStrength( final String uri ) {
//      return Math.max( 0, Math.min( 100, GradeCodeInfoStore.getUriGradeNumber( uri ) * 20 ) );
//   }

}
