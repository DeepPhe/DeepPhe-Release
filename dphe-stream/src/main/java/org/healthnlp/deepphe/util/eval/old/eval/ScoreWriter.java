package org.healthnlp.deepphe.util.eval.old.eval;

import org.apache.log4j.Logger;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.healthnlp.deepphe.EvalSummarizer.PATIENT_ID;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/23/2019
 */
final class ScoreWriter {

   static private final Logger LOGGER = Logger.getLogger( "ScoreWriter" );

   private ScoreWriter() {
   }

   static void saveScoreFile( final String cancerType,
                              final Collection<String> requiredColumnNames,
                              final Collection<String> scoringColumnNames,
                              final File systemFile,
                              final EvalCorpus corpus ) {
//      final File output = new File( systemFile.getParentFile(), systemFile.getName() + "_score.txt" );
      final File output = new File( systemFile.getParentFile(), cancerType + "_score.txt" );
      LOGGER.info( "Writing scores to " + output.getPath() );
      final File featureDir = new File( systemFile.getParentFile(), "feature_score" );
      featureDir.mkdirs();

      try ( Writer writer = new BufferedWriter( new FileWriter( output ) ) ) {
         final String stars
               = String.format( "**************************************%"
                                + cancerType.length()
                                + "s**************************************\n\n", "" )
                       .replace( ' ', '*' );
         writer.write( stars );
         writer.write(
               "***                                  " + cancerType + "                                    ***\n\n" );
         writer.write( stars );
         writer.write( "Corpus Patient Score:\n" + getCorpusPatientF1s( corpus ) + "\n" );
         writer.write( "Corpus Neoplasm Score:\n" + getPureF1( corpus ) + "\n" );
//         writer.write( "Attribute Score:\n" + getPropertyF1( corpus ) + "\n" );
         writer.write( "Corpus Attribute Score:\n" + getPropertyA_F1( corpus, false ) + "\n" );
         writer.write(
               "Attribute Scores:\n" + getPropertyA_F1s( requiredColumnNames, scoringColumnNames, corpus, false ) + "\n" );
         writer.write(
               "Attribute Scores, Simple:\n" + getPropertyA_F1s( requiredColumnNames, scoringColumnNames, corpus, true ) +
               "\n" );
         writer.write( stars + "\n" );
         for ( EvalPatient patient : corpus.getPatients() ) {
            savePatientScore( writer, featureDir, requiredColumnNames, scoringColumnNames, patient );
         }
         writer.write( createWikiTable( cancerType, requiredColumnNames, scoringColumnNames, corpus ) );
         writer.write( createExcelColumns( cancerType, requiredColumnNames, scoringColumnNames, corpus ) );
         writer.write( getLegend() );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         System.exit( -1 );
      }
   }

   static private void savePatientScore( final Writer writer,
                                         final File featureDir,
                                         final Collection<String> requiredNames,
                                         final Collection<String> scoringNames,
                                         final EvalPatient patient ) throws IOException {
      writer.write(
            "----------------------------------  " + patient.getId() + "  ----------------------------------\n\n" );
      writer.write( getPureF1( "Neoplasm:", patient ) );
      writer.write( getPropertyF1( "Attribute:", patient ) + "\n" );
      final Collection<EvalNeoplasm> tp = new ArrayList<>();
      final Collection<EvalNeoplasm> fp = new ArrayList<>();
      final Collection<EvalNeoplasm> fn = new ArrayList<>();
      for ( EvalNeoplasm pair : patient.getNeoplasmPairs() ) {
         if ( pair.getTP() > 0 ) {
            tp.add( pair );
         } else if ( pair.getFP() > 0 ) {
            fp.add( pair );
         } else {
            fn.add( pair );
         }
      }
      for ( EvalNeoplasm pair : tp ) {
         saveTP( writer, patient.getId(), featureDir, requiredNames, scoringNames, pair );
      }
      for ( EvalNeoplasm pair : fp ) {
         saveFP( writer, requiredNames, scoringNames, pair );
      }
      for ( EvalNeoplasm pair : fn ) {
         saveFN( writer, requiredNames, scoringNames, pair );
      }
   }

