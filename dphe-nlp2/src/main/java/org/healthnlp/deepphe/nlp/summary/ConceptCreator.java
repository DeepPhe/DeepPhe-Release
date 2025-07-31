package org.healthnlp.deepphe.nlp.summary;

import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.healthnlp.deepphe.neo4j.node.Codification;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.xn.Concept;
import org.healthnlp.deepphe.neo4j.node.xn.ConceptRelation;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {12/29/2023}
 */
final public class ConceptCreator {

    private ConceptCreator() {}

    static public Concept createConcept( final UriConcept uriConcept) {
        final Concept concept = new Concept();
        concept.setDpheGroup( uriConcept.getDpheGroup().getName() );
        if ( UriInfoCache.getInstance().getPrefText( uriConcept.getUri() ).isEmpty() ) {
            LogFileWriter.add( "ConceptCreator empty prefText for " + uriConcept.getUri() );
        }
        concept.setPreferredText( UriInfoCache.getInstance().getPrefText( uriConcept.getUri() ) );
        concept.setId( uriConcept.getId() );
        concept.setClassUri( uriConcept.getUri() );
        concept.setdConfidence( uriConcept.getConfidence() );
        concept.setNegated( uriConcept.isNegated() );
        concept.setUncertain( uriConcept.isUncertain() );
        concept.setHistoric( uriConcept.inPatientHistory() );
        final List<String> mentionIds = MentionCreator.sortMentions( uriConcept.getMentions() )
                .stream().map( Mention::getId ).collect( Collectors.toList() );
        concept.setMentionIds( mentionIds );
        final List<Codification> codifications = new ArrayList<>();
        for ( Map.Entry<String,Collection<String>> coding : uriConcept.getCodifications().entrySet() ) {
            final Codification codification = new Codification();
            codification.setSource( coding.getKey() );
            codification.setCodes( new ArrayList<>( coding.getValue() ) );
            codifications.add( codification );
        }
        concept.setCodifications( codifications );
        return concept;
    }

    static public List<Concept> createConcepts( final Collection<UriConcept> uriConcepts ) {
        return uriConcepts.stream()
                .filter( c -> !CONST.ATTR_SUBJECT_FAMILY_MEMBER.equals( c.getSubject() ) )
                .map( ConceptCreator::createConcept )
                .collect( Collectors.toList() );
    }


    static public Map<UriConcept, Concept> createUriConceptConceptMap( final Collection<UriConcept> uriConcepts ) {
        return uriConcepts.stream()
                .filter( c -> !CONST.ATTR_SUBJECT_FAMILY_MEMBER.equals( c.getSubject() ) )
                .collect( Collectors.toMap( Function.identity(), ConceptCreator::createConcept ) );
    }


    static public List<Concept> sortConcepts(final Collection<Concept> concepts ) {
        return concepts.stream().sorted( CONCEPT_COMPARATOR ).collect( Collectors.toList() );
    }


//    static public List<ConceptRelation> createConceptRelations( final Collection<UriConcept> uriConcepts ) {
//        final List<ConceptRelation> relations = new ArrayList<>();
//        for ( UriConcept source : uriConcepts ) {
//            final String sourceId = source.getId();
//            for ( Map.Entry<String,Collection<UriConcept>> entry : source.getRelatedConceptMap().entrySet() ) {
//                final String type = entry.getKey();
//                for ( UriConcept target : entry.getValue() ) {
//                    final ConceptRelation relation = new ConceptRelation();
////                    relation.setId( sourceId + "_" + type + "_" + target.getId() );
//                    relation.setType( type );
//                    relation.setSourceId( sourceId );
//                    relation.setTargetId( target.getId() );
//                    relations.add( relation );
//                }
//            }
//        }
//        return relations;
//    }

    static public List<ConceptRelation> createConceptRelations( final Collection<UriConcept> uriConcepts ) {
        final List<ConceptRelation> relations = new ArrayList<>();
        final Collection<String> validIDs = uriConcepts.stream().map( UriConcept::getId ).collect( Collectors.toSet() );
        for ( UriConcept source : uriConcepts ) {
            final String sourceId = source.getId();
            for ( Map.Entry<String,Collection<UriConceptRelation>> typeRelations : source.getAllRelations().entrySet() ) {
                final String type = typeRelations.getKey();
                for ( UriConceptRelation uriConceptRelation : typeRelations.getValue() ) {
                    final String targetId = uriConceptRelation.getTarget().getId();
                    if ( !validIDs.contains( targetId ) ) {
                        continue;
                    }
                    final ConceptRelation relation = new ConceptRelation();
                    relation.setType( type );
                    relation.setSourceId( sourceId );
                    relation.setTargetId( targetId );
                    relation.setdConfidence( uriConceptRelation.getConfidence() );
                    relations.add( relation );
                }
            }
        }
        return relations;
    }



    static public List<ConceptRelation> sortConceptRelations(final Collection<ConceptRelation> relations ) {
        return relations.stream()
                .sorted( Comparator.comparing( ConceptRelation::getType ) )
                .collect( Collectors.toList() );
    }


    static private final Comparator<Concept> CONCEPT_COMPARATOR = (c1, c2) -> {
        final int typeCompare = String.CASE_INSENSITIVE_ORDER.compare( c1.getDpheGroup(), c2.getDpheGroup() );
        if ( typeCompare != 0 ) {
            return typeCompare;
        }
        final int confidenceCompare = Double.compare( c1.getdConfidence(), c2.getdConfidence() );
        if ( confidenceCompare != 0 ) {
            return -1 * confidenceCompare;
        }
        return String.CASE_INSENSITIVE_ORDER.compare( c1.getClassUri(), c2.getClassUri() );
    };

}
