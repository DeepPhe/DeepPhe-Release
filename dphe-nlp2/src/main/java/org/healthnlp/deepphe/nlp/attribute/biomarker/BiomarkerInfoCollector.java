package org.healthnlp.deepphe.nlp.attribute.biomarker;

import org.healthnlp.deepphe.nlp.attribute.newInfoStore.DefaultInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class BiomarkerInfoCollector extends DefaultInfoCollector {

   private final Collection<String> _wantedUris = new HashSet<>();

   public void setWantedUris( final String... uris ) {
      _wantedUris.addAll( Arrays.asList( uris ) );
   }

//   public Collection<String> getRelationTypes() {
//      return Collections.singletonList( RelationConstants.has_Biomarker );
//   }

   public Collection<UriConceptRelation> getRelations() {
//      NeoplasmSummaryCreator.addDebug( "BiomarkerInfoCollector getting relations for "
//                                       + String.join( ",", _wantedUris ) +
//                                       " " + getNeoplasm()
//                                             .getRelations( getRelationTypes()
//                                                                  .toArray( new String[0] ) )
//                                             .stream().map( ConceptAggregateRelation::getTarget )
//                                             .map( CrConceptAggregate::getCoveredText ).collect( Collectors.joining(
//                                                   ",") ) + "  --> "
//                                       + getNeoplasm().getRelations( getRelationTypes().toArray( new String[0] ) )
//                                                     .stream()
//                                                     .filter( hasWantedUri )
//                                                      .map( ConceptAggregateRelation::getTarget )
//                                                      .map( CrConceptAggregate::getCoveredText )
//                                                     .collect( Collectors.joining(",") )
//                                                     + "\n" );
      return getNeoplasm().getUriConceptRelations( getRelationTypes() )
                          .stream()
                          .filter( isWanted )
                          .collect( Collectors.toSet() );
   }

//   private final Predicate<UriConceptRelation> isWantedUri = r -> _wantedUris.contains( r.getTarget().getUri() );
//
//   private final Predicate<UriConceptRelation> hasWantedUri
//         = r -> r.getTarget()
//                 .getAllUris()
//                 .stream()
//                 .anyMatch( _wantedUris::contains );

   private final Predicate<UriConceptRelation> isWanted = r -> isWantedUri( r ) || hasWantedUri( r );

   private boolean hasWantedUri( final UriConceptRelation r ) {
      return _wantedUris.contains( r.getTarget().getUri() );
   }

   private boolean isWantedUri( final UriConceptRelation r ) {
      return r.getTarget()
              .getAllUris()
              .stream()
              .anyMatch( _wantedUris::contains );
   }


}
