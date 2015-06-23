package com.sciaps;


import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DBMerge {

    static Logger logger = LoggerFactory.getLogger(DBMerge.class);

    public static void main(String[] args) {

        Options options = new Options();

        options.addOption(OptionBuilder
                        .hasArg()
                        .withArgName("seed.db")
                        .withDescription("the seed db")
                        .isRequired()
                        .create("seed")
        );

        options.addOption(OptionBuilder
                        .hasArg()
                        .withArgName("libzdb.db")
                        .withDescription("the calibration data db")
                        .isRequired()
                        .create("caldata")
        );

        options.addOption(OptionBuilder
                        .hasArg()
                        .withArgName("libzdb.db")
                        .withDescription("the output db")
                        .create("o")
        );

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            final File seedsdb = new File(cmd.getOptionValue("seed"));
            final File caldatadb = new File(cmd.getOptionValue("caldata"));

            final File outputfile = new File(cmd.getOptionValue("o", "libzdb.sdb"));






        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "dbmerge", options );
            System.exit(-1);
        } catch(Exception e){
            logger.error("", e);
            System.exit(-1);
        }
    }
}
