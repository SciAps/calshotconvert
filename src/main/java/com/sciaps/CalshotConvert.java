package com.sciaps;


import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class CalshotConvert {

    static Logger logger = LoggerFactory.getLogger(CalshotConvert.class);


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

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            final File curveDataDir = new File(cmd.getOptionValue("curveData"));
            if(!curveDataDir.exists()){
                logger.error("curveData dir does not exist: {}", curveDataDir.getAbsolutePath());
                System.exit(-1);
            }
            CalshotAvg avg = new CalshotAvg(curveDataDir);


            File outputFile = new File(cmd.getOptionValue("out", "libzdb.sdb"));
            logger.info("outputfile: {}", outputFile.getAbsolutePath());

            final int numShotAvg = Integer.parseInt(cmd.getOptionValue("numShots", "10"));
            logger.info("using {} shot avg", numShotAvg);

            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile));
            avg.doIt(zipOut, numShotAvg);
            zipOut.close();

        } catch(IOException e){
            logger.error("", e);
            System.exit(-1);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "calshotconvert", options );
            System.exit(-1);
        }
    }


}
