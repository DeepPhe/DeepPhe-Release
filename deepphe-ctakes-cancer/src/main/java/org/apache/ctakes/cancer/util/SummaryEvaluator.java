package org.apache.ctakes.cancer.util;


import org.apache.ctakes.cancer.uri.UriUtil;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.cancer.summary.writer.AbstractNeoplasmBsvWriter.PATIENT_ID;
import static org.apache.ctakes.cancer.summary.writer.AbstractNeoplasmBsvWriter.SUMMARY_ID;
import static org.healthnlp.deepphe.neo4j.RelationConstants.*;

/**
 * See https://www.aclweb.org/anthology/S16-1165
 *
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/9/2019
 */
final public class SummaryEvaluator {

   static private final Logger LOGGER = Logger.getLogger( "SummaryEvaluator" );

   public static void main( final String... args ) {
      if ( args == null || args.length != 2 ) {
         System.err.println("Example: java SummaryEvaluator <gold_summary_file> <system_summary_file" );
         System.exit(-1);
      }

      try {
         final File goldFile = FileLocator.getFile( args[ 0 ] );
         final File systemFile = FileLocator.getFile( args[ 1 ] );
         scoreSystem( goldFile.getName(), goldFile, systemFile );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit(-1);
      }
   }


   static private void scoreSystem( final String neoplasmType, final File goldFile, final File systemFile ) {
      final List<String> goldProperties = readHeader( goldFile );
      final Collection<String> requiredProperties = getRequiredProperties( goldProperties );
      final Collection<String> otherProperties = getOtherProperties( goldProperties );
      final Map<String,Integer> goldIndices = mapPropertyIndices( goldProperties );
      final List<String> systemProperties = readHeader( systemFile );
      final Map<String,Integer> systemIndices = mapPropertyIndices( systemProperties );

      final Map<String,Collection<NeoplasmSummary>> goldSummaries
            = readSummaries( goldFile, requiredProperties, otherProperties, goldIndices );

      final Map<String,Collection<NeoplasmSummary>> systemSummaries
            = readSummaries( systemFile, requiredProperties, otherProperties, systemIndices );

      final Collection<EvalPatient> bestPatients = getBestPatients( goldSummaries, systemSummaries );
      final EvalCorpus corpus = new EvalCorpus( bestPatients );

      mergeValueNames( otherProperties );
      saveScoreFile( neoplasmType, requiredProperties, otherProperties, systemFile, corpus );
//      logWikiRow( neoplasmType, corpus );
//      logExcelRow( neoplasmType, corpus );
   }

   static private List<String> readHeader( final File file ) {
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         final String header = reader.readLine();
         return Arrays.asList( StringUtil.fastSplit( header, '|' ) );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      return Collections.emptyList();
   }

   static private Collection<String> getRequiredProperties( final List<String> goldProperties ) {
      return goldProperties.stream()
                           .filter( p -> p.startsWith( "*" ) )
                           .map( p -> p.substring( 1 ) )
                           .collect( Collectors.toList() );
   }

   static private Collection<String> getOtherProperties( final List<String> goldProperties ) {
      return goldProperties.stream()
                           .filter( p -> !p.startsWith( "*" ) )
                           .filter( p -> !p.startsWith( "-" ) )
                           .collect( Collectors.toList() );
   }

   static private Map<String,Integer> mapPropertyIndices( final List<String> propertyNames ) {
      final Map<String,Integer> map = new HashMap<>( propertyNames.size() );
      int index = 0;
      for ( String propertyName : propertyNames ) {
         String name = propertyName;
         if ( propertyName.startsWith( "*" ) || propertyName.startsWith( "-" ) ) {
            name = propertyName.substring( 1 );
         }
         map.put( name, index );
         index++;
      }
      return map;
   }


