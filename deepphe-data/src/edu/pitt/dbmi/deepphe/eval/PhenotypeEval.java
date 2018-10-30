package edu.pitt.dbmi.deepphe.eval;

import org.apache.ctakes.cancer.ae.ByUriRelationFinder;
import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.neo4j.Neo4jConnectionFactory;
import org.apache.ctakes.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 Evaluate the output of the patient summarizer by comparing two sets of files.
 Typically this is for comparing gold standard file(s) to output file(s) from the pipeline.
 Variable names and output messages make this assumption.
 This could also be used to compare the output of two runs of the system.
 Input can be a pair of files, or a pair of directories.
 The name of each file should contain Cancer|Tumor|Episode|Patient (see {@link #TYPES_PATTERN})
 Files should be bar separated (See {@link Record#FIELD_SEPARATOR}).
 The first line of each file should be a header line with the names of the fields (columns).
 The field names can be prefixed by various characters (- -doc *) to indicate fields to ignore, documentation, etc.
 See the isXXXXXField methods within {@link Fields} for details.
 If the value of a field is actually the concatenation of multiple values such as "right" and "left", they
 should be separated by a semicolon, for example "right;left" (see {@link Record#INTRA_VALUE_DELIMITER}).
 to indicate how to pair up a gold file with a corresponding candidate file.

 The word 'candidate' is used within this program to refer to annotations/output of the DeepPhe system (the patient summarizer),
 as opposed to the gold standard annotations.

 To evaluate a set of system output files against a set of gold standard files:
 edu.pitt.dbmi.deepphe.eval.PhenotypeEval
 -verbose
 .../DeepPhe/Datasets/GOLD/eval/
 .../DeepPhe/Datasets/system/output/EVAL/

 Can also add a third path argument to use histogram files to filter out some tumors.
 edu.pitt.dbmi.deepphe.eval.PhenotypeEval
 -verbose
 .../DeepPhe/Datasets/GOLD/eval/
 .../DeepPhe/Datasets/system/output/EVAL/
 .../DeepPhe/Datasets/system/output/histogram/   //// note when using this, you must add the args for type of filtering

 */
public class PhenotypeEval {

   private static final String VERSION = "2018.09.28.002";

   private static final String TYPE_OF_DATA_CANCER = "Cancer";
   private static final String TYPE_OF_DATA_TUMOR = "Tumor";
   private static final String TYPES_PATTERN = "("+ TYPE_OF_DATA_CANCER + "|" + TYPE_OF_DATA_TUMOR +")"; // "(Cancer|Tumor|Episode|Patient)";

   private enum DataType {
      CANCER, TUMOR, PATIENT
   }
   private static final String EMPTY_CANCER_INDICATOR = "__";

   private static final String CLASS_NAME = MethodHandles.lookup().lookupClass().getSimpleName();

   private static boolean READ_IN_AS_LOWER_CASE;               // When read in data from files, read in as lower case to avoid any case-related mis-matches
   private static boolean PRINT_DEBUG_DETAIL;
   private static boolean PRINT_DEBUG_FINE_DETAIL;

   private static boolean PRINT_WIKI_OUTPUT;

   private static boolean STRICT_VALUE_CALCULATION;           // input parm
   private static boolean PRINT_RECORD_LEVEL_STATS;           // input parm
   private static boolean LIST_DETAILED_RECORD_STATS;         // input_parm
   private static boolean INCLUDE_THOSE_WITHOUT_LOCATION;         // optional input_parm to include those unusual ones where there is no location
   private static boolean DISALLOW_OPPOSITE_LATERALITY;           // optional input parm
   private static boolean IGNORE_IF_DIFFER_ONLY_BY_SIZE;          // optional input parm
   private static boolean IGNORE_TNM_WITH_X;                      // not a parm, just set here in code.
   private static boolean IGNORE_TNM_PREFIX;                      // not a parm, just set here in code.

   static private Logger LOGGER;

   static ArrayList<String> variation;
   static String variationArg;
   static File gold;
   static File candidate;
   static File histogramDirectory;
   private static int idCounter;

   public enum DataSet {
      GOLD, CANDIDATE
   }

   public static void initializeStatics() {

      READ_IN_AS_LOWER_CASE = false;
      PRINT_DEBUG_DETAIL = false;      // for debug/development. change to false before runs to give to other people
      PRINT_DEBUG_FINE_DETAIL = false; // for debug/development. change to false before runs to give to other people
      PRINT_WIKI_OUTPUT = true;        // enable when not debugging/developing other parts

      STRICT_VALUE_CALCULATION = false;   // input parm
      PRINT_RECORD_LEVEL_STATS = false;   // input parm
      LIST_DETAILED_RECORD_STATS = false; // input_parm
      INCLUDE_THOSE_WITHOUT_LOCATION = false; // optional input_parm to include those unusual ones where there is no location
      DISALLOW_OPPOSITE_LATERALITY = false;   // optional input parm, can be used to prevent left <some body part> from aligning with right <same body part>
      IGNORE_IF_DIFFER_ONLY_BY_SIZE = true;   // optional input parm
      IGNORE_TNM_WITH_X = true;               // not a parm, just set here in code.
      IGNORE_TNM_PREFIX = true;               // not a parm, just set here in code.

      LOGGER = Logger.getLogger(CLASS_NAME);
      idCounter = 1000;                  // start at 1000 not at 1 so don't have to worry about weird ordering of 1,10,2,3,...
      // Minimum set of fields needed to be considered a match
      fieldsToMatch = new ArrayList<>();
      fieldsToMatch.add(Fields.patientIdField);
      fieldsToMatch.add(Fields.bodyLocationField);
      LOGGER.fine("Number of fields required to match in order to align two records: " + fieldsToMatch.size());

      variation = new ArrayList<>();
      variationArg = null;
      gold = null;
      candidate = null;
      histogramDirectory = null;

   }

   // compare two files or two directories of files
   public static void main(String[] args) throws IOException {

      System.out.println("Started: " + CLASS_NAME + " at " + new Date() + " using version " + VERSION);
      initializeStatics();

      if (args == null || args.length < 2) {
         System.err.println("Usage:   java " + CLASS_NAME + " [-verbose|-print] [-strict] [-include-those-without-location] [-disallow-opposite-laterality] [-include-those-differ-only-by-size] <gold .bsv file or dir> <candidate .bsv file or dir>");
         System.err.println("Example: java " + CLASS_NAME + " -verbose  /DeepPhe/data/sample/eval/  /DeepPhe/output/melanoma/eval/");
         System.err.println("Example: java " + CLASS_NAME + " -verbose  /@data/gold/DeepPhe_Gold_Cancer.bsv  /@data/breast/DeepPhe_Evaluation_Cancer.bsv");
         System.exit(-1);
      }

      System.out.println(" with arguments:");
      for (String s: args) {
         System.out.println("  " + quoted(s)); // quoted for easy copy/paste in windows if a path contains a blank
      }
      System.out.println();

      if (args[0].equals("-test") && args[1].equals("-new")) {
         try {
            runInternalTests(Tests.NEW);
         } catch (NullPointerException e) {
            System.err.println("Caught NPE from runInternalTests() " + e);
            throw e;
         }
      }
      if (args[0].equals("-test") && args[1].equals("-test")) {
         try {
            runInternalTests(Tests.ALL);
         } catch (NullPointerException e) {
            System.err.println("Caught NPE from runInternalTests() " + e);
            throw e;
         }

      }


      for (String s : args) {
         if ("-strict".equals(s)) {
            STRICT_VALUE_CALCULATION = true;
         } else if ("-verbose".equals(s)) {
            LIST_DETAILED_RECORD_STATS = true;
         } else if ("-include-those-without-location".equals(s)) {
            INCLUDE_THOSE_WITHOUT_LOCATION = true;
         } else if ("-disallow-opposite-laterality".equals(s)) {
            DISALLOW_OPPOSITE_LATERALITY = true;
         } else if ("-include-those-differ-only-by-size".equals(s)) {
            IGNORE_IF_DIFFER_ONLY_BY_SIZE = false;
         } else if ("-print".equals(s)) {
            PRINT_RECORD_LEVEL_STATS = true;
         } else if (s!=null && s.trim().length()==0) {
            // do nothing, skip blank parameter
         } else if (s!=null && s.startsWith("-variation=")) {
            variation.add(s.substring("-variation=".length()).trim());
         } else if (s!=null && s.startsWith("-variation-arg=")) {
            variationArg = s.substring("-variation-arg=".length()).trim();
         } else if (gold == null) {
            gold = new File(s);
         } else if (candidate == null) {
            candidate = new File(s);
         } else if (histogramDirectory == null) {
            if (!s.toLowerCase().contains("hist")) {
               throw new RuntimeException("expect 'hist' to be part of the directory path for histograms, found: " + s);
            }
            histogramDirectory = new File(s);
         }
      }

      if (!DISALLOW_OPPOSITE_LATERALITY) {
         System.out.println("DISALLOW_OPPOSITE_LATERALITY is false.\n  -disallow-opposite-laterality option was not used");
      }

      if (LIST_DETAILED_RECORD_STATS && PRINT_RECORD_LEVEL_STATS ) throw new IllegalArgumentException("Can't use both -print and -verbose");

      Map<String, Histogram> histograms = Histogram.loadHistograms(histogramDirectory, Histogram.HistogramType.MERGED_TUMOR); // maps patient to histogram
      //if (histograms!=null) printHistograms(histograms);

      process(gold, candidate, histograms);

      System.out.println("Completed: " + CLASS_NAME + " at " + new Date());
   }

   private static void printHistograms(Map<String, Histogram> histograms) {
      for (Map.Entry<String, Histogram> entry: histograms.entrySet()) {
         String patient = entry.getKey();
         Histogram h = entry.getValue();
         System.out.println("Histogram for patient " + patient + ":");
         for (Map.Entry<String, String> e: h.mapBodyLocationToCount.entrySet()) {
            String loc = e.getKey();
            String count = e.getValue();
            int i = (isNullOrZeroLength(count) ? 0 : Integer.parseInt(count));
            System.out.println(loc + "|" + count);
         }
      }
   }
   private static class Histogram {
      public enum HistogramType {
         MERGED_TUMOR, RAW
      }

      private String [] fieldNames = null;
      private String [] values = null;
      private ArrayList<String []> data = new ArrayList<>();
      private double meanByLocation;
      private double meanByLocAndLat;
      private Map<String, String> mapBodyLocationToCount = new HashMap<>();
      private Map<String, String> mapBodyLocAndLatToCount = new HashMap<>();
      private Map<String, String> mapTumorTypeToCount = new HashMap<>();
      private String patient = null;

      private int indexOfBodySite = -1;   // -1 indicates field name not found in header line when file is read
      private int indexOfLaterality = -1; // -1 indicates field name not found in header line when file is read
      private int indexOfCount = -1;      // -1 indicates field name not found in header line when file is read
      private int indexOfTumorType = -1;  // -1 indicates field name not found in header line when file is read

      private Histogram() { throw new RuntimeException("Use constructor that takes a String arg for patient"); }

      public Histogram(String patient, HistogramType histogramType, List<String> lines, String whereFrom) {
         this.patient = patient;
         loadHistogram(histogramType, lines, whereFrom, patient);
      }

      /**
       * @param directory Directory containing histogram files
       * @param histogramType MERGED or RAW
       * @return null if directory is null (histograms dir was not provided, no filtering by histogram requested)
       * @throws IOException
       */
      public static Map<String, Histogram> loadHistograms(File directory, HistogramType histogramType) throws IOException {
         if (directory == null) return null;
         Map<String, Histogram> map = new HashMap<>();
         if (directory.list()==null) throw new RuntimeException("Can't find files within " + directory);
         for (File f: directory.listFiles()) {
            if (f.isDirectory()) {
               LOGGER.fine("Skipping directory " + f + " when going through files in its parent " + directory);
               continue;
            }
            List<String> lines = readFile(f);
            String patient = f.getName().split("_")[1]; // file name such as "Melanoma_5_etc" results in "5" for patient
            if (!patient.toLowerCase().startsWith("patient")) {
               patient = "patient" + patient;
            }
            if (f.getName().toLowerCase().contains(histogramType.toString().toLowerCase())) {
               Histogram h = new Histogram(patient, histogramType, lines, f.getName());
               Histogram alreadyExisted = map.put(patient, h);
               if (alreadyExisted != null) {
                  throw new RuntimeException("already have a histogram for patient " + quoted(patient) + " in this map: " + map);
               }
            }
         }
         if (map.size()==0) {
            throw new RuntimeException("Found no histogram files for, or encountered error processing files for " + histogramType + " in " + directory);
         }
         return map;
      }

       // Constants from LocationHistogram
       public static final String BODYSITE_HEADING = "BodySite";
       public static final String LATERALITY_HEADING = "Laterality";
       public static final String COUNT_HEADING = "Count_Mentions";
       public static final String TUMOR_TYPE_HEADING = "TumorType";
       private void loadHistogram(HistogramType histogramType, List<String> lines, String whereFrom, String patient) {

         for (String line: lines) {
            if (fieldNames == null) {
               fieldNames = line.split(Record.FIELD_SEPARATOR);
               int i = 0;
               for (String field:fieldNames) {
                  if (field.equals(BODYSITE_HEADING)) indexOfBodySite = i;
                  if (field.equals(LATERALITY_HEADING)) indexOfLaterality = i;
                  if (field.equals(COUNT_HEADING)) indexOfCount = i;
                  if (field.equals(TUMOR_TYPE_HEADING)) indexOfTumorType = i;
                  i++;
               }

            } else {
               if (line.split(Record.FIELD_SEPARATOR).length == fieldNames.length-1 && line.length()!=0 && line.charAt(line.length()-1) == '|') {
                  line = line + "LastValueInLineNotFilledIn";
               }
               values = line.split(Record.FIELD_SEPARATOR);
               if (values.length!=fieldNames.length) {
                  System.err.println("line = " + quoted(line));
                  throw new RuntimeException("Ill-formed histogram file " + whereFrom + " has some lines with numbers of columns: " + fieldNames.length + " " + values.length);
               }
               data.add(values);
            }
         }
         try {
            calculateStats();
         } catch (Exception e) {
            System.err.println("Problem processing histogram file " + whereFrom + " for " + patient);
         }
      }

      private void calculateStats() {
         double countLoc = 0;
         try {
            for (String[] values : data) {
               addToValue(mapBodyLocationToCount, values[indexOfBodySite], values[indexOfCount]);
               addToValue(mapBodyLocAndLatToCount, createLocAndLatKey(values[indexOfBodySite], values[indexOfLaterality]), values[indexOfCount]);
               addToValue(mapTumorTypeToCount, values[indexOfTumorType], values[indexOfCount]);
               countLoc = countLoc + Double.parseDouble(values[indexOfCount]);
            }
         } catch (Exception e) {
            System.err.println("Unable to calculate stats");
            System.err.println("  indexOfBodySite = " + indexOfBodySite);
            System.err.println("  indexOfCount = " + indexOfCount);
            System.err.println("  indexOfLaterality = " + indexOfLaterality);
            System.err.println("  indexOfTumorType = " + indexOfTumorType);
            System.err.println("  values = " + values);
            System.err.println("  values.length = " + values.length);

            System.err.println("  mapBodyLocationToCount = " + mapBodyLocationToCount);
            System.err.println("  mapBodyLocationToCount.size() = " + mapBodyLocationToCount.size());
            System.err.println("  mapBodyLocAndLatToCount.size() = " + mapBodyLocAndLatToCount.size());
            System.err.println("Unable to calculate stats");
            throw e;
         }
         meanByLocation = countLoc / ((double)mapBodyLocationToCount.entrySet().size());
         meanByLocAndLat = countLoc / ((double)mapBodyLocAndLatToCount.entrySet().size());

      }

      private void addToValue(Map<String, String> map, String index, String count) {
         String current = map.get(index);
         if (current==null) {map.put(index, count); return;}
         map.put(index, addStringAsInts(current, count));

      }

      private int stringToInt(String s) {
         try {
            return Integer.parseInt(s);
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }

      private String intToString(int i) {
         return i+"";
      }
      private String addStringAsInts(String a, String b) {
         int i = stringToInt(a);
         int j = stringToInt(b);
         return intToString(i+j);
      }

      public String getDataAt(String key, int lineNum) {
         int i = 0;
         for (String field: fieldNames) {
            if (field.equals(key)) return data.get(lineNum)[i];
            i++;
         }
         throw new RuntimeException("Not found: " + key + " for line num " + lineNum);
      }

      /**
       * @param bodyLocation
       * @return -1 if no count found for the given bodyLocation
       */
      public int getCountByLoc(String bodyLocation) {
         String value = mapBodyLocationToCount.get(bodyLocation);
         if (value==null) return -1;
         int count = stringToInt(value);
         return count;
      }
      /**
       * @param bodyLocation
       * @param laterality
       * @return -1 if no count found for the given bodyLocation
       */
      public int getCountByLocAndLat(String bodyLocation, String laterality) {
         String value = mapBodyLocAndLatToCount.get(createLocAndLatKey(bodyLocation, laterality));
         if (value==null) return -1;
         int count = stringToInt(value);
         return count;

      }
   }

   private static String createLocAndLatKey(String bodyLocation, String laterality) {
      return bodyLocation + "|" + laterality;
   }

   private enum Tests {
      ALL, NEW
   }
   private static void runInternalTests(Tests which) {
      // some tests of the compare function

      Record r = new Record();

         if (which == Tests.NEW || which == Tests.ALL) {
         }

         if (which == Tests.ALL) {
            String f = null;
            try {
               f = "NoSuchFile-with.this.name-.xyz.aqw";
               List<String> lines = readFile(new File(f));
               throw new RuntimeException("readFile did not throw expected exception trying to read from " + f);
            } catch (IOException e) {
               System.out.println("\nCaught expected exception trying to read " + f + "\nException was: " + e.toString() + "\n");
            }

            try {
               f = "C:/PerfLogs/";
               List<String> lines = readFile(new File(f));
               throw new RuntimeException("readFile did not throw expected exception trying to read from " + f);
            } catch (IOException e) {
               System.out.println("\nCaught expected exception trying to read directory " + f + "\nException was: " + e.toString() + "\n");
            }

            try {
               f = "C:\\Windows\\WindowsUpdate.log";
               List<String> lines = readFile(new File(f));
               if (lines==null) throw new RuntimeException("readFile returned null trying to read from " + f + " which should exist on Windows 7.");
            } catch (IOException e) {
               System.out.println("\nCaught UNexpected exception trying to read " + f + "\nException was: " + e.toString() + "\n");
            }

            try {
               f = "C:\\Windows\\NoSuchDIRECTORY-with-this-name-xyz-aqw-DIRECTORY\\";
               List<String> lines = readFile(new File(f));
               throw new RuntimeException("readFile did not throw expected exception trying to read from " + f);
            } catch (IOException e) {
               System.out.println("\nCaught expected exception trying to read " + f + "\nException was: " + e.toString() + "\n");
            }

         }

         if (which == Tests.ALL) {
            List<String> list1 = new ArrayList<>();
            List<String> list2 = new ArrayList<>();
            expect(r.compare(list1, list2), 1); // both empty
            list1.add("right");
            list2.add("left");
            expect(r.compare(list1, list2), 0);
            list2.add("right");
            expect(r.compare(list1, list2), 0.5); // both contain "right"
            list1.add("left");
            expect(r.compare(list1, list2), 1);
            list1.add("middle");
            expect(r.compare(list1, list2), (2.0 / 3.0)); // both contain right and left
         }

         if (which == Tests.ALL) {
            List<String> listAbc = new ArrayList<>();
            List<String> listXyz = new ArrayList<>();
            listAbc.add("Negative"); listAbc.add("Negative"); listAbc.add("Positive");
            listXyz.add("Negative");
            //common = 1, denominator = size1+size2 - common = 3+1 - 1 ; result = 1/3
            expect(r.compare(listAbc, listXyz), 1.0 / 3.0);
         }

         if (which == Tests.ALL) {
            List<String> listAbc = new ArrayList<>();
            List<String> listXyz = new ArrayList<>();
            listAbc.add("Negative"); listAbc.add("Negative"); listAbc.add("Negative");
            listXyz.add("Positive");
            //common = 0, denominator = size1+size2 - common ; result = 3/9
            expect(r.compare(listAbc, listXyz), 0.0);
         }

         if (which == Tests.ALL) {
            List<String> listAbc = new ArrayList<>();
            List<String> listXyz = new ArrayList<>();
            listAbc.add("Negative"); listAbc.add("Negative"); listAbc.add("Negative");
            listXyz.add("Negative");
            //common = 1, denominator = size1+size2 - common = 3+1 - 1 ; result = 1/3
            expect(r.compare(listAbc, listXyz), 1.0 / 3.0);
         }

         if (which == Tests.ALL) {
            List<String> listA = new ArrayList<>();
            List<String> listB = new ArrayList<>();
            expect(r.compare(listA, listB), 1); // both empty
            listA.add("Ovary");
            listB.add("Fallopian Tube");
            expect(r.compare(listA, listB), 1); // special case, should match
            listA.add("Fallopian Tube");
            expect(r.compare(listA, listB), 1); // special case
            listB.add("Other");
            expect(r.compare(listA, listB), 0.5); // consider ovary + fallopian to be a single match with fallopian

            List<String> container = Arrays.asList("Bilateral");
            List<String> side = Arrays.asList("Right");
            expect(r.compare(container, side), 0.5);  // bilateral counts as half match with right
            expect(r.compare(side, container), 0.5);

            side = Arrays.asList("Left");
            expect(r.compare(container, side), 0.5); // bilateral counts as half match with left
            expect(r.compare(side, container), 0.5);

            container = Arrays.asList("Left");
            expect(r.compare(container, side), 1.0); // left is full match with left

            container = Arrays.asList("Right");
            expect(r.compare(container, side), 0.0); // right is complete mismatch with left

            side = Arrays.asList("");
            expect(r.compare(container, side), 0.0); // right is complete mismatch with empty string
            expect(r.compare(side, container), 0.0); // right is complete mismatch with empty string

            container = Arrays.asList("Left");
            side = new ArrayList<String>();
            expect(r.compare(container, side), 0.0); // left is complete mismatch with empty list
            expect(r.compare(side, container), 0.0); // left is complete mismatch with empty list
         }

         if (which == Tests.ALL) {
            // Test Lower_Right_Lobe can match Lower_Lobe because of the removeEmbeddedLaterality method
            PhenotypeEval evaluator = new PhenotypeEval();

            String header = "-geographically determined (yes/no)|*patient ID|-cancer  link|*body location|body location laterality|Diagnosis|tumor type|cancer type|histologic type|tumor extent|";
            Fields fields = Fields.loadFieldNames(header);
            Record gold = Record.loadRecord(fields, "no|patient00000001|C1|Lower_Right_Lobe|Right|dx|ttype|ctype|htype|extent", DataSet.GOLD, TYPE_OF_DATA_TUMOR);
            Record candidate = Record.loadRecord(fields, "no|patient00000001|C1|Lower_Right_Lobe|Right|dx|ttype|ctype|htype|extent", DataSet.CANDIDATE, TYPE_OF_DATA_TUMOR);
            Record missingEmbeddedLaterality = Record.loadRecord(fields, "no|patient00000001|C1|Lower_Lobe      |Right|dx|ttype|ctype|htype|extent", DataSet.CANDIDATE, TYPE_OF_DATA_TUMOR);

            String patientSeparator = " - - - - -  - - - - -  - - - - -  - - - - -  - - - - -  - - - - -  ";

            System.out.println(patientSeparator);
            System.out.println("Comparing gold to candidate:");
            expectMatchAndNotNull(gold, candidate);

            List<Record> goldAnnotations = new ArrayList<>();
            goldAnnotations.add(gold);
            Map<String, List<Record>> goldMap = new HashMap<>();
            List<Record> candidateAnnotations = new ArrayList<>();
            candidateAnnotations.add(candidate);
            Map<String, List<Record>> candidateMap = new HashMap<>();

            goldMap.put("patient00000001", goldAnnotations);
            candidateMap.put("patient00000001", candidateAnnotations);
            Pair<Record> bestPair;
            Record candidateMatchingBestGold;

            bestPair = evaluator.findPairWithBestScore(goldMap.get("patient00000001"), candidateAnnotations);
            candidateMatchingBestGold = bestPair.getValue2();
            if (goldMap.get("patient00000001") == candidateMatchingBestGold) throw new RuntimeException("findPairWithBestScore didn't find a candidate to match the gold");

            System.out.println(patientSeparator);
            System.out.println("Comparing best to candidate:");
            System.out.println(expectMatchAndNotNull(candidateMatchingBestGold, candidate));

            System.out.println(patientSeparator);
            System.out.println("Comparing best to gold:");
            System.out.println(expectMatchAndNotNull(candidateMatchingBestGold, gold));

            List<Record> missingEmbeddedLateralityList = new ArrayList<>();
            missingEmbeddedLateralityList.add(missingEmbeddedLaterality);
            System.out.println(patientSeparator);
            System.out.println("Comparing best from missingEmbeddedLaterality to gold:");
            bestPair = evaluator.findPairWithBestScore(goldMap.get("patient00000001"), missingEmbeddedLateralityList);
            if (goldMap.get("patient00000001") == missingEmbeddedLateralityList) throw new RuntimeException("findPairWithBestScore didn't find a candidate to match the gold");
            System.out.println(expectMatchAndNotNull(bestPair.getValue1(), missingEmbeddedLaterality));

            System.out.println("Testing containsAllIgnoreEmbedded, all should be true:");
            List<String> container = Arrays.asList("a_b", "this_Right_thing", "that_thing", "some_other_Left_thing");
            List<String> list = new ArrayList<String>(Arrays.asList("this_thing"));
            System.out.println(containsAllIgnoreEmbedded(container, list));
            list.add("that_Right_thing");
            System.out.println(containsAllIgnoreEmbedded(container, list));
            list.add("a_Left_b");
            System.out.println(containsAllIgnoreEmbedded(container, list));
            list.add("some_other_Left_thing");
            System.out.println(containsAllIgnoreEmbedded(container, list));
            list.add("extra_thing");
            System.out.println(!containsAllIgnoreEmbedded(container, list));

         }

         if (which != Tests.ALL) {
            try {  // Try to get the warning (RuntimeException) below to print after everything else by delaying it
               Thread.sleep(100L);
            } catch (InterruptedException e) {
               /* ignore */
            }

            System.err.println(new RuntimeException("Be aware did not run all tests. This should only happen when debugging a test."));
         }

      System.exit(0);
   }

   /**
    * if r == e (same object), or if records have field values that align, returns true; otherwise false
    * @param r if null, fails
    * @param e if null, fails
    */
   private static boolean expectMatchAndNotNull(Record r, Record e) {

      if (r==null || e == null) {
         if (r == null) {
            throw new RuntimeException("r is null, e = " + quoted(e.toString()));
         }
         if (e == null) {
            throw new RuntimeException("e is null, r = " + quoted(r.toString()));
         }
      }
      if (r==e) {
         return true;
         //System.out.println("Not expecting to find itself");
         //// Using System.out for stacktrace so it shows up in proper place within the other output
         //(new RuntimeException()).printStackTrace(System.out);
         //return;
      }
      if (r.getPatientName()==null || r.getPatientName().length()==0) {
         if (r.getPatientName()==null) System.out.println("r.getPatientName()==null");
         else if (r.getPatientName().length()==0) System.out.println("r.getPatientName().length()==0");
         System.out.println("for record " + r);
         (new RuntimeException()).printStackTrace(System.out);
         return false;
      }
      if (e.getPatientName()==null || e.getPatientName().length()==0) {
         if (e.getPatientName()==null) System.out.println("e.getPatientName()==null");
         else if (e.getPatientName().length()==0) System.out.println("e.getPatientName().length()==0");
         System.out.println("for record " + e);
         (new RuntimeException()).printStackTrace(System.out);
         return false;
      }
      System.out.println("Comparing " + r.getPatientName() + " to " + e.getPatientName());

      for (String field: r.getFields().getValueFieldNames()) {
         String gold = r.getValue( field );
         String expected = "";
         if (e.getValue(field) != null ) expected = e.getValue( field );

         List<String> goldValues = r.convertDelimitedValueToSortedList(gold);
         List<String> expectedValues = r.convertDelimitedValueToSortedList(expected);

         if (goldValues==null) {
            if (expectedValues!=null && expectedValues.size()>0) {
               System.out.println("ERROR: For field " + field + ", gold is '" + gold + "' but expected is '" + expected + "'.");
               System.out.println();
               (new RuntimeException()).printStackTrace(System.out);
               return false;
            }
         } else if (goldValues.size() != expectedValues.size()) {
            System.out.println("ERROR: For field " + field + ", value '" + gold + "' does not align with '" + expected + "' -- different sizes." );
            System.out.println();
            (new RuntimeException()).printStackTrace(System.out);
            return false;
         } else if (!containsAllIgnoreEmbedded(goldValues, expectedValues)) {
            System.out.println("ERROR: For field " + field + ", value '" + gold + "' does not align with '" + expected + "' -- gold does not contain all." );
            System.out.println();
            (new RuntimeException()).printStackTrace(System.out);
            return false;
         }

      }

      return true;

   }


   public static boolean containsAllIgnoreEmbedded(List<String> container, List<String> list) {
      for (String s: list) {
         String lookingFor = removeEmbeddedLaterality(s);
         boolean found = false;
         for (String t: container) {
            if (lookingFor.equals(removeEmbeddedLaterality(t))) {
               found = true;
            }
         }
         if (found == false) return false;
      }
      return true;
   }


   public static String toStringSafely(Object o) {
      if (o==null) return "unable to use toString because object is null";
      return o.toString();
   }

   private static void expect(double d, double e) {
      if (d!=e) {
         System.err.println("Expected " + e + " but returned " + d);
         (new RuntimeException()).printStackTrace(System.out);
         return;
      }
      if (PRINT_DEBUG_DETAIL) {
         System.out.println("Expected and actual match: " + e + " == " + d);
      }
   }

   /**
    * compare two files or two directories of files
    * @param gold  a single gold file or a directory of gold files
    * @param candidate a single file to compare to the gold file or a directory of files to compare to the gold files
    * @throws IOException If an I/O error occurs
    */
   private static void process(File gold, File candidate, Map<String, Histogram> histograms) throws IOException {

      System.out.println("---------------------------------| " + new Date() + " |---------------------------------");
      if (!gold.exists()) {
         System.err.println("Did not find a file or directory: " + gold);
         System.exit(-1);
      }
      if (!candidate.exists()) {
         System.err.println("Did not find a file or directory: " + candidate);
         System.exit(-1);
      }

      if (gold.isDirectory() && candidate.isDirectory()) {
         if (isNullOrEmpty(gold.listFiles()) || isNullOrEmpty(candidate.listFiles())) {
            System.err.println("At least one of the directories is empty.");
            System.exit(-1);
         }
      } else { // if just one is a directory, tell user to fix input parameters
         if (gold.isDirectory() || candidate.isDirectory()) {
            System.err.println("Gold and candidate must both be directories or must both be non-directory files.");
            System.exit(-1);
         }
      }
      Map<String, File> goldFilesByType = mapFilesToTypes(gold);
      Map<String, File> candidateFilesByType = mapFilesToTypes(candidate);

      for (String type : goldFilesByType.keySet()) {
         // System.out.println("goldFilesByType type: " + type);
         if (candidateFilesByType.containsKey(type)) {
            System.out.println("\n\n\n--------------------------------------| " + type + " |--------------------------------------");
            PhenotypeEval evaluator = new PhenotypeEval();
            evaluator.evaluate(goldFilesByType.get(type), candidateFilesByType.get(type), type, histograms);
            System.out.println("\n\n\n----------------------------------| End of " + type + " |-----------------------------------");
         }
      }

   }

   public static boolean isNullOrEmpty(Collection c) {
      if (c==null || c.size()==0) return true;
      return false;
   }

   public static boolean isNullOrEmpty(Object [] array) {
      if (array==null || array.length==0) return true;
      return false;
   }

   public static boolean isNullOrAllWhitespace(String s) {
      if (s==null || s.trim().length() == 0) return true;
      return false;
   }

   public static boolean isNullOrZeroLength(String s) {
      if (s==null || s.length() == 0) return true;
      return false;
   }

   /**
    *
    * @param goldThisPatient
    * @param candThisPatient The candidates for this pt that are still eligible to be matched/aligned with the gold annotations in {@code goldThisPatient}
    * @return Pair (goldWithHighestScore, bestMatchingCandidate) -
    * the gold from {@code goldThisPatient} that has the highest score of any gold against any candidate in {@code candThisPatient},
    * paired with itself if no candidate can align, or paired with the element of {@code candThisPatient} that gives the best score with goldWithHighestScore
    */
   private Pair<Record> findPairWithBestScore(List<Record> goldThisPatient, List<Record> candThisPatient) {
      if (goldThisPatient==null || goldThisPatient.size()==0) return null; // should never happen, but avoid coding to handle empty list below by just returning right away
      double bestScore = 0.0;
      Record goldWithHighestScore = goldThisPatient.get(0);
      Record bestMatchingCandidate = null;

      for (Record gold: goldThisPatient) {

         final Collection<Record> filteredCand = getMatchesOnRequiredFields(candThisPatient, gold);

         final List<Record> potentialCandidates;
         if (DISALLOW_OPPOSITE_LATERALITY) {
            potentialCandidates = removeOppositeLateralities(filteredCand, gold);
         } else {
            potentialCandidates = new ArrayList<>(filteredCand);
         }

         if ( !potentialCandidates.isEmpty() ) {
            Record bestCand = findBestScoringMatch(potentialCandidates, gold);
            double score = gold.getWeightedScore(bestCand);
            // prefer gold with score 0 that has some matching candidate over one that has no matching candidate.
            // Should never happen with current scoring, but in case scoring ever changes, ensure.
            if (score >= bestScore && bestCand != null) {
               bestScore = score;
               goldWithHighestScore = gold;
               bestMatchingCandidate = bestCand;
            }
         }
      }

      Pair pair;
      if (bestMatchingCandidate == null) {
         // Pair object doesn't allow 2nd value to be null
         pair = new Pair(goldWithHighestScore, goldWithHighestScore);
      } else {
         pair = new Pair(goldWithHighestScore, bestMatchingCandidate);
      }
      return pair;
   }


   /**
    * Instead of relying, like V1 did, solely on the fields marked with * in the input files to determine how to
    * align/match records in the gold standard with records produced by the system (aka candidate records),
    * use the algorithm within this method to match records, to allow for multiple tumors and to handle body sites better.
    * Note what we consider the best match might not be the one with the best score if laterality=right is not allowed to align with left.
    * or if we require the body location to match before we allow the candidate to be considered.
    * That's what differentiates this method from just using findBestScoringMatch.
    * @param annotations The annotations for this patient within which to look for a "best" match
    * @param searchFor The annotation we want to find a match for
    * @param alreadyUsed The annotations that have already been aligned with some gold annotation
    * @param allowedMultipleMatches The annotations that are allowed to be aligned with more than one gold annotation
    * @return The Record for the annotation that is the best match
    */
   private Record findBestMatch(List<Record> annotations, Record searchFor, Collection<Record> alreadyUsed, Set<Record> allowedMultipleMatches) {
      throw new RuntimeException("Replaced by findPairWithBestScore");
   }

   // Those fields required to match in order for two records to be aligned with each other
   private static List<String> fieldsToMatch = new ArrayList<>();

   /**
    * Search the input annotations for the best match to <code>searchFor</code>
    * @param annotations Intended to be some subset, at worst could be all annotations for the same patient
    * @param searchFor The record (typically from the gold standard) that we want to find a good match for, from within <code>subsetOfAnnotations</code>
    * @return A single best match or null
    */
   private Record findBestScoringMatch(final List<Record> annotations, Record searchFor) {
      Record best = null;
      int bestScore = 0;
      final int allValueMatch = searchFor.getFields().getValueFieldNames().size();
      for (Record r: annotations) {
         Fields fields = r.getFields();
         int countMatchingFields = 0;

         for ( String field : fields.getValueFieldNames() ) {
            String potential = r.getValue( field );
            String compareTo = searchFor.getValue( field );
            if ( canMatchUri( potential, compareTo ) ) {
               countMatchingFields++;
               if ( countMatchingFields == allValueMatch ) {
                  return r;  // stop looping through all records since this is a perfect match
               }
               if ( countMatchingFields > bestScore ) {
                  bestScore = countMatchingFields;
                  best = r;
               }
            } else {
               potential = removeEmbeddedLaterality(potential);
               compareTo = removeEmbeddedLaterality(compareTo);
               if ( canMatchUri( potential, compareTo ) ) {
                  countMatchingFields++;
                  System.out.println("INFO: Can match URI when ignore an embedded laterality: " + r.getValue( field ) + " " + searchFor.getValue( field ));
                  if ( countMatchingFields == allValueMatch ) {
                     return r;  // stop looping through all records since this is a perfect match
                  }
                  if ( countMatchingFields > bestScore ) {
                     bestScore = countMatchingFields;
                     best = r;
                  }
               }
            }
         }

      }

      return best;

   }

   // a cancer with ovary and fallopian tube is a special case
   // So is axilla vs. axillary lymph node
   private boolean shouldAllowMultipleMatches(Record record) {
      String laterality = record.getLaterality().toLowerCase();
      if (laterality.equalsIgnoreCase("bilateral")) {
         System.out.println("Allowing record with laterality " + laterality + " to be left in pool of potentials.");
         return true; // allow to align with one from gold for right and with one for left
      }
      String locations = record.getLocations();
      String lc = locations.toLowerCase();
      if (lc.contains("ovary") && lc.contains("fallopian tube") && lc.contains(Record.INTRA_VALUE_DELIMITER)
              || lc.contains("axilla")) { // for Axillary_Lymph_node and Axilla
         System.out.println("Allowing record with locations " + locations + " to be left in pool of potentials.");
         return true;
      }
      return false;
   }

   // for laterality, if both have a laterality value, can't have one left and one right
   private List<Record> removeOppositeLateralities(Collection<Record> subsetOfAnnotations, Record searchFor) {
      List<Record> potentials = new ArrayList<>( subsetOfAnnotations );
      String lateralityFieldSuffix = Fields.lateralityField;
      if (lateralityFieldSuffix.startsWith(Record.PRIMARY_KEY_INDICATOR)) {
         lateralityFieldSuffix = lateralityFieldSuffix.substring(Record.PRIMARY_KEY_INDICATOR.length());
      }
      for (Record r: subsetOfAnnotations) {
         String lat1 = r.getLaterality(lateralityFieldSuffix);
         String lat2 = searchFor.getLaterality(lateralityFieldSuffix);
         // Don't allow sites with opposite lateralities to be aligned.
         // Leg with laterality Right not allowed to align with Leg with laterality Left,
         // but is allowed to align with Leg that has no laterality or Leg that has laterality Bilateral
         if ("rightleft".equalsIgnoreCase(lat1 + lat2) || "leftright".equalsIgnoreCase(lat1 + lat2)) { // handles if either or both are null
            potentials.remove(r);
         }
      }
      return potentials;
   }

   private Collection<Record> getMatchesOnRequiredFields(List<Record> subsetOfAnnotations, Record searchFor) {
      // First just check the fields that must match for the two records to be considered aligned.
      // Some fields, such as calcifications, might be wrong, but we still want to match up the
      // two records if the more important fields match up, and then report on how many of the
      // fields didn't match up
      Collection<Record> potentials = new HashSet<>( subsetOfAnnotations );
      for (Record r: subsetOfAnnotations) {

         for (String field: fieldsToMatch) {
            String withoutStar = field;
            if (field.startsWith(Record.PRIMARY_KEY_INDICATOR)) {
               withoutStar = field.substring(Record.PRIMARY_KEY_INDICATOR.length());
            }
            if (r.getValue(field)==null && r.getValue(withoutStar)==null) {
               System.out.println("WARN: Null found for " + r.getPatientName() + " for required field " + field);
               potentials.remove(r);
               continue;
            }
            String potentialValue = r.getValue( field );
            String searchForValue = searchFor.getValue( field );
            boolean valueMatch = canMatchUri( potentialValue, searchForValue );
            if ( !valueMatch ) {
               valueMatch = canMatchUri( removeEmbeddedLaterality(potentialValue), removeEmbeddedLaterality(searchForValue) );
            }
            if ( !valueMatch ) {
               String potentialWithoutStar = r.getValue( withoutStar );
               String searchForWithoutStar = searchFor.getValue( withoutStar );
               if ( field.startsWith( Record.PRIMARY_KEY_INDICATOR ) ) {
                  // they match when ignore the * such as for "body location" for cancer (whereas it's "*body location" for tumor)
                  valueMatch = canMatchUri(potentialWithoutStar, searchForWithoutStar);
                  if ( !valueMatch ) {
                     valueMatch = canMatchUri( removeEmbeddedLaterality(potentialWithoutStar), removeEmbeddedLaterality(searchForWithoutStar) );
                  }
               }
            }
            if ( !valueMatch ) {
               potentials.remove( r );
            }
         }
      }

      return potentials;
   }

   static private String removeEmbeddedLaterality(String s) {
      String t = "_Right_";
      if (s!=null && s.contains(t)) {
         s = s.replace(t, "_");
      }
      t = "_Left_";
      if (s!=null && s.contains(t)) {
         s = s.replace(t, "_");
      }
      return s;
   }

   static private double compareByOntology( final Collection<String> uris1, final Collection<String> uris2 ) {
      if ( uris1.isEmpty() && uris2.isEmpty() ) {
         return 1.0;
      }
      if ( uris1.isEmpty() || uris2.isEmpty() ) {
         return 0.0;
      }
      final Collection<String> collate1 = ByUriRelationFinder.collateUris( uris1 ).keySet();
      final Collection<String> collate2 = ByUriRelationFinder.collateUris( uris2 ).keySet();

      final Collection<String> allUris = new ArrayList<>( collate1 );
      allUris.addAll( collate2 );
      final Collection<String> allCollate = ByUriRelationFinder.collateUris( allUris ).keySet();

      return scoreCollate( collate1.size(), collate2.size(), allCollate.size() );
   }

   static private double scoreCollate( final int count1, final int count2, final int collateCount ) {
      final double matchedCount = count1 + count2 - collateCount;
      final double matchScore = matchedCount / Math.max( count1, count2 );
      return Math.min( 1.0, matchScore );
   }

   static private boolean canMatchAnyUri( final String maybeUri1, final Collection<String> maybeUris ) {
      return maybeUris.stream().anyMatch( u -> canMatchUri( maybeUri1, u ) );
   }

   static private boolean canMatchUri( final String maybeUri1, final String maybeUri2 ) {
      if ( maybeUri1 == null || maybeUri2 == null ) {
         return false;
      }
      if ( maybeUri1.contains( ";" ) ) {
         // Right now splitting is iffy.  Return a true if any split matches any split.
         return Arrays.stream( maybeUri1.split( ";" ) ).map( String::trim )
                      .anyMatch( u1 -> canMatchUri( maybeUri2, u1 ) );
      }
      if ( maybeUri1.equalsIgnoreCase( maybeUri2 ) ) {
         return true;
      }
      if ( maybeUri1.isEmpty() || maybeUri2.isEmpty() ) {
         return false;
      }
       if (hardcodedSynonyms(maybeUri1, maybeUri2)) {
           System.out.println("Allowing " + maybeUri1 + " to match " + maybeUri2 + " without checking ontology.");
           return true;
       }

      final GraphDatabaseService graphDb = Neo4jConnectionFactory.getInstance().getGraph();
      final Collection<String> maybeBranch1 = SearchUtil.getBranchUris( graphDb, maybeUri1 );
      if ( maybeBranch1.isEmpty() ) {
         return false;
      }
      final Collection<String> maybeBranch2 = SearchUtil.getBranchUris( graphDb, maybeUri2 );
      if ( maybeBranch2.isEmpty() ) {
         return false;
      }
      if ( maybeBranch1.contains( maybeUri2 ) || maybeBranch2.contains( maybeUri1 ) ) {
         System.out.println( "Uri Match for : " + maybeUri1 + " , " + maybeUri2 + " by ancestry" );
         return true;
      }
      final Collection<String> maybeRoots1 = Neo4jOntologyConceptUtil.getRootUris( maybeUri1 );
      if ( maybeRoots1.contains( maybeUri2+"_Part" ) ) {
         System.out.println( "Uri Match for : " + maybeUri1 + " , " + maybeUri2 + " by _Part" );
         return true;
      }
      final Collection<String> maybeRoots2 = Neo4jOntologyConceptUtil.getRootUris( maybeUri2 );
      if ( maybeRoots2.contains( maybeUri1+"_Part" ) ) {
         System.out.println( "Uri Match for : " + maybeUri1 + " , " + maybeUri2 + " by _Part" );
         return true;
      }
      if ( maybeRoots1.contains( "Body_Part" ) && maybeUri1.contains( maybeUri2 ) ) {
         System.out.println( "Uri Match for : " + maybeUri1 + " , " + maybeUri2 + " by Body_Part" );
         return true;
      }
      if ( maybeRoots2.contains( "Body_Part" ) && maybeUri2.contains( maybeUri1 ) ) {
         System.out.println( "Uri Match for : " + maybeUri1 + " , " + maybeUri2 + " by Body_Part" );
         return true;
      }
      return false;
   }

    private static Map<String, String> HARCODED_SYNONYMS = new HashMap<>();
    static {
       addSynonym("Axilla", "Axillary_Lymph_Node"); // discussed 9/5/2018
       addSynonym("Thigh", "Femur");
       addSynonym("Gastric_Tissue", "Stomach"); // from email 9/13/2018
    }

    private static void addSynonym(String key , String synonym) {
        if (HARCODED_SYNONYMS.put(key, synonym)!=null) throw new RuntimeException("Already used this key " + key);

    }

    private static boolean hardcodedSynonyms(String uri1, String uri2) {
        if (uri2.equals(HARCODED_SYNONYMS.get(uri1))) {
           return true;
        } else if (uri1.equals(HARCODED_SYNONYMS.get(uri2))) {
            return true;
        }
        return false;
    }


    private static String removePrependedLaterality(String s) {
      if (s==null) return s;
      String prefix;
      prefix = "Right_";
      if (s.startsWith(prefix) || s.startsWith(prefix.toLowerCase())) {
         System.out.println("Removing prepended laterality from location " + s);
         return s.substring(prefix.length());
      }
      prefix = "Left_";
      if (s.startsWith(prefix) || s.startsWith(prefix.toLowerCase())) {
         System.out.println("Removing prepended laterality from location " + s);
         return s.substring(prefix.length());
      }
      prefix = "Bilateral_";
      if (s.startsWith(prefix) || s.startsWith(prefix.toLowerCase())) {
         System.out.println("Removing prepended laterality from location " + s);
         return s.substring(prefix.length());
      }
      return s;
   }

   /**
    * Determine the type(s) of file(s) based on what's within the file name: Cancer|Tumor|Episode|Patient
    * @param file A single file or a directory containing files to process
    */
   private static Map<String, File> mapFilesToTypes(File file) {

      Map<String, File> map = new HashMap<>();
      Pattern pt = Pattern.compile(TYPES_PATTERN);
      File[] files;

      if (file.isDirectory()) {
         files = file.listFiles();
         if (isNullOrEmpty(files)) {
            System.err.println("No files found within " + file);
            return map;
         }
      } else {
         files = new File[1];
         files[0] = file;
      }
      for (File f : files) {
         Matcher mt = pt.matcher(f.getName());
         if (mt.find()) {
            map.put(mt.group(), f);
         }
      }
      if (map.keySet().size()==0) {
         System.err.println("No files with names that match an expected pattern were found within " + file);
         System.err.println("Pattern is defined by TYPES_PATTERN: \"" + TYPES_PATTERN + "\"" );
         return map;
      }
      return map;
   }

   public static String quoted(String s) {
      return '"' + s + '"';
   }

   public enum ConfusionLabel {
      TP, FP, FN, TN
   }
   private enum DETAIL {
      NORMAL, VERBOSE
   }

   public static class ConfusionMatrix {
      public double TPP, TP, FP, FN, TN;

      public void includeCountsFrom(ConfusionMatrix cm) {
         TPP += cm.TPP;
         TP += cm.TP;
         FP += cm.FP;
         FN += cm.FN;
         TN += cm.TN;
      }

      public double getPrecision() {
         if ( TP + FP + FN == 0 && TN > 0 ) {
            return 1.0;
         }
         return TP / (TP + FP);
      }

      public double getRecall() {
         if ( TP + FP + FN == 0 && TN > 0 ) {
            return 1.0;
         }
         return TP / (TP + FN);
      }

      public double getFscore() {
         double p = getPrecision();
         double r = getRecall();
         return (2 * p * r) / (p + r);
      }

      public double getAccuracy() {
         return (TP + TN) / (TP + TN + FP + FN);
      }

      public static void printHeader(PrintStream out) {
         out.println(String.format("%1$-" + Record.attrColumnLen + "s", "Label") + "\tTP    \tTP'\t\tFP\tFN\tTN\tPrecis\tRecall\tAccur\tF1-Score");
      }

      public void print(PrintStream out, String label) {
         out.println( String.format("%1$-" + Record.attrColumnLen + "s", label) + "\t" +
                      String.format( "%.4f", TP ) + "\t" + String.format( "%.0f", TPP ) + "\t\t" +
                      String.format( "%.0f", FP ) + "\t" +
                      String.format("%.0f", FN) + "\t" + String.format("%.0f", TN) + "\t" +
                      format(getPrecision()) + "\t" +
                      format(getRecall()) + "\t" +
                      format(getAccuracy()) + "\t" +
                      format(getFscore()));
      }

      public String format(double d) {
         if (Double.isNaN(d)) return "   -  ";
         return String.format("%.4f", d);
      }
   }

   private static class Fields {

      public static final String patientIdField = Record.PRIMARY_KEY_INDICATOR + lowerCaseIfRequested("patient ID");
      public static final String bodyLocationField = Record.PRIMARY_KEY_INDICATOR + lowerCaseIfRequested("body location");
      public static final String lateralityField = Record.PRIMARY_KEY_INDICATOR +
                                                   lowerCaseIfRequested( "body location laterality" );
      public static final String cancerIdField = lowerCaseIfRequested("cancer ID");
      public static final String cancerLinkField = lowerCaseIfRequested("cancer link");
      public static final String diagnosisField = lowerCaseIfRequested("Diagnosis");

      private List<String> allFields = new ArrayList<>();
      private List<String> valueFields;
      private List<String> provenanceFields;

      private static String lowerCaseIfRequested(String s) {
         if (READ_IN_AS_LOWER_CASE) {
            return s.toLowerCase();
         }
         return s;
      }

      public static boolean isIdField(String name) {
         return name.startsWith(Record.PRIMARY_KEY_INDICATOR);
      }

      public static boolean isIgnoreField(String name) {
         return name.startsWith(Record.IGNORE_FIELD_INDICATOR);
      }

      public static boolean isProvenanceField(String name) {
         return name.startsWith(Record.PROVENANCE_FIELD_INDICATOR);
      }

      public static Fields loadFieldNames(String delimitedNames) {
         Fields fields = new Fields();
         for (String field : delimitedNames.split(Record.FIELD_SEPARATOR)) {
            field = field.trim();
            field = field.replace("  ", " "); // in case any with 2 spaces by accident.  "cancer link" vs. "cancer  link"
            fields.allFields.add(field.trim());
         }
         return fields;
      }

      private List<String> getAllFieldNames() {
         return allFields;
      }

      public List<String> getValueFieldNames() {  // aka attributes
         if (valueFields == null) {
            valueFields = new ArrayList<>();
            for (String field : allFields) {
               if (!isIgnoreField(field)) {
                  valueFields.add(field);
               }
            }
         }
         return valueFields;
      }

      public List<String> getProvenanceFieldNames() {
         if (provenanceFields == null) {
            provenanceFields = new ArrayList<>();
            for (String field : allFields) {
               if (isProvenanceField(field)) {
                  provenanceFields.add(field);
               }
            }
         }
         return provenanceFields;
      }

      public static String getNameWithoutIndicator(String fieldName) {
         final String name;
         if (fieldName.startsWith(Record.PRIMARY_KEY_INDICATOR)) name = fieldName.substring(Record.PRIMARY_KEY_INDICATOR.length()).toLowerCase();
         else if (fieldName.startsWith(Record.IGNORE_FIELD_INDICATOR)) name = fieldName.substring(Record.IGNORE_FIELD_INDICATOR.length()).toLowerCase();
         else if (fieldName.startsWith(Record.PROVENANCE_FIELD_INDICATOR)) name = fieldName.substring(Record.PROVENANCE_FIELD_INDICATOR.length()).toLowerCase();
         else {
            name = fieldName;
         }

         return name;

      }

      public static boolean isTnmField(String name) {
         return (name.toLowerCase().contains("t classification") || name.toLowerCase().contains("n classification") || name.toLowerCase().contains("m classification"));
      }
      public static boolean isSizeField(String name) {
         return (name.toLowerCase().contains("size"));
      }
   }


    /**
    * record data
    * One line turns into one record
    *
    * @author tseytlin
    */

   private static class Record implements Comparable<Record> {

      public static final String FIELD_SEPARATOR = "\\|";

      public static final String PRIMARY_KEY_INDICATOR = "*";
      public static final String IGNORE_FIELD_INDICATOR = "-";
      public static final String PROVENANCE_FIELD_INDICATOR = "-doc";
      public static final String TUMOR_TYPE_PRIMARYTUMOR = "PrimaryTumor";
      public static int attrColumnLen = 1; // for formatting the output nicely, need length of longest attribute

      private static final String INTRA_VALUE_DELIMITER = ";";

      private Map<String, String> content;
      private String patientName;
      private String id;
      private ConfusionLabel confusionLabel;
      private Record matchingCandidateRecord; // Used to get the candidate record that is paired with this gold record, if there is one
      private String setCameFrom;
      private String typeOfAnnotation;

      private Fields fields;

      public String toString() { // TODO should be a separate method that tacks on the matchingCandidateRecord
         String s;

         s = id + "|" + content + "|";
         if (matchingCandidateRecord==null) s = s + matchingCandidateRecord;
         else s = s + matchingCandidateRecord.toString();
         return s;

      }

      private String getTumorType() {
         for (String field: this.fields.getAllFieldNames()) {
            if (field.toLowerCase().endsWith("tumor type") && this.getValue(field)!=null) {
               return this.getValue(field);
            }
         }
         return "";
      }

      // If there are multiple locations for this record, returns a single string with delimited values
      private String getLocations() {
         String indicator =  Record.PRIMARY_KEY_INDICATOR;
         String locationField = Fields.bodyLocationField;
         if (locationField.startsWith(indicator)) locationField = locationField.substring(indicator.length());

         for (String field: this.fields.getAllFieldNames()) {
            if (field.endsWith(locationField) && this.getValue(field)!=null) {
               return this.getValue(field);
            }
         }
         return "";
      }
      private boolean locationIsJustSkin() {
         return toStringSafely(getLocations()).toLowerCase().equals("skin");
      }
      private void fixLocationsWithLaterality() {

         String indicator = Record.PRIMARY_KEY_INDICATOR;
         String locationField = Fields.bodyLocationField;
         if (locationField.startsWith(indicator)) locationField = locationField.substring(indicator.length());

         String lateralityFieldSuffix = Fields.lateralityField;
         if (lateralityFieldSuffix.startsWith(indicator)) lateralityFieldSuffix = lateralityFieldSuffix.substring(indicator.length());
         fixLocation(this, locationField, lateralityFieldSuffix);

      }

      private String latFieldWithoutIndicator = null;
      private String getLaterality() {
         if (latFieldWithoutIndicator == null) {
            latFieldWithoutIndicator = Fields.lateralityField;
            String indicator = Record.PRIMARY_KEY_INDICATOR;
            if (latFieldWithoutIndicator.startsWith(indicator)) latFieldWithoutIndicator = latFieldWithoutIndicator.substring(indicator.length());
         }
         return getLaterality(latFieldWithoutIndicator);
      }

      private String getLaterality(String lateralityFieldSuffix) {
         for (String field: fields.getAllFieldNames()) {
            if (field.endsWith(lateralityFieldSuffix) && getValue(field)!=null && getValue(field).length()>0) {
               return getValue(field);
            }
         }
         return "";
      }

      private static void fixLocation(Record r, String locField, String lateralityFieldSuffix) {
         String lateralityValue = r.getLaterality(lateralityFieldSuffix);
         if (lateralityValue!=null && lateralityValue.length()>0) {
            for (String field: r.fields.getAllFieldNames()) {
               if (field.endsWith(locField) && r.getValue(field)!=null) {
                  r.addField(field, removePrependedLaterality(r.getValue(field)));
               }
            }
         }
      }

      public static Record loadRecord(Fields fields, String line, DataSet whichSet, String typeOfAnnotation) {
         if (fields == null) throw new RuntimeException("fields == null");
         Record r = new Record();
         r.setFields(fields);
         r.setSetCameFrom(whichSet);
         r.setTypeOfAnnotation(typeOfAnnotation);
         int i = 0;
         for (String value : line.split(FIELD_SEPARATOR)) {

            String field;
            if (i >=  fields.getAllFieldNames().size()) continue; // skip if have additional gold fields like treatment which we aren't creating yet
            field = fields.getAllFieldNames().get(i++);

            String trimmed = value.trim();

            if (field.equalsIgnoreCase(Fields.patientIdField) || (Record.PRIMARY_KEY_INDICATOR+field).equalsIgnoreCase(Fields.patientIdField)) {

               r.setPatientName(trimmed);
               r.addField(field, r.getPatientName());

            } else {

               if (IGNORE_TNM_WITH_X && Fields.isTnmField(field)) {
                  trimmed = removeTNMsWithX(r, field, trimmed);
               }

               if (IGNORE_TNM_PREFIX && Fields.isTnmField(field)) {
                  // remove c or p prefix, e.g. for pT2 use T2, for cN3 use N3
                  trimmed = removeTnmPrefix(r, field, trimmed);
               }

               r.addField(field, trimmed);
            }
            if (field.length() > attrColumnLen) {
               attrColumnLen = field.length();
            }
         }
         printPatientLocations(r);
         return r;
      }

      private static String removeTnmPrefix(Record r, String fieldName, String value) {
         String withoutPrefix = "";
         for (String val : r.convertDelimitedValueToSortedList(value)) {
            String lc = val.toLowerCase();
            if (lc.startsWith("pt") || lc.startsWith("pm") || lc.startsWith("pn") || lc.startsWith("ct") || lc.startsWith("cm") || lc.startsWith("cn")) {
               System.out.println("INFO: removing TNM prefix " + val.charAt(0) + " from " + val + " for field " + fieldName + " from " + r.getSetCameFrom());
               val = val.substring(1);
            }
            if (withoutPrefix.length() > 0) withoutPrefix = withoutPrefix + INTRA_VALUE_DELIMITER + " ";
            withoutPrefix = withoutPrefix + val;
         }
         return withoutPrefix;
      }

      private static String removeTNMsWithX(Record r, String fieldName, String value) {
         String withoutXs = "";
         for (String val : r.convertDelimitedValueToSortedList(value)) {
            String lc = val.toLowerCase();
            if (lc.contains("tx") || lc.contains("mx") || lc.contains("nx")) {
               System.out.println("INFO: ignoring TNM value " + val + " for field " + fieldName + " from " + r.getSetCameFrom());
            } else {
               if (withoutXs.length() > 0) withoutXs = withoutXs + INTRA_VALUE_DELIMITER + " ";
               withoutXs = withoutXs + val;
            }
         }
         return withoutXs;
      }

      private void setSetCameFrom(DataSet set) {
         setCameFrom = set.toString().toLowerCase();
      }
      private String getSetCameFrom() {
         return setCameFrom;
      }

      private void setTypeOfAnnotation(String type) {
         typeOfAnnotation = type;
      }
      private String getTypeOfAnnotation() {
         return typeOfAnnotation;
      }

      private void addField(String name, String value) {
         if (content == null) {
            content = new HashMap<>();
         }
         content.put(name, value);

      }

      private void setFields(Fields fields) {
         this.fields = fields;
      }

      public Fields getFields() {
         return fields;
      }

      public String getDiagnosis() {
         String dx = content.get(Fields.diagnosisField);
         return  (dx==null) ? "" : dx;
      }

      private void setPatientName(String name) {
         if (name.startsWith("patient") || name.startsWith("Patient")) {
            // the most tested code path...
            LOGGER.fine("Setting patient name: '" + name + "'");
            patientName = name;
         } else {
            // if patient ID doesn't start with "patient", prepend 'patient' a mismatch between gold and system-output gets ignored.
            if (PRINT_DEBUG_DETAIL) System.out.println("patient name does not start with 'patient' or 'Patient': '" + name + "' - prepending 'patient'");
            patientName = "patient" + name;
         }
      }

      private String getPatientName() {
         return patientName;
      }

      /**
       * In V1, this id was used to match with record from other set (gold set vs. candidate set).
       * The id is determined by <code>isIdField</code>, which uses any fields that start with the '*' character.
       * Appending the line number to increase chance Id is unique, if necessary, appends a counter to guarantee unique
       * @return
       */
      public String getId() {
         if (id == null) {
            id = createId(-1);
         }
         return id;
      }

      /**
       *
       * @param lineNumber The line the data came from, if read in from a file.
       */
      private void setId(int lineNumber) {
         if (lineNumber<1) {
            throw new RuntimeException("Line numbers should be positive");
         }
         if (id != null) {
            throw new RuntimeException("Should not be changing the Id once set");
         }
         id = createId(lineNumber);
      }

      private static Map<String, List<String>> allIdsByPatient = new HashMap<>();

      public String createId(int lineNumber) {
         StringBuffer b = new StringBuffer();
         for (String field : fields.getAllFieldNames()) {
            if (Fields.isIdField(field)) {
               for (String val : convertDelimitedToSortedList(field)) {
                  b.append(val).append(" ");
               }
            }
         }
         b.append(" ").append(getSetCameFrom()).append(" "); // purposely put extra space before, for more separation from list of sites - extra space makes easier to read if site list is long
         b.append(getTypeOfAnnotation());
         String potentialId = b.toString().trim();
         if (lineNumber > 0) {
            potentialId = potentialId + " from line # " + lineNumber;
         }
         // Append a counter only if the id without any counter is already is use by another record.
         // Appending the counter is our failsafe so sort will work even if two records have the same IdField values, such as if only differ by laterality
         List IDs = allIdsByPatient.get(getPatientName());
         if (IDs==null) {
            IDs = new ArrayList<String>();
            allIdsByPatient.put(getPatientName(), IDs);
         }
         if (IDs.contains(potentialId)) {
            if (lineNumber > 0) {
               potentialId = potentialId + " from line # " + lineNumber;
            } else {
               potentialId = potentialId + " " + idCounter;
               idCounter++;
            }
         }
         IDs.add(potentialId);
         return potentialId;
      }

      public boolean isValid() {
         for ( String field : fields.getAllFieldNames() ) {
            if ( Fields.isIdField( field ) || field.endsWith(Fields.cancerIdField) || field.endsWith(Fields.cancerLinkField)) {
               for ( String val : convertDelimitedToSortedList( field ) ) {
                  if ( val.endsWith( EMPTY_CANCER_INDICATOR )
                       || val.endsWith( EMPTY_CANCER_INDICATOR + "Current" )
                       || val.endsWith( EMPTY_CANCER_INDICATOR + "Historical" ) ) {
                     return false;
                  }
               }
            }
         }
         return true;
      }

      public ConfusionLabel getConfusion() {
         return confusionLabel;
      }

      public void setConfusion(ConfusionLabel confusionLabel) {
         this.confusionLabel = confusionLabel;
      }


      /**
       * Set the candidate record that is paired with this gold record, if there is one
       * @param record -
       */
      public void setMatchingCandidateRecord(Record record) {
         this.matchingCandidateRecord = record;
      }

      /**
       * Print to a stream the values for the fields for this record, and if this record is aligned with another record,
       * also print the values for the other record, as well as the scores for the alignment
       * that record
       * @param out the stream to print to
       * @param level what level of DETAIL to output
       */
      public void print(PrintStream out, DETAIL level) {

         out.println(getConfusion() + ": " + getId());
         if (level.equals(DETAIL.NORMAL)) {
            return;
         }

         for (String field : fields.getValueFieldNames()) {
            List<String> gold = convertDelimitedToSortedList(field);
            List<String> pred = Collections.EMPTY_LIST;
            if (matchingCandidateRecord != null) {
              pred = matchingCandidateRecord.convertDelimitedToSortedList(field);
            }
            String scoreColumn = "\tscore: " + String.format("%.4f", compare(gold, pred));
            String goldColumn = "\tgold: " + PhenotypeEval.toSimpleString(gold);
            String predColumn = "\tpred: " + PhenotypeEval.toSimpleString(pred);
            if (getConfusion().equals(ConfusionLabel.FP)) {
               scoreColumn = "";
               predColumn = "\tpred: " + getValue(field);
               goldColumn = "\t";
            } else if (getConfusion().equals(ConfusionLabel.FN)) {
               scoreColumn = "";
               predColumn =  "";
            }

            out.println("\t" + String.format("%1$-" + Record.attrColumnLen + "s", field) + scoreColumn + goldColumn +  predColumn);
         }

         if (getConfusion().equals(ConfusionLabel.TP)) {
            out.println("\n\t" + String.format("%1$-" + Record.attrColumnLen + "s", "Weighted Score:") + "\t" + String.format("%.4f", getWeightedScore()) + "\n");
         } else {
            out.println();
         }

         boolean firstProvenanceField = true;
         for (String field : fields.getProvenanceFieldNames()) {
            if (firstProvenanceField) {
               firstProvenanceField = false;
               out.println("\t-----------");
            }
            String gold = getValue(field);
            String pred = "";
            switch (getConfusion()) {
               case TP:
                  pred = matchingCandidateRecord.getValue(field);
                  break;
               case FP:
                  gold = "";
                  pred = getValue(field);
                  break;
               default:
                  break;
            }
            if (gold != null && gold.length() > 0) out.println("\t" + String.format("%1$-" + Record.attrColumnLen + "s", field) + "\t gold: " + gold);
            if (pred != null && pred.length() > 0) out.println("\t" + String.format("%1$-" + Record.attrColumnLen + "s", field) + "\t pred: " + pred);

         }

      }

      public double getWeightedScore() {
         return getWeightedScore(matchingCandidateRecord);
      }

      // Intended to be used before a matching candidate is decided upon, to calculate the weighted score that
      // would result from matching the given candidate with this (gold) Record.
      public double getWeightedScore(Record candidate) {
         if (candidate != null) {
            double score = 0;
            int total = fields.getValueFieldNames().size();
            for (String field : fields.getValueFieldNames()) {
               if (field.endsWith(Fields.cancerIdField) || field.endsWith(Fields.cancerLinkField)) {
                  total = total - 1;
               } else {
                  score += compare(convertDelimitedToSortedList(field), candidate.convertDelimitedToSortedList(field));
               }
      }
            return score / total;
         }
         return 0.0;
      }


      public Map<String, ConfusionMatrix> getAttributeConfusionMatrices() {
         Map<String, ConfusionMatrix> map = new LinkedHashMap<>();
         if (matchingCandidateRecord != null) {
            for (String attribute : fields.getValueFieldNames()) {
               ConfusionMatrix cm = new ConfusionMatrix();
               List<String> val1 = convertDelimitedToSortedList(attribute);
               List<String> val2 = matchingCandidateRecord.convertDelimitedToSortedList(attribute);
               // if both value sets are empty, we have a TN
               if ( val1.isEmpty() && val2.isEmpty() ) {
                  cm.TN++;
               } else {
                  for ( String v : val1 ) {
                     if ( canMatchAnyUri( v, val2 ) ) {
                        cm.TP++;
                        cm.TPP++;
                     } else {
                        cm.FN++;
                     }
                  }
                  for ( String v : val2 ) {
                     if ( !canMatchAnyUri( v, val1 ) ) {
                        cm.FP++;
                     }
                  }
               }
               map.put(attribute, cm);
            }
         }
         return map;
      }


      private List<String> lowercase(Iterable<String> items) {
         ArrayList<String> list = new ArrayList<>();
         for (String item: items) {
            list.add(item.toLowerCase());
         }
         return list;
      }

      /**
       * Compare the elements of the lists, used in our case for a single field, in contract to
       * getWeightedScore, which scores the alignment of all fields for a pair of records
       * @param list1 strings to compare to other list
       * @param list2 strings to compare to other list
       * @return score, calculated as follows:
       * For STRICT_VALUE_CALCULATION, list1 and list2 must have all of their elements in common with each
       * other (but not necessarily in the same order) and must be of the same size to return 1.
       * If there are any elements that are not in both lists,
       * If STRICT_VALUE_CALCULATION is not in force, the result is between 0 and 1 inclusive, and depends on how
       * many elements are in common (the fraction of the total that are in common).
       * There is an exception for Fallopian Tube(s) and Ovary.
       * If both appear in a list, they are collapsed so that:
       * [Fallopian Tube, Ovary] is not penalized when comparing to just Ovary
       * [Fallopian Tube, Ovary] is not penalized when comparing to just Fallopian Tube
       * Fallopian Tube is considered a match for Ovary
       */
      private double compare(List<String> list1, List<String> list2) {

         if (list1==null || list2==null) throw new RuntimeException("This method expects non-null input.");

         // if all values are empty, then we have a match :)
         if (list1.size() + list2.size() == 0) return 1;

         List<String> list1Collapsed = new ArrayList<String>(list1);
         List<String> list2Collapsed = new ArrayList<String>(list2);

         List<String> lower1Collapsed = lowercase(list1Collapsed);
         List<String> lower2Collapsed = lowercase(list2Collapsed);

         // change ovary to fallopian tube to allow them to match up for scoring purposes
         // remove ovary if both ovary and fallopian tube appear in same list so don't count twice
         if (lower1Collapsed.contains("ovary") && lower1Collapsed.contains("fallopian tube")) {
            list1Collapsed.remove("Ovary");
            list1Collapsed.remove("ovary");
            lower1Collapsed.remove("ovary");
         }
         if (lower2Collapsed.contains("ovary") && lower2Collapsed.contains("fallopian tube")) {
            list2Collapsed.remove("Ovary");
            list2Collapsed.remove("ovary");
            lower2Collapsed.remove("ovary");
         }

         if (lower1Collapsed.contains("ovary") && !lower1Collapsed.contains("fallopian tube")) {
            list1Collapsed.remove("Ovary");
            list1Collapsed.remove("ovary");
            lower1Collapsed.remove("ovary");
            list1Collapsed.add("Fallopian Tube");
            lower1Collapsed.add("fallopian tube");
         }
         if (lower2Collapsed.contains("ovary") && !lower2Collapsed.contains("fallopian tube")) {
            list2Collapsed.remove("Ovary");
            list2Collapsed.remove("ovary");
            lower2Collapsed.remove("ovary");
            list2Collapsed.add("Fallopian Tube");
            lower2Collapsed.add("fallopian tube");
         }


         // if strict calculation, no partial scoring
         if (STRICT_VALUE_CALCULATION) {
            if (list1Collapsed.size() != list2Collapsed.size()) return 0;
            return (lower1Collapsed.containsAll(lower2Collapsed) ? 1 : 0);
            // TODO there are weird cases where this might not be what's expected,
            // list1 = a, a, b
            // list2 = a, b, b
            // This should return 0
            // Should not matter for this eval script but be aware.
         }

         boolean useOntologyCompare = false;
         // If the only values are Positive and Negative or a variation on those, then don't use compareByOntology method,
         // which doesn't handle lists with repeated values
         // such as "Negative", "Negative", "Positive" when compared to system output of just one value such as "Negative"
         List<String> positiveAndNegative = Arrays.asList("Positive", "positive", "Negative", "negative");
         int common = 0;
         for ( String v : lower1Collapsed ) {
            if (!positiveAndNegative.contains(v)) {
               useOntologyCompare = true;
            }
            if ( lower2Collapsed.contains( v ) ) {
               lower2Collapsed.remove(v);
               common++;
            }
         }
         for (String v: lower2Collapsed) {
            if (!positiveAndNegative.contains(v)) {
               useOntologyCompare = true;
            }
         }

         int t = list1Collapsed.size() + list2Collapsed.size() - common;

         // Make bilateral that is aligned with left or right score 0.5, not 0.0
         // Ignore if there are multiple values, e.g. let "Bilateral" vs "Right, Left" score zero
         // since "Right, Left" should never happen
         List<String> rightAndLeft = Arrays.asList("right", "left");
         if (lower1Collapsed.size()==1 && lower2Collapsed.size()==1) {
            if (lower1Collapsed.get(0).equals("bilateral") && rightAndLeft.contains(lower2Collapsed.get(0))) {
               common = 1; // t will be 2, since common had been 0 when t was calculated
            } else if (lower2Collapsed.get(0).equals("bilateral") && rightAndLeft.contains(lower1Collapsed.get(0))) {
               common = 1;
            }
         }
         final double byStrict = (double) common / (double)t;
         double byOntology = byStrict;
         if (useOntologyCompare) {
             byOntology = compareByOntology(list1, list2);
         }

         double result = Math.max( byStrict, byOntology );
         if (result > 1.0 || result < 0.0) {
            (new RuntimeException("Unexpected score: " + result)).printStackTrace();
         }
         return result;
      }

      /**
       * Use sorted list to make it easy to compare.
       * @param field
       * @return List of the strings that were separated by INTRA_VALUE_DELIMITER
       */
      public List<String> convertDelimitedToSortedList(String field) {
         if (content == null) {
            System.out.println("No content for " + field);
            return Collections.emptyList();
         }
         String str = content.get(field);
         if ( Fields.patientIdField.equalsIgnoreCase( field ) && str != null ) {
            str = str.toLowerCase();
         }
         return convertDelimitedValueToSortedList(str);
      }

      public List<String> convertDelimitedValueToSortedList(String value) {
         List<String> valueAsList = new ArrayList<>();
         if (value != null && value.trim().length() > 0) {
            if (value.contains(INTRA_VALUE_DELIMITER)) {
               for (String s : value.split(INTRA_VALUE_DELIMITER)) {
                  valueAsList.add(s.trim());
               }
               Collections.sort(valueAsList);
            } else {
               valueAsList.add(value);
            }
         }
         return valueAsList;

      }

      public String getValue(String field) {
         return content.get(field);
      }

      public boolean isDuplicateRecordIfIgnoreSizes(Record r) {
         boolean onlySizesDiffer = true;
         Fields fields = getFields();
         if (fields.getValueFieldNames().size() != r.getFields().getValueFieldNames().size()) {
            throw new RuntimeException("Different number of fields: " + fields.allFields.size() + "vs." + r.getFields().allFields.size() + " for \n" + r + "\n" + this);
         }
         for (String fieldName: r.fields.getValueFieldNames()) {
            if (Fields.isSizeField(fieldName) || Fields.isProvenanceField(fieldName) || Fields.isIgnoreField(fieldName)) {
               // ignore, not required to match
            } else {
               String value = this.getValue(fieldName);
               String rValue = r.getValue(fieldName);
               if (value==null) value="";
               if (rValue==null) rValue="";
               if (!value.trim().equals(rValue.trim())) {
                  onlySizesDiffer = false;
               }
            }
         }
         return onlySizesDiffer;
      }

      // Finds first match whose fields other than ID and size fields.
      public Record findMatchIfIgnoreSizeFields(ArrayList<Record> annotations) {
         for (Record r: annotations) {
            if (isDuplicateRecordIfIgnoreSizes(r)) {
               return r;
            }
         }
         return null;
      }


      public int compareTo(Record r) {
         return getId().compareTo(r.getId());
      }

   }

    /**
    * Read the annotations from a File and load them into a map indexed by patient name.
    * The file is assumed to have one header line containing the names of the fields, with
    * each subsequent line containing a (possibly empty) value for each field.
    * Fields are delimited by the
    * One line of the file turns into one <code>Record</code>.
    * @param file The file to load annotations from
    * @param whichSet is the file for gold or for candidate (system) annotations
    * @param type Type of annotations within the files - Cancer or Tumor
    * @return a List of Records, one record per annotation line in the file
    * @see #READ_IN_AS_LOWER_CASE
    * @throws IOException If an I/O error occurs
    */
   private List<Record> loadAnnotations(File file, DataSet whichSet, String type) throws IOException {

      ArrayList<Record> annotations = new ArrayList<>();
      List<String> previousLines = new ArrayList<>();
      BufferedReader reader = new BufferedReader(new FileReader(file));
      boolean isHeaderLine = true;
      Fields fields = null;
      int lineNumber = 0; // keep track of which line of the input file the data came from, used to disambiguate 2 identical lines
      readLines:
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
         lineNumber++;
         if (line.trim().length() == 0) {
            continue readLines;
         } else if (line.startsWith("#")) {
            LOGGER.fine("Skipping input line # " + lineNumber + ": '" + line + "'"); // just in case there is some valuable line that starts with #, output this to aid debug
            continue readLines;
         }

         line = Fields.lowerCaseIfRequested(line);


         if (isHeaderLine) { // first non-all-whitespace and non-comment line is assumed to be the header line that contains the field names
            fields = Fields.loadFieldNames( line );
            isHeaderLine = false;

         } else {

            if (previousLines.contains(line)) {
               System.out.println("WARN: Ignoring duplicate annotation on input line # " + lineNumber + ": '"  + line + "'");
               continue readLines;
            }
            previousLines.add(line);

            Record record = Record.loadRecord(fields, line, whichSet, type);

            if (IGNORE_IF_DIFFER_ONLY_BY_SIZE) {
               Record match = record.findMatchIfIgnoreSizeFields(annotations);
               if (match!=null) {
                  System.out.println("WARN: Ignoring near-duplicate annotation on input line # " + lineNumber + ": '" + line);
                  System.out.println("      Annotations only differ by some size field(s)");
                  System.out.println("      " + record + " " + record);
                  System.out.println("      " + match);

                  for (String fieldName: record.getFields().getValueFieldNames()) {
                     System.out.println("  record's field " + quoted(fieldName) + " contains " + quoted(record.getValue(fieldName)));
                     System.out.println("   match's field " + quoted(fieldName) + " contains " + quoted(match.getValue(fieldName)));
                  }
                  continue readLines;
               }
            }

            if (INCLUDE_THOSE_WITHOUT_LOCATION) {
               record.fixLocationsWithLaterality();

               record.setId(lineNumber);
               annotations.add(record);

               if (!hasLocation(record)) {
                  // There can be a met where the primary location cannot be found/determined by science.
                  // There are case in gold where no primary location because not in the (few) records we have for the patient
                  System.out.println("INFO: Including " + whichSet.toString().toLowerCase() + " " + type + " that doesn't have a body location: " + record.getId());
               }

            } else {
               // In this case, those gold without a location are ignored so that we can report separately those with locations and those without
               if (isCancerRecord(type) && whichSet.equals(DataSet.GOLD) && !hasLocation(record)) {
                  // There are cancers in the gold without a location.
                  // For primary, the cancer registries dig deeper to find a location (but Alicia says there might still be some without a location)
                  // Also there can be a met where the primary location cannot be found/determined by science.
                  System.out.println("INFO: Ignoring " + whichSet.toString().toLowerCase() + " " + type + " because it doesn't have a body location: " + record.getPatientName() + " " + record.getId() );
               } else if (isTumorRecord(type) && whichSet.equals(DataSet.GOLD) && !hasLocation(record)) {
                  System.out.println("INFO: Ignoring " + whichSet.toString().toLowerCase() + " " + type + " because it doesn't have a body location: " + record.getPatientName() + " " + record.getDiagnosis());
               } else {
                  record.fixLocationsWithLaterality();
                  record.setId(lineNumber);
                  annotations.add(record);
               }
            }
         }

      }

      reader.close();
      return annotations;

   }

   /**
    * @param record annotation
    * @return true iff there is a body location field that is not empty
    */
   private boolean hasLocation(Record record) {
      for (String field : record.getFields().getAllFieldNames()) {
         if (field.equalsIgnoreCase(Fields.bodyLocationField) || (Record.PRIMARY_KEY_INDICATOR+field).equalsIgnoreCase(Fields.bodyLocationField)) {
            if (isNullOrZeroLength(record.getValue(field))) {
               return false;
            } else {
               return true;
            }
         }
      }
      return false;
   }

   /** record is for a Tumor (not Cancer or anything else)
    * @param "Cancer" "Tumor" etc
    * @return
    */
   private boolean isTumorRecord(String type) {
      return type.equalsIgnoreCase(TYPE_OF_DATA_TUMOR);
   }

   /** record is for a Cancer (not a Tumor)
    * @param type "Cancer" "Tumor" etc
    * @return
    */
   private boolean isCancerRecord(String type) {
      return type.equalsIgnoreCase(TYPE_OF_DATA_CANCER);
   }

   /**
    * Create an index (map) of the annotations, where the key is the patient name/ID
    * and the map's keys are ordered by patient ID
    * @param annotations the Records to be indexed by patient name
    * @return a LinkedHashMap from patient names to a List of the Records for that patient, where the
    * entries are sorted by the patient ID aka patient name
    */
   private LinkedHashMap<String, List<Record>> indexByPatient(List<Record> annotations) {

      LinkedHashMap<String, List<Record>> mapByPatient = new LinkedHashMap<>();
      ArrayList<String> patients = new ArrayList<>();
      for (Record r: annotations) {
         patients.add(r.getPatientName());
      }
      final Comparator<String> comparator = new NumberedSuffixComparator();
      Collections.sort(patients, comparator);

      // Create the lists so later we can just do an add without checking for null,
      // and so they get inserted in order
      for (String patient: patients) {
         mapByPatient.put(patient, new ArrayList<>());
      }

      // Add records to the map used to get a list of records for a patient name
      for (Record r: annotations) {
         mapByPatient.get(r.getPatientName()).add(r);
      }

      return mapByPatient;

   }

   /**
    * Add counts from a set of confusion matrices to the counts within another set of confusion matrices,
    * creating a confusion matrix for each of the fields that didn't already have a matrix
    * @param to
    * @param from
    */
   private void addCounts(Map<String, ConfusionMatrix> to, Map<String, ConfusionMatrix> from) {
      for (String field : from.keySet()) {
         ConfusionMatrix cm = to.get(field);
         if (cm == null) {
            cm = new ConfusionMatrix();
            to.put(field, cm);
         }
         cm.includeCountsFrom(from.get(field));
      }

   }

   /**
    * For a collection with elements, don't return the enclosing square brackets that Collection's toString() returns
    * @return Empty string if c is null
    */
   private static String toSimpleString(Collection c) {
      if (c == null) return "";
      String s = c.toString();
      if (s.startsWith("[") && s.endsWith("]")) {
         return s.substring(1, s.length() - 1);
      }
      return s;
   }

   private List<Record> filterTumorsByHistogram(List<Record> tumors, Map<String, Histogram> histograms) {
      List<Record> filtered = new ArrayList<>();
      for (Record tumor: tumors) {
         String patient = tumor.getPatientName();
         Histogram h = histograms.get(patient);
         if (h==null) throw new RuntimeException("Missing histogram for " + patient);
         if (passesFilters(tumor, h, tumors)) filtered.add(tumor);
      }
      return filtered;
   }

   public enum Variation {
      TUMOR_TYPE_CONTAINS_ARG,
      TUMOR_TYPE_NOT_PRIMARY,
      LOCATION_MEAN,
      LOCATION_MEAN_PLUS_X,
      LOC_LAT_MEAN,
      LOC_LAT_MEAN_PLUS_X,
      NO_EVAL_FILTERING,
      IGNORE_SKIN_PRIMARY_IF_OTHER_PRIMARY
   }
   private boolean passesFilters(Record tumor, Histogram histogram, List<Record> tumors) {
      if (variation.contains(Variation.NO_EVAL_FILTERING.name())) {
         if (variation.size() > 1) {
            throw new RuntimeException("NO_EVAL_FILTERING should be used alone. " + variation.size() + " variations were passed");
         }
         return true;
      }

      int variationsUsed = 0;
      boolean result = false;
      String patient = tumor.getPatientName();
      if (variation.contains(Variation.TUMOR_TYPE_CONTAINS_ARG.name())){
         variationsUsed++;
         String filterTerm = variationArg;
         if (isNullOrZeroLength(variationArg)) throw new RuntimeException("Check the parms passed -- expected an argument for the variation, such as -variation-arg=metast ");
         if (tumor.getTumorType().toLowerCase().contains(filterTerm.toLowerCase())) {
            System.out.println("Filtering out tumor of TumorType " + tumor.getTumorType() + " for " + patient + " using term " + quoted(filterTerm));
            return false;
         }
         result = true;
      }
      if (variation.contains(Variation.IGNORE_SKIN_PRIMARY_IF_OTHER_PRIMARY.name())) {
         variationsUsed++;
         if (tumor.locationIsJustSkin() && toStringSafely(tumor.getTumorType()).equals(Record.TUMOR_TYPE_PRIMARYTUMOR)) {
            if (hasSomeNonSkinPrimary(patient, tumors)) {
               System.out.println("Filtering out tumor with location Skin, of TumorType " + tumor.getTumorType() + " for " + patient);
               return false;
            }
         }
         result = true;

      }
      if (variation.contains(Variation.TUMOR_TYPE_NOT_PRIMARY.name())){
         variationsUsed++;
         String filterTerm = Record.TUMOR_TYPE_PRIMARYTUMOR;
         if (tumor.getTumorType().toLowerCase().contains(filterTerm.toLowerCase())) {
            System.out.println("Filtering out tumor of TumorType " + tumor.getTumorType() + " for " + patient + " using term " + quoted(filterTerm));
            return false;
         }
         result = true;
      }
      if (variation.contains(Variation.LOCATION_MEAN.name())) {
         variationsUsed++;
         if (passesLocCutoff(tumor, histogram, 0)) result = true;
         else {
            System.out.println("Filtering out tumor for " + patient + " using rule " + Variation.LOCATION_MEAN.name());
            return false;
         }
      }
      if (variation.contains(Variation.LOC_LAT_MEAN.name())) {
         variationsUsed++;
         if (passesLocAndLatCutoff(tumor, histogram, 0)) result = true;
         else {
            System.out.println("Filtering out tumor for " + patient + " using rule " + Variation.LOC_LAT_MEAN.name());
         }
      }

      if (variation.contains(Variation.LOCATION_MEAN_PLUS_X.name())) {
         variationsUsed++;
         int howFarAboveMean = parseIntFromVariationArg(variationArg);
         if (howFarAboveMean < 0) throw new RuntimeException("Check the parms passed -- expected an argument for the variation, such as -variation-arg=1 ");
         if (passesLocCutoff(tumor, histogram, howFarAboveMean)) result = true;
         else {
            System.out.println("Filtering out tumor for " + patient + " using rule " + Variation.LOCATION_MEAN_PLUS_X.name());
         }
      }
      if (variation.contains(Variation.LOC_LAT_MEAN_PLUS_X.name())) {
         variationsUsed++;
         int howFarAboveMean = parseIntFromVariationArg(variationArg);
         if (howFarAboveMean < 0) throw new RuntimeException("Check the parms passed -- expected an argument for the variation, such as -variation-arg=1 ");
         if (passesLocAndLatCutoff(tumor, histogram, howFarAboveMean)) result = true;
         else {
            System.out.println("Filtering out tumor for " + patient + " using rule " + Variation.LOC_LAT_MEAN_PLUS_X.name());
         }
      }

      if (variationsUsed < 1) {
         throw new RuntimeException("No valid variation was given - check parms passed -- expected a variation, such as --variation=" + Variation.NO_EVAL_FILTERING.name());
      }

      return result;

   }

   private boolean hasSomeNonSkinPrimary(String patientName, List<Record> tumors) {
      for (Record tumor: tumors) {
         if (tumor.getPatientName().equals(patientName)) {
            if (!tumor.locationIsJustSkin() && toStringSafely(tumor.getTumorType()).equals(Record.TUMOR_TYPE_PRIMARYTUMOR)) {
               return true;
            }
         }
      }
      return false;
   }


   private int parseIntFromVariationArg(String varArg) {
      try {
         return Integer.parseInt(varArg);
      } catch (Exception e) {
         throw new RuntimeException("unable to parse an int from -variation-arg=" + varArg);
      }
   }

   private boolean passesLocCutoff(Record tumor, Histogram histogram, int aboveMean) {
      for (String location: tumor.getLocations().split(Record.INTRA_VALUE_DELIMITER)) {
         int count = histogram.getCountByLoc(location);
         if (count >= (histogram.meanByLocation + aboveMean)) {
            return true;
         }
      }
      System.out.println("Filtering out tumor for " + tumor.getLocations() + " for " + tumor.getPatientName());
      return false;
   }

   private boolean passesLocAndLatCutoff(Record tumor, Histogram histogram, int aboveMean) {
      for (String location: tumor.getLocations().split(Record.INTRA_VALUE_DELIMITER)) {
         int count = histogram.getCountByLocAndLat(location, tumor.getLaterality());
         if ((count >= histogram.meanByLocAndLat + aboveMean)) {
            return true;
         }
      }
      System.out.println("Filtering out tumor for " + tumor.getLocations() + ", " + tumor.getLaterality() + " for " + tumor.getPatientName());
      return false;
   }

   private Map<String, Integer> countAnnotationsByPatient(List<Record> candAnnotations) {
      Map<String, Integer> map = new HashMap<>();
      for (Record record: candAnnotations) {
         addToValue(map, record.getPatientName(), 1);
      }
      return map;
   }
   private void addToValue(Map<String, Integer> map, String index, Integer count) {
      Integer current = map.get(index);
      if (current==null) {map.put(index, count); return;}
      map.put(index, (current+count));

   }


   // used when filtering Tumors By Histogram
   private void printNumPred(String patient, int countBeforeFiltering, int countAfterFiltering) {
      if (countBeforeFiltering!=countAfterFiltering) {
         System.out.println("   # pred for " + patient + ": " + countBeforeFiltering + " before filtering tumors by histogram");
      }
      System.out.println("   # pred for " + patient + ": " + countAfterFiltering);
   }

   /**
    * Evaluate phenotype of two delimited files, such as a set of gold (expected) annotations vs system output.
    * Written and tested with the delimited files being BSV files.
    *
    * @param goldFile File containing (human-curated) results, to compare the system output to
    * @param candidateFile File containing system output of the DeepPhe Phenotype summarizer
    * @param type Type of annotations within the files - Cancer or Tumor
    * @throws IOException If an I/O error occurs
    */
   private void evaluate(File goldFile, File candidateFile, String type, Map<String, Histogram> histograms) throws IOException {

      List<Record> goldAnnotations = loadAnnotations(goldFile, DataSet.GOLD, type);
      List<Record> candAnnotations = loadAnnotations(candidateFile, DataSet.CANDIDATE, type);
      Map<String, Integer> countCandByPatient = countAnnotationsByPatient(candAnnotations);

      if (histograms != null && type.toLowerCase().equals(TYPE_OF_DATA_TUMOR.toLowerCase())) {
         candAnnotations = filterTumorsByHistogram(candAnnotations, histograms);
      }

      LinkedHashMap<String, List<Record>> goldMapByPatientName = indexByPatient(goldAnnotations);
      LinkedHashMap<String, List<Record>> candMapByPatientName = indexByPatient(candAnnotations);

      System.out.println("Gold File:     \t" + goldFile.getAbsolutePath());
      System.out.println(" count: gold: " + goldAnnotations.size());
      System.out.println(" type: " + type);

      goldMapByPatientName.keySet().stream()
              .forEach(p -> System.out.println("   # gold for " + p + ": " + goldMapByPatientName.get(p).size()));
      System.out.println("Candidate File:\t" + candidateFile.getAbsolutePath());
      System.out.println(" count: candidate: " + candAnnotations.size());
      System.out.println(" type: " + type);
      if (histograms != null && type.toLowerCase().equals(TYPE_OF_DATA_TUMOR.toLowerCase())) {
         candMapByPatientName.keySet().stream()
                 .forEach(p -> printNumPred(p, countCandByPatient.get(p), candMapByPatientName.get(p).size()));
      } else {
         candMapByPatientName.keySet().stream()
                 .forEach(p -> System.out.println("   # pred for " + p + ": " + candMapByPatientName.get(p).size()));
      }
      System.out.println();

      ConfusionMatrix totalConfusion = new ConfusionMatrix();
      Map<String, ConfusionMatrix> attributeConfusions = new LinkedHashMap<>();

      final Collection<String> candidateNames = candMapByPatientName.keySet();

      verifyNoExtraSystemPatients(candidateNames, goldMapByPatientName);

      // Iterate over all annotations in this gold standard, looking for same patient in the candidate (system) output (annotations)
      // If no match, we have a FN.
      // If a candidate annotation "matches" the given gold standard annotation, we have a TP.
      List<Record> recordsToPrintOut = new ArrayList<>();
      Collection<Record> candidatesAlignedWithSomeGold = new HashSet<>();

      for (String goldName : goldMapByPatientName.keySet()) {
         if (!candidateNames.contains(goldName)) {
            // Patient was not annotated by the DeepPhe system (or there was a bug and no output produced by DeepPhe).  Skip the comparison.
            // don't use LOGGER for this warning because we want this output synchronously with the other output
            // Do not ignore this gold annotation completely -- unless we add an empty tumor indicator, we would not be reporting the FNs
            // for patients for whom DeepPhe found no tumors.
            System.out.println("WARN: No DeepPhe " + type + " candidate annotations found for " + goldName);
         }

         // Find the gold with the highest matching candidate.
         // This is especially important if there are more gold than candidate annotations.
         // We don't want to align a candidate with a gold if it could have a much better score
         // by leaving that gold unaligned and aligning the candidate with some other gold

         List<Record> remainingGold = new ArrayList<Record>(goldMapByPatientName.get(goldName));
         List<Record> candidates = candMapByPatientName.get(goldName);
         if (candidates == null) {
            candidates = new ArrayList<>();
         }
         List<Record> remainingCandidates = new ArrayList<>(candidates);

         while (remainingGold.size() > 0) {

            Pair<Record> pair = findPairWithBestScore(remainingGold, remainingCandidates);
            Record gold = pair.getValue1();
            if (gold == null) throw new RuntimeException("findPairWithBestScore should always return something since goldThisPatient.size()>0.");

            Record bestMatchingCandidate = pair.getValue2();

            remainingGold.remove(gold);

            recordsToPrintOut.add(gold);

            // If no candidate matches, have a FN
            if (bestMatchingCandidate == gold) { // indicates no candidate aligns/matches with this gold

               gold.setConfusion(ConfusionLabel.FN);
               totalConfusion.FN++;

            } else {
               // Found a candidate that matches at least the fields that are required to match to be considered aligned; we have a TP

               if (!shouldAllowMultipleMatches(bestMatchingCandidate)) {
                  remainingCandidates.remove(bestMatchingCandidate);
               }
               candidatesAlignedWithSomeGold.add(bestMatchingCandidate);
               gold.setMatchingCandidateRecord(bestMatchingCandidate);
               gold.setConfusion(ConfusionLabel.TP);
               totalConfusion.TPP++;
               totalConfusion.TP += gold.getWeightedScore();

               // calculate stats per attribute
               Map<String, ConfusionMatrix> attrConf = gold.getAttributeConfusionMatrices();
               addCounts(attributeConfusions, attrConf);

            }
         }
      }



      // each candidate annotation that did not "match" some gold standard annotation is a FP
      for (Record r: candAnnotations) {
         // empty cancer records are made with "__" to indicate the presence of a patient.
         if (r.getId().endsWith(EMPTY_CANCER_INDICATOR)) {
            continue;
         }
         if (r.isValid() && !candidatesAlignedWithSomeGold.contains(r)) {
            r.setConfusion(ConfusionLabel.FP);
            recordsToPrintOut.add(r);
            totalConfusion.FP++;
         }

      }

      // display individual counts
      if (PRINT_RECORD_LEVEL_STATS) {
         Collections.sort(recordsToPrintOut);
         for (Record r : recordsToPrintOut) {
            r.print(System.out, DETAIL.NORMAL);
         }
      } else if (LIST_DETAILED_RECORD_STATS) {
         // more verbose output
         Collections.sort(recordsToPrintOut);
         String previousPatient = "the.first.time.this.indicates.first.patient"; // indicator. any value that isn't a valid patient name could work
         System.out.println();
         for (Record r : recordsToPrintOut) {

            if (!previousPatient.equals(r.getPatientName())) {
               previousPatient = r.getPatientName();
               System.out.println("- - - - - - - - - - - - - - - - - - - | " + r.getPatientName() + " |- - - - - - - - - - - - - - - - - - - ");
               System.out.println();
            }
            // Even print out TPs so we can look at the attributes that matched or not
             r.print(System.out, DETAIL.VERBOSE);

         }

         System.out.println();
      }


      System.out.println("-----------------------------------------------------------------------------------------");
      String label = "Container";
      Pattern pt = Pattern.compile(TYPES_PATTERN);
      Matcher mt = pt.matcher(goldFile.getName());
      if (mt.find()) {
         label = mt.group();
      }

      ConfusionMatrix.printHeader(System.out);
      totalConfusion.print(System.out, label);
      for (String attribute : attributeConfusions.keySet()) {
         attributeConfusions.get(attribute).print(System.out, attribute);
      }

      printCounts(recordsToPrintOut, type);

      if (PRINT_WIKI_OUTPUT) {
         writeConfluenceTable(System.out, label, totalConfusion, attributeConfusions);
      } else {
         System.out.println("** ==>> Skipping writing output formatted for confluence wiki.");
      }

      if (PRINT_DEBUG_FINE_DETAIL) printLocations(goldMapByPatientName, candMapByPatientName);

   }

   static private void printCounts( final Collection<Record> records, String type ) {
      int goldCount = 0;
      int systemCount = 0;
      final Map<String, Integer> goldCounts = new HashMap<>();
      final Map<String, Integer> systemCounts = new HashMap<>();
      for ( Record record : records ) {
         if ( !record.isValid() ) {
            continue;
         }
         switch ( record.confusionLabel ) {
            case TP: {
               goldCount++;
               systemCount++;
               getContentCounts( record ).forEach( ( k, v ) -> goldCounts.merge( k, v, Integer::sum ) );
               getContentCounts( record.matchingCandidateRecord )
                     .forEach( ( k, v ) -> systemCounts.merge( k, v, Integer::sum ) );
               break;
            }
            case FP: {
               systemCount++;
               getContentCounts( record ).forEach( ( k, v ) -> systemCounts.merge( k, v, Integer::sum ) );
               break;
            }
            case FN: {
               goldCount++;
               getContentCounts( record ).forEach( ( k, v ) -> goldCounts.merge( k, v, Integer::sum ) );
               break;
            }
         }
      }
      System.out.println( "\nTotal Counts:" );
      System.out.println( "Gold Overall: " + goldCount + " " + type + "s.");
      goldCounts.entrySet().stream()
                .map( e -> String.format( "%1$30s   %2$d", e.getKey(), e.getValue() ) )
                .forEach( System.out::println );
      System.out.println( "\nSystem Overall: " + systemCount + " candidate " + type + "s.");
      systemCounts.entrySet().stream()
                  .map( e -> String.format( "%1$30s   %2$d", e.getKey(), e.getValue() ) )
                  .forEach( System.out::println );
   }

   static private Map<String, Integer> getContentCounts( final Record record ) {
      return record.content.entrySet().stream()
                           .filter( e -> !e.getValue().trim().isEmpty() )
                           .collect( Collectors.toMap( Map.Entry::getKey, e -> getCount( e.getValue() ) ) );
   }

   static private int getCount( final String text ) {
      if ( text == null || text.trim().isEmpty() ) {
         return 0;
      }
      return (int)Arrays.stream( StringUtil.fastSplit( text, ';' ) )
                        .map( String::trim )
                        .filter( t -> !t.isEmpty() )
                        .count();
   }

   private void verifyNoExtraSystemPatients(Collection<String> candidateNames, Map<String, List<Record>> goldAnnotations) {
      for (String systemName : candidateNames) {
         boolean match = false;
         for (List<Record> goldList: goldAnnotations.values()) {
            for (Record gold: goldList) {
               String goldName = gold.getPatientName();
               if (systemName.equals(goldName)) {
                  match = true;
               }
            }
         }
         if (!match) System.out.println("WARN: Found a candidate annotation for '" + systemName + "' but no gold annotations for that same patient.");
      }

   }

   private void printLocations(Map<String, List<Record>> goldAnnotations, Map<String, List<Record>> candidateAnnotations) {

      final Map<String, Collection<String>> goldLocations = new HashMap<>();
      collectLocationsByPatient(goldAnnotations, goldLocations);

      final Map<String, Collection<String>> candidateLocations = new HashMap<>();
      collectLocationsByPatient(candidateAnnotations, candidateLocations);

      final Collection<String> indices = new HashSet<>(goldLocations.keySet());
      indices.addAll(candidateLocations.keySet());
      final List<String> patientList = new ArrayList<>(indices);
      Collections.sort(patientList);
      for (String patient : patientList) {
         System.out.println("Patient " + patient);
         System.out.print(" Gold Locations : ");
         goldLocations.getOrDefault(patient, Collections.emptyList()).stream().sorted().forEach(loc -> System.out.print("  " + loc));
         System.out.println();
         System.out.print(" Sysm Locations : ");  // Candidate aka system locations
         candidateLocations.getOrDefault(patient, Collections.emptyList()).stream().sorted().forEach(loc -> System.out.print("  " + loc));
         System.out.println();
      }

   }

   /**
    * For each patient, collect all locations across all the records for that patient
    * @param namedRecords records indexed by patient name
    * @param patientLocations
    */
   static private void collectLocationsByPatient(final Map<String, List<Record>> namedRecords, final Map<String, Collection<String>> patientLocations) {
      namedRecords.values().forEach(list -> list.forEach(record -> collectLocationsByPatient(record, patientLocations)));
   }

   static private void collectLocationsByPatient(final Record record, final Map<String, Collection<String>> patientLocations) {

      final List<String> patientIds = record.convertDelimitedToSortedList(Fields.patientIdField);
      if (patientIds.size() > 1) {
         System.out.println("ERROR: Too many (" + patientIds.size() + ") patient IDs for record ID " + record.getId());
         return;
      } else if (patientIds.size() < 1) {
         System.out.println("WARN: No value in column '" + Fields.patientIdField + "' for " + record);
         return;
      }
      final String patientId = patientIds.get(0);
      final List<String> locations = record.convertDelimitedToSortedList(Fields.bodyLocationField);
      if (PRINT_DEBUG_FINE_DETAIL) {
         System.out.println("EXTRA DETAIL: Collecting locations per patient. One record for " + patientId + " has locations = " + record.getValue(Fields.bodyLocationField));
      }
      Collection<String> loc = patientLocations.get(patientId);
      if (loc == null) {
         loc = new HashSet<>();
         patientLocations.put(patientId, loc);
      }
      loc.addAll(locations);

   }

   static private void printPatientLocations(final Record record) {
      final List<String> patientIDs = record.convertDelimitedToSortedList(Fields.patientIdField);
      patientIDs.forEach(System.out::println);
      record.convertDelimitedToSortedList(Fields.bodyLocationField).forEach(System.out::println);
   }

   static private void writeF1(final PrintStream out, final double f1) {
      out.print( "|" );
      if ( Double.isNaN( f1 ) ) {
         out.print( "-" );
      } else {
         double cutoff = 0.7;
         if ( f1 < cutoff ) {
            out.print( "<span style=\"color: red\">" );
         }
         out.print( String.format( "%.4f", f1 ) );
         if ( f1 < cutoff ) {
            out.print( "</span>" );
         }
      }
      out.println();
      out.println("|-");
   }

   static private void writeConfluenceTable(final PrintStream out,
                                            final String label,
                                            final ConfusionMatrix totalConfusion,
                                            final Map<String, ConfusionMatrix> attributeConfusions) {
      final String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
      out.println();
      out.println("{| class=\"wikitable\"");
      out.println("!Name");
      out.println("![[Error Analysis Run " + date + "| F1 from " + date + "]]");
      out.println("|-");
      out.println("|'''" + label + "'''");
      writeF1(out, totalConfusion.getFscore());
      out.println("");
      for (Map.Entry<String, ConfusionMatrix> entry : attributeConfusions.entrySet()) {
         out.println("|" + entry.getKey());
         writeF1(out, entry.getValue().getFscore());
      }
      out.println("|}");
   }


   static private List<String> readFile(File file) throws IOException {
      List<String> lines = new ArrayList<>();
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
         String line = br.readLine();
         while (line != null) {
            lines.add(line);
            line = br.readLine();
         }
      } catch (IOException e) {
         throw e;
      }

      return lines;
   }

}
