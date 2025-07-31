package org.healthnlp.deepphe.nlp.attribute.topo_minor.crc;


import org.healthnlp.deepphe.nlp.uri.UriInfoCache;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public enum CrcUriCollection {
   INSTANCE;

   static public CrcUriCollection getInstance() {
      return INSTANCE;
   }

   private Collection<String> _allColonUris;
   private final String _cecumUri = "Cecum";
   private final String _appendixUri = "Appendix";
   private final Collection<String> _ascendingUris = Arrays.asList( "RightColon", "AscendingColon" );
   private final String _hepaticUri = "HepaticFlexure";
   private final String _transverseUri = "TransverseColon";
   private final String _splenicUri = "SplenicFlexure";
   private final Collection<String> _descendingUris = Arrays.asList( "LeftColon", "DescendingColon" );
   private final String _sigmoidUri = "SigmoidColon";
   private Collection<String> _colonUris;
   // C19.9
   private final Collection<String> _rectosigmoidUris = Arrays.asList( "RectosigmoidColon", "RectosigmoidRegion" );
   // C20.9 = "Rectum"
   private final String _rectumUri = "Rectum";


   private final String _anusUri = "Anus";
   private final Collection<String> _analCanalUris = Arrays.asList( "AnalCanal", "AnalColumn" );
   private final Collection<String> _cloacogenicZoneUris = Arrays.asList( "CloacalSphincter", "AnalTransitionalZone" );
   private final String _anorectalUri = "AnoRectum";
   private final Collection<String> _allAnusUris = new HashSet<>();

   CrcUriCollection() {
      initColonUris();
   }

   private void initColonUris() {
      _colonUris = UriInfoCache.getInstance().getUriBranch( "Colon" );
      _colonUris.add( _appendixUri );
      _colonUris.removeAll( _rectosigmoidUris );
      _colonUris.remove( _rectumUri );
      _colonUris.remove( _anusUri );
      _colonUris.removeAll( _analCanalUris );
      _colonUris.removeAll( _cloacogenicZoneUris );
      _allColonUris = new HashSet<>( _colonUris );
      _colonUris.remove( _cecumUri );
      _colonUris.removeAll( _ascendingUris );
      _colonUris.remove( _hepaticUri );
      _colonUris.remove( _transverseUri );
      _colonUris.remove( _splenicUri );
      _colonUris.removeAll( _descendingUris );
      _colonUris.remove( _sigmoidUri );

      _allAnusUris.add( _anusUri );
      _allAnusUris.addAll( _analCanalUris );
      _allAnusUris.addAll( _cloacogenicZoneUris );

      // Need laterality for ascending/descending determination,
      // but cannot use icdo laterality code as for C18 the code is always 9 (no laterality)
//      ALL_COLON_URIS.add( "Right" );
//      ALL_COLON_URIS.add( "Left" );
   }


   public Collection<String> getAllColonUris() {
      return _allColonUris;
   }

   public String getCecumUri() {
      return _cecumUri;
   }

   public String getAppendixUri() {
      return _appendixUri;
   }

   public Collection<String> getAscendingUris() {
      return _ascendingUris;
   }

   public String getHepaticUri() {
      return _hepaticUri;
   }

   public String getTransverseUri() {
      return _transverseUri;
   }

   public String getSplenicUri() {
      return _splenicUri;
   }

   public Collection<String> getDescendingUris() {
      return _descendingUris;
   }

   public String getSigmoidUri() {
      return _sigmoidUri;
   }

   public Collection<String> getColonUris() {
      return _colonUris;
   }

   public Collection<String> getRectosigmoidUris() {
      return _rectosigmoidUris;
   }

   public Collection<String> getAllAnusUris() {
      return _allAnusUris;
   }

   public String getAnusUri() {
      return _anusUri;
   }

   public Collection<String> getAnalCanalUris() {
      return _analCanalUris;
   }

   public Collection<String> getCloacogenicZoneUris() {
      return _cloacogenicZoneUris;
   }

   public String getAnorectalUri() {
      return _anorectalUri;
   }


}