   /**
    *
    * @param file -
    * @return map of patient id to summaries
    */
   static private Map<String,Collection<NeoplasmSummary>> readSummaries( final File file,
                                                                           final Collection<String> requiredNames,
                                                                           final Collection<String> otherNames,
                                                                           final Map<String,Integer> indices ) {
      final Map<String,Collection<NeoplasmSummary>> summaryMap = new HashMap<>();
      final int patientIndex = indices.get( PATIENT_ID );
      final int summaryIndex = indices.get( SUMMARY_ID );
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         // skip header
         reader.readLine();
         String line = reader.readLine();
         while ( line != null ) {
            final List<String> values = Arrays.asList( StringUtil.fastSplit( line, '|' ) );
            if ( patientIndex >= values.size() || summaryIndex >= values.size() ) {
               LOGGER.error( file.getPath() + " is missing either " + PATIENT_ID + " or " + SUMMARY_ID + " on " + line );
               System.exit( -1 );
            }
            final String patientId = values.get( patientIndex );
            final String summaryId = values.get( summaryIndex );

            final Map<String,String> required = new HashMap<>( requiredNames.size() );
            for ( String name : requiredNames ) {
               final Integer index = indices.get( name );
               if ( index == null || index >= values.size() ) {
                  required.put( name, "" );
               } else {
                  required.put( name, values.get( index ) );
               }
            }

            final Map<String,String> other = new HashMap<>( otherNames.size() );
            for ( String name : otherNames ) {
               final Integer index = indices.get( name );
               if ( index == null || index >= values.size() ) {
                  other.put( name, "" );
               } else {
                  other.put( name, values.get( index ) );
               }
            }

            final NeoplasmSummary summary = new NeoplasmSummary( summaryId, required, other );
            summaryMap.computeIfAbsent( patientId, p -> new ArrayList<>() ).add( summary );

            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
      return summaryMap;
   }

   static private Collection<EvalPatient> getBestPatients( final Map<String,Collection<NeoplasmSummary>> goldMap,
                                                           final Map<String,Collection<NeoplasmSummary>> systemMap ) {
      final Collection<String> patientIds = new HashSet<>( goldMap.keySet() );
      patientIds.addAll( systemMap.keySet() );
      final List<String> patientIdList = new ArrayList<>( patientIds );
      Collections.sort( patientIdList );
      final Collection<EvalPatient> bestPatients = new ArrayList<>( patientIdList.size() );
      for ( String patientId : patientIdList ) {
         bestPatients.add( getBestPatient( patientId, goldMap, systemMap ) );
      }
      return bestPatients;
   }

   static private EvalPatient getBestPatient( final String patientId,
                                              final Map<String,Collection<NeoplasmSummary>> goldMap,
                                              final Map<String,Collection<NeoplasmSummary>> systemMap ) {
      final Collection<NeoplasmSummary> gold = goldMap.get( patientId );
      final Collection<NeoplasmSummary> system = systemMap.get( patientId );
      if ( gold == null || gold.size() == 0 ) {
         LOGGER.warn( "Gold is missing annotations for patient " + patientId );
         final Collection<EvalNeoplasm> pairs = system.stream()
                                                      .map( s -> new EvalNeoplasm( null, s ) )
                                                      .collect( Collectors.toList());
         return new EvalPatient( patientId, pairs );
      }
      if ( system == null || system.size() == 0 ) {
         LOGGER.warn( "System is missing output for patient " + patientId );
         final Collection<EvalNeoplasm> pairs = gold.stream()
                                                    .map( g -> new EvalNeoplasm( null, g ) )
                                                    .collect( Collectors.toList());
         return new EvalPatient( patientId, pairs );
      }

      final List<NeoplasmSummary> goldList = new ArrayList<>( gold );
      final List<NeoplasmSummary> systemList = new ArrayList<>( system );

      if ( gold.size() == 1 && system.size() == 1 ) {
         return new EvalPatient( patientId, new EvalNeoplasm( goldList.get( 0 ), systemList.get( 0 ) ) );
      }

      return getBestEvalPatient( patientId, goldList, systemList );
   }

   static private EvalPatient getBestEvalPatient( final String patientId,
                                                  final List<NeoplasmSummary> goldList,
                                                  final List<NeoplasmSummary> systemList ) {
      goldList.sort( Comparator.comparing( n -> n._id ) );
      systemList.sort( Comparator.comparing( n -> n._id ) );
      final int count = Math.max( goldList.size(), systemList.size() );
      final List<List<Pair<Integer>>> allCombinations = buildCombinations( count );
      double bestF1_ = 0;
      EvalPatient bestPatient = null;
      for ( List<Pair<Integer>> combination : allCombinations ) {
         final Collection<EvalNeoplasm> evalNeoplasms = new ArrayList<>( count );
         for ( Pair<Integer> combo : combination ) {
            NeoplasmSummary gold = null;
            if ( combo.getValue1() < goldList.size() ) {
               gold = goldList.get( combo.getValue1() );
            }
            NeoplasmSummary system = null;
            if ( combo.getValue2() < systemList.size() ) {
               system = systemList.get( combo.getValue2() );
            }
            evalNeoplasms.add( new EvalNeoplasm( gold, system ) );
         }
         final EvalPatient evalPatient = new EvalPatient( patientId, evalNeoplasms );
         final double evalF1_ = evalPatient.getF1_s();
         if ( bestPatient == null || evalF1_ > bestF1_ ) {
            bestPatient = evalPatient;
            bestF1_ = evalF1_;
         }
      }
      return bestPatient;
   }

   static private List<List<Pair<Integer>>> buildCombinations( final int count ) {
      final List<List<Pair<Integer>>> allCombinations = new ArrayList<>();

      for ( int i=0; i<count; i++ ) {
         for ( int j=0; j<count; j++ ) {
            final List<Pair<Integer>> thisCombination = new ArrayList<>( count );
            thisCombination.add( new Pair<>( i, j ) );
            allCombinations.add( buildCombinations( count, thisCombination ) );
         }
      }
      return allCombinations;
   }


   static private List<Pair<Integer>> buildCombinations( final int count, final List<Pair<Integer>> combination ) {
      if ( combination.size() == count ) {
         return combination;
      }
      boolean added = false;
      final List<Pair<Integer>> iterableCombination = new ArrayList<>( combination );
      for ( int i=0; i<count; i++ ) {
         if ( added ) {
            break;
         }
         for ( Pair<Integer> combo : iterableCombination ) {
            if ( added || combo.getValue1() == i ) {
               break;
            }
            for ( int j=0; j<count; j++ ) {
               if ( added ) {
                  break;
               }
               for ( Pair<Integer> combo2 : iterableCombination ) {
                  if ( combo2.getValue2() == j ) {
                     break;
                  }
                  combination.add( new Pair<>( i, j ) );
                  if ( combination.size() == count ) {
                     return combination;
                  }
                  added = true;
               }
            }
         }
      }
      return buildCombinations( count, combination );
   }



   static private Collection<String> getUris( final String value ) {
      return Arrays.stream( StringUtil.fastSplit( value.trim(), ';' ) )
                   .map( SummaryEvaluator::getAdjustedUri )
                   .map( String::trim )
                   .filter( s -> !s.isEmpty() )
                   .distinct()
                   .sorted()
                   .collect( Collectors.toList() );
   }

   static private String getAdjustedUri( final String uri ) {
      if ( uri.endsWith( "_Stage_Finding" ) ) {
         if ( uri.startsWith( "c" ) || uri.startsWith( "p" ) ) {
            return uri.substring( 1 );
         }
      }
      return uri;
   }

   static private boolean anyMatch( final String goldValue, final String systemValue ) {
      if ( specialMatch( goldValue, systemValue ) ) {
         return true;
      }
      if ( goldValue == null || goldValue.isEmpty() ) {
         return systemValue == null || systemValue.isEmpty();
      }
      final Collection<String> goldUris = getUris( goldValue );
      final Collection<String> systemUris = getUris( systemValue );
      for ( String goldUri : goldUris ) {
         for ( String systemUri : systemUris ) {
            if (goldUri.equalsIgnoreCase( systemUri ) ) {
               return true;
            }
         }
      }
      if ( UriUtil.isUriBranchMatch( goldUris, systemUris ) ) {
         return true;
      }
      for ( String goldUri : goldUris ) {
         for ( String systemUri : systemUris ) {
            if ( UriUtil.getCloseUriLeaf( goldUri, systemUri ) != null ) {
               return true;
            }
         }
      }
      return false;
   }

   static private double countMatched( final String goldValue, final String systemValue ) {
      if ( specialMatch( goldValue, systemValue ) ) {
         return 1;
      }
      return countMatched( getUris( goldValue ), getUris( systemValue ) );
   }

   static private double countMatched( final Collection<String> uris1, final Collection<String> uris2 ) {
      final int un2 = countUnMatched( uris2, uris1 );
      return uris2.size() - un2;
   }

   static private double countUnMatched( final String value1, final String value2 ) {
      if ( specialMatch( value1, value2 ) || specialMatch( value2, value1 ) ) {
         return 0;
      }
      return countUnMatched( getUris( value1 ), getUris( value2 ) );
   }

   static private int countUnMatched( final Collection<String> uris1, final Collection<String> uris2 ) {
      int matchCount = 0;
      for ( String uri : uris1 ) {
         boolean matched = false;
         for ( String uri2 : uris2 ) {
            if (uri.equalsIgnoreCase( uri2 ) ) {
               matched = true;
            } else if ( UriUtil.isUriBranchMatch( uri, uri2 ) ) {
               matched = true;
            } else if ( UriUtil.getCloseUriLeaf( uri, uri2 ) != null ) {
               matched = true;
            }
            if ( matched ) {
               matchCount++;
               break;
            }
         }
      }
      return uris1.size() - matchCount;
   }

   static private boolean specialMatch( final String goldValue, final String systemValue ) {
      if ( goldValue == null ) {
         return false;
      }
      if ( goldValue.equals( "[]" ) ) {
         // [] is used for gold when there should be a value but none was determined.  e.g. hasHistologicType
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Present" )
           && systemValue != null
           && !systemValue.isEmpty()
           && !systemValue.equalsIgnoreCase( "Absent" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Absent" )
           && (systemValue == null || systemValue.isEmpty() || systemValue.equalsIgnoreCase( "Absent" )) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Positive" )
           && systemValue != null
           && systemValue.contains( "Positive" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Negative" )
           && systemValue != null
           && systemValue.contains( "Negative" ) ) {
         return true;
      }
      if ( goldValue.equalsIgnoreCase( "Unknown" )
           && systemValue != null
           && ( systemValue.isEmpty() || systemValue.contains( "Unknown" ) ) ) {
         return true;
      }
      return false;
   }

   static private double getPRS( final double tp, final double other ) {
      if ( tp == 0 && other == 0 ) {
         return 0;
      }
      return tp / (tp + other );
   }

   static private double getF1( final double p, final double r ) {
      if ( p == 0 && r == 0 ) {
         return 0;
      }
      return 2 * p * r / ( p + r );
   }

   static abstract private class AbstractEvalThing {
      abstract double getTP();
      abstract double getTN();
      abstract double getFP();
      abstract double getFN();
      abstract double getTP_();
      abstract double getTN_();
      abstract double getFP_();
      abstract double getFN_();
      private double getP() {
         return getPRS( getTP(), getFP() );
      }
      double getR() {
         return getPRS( getTP(), getFN() );
      }
      double getS() {
         return getPRS( getTN(), getFP() );
      }
      double getP_s() {
         return getPRS( getTP_(), getFP_() );
      }
      double getP_j() {
         return getPRS( getTP_(), getFP() );
      }
      double getR_s() {
         return getPRS( getTP_(), getFN_() );
      }
      double getR_j() {
         return getPRS( getTP_(), getFN() );
      }
      // Specificity
      double getS_s() {
         return getPRS( getTN_(), getFP_() );
      }
      double getF1() {
         return getF1( getP(), getR() );
      }
      double getF1_s() {
         return getF1( getP_s(), getR_s() );
      }
      double getF1_j() {
         return getF1( getP_j(), getR_j() );
      }
      double getF1( final double p, final double r ) {
         if ( p == 0 && r == 0 ) {
            return 0;
         }
         return 2 * p * r / ( p + r );
      }
      double getAccuracy() {
         final double tp = getTP();
         final double tn = getTN();
         return ( tp + tn ) / ( tp + tn + getFP() + getFN() );
      }
      double getAccuracy_() {
         final double tp = getTP_();
         final double tn = getTN_();
         return ( tp + tn ) / ( tp + tn + getFP_() + getFN_() );
      }
   }


   static private final class EvalCorpus extends AbstractEvalThing {
      private final Collection<EvalNeoplasm> _pairs;
      private final Collection<EvalPatient> _patients;
      private EvalCorpus( final Collection<EvalPatient> patients ) {
         _patients = patients;
         _pairs = patients.stream()
                           .map( EvalPatient::getPairs )
                           .flatMap( Collection::stream )
                           .collect( Collectors.toList());
      }
      void getA_F1( final Map<String,Double> attributeTPs,
                    final Map<String,Double> attributeTNs,
                    final Map<String,Double> attributeFPs,
                    final Map<String,Double> attributeFNs ) {
         for ( EvalNeoplasm neoplasm : _pairs ) {
            for ( String name : neoplasm.getValueNames() ) {
               double tp = attributeTPs.computeIfAbsent( name, d -> 0d );
               double tn = attributeTNs.computeIfAbsent( name, d -> 0d );
               double fp = attributeFPs.computeIfAbsent( name, d -> 0d );
               double fn = attributeFNs.computeIfAbsent( name, d -> 0d );
               final String gold = neoplasm.getGold( name );
               final String system = neoplasm.getSystem( name );
               final EvalUris evalUris = new EvalUris( gold, system );
               attributeTPs.put( name, tp + evalUris.getTP_() );
               attributeTNs.put( name, tn + evalUris.getTN_() );
               attributeFPs.put( name, fp + evalUris.getFP_() );
               attributeFNs.put( name, fn + evalUris.getFN_() );
            }
            for ( String name : neoplasm.getRequiredNames() ) {
               if ( !PATIENT_ID.equalsIgnoreCase( name ) ) {
                  double tp = attributeTPs.computeIfAbsent( name, d -> 0d );
                  attributeTPs.put( name, tp + 1 );
               }
            }
         }
      }
      double getTP() {
//         return ( getTP_() > 0 ) ? 1 : 0;
         return _pairs.stream()
                      .mapToDouble( EvalNeoplasm::getTP )
                         .sum();
      }
      double getTN() {
         return 0;
      }
      double getFP() {
//         return 0;
         return _pairs.stream()
                         .mapToDouble( EvalNeoplasm::getFP )
                         .sum();
      }
      double getFN() {
//         return ( getFN_() > 0 ) ? 1 : 0;
         return _pairs.stream()
                         .mapToDouble( EvalNeoplasm::getFN )
                         .sum();
      }
      double getTP_() {
         return _pairs.stream()
                         .mapToDouble( EvalNeoplasm::getTP_ )
                         .sum();
      }
      double getTN_() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getTN_ ).sum();
      }
      double getFP_() {
         return _pairs.stream()
                         .mapToDouble( EvalNeoplasm::getFP_ )
                         .sum();
      }
      double getFN_() {
         return _pairs.stream()
                         .mapToDouble( EvalNeoplasm::getFN_ )
                         .sum();
      }
   }

   static private final class EvalPatient extends AbstractEvalThing {
      private final String _id;
      private final Collection<EvalNeoplasm> _pairs;

      private EvalPatient( final String id, final EvalNeoplasm pair ) {
         this( id, Collections.singletonList( pair ) );
      }
      private EvalPatient( final String id, final Collection<EvalNeoplasm> pairs ) {
         _id = id;
         _pairs = pairs;
      }
      Collection<EvalNeoplasm> getPairs() {
         return _pairs;
      }
      double getTP() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getTP ).sum();
      }
      double getTN() {
         return 0;
      }
      double getFP() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getFP ).sum();
      }
      double getFN() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getFN ).sum();
      }
      double getTP_() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getTP_ ).sum();
      }
      double getTN_() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getTN_ ).sum();
      }
      double getFP_() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getFP_ ).sum();
      }
      double getFN_() {
         return _pairs.stream().mapToDouble( EvalNeoplasm::getFN_ ).sum();
      }
   }



   static private final class EvalNeoplasm extends AbstractEvalThing {
      private final NeoplasmSummary _gold;
      private final NeoplasmSummary _system;
      private EvalNeoplasm( final NeoplasmSummary gold, final NeoplasmSummary system ) {
         _gold = gold;
         _system = system;
      }
      private String getGold( final String name ) {
         if ( _gold == null ) {
            return "";
         }
         final String gold = _gold._valueMap.get( name );
         return gold == null ? "" : gold.trim();
      }
      private String getSystem( final String name ) {
         if ( _system == null ) {
            return "";
         }
         final String system = _system._valueMap.get( name );
         return system == null ? "" : system.trim();
      }
      private Collection<String> getRequiredNames() {
         if ( _gold == null ) {
            return Collections.emptyList();
         }
         return _gold._requiredMap.keySet();
      }
      private Collection<String> getValueNames() {
         if ( _gold == null ) {
            return Collections.emptyList();
         }
         return _gold._valueMap.keySet();
      }
      double getTP() {
         if ( _gold == null || _system == null ) {
            return 0;
         }
         for ( String name : getRequiredNames() ) {
            final String goldValue = getGold( name );
            final String systemValue = getSystem( name );
            if ( !anyMatch( goldValue, systemValue ) ) {
               return 0;
            }
         }
         return 1;
      }
      double getTN() {
         return 0;
      }
      double getFP() {
         if ( _gold == null && _system != null ) {
            return 1;
         }
         return 0;
      }
      double getFN() {
         if ( _gold != null && _system == null ) {
            return 1;
         }
         return 0;
      }
      double getTP_() {
         if ( _gold == null || _system == null ) {
            return 0;
         }
         double score = 0;
         for ( String name : getValueNames() ) {
            final String gold = getGold( name );
            final String system = getSystem( name );
            final EvalUris evalUris = new EvalUris( gold, system );
            score += evalUris.getTP_();
         }
         score += getRequiredNames().size() - 1;
         return score;
      }
      double getTN_() {
         if ( _gold == null || _system == null ) {
            return 0;
         }
         double score = 0;
         for ( String name : getValueNames() ) {
            final String gold = getGold( name );
            final String system = getSystem( name );
            final boolean gEmpty = gold == null || gold.isEmpty();
            final boolean sEmpty = system == null || system.isEmpty();
            if ( gEmpty && sEmpty ) {
               score++;
            }
         }
         return score;
      }
      double getFP_() {
         if ( _gold == null ) {
            return 1;
         }
         if ( _system == null ) {
            return 0;
         }
         double score = 0;
         for ( String name : getValueNames() ) {
            final String gold = getGold( name );
            final String system = getSystem( name );
            final EvalUris evalUris = new EvalUris( gold, system );
            score += evalUris.getFP_();
         }
         return score;
      }
      double getFN_() {
         if ( _system == null ) {
            return 1;
         }
         double score = 0;
         for ( String name : getValueNames() ) {
            final String gold = getGold( name );
            final String system = getSystem( name );
            final EvalUris evalUris = new EvalUris( gold, system );
            score += evalUris.getFN_();
         }
         return score;
      }
   }

   static private final class EvalUris extends AbstractEvalThing {
      private final String _gold;
      private final String _system;

      private EvalUris( final String gold, final String system ) {
         _gold = gold;
         _system = system;
      }
      private String getGoldPrefix() {
         if ( getTP_() > 0 ) {
            return "   ";
         }
         return ( getFN_() > 0 ) ? "FN " : "   ";
      }
      private String getSystemPrefix() {
         if ( getTP_() > 0 ) {
            if ( getFP_() > 0 || getFN_() > 0 ) {
               return "MX ";
            }
            return "   ";
         }
         return ( getFP_() > 0 ) ? "FP " : "   ";
      }
      double getTP() {
         return ( getTP_() > 0 ) ? 1 : 0;
      }
      double getTN() {
         return ( getTN_() > 0 ) ? 1 : 0;
      }
      double getFP() {
         return ( getFP_() == 1 ) ? 1 : 0;
      }
      double getFN() {
         return ( getFN_() == 1 ) ? 1 : 0;
      }
      double getTP_() {
         if ( _gold == null || _gold.isEmpty() || _system == null || _system.isEmpty() ) {
            return 0;
         }
         return countMatched( _gold, _system );
      }
      double getTN_() {
         final boolean gEmpty = _gold == null || _gold.isEmpty();
         final boolean sEmpty = _system == null || _system.isEmpty();
         if ( gEmpty && sEmpty ) {
            return 1;
         }
         return 0;
      }
      double getFP_() {
         if ( getTN_() > 0 ) {
            return 0;
         }
         return countUnMatched( _system, _gold );
      }
      double getFN_() {
         if ( getTN_() > 0 ) {
            return 0;
         }
         return countUnMatched( _gold, _system );
      }
   }

   // TODO  Make an array of EvalPair objects, gold rows and system columns.
   //  For each "best" pairing stick in map and remove row and column.
   //   Of course, if there is just one system and one gold then the pairing is obvious.
   //   Ditto for zero and one.

   static private void mergeValueNames( final Collection<String> names ) {
      names.remove( HAS_PATHOLOGIC_T );
      names.remove( HAS_PATHOLOGIC_N );
      names.remove( HAS_PATHOLOGIC_M );
   }

   static private final class NeoplasmSummary {
      private final String _id;
      private final Map<String,String> _requiredMap = new HashMap<>();
      private final Map<String,String> _valueMap = new HashMap<>();
      private NeoplasmSummary( final String id,
                               final Map<String,String> requiredMap,
                               final Map<String,String> valueMap ) {
         _id = id;
         _requiredMap.putAll( requiredMap );
         _valueMap.putAll( valueMap );
         mergeAlikeValues();
      }
      private void mergeAlikeValues() {
         mergeAlikeValues( HAS_PATHOLOGIC_T, HAS_CLINICAL_T );
         mergeAlikeValues( HAS_PATHOLOGIC_N, HAS_CLINICAL_N );
         mergeAlikeValues( HAS_PATHOLOGIC_M, HAS_CLINICAL_M );
         mergeValueNames( _valueMap.keySet() );
      }
      private void mergeAlikeValues( final String toMerge, final String toUse ) {
         final String pt = _valueMap.get( toMerge );
         if ( pt != null && !pt.isEmpty() ) {
            String ct = _valueMap.computeIfAbsent( toUse, String::new );
            _valueMap.put( toUse, ct + ";" + pt );
         }
      }
      public boolean equals( final Object other ) {
         return hashCode() == other.hashCode();
      }
      public int hashCode() {
         return _id.hashCode();
      }
   }

   static private void saveScoreFile( final String neoplasmType,
                                      final Collection<String> requiredProperties,
                                      final Collection<String> otherProperties,
                                      final File systemFile,
                                      final EvalCorpus corpus ) {
      final File output = new File( systemFile.getParentFile(), systemFile.getName() + "_score.txt" );
      try ( Writer writer = new BufferedWriter( new FileWriter( output ) ) ) {
         final String stars
               = String.format( "**************************************%"
                                + neoplasmType.length()
                                + "s**************************************\n\n", "")
                       .replace(' ', '*' );
         writer.write( stars );
         writer.write( "***                                  " + neoplasmType + "                                    ***\n\n" );
         writer.write( stars );
         writer.write( "Corpus Patient Score:\n" + getCorpusPatientF1s( corpus ) + "\n" );
         writer.write( "Corpus Neoplasm Score:\n" + getPureF1( corpus ) + "\n" );
//         writer.write( "Attribute Score:\n" + getPropertyF1( corpus ) + "\n" );
         writer.write( "Corpus Attribute Score:\n" + getPropertyA_F1( corpus ) + "\n" );
         writer.write( "Attribute Scores:\n" + getPropertyA_F1s( requiredProperties, otherProperties, corpus ) + "\n" );
         writer.write( stars + "\n" );
         for ( EvalPatient patient : corpus._patients ) {
            savePatientScore( writer, requiredProperties, otherProperties, patient );
         }
         writer.write( getLegend() );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
   }

   static private void savePatientScore( final Writer writer,
                                         final Collection<String> requiredProperties,
                                         final Collection<String> otherProperties,
                                         final EvalPatient patient ) throws IOException {
      writer.write( "----------------------------------  " + patient._id + "  ----------------------------------\n\n" );
//      writer.write( "Patient Neoplasm Score:\n" + getPureF1( patient) + "\n" );
//      writer.write( "Patient Attribute Score:\n" + getPropertyF1( patient ) + "\n" );
      writer.write( getPureF1( "Neoplasm:", patient) );
      writer.write( getPropertyF1( "Attribute:", patient ) + "\n" );
      final Collection<EvalNeoplasm> tp = new ArrayList<>();
      final Collection<EvalNeoplasm> fp = new ArrayList<>();
      final Collection<EvalNeoplasm> fn = new ArrayList<>();
      for ( EvalNeoplasm pair : patient.getPairs() ) {
         if ( pair.getTP() > 0 ) {
            tp.add( pair );
         } else if ( pair.getFP() > 0 ) {
            fp.add( pair );
         } else {
            fn.add( pair );
         }
      }
      for ( EvalNeoplasm pair : tp ) {
         saveTP( writer, requiredProperties, otherProperties, pair );
      }
      for ( EvalNeoplasm pair : fp ) {
         saveFP( writer, requiredProperties, otherProperties, pair );
      }
      for ( EvalNeoplasm pair : fn ) {
         saveFN( writer, requiredProperties, otherProperties, pair );
      }
   }

   static private void saveTP( final Writer writer,
                               final Collection<String> requiredProperties,
                               final Collection<String> otherProperties,
                               final EvalNeoplasm evalNeoplasm ) throws IOException {
      final NeoplasmSummary system = evalNeoplasm._system;
      final NeoplasmSummary gold = evalNeoplasm._gold;
      writer.write( "\nTRUE POSITIVE     " + gold._id );
      writer.write( "\n==== ========     " + system._id + "\n\n" );
      writer.write( getPropertyF1( evalNeoplasm ) + "\n" );
      for ( String required : requiredProperties ) {
         final String goldValue = gold._requiredMap.get( required );
         final String systemValue = system._requiredMap.get( required );
         final EvalUris evalUris = new EvalUris( goldValue, systemValue );
         writer.write( getScoreRow( "*" + required, evalUris ) );
         writer.write( getValueRow( goldValue, systemValue, evalUris ) );
      }
      for ( String other : otherProperties ) {
         final String goldValue = gold._valueMap.get( other );
         final String systemValue = system._valueMap.get( other );
         final EvalUris evalUris = new EvalUris( goldValue, systemValue );
         writer.write( getScoreRow( other, evalUris ) );
         writer.write( getValueRow( goldValue, systemValue, evalUris ) );
      }
      writer.write( "\n\n" );
   }


   static private void saveFP( final Writer writer,
                               final Collection<String> requiredProperties,
                               final Collection<String> otherProperties,
                               final EvalNeoplasm evalNeoplasm ) throws IOException {
      final NeoplasmSummary system = evalNeoplasm._system;
      writer.write( "\nFALSE POSITIVE     " + system._id );
      writer.write( "\n===== ========\n\n" );
      for ( String required : requiredProperties ) {
         final String systemValue = system._requiredMap.get( required );
         writer.write( getSystemValueRow( "*" + required, systemValue ) );
      }
      for ( String other : otherProperties ) {
         final String systemValue = system._valueMap.get( other );
         writer.write( getSystemValueRow( other, systemValue ) );
      }
      writer.write( "\n\n" );
   }

   static private void saveFN( final Writer writer,
                               final Collection<String> requiredProperties,
                               final Collection<String> otherProperties,
                               final EvalNeoplasm evalNeoplasm ) throws IOException {
      final NeoplasmSummary gold = evalNeoplasm._gold;
      writer.write( "\nFALSE NEGATIVE     " + gold._id );
      writer.write( "\n===== ========\n\n" );
      for ( String required : requiredProperties ) {
         final String goldValue = gold._requiredMap.get( required );
         writer.write( getGoldValueRow( "*" + required, goldValue ) );
      }
      for ( String other : otherProperties ) {
         final String goldValue = gold._valueMap.get( other );
         writer.write( getGoldValueRow( other, goldValue ) );
      }
      writer.write( "\n\n" );
   }


   static private String getPureF1( final AbstractEvalThing evalThing ) {
      final String score1 = getTitleLine("TP","FP","FN","TN","Accur %","P","R","Spcfty","F1" );
      final String score2 = getScoreLine(
            noDot( evalThing.getTP() ),
            noDot( evalThing.getFP() ),
            noDot( evalThing.getFN() ),
            noDot( evalThing.getTN() ),
            evalThing.getAccuracy(),
            evalThing.getP(),
            evalThing.getR(),
            evalThing.getS(),
            evalThing.getF1() );
      return score1 + "\n" + score2 + "\n";
   }

   static private String getPureF1( final String name, final AbstractEvalThing evalThing ) {
      final String score1 = getTitleLine( "", "TP","FP","FN","TN","Accur %","P","R","Spcfty","F1" );
      final String score2 = getScoreLine(
            name + "     ",
            noDot( evalThing.getTP() ),
            noDot( evalThing.getFP() ),
            noDot( evalThing.getFN() ),
            noDot( evalThing.getTN() ),
            evalThing.getAccuracy(),
            evalThing.getP(),
            evalThing.getR(),
            evalThing.getS(),
            evalThing.getF1() );
      return score1 + "\n" + score2 + "\n";
   }


   static private String getCorpusPatientF1s( final EvalCorpus corpus ) {
      double tp = 0;
      double tn = 0;
      double fp = 0;
      double fn = 0;
      for ( EvalPatient patient : corpus._patients ) {
         if ( patient.getTP() > 0 ) {
            tp++;
         } else if ( patient.getFP() > 0 ) {
            fp++;
         } else if ( patient.getFN() > 0 ) {
            fn++;
         } else {
            tn++;
         }
      }
      final double accuracy = ( tp + tn ) / ( tp + tn + fp + fn );
      final double p = getPRS( tp, fp );
      final double r = getPRS( tp, fn );
      final double s = getPRS( tn, fp );
      final double f1 = getF1( p, r );
      final String score1 = getTitleLine("TP","FP","FN","TN","Accur %","P","R","Spcfty","F1" );
      final String score2 = getScoreLine(
            noDot( tp ),
            noDot( fp ),
            noDot( fn ),
            noDot( tn ),
            accuracy,
            p,
            r,
            s,
            f1 );
      return score1 + "\n" + score2 + "\n";
   }

   static private String getPropertyF1( final AbstractEvalThing evalThing ) {
//      final String score5 = getTitleLine("TP'","FP'","FN'","TN'","Accur' %","P'","R'","Spcfty'","F1'" );
      final String score1 = getTitleLine("", "TP","FP","FN","TN","Accur %","P","R","Spcfty","F1" );
      final String score6 = getScoreLine( "Attribute:",
            noDot( evalThing.getTP_() ),
            noDot( evalThing.getFP_() ),
            noDot( evalThing.getFN_() ),
            noDot( evalThing.getTN_() ),
            evalThing.getAccuracy_(),
            evalThing.getP_s(),
            evalThing.getR_s(),
            evalThing.getS_s(),
            evalThing.getF1_s() );
      return score1 + "\n" + score6 + "\n";
   }

   static private String getPropertyF1( final String name, final AbstractEvalThing evalThing ) {
//      final String score5 = getTitleLine("TP'","FP'","FN'","TN'","Accur' %","P'","R'","Spcfty'","F1'" );
//      final String score1 = getTitleLine("", "TP","FP","FN","TN","Accur %","P","R","Spcfty","F1" );
      final String score6 = getScoreLine(
            name + "     ",
            noDot( evalThing.getTP_() ),
            noDot( evalThing.getFP_() ),
            noDot( evalThing.getFN_() ),
            noDot( evalThing.getTN_() ),
            evalThing.getAccuracy_(),
            evalThing.getP_s(),
            evalThing.getR_s(),
            evalThing.getS_s(),
            evalThing.getF1_s() );
//      return score1 + "\n" + score6 + "\n";
      return score6 + "\n";
   }


   static private String getPropertyA_F1( final EvalCorpus corpus ) {
      final String score5 = String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            "TP","FP","FN","TN","Accur %","P","R","Spcfty","F1", "Attr/Neo F1" );
      final double propertyF1 = corpus.getF1_s();
      final double spanF1 = corpus.getF1();
      final String score6 = String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            noDot( corpus.getTP_() ),
            noDot( corpus.getFP_() ),
            noDot( corpus.getFN_() ),
            noDot( corpus.getTN_() ),
            percent2( corpus.getAccuracy_() ),
            dot4( corpus.getP_s() ),
            dot4( corpus.getR_s() ),
            dot4( corpus.getS_s() ),
            dot4( propertyF1 ),
            dot4( propertyF1 / spanF1 )
            );
      return score5 + "\n" + score6 + "\n";
   }

   static private String getPropertyA_F1_title() {
      return String.format( "%-20s   %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n",
            "Attribute", "TP","FP","FN","TN","Accur %","P","R","Spcfty","F1", "Attr/Neo F1" );
   }

   static private String getPropertyA_F1s( final Collection<String> requiredProperties,
                                           final Collection<String> otherProperties,
                                           final EvalCorpus corpus ) {
      final Map<String,Double> attributeTPs = new HashMap<>();
      final Map<String,Double> attributeTNs = new HashMap<>();
      final Map<String,Double> attributeFPs = new HashMap<>();
      final Map<String,Double> attributeFNs = new HashMap<>();
      corpus.getA_F1( attributeTPs, attributeTNs, attributeFPs, attributeFNs );
      final double spanF1 = corpus.getF1();
      final StringBuilder sb = new StringBuilder();
      sb.append( getPropertyA_F1_title() );
      for ( String required : requiredProperties ) {
         if ( PATIENT_ID.equals( required ) ) {
            continue;
         }
         sb.append( getPropertyA_F1( required,
               attributeTPs.get( required ),
               0,
               0,
               0,
               spanF1 ) );
      }
      for ( String other : otherProperties ) {
         sb.append( getPropertyA_F1( other,
               attributeTPs.get( other ),
               attributeTNs.get( other ),
               attributeFPs.get( other ),
               attributeFNs.get( other ),
               spanF1 ) );
      }
      return sb.toString();
   }

   static private String getPropertyA_F1( final String name, final double tp, final double tn, final double fp, final double fn, final double spanF1 ) {
      final double accuracy = ( tp + tn ) / ( tp + tn + fp + fn );
      final double p = getPRS( tp, fp );
      final double r = getPRS( tp, fn );
      final double s = getPRS( tn, fp );
      final double f1 = getF1( p, r );
      final double aF1 = f1 / spanF1;
      return String.format( "%-20s   %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n",
            name,
            noDot( tp ),
            noDot( fp ),
            noDot( fn ),
            noDot( tn ),
            percent2( accuracy ),
            dot4( p ),
            dot4( r ),
            dot4( s ),
            dot4( f1 ),
            dot4( aF1 ) );
   }

