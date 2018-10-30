package org.healthnlp.deepphe.fact;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/25/2018
 */
public interface BodySiteOwner {

   FactList getBodySite();

   void setBodySite( FactList bodySite );

   default void addBodySite( final BodySiteFact bodySite ) {
      final List<Fact> myBodySite = getBodySite();
      if ( FactHelper.isMissingFact( myBodySite, bodySite ) ) {
         myBodySite.add( bodySite );
      }
   }

   default void appendBodySites( final BodySiteOwner otherOwner ) {
      final List<Fact> myBodySite = getBodySite();
      otherOwner.getBodySite()
                .stream()
                .filter( s -> FactHelper.isMissingFact( myBodySite, s ) )
                .forEach( myBodySite::add );
   }

   default String getBodySiteSnippet() {
      final FactList bodysites = getBodySite();
      if ( bodysites == null || bodysites.isEmpty() ) {
         return "";
      }
      return " | location: " + bodysites.stream()
                                        .map( Fact::getSummaryText )
                                        .collect( Collectors.joining( " | location: " ) );
   }

}
