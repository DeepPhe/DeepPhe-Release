package org.apache.ctakes.ner.detail;

import org.apache.ctakes.core.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {11/8/2023}
 */
final public class JoinedDetails {

   private final String _preferredText;
   private final Collection<Details> _allDetails;

//   private final Collection<String> _groupings;
//   private final Collection<String> _cuis;
//   private final Collection<Integer> _tuis;
//   private final Collection<String> _uris;
//   private final Map<String, Collection<String>> _codesMap;
   private final int _hashcode;

   public JoinedDetails( final String preferredText, final Collection<Details> allDetails ) {
      _preferredText = preferredText;
      _allDetails = allDetails;
//      _grouping = grouping;
//      _cui = cui;
//      _tui = tui;
//      _uri = uri;
//      _codesMap = createCodesMap( codesText );
//      _hashcode = (preferredText + grouping + cui + uri).hashCode() + _codesMap.hashCode();
      _hashcode = _allDetails.hashCode();
   }

   static private Map<String,Collection<String>> createCodesMap( final String codesText ) {
      final String[] encodings = StringUtil.fastSplit( codesText, '|' );
      if ( encodings.length == 0 ) {
         return Collections.emptyMap();
      }
      final Map<String,Collection<String>> codesMap = new HashMap<>( encodings.length );
      for ( String encoding :encodings ) {
         final String[] codeEntries = encoding.split( encoding, '=' );
         if ( codeEntries.length != 2 ) {
            continue;
         }
         final String schema = codeEntries[0].trim();
         if ( schema.isEmpty() ) {
            continue;
         }
         final String[] codeEntry = StringUtil.fastSplit( codeEntries[1], ' ' );
         if ( codeEntry.length == 0 ) {
            continue;
         }
         final Collection<String> codes = Arrays.stream( codeEntry )
                                                .map( String::trim )
                                                .filter( c -> !c.isEmpty() )
                                                .collect( Collectors.toSet() );
         if ( !codes.isEmpty() ) {
            // Just in case the column had the same code schema more than once.
            codesMap.computeIfAbsent( schema, s -> new HashSet<>() ).addAll( codes );
         }
      }
      return codesMap;
   }


   public boolean equals( final Object object ) {
//      if ( object instanceof Details ) {
//         final Details other = (Details)object;
//         return _preferredText.equals( other._preferredText )
//               && _grouping.equals( other._grouping )
//               && _cui.equals( other._cui )
//               && _tui == other._tui
//               && _uri.equals( other._uri )
//               && _codesMap.equals( other._codesMap );
//      }
//      return false;
      return object instanceof JoinedDetails && _allDetails.equals( ((JoinedDetails)object)._allDetails );
   }

   public int hashCode() {
      return _hashcode;
   }


}