//   private double getP() {
//      return getPRS( getTP(), getFP() );
//   }
//   double getR() {
//      return getPRS( getTP(), getFN() );
//   }
//   double getS() {
//      return getPRS( getTN(), getFP() );
//   }

   static private String getTitleLine( final String tp, final String fp, final String fn, final String tn,
                                       final String accuracy, final String p, final String r, final String s,
                                       final String f1 ) {
      return String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            tp,
            fp,
            fn,
            tn,
            accuracy,
            p,
            r,
            s,
            f1 );
   }

   static private String getTitleLine( final String name, final String tp, final String fp, final String fn, final String tn,
                                       final String accuracy, final String p, final String r, final String s,
                                       final String f1 ) {
      return String.format( " %20s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            name,
            tp,
            fp,
            fn,
            tn,
            accuracy,
            p,
            r,
            s,
            f1 );
   }

   static private String getScoreLine( final String tp, final String fp, final String fn, final String tn,
                                       final double accuracy, final double p, final double r, final double s,
                                       final double f1 ) {
      return String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            tp,
            fp,
            fn,
            tn,
            percent2( accuracy ),
            dot4( p ),
            dot4( r ),
            dot4( s ),
            dot4( f1 ) );
   }

   static private String getScoreLine( final String name, final String tp, final String fp, final String fn, final String tn,
                                       final double accuracy, final double p, final double r, final double s,
                                       final double f1 ) {
      return String.format( " %-20s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            name,
            tp,
            fp,
            fn,
            tn,
            percent2( accuracy ),
            dot4( p ),
            dot4( r ),
            dot4( s ),
            dot4( f1 ) );
   }


   static private String getOneLineF1( final AbstractEvalThing evalThing ) {
      return "Accur %  " + dot4( evalThing.getAccuracy_() ) + "   F1  " + dot4( evalThing.getF1_s() );
   }

   static private String getScoreRow( final String name,
                                       final EvalUris evalUris ) {
      final String title = String.format( "%-50s  ", name );
      return title + "      " + getOneLineF1( evalUris ) + "\n";
   }

   static private String getValueRow( final String gold,
                                      final String system,
                                      final EvalUris evalUris ) {
      final String gPrefix = evalUris.getGoldPrefix();
      final String sPrefix = evalUris.getSystemPrefix();
      return String.format( "   " + gPrefix + " Gold:   %40s\n   " + sPrefix + " System: %40s\n",
            gold.replace( ";"," , " ),
            system.replace( ";", " , " ) );
   }

   static private String getGoldValueRow( final String name, final String gold ) {
      final String title = String.format( "%-20s  ", name );
      return title + String.format( "   Gold: %40s\n",
            gold.replace( ";"," , " ) );
   }

   static private String getSystemValueRow( final String name, final String system ) {
      final String title = String.format( "%-20s  ", name );
      return title + String.format( "   System: %40s\n",
            system.replace( ";", " , " ) );
   }




