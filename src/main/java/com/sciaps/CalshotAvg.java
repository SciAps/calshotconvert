package com.sciaps;


import com.devsmart.IOUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.sciaps.common.data.LIBZTest;
import com.sciaps.common.data.Standard;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.utils.ShotDataHelper;
import com.sciaps.utils.AvgShot;
import com.sciaps.utils.CurveDataManager;
import com.sciaps.utils.EmpiricalCurvesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CalshotAvg {

    static Logger logger = LoggerFactory.getLogger(CalshotAvg.class);
    private static final HashFunction spectrumChecksum = Hashing.sha1();

    private final CurveDataManager curveDataManager;


    public CalshotAvg(CurveDataManager curveDataManager) {
        this.curveDataManager = curveDataManager;

    }

    public void doIt(ZipOutputStream zipOut, int numShotAvg) throws IOException {


        final OutputStreamWriter writer = new OutputStreamWriter(zipOut, Charsets.UTF_8);

        for(String standardName : curveDataManager.getStandardNamesWithShotData()) {

            final Standard standard = new Standard();
            standard.mId = EmpiricalCurvesManager.NamingHashFunction.newHasher()
                    .putString(standardName, Charsets.UTF_8)
                    .hash()
                    .toString();

            File[] allShotFiles = curveDataManager.getSingleShotFilesForStandard(standardName);

            List<List<File>> shotGroup = Lists.partition(Arrays.asList(allShotFiles), 60);
            for(List<File> shotFiles : shotGroup) {
                Map<String, String> shotTable = new HashMap<String,String>();
                Collections.shuffle(shotFiles);

                for(List<File> smallAvg : Lists.partition(shotFiles, numShotAvg)){
                    AvgShot avgShot = new AvgShot(smallAvg.toArray(new File[smallAvg.size()]));

                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ShotDataHelper.saveCompressed(avgShot.getPixelSpectrum(), bout);
                    bout.close();

                    byte[] data = bout.toByteArray();
                    final String shotId = spectrumChecksum.hashBytes(data).toString();

                    logger.info("creating random avg from {} shots for {}", smallAvg.size(), standard.name);

                    ZipEntry spectrumEntry = new ZipEntry(String.format("spectrum/%s.gz", shotId));
                    zipOut.putNextEntry(spectrumEntry);
                    IOUtils.pump(new ByteArrayInputStream(data), zipOut, true, false);
                    zipOut.closeEntry();

                    String name = String.format("shot_%d", shotTable.size());
                    shotTable.put(name, shotId);
                }

                LIBZTest test = new LIBZTest();
                test.mId = UUID.randomUUID().toString();
                test.standard = standard;

                test.saveIds(mIdLookup);
                JsonElement element = EmpiricalCurvesManager.TypeGson.toJsonTree(test, test.getClass());
                Map<String, Object> newProps = EmpiricalCurvesManager.TypeGson.fromJson(element, EmpiricalCurvesManager.MapType);
                newProps.put("type", "test");
                newProps.put("shotTable", shotTable);

                logger.info("writing test: standard {} : {}", standardName, test.mId);

                final String fileName = String.format("dbobj/test/%s.json", test.mId);
                ZipEntry entry = new ZipEntry(fileName);
                zipOut.putNextEntry(entry);
                EmpiricalCurvesManager.ZipGson.toJson(newProps, writer);
                writer.flush();
                zipOut.closeEntry();
            }

        }

    }

    private DBObj.IdLookup mIdLookup = new DBObj.IdLookup() {
        @Override
        public String getId(Object obj) {
            return ((DBObj)obj).mId;
        }
    };



}
