package org.apache.ctakes.dictionary.lookup2.concept;

import org.apache.ctakes.cancer.owl.UriAnnotationCache;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/23/2015
 */
final public class OwlConcept implements Concept {

   static private final Logger LOGGER = Logger.getLogger( "OwlConcept" );

   static public final String URI_CODING_SCHEME = "OWL_URI";
   static public final String NULL_TUI = "T000";

   final private String _cui;
   final private String _tui;
   final private String _uri;
   final private String _preferredText;

   public OwlConcept( final String cui, final String tui, final String uri, final String preferredText ) {
      _cui = cui;
      _tui = tui;
      _uri = uri;
      _preferredText = preferredText;
   }

   public String getUri() {
      return _uri;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getCui() {
      return _cui;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPreferredText() {
      return (_preferredText != null && !_preferredText.isEmpty()) ? _preferredText : PREFERRED_TERM_UNKNOWN;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodeNames() {
      return Arrays.asList( Concept.TUI, URI_CODING_SCHEME );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getCodes( final String codeType ) {
      if ( Concept.TUI.equals( codeType ) ) {
         return Collections.singletonList( _tui );
      } else if ( URI_CODING_SCHEME.equals( codeType ) ) {
         return Collections.singletonList( _uri );
      }
      return Collections.emptyList();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<Integer> getCtakesSemantics() {
      final Integer semanticGroupId
            = UriAnnotationCache.getInstance().getUriSemanticRoot( _uri );
      return Collections.singletonList( semanticGroupId );
   }

   /**
    * Always return false, otherwise it is disregarded as useless
    * {@inheritDoc}
    */
   @Override
   public boolean isEmpty() {
      return false;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object value ) {
      return value instanceof OwlConcept && _uri.equals( ((OwlConcept) value).getUri() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode() {
      return _uri.hashCode();
   }

}
