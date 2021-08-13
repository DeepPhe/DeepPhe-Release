package org.healthnlp.deepphe.summary.concept;


import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.util.KeyValue;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/17/2020
 */
final public class DefaultConceptAggregate implements ConceptAggregate {

   static private final IdCounter ID_COUNTER = new IdCounter();

   private final BigInteger _unique_id_num;
   private final Map<String,Collection<String>> _uriRootsMap;
   private final String _patientId;
   private Map<String,Collection<ConceptAggregate>> _relatedConceptMap;
   // TODO What we should actually do is create a map of annotations to some DocInfo object,
   // which contains patientId, docId, date, etc.
   private final Map<Mention, Date> _noteDateMap;
   private final Map<Mention, String> _noteIdMap;
   private final Date _date;
   private final String _uri;
   private final List<KeyValue<String, Double>> _uriQuotients;
   private final double _uriScore;

   public DefaultConceptAggregate( final String patientId,
                                   final Map<String,Collection<String>> uriRootsMap,
                                    final Map<String, Collection<Mention>> docMentionMap ) {
      _unique_id_num = ID_COUNTER.incrementAndGet();
      _patientId = patientId;
      _uriRootsMap = uriRootsMap;
      _date = new Date();
      _noteIdMap = new HashMap<>();
      _noteDateMap = new HashMap<>();
      for ( Map.Entry<String, Collection<Mention>> docMentions : docMentionMap.entrySet() ) {
         final String documentId = docMentions.getKey();
         docMentions.getValue().forEach( a -> addMention( a, documentId, _date ) );
      }
      _uriQuotients = UriScoreUtil.mapUriQuotients( createUriList(), uriRootsMap, getMentions() );
      final KeyValue<String,Double> bestUriScore
            = UriScoreUtil.getBestUriScore( uriRootsMap, _uriQuotients );
      _uri = bestUriScore.getKey();
      _uriScore = bestUriScore.getValue();
   }


   /**
    *
    * @param mention annotation within Concept
    * @param documentId id of the document with annotation
    * @param date       the date for the document in which the given annotation is found
    */
   final public void addMention( final Mention mention,
                                 final String documentId,
                                 final Date date ) {
      _noteIdMap.put( mention, documentId );
      if ( date != null ) {
         _noteDateMap.put( mention, date );
      }
   }

   /**
    * @return the url of the instance
    */
   @Override
   public String getUri() {
      return _uri;
   }

   @Override
   public double getUriScore() {
      return _uriScore;
   }

   @Override
   public List<KeyValue<String, Double>> getUriQuotients() {
      return _uriQuotients;
   }

   /**
    * @return [PatientId]_[DocId]_[SemanticGroup]_[HashCode]  -- now much shorter 9/18/2020
    */
   @Override
   public String getId() {
      return getPatientId() + '_' + _unique_id_num;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPatientId() {
      return _patientId;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Date getNoteDate( final Mention mention ) {
      if ( _noteDateMap != null && mention != null ) {
         final Date date = _noteDateMap.get( mention );
         if ( date != null ) {
            return date;
         }
      }
      return _date;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   final public Map<String,Collection<ConceptAggregate>> getRelatedConceptMap() {
      return _relatedConceptMap != null ? _relatedConceptMap : Collections.emptyMap();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public void addRelated( final String type, final ConceptAggregate related ) {
      if ( related == null ) {
         return;
      }
      if ( _relatedConceptMap == null ) {
         _relatedConceptMap = new HashMap<>();
      }
      _relatedConceptMap.computeIfAbsent( type, c -> new HashSet<>() ).add( related );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public void clearRelations() {
      if ( _relatedConceptMap != null ) {
         _relatedConceptMap.clear();
      }
   }



   final public Map<Mention, String> getNoteIdMap() {
      return _noteIdMap != null ? _noteIdMap : Collections.emptyMap();
   }

   private Map<Mention, Date> getNoteDateMap() {
      return _noteDateMap != null ? _noteDateMap : Collections.emptyMap();
   }

   private List<String> createUriList() {
      return getMentions().stream()
                           .map( Mention::getClassUri )
                           .collect( Collectors.toList() );
   }

   public Map<String,Collection<String>> getUriRootsMap() {
      return _uriRootsMap;
   }









   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return toText();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof ConceptAggregate && ((ConceptAggregate)other).getId().equals( getId() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return getId().hashCode();
   }


   /**
    * Like an AtomicLong using a BigInteger to go beyond the signed 64bit long max.
    */
   static private final class IdCounter {
      private final AtomicReference<BigInteger> _reference = new AtomicReference<>();
      private IdCounter() {
         _reference.set( BigInteger.valueOf( 0 ) );
      }

      private BigInteger incrementAndGet() {
         for ( ; ; ) {
            final BigInteger current = _reference.get();
            final BigInteger next = current.add( BigInteger.ONE );
            if ( _reference.compareAndSet( current, next ) ) {
               return next;
            }
         }
      }
   }

}
