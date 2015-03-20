package com.sciaps.utils;


import com.google.common.base.Charsets;
import com.sciaps.common.algorithms.AlloyFingerprintLibraryCreator;
import com.sciaps.common.algorithms.FingerprintLibraryCreator;
import com.sciaps.common.data.IRRatio;
import com.sciaps.common.data.Model;
import com.sciaps.common.data.Shot;
import com.sciaps.common.data.Standard;
import com.sciaps.common.data.fingerprint.FingerprintLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class LIBZFingerprintCreator {

    static Logger logger = LoggerFactory.getLogger(LIBZFingerprintCreator.class);

    private static final HashSet<String> SpecialityBases = new HashSet<String>();

    static {
        String specialityBaseStr = "Ag, Co, Cr, Hf, Mg, Mn, Mo, Nb, Pb, Re, Sn, Ta, V, W, Zn, Zr";
        for(String baseStr : specialityBaseStr.split(",")){
            baseStr = baseStr.trim();
            SpecialityBases.add(baseStr);
        }

    }

    final EmpiricalCurvesManager mEmpiricalCurvesManager;
    final CurveDataManager mCurveDataManager;

    public LIBZFingerprintCreator(CurveDataManager curveDataManager, EmpiricalCurvesManager empiricalCurvesManager) {
        mCurveDataManager = curveDataManager;
        mEmpiricalCurvesManager = empiricalCurvesManager;

    }

    abstract List<IRRatio> getSpecilityRatios();
    abstract List<IRRatio> getFPRatios(String name);
    abstract double[] getWeights(String name);

    private AlloyFingerprintLibraryCreator createAlloyFPCreator(String name) {
        return createAlloyFPCreator(name, getModelByName(name));
    }

    private AlloyFingerprintLibraryCreator createAlloyFPCreator(String name, Model model) {
        List<IRRatio> ratios = getFPRatios(name);
        double[] weights = getWeights(name);

        if(model == null || ratios == null || weights == null) {
            throw new RuntimeException("cannot create Alloy library for " + name);
        }

        return new AlloyFingerprintLibraryCreator(name, ratios, model, weights);
    }

    Model getModelByName(String name) {
        Model model = null;
        for(Model m : mEmpiricalCurvesManager.converter.modelList) {
            if(m.name.equals(name)) {
                model = m;
                break;
            }
        }
        return model;
    }

    Standard getStandardByName(String standardName) {
        Standard retval = null;
        for(Standard s : mEmpiricalCurvesManager.converter.standards) {
            if(s.name.equals(standardName)) {
                retval = s;
                break;
            }
        }
        return retval;
    }

    public void export(ZipOutputStream zipout, int numShotAvg) throws IOException {

        FingerprintLibraryCreator alloyLib = new FingerprintLibraryCreator("AlloyBases", getSpecilityRatios());

        AlloyFingerprintLibraryCreator niLib = createAlloyFPCreator("Ni", getModelByName("NiStd"));
        AlloyFingerprintLibraryCreator stainlessLib = createAlloyFPCreator("Stainless", getModelByName("StainlessStd"));
        AlloyFingerprintLibraryCreator cuLib = createAlloyFPCreator("Cu", getModelByName("CuStd"));
        AlloyFingerprintLibraryCreator alLib = createAlloyFPCreator("Al");
        AlloyFingerprintLibraryCreator tiLib = createAlloyFPCreator("Ti");

        for(String standardName : mCurveDataManager.getStandardNamesWithShotData()){
            File[] singleShotFiles = mCurveDataManager.getSingleShotFilesForStandard(standardName);

            final Standard standard = getStandardByName(standardName);

            if (standard == null) {
                logger.error("Can't find standard by the name of {}", standardName);

            } else {

                final String baseName = standard.getBase();

                final Shot avgSpectrum = new AvgShot(singleShotFiles);

                alloyLib.addToLibrary(standardName, avgSpectrum.getSpectrum());
                Collection<Shot> shots = CurveDataManager.createRandomAvgOf(numShotAvg, singleShotFiles);

                if (baseName.startsWith("Ni")) {
                    niLib.addToLibrary(standard, avgSpectrum.getSpectrum(), shots);
                } else if (baseName.startsWith("Stainless")) {
                    stainlessLib.addToLibrary(standard, avgSpectrum.getSpectrum(), shots);
                } else if (baseName.startsWith("Cu")) {
                    cuLib.addToLibrary(standard, avgSpectrum.getSpectrum(), shots);
                } else if (baseName.startsWith("Al")) {
                    alLib.addToLibrary(standard, avgSpectrum.getSpectrum(), shots);
                } else if (baseName.startsWith("Ti")) {
                    tiLib.addToLibrary(standard, avgSpectrum.getSpectrum(), shots);
                }
            }
        }

        final OutputStreamWriter writer = new OutputStreamWriter(zipout, Charsets.UTF_8);

        write(zipout, writer, alloyLib.finalizeLibrary(), String.format("fplib/%s.json", "Alloy"));
        write(zipout, writer, niLib.finalizeLibrary(), String.format("fplib/%s.json", "Ni"));
        write(zipout, writer, stainlessLib.finalizeLibrary(), String.format("fplib/%s.json", "Stainless"));
        write(zipout, writer, cuLib.finalizeLibrary(), String.format("fplib/%s.json", "Cu"));
        write(zipout, writer, alLib.finalizeLibrary(), String.format("fplib/%s.json", "Al"));
        write(zipout, writer, tiLib.finalizeLibrary(), String.format("fplib/%s.json", "Ti"));


    }

    private void write(ZipOutputStream zipOutputStream, OutputStreamWriter writer, FingerprintLibrary library, String name) throws IOException {

        logger.info("writing FP library file {}", name);

        ZipEntry entry = new ZipEntry(name);
        zipOutputStream.putNextEntry(entry);
        EmpiricalCurvesManager.ZipGson.toJson(library, writer);
        writer.flush();

    }

}
