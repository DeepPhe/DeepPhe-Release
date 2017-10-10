package org.apache.ctakes.dictionary.lookup2.bsv;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.DotLogger;
import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.concept.DefaultConcept;
import org.apache.ctakes.dictionary.lookup2.concept.OwlConcept;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator;
import org.apache.ctakes.dictionary.lookup2.ontology.OwlParserUtil;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.LookupUtil;
import org.apache.ctakes.dictionary.lookup2.util.TuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.ValidTextUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/5/2016
 */
final public class BsvParserUtil {

   private BsvParserUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "BsvParserUtil" );


   static private final RareWordTermMapCreator.CuiTerm EMPTY_CUI_TERM = new RareWordTermMapCreator.CuiTerm( "", "" );
   static private final CuiTuiUriTerm EMPTY_CUI_TUI_URI_TERM = new CuiTuiUriTerm( "", "", "" );

   /**
    * Create a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects
    * by parsing all of the bsv files in the same directory as the owl file.
    *
    * @param owlFilePath path to file containing ontology owl
    * @return collection of all valid terms read from bsv files
    */
   static public Collection<RareWordTermMapCreator.CuiTerm> parseCuiTermFiles( final String owlFilePath ) {
      File owlDir;
      try {
         final File owlParent = FileLocator.getFile( owlFilePath );
         owlDir = owlParent.getParentFile();
      } catch ( IOException ioE ) {
         return new HashSet<>();
      }
      final FilenameFilter bsvFilter = ( dir, name ) -> name.toLowerCase().endsWith( ".bsv" );
      final File[] bsvFiles = owlDir.listFiles( bsvFilter );
      if ( bsvFiles == null || bsvFiles.length == 0 ) {
         return new HashSet<>();
      }
      LOGGER.info( "Loading Dictionary BSV Files in " + owlDir.getPath() + ":" );
      final Collection<RareWordTermMapCreator.CuiTerm> cuiTerms = new HashSet<>();
      try ( DotLogger dotter = new DotLogger() ) {
         for ( File bsvFile : bsvFiles ) {
            cuiTerms.addAll( parseCuiTermFile( bsvFile.getPath() ) );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not load Dictionary BSV Files in " + owlDir.getPath() );
      }
      LOGGER.info( "Dictionary BSV Files loaded" );
      return cuiTerms;
   }

   /**
    * Create a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects
    * by parsing a bsv file.  The file should be in the following format:
    * <p>
    * CUI|Text|TUI|URI
    * </p>
    *
    * @param bsvFilePath path to file containing term rows and bsv columns
    * @return collection of all valid terms read from the bsv file
    */
   static private Collection<RareWordTermMapCreator.CuiTerm> parseCuiTermFile( final String bsvFilePath ) {
      final Collection<RareWordTermMapCreator.CuiTerm> cuiTerms = new ArrayList<>();
      try ( final BufferedReader reader = new BufferedReader( new InputStreamReader( FileLocator
            .getAsStream( bsvFilePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            if ( ValidTextUtil.isCommentLine( line ) || line.trim().isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] columns = LookupUtil.fastSplit( line, '|' );
            final RareWordTermMapCreator.CuiTerm cuiTerm = createCuiTuiTerm( columns );
            if ( cuiTerm == null ) {
               LOGGER.debug( "Bad BSV line " + line + " in " + bsvFilePath );
            } else if ( !cuiTerm.equals( EMPTY_CUI_TERM ) ) {
               cuiTerms.add( cuiTerm );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      return cuiTerms;
   }

   /**
    * @param columns four columns representing CUI,Text,TUI,URI respectively
    * @return a term created from the columns or null if the columns are malformed
    */
   static private RareWordTermMapCreator.CuiTerm createCuiTuiTerm( final String... columns ) {
      if ( columns.length < 4 ) {
         return null;
      }
      final String uri = columns[ 3 ].trim();
      if ( OwlParserUtil.getInstance().isUnwantedUri( uri ) ) {
         return EMPTY_CUI_TERM;
      }
      final String text = columns[ 1 ].trim().toLowerCase();
      if ( !ValidTextUtil.isValidText( text ) ) {
         return EMPTY_CUI_TERM;
      }
      final String cui = columns[ 0 ];
      return new RareWordTermMapCreator.CuiTerm( cui, text );
   }


   /**
    * Create a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects
    * by parsing all of the bsv files in the same directory as the owl file.
    *
    * @param owlFilePath path to file containing ontology owl
    * @return collection of all valid terms read from bsv files
    */
   static public Map<Long, Concept> parseConceptFiles( final String owlFilePath ) {
      File owlDir;
      try {
         final File owlParent = FileLocator.getFile( owlFilePath );
         owlDir = owlParent.getParentFile();
      } catch ( IOException ioE ) {
         return new HashMap<>();
      }
      final FilenameFilter bsvFilter = ( dir, name ) -> name.toLowerCase().endsWith( ".bsv" );
      final File[] bsvFiles = owlDir.listFiles( bsvFilter );
      if ( bsvFiles == null || bsvFiles.length == 0 ) {
         return new HashMap<>();
      }
      final Map<Long, Concept> bsvConcepts = new HashMap<>();
      LOGGER.info( "Loading Concept BSV Files in " + owlDir.getPath() + ":" );
      try ( DotLogger dotter = new DotLogger() ) {
         for ( File bsvFile : bsvFiles ) {
            bsvConcepts.putAll( parseConceptFile( bsvFile.getPath() ) );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not load Concept BSV Files in " + owlDir.getPath() );
      }
      return bsvConcepts;
   }

   static private Map<Long, Concept> parseConceptFile( final String bsvFilePath ) {
      final Collection<CuiTuiUriTerm> cuiTuiTerms = parseCuiTuiUriTermFile( bsvFilePath );
      final Map<Long, Concept> conceptMap = new HashMap<>( cuiTuiTerms.size() );
      for ( CuiTuiUriTerm cuiTuiTerm : cuiTuiTerms ) {
         if ( OwlParserUtil.getInstance().isUnwantedUri( cuiTuiTerm.getUri() ) ) {
            continue;
         }
         final CollectionMap<String, String, ? extends Collection<String>> codes
               = new HashSetMap<>();
         codes.placeValue( Concept.TUI, TuiCodeUtil.getAsTui( cuiTuiTerm.getTui() ) );
         codes.placeValue( OwlConcept.URI_CODING_SCHEME, cuiTuiTerm.getUri() );
         conceptMap.put( CuiCodeUtil.getInstance().getCuiCode( cuiTuiTerm.getCui() ),
               new DefaultConcept( cuiTuiTerm.getCui(), Concept.PREFERRED_TERM_UNKNOWN, codes ) );
      }
      return conceptMap;
   }


   /**
    * Create a collection of {@link org.apache.ctakes.dictionary.lookup2.dictionary.RareWordTermMapCreator.CuiTerm} Objects
    * by parsing a bsv file.  The file can be in one of two columnar formats:
    * <p>
    * CUI|Text|TUI|URI
    * </p>
    *
    * @param bsvFilePath file containing term rows and bsv columns
    * @return collection of all valid terms read from the bsv file
    */
   static private Collection<CuiTuiUriTerm> parseCuiTuiUriTermFile( final String bsvFilePath ) {
      final Collection<CuiTuiUriTerm> cuiTuiTerms = new ArrayList<>();
      try ( final BufferedReader reader
                  = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( bsvFilePath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            if ( ValidTextUtil.isCommentLine( line ) || line.trim().isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] columns = LookupUtil.fastSplit( line, '|' );
            final CuiTuiUriTerm cuiTuiTerm = createCuiTuiUriTerm( columns );
            if ( cuiTuiTerm == null ) {
               LOGGER.debug( "Bad BSV line " + line + " in " + bsvFilePath );
            } else if ( !cuiTuiTerm.equals( EMPTY_CUI_TUI_URI_TERM ) ) {
               cuiTuiTerms.add( cuiTuiTerm );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      return cuiTuiTerms;
   }


   /**
    * @param columns two or three columns representing CUI,Text or CUI,TUI,Text respectively
    * @return a term created from the columns or null if the columns are malformed
    */
   static private CuiTuiUriTerm createCuiTuiUriTerm( final String... columns ) {
      if ( columns.length < 4 ) {
         return null;
      }
      if ( !ValidTextUtil.isValidText( columns[ 1 ].trim().toLowerCase() ) ) {
         return EMPTY_CUI_TUI_URI_TERM;
      }
      final String uri = columns[ 3 ].trim();
      if ( OwlParserUtil.getInstance().isUnwantedUri( uri ) ) {
         return EMPTY_CUI_TUI_URI_TERM;
      }
      final String cui = columns[ 0 ];
      // default for an empty tui column is tui 0 = unknown
      final String tui = columns[ 2 ].trim().isEmpty() ? "T000" : columns[ 2 ].trim().split( "," )[ 0 ];
      return new CuiTuiUriTerm( cui, tui, uri );
   }

   static public class CuiTuiUriTerm {

      final private String __cui;
      final private String __tui;
      final private String __uri;
      final private int __hashcode;

      public CuiTuiUriTerm( final String cui, final String tui, final String uri ) {
         __cui = cui;
         __tui = tui;
         __uri = uri;
         __hashcode = (__cui + "_" + __tui + "_" + __uri).hashCode();
      }

      public String getCui() {
         return __cui;
      }

      public String getTui() {
         return __tui;
      }

      public String getUri() {
         return __uri;
      }

      public boolean equals( final Object value ) {
         return value instanceof CuiTuiUriTerm
               && __uri.equals( ((CuiTuiUriTerm) value).__uri )
               && __tui.equals( ((CuiTuiUriTerm) value).__tui )
               && __cui.equals( ((CuiTuiUriTerm) value).__cui );
      }

      public int hashCode() {
         return __hashcode;
      }
   }


}
