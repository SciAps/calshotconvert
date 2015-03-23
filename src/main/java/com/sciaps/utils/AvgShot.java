package com.sciaps.utils;

import com.google.common.base.Throwables;
import com.sciaps.common.algorithms.DarkPixSubtract;
import com.sciaps.common.data.Shot;
import com.sciaps.common.spectrum.LIBZPixelSpectrum;
import com.sciaps.common.spectrum.Spectrum;
import com.sciaps.common.utils.LIBZPixelShotAvg;
import com.sciaps.common.utils.ShotDataHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


public class AvgShot implements Shot {

    static Logger logger = LoggerFactory.getLogger(AvgShot.class);

    private final File[] mShotFiles;
    private Spectrum mSpectrum;

    public AvgShot(File[] singleCompressedShotFiles) {
        mShotFiles = singleCompressedShotFiles.clone();
    }

    @Override
    public synchronized Spectrum getSpectrum() {
        if(mSpectrum == null) {
            mSpectrum = getPixelSpectrum().createSpectrum();
        }
        return mSpectrum;
    }

    public LIBZPixelSpectrum getPixelSpectrum() {
        LIBZPixelShotAvg shotAverager = new LIBZPixelShotAvg();
        for (File file : mShotFiles) {
            LIBZPixelSpectrum shotData = null;
            try {
                shotData = ShotDataHelper.loadCompressedFile(file);
            } catch (IOException e) {
                Throwables.propagate(e);
            }

            for(int i=0;i<shotData.pixels.length;i++) {
                if(shotData.pixels[i].length != 2066) {
                    logger.warn("file: {} has incorrect buffer size: {}", file.getAbsolutePath(), shotData.pixels[i].length);
                }
            }

            doDarkPixSubtract(shotData);
            shotAverager.addLIBZPixelSpectrum(shotData);
        }
        return shotAverager.getPixelSpectrum();
    }

    private static void doDarkPixSubtract(LIBZPixelSpectrum pixelSpectrum) {
        double[][] pixData = pixelSpectrum.pixels;
        for(int i=0;i<pixData.length;i++){
            DarkPixSubtract.doDarkPixSubtract(pixData[i], pixData[i].length);
        }
    }
}
