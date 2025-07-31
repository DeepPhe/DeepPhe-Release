package org.healthnlp.deepphe.nlp.attribute.histology;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.core.util.log.LogFileWriter;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.nlp.util.DebugLogger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public enum TopoMorphValidator {
INSTANCE;

   static public TopoMorphValidator getInstance() {
      return INSTANCE;
   }

   // Ex: C000, LIP
   private final Map<String,String> _siteClasses = new HashMap<>();
   private final Map<String,String> _siteCodes = new HashMap<>();

   // Ex: C000, [8000/3,8001/1,8002/3]
   private final Map<String, Collection<String>> _topoMorphs = new HashMap<>();

   // Ex: 800, Neoplasm
//   private final Map<String,String> _histoClasses = new HashMap<>();
//   private final Map<String,String> _histoCodes = new HashMap<>();
   private final Map<String,Collection<String>> _broadMorphCodes = new HashMap<>();

   // Ex: 8000/3, "Neoplasm, Malignant"
//   private final Map<String,String> _morphClasses = new HashMap<>();
//   private final Map<String,String> _exactMorphCodes = new HashMap<>();
   private final Map<String,Collection<String>> _exactMorphCodes = new HashMap<>();

   // There are "primary" morphology codes where the main histology type is the same as the subtype.
// C340-C343,C348-C349|Bronchus|807|Squamous_Cell_Carcinoma|8070/3|Squamous_Cell_Carcinoma
// C340-C343,C348-C349|Bronchus|804|Non_Small_Cell_Carcinoma|8046/3|Non_Small_Cell_Carcinoma
//   private final Map<String,String> _primaryMorphs = new HashMap<>();


   TopoMorphValidator() {
      parseValidationFile();
   }

   public String getSiteClass( final String siteCode ) {
      return _siteClasses.getOrDefault( siteCode, "UNKNOWN" );
   }

   public String getSiteCode( final String siteClass ) {
      return _siteCodes.getOrDefault( siteClass, "" );
   }

   public Collection<String> getValidTopoMorphs( final String topoCode ) {
      return _topoMorphs.getOrDefault( topoCode.replace( ".", "" ), Collections.emptyList() );
   }

//   public String getHistoCode( final String histologyClass ) {
//      return _histoCodes.getOrDefault( histologyClass, "" );
//   }

   public Collection<String> getExactMorphCode( final String morphologyClass ) {
//      LogFileWriter.add( "TopoMorphValidator.getExactMorphCodes " + morphologyClass + " "
//                                       + _exactMorphCodes.getOrDefault( morphologyClass, Collections.emptyList() ) );
      return _exactMorphCodes.getOrDefault( morphologyClass, Collections.emptyList() );
   }

//   public Collection<String> getBroadMorphCode( final String morphologyClass ) {
//      return _broadMorphCodes.getOrDefault( morphologyClass, Collections.emptyList() );
//   }
public Collection<String> getBroadHistoCode( final String morphologyClass ) {
//   return _broadMorphCodes.getOrDefault( morphologyClass, Collections.emptyList() );
   // Changed 04/07/2022.
   return Collections.emptyList();
}

   
   private void parseValidationFile() {
//      LogFileWriter.add( "Parsing Exact Codes from org/healthnlp/deepphe/icdo/DpheHistologySites.bsv" );
      File file = new File( "" );
      try {
         file = FileLocator.getFile( "org/healthnlp/deepphe/icdo/DpheHistologySites.bsv" );
      } catch ( FileNotFoundException fnfE ) {
         System.err.println( fnfE.getMessage() );
         System.exit( -1 );
      }
//      final Map<String,Collection<String>> primaryMorphs = new HashMap<>();
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         int i = 0;
         String line = reader.readLine();
         while ( line != null ) {
            i++;
            if ( i == 1 || line.isEmpty() || line.startsWith( "//" ) ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length < 6 ) {
               line = reader.readLine();
               continue;
            }
            final Collection<String> topoCodes = parseTopoCodes( splits[ 0 ].trim() );
            final String siteDescription = splits[ 1 ].trim();
            final String histology = splits[ 2 ].trim();
            final String morphologyBroad = splits[ 3 ].trim();
            final String morphologyCode = splits[ 4 ].trim();
            final String morphologyExact = splits[ 5 ].trim();

            // Ex: C000, LIP
//            topoCodes.stream().map( c -> c.substring( 0,3 ) ).distinct().forEach( c -> _siteClasses.put( c, siteDescription ) );
//            topoCodes.stream().map( c -> c.substring( 0,3 ) ).distinct().forEach( c -> _siteCodes.put( siteDescription, c ) );
            topoCodes.stream().distinct().forEach( c -> _siteClasses.put( c, siteDescription ) );
            topoCodes.stream().distinct().forEach( c -> _siteCodes.put( siteDescription, c ) );
            // Ex: C000, [8000/3,8001/1,8002/3]
            topoCodes.forEach( c -> _topoMorphs.computeIfAbsent( c, n -> new HashSet<>() ).add( morphologyCode ) );
            // Ex: 800, Neoplasm
//            _histoCodes.put( histoDescription, histology );
            // Ex: "Neoplasm, Malignant", 8000/3
//            _broadMorphCodes.computeIfAbsent( morphologyBroad, h -> new HashSet<>() ).add( morphologyCode );
            // 04/07/2022 Took out the broad morphs.  They are too imprecise to help with a final 4 digit histology.
//            _broadMorphCodes.computeIfAbsent( morphologyBroad, h -> new HashSet<>() ).add( histology + "0" );
//            for ( int post=0; post<10; post++ ) {
//               _broadMorphCodes.computeIfAbsent( morphologyBroad, h -> new HashSet<>() )
//                               .add( histology + post );
//            }
//            String prev = _exactMorphCodes.computeIfAbsent( morphologyExact, morphologyCode );
//            if ( prev != null && !prev.equals( morphologyCode ) ) {
//               LogFileWriter.add( "Previous morph " + prev
//                                                               + " does not match " + morphologyCode
//                                                               + " for " + morphologyExact );
//            }
            _exactMorphCodes.computeIfAbsent( morphologyExact, c -> new HashSet<>() ).add( morphologyCode );

//            if ( histoDescription.equals( morphoDescription ) ) {
//               primaryMorphs.computeIfAbsent( histoDescription, h -> new HashSet<>() )
//                            .add( morphology );
//            }

            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         Logger.getLogger( "TopoMorphValidator" ).error( "parseValidationFile " + ioE.getMessage() );
      }
//      for ( Map.Entry<String,Collection<String>> entry : primaryMorphs.entrySet() ) {
//         _primaryMorphs.put( entry.getKey(),
//                             entry.getValue()
//                                  .stream()
//                                  .filter( m -> !m.startsWith( "800" )
//                                                && !m.startsWith( "801" ) )
//                                  .min( Comparator.naturalOrder() )
//                                  .orElse( "" ) );
//      }
   }

   static private Collection<String> parseTopoCodes( final String codeLine ) {
      final String[] commaCodes = StringUtil.fastSplit( codeLine, ',' );
      if ( commaCodes.length > 1 ) {
         return Arrays.stream( commaCodes )
                      .map( TopoMorphValidator::parseTopoCodes )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toSet() );
      }
      final String[] dashCodes = StringUtil.fastSplit( codeLine, '-' );
      if ( dashCodes.length == 1 ) {
         return Collections.singletonList( codeLine.trim() );
      }
      if ( dashCodes.length != 2 ) {
         Logger.getLogger( "TopoMorphValidator" ).error( "Illegal TopoCodes " + codeLine );
         return Collections.emptyList();
      }
      final int low = parseTopoInt( dashCodes[ 0 ] );
      final int high = parseTopoInt( dashCodes[ 1 ] );
      return getTopoRange( low, high );
   }

   static private int parseTopoInt( final String topoCode ) {
      try {
         final String topoInt = topoCode.trim().substring( 1 );
         return Integer.parseInt( topoInt );
      } catch ( NumberFormatException nfE ) {
         System.err.println( "ParseTopoInt " + topoCode + " " + nfE.getMessage() );
         return 0;
      }
   }

   static private Collection<String> getTopoRange( final int low, final int high ) {
      final Collection<String> topoRange = new HashSet<>();
      for ( int i=low; i<=high; i++ ) {
         topoRange.add( String.format( "C%03d", i ) );
      }
      return topoRange;
   }


//   public static void main( final String ... args ) {
//      final String filePath = args[ 0 ];
//      TopoMorphValidator.getInstance().parseValidation( filePath );
//   }


}
