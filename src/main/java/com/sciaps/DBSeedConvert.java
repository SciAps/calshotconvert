package com.sciaps;


import com.devsmart.IOUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sciaps.utils.LIBZDB;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DBSeedConvert {

    static Logger logger = LoggerFactory.getLogger(DBMerge.class);
    private static final Pattern SPECTRUM_FILE_REGEX = Pattern.compile("spectrum/(.*).gz");
    public static final HashFunction NamingHashFunction = Hashing.sha1();

    private static Gson GSON = new GsonBuilder().create();

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
                        .withArgName("newdbseed.db")
                        .withDescription("the new dbseed")
                        .create("o")
        );

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            final File seedsdb = new File(cmd.getOptionValue("seed"));
            final File caldatadb = new File(cmd.getOptionValue("caldata"));

            final File outputfile = new File(cmd.getOptionValue("o", "newdbseed.sdb"));



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


            ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(outputfile));

            Iterator<LIBZDB.DBEntry> it;

            /// regions ///
            it = seedDB.getAllOfType("region");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();
                entry.writeToZip(zipout);
            }

            HashSet<String> standardsToGet = new HashSet<String>();

            /// models ///
            it = seedDB.getAllOfType("model");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();
                JsonObject seedModel = entry.value.getAsJsonObject();

                final String modelName = seedModel.getAsJsonPrimitive("name").getAsString();
                logger.info("working on model: {}", modelName);

                LIBZDB.DBEntry caldatamodelEntry = getObjectWithTypeAndName("model", modelName, caldataDB);
                if(caldatamodelEntry == null) {
                    throw new Exception("missing Model: " + modelName);
                }
                JsonObject calModelObj = caldatamodelEntry.value.getAsJsonObject();



                JsonObject irs = seedModel.getAsJsonObject("irs");
                JsonObject calIrs = calModelObj.getAsJsonObject("irs");
                for(Map.Entry<String, JsonElement> irEntry : irs.entrySet()) {
                    JsonObject seedCurveObj = irEntry.getValue().getAsJsonObject();
                    JsonObject calCurveObj = calIrs.getAsJsonObject(irEntry.getKey());


                    HashSet<String> standardsSet = new HashSet<String>();

                    List<String> standardIds = GSON.fromJson(
                            transformStandardsList(seedCurveObj.getAsJsonArray("excludedStandards"), seedDB),
                            new TypeToken<List<String>>(){}.getType());

                    standardsSet.addAll(standardIds);
                    standardsToGet.addAll(standardIds);

                    standardIds = GSON.fromJson(
                            transformStandardsList(calCurveObj.getAsJsonArray("excludedStandards"), caldataDB),
                            new TypeToken<List<String>>(){}.getType());

                    standardsSet.addAll(standardIds);
                    standardsToGet.addAll(standardIds);

                    seedCurveObj.add("excludedStandards", GSON.toJsonTree(standardsSet));

                }


                HashSet<String> standardsSet = new HashSet<String>();


                List<String> convertedStandardIds = GSON.fromJson(
                        transformStandardsList(seedModel.getAsJsonArray("standardList"), seedDB),
                        new TypeToken<List<String>>(){}.getType());


                standardsSet.addAll(convertedStandardIds);
                standardsToGet.addAll(convertedStandardIds);

                convertedStandardIds = GSON.fromJson(
                        transformStandardsList(calModelObj.getAsJsonArray("standardList"), caldataDB),
                        new TypeToken<List<String>>() {}.getType());

                standardsSet.addAll(convertedStandardIds);
                standardsToGet.addAll(convertedStandardIds);

                seedModel.add("standardList", GSON.toJsonTree(standardsSet));

                entry.writeToZip(zipout);
            }

            HashSet<String> addedStandards = new HashSet<String>();


            /// Standards from seed ///
            it = seedDB.getAllOfType("standard");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();

                LIBZDB.DBEntry newEntry = new LIBZDB.DBEntry(entry);
                newEntry.key = transformStandardId(entry.key, seedDB);
                if(addedStandards.add(newEntry.key)) {
                    logger.info("adding standard from dbseed {} {}", newEntry.key, newEntry.value.getAsJsonObject().getAsJsonPrimitive("name"));
                    newEntry.writeToZip(zipout);
                }

            }

            /// Standards from seed ///
            it = caldataDB.getAllOfType("standard");
            while(it.hasNext()) {
                LIBZDB.DBEntry entry = it.next();

                LIBZDB.DBEntry newEntry = new LIBZDB.DBEntry(entry);
                newEntry.key = transformStandardId(entry.key, caldataDB);
                if(addedStandards.add(newEntry.key)) {
                    logger.info("adding standard from caldata {} {}", newEntry.key, newEntry.value.getAsJsonObject().getAsJsonPrimitive("name"));
                    newEntry.writeToZip(zipout);
                }

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

    private static List<JsonElement> toList(JsonArray input) {
        ArrayList<JsonElement> retval = new ArrayList<JsonElement>(input.size());
        for(int i=0;i<input.size();i++){
            retval.add(input.get(i));
        }
        return retval;
    }

    private static LIBZDB.DBEntry getObjectWithTypeAndName(String type, String name, LIBZDB db) {
        Iterator<LIBZDB.DBEntry> it = db.getAllOfType(type);
        while(it.hasNext()) {
            LIBZDB.DBEntry entry = it.next();

            if(entry.value.isJsonObject()) {
                JsonPrimitive nameProp = entry.value.getAsJsonObject().getAsJsonPrimitive("name");
                if(nameProp.isString() && name.equals(nameProp.getAsString())) {
                    return entry;
                }
            }
        }

        return null;
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
