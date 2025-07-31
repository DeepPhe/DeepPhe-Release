package org.healthnlp.deepphe.nlp.attribute.topo_minor.ovary;


import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public enum OvaryUriCollection {
   INSTANCE;

   static public OvaryUriCollection getInstance() {
      return INSTANCE;
   }

   // C48  1, 2, 8
   private Collection<String> _allPeritoneumUris;
   private final String _peritoneumUri = "Peritoneum";
   private Collection<String> _peritoneumPartUris;
//   private Collection<String> _overlappingRpUris;

   // C56
   private final String _ovaryUri = "Ovary";

   // C57
   private Collection<String> _allGenitalUris;
   private final String _fallopianTubeUri = "FallopianTube";
   private final String _broadLigamentUri = "BroadLigament";
   private Collection<String> _roundLigamentUris;
   private final String _parametriumUri = "Parametrium";
   private final String _uterineAdnexaUri = "AppendageOfTheUterus";
   private Collection<String> _otherGenitalUris;
//   private Collection<String> _overlappingGenitalUris;
   private final String _genitalTractUri = "FemaleReproductiveSystemPart";


   // Peritoneum NOS = C48.2

   OvaryUriCollection() {
      initOvaryUris();
   }


   private void initOvaryUris() {
      _peritoneumPartUris = getBranchUris( _peritoneumUri );
      _allPeritoneumUris = new HashSet<>( _peritoneumPartUris );
      _peritoneumPartUris.remove( _peritoneumUri );

      _allGenitalUris = getBranchUris( _genitalTractUri );
      _allGenitalUris.remove( _ovaryUri );
      _otherGenitalUris = new HashSet<>( _allGenitalUris );
      _allGenitalUris.add( _broadLigamentUri );
      _roundLigamentUris = getBranchUris( "RoundLigament" );
      _roundLigamentUris.remove( "RoundLigamentOfTheLiver" );
      _allGenitalUris.addAll( _roundLigamentUris );

      _otherGenitalUris.remove( _fallopianTubeUri );
      _otherGenitalUris.remove( _parametriumUri );
      _otherGenitalUris.remove( _uterineAdnexaUri );
      _otherGenitalUris.remove( _genitalTractUri );
   }


   Collection<String> getAllPeritoneumUris() {
      return _allPeritoneumUris;
   }

   Collection<String> getPeritoneumPartUris() {
      return _peritoneumPartUris;
   }

   String getPeritoneumUri() {
      return _peritoneumUri;
   }

//   Collection<String> getOverlappingRpUris() {
//      return _overlappingRpUris;
//   }


   Collection<String> getAllGenitalUris() {
      return _allGenitalUris;
   }

   String getFallopianTubeUri() {
      return _fallopianTubeUri;
   }

   String getBroadLigamentUri() {
      return _broadLigamentUri;
   }

   Collection<String> getRoundLigamentUris() {
      return _roundLigamentUris;
   }

   String getParametriumUri() {
      return _parametriumUri;
   }

   String getUterineAdnexaUri() {
      return _uterineAdnexaUri;
   }

   Collection<String> getOtherGenitalUris() {
      return _otherGenitalUris;
   }

//   Collection<String> getOverlappingGenitalUris() {
//      return _overlappingGenitalUris;
//   }

   String getGenitalTractUri() {
      return _genitalTractUri;
   }

   static private Collection<String> getBranchUris( final String root ) {
      return new HashSet<>( UriInfoCache.getInstance().getUriBranch(  root ) );
   }

}