   static private void saveTP( final Writer writer,
                               final String patientId,
                               final File featureDir,
                               final Collection<String> requiredNames,
                               final Collection<String> scoringNames,
                               final EvalNeoplasm evalNeoplasm ) throws IOException {
      final NeoplasmSummary gold = evalNeoplasm.getGoldSummary();
      final NeoplasmSummary system = evalNeoplasm.getSystemSummary();
      writer.write( "\nTRUE POSITIVE     " + gold.getId() );
      writer.write( "\n==== ========     " + system.getId() + "\n\n" );
      writer.write( getPropertyF1( evalNeoplasm ) + "\n" );
      for ( String required : requiredNames ) {
         final String goldValue = gold.getAttribute( required );
         final String systemValue = system.getAttribute( required );
         final EvalUris evalUris = new EvalUris( required, goldValue, systemValue );
         writer.write( getScoreRow( "*" + required, evalUris ) );
         writer.write( getValueRow( goldValue, systemValue, evalUris ) );
      }
      for ( String scoring : scoringNames ) {
         final String goldValue = gold.getAttribute( scoring );
         final String systemValue = system.getAttribute( scoring );
         final EvalUris evalUris = new EvalUris( scoring, goldValue, systemValue );
         writer.write( getScoreRow( scoring, evalUris ) );
         writer.write( getValueRow( goldValue, systemValue, evalUris ) );
         final int nameEnd = Math.min( 30, scoring.length() );
         final Writer featureWriter
               = new FileWriter( new File( featureDir,
                                           scoring.replace( ' ', '_' )
                                                  .replace( '(', '_' )
                                                  .replace( ')', '_' )
                                                  .replace( ':', '_' )
                                                  .replace( ';', '_' )
                                                  .replace( '-', '_' )
                                                  .substring( 0,nameEnd ) ), true );
         featureWriter.write( patientId
                              + "|" + systemValue
                              + "|" + goldValue
                              + "|" + dot4( evalUris.getF1_s() ).charAt( 0 )
                              + "|\n" );
         featureWriter.flush();
      }
      writer.write( "\n\n" );
   }


   static private void saveFP( final Writer writer,
                               final Collection<String> requiredNames,
                               final Collection<String> scoringNames,
                               final EvalNeoplasm evalNeoplasm ) throws IOException {
      final NeoplasmSummary system = evalNeoplasm.getSystemSummary();
      writer.write( "\nFALSE POSITIVE     " + system.getId() );
      writer.write( "\n===== ========\n\n" );
      for ( String required : requiredNames ) {
         final String systemValue = system.getAttribute( required );
         writer.write( getSystemValueRow( "*" + required, systemValue ) );
      }
      for ( String scoring : scoringNames ) {
         final String systemValue = system.getAttribute( scoring );
         writer.write( getSystemValueRow( scoring, systemValue ) );
      }
      writer.write( "\n\n" );
   }

   static private void saveFN( final Writer writer,
                               final Collection<String> requiredNames,
                               final Collection<String> scoringNames,
                               final EvalNeoplasm evalNeoplasm ) throws IOException {
      final NeoplasmSummary gold = evalNeoplasm.getGoldSummary();
      writer.write( "\nFALSE NEGATIVE     " + gold.getId() );
      writer.write( "\n===== ========\n\n" );
      for ( String required : requiredNames ) {
         final String goldValue = gold.getAttribute( required );
         writer.write( getGoldValueRow( "*" + required, goldValue ) );
      }
      for ( String scoring : scoringNames ) {
         final String goldValue = gold.getAttribute( scoring );
         writer.write( getGoldValueRow( scoring, goldValue ) );
      }
      writer.write( "\n\n" );
   }


   static private String getPureF1( final AbstractEvalObject evalThing ) {
      final String score1 = getTitleLine2( "TP", "FP", "FN", "TN", "Accur %", "P", "R", "Spcfty", "F1", "Audit" );
      final String score2 = getScoreLine2(
            noDot( evalThing.getTP() ),
            noDot( evalThing.getFP() ),
            noDot( evalThing.getFN() ),
            noDot( evalThing.getTN() ),
            evalThing.getAccuracy(),
            evalThing.getP(),
            evalThing.getR(),
            evalThing.getS(),
            evalThing.getF1(),
            evalThing.getAudit() );
      return score1 + "\n" + score2 + "\n";
   }

