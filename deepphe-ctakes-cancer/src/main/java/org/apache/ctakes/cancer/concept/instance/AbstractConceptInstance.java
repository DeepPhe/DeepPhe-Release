package org.apache.ctakes.cancer.concept.instance;


import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/4/2018
 */
abstract public class AbstractConceptInstance implements ConceptInstance {

   static private AtomicLong _ID_NUM = new AtomicLong( 0 );

   private final long _unique_id_num;
   private final String _uri;
   private final String _patientId;
   private Map<String,Collection<ConceptInstance>> _related;
   // TODO What we should actually do is create a map of annotations to some DocInfo object,
   // which contains patientId, docId, date, etc.
   private Map<IdentifiedAnnotation, Date> _documentDates;
   private Map<IdentifiedAnnotation, String> _documentIds;
   private Date _date;

   /**
    *
    * @param patientId -
    */
   AbstractConceptInstance( final String patientId, final String uri ) {
      _unique_id_num = _ID_NUM.incrementAndGet();
      _patientId = patientId;
      _uri = uri;
      _date = new Date();
   }

   /**
    *
    * @param annotation annotation within Concept
    * @param documentId id of the document with annotation
    * @param date       the date for the document in which the given annotation is found
    */
   final protected void addAnnotation( final IdentifiedAnnotation annotation,
                                       final String documentId,
                                       final Date date ) {
      if ( _documentIds == null ) {
         _documentIds = new HashMap<>();
      }
      _documentIds.put( annotation, documentId );
      if ( date != null ) {
         if ( _documentDates == null ) {
            _documentDates = new HashMap<>();
         }
         _documentDates.put( annotation, date );
      }
   }

   /**
    *
    * @return document identifiers as single text string joined by underscore
    */
   @Override
   public String getJoinedDocumentId() {
      return _documentIds.values().stream()
                         .distinct()
                         .collect( Collectors.joining( "_") );
   }

   /**
    * @return document identifiers as single text string joined by underscore
    */
   @Override
   public Collection<String> getDocumentIds() {
      return _documentIds.values().stream()
                         .distinct()
                         .collect( Collectors.toList() );
   }

   /**
    * @return the url of the instance
    */
   @Override
   public String getUri() {
      return _uri;
   }

   /**
    * @return [PatientId]_[DocId]_[SemanticGroup]_[HashCode]
    */
   @Override
   public String getId() {
      return getPatientId() + '_' + getUri() + '_' + _unique_id_num;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPatientId() {
      return _patientId;
   }

   /**
    * @return all represented Identified Annotations
    */
   @Override
   public Collection<IdentifiedAnnotation> getAnnotations() {
      if ( _documentIds == null ) {
         return Collections.emptyList();
      }
      return _documentIds.keySet().stream()
                         .sorted( Comparator.comparing( IdentifiedAnnotation::getBegin ) )
                         .collect( Collectors.toList() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getDocumentId( final IdentifiedAnnotation annotation ) {
      return _documentIds.get( annotation );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Map<String,Collection<IdentifiedAnnotation>> getDocAnnotations() {
      final Map<String,Collection<IdentifiedAnnotation>> docAnnotations = new HashMap<>();
      for ( Map.Entry<IdentifiedAnnotation,String> annotationDoc : _documentIds.entrySet() ) {
         docAnnotations.computeIfAbsent( annotationDoc.getValue(), d -> new ArrayList<>() )
                       .add( annotationDoc.getKey() );
      }
      return docAnnotations;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Date getDocumentDate( final IdentifiedAnnotation annotation ) {
      if ( _documentDates != null && annotation != null ) {
         final Date date = _documentDates.get( annotation );
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
   final public Map<String,Collection<ConceptInstance>> getRelated() {
      return _related != null ? _related : Collections.emptyMap();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public void addRelated( final String type, final ConceptInstance related ) {
      if ( related == null ) {
         return;
      }
      if ( _related == null ) {
         _related = new HashMap<>();
      }
      _related.computeIfAbsent( type, c -> new HashSet<>() ).add( related );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   final public void clearRelations() {
      if ( _related != null ) {
         _related.clear();
      }
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
      return other.toString().equals( toString() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return toString().hashCode();
   }

}

