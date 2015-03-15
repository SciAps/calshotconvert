package com.sciaps;


import com.sciaps.utils.CurveDataManager;
import com.sciaps.utils.EmpiricalCurvesManager;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class DBBridge {

    static Logger logger = LoggerFactory.getLogger(DBBridge.class);


    public static void main(String[] args) {

        Options options = new Options();

        options.addOption(OptionBuilder
                        .withArgName("dir")
                        .hasArg()
                        .withDescription("path to CurveData dir")
                        .isRequired()
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

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            final File curveDataDir = new File(cmd.getOptionValue("curveData"));
            if(!curveDataDir.exists()){
                logger.error("curveData dir does not exist: {}", curveDataDir.getAbsolutePath());
                System.exit(-1);
            }

            final File outputFile = new File(cmd.getOptionValue("out", "libzdb.sdb"));
            logger.info("outputfile: {}", outputFile.getAbsolutePath());

            final int numShotAvg = Integer.parseInt(cmd.getOptionValue("numShots", "10"));
            logger.info("using {} shot avg", numShotAvg);

            final File empiricalCurvesFile = new File(cmd.getOptionValue("empiricalCurve"));
            if(!empiricalCurvesFile.exists()) {
                logger.error("empiricalCurve file does not exist: {}", empiricalCurvesFile.getAbsolutePath());
                System.exit(-1);
            }

            final File assaysFile = new File(cmd.getOptionValue("assays"));
            if(!assaysFile.exists()) {
                logger.error("assays file does not exist: {}", assaysFile.getAbsolutePath());
                System.exit(-1);
            }

            CurveDataManager curveDataManager = new CurveDataManager(curveDataDir);
            CalshotAvg avg = new CalshotAvg(curveDataManager);
            EmpiricalCurvesManager empiricalCurvesManager = new EmpiricalCurvesManager(empiricalCurvesFile, assaysFile, curveDataManager);

            empiricalCurvesManager.load();

            ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(outputFile));

            empiricalCurvesManager.export(zipout, numShotAvg);


            avg.doIt(zipout, numShotAvg);
            zipout.close();

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
