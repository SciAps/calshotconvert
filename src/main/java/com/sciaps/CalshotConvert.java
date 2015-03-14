package com.sciaps;


import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CalshotConvert {

    static Logger logger = LoggerFactory.getLogger(CalshotConvert.class);


    public static void main(String[] args) {
        Options options = new Options();

        options.addOption(OptionBuilder
                        .withArgName("dir")
                        .hasArg()
                        .withDescription("path to LIBZAnalysis")
                        .create("analysisDir")
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
                        .isRequired(false)
                        .create("numShots")
        );

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            File libzAnalysisDir = new File(cmd.getOptionValue("analysisDir"));
            if(libzAnalysisDir.exists()){
                logger.error("analysisDir does not exist: {}", libzAnalysisDir.getAbsolutePath());
                System.exit(-1);
            }


        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "calshotconvert", options );
            System.exit(-1);
        }


    }
}
