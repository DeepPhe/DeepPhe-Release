package org.healthnlp.deepphe.nlp.attribute.topo_minor.lung;


import org.healthnlp.deepphe.nlp.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public enum LungUriCollection {
   INSTANCE;

   static public LungUriCollection getInstance() {
      return INSTANCE;
   }

   private final String _lungUri = "Lung";
   private Collection<String> _bronchusUris;
   private Collection<String> _upperLobeUris;
   private final String _middleLobeUri = "MiddleLobeOfTheRightLung";
   private Collection<String> _lowerLobeUris;
   private Collection<String> _allLungUris;

   // Trachea is 33.9

   LungUriCollection() {
      initLungUris();
   }

   private void initLungUris() {
      _bronchusUris = getBranchUris( "Bronchus" );
      _upperLobeUris = getBranchUris( "UpperLobeOfTheLung" );
      _lowerLobeUris = getBranchUris( "LowerLobeOfTheLung" );
      _allLungUris = new HashSet<>();
      _allLungUris.addAll( _bronchusUris );
      _allLungUris.addAll( _upperLobeUris );
      _allLungUris.add( _middleLobeUri );
      _allLungUris.addAll( _lowerLobeUris );
      _allLungUris.add( _lungUri );
   }

   public Collection<String> getAllLungUris() {
      return _allLungUris;
   }

   public String getLungUri() {
      return _lungUri;
   }

   public Collection<String> getBronchusUris() {
      return _bronchusUris;
   }

   public Collection<String> getUpperLobeUris() {
      return _upperLobeUris;
   }

   public String getMiddleLobeUri() {
      return _middleLobeUri;
   }

   public Collection<String> getLowerLobeUris() {
      return _lowerLobeUris;
   }

   static private Collection<String> getBranchUris( final String root ) {
      return new HashSet<>( UriInfoCache.getInstance().getUriBranch(  root ) );
   }

}
