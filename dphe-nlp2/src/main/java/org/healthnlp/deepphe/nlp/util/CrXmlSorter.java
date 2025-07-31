package org.healthnlp.deepphe.nlp.util;

import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {2/9/2024}
 */
public class CrXmlSorter {

   // Wanted (initial) file structure is:    kcr/[cancer]/[patient]/[doctype]/*.xml
   // cancer is brain breast crc lung ovarian prostate
   // doctype = epath or naxml  (plus others)

   static private final String KCR_ROOT = "C:/Spiffy/data/dphe_cr_xml/kcr";
   static private final String LTR_ROOT = "C:/Spiffy/data/dphe_cr_xml/ltr";

   static private void moveKcr_1_90_epath() {
      // Existing structure is epath/xmlfiles_Ascii/[patient]/*.xml
      final String xmlsPath = "C:\\Spiffy\\data\\dphe_cr_kcr_1_90\\epath\\xmlfiles_Ascii";
      // And of course there is no sorting by cancer, but we can get it here:
      final Map<String,String> patientCancers = readKcr_1_90_splits();
      try {
         final File[] patientDirs = new File( xmlsPath ).listFiles();
         for ( File patientDir : patientDirs ) {
            final String patient = patientDir.getName();
            String cancer = patientCancers.get( patient );
            if ( cancer.equals( "lung?" ) ) {
               cancer = "lung";
            } else if ( cancer.equals( "unknown primary" ) || cancer.trim().isEmpty() ) {
               cancer = "unknown";
            } else if ( cancer.equals( "could not find it in the gold annotations" ) ) {
               cancer = "nogold";
            }
            final File targetDir = new File( KCR_ROOT + "/" + cancer + "/" + patient + "/epath" );
            targetDir.mkdirs();
            final File[] xmls = patientDir.listFiles();
            for ( File xml : xmls ) {
               final Path source = Paths.get( xml.getAbsolutePath() );
               final Path target = Paths.get( targetDir.getAbsolutePath() + "\\" + xml.getName() );
               Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
            }
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   static private Map<String,String> readKcr_1_90_splits() {
      final String cancerSplits = "C:\\Spiffy\\data\\dphe_data\\kcr\\TRAIN_split.bsv";
      final int patientColumn = 1;
      final int cancerColumn = 3;
      final Map<String,String> patientCancers = new HashMap<>( 90 );
      try ( BufferedReader reader = new BufferedReader( new FileReader( cancerSplits ) ) ) {
         reader.readLine();
         String line;
         for ( int i=0; i<90; i++ ) {
            line = reader.readLine();
            final String[] splits = StringUtil.fastSplit( line, '|' );
            patientCancers.put( splits[ patientColumn ], splits[ cancerColumn ] );
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
      return patientCancers;
   }

   static private Map<String,String> readKcrCancers() {
      final Collection<String> cancerSplits = Arrays.asList(
            "C:\\Spiffy\\data\\dphe_data\\kcr\\DEV_split.bsv",
            "C:\\Spiffy\\data\\dphe_data\\kcr\\TEST_split.bsv",
            "C:\\Spiffy\\data\\dphe_data\\kcr\\TRAIN_split.bsv" );
      final int patientColumn = 1;
      final int cancerColumn = 3;
      final Map<String,String> patientCancers = new HashMap<>();
      for ( String cancerSplit : cancerSplits ) {
         try ( BufferedReader reader = new BufferedReader( new FileReader( cancerSplit ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               if ( !line.isEmpty() ) {
                  final String[] splits = StringUtil.fastSplit( line, '|' );
                  patientCancers.put( splits[ patientColumn ], getCancerName( splits[ cancerColumn ] ) );
               }
               line = reader.readLine();
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
            System.exit( 1 );
         }
      }
      return patientCancers;
   }

   static private Map<String,String> readKcrSplits() {
      final Collection<String> splitFiles = Arrays.asList(
            "C:\\Spiffy\\data\\dphe_data\\kcr\\DEV_split.bsv",
            "C:\\Spiffy\\data\\dphe_data\\kcr\\TEST_split.bsv",
            "C:\\Spiffy\\data\\dphe_data\\kcr\\TRAIN_split.bsv" );
      final int patientColumn = 1;
      final Map<String,String> patientSplits = new HashMap<>();
      for ( String splitFile : splitFiles ) {
         final String split = new File( splitFile ).getName()
                                                     .replace( "_split.bsv", "" )
                                                     .toLowerCase();
         try ( BufferedReader reader = new BufferedReader( new FileReader( splitFile ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               if ( !line.isEmpty() ) {
                  final String[] splits = StringUtil.fastSplit( line, '|' );
                  patientSplits.put( splits[ patientColumn ], split );
               }
               line = reader.readLine();
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
            System.exit( 1 );
         }
      }
      final Collection<String> moreSplitFiles = Arrays.asList(
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\kcr\\all_splits\\id_train.txt",
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\kcr\\all_splits\\id_dev.txt",
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\kcr\\all_splits\\id_test.txt" );
      for ( String splitFile : moreSplitFiles ) {
         final String split = new File( splitFile ).getName().replace( "id_", "" )
                                                   .replace( ".txt", "" );
         try ( BufferedReader reader = new BufferedReader( new FileReader( splitFile ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               patientSplits.put( line, split );
               line = reader.readLine();
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
            System.exit( 1 );
         }
      }
      final Collection<String> moreSplitFiles2 = Arrays.asList(
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\kcr_2\\all_splits\\id_train.txt",
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\kcr_2\\all_splits\\id_dev.txt",
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\kcr_2\\all_splits\\id_test.txt" );
      for ( String splitFile : moreSplitFiles2 ) {
         final String split = new File( splitFile ).getName().replace( "id_", "" )
                                                   .replace( ".txt", "" );
         try ( BufferedReader reader = new BufferedReader( new FileReader( splitFile ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               patientSplits.put( line, split );
               line = reader.readLine();
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
            System.exit( 1 );
         }
      }
      return patientSplits;
   }

   static private Map<String,String> readLtrSplits() {
      final Collection<String> splitFiles = Arrays.asList(
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\ltr\\all_splits\\id_train.txt",
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\ltr\\all_splits\\id_dev.txt",
            "C:\\Spiffy\\data\\dphe_cr\\Eval\\ltr\\all_splits\\id_test.txt" );
      final Map<String,String> patientSplits = new HashMap<>();
      for ( String splitFile : splitFiles ) {
         final String split = new File( splitFile ).getName().replace( "id_", "" )
                                                   .replace( ".txt", "" );
         try ( BufferedReader reader = new BufferedReader( new FileReader( splitFile ) ) ) {
            String line = reader.readLine();
            while ( line != null ) {
               patientSplits.put( line, split );
               line = reader.readLine();
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
            System.exit( 1 );
         }
      }
      return patientSplits;
   }

   static private String getCancerName( final String cancer ) {
      if ( cancer.equals( "lung?" ) ) {
         return "lung";
      } else if ( cancer.equals( "unknown primary" ) ) {
         return "unknown";
      } else if ( cancer.equals( "could not find it in the gold annotations" ) ) {
         return  "nogold";
      }
      return cancer;
   }


   static private void moveKcr_1_90_naxml() {
      // Existing structure is naaccr/*.xml
      final String xmlsPath = "C:\\Spiffy\\data\\dphe_cr_kcr_1_90\\naaccr";
      final Map<String,String> patientCancers = readKcr_1_90_splits();
      try {
         final File[] naxmls = new File( xmlsPath ).listFiles();
         for ( File naxml : naxmls ) {
            final String patient = naxml.getName().replace( ".xml", "" );
            final String cancer = getCancerName( patientCancers.get( patient ) );

            final File targetDir = new File( KCR_ROOT + "/" + cancer + "/" + patient + "/naxml" );
            targetDir.mkdirs();
            final Path source = Paths.get( naxml.getAbsolutePath() );
            final Path target = Paths.get( targetDir.getAbsolutePath() + "\\" + naxml.getName() );
            Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   static private void moveKcr_90_300_epath() {
      // Existing structure is [doctype]/[cancer]/*.xml
      final String xmlsPath = "C:\\Spiffy\\data\\dphe_cr_kcr_90_300\\epath";
      try {
         final File[] cancers = new File( xmlsPath ).listFiles();
         for ( File cancer : cancers ) {
            final File[] xmls = cancer.listFiles();
            for ( File xml : xmls ) {
               final String patient = xml.getName().substring( 0, xml.getName().indexOf( '_' ) );
               final File targetDir = new File( KCR_ROOT + "/" + cancer.getName().toLowerCase()
                        + "/" + patient + "/epath" );
               targetDir.mkdirs();
               final Path source = Paths.get( xml.getAbsolutePath() );
               final Path target = Paths.get( targetDir.getAbsolutePath() + "\\" + xml.getName() );
               Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
            }
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   static private void moveKcr_90_300_naxml() {
      // Existing structure is [doctype]/[cancer]/*.xml
      final String xmlsPath = "C:\\Spiffy\\data\\dphe_cr_kcr_90_300\\naaccr";
      try {
         final File[] cancers = new File( xmlsPath ).listFiles();
         for ( File cancer : cancers ) {
            final File[] xmls = cancer.listFiles();
            for ( File xml : xmls ) {
               final String patient = xml.getName().replace( ".xml", "" );
               final File targetDir = new File( KCR_ROOT + "/" + cancer.getName().toLowerCase()
                     + "/" + patient + "/naxml" );
               targetDir.mkdirs();
               final Path source = Paths.get( xml.getAbsolutePath() );
               final Path target = Paths.get( targetDir.getAbsolutePath() + "\\" + xml.getName() );
               Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
            }
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   static private Collection<String> readKcrExtendedPatients() {
      final String extendedPath = "C:\\Spiffy\\data\\dphe_cr_extended\\corpus_for_3_cancers";
      return Arrays.stream( Objects.requireNonNull( new File( extendedPath ).listFiles() ) )
                   .map( File::getName )
                   .collect( Collectors.toList() );
   }

   static private void moveKcrExtendedText() {
      // Existing structure is [patient]/[filename(patient)]
      final String extendedPath = "C:\\Spiffy\\data\\dphe_cr_extended\\corpus_for_3_cancers";
      final Map<String,String> patientCancers = readKcrCancers();
      try {
         final String[] patients = new File( extendedPath ).list();
         for ( String patient : patients ) {
            final String cancer = patientCancers.getOrDefault( patient, "unknown" );
            final File targetDir = new File( KCR_ROOT + "/" + cancer
                  + "/" + patient + "/csv_text" );
            targetDir.mkdirs();
            final File sourceFile = new File( extendedPath + "/" + patient + "/" + patient );
            final Path source = Paths.get( sourceFile.getAbsolutePath() );
            final Path target = Paths.get( targetDir.getAbsolutePath() + "\\" + patient + ".txt" );
            Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   static private void printKcrTree() {
      final Comparator<String> comparator = new NumberedSuffixComparator();
      // Wanted tree structure is:    [registry]/[cancer]/[split]/[patient]/[doctype]/[filename]
      final String treeFile = "C:/Spiffy/data/dphe_cr_xml/Kcr_epath_Tree.txt";
      final List<String> splits = Arrays.asList( "train", "dev", "test" );
      final Map<String, String> patientSplits = readKcrSplits();
      try ( Writer writer = new FileWriter( treeFile ) ) {
         writer.write( "kcr\n" );
         final Collection<String> cancers = Arrays.asList( "brain", "breast", "crc", "lung", "ovarian", "prostate" );
         for ( String cancer : cancers ) {
            writer.write( "   " + cancer + "\n" );
            final List<String> patients = Arrays.asList(
                  Objects.requireNonNull( new File( KCR_ROOT, cancer ).list() ) );
            patients.sort( comparator );
            for ( String split : splits ) {
               writer.write( "      " + split + "\n" );
               for ( String patient : patients ) {
                  if ( patientSplits.get( patient ) == null || !patientSplits.get( patient ).equals( split ) ) {
                     if ( patientSplits.get( patient ) == null ) {
                        System.out.println( "No split for KCR patient " + patient );
                     }
                     continue;
                  }
//                  writer.write( "         " + patient + "\n" );
                  final File patientDir = new File( KCR_ROOT + "/" + cancer + "/" + patient );
                  final List<String> types = Arrays.asList(
                        Objects.requireNonNull( patientDir.list() ) );
                  types.sort( comparator );
                  for ( String type : types ) {
                     if ( !type.toLowerCase().equals( "epath" ) ) {
                        continue;
                     }
//                     writer.write( "            " + type + "\n" );
                     final List<String> files = Arrays.asList(
                           Objects.requireNonNull( new File( patientDir, type ).list() ) );
                     if ( !files.isEmpty() ) {
                        writer.write( "         " + patient + "\n" );
                        files.sort( comparator );
                        for ( String file : files ) {
//                        writer.write( "               " + file + "\n" );
                           writer.write( "            " + file + "\n" );
                        }
                     }
                  }
               }
            }
         }
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   static private void printLtrTree() {
         final Comparator<String> comparator = new NumberedSuffixComparator();
         // Wanted tree structure is:    [registry]/[cancer]/[split]/[patient]/[doctype]/[filename]
         final String treeFile = "C:/Spiffy/data/dphe_cr_xml/Ltr_epath_Tree.txt";
         final List<String> splits = Arrays.asList( "train", "dev", "test" );
         final Map<String,String> patientSplits = readLtrSplits();
         try ( Writer writer = new FileWriter( treeFile ) ) {
            writer.write( "ltr\n" );
            final Collection<String> cancers = Arrays.asList( "brain", "breast", "crc", "lung", "ovarian", "prostate" );
            for ( String cancer : cancers ) {
               writer.write( "   " + cancer + "\n" );
               final List<String> patients = Arrays.asList(
                     Objects.requireNonNull( new File( LTR_ROOT, cancer ).list() ) );
               patients.sort( comparator );
               for ( String split : splits ) {
                  writer.write( "      " + split + "\n" );
                  for ( String patient : patients ) {
                     if ( patientSplits.get( patient ) == null || !patientSplits.get( patient ).equals( split ) ) {
                        if ( patientSplits.get( patient ) == null ) {
                           System.out.println( "No split for LTR patient " + patient );
                        }
                        continue;
                     }
//                     writer.write( "         " + patient + "\n" );
                     final File patientDir = new File( LTR_ROOT + "/" + cancer + "/" + patient );
                     final List<String> types = Arrays.asList(
                           Objects.requireNonNull( patientDir.list() ) );
                     types.sort( comparator );
                     for ( String type : types ) {
                        if ( !type.toLowerCase().equals( "epath" ) ) {
                           continue;
                        }
//                        writer.write( "            " + type + "\n" );
                        final List<String> files = Arrays.asList(
                              Objects.requireNonNull( new File( patientDir, type ).list() ) );
                        if ( !files.isEmpty() ) {
                           writer.write( "         " + patient + "\n" );
                           files.sort( comparator );
                           for ( String file : files ) {
   //                           writer.write( "               " + file + "\n" );
                              writer.write( "            " + file + "\n" );
                           }
                        }
                     }
                  }
               }
            }
         } catch ( IOException ioE ) {
            System.err.println( ioE.getMessage() );
            System.exit( 1 );
         }

   }


   public static void main( String[] args ) {
//      moveKcr_1_90_epath();
//      moveKcr_1_90_naxml();
//      moveKcr_90_300_epath();
//      moveKcr_90_300_naxml();
//      moveKcrExtendedText();
      printKcrTree();
      printLtrTree();
   }

}
