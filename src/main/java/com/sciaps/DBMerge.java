package com.sciaps;


import com.devsmart.IOUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.sciaps.utils.LIBZDB;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DBMerge {

    static Logger logger = LoggerFactory.getLogger(DBMerge.class);
    private static final Pattern SPECTRUM_FILE_REGEX = Pattern.compile("spectrum/(.*).gz");
    public static final HashFunction NamingHashFunction = Hashing.sha1();

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






            LIBZDB seedDB = new LIBZDB();
            {
                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(seedsdb));
                seedDB.load(zipInputStream);
                zipInputStream.close();
            }

            LIBZDB caldataDB = new LIBZDB();
            {
                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(caldatadb));
                caldataDB.load(zipInputStream);
                zipInputStream.close();
            }


            logger.info("merged db output to: {}", outputfile.getAbsolutePath());
            ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(outputfile));

            /// regions ///
            Iterator<LIBZDB.DBEntry> it = seedDB.getAllOfType("region");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();
                entry.writeToZip(zipout);
            }

            /// models ///
            it = seedDB.getAllOfType("model");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();
                JsonObject model = entry.value.getAsJsonObject();

                JsonObject irs = model.getAsJsonObject("irs");
                for(Map.Entry<String, JsonElement> irEntry : irs.entrySet()) {
                    JsonObject curveObj = irEntry.getValue().getAsJsonObject();
                    curveObj.add("excludedStandards", transformStandardsList(curveObj.getAsJsonArray("excludedStandards"), seedDB));
                }

                model.add("standardList", transformStandardsList(model.getAsJsonArray("standardList"), seedDB));

                entry.writeToZip(zipout);
            }

            /// Standards ///
            it = seedDB.getAllOfType("standard");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();

                LIBZDB.DBEntry newEntry = new LIBZDB.DBEntry(entry);
                newEntry.key = transformStandardId(entry.key, seedDB);
                newEntry.writeToZip(zipout);

            }

            /// Tests ///
            it = caldataDB.getAllOfType("test");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();

                JsonObject testObj = entry.value.getAsJsonObject();
                testObj.add("standard", transformStandardsList(testObj.getAsJsonArray("standard"), caldataDB));

                entry.writeToZip(zipout);

            }

            /// Spectrum ///
            {
                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(caldatadb));
                ZipEntry entry = null;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    final String name = entry.getName();
                    Matcher m = SPECTRUM_FILE_REGEX.matcher(name);
                    if(m.find()) {
                        String id = m.group(1);
                        logger.info("loading spectrum file: {}", id);

                        ZipEntry outEntry = new ZipEntry(name);
                        zipout.putNextEntry(outEntry);

                        IOUtils.pump(zipInputStream, zipout, false, false);
                        //zipout.flush();
                    }

                }
                zipInputStream.close();
            }

            zipout.close();


            logger.info("done");



        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "dbmerge", options );
            System.exit(-1);
        } catch(Exception e){
            logger.error("", e);
            System.exit(-1);
        }
    }

    private static String transformStandardId(String standardId, LIBZDB db) {
        LIBZDB.DBEntry standardEntry = db.get(standardId);
        if(standardEntry == null) {
            throw new RuntimeException("no standard with id: " + standardId);
        }

        JsonObject standardObj = standardEntry.value.getAsJsonObject();



        final String standardName = standardObj.getAsJsonPrimitive("name").getAsString();

        final String newId = NamingHashFunction.newHasher()
                .putString(standardName, Charsets.UTF_8)
                .hash()
                .toString();


        return newId;
    }

    private static JsonArray transformStandardsList(JsonArray input, LIBZDB db) {

        JsonArray retval = new JsonArray();

        for(int i=0;i<input.size();i++) {
            JsonElement oldidprim = input.get(i);
            if(!oldidprim.isJsonNull()) {
                String oldId = input.get(i).getAsString();

                String newId = transformStandardId(oldId, db);

                retval.add(new JsonPrimitive(newId));
            } else {
                retval.add(JsonNull.INSTANCE);
            }
        }

        return retval;

    }
}
