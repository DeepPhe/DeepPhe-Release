package org.healthnlp.deepphe.summary.attribute;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.*;
import java.util.stream.Collectors;

public interface SpecificAttribute {

   enum EvidenceLevel {
      DIRECT_EVIDENCE,
      INDIRECT_EVIDENCE,
      NOT_EVIDENCE;
   }

   NeoplasmAttribute toNeoplasmAttribute();

   static NeoplasmAttribute createAttribute( final String name, final String value, final String uri ) {
      return createAttribute( name, value, uri, Collections.emptyMap(), Collections.emptyList() );
   }

   static NeoplasmAttribute createAttribute( final String name, final String value,
                                             final String uri,
                                             final Map<EvidenceLevel, Collection<Mention>> evidence,
                                             final List<Integer> features ) {
      return createAttributeWithFeatures( name, value, uri,
                                          new ArrayList<>( evidence.getOrDefault( EvidenceLevel.DIRECT_EVIDENCE,
                                                                                  Collections.emptyList() ) ),
                                          new ArrayList<>( evidence.getOrDefault( EvidenceLevel.INDIRECT_EVIDENCE,
                                                                                  Collections.emptyList() ) ),
                                          new ArrayList<>( evidence.getOrDefault( EvidenceLevel.NOT_EVIDENCE,
                                                                                  Collections.emptyList() ) ),
                                          features );
   }

   static NeoplasmAttribute createAttribute( final String name, final String value,
                                             final String uri,
                                             final List<Mention> directEvidence,
                                             final List<Mention> indirectEvidence,
                                             final List<Mention> notEvidence,
                                             final List<Integer> features ) {
      return createAttributeWithFeatures( name, value, uri, directEvidence, indirectEvidence, notEvidence, features );
   }

      static NeoplasmAttribute createAttributeWithFeatures( final String name, final String value,
                                                            final String uri,
                                                            final List<Mention> directEvidence,
                                             final List<Mention> indirectEvidence,
                                             final List<Integer> features  ) {
      return createAttributeWithFeatures( name, value, uri, directEvidence, indirectEvidence, indirectEvidence,
                                          features );
   }

   // Todo  prettyName    prettyValue
   static NeoplasmAttribute createAttributeWithFeatures( final String name, final String value,
                                                         final String uri,
                                                         final List<Mention> directEvidence,
                                             final List<Mention> indirectEvidence,
                                             final List<Mention> notEvidence,
                                             final List<Integer> features ) {
      NeoplasmSummaryCreator.addDebug( name + "=" + value + ":" + uri + "\n" );
      final NeoplasmAttribute attribute = new NeoplasmAttribute();
      attribute.setName( name );
      attribute.setValue( value );
      attribute.setClassUri( uri );
      attribute.setId( uri + "_" + System.currentTimeMillis() );
      attribute.setDirectEvidence( directEvidence );
      attribute.setIndirectEvidence( indirectEvidence );
      attribute.setNotEvidence( notEvidence );
      attribute.setConfidenceFeatures( features );
      return attribute;
   }



   static Map<EvidenceLevel, Collection<Mention>> mapEvidence( final Collection<ConceptAggregate> neoplasmConcepts,
                                                               final Collection<ConceptAggregate> patientConcepts,
                                                               final Collection<ConceptAggregate> allConcepts ) {
      final Map<EvidenceLevel,Collection<Mention>> evidenceMap = new HashMap<>();
      Arrays.stream( EvidenceLevel.values() )
            .forEach( l -> evidenceMap.put( l, new HashSet<>() ) );
      final Collection<Mention> neoplasmMentions = getAllMentions( neoplasmConcepts );
      final Collection<Mention> patientMentions = getAllMentions( patientConcepts );
      final Collection<Mention> otherMentions = getAllMentions( allConcepts  );
      patientMentions.removeAll( neoplasmMentions );
      otherMentions.removeAll( neoplasmMentions );
      otherMentions.removeAll( patientMentions );
      evidenceMap.put( EvidenceLevel.DIRECT_EVIDENCE, neoplasmMentions );
      evidenceMap.put( EvidenceLevel.INDIRECT_EVIDENCE, patientMentions );
      evidenceMap.put( EvidenceLevel.NOT_EVIDENCE, otherMentions );
      return evidenceMap;
   }


   static Collection<Mention> getAllMentions( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }

   static Collection<Mention> getMentions( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getMentions )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }

   static Collection<String> getMainUris( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getUri )
                     .collect( Collectors.toSet() );
   }

   static Collection<String> getAllUris( final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .map( ConceptAggregate::getAllUris )
                     .flatMap( Collection::stream )
                     .collect( Collectors.toSet() );
   }


   static Collection<ConceptAggregate> getIfUriIsMain( final String uri,
                                                       final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .filter( c -> c.getUri().equals( uri ) )
                     .collect( Collectors.toSet() );
   }


   static Collection<ConceptAggregate> getIfUriIsAny( final String uri, final Collection<ConceptAggregate> concepts ) {
      return concepts.stream()
                     .filter( c -> c.getAllUris().contains( uri ) )
                     .collect( Collectors.toSet() );
   }


   static int getBranchCountsSum( final Map<String, Integer> conceptBranchCounts ) {
      return conceptBranchCounts.values()
                                .stream()
                                .mapToInt( i -> i )
                                .sum();
   }




}