   static private String getPureF1( final String name, final AbstractEvalObject evalThing ) {
      final String score1 = getTitleLine( "", "TP", "FP", "FN", "TN", "Accur %", "P", "R", "Spcfty", "F1" );
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
      for ( EvalPatient patient : corpus.getPatients() ) {
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
      final double accuracy = (tp + tn) / (tp + tn + fp + fn);
      final double p = EvalMath.getPRS( tp, fp );
      final double r = EvalMath.getPRS( tp, fn );
      final double s = EvalMath.getPRS( tn, fp );
      final double n = EvalMath.getPRS( tn, fn );
      final double f1 = EvalMath.getF1( p, r, s, n );
      final String score1 = getTitleLine( "TP", "FP", "FN", "TN", "Accur %", "P", "R", "Spcfty", "F1" );
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

   static private String getPropertyF1( final AbstractEvalObject evalThing ) {
      final String score1 = getTitleLine( "", "TP", "FP", "FN", "TN", "Accur %", "P", "R", "Spcfty", "F1" );
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

   static private String getPropertyF1( final String name, final AbstractEvalObject evalThing ) {
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
      return score6 + "\n";
   }


   static private String getPropertyA_F1( final EvalCorpus corpus, boolean simple ) {
      final String score5 = String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            "TP", "FP", "FN", "TN", "Accur %", "P", "R", "Spcfty", "F1" );
      final double propertyF1 = corpus.getF1_s();
//      final double spanF1 = corpus.getF1();
      final String score6 = String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            noDot( corpus.getTP_() ),
            noDot( corpus.getFP_() ),
            noDot( corpus.getFN_() ),
            noDot( corpus.getTN_() ),
            percent2( corpus.getAccuracy_() ),
            dot4( corpus.getP_s() ),
            dot4( corpus.getR_s() ),
            dot4( corpus.getS_s() ),
            dot4( propertyF1 ) );
      return score5 + "\n" + score6 + "\n";
   }

   static private String getPropertyA_F1_title( final boolean simple ) {
      if ( simple ) {
         return String.format( "%-20s   %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n",
               "Attribute", "TP", "FP", "FN", "TN", "Accur %", "P", "R", "Spcfty", "F1", "Attr/Neo", "Attr*Neo" );
      }
      return String.format( "%-20s   %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n",
            "Attribute", "TP", "FP", "FN", "TN", "Accur %", "P", "R", "Spcfty", "F1" );
   }

   static private String getPropertyA_F1s( final Collection<String> requiredNames,
                                           final Collection<String> scoringNames,
                                           final EvalCorpus corpus,
                                           final boolean simple ) {
      final Map<String, Double> attributeTPs = new HashMap<>();
      final Map<String, Double> attributeTNs = new HashMap<>();
      final Map<String, Double> attributeFPs = new HashMap<>();
      final Map<String, Double> attributeFNs = new HashMap<>();
      corpus.fillAttributeScores( attributeTPs, attributeTNs, attributeFPs, attributeFNs, requiredNames, scoringNames, simple );
      final double spanF1 = corpus.getF1();
      final StringBuilder sb = new StringBuilder();
      sb.append( getPropertyA_F1_title( simple ) );
      for ( String required : requiredNames ) {
         if ( PATIENT_ID.equals( required ) ) {
            continue;
         }
         if ( !attributeTPs.containsKey( required ) ) {
            continue;
         }
         sb.append( getPropertyA_F1( required,
               attributeTPs.get( required ),
               0,
               0,
               0,
               spanF1, simple ) );
      }
      for ( String scoring : scoringNames ) {
         if ( !attributeTPs.containsKey( scoring ) ) {
            continue;
         }
         sb.append( getPropertyA_F1( scoring,
               attributeTPs.get( scoring ),
               attributeTNs.get( scoring ),
               attributeFPs.get( scoring ),
               attributeFNs.get( scoring ),
               spanF1, simple ) );
      }
      return sb.toString();
   }

   static private String getPropertyA_F1( final String name, final double tp, final double tn, final double fp,
                                          final double fn,
                                          final double spanF1, final boolean simple ) {
      final double p = EvalMath.getPRS( tp, fp );
      final double r = EvalMath.getPRS( tp, fn );
      final double s = EvalMath.getPRS( tn, fp );
      final double n = EvalMath.getPRS( tn, fn );
      final double f1 = EvalMath.getF1( p, r, s, n );
      final double accuracy = EvalMath.getAccuracy( tp, tn, fp, fn );
      if ( simple ) {
         final double aF1 = f1 / spanF1;
         final double aF1_2 = f1 * spanF1;
         return String.format( "%-20s   %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n",
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
               dot4( aF1 ),
               dot4( aF1_2 ) );
      }
      return String.format( "%-20s   %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n",
            name,
            noDot( tp ),
            noDot( fp ),
            noDot( fn ),
            noDot( tn ),
            percent2( accuracy ),
            dot4( p ),
            dot4( r ),
            dot4( s ),
            dot4( f1 ) );
   }

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

   static private String getTitleLine( final String name, final String tp, final String fp, final String fn,
                                       final String tn,
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

   static private String getTitleLine2( final String tp, final String fp, final String fn, final String tn,
                                        final String accuracy, final String p, final String r, final String s,
                                        final String f1, final String audit ) {
      return String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            tp,
            fp,
            fn,
            tn,
            accuracy,
            p,
            r,
            s,
            f1,
            audit );
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

   static private String getScoreLine2( final String tp, final String fp, final String fn, final String tn,
                                        final double accuracy, final double p, final double r, final double s,
                                        final double f1, final double audit ) {
      return String.format( " %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s",
            tp,
            fp,
            fn,
            tn,
            percent2( accuracy ),
            dot4( p ),
            dot4( r ),
            dot4( s ),
            dot4( f1 ),
            dot4( audit ) );
   }

   static private String getScoreLine( final String name, final String tp, final String fp, final String fn,
                                       final String tn,
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


   static private String getOneLineF1( final AbstractEvalObject evalThing ) {
      return "Accur %  " + percent2( evalThing.getAccuracy_() ) + "   F1  " + dot4( evalThing.getF1_s() );
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
            gold.replace( ";", " , " ),
            system.replace( ";", " , " ) );
   }

   static private String getGoldValueRow( final String name, final String gold ) {
      final String title = String.format( "%-20s  ", name );
      return title + String.format( "   Gold: %40s\n",
            gold.replace( ";", " , " ) );
   }

   static private String getSystemValueRow( final String name, final String system ) {
      final String title = String.format( "%-20s  ", name );
      return title + String.format( "   System: %40s\n",
            system.replace( ";", " , " ) );
   }


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
      return String.format( "%.0f", value * 100 ) + "%";
   }

   static private String percent2( final double value ) {
      if ( Double.isNaN( value ) ) {
         return "  0.00 %";
      }
      return String.format( "%6s", String.format( "%.2f", value * 100 ) );
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
      sb.append( String.format( " %-40s The Score of the Corpus for each Attribute type (macro, TP neoplasms only).\n\n", "Attribute Scores, Simple:" ) );

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
      sb.append( String.format( " %-15s Attribute F1        F1   / Neoplasm F1\n", "Attr/Neo" ) );
      sb.append( "* See (Bethard et al., 2016) \"SemEval-2016 Task 12: Clinical TempEval\"   https://www.aclweb.org/anthology/S16-1165\n\n" );
      sb.append( String.format( " %-15s Attribute F1 (2)    F1   * Neoplasm F1\n", "Attr*Neo" ) );
      sb.append( String.format( " %-15s Auditable Recall   2 * R / ( 1 + R )\n\n", "Audit" ) );

      sb.append( String.format( " %-15s Neoplasm Header.  Gold and System Neoplasm Match Information.\n", "TRUE POSITIVE" ) );
      sb.append( String.format( " %-15s Neoplasm Header.  System Neoplasm Information.  No matching Gold Neoplasm.\n", "FALSE POSITIVE" ) );
      sb.append( String.format( " %-15s Neoplasm Header.  Gold Neoplasm Information.    No matching System Neoplasm.\n\n", "FALSE NEGATIVE" ) );

      sb.append( "For each Attribute in a True Positive Neoplasm Summary, there may be a mark preceding the Attribute values.\n" );
      sb.append( String.format( " %-15s No Matching Gold Attribute values.\n", "FP" ) );
      sb.append( String.format( " %-15s No Matching System Attribute values.\n", "FN" ) );
      sb.append( String.format( " %-15s Some Matching Gold and System Attribute values.\n", "MX" ) );

      sb.append(
            "\nAuditable is essentially the maximum F1 that can be achieved by an auditor simply going to each reported neoplasm/attribute and correcting it 100%;\n" +
            "affirming that it is TP or removing it if FP.  FN and TN are not changed.\n" +
            "In other words, how high can the golden mean be without an auditor reading each and every document looking for FN neoplasms/attributes.\n" +
            "The auditor increases Precision (TP & FP) to 1 without changing system Recall (TP & FN).\n" +
            "This is important because correcting reported results just by clicking through the system findings is very fast when compared to reading all of the documents.\n" +
            "In addition, the user only has to click enough system findings to affirm a TP, which may be evident in the first tagged mention out of dozens (or the nth).\n" );
      sb.append(
            "\nAll non-required Attribute F1 scores are based upon all neoplasms, including FP and FN.  In other words, an attribute value that exists in an FN neoplasm\n" +
            " counts as an FN in the total attribute FN count.  An attribute value that exists in an FP neoplasm counts as an FP in the total attribute FP count.\n" );
      return sb.toString();
   }

   static private String createWikiTable( final String neoplasmType,
                                          final Collection<String> requiredProperties,
                                          final Collection<String> otherProperties,
                                          final EvalCorpus corpus ) {
      final String date = LocalDate.now().format( DateTimeFormatter.ISO_DATE );
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n\n-------------------------------------------------------------------\n" );
      sb.append( "***  Mediawiki Table  ***\n\n" );
      sb.append( "{| class=\"wikitable\"\n" );
      sb.append( "!Name\n" );
      sb.append( "![[Error Analysis Run " ).append( date ).append( "| F1 from " ).append( date ).append( "]]\n" );
      sb.append( "|-\n" );
      sb.append( "|'''" ).append( neoplasmType ).append( "'''\n" );
      sb.append( getWikiA_F1s( requiredProperties, otherProperties, corpus ) ).append( "\n" );
      sb.append( "|}\n" );
      return sb.toString();
   }

   static private String getWikiA_F1s( final Collection<String> requiredNames,
                                       final Collection<String> scoringNames,
                                       final EvalCorpus corpus ) {
      final Map<String, Double> attributeTPs = new HashMap<>();
      final Map<String, Double> attributeTNs = new HashMap<>();
      final Map<String, Double> attributeFPs = new HashMap<>();
      final Map<String, Double> attributeFNs = new HashMap<>();
      corpus.fillAttributeScores( attributeTPs, attributeTNs, attributeFPs, attributeFNs, requiredNames, scoringNames, false );
      final StringBuilder sb = new StringBuilder();
      for ( String required : requiredNames ) {
         if ( PATIENT_ID.equals( required ) ) {
            continue;
         }
         sb.append( createWikiF1( required, attributeTPs, attributeFPs, attributeFNs, attributeTNs ) );
      }
      for ( String scoring : scoringNames ) {
         sb.append( createWikiF1( scoring, attributeTPs, attributeFPs, attributeFNs, attributeTNs ) );
      }
      return sb.toString();
   }

   static private double getAttributeValue( final Map<String, Double> map, final String attribute ) {
      final Double value = map.get( attribute );
      if ( value == null ) {
         return 0;
      }
      return value;
   }

   static private String createWikiF1( final String attribute,
                                       final Map<String, Double> attributeTPs,
                                       final Map<String, Double> attributeFPs,
                                       final Map<String, Double> attributeFNs,
                                       final Map<String, Double> attributeTNs ) {
      final double tp = getAttributeValue( attributeTPs, attribute );
      final double fp = getAttributeValue( attributeFPs, attribute );
      final double fn = getAttributeValue( attributeFNs, attribute );
      final double tn = getAttributeValue( attributeTNs, attribute );
      final double p = EvalMath.getPRS( tp, fp );
      final double r = EvalMath.getPRS( tp, fn );
      final double s = EvalMath.getPRS( tn, fp );
      final double n = EvalMath.getPRS( tn, fn );
      final double f1 = EvalMath.getF1( p, r, s, n );
      final StringBuilder sb = new StringBuilder();
      sb.append( "|" ).append( attribute ).append( "|" );
      if ( Double.isNaN( f1 ) ) {
         sb.append( "-" );
      } else {
         double cutoff = 0.7;
         if ( f1 < cutoff ) {
            sb.append( "<span style=\"color: red\">" );
         }
         sb.append( String.format( "%.4f", f1 ) );
         if ( f1 < cutoff ) {
            sb.append( "</span>" );
         }
      }
      sb.append( "\n|-" );
      return sb.toString();
   }

   static private String createExcelColumns(
         final String label,
         final Collection<String> requiredNames,
         final Collection<String> scoringNames,
         final EvalCorpus corpus ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( "\n\n-------------------------------------------------------------------\n" );
      sb.append( "***  Excel Table  ***\n\n" );
      final String date = LocalDate.now().format( DateTimeFormatter.ISO_DATE );
      sb.append( "Date,Neoplasm" );
      for ( String required : requiredNames ) {
         if ( PATIENT_ID.equals( required ) ) {
            continue;
         }
         sb.append( "," ).append( required );
      }
      for ( String other : scoringNames ) {
         sb.append( "," ).append( other );
      }
      sb.append( ",,Auditable" );
      sb.append( "\n" );

      sb.append( "\"" ).append( date ).append( "\"," ).append( createExcelColumn( corpus ) );

      final Map<String, Double> attributeTPs = new HashMap<>();
      final Map<String, Double> attributeTNs = new HashMap<>();
      final Map<String, Double> attributeFPs = new HashMap<>();
      final Map<String, Double> attributeFNs = new HashMap<>();
      corpus.fillAttributeScores( attributeTPs, attributeTNs, attributeFPs, attributeFNs, requiredNames, scoringNames, false );

      for ( String required : requiredNames ) {
         if ( PATIENT_ID.equals( required ) ) {
            continue;
         }
         sb.append( "," )
           .append( createExcelColumn( required, attributeTPs, attributeTNs, attributeFPs, attributeFNs ) );
      }
      for ( String scoring : scoringNames ) {
         sb.append( "," )
           .append( createExcelColumn( scoring, attributeTPs, attributeTNs, attributeFPs, attributeFNs ) );
      }
      sb.append( ",," ).append( dot4( corpus.getAudit() ) );
      sb.append( "\n\n" );
      return sb.toString();
   }


   static private String createExcelColumn( final AbstractEvalObject thing ) {
      return "\""
             + noDot( thing.getTP() )
             + "," + noDot( thing.getFP() )
             + "," + noDot( thing.getFN() )
             + "  " + percent( thing.getAccuracy() ) + "  "
             + "  " + dot4( thing.getF1() )
             + "\"";
   }

   static private String createExcelColumn( final String attribute,
                                            final Map<String, Double> attributeTPs,
                                            final Map<String, Double> attributeTNs,
                                            final Map<String, Double> attributeFPs,
                                            final Map<String, Double> attributeFNs ) {
      final double tp = getAttributeValue( attributeTPs, attribute );
      final double tn = getAttributeValue( attributeTNs, attribute );
      final double fp = getAttributeValue( attributeFPs, attribute );
      final double fn = getAttributeValue( attributeFNs, attribute );
      final double p = EvalMath.getPRS( tp, fp );
      final double r = EvalMath.getPRS( tp, fn );
      final double s = EvalMath.getPRS( tn, fp );
      final double n = EvalMath.getPRS( tn, fn );
      final double a = EvalMath.getAccuracy( tp, tn, fp, fn );
      final double f1 = EvalMath.getF1( p, r, s, n );
      return "\""
             + noDot( tp )
             + "," + noDot( fp )
             + "," + noDot( fn )
             + "  " + percent( a ) + "  "
             + "  " + dot4( f1 )
             + "\"";
   }

}
