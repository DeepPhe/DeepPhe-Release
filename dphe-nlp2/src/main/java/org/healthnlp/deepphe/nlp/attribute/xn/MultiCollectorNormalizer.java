package org.healthnlp.deepphe.nlp.attribute.xn;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConcept;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/25/2024}
 */
public class MultiCollectorNormalizer  implements AttributeInfoCollector, XnAttributeNormalizer {

   private UriConcept _neoplasm;
   private final Collection<CombinedCollectorNormalizer> _combined;

   public MultiCollectorNormalizer( Collection<CombinedCollectorNormalizer> combined ) {
      _combined = combined;
   }

   public void init( final UriConcept neoplasm, final String... relationType ) {
      _neoplasm = neoplasm;
      _combined.forEach( c -> c.init( neoplasm, relationType ) );
   }

   // Shouldn't really be used.
   public UriConcept getNeoplasm() {
      return _neoplasm;
   }

   // Shouldn't really be used.
   public String[] getRelationTypes() {
      final Collection<String> relations = new HashSet<>();
      for ( AttributeInfoCollector collector : _combined ) {
         relations.addAll( Arrays.asList( collector.getRelationTypes() ) );
      }
      return relations.toArray( new String[0] );
   }

   // Shouldn't really be used.
   public Collection<UriConceptRelation> getRelations() {
      return _combined.stream().map( AttributeInfoCollector::getRelations )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toSet() );
   }

   // Shouldn't really be used.
   public Collection<UriConcept> getUriConcepts() {
      return _combined.stream().map( AttributeInfoCollector::getUriConcepts )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toSet() );   }

   public void init( final AttributeInfoCollector infoCollector,
                     final Map<String, List<XnAttributeValue>> dependencies, final long mentionCount ) {
      _combined.forEach( c -> c.init( infoCollector, dependencies, mentionCount ) );
   }

   public List<XnAttributeValue> getValues() {
      return _combined.stream().map( XnAttributeNormalizer::getValues )
                      .flatMap( Collection::stream )
                      .distinct()
            .sorted( Comparator.comparingDouble( XnAttributeValue::getConfidence ).reversed() )
                      .collect( Collectors.toList() );
   }


}
