package org.apache.ctakes.dictionary.lookup.cased.detailer;


import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class BsvListDetailer implements TermDetailer {

   static public final String DETAILER_TYPE = "BSV_LIST";

   static private final Logger LOGGER = Logger.getLogger( "BsvListDetailer" );


   private final InMemoryDetailer _delegate;

   public BsvListDetailer( final String name, final UimaContext uimaContext ) {
      this( name, EnvironmentVariable.getEnv( name + "_list", uimaContext ) );
   }

   public BsvListDetailer( final String name, final String bsvList ) {
      final Map<String, Details> detailsMap = parseList( name, bsvList );
      LOGGER.info( "Parsed " + detailsMap.size() + " Term Details for " + name );
      _delegate = new InMemoryDetailer( name, detailsMap );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegate.getName();
   }


   /**
    * {@inheritDoc}
    * @param cui
    * @return
    */
   @Override
   public Details getDetails( final String cui ) {
      return _delegate.getDetails( cui );
   }


   /**
    * Create a map of {@link Details} Objects
    * by parsing a bsv file.  The file should have a columnar format:
    * <p>
    * CUI|Code
    * </p>
    *
    * @param bsvList path to file containing term rows and bsv columns
    * @return map of all cuis and codes read from the bsv file
    */
   static private Map<String, Details> parseList( final String name, final String bsvList ) {
      if ( bsvList.isEmpty() ) {
         LOGGER.error( "List of term encodings is empty for " + name );
         return Collections.emptyMap();
      }
      final Map<String, Details> detailsMap = new HashMap<>();
      for ( String encoding : StringUtil.fastSplit( bsvList, '|' ) ) {
         final String[] columns = StringUtil.fastSplit( encoding, ':' );
         if ( columns.length != 5 ) {
            LOGGER.warn( "Improper values for Term Details " + encoding );
            continue;
         }
         detailsMap.put( columns[0], new Details( columns[1], columns[2], columns[3], getShort( columns[4] ) ) );
      }
      return detailsMap;
   }

   static private short getShort( final String shorty ) {
      if ( shorty.isEmpty() ) {
         return (short)0;
      }
      try {
         return Short.parseShort( shorty );
      } catch ( NumberFormatException nfE ) {
         //
      }
      return (short)0;
   }

}
