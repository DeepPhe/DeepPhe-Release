package org.healthnlp.deepphe.nlp.attribute.xn;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.nlp.attribute.newInfoStore.DefaultInfoCollector;
import org.healthnlp.deepphe.nlp.concept.UriConceptRelation;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
abstract public class FindingsInfoCollector extends DefaultInfoCollector {

   static private final Collection<String> _uris = new HashSet<>();

   public FindingsInfoCollector() {
      initWantedUris();
   }

   protected abstract String getWantedRootUri();

   protected Collection<String> getWantedUris() {
      return _uris;
   }

   public void initWantedUris() {
      if ( _uris.isEmpty() ) {
         _uris.addAll( UriInfoCache.getInstance().getUriBranch( getWantedRootUri() ) );
      }
   }

   static private boolean isWantedTarget( final UriConceptRelation relation ) {
      return _uris.contains( relation.getTarget().getUri() );
   }

   public Collection<UriConceptRelation> getRelations() {
      return getNeoplasm().getUriConceptRelations( RelationConstants.HAS_FINDING )
                          .stream()
                          .filter( FindingsInfoCollector::isWantedTarget )
                          .collect( Collectors.toSet() );
   }

}
