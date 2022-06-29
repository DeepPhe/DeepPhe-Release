package org.healthnlp.deepphe.summary.attribute.behavior;

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

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.DISEASE_HAS_FINDING;

final public class BehaviorUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _behaviorConcepts;
   final private Collection<String> _lymphBehaviorUris = new HashSet<>();

   static private final int BEHAVIOR_WINDOW = 30;

   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _behaviorConcepts == null ) {
         _lymphBehaviorUris.clear();
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                .getGraph();
         final Collection<String> malignantUris = new HashSet<>( UriConstants.getMalignantTumorUris( graphDb ) );
         // The registry (KY) uses carcinoma as proof of invasion
         malignantUris.add( "Carcinoma" );
         malignantUris.add( "Adenocarcinoma" );
         final Collection<ConceptAggregate> malignantConcepts = neoplasms.stream()
//                                                                          .filter( c -> !c.isNegated() )
                                                                          .filter( c -> c.getAllUris()
                                                                                         .stream()
                                                                                         .anyMatch( malignantUris::contains ) )
                                                                          .collect( Collectors.toSet() );
         final Collection<String> metastasisUris = new HashSet<>( UriConstants.getMetastasisUris( graphDb ) );
         metastasisUris.add( "Metastasis" );
         final Collection<ConceptAggregate> metastasisConcepts = neoplasms.stream()
//                                                                          .filter( c -> !c.isNegated() )
                        .filter( c -> c.getAllUris()
                                       .stream()
                                       .anyMatch( metastasisUris::contains ) )
                          .collect( Collectors.toSet() );
         //  Added 4/12/2022
         //  If preceding text contains "lymph node" for metastases then collect the uri for strength discount.
         for ( ConceptAggregate concept : metastasisConcepts ) {
            for ( Mention mention : concept.getMentions() ) {
               final int mentionBegin = mention.getBegin();
               if ( mentionBegin <= BEHAVIOR_WINDOW ) {
                  continue;
               }
               final Note note = NoteNodeStore.getInstance().get( mention.getNoteId() );
               if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
                  continue;
               }
               final String preText = note.getText()
                                          .substring( mentionBegin-BEHAVIOR_WINDOW, mentionBegin )
                                          .toLowerCase();
               NeoplasmSummaryCreator.addDebug( "Behavior Candidate and pretext "
                                                       + note.getText().substring( mentionBegin-BEHAVIOR_WINDOW,
                                                                                   mention.getEnd() )
                                                       + "\n" );
               if ( preText.contains( "lymph node" ) ) {
                  NeoplasmSummaryCreator.addDebug( "Tracking Behavior uri "
                                                          + mention.getClassUri()+ "\n" );
                  _lymphBehaviorUris.add( mention.getClassUri() );
               }
            }
         }

         final Collection<ConceptAggregate> behaviorConcepts = neoplasms.stream()
                                      .map( c -> c.getRelated( DISEASE_HAS_FINDING ) )
                                      .flatMap( Collection::stream )
//                                                                        .filter( c -> !c.isNegated() )
                                                                        .filter( c -> BehaviorCodeInfoStore.getBehaviorNumber( c ) >= 0 )
                                      .collect( Collectors.toSet() );
         _behaviorConcepts = new HashSet<>( malignantConcepts );
         _behaviorConcepts.addAll( metastasisConcepts );
         _behaviorConcepts.addAll( behaviorConcepts );
      }
      return _behaviorConcepts;
   }


   //  Added 4/12/2022
   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Collection<ConceptAggregate> attributes = getAttributeConcepts( neoplasms );
      if ( attributes.isEmpty() ) {
         return Collections.emptyMap();
      }
      // Switched from getAllUris to uris for affirmed mentions 3/23/2021
      final Collection<String> allUris = attributes.stream()
//                                                 .map( ConceptAggregate::getAllUris )
                                                   .map( ConceptAggregate::getMentions )
                                                   .flatMap( Collection::stream )
                                                   .filter( m -> !m.isNegated() )
                                                   .map( Mention::getClassUri )
                                                   .collect( Collectors.toSet() );
      final Map<String,Collection<String>> allUriRoots = attributes.stream()
                                                                   .map( ConceptAggregate::getUriRootsMap )
                                                                   .map( Map::entrySet )
                                                                   .flatMap( Collection::stream )
                                                                   .distinct()
                                                                   .collect( Collectors.toMap( Map.Entry::getKey,
                                                                                               Map.Entry::getValue ) );
      // Switched from getAllUris to uris for affirmed mentions 3/23/2021
      final Collection<Mention> allMentions = attributes.stream()
                                                        .map( ConceptAggregate::getMentions )
                                                        .flatMap( Collection::stream )
                                                        .filter( m -> !m.isNegated() )
                                                        .collect( Collectors.toSet() );
      final List<KeyValue<String, Double>> uriQuotients = UriScoreUtil.mapUriQuotients( allUris,
                                                                                        allUriRoots,
                                                                                        allMentions );
      final Map<String,Integer> uriStrengths = new HashMap<>();
      for ( KeyValue<String,Double> quotients : uriQuotients ) {
         final int previousStrength = uriStrengths.getOrDefault( quotients.getKey(), 0 );
         int strength = (int)Math.ceil( quotients.getValue() * 100 );
         if ( _lymphBehaviorUris.contains( quotients.getKey() ) ) {
            strength -= 25;
         }
         uriStrengths.put( quotients.getKey(), Math.max( previousStrength, strength ) );
      }
      UriInfoVisitor.applySectionAttributeUriStrengths( attributes, uriStrengths );
      UriInfoVisitor.applyHistoryAttributeUriStrengths( attributes, uriStrengths );
      return uriStrengths;
   }




}
