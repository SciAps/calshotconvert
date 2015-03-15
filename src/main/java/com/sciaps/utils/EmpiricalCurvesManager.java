package com.sciaps.utils;


import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sciaps.common.calculation.libs.EmpiricalCurveCreator;
import com.sciaps.common.data.*;
import com.sciaps.common.database.EmpiricalCurveConverter;
import com.sciaps.common.objtracker.DBObj;
import com.sciaps.common.objtracker.IdRefTypeAdapterFactory;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EmpiricalCurvesManager {

    static Logger logger = LoggerFactory.getLogger(EmpiricalCurvesManager.class);

    public static final HashFunction NamingHashFunction = Hashing.sha1();

    public static final Type MapType = new TypeToken<Map<String, Object>>(){}.getType();

    public static final Gson TypeGson = new GsonBuilder()
                        .registerTypeAdapterFactory(new IdRefTypeAdapterFactory())
                                .serializeNulls()
                        .serializeSpecialFloatingPointValues()
                        .create();

    public static final Gson ZipGson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .serializeSpecialFloatingPointValues()
            .create();

    private final File assaysFiles;
    private final File empiricalCurvesFile;
    public final EmpiricalCurveConverter converter;
    private final CurveDataManager curveDataManager;

    public EmpiricalCurvesManager(File empiricalCurvesFile, File assaysFiles, CurveDataManager curveDataManager) {
        this.empiricalCurvesFile = empiricalCurvesFile;
        this.assaysFiles = assaysFiles;
        this.curveDataManager = curveDataManager;

        converter = new EmpiricalCurveConverter();
        converter.setAssaysFile(assaysFiles);
        converter.setEmpiricalCurvesFile(empiricalCurvesFile);
    }

    public void load() throws Exception {
        converter.convert();
    }

    private DBObj.IdLookup mIdLookup = new DBObj.IdLookup() {
        @Override
        public String getId(Object obj) {
            return ((DBObj)obj).mId;
        }
    };


    public void export(ZipOutputStream zipout, int numShotAvg) throws IOException {
        final OutputStreamWriter writer = new OutputStreamWriter(zipout, Charsets.UTF_8);

        //load standards
        for(Standard standard : converter.standards) {
            /*
             * use determinalistic hash for standards so that calibration data will work
             * even if you re-import the database.
             */
            standard.mId = NamingHashFunction.newHasher()
                    .putString(standard.name, Charsets.UTF_8)
                    .hash()
                    .toString();

            JsonElement element = TypeGson.toJsonTree(standard, standard.getClass());
            Map<String, Object> newProps = TypeGson.fromJson(element, MapType);
            newProps.put("type", "standard");

            logger.info("writing standard {} : {}", standard.name, standard.mId);

            final String fileName = String.format("dbobj/standard/%s.json", standard.mId);
            ZipEntry entry = new ZipEntry(fileName);
            zipout.putNextEntry(entry);
            ZipGson.toJson(newProps, writer);
            writer.flush();
            zipout.closeEntry();

        }

        //load regions
        for(EmpiricalCurveConverter.MyRegion region : converter.regions) {
            Region r = region.getRegion();

            r.mId = UUID.randomUUID().toString();

            JsonElement element = TypeGson.toJsonTree(r, r.getClass());
            Map<String, Object> newProps = TypeGson.fromJson(element, MapType);
            newProps.put("type", "region");

            logger.info("writing region {} : {}", r.name, r.mId);

            final String fileName = String.format("dbobj/region/%s.json", r.mId);
            ZipEntry entry = new ZipEntry(fileName);
            zipout.putNextEntry(entry);
            ZipGson.toJson(newProps, writer);
            writer.flush();
            zipout.closeEntry();
        }

        //load calibration models
        for(Model model : converter.modelList) {
            model.mId = UUID.randomUUID().toString();
            model.saveIds(mIdLookup);

            for(IRCurve curve : model.irs.values()) {
                curve.saveIds(mIdLookup);

                EmpiricalCurveCreator curveCreator = new EmpiricalCurveCreator(curve.degree, curve.forceZero, 0.1);

                ArrayList<EmpiricalCurveCreator.Sample> samples = new ArrayList<EmpiricalCurveCreator.Sample>();
                for(Standard s : model.standardList) {
                    if(!curve.excludedStandards.contains(s)) {
                        File[] shotFiles = curveDataManager.getSingleShotFilesForStandard(s.name);
                        if(shotFiles != null && shotFiles.length > 0) {
                            EmpiricalCurveCreator.Sample sample = new EmpiricalCurveCreator.Sample();
                            sample.standard = s;
                            sample.shots = CurveDataManager.createRandomAvgOf(numShotAvg, shotFiles);
                            samples.add(sample);
                        } else {
                            logger.warn("no shot data for {} {}", model, s);
                        }
                    }
                }


                if(!samples.isEmpty()) {
                    try {
                        PolynomialFunction polynomialFunction = curveCreator.createCurve(curve, samples);
                        curve.coefficients = polynomialFunction.getCoefficients();
                        curve.irRange = curveCreator.getIRRange();
                        curve.r2 = curveCreator.getRSquared();
                        logger.info("curve for {} {} {} : {} r2: {}", model, curve.element, polynomialFunction, curve.irRange, curve.r2);
                    } catch (Exception e) {
                        logger.error("error computing curve for {} {}", model, curve.element, e);
                    }
                }
            }



            JsonElement element = TypeGson.toJsonTree(model, model.getClass());
            Map<String, Object> newProps = TypeGson.fromJson(element, MapType);
            newProps.put("type", "model");

            logger.info("writing model {} : {}", model.name, model.mId);

            final String fileName = String.format("dbobj/model/%s.json", model.mId);
            ZipEntry entry = new ZipEntry(fileName);
            zipout.putNextEntry(entry);
            ZipGson.toJson(newProps, writer);
            writer.flush();
            zipout.closeEntry();

        }
    }




}
