package com.sciaps;


import com.google.common.base.Stopwatch;
import com.sciaps.utils.*;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;

public class DBBridge {

    static Logger logger = LoggerFactory.getLogger(DBBridge.class);


    public static void main(String[] args) {

        Options options = new Options();

        options.addOption(OptionBuilder
                        .withArgName("dir")
                        .hasArg()
                        .withDescription("path to CurveData dir")
                        .create("curveData")
        );

        options.addOption(OptionBuilder
                        .withArgName("libzdb.sdb")
                        .hasArg()
                        .withDescription("output Sciaps DataBase file")
                        .create("out")
        );

        options.addOption(OptionBuilder
                        .withArgName("num")
                        .hasArg()
                        .withDescription("number of shots to avg together (default 10)")
                        .create("numShots")
        );

        options.addOption(OptionBuilder
                .withArgName("EmpiricalCurves.json")
                .hasArg()
                .withDescription("path to the EmpiricalCurves.json file")
                .create("empiricalCurve"));

        options.addOption(OptionBuilder
                .withArgName("assays.json")
                .hasArg()
                .withDescription("path to assays.json file")
                .create("assays"));

        options.addOption(OptionBuilder
                .withArgName("LIBZAnalysis")
                .hasArg()
                .withDescription("path to LIBZAnalysis dir")
                .create("libzAnalysis"));

        options.addOption(OptionBuilder
                .withArgName("z500|z100")
                .hasArg()
                .isRequired()
                .withDescription("type of instrument (z500|z100)")
                .create("type"));

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            final File empiricalCurvesFile;
            final File assaysFile;
            final File curveDataDir;

            if(cmd.hasOption("libzAnalysis")) {
                File libzAnalysisDir = new File(cmd.getOptionValue("libzAnalysis"));
                empiricalCurvesFile = new File(libzAnalysisDir, "EmpiricalCurves.json");
                assaysFile = new File(libzAnalysisDir, "assays.json");
                curveDataDir = new File(libzAnalysisDir, "CurveData");
            } else {
                curveDataDir = new File(cmd.getOptionValue("curveData"));
                empiricalCurvesFile = new File(cmd.getOptionValue("empiricalCurve"));
                assaysFile = new File(cmd.getOptionValue("assays"));
            }

            if(!curveDataDir.exists()){
                logger.error("curveData dir does not exist: {}", curveDataDir.getAbsolutePath());
                System.exit(-1);
            }

            if(!empiricalCurvesFile.exists()) {
                logger.error("empiricalCurve file does not exist: {}", empiricalCurvesFile.getAbsolutePath());
                System.exit(-1);
            }

            if(!assaysFile.exists()) {
                logger.error("assays file does not exist: {}", assaysFile.getAbsolutePath());
                System.exit(-1);
            }

            logger.info("using {}", empiricalCurvesFile.getAbsolutePath());
            logger.info("using {}", assaysFile.getAbsolutePath());
            logger.info("using {}", curveDataDir.getAbsolutePath());

            final File outputFile = new File(cmd.getOptionValue("out", "libzdb.sdb"));
            logger.info("outputfile: {}", outputFile.getAbsolutePath());

            final int numShotAvg = Integer.parseInt(cmd.getOptionValue("numShots", "10"));
            logger.info("using {} shot avg", numShotAvg);

            Stopwatch sw = Stopwatch.createStarted();

            LIBZFingerprintCreator fpcreator = null;

            CurveDataManager curveDataManager = new CurveDataManager(curveDataDir);

            EmpiricalCurvesManager empiricalCurvesManager = new EmpiricalCurvesManager(empiricalCurvesFile, assaysFile, curveDataManager);
            CalshotAvg avg = new CalshotAvg(curveDataManager, empiricalCurvesManager);

            empiricalCurvesManager.load();

            final String type = cmd.getOptionValue("type");
            if("z500".equals(type)) {
                fpcreator = new Z500FPCreator(curveDataManager, empiricalCurvesManager);
            } else if("z100".equals(type)) {
                fpcreator = new Z100FPCreator(curveDataManager, empiricalCurvesManager);
            } else {
                logger.error("unknown type: {}", type);
                System.exit(-1);
            }

            ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(outputFile));

            empiricalCurvesManager.export(zipout, numShotAvg);
            avg.doIt(zipout, numShotAvg);
            fpcreator.export(zipout, numShotAvg);
            zipout.close();

            sw.stop();
            logger.info("Done. process took {}", sw);


        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "calshotconvert", options );
            System.exit(-1);
        } catch(Exception e){
            logger.error("", e);
            System.exit(-1);
        }

    }

}
