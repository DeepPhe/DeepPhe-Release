package org.apache.ctakes.dictionary.lookup.cased.detailer;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

/**
 * @author SPF , chip-nlp
 * @since {3/5/2022}
 */
final public class Details {
   static public final Details EMPTY_DETAILS = new Details( "", "", "", (short)0 );

   private final Collection<SemanticTui> _semanticTuis = EnumSet.noneOf( SemanticTui.class );
   private final String _preferredText;
   private final String _uri;
   private final short _rank;

   public Details( final String tuis, final String preferredText, final String uri, final short rank ) {
      if ( !tuis.isEmpty() ) {
         Arrays.stream( StringUtil.fastSplit( tuis, '|' ) )
               .map( SemanticTui::getTuiFromCode )
               .forEach( _semanticTuis::add );
      }
      _preferredText = preferredText;
      _uri = uri;
      _rank = rank;
   }

   public Collection<SemanticTui> getTuis() {
      return _semanticTuis;
   }

   public String getPreferredText() {
      return _preferredText;
   }

   public String getUri() {
      return _uri;
   }

   public short getRank() {
      return _rank;
   }

}
