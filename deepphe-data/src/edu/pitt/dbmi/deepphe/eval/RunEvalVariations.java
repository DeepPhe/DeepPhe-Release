package edu.pitt.dbmi.deepphe.eval;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import edu.pitt.dbmi.deepphe.eval.PhenotypeEval.Variation;

/**
 *
 */
public class RunEvalVariations {

    static private final String CLASS_NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    static private final Logger LOGGER = Logger.getLogger(CLASS_NAME);
    static private final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM.dd.HHmm");
    static private final String MEAN_ADJUSTMENT = "1";

    static private List<String> MESSAGES = new ArrayList<>();

    public static void main(String[] args) {

        if (args.length != 2 && args.length != 4) throw new RuntimeException("Usage: " + CLASS_NAME + " -dir  some-path <-subdir parent-of-EMR_EVAL>");
        File dir = null;
        String subdir = null;
        String badDir = "";
        try {
            badDir = args[1];
            if (!args[0].equals("-dir"))
                throw new RuntimeException("Missing -dir argument. " + "Usage: " + CLASS_NAME + " -dir  some-path");
            dir = new File(args[1]);
            if (!dir.isDirectory()) throw new RuntimeException("Not a directory: " + dir);
            if (args.length > 2) {
                badDir = args[3];
                if (!args[2].equals("-subdir")) {
                    throw new RuntimeException("Missing -subdir argument. " + "Usage: " + CLASS_NAME + " -dir  some-path <-subdir parent-of-EMR_EVAL>");
                }
                subdir = args[3];
                if (subdir==null || subdir.trim().length()==0) throw new RuntimeException("Not a directory: " + subdir);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error trying to access directory: " + badDir);
        }

        Date date = new Date();
        System.out.println("Started running " + CLASS_NAME + " at " + date);
        PrintStream outBefore = System.out;
        PrintStream errBefore = System.err;


        runVariations(dir, subdir);

        System.setOut(outBefore);
        System.setErr(errBefore);

        Date endDate = new Date();
        Double elapsed = new Double(endDate.getTime() - date.getTime());
        elapsed = elapsed / 1000.0;
        String elapsedTime = (elapsed > 60.0 ? (elapsed / 60.0) + " minutes." : elapsed + " seconds.");
        System.out.println("Finished running " + CLASS_NAME + " at " + endDate + ". Elapsed time = " + elapsedTime);

    }

    private static void runVariations(File directory, String subdir) {
        try {
            runVariations(directory.getAbsolutePath(), subdir);
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter buffer = new StringWriter();
            e.printStackTrace(new PrintWriter(buffer));
            MESSAGES.add(buffer.toString());
        }
    }

    private static void runVariations(String absolutePathOfDir, String subdir) throws FileNotFoundException {

        String goldParentDir = absolutePathOfDir + "\\V2.gold\\";
        String systemOutputParentDir = absolutePathOfDir + "\\output.DeepPhe.piper\\";

        String EVAL_SUBDIR = "EMR_EVAL"; // EMR_EVAL - Copy";
        String[] datasets = new String[]{
                "V2.LSU.Train.brca.output",         // 0
                "V2.LSU.Train.melanoma.output",     // 1
                "V2.UKY.Train.brca.output",         // 2
                "V2.UKY.Train.melanoma.output",     // 3
                "V2.UPMC.ov.Train.output",          // 4
                "V2.UPMC.Train.brca.output",        // 5
                "V2.UPMC.Train.melanoma.output",    // 6
                "V2.SEER.BrCa.Train.SIMPLE.output", // 7
        };

        LinkedHashMap<String, String> goldDir = new LinkedHashMap<>();
        goldDir.put(datasets[0], goldParentDir + "V2.LSU.Train.brca.gold");
        goldDir.put(datasets[1], goldParentDir + "V2.LSU.melanoma.train.gold");
        goldDir.put(datasets[2], goldParentDir + "V2.UKY.Train.brca.gold");
        goldDir.put(datasets[3], goldParentDir + "V2.UKY.Train.melanoma.gold");
        goldDir.put(datasets[4], goldParentDir + "V2.UPMC.ov.Train.gold");
        goldDir.put(datasets[5], goldParentDir + "V2.UPMC.brca.Train.gold");
        goldDir.put(datasets[6], goldParentDir + "UPMC.Melanoma.Train.gold");
        goldDir.put(datasets[7], goldParentDir + "V2.SEER.Train.SIMPLE.gold");
        if (datasets.length != goldDir.size()) {
            throw new RuntimeException("Fix the number of paths or variation names.");
        }

        String DIR_NAME = (subdir==null ? "06.26.histo" : subdir); //"06.26.histo";
        LinkedHashMap<String, String> systemOutputDir = new LinkedHashMap<>();
        for (int i=0; i<datasets.length; i++) {
            systemOutputDir.put(datasets[i], systemOutputParentDir + datasets[i] + "\\" + DIR_NAME + "\\" + EVAL_SUBDIR);
        }
        if (datasets.length != systemOutputDir.size()) {
            throw new RuntimeException("Fix the number of paths or variation names.");
        }

        process(datasets, goldDir, systemOutputDir, systemOutputParentDir);

    }

    private static void process(String[] datasets, HashMap<String, String> goldDir, HashMap<String, String> systemOutputDir, String systemOutputParentDir) throws FileNotFoundException {

        String[] evalArgs;
        String batchStartDatetime = DATE_TIME_FORMAT.format(new Date());

        int i = 0;
        for (String dataset : datasets) {
            if (i > 1000) { // Change this to modify which datasets to run if don't want to run all that are defined within datasets
                System.out.println("Skipping dataset " + dataset);
                continue;
            }

            String histogramDir = (new File(systemOutputDir.get(dataset))).getParent() + "\\histogram";
            evalArgs = new String[]{
                    "-verbose",
                    "",    // "-variation="
                    "",    // "-variation-arg=",
                    goldDir.get(dataset),          // Such as   /some-path-to-gold/UPMC.Melanoma.Train.gold
                    systemOutputDir.get(dataset),  // Such as   /some-path-to-dPhe-output/EMR_EVAL
                    histogramDir,                  // Such as   /some-path-to-dPhe-output/histogram

            };

            for (Variation variation : Variation.values()) {

                evalArgs[1] = "-variation=" + variation;
                if (variation.equals(Variation.TUMOR_TYPE_CONTAINS_ARG)) {
                    evalArgs[2] = "-variation-arg=" + "metast";
                } else if (variation.equals(Variation.LOC_LAT_MEAN_PLUS_X)) {
                    evalArgs[2] = "-variation-arg=" + MEAN_ADJUSTMENT;
                } else if (variation.equals(Variation.LOCATION_MEAN_PLUS_X)) {
                    evalArgs[2] = "-variation-arg=" + MEAN_ADJUSTMENT;
                } else {
                    evalArgs[2] = "";
                }

                redirectConsole(batchStartDatetime, systemOutputParentDir + "eval.output", variation, dataset);

                if (false) {
                    skipEvalAndPrintDebugInfo(evalArgs, null);
                } else if (!(new File(histogramDir)).exists()){
                    skipEvalAndPrintDebugInfo(evalArgs, "Histogram directory missing");
                } else {
                    try {
                        MESSAGES.add("\nRunning eval with " + evalArgs.length + " arguments:");
                        printArgs(evalArgs);
                        PhenotypeEval.main(evalArgs);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out))); // reset to using original stdout
                System.out.println("\nPrinting out queued up messages:");
                for (String line : MESSAGES) {
                    System.out.println(line);
                }
                MESSAGES.clear();
                System.out.println("Completed variation " + variation + " for dataset " + dataset);

            }
            i++;

        }
    }

