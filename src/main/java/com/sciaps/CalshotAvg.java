package com.sciaps;


import com.devsmart.IOUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sciaps.common.algorithms.DarkPixSubtract;
import com.sciaps.common.data.LIBZTest;
import com.sciaps.common.data.Standard;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdRefTypeAdapterFactory;
import com.sciaps.common.spectrum.LIBZPixelSpectrum;
import com.sciaps.common.utils.LIBZPixelShotAvg;
import com.sciaps.common.utils.ShotDataHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CalshotAvg {

    static Logger logger = LoggerFactory.getLogger(CalshotAvg.class);
    private static HashFunction hashFunction = Hashing.sha1();
    private static Type MapType = new TypeToken<Map<String, Object>>(){}.getType();


    private final Gson TypeGson;
    private final Gson ZipGson;


    public final File baseDir;

    public CalshotAvg(File curveDataDir) {
        baseDir = curveDataDir;
        TypeGson = new GsonBuilder()
                .registerTypeAdapterFactory(new IdRefTypeAdapterFactory())
                .serializeNulls()
                .serializeSpecialFloatingPointValues()
                .create();

        ZipGson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .serializeSpecialFloatingPointValues()
                .create();
    }

    private static final Pattern singleShotPattern = Pattern.compile("([0-9]+)\\.json\\.gz");

    private static int getShotNum(String filename) {
        int retval = -1;
        Matcher m = singleShotPattern.matcher(filename);
        if(m.find()) {
            retval = Integer.parseInt(m.group(1));
        }
        return retval;
    }

    private static void doDarkPixSubtract(LIBZPixelSpectrum pixelSpectrum) {
        double[][] pixData = pixelSpectrum.pixels;
        for(int i=0;i<pixData.length;i++){
            DarkPixSubtract.doDarkPixSubtract(pixData[i], pixData[i].length);
        }
    }

    private static void loadAndAvg(List<File> files, Map<String, String> shotTable, String standardName, int numShotAvg, ZipOutputStream zipOut) throws IOException {
        Collections.shuffle(files);
        try {
            while (!files.isEmpty()) {
                LIBZPixelShotAvg shotAverager = new LIBZPixelShotAvg();
                final int numShots = Math.min(files.size(), numShotAvg);
                for (int i = 0; i < numShots; i++) {
                    File shotFile = files.remove(0);
                    LIBZPixelSpectrum shotData = ShotDataHelper.loadCompressedFile(shotFile);
                    doDarkPixSubtract(shotData);
                    shotAverager.addLIBZPixelSpectrum(shotData);
                }

                final int shotNum = shotTable.size();

                logger.info("{}:{} creating avg spectrum from {} shots", standardName, shotNum, numShots);

                LIBZPixelSpectrum avgPixSpectrum = shotAverager.getPixelSpectrum();

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ShotDataHelper.saveCompressed(avgPixSpectrum, bout);
                bout.close();

                byte[] data = bout.toByteArray();
                final String shotId = hashFunction.hashBytes(data).toString();

                ZipEntry spectrumEntry = new ZipEntry(String.format("spectrum/%s.gz", shotId));
                zipOut.putNextEntry(spectrumEntry);
                IOUtils.pump(new ByteArrayInputStream(data), zipOut, true, false);
                zipOut.closeEntry();

                String name = String.format("shot_%d", shotNum);
                shotTable.put(name, shotId);
            }
        } finally {
            files.clear();
        }
    }

    public void doIt(ZipOutputStream zipOut, int numShotAvg) throws IOException {

        final HashFunction standardIDHashFunction = Hashing.sha1();
        final OutputStreamWriter writer = new OutputStreamWriter(zipOut, Charsets.UTF_8);


        File[] standardDirs = baseDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        Arrays.sort(standardDirs, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for(File standardDir : standardDirs) {
            final String standardName = standardDir.getName();

            final String standardId = standardIDHashFunction.newHasher()
                    .putString(standardName, Charsets.UTF_8)
                    .hash()
                    .toString();

            LIBZTest test = new LIBZTest();
            test.standard = new Standard();
            test.standard.mId = standardId;

            File[] shotFiles = standardDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String filename = file.getName();
                    Matcher m = singleShotPattern.matcher(filename);
                    return m.find();
                }
            });

            Arrays.sort(shotFiles, new Comparator<File>() {
                @Override
                public int compare(File file, File file2) {
                    int AshotNum = getShotNum(file.getName());
                    int BshotNum = getShotNum(file2.getName());

                    int retval = AshotNum - BshotNum;
                    return retval;
                }
            });

            Map<String, String> shotTable = new HashMap<String,String>();
            ArrayList<File> shotGroupFiles = new ArrayList<File>(60);

            for(File shotFile : shotFiles){
                shotGroupFiles.add(shotFile);
                if(shotGroupFiles.size() >= 60){
                    try {
                        loadAndAvg(shotGroupFiles, shotTable, standardName, numShotAvg, zipOut);
                    } catch (IOException e) {
                        logger.error("", e);
                    }
                }
            }
            if(!shotGroupFiles.isEmpty()) {
                try {
                    loadAndAvg(shotGroupFiles, shotTable, standardName, numShotAvg, zipOut);
                } catch (IOException e) {
                    logger.error("", e);
                }
            }

            //dirty hack because we do not know what the original raster setting were when calibration was shot
            test.config.rasterNumLocations = 1;
            test.config.numShotsPerLocation = shotTable.size();

            final String testId = UUID.randomUUID().toString();
            test.mId = testId;

            test.saveIds(mIdLookup);
            JsonElement element = TypeGson.toJsonTree(test, test.getClass());
            Map<String, Object> newProps = TypeGson.fromJson(element, MapType);
            newProps.put("type", "test");
            newProps.put("shotTable", shotTable);

            logger.info("writing test: {}", testId);

            final String fileName = String.format("dbobj/test/%s.json", testId);
            ZipEntry entry = new ZipEntry(fileName);
            zipOut.putNextEntry(entry);
            ZipGson.toJson(newProps, writer);
            writer.flush();
            zipOut.closeEntry();

        }

    }

    private DBObj.IdLookup mIdLookup = new DBObj.IdLookup() {
        @Override
        public String getId(Object obj) {
            return ((DBObj)obj).mId;
        }
    };



}
