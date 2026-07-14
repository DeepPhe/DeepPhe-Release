package org.apache.ctakes.ner.detail;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class Details {

   private final String _preferredText;
   private final String _group;
   private final String _cui;
   private final int _tui;
   private final String _uri;
   private final Map<String, Collection<String>> _codesMap;
   private final int _hashcode;

   public Details( final String preferredText,
                   final String group,
                   final String cui,
                   final int tui,
                   final String uri,
                   final String codesText ) {
      _preferredText = preferredText;
      _group = group;
      _cui = cui;
      _tui = tui;
      _uri = uri;
      _codesMap = createCodesMap( codesText );
      _hashcode = (preferredText + group + cui + uri).hashCode() + _codesMap.hashCode();
   }

   public String getGroupName() {
      return _group;
   }

   public String getPreferredText() {
      return _preferredText;
   }

   public String getCui() {
      return _cui;
   }

   public int getTui() {
      return _tui;
   }

   public String getUri() {
      return _uri;
   }

   public Map<String, Collection<String>> getCodesMap() {
      return _codesMap;
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
      if ( object instanceof Details ) {
         final Details other = (Details)object;
         return _preferredText.equals( other._preferredText )
               && _group.equals( other._group )
               && _cui.equals( other._cui )
               && _tui == other._tui
               && _uri.equals( other._uri )
               && _codesMap.equals( other._codesMap );
      }
      return false;
   }

   public int hashCode() {
      return _hashcode;
   }

}
