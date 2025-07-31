package org.healthnlp.deepphe.nlp.concept;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.ner.group.dphe.DpheGroup;
import org.healthnlp.deepphe.neo4j.node.Codification;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.nlp.score.UriScoreUtil;
import org.apache.ctakes.core.util.KeyValue;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.nlp.util.IdCreator;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @since {9/9/2023}
 */
public class DefaultUriConcept implements UriConcept {

    private final String _id;
    private final String _patientId;
    private final String _uri;
    private final DpheGroup _dpheGroup;
    private final Map<String,Collection<String>> _uriRootsMap;
    private final Map<String,Collection<UriConceptRelation>> _relationsMap;
    // TODO What we should actually do is create a map of annotations to some DocInfo object,
    //  which contains patientId, docId, date, etc.
    private final Map<Mention, Date> _noteDateMap;
    private final Map<Mention, String> _noteIdMap;
    private final Map<String,Collection<String>> _codifications;
    private double _confidence;
//    private final double _groupedConfidence;
    private final String _value;



    public <M extends Mention> DefaultUriConcept( final String patientId, final String patientTime,
                               final Map<String, Collection<String>> uriRootsMap,
                               final Map<String, Collection<M>> docMentionMap ) {
//                                                  final double groupedConfidence ) {
        _id = IdCreator.createId( patientId, patientTime, "C", UriConceptCreator.getIdCounter() );
        _patientId = patientId;
        _uriRootsMap = uriRootsMap;
        _relationsMap = new HashMap<>();
        _noteIdMap = new HashMap<>();
        _noteDateMap = new HashMap<>();
        _codifications = new HashMap<>();
//        _groupedConfidence = groupedConfidence;
        final Date date = new Date();
        final Collection<String> values = new HashSet<>();
        for ( Map.Entry<String, Collection<M>> docMentions : docMentionMap.entrySet() ) {
            final String documentId = docMentions.getKey();
            docMentions.getValue().forEach( a -> addMention( a, documentId, date ) );
            docMentions.getValue().stream()
                       .map( Mention::getValue )
                       .filter( Objects::nonNull )
                       .filter( v -> !v.isEmpty() ).forEach( values::add );
        }
        _value = String.join( ";", values );
        // Quotients are not used except to calculate uriScore, which goes into confidence.
        // TODO swap out to use combination of confidence calculator (mention confidence) and distance down branch
        final List<KeyValue<String, Double>> uriQuotients
                = UriScoreUtil.mapUriQuotients( getAllUrisList(), uriRootsMap, getMentions() );
        final KeyValue<String,Double> bestUriScore
                = UriScoreUtil.getBestUriScore( uriRootsMap, uriQuotients );
        _uri = bestUriScore.getKey();
        _dpheGroup = UriInfoCache.getInstance().getDpheGroup( _uri );
        if ( _dpheGroup == DpheGroup.UNKNOWN ) {
            LogFileWriter.add( "DefaultUriConcept no group for " + _uri );
        }
    }

    public void setConfidence( final double confidence ) {
        _confidence = confidence;
    }

//    @Override
//    public double getGroupedConfidence() {
//        return _groupedConfidence;
//    }

    /**
     * @return the uri of the instance
     */
    @Override
    public String getUri() {
        return _uri;
    }

    /**
     * @return the semantic grouping of the instance
     */
    @Override
    public DpheGroup getDpheGroup() {
        return _dpheGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,Collection<String>> getUriRootsMap() {
        return _uriRootsMap;
    }

    @Override
    public String getValue() {
        return _value;
    }

    /**
     * @return [PatientId]_[summaryDateTime]_C_[index]
     */
    @Override
    public String getId() {
        return _id;
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
    public void addMention( final Mention mention,
                            final String documentId,
                            final Date date ) {
        _noteIdMap.put( mention, documentId );
        if ( date != null ) {
            _noteDateMap.put( mention, date );
        }
        for ( Codification codification : mention.getCodifications() ) {
            _codifications.computeIfAbsent( codification.getSource(), s -> new HashSet<>() )
                          .addAll( codification.getCodes() );
        }
    }

    @Override
    public Map<String,Collection<String>> getCodifications() {
        return _codifications;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Mention, String> getNoteIdMap() {
        return _noteIdMap != null ? _noteIdMap : Collections.emptyMap();
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public Date getNoteDate( final Mention mention ) {
//        if ( _noteDateMap != null && mention != null ) {
//            final Date date = _noteDateMap.get( mention );
//            if ( date != null ) {
//                return date;
//            }
//        }
//        return _date;
//    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,Collection<UriConceptRelation>> getAllRelations() {
        return Collections.unmodifiableMap( _relationsMap );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRelation(final UriConceptRelation relation ) {
        if ( relation == null ) {
            return;
        }
        _relationsMap.computeIfAbsent( relation.getType(), c -> new HashSet<>() ).add( relation );
    }

    @Override
    public void addRelatedConcept( final String type, final UriConcept related ) {
        throw new UnsupportedOperationException( "DefaultUriConcept.addRelatedConcept is not supported." );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearRelations() {
        _relationsMap.clear();
    }

    /**
     * For everything except cancer and mass this is based solely upon mention confidence.
     * For cancer and mass the relations are considered.
     * @return confidence between 0 and 1
     */
    public double getConfidence() {
        return _confidence;
        // decrease for negation and/or uncertainty ?
        // Multiply by some factor for relation confidence ?  Neoplasm and Mass only ...
//        final double mentionsConfidence = ConfidenceCalculator.getMentionsConfidence( getMentions() );
//        _confidence = ConfidenceCalculator.reduceTo1( mentionsConfidence, ConfidenceCalculator.MaxAt100.AT_200K );
//        _confidence = mentionsConfidence;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toLongText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( final Object other ) {
        return other instanceof UriConcept && ((UriConcept)other).getId().equals( getId() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }


}