    private static void printArgs(String[] evalArgs) {
        int i = 0;
        for (String s : evalArgs) {
            MESSAGES.add("arg [" + i + "] = " + s);
            i++;
        }

    }

    private static void skipEvalAndPrintDebugInfo(String[] evalArgs, String reason) {

        LOGGER.info("Starting...");
        if (reason==null) {
            // test where these end up since we've redirect stdout and stderr
            LOGGER.warning("Skipping running actual eval to test parms....");
            LOGGER.severe("Severe log msg here.");
            System.out.println("This is system out.");
            System.err.println("This is system err aka stderr.");
        } else {
            LOGGER.warning("Skipping running eval tool because: " + reason);
        }
        // output the arguments that would have been used
        System.out.println("evalArgs=" + evalArgs);
        printArgs(evalArgs);

        LOGGER.info("Ending...");
    }


    static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "MM.dd" );
    private static void redirectConsole(String batchStartDatetime, String evalOutputDir, Variation variation, String dataset) throws FileNotFoundException {
        String date = DATE_FORMAT.format( new Date() );

        if (evalOutputDir.endsWith("\\") || evalOutputDir.endsWith("/") ) {

        } else {
            evalOutputDir +="\\";
        }
        evalOutputDir += (batchStartDatetime + "\\");
        File f = new File(evalOutputDir);
        f.mkdir();
        String filename = /***  date + "." +  ***/   dataset + "." + variation.name() + ".eval.txt";
        System.setOut(new PrintStream(new FileOutputStream(evalOutputDir + filename)));
        String filenameErr = date + "." + dataset + "." + variation.name() + ".eval.stderr.txt";
        File stderrFile = new File(evalOutputDir + filenameErr);
        FileOutputStream stderr = new FileOutputStream(stderrFile);
        System.err.println("Redirecting system error to " +  stderrFile.getAbsolutePath());
        System.setErr(new PrintStream(stderr));

    }

}