//   static private void writeConfluenceTable(final PrintStream out,
//                                            final String label,
//                                            final ConfusionMatrix totalConfusion,
//                                            final Map<String, ConfusionMatrix> attributeConfusions) {
//      final String date = LocalDate.now().format( DateTimeFormatter.ISO_DATE);
//      out.println();
//      out.println("{| class=\"wikitable\"");
//      out.println("!Name");
//      out.println("![[Error Analysis Run " + date + "| F1 from " + date + "]]");
//      out.println("|-");
//      out.println("|'''" + label + "'''");
//      writeF1(out, totalConfusion.getFscore());
//      out.println("");
//      for (Map.Entry<String, ConfusionMatrix> entry : attributeConfusions.entrySet()) {
//         out.println("|" + entry.getKey());
//         writeF1(out, entry.getValue().getFscore());
//      }
//      out.println("|}");
//   }
//
//   static private void writeExcelColumns( final PrintStream out,
//                                          final String label,
//                                          final ConfusionMatrix totalConfusion,
//                                          final Map<String, ConfusionMatrix> attributeConfusions ) {
//      out.println();
//      final String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
//      out.print( "Date," + label );
//      for ( String attribute : attributeConfusions.keySet() ) {
//         out.print( "," + attribute );
//      }
//      out.print( ",,Auditable" );
//      out.println();
//      out.print( "\"" + date + "\"" + columnize( totalConfusion ) );
//      for ( ConfusionMatrix matrix : attributeConfusions.values() ) {
//         out.print( columnize( matrix ) );
//      }
//      // Auditable represents the percent of elements that can be corrected by simply verifying the output.
//      // In other words, without reading all of the notes from scratch a certain percent will always be missed.
//      //   - An auditor can easily get rid of FP.  They cannot easily get rid of FN.
//      // This comes out to the Recall' and an F1' based upon P=1 F1' = 2R/(1+R)
//      final double recallP = totalConfusion.TPP/(totalConfusion.TPP+totalConfusion.FN);
//      out.print( ",," + percent( recallP )
//                 + "    " + dot4( 2*recallP/(1+recallP) ) );
//      out.println();
//      out.println();
//   }

