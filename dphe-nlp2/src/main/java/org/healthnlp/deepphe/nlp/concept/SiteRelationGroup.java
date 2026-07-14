package org.healthnlp.deepphe.nlp.concept;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {9/9/2023}
 */
public interface SiteRelationGroup extends RelationGroup {

    // TODO - in uriconcept make a siteRelations group and a non-siteRelations group.
    //  Have a getSitesGroup() and getNonSitesGroup()

    default Collection<UriConcept> getRelatedSites() {
        return getRelatedConceptMap().entrySet()
                .stream()
                .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
                .map( Map.Entry::getValue)
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
    }


    default Collection<String> getRelatedSiteMainUris() {
        return getRelatedSites().stream()
                .map( UriConcept::getUri )
                .collect( Collectors.toSet() );
    }

    default Collection<String> getRelatedSiteAllUris() {
        return getRelatedSites().stream()
                .map( UriConcept::getAllUris )
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
    }

    default Map<String,Collection<UriConcept>> getNonLocationRelationMap() {
        return  getRelatedConceptMap().entrySet()
                .stream()
                .filter( e -> !RelationConstants.isLocationRelation( e.getKey() ) )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }


}