//   static private final String columnize( final ConfusionMatrix matrix ) {
//      return ",\""
//             + noDot( matrix.TPP )
//             + "," + noDot( matrix.FP )
//             + "," + noDot( matrix.FN )
//             + "  " + percent( matrix.getAccuracy() ) + "  "
//             + "  "  + dot4( matrix.getFscore() )
//             + "\"";
//   }

   static private String dot4( final double value ) {
      if ( Double.isNaN( value ) ) {
         return "-";
      }
      return String.format( "%.4f", value );
   }

   static private String percent( final double value ) {
      if ( Double.isNaN( value ) ) {
         return "-";
      }
      return String.format( "%.0f", value*100 ) + "%";
   }

   static private String percent2( final double value ) {
      if ( Double.isNaN( value ) ) {
         return "  0.00 %";
      }
      return String.format( "%6s", String.format( "%.2f", value*100 ) );
   }

   static private String noDot( final double value ) {
      if ( Double.isNaN( value ) ) {
         return "-";
      }
      return String.format( "%.0f", value );
   }


   static private String getLegend() {
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n\n-------------------------------------------------------------------\n" );
      sb.append( String.format( " %-40s The Score of the Corpus, based upon Patients.\n", "Corpus Patient Score:" ) );
      sb.append( String.format( " %-40s The Score of the Corpus, based upon Neoplasms (macro).\n", "Corpus Neoplasm Score:" ) );
      sb.append( String.format( " %-40s The Score of the Corpus, based upon Neoplasm Attributes (macro).\n", "Corpus Attribute Score:" ) );
      sb.append( String.format( " %-40s The Score of the Corpus for each Attribute type (macro).\n\n", "Attribute Scores:" ) );

      sb.append( String.format( " %-40s The Score of the Patient, based upon Neoplasms.\n", "Neoplasm:" ) );
      sb.append( String.format( " %-40s The Score of the Patient, based upon Neoplasm Attributes (macro).\n\n", "Attribute:" ) );

      sb.append( String.format( " %-15s True Positive Count.\n", "TP" ) );
      sb.append( String.format( " %-15s True Negative Count.\n", "TN" ) );
      sb.append( String.format( " %-15s False Positive Count.\n", "FP" ) );
      sb.append( String.format( " %-15s False Negative Count.\n\n", "FN" ) );

      sb.append( String.format( " %-15s Accuracy     ( TP + TN ) / ( TP + TN + FP + FN )\n", "Accur %" ) );
      sb.append( String.format( " %-15s Precision           TP   / ( TP + FP )\n", "P" ) );
      sb.append( String.format( " %-15s Recall              TP   / ( TP + FN )\n", "R" ) );
      sb.append( String.format( " %-15s Specificity         TN   / ( TN + FP )\n\n", "Spcfty" ) );

      sb.append( String.format( " %-15s F1             2 * P * R / ( P + R )\n", "F1" ) );
      sb.append( String.format( " %-15s Attribute F1        F1   / Neoplasm F1\n", "Attr/Neo F1" ) );
      sb.append( "* See (Bethard et al., 2016) \"SemEval-2016 Task 12: Clinical TempEval\"   https://www.aclweb.org/anthology/S16-1165\n\n" );

      sb.append( String.format( " %-15s Neoplasm Header.  Gold and System Neoplasm Match Information.\n", "TRUE POSITIVE" ) );
      sb.append( String.format( " %-15s Neoplasm Header.  System Neoplasm Information.  No matching Gold Neoplasm.\n", "FALSE POSITIVE" ) );
      sb.append( String.format( " %-15s Neoplasm Header.  Gold Neoplasm Information.    No matching System Neoplasm.\n\n", "FALSE NEGATIVE" ) );

      sb.append( "For each Attribute in a True Positive Neoplasm Summary, there may be a mark preceding the Attribute values.\n" );
      sb.append( String.format( " %-15s No Matching Gold Attribute values.\n", "FP" ) );
      sb.append( String.format( " %-15s No Matching System Attribute values.\n", "FN" ) );
      sb.append( String.format( " %-15s Some Matching Gold and System Attribute values.\n", "MX" ) );

      return sb.toString();
   }
}
