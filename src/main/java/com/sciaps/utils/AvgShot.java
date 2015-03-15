package com.sciaps.utils;

import com.google.common.base.Throwables;
import com.sciaps.common.algorithms.DarkPixSubtract;
import com.sciaps.common.data.Shot;
import com.sciaps.common.spectrum.LIBZPixelSpectrum;
import com.sciaps.common.spectrum.Spectrum;
import com.sciaps.common.utils.LIBZPixelShotAvg;
import com.sciaps.common.utils.ShotDataHelper;

import java.io.File;
import java.io.IOException;


public class AvgShot implements Shot {

    private final File[] mShotFiles;
    private Spectrum mSpectrum;

    public AvgShot(File[] singleCompressedShotFiles) {
        mShotFiles = singleCompressedShotFiles.clone();
    }

    @Override
    public synchronized Spectrum getSpectrum() {
        if(mSpectrum == null) {
            LIBZPixelShotAvg shotAverager = new LIBZPixelShotAvg();
            for (File file : mShotFiles) {
                LIBZPixelSpectrum shotData = null;
                try {
                    shotData = ShotDataHelper.loadCompressedFile(file);
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
                doDarkPixSubtract(shotData);
                shotAverager.addLIBZPixelSpectrum(shotData);
                mSpectrum = shotAverager.getSpectrum();
            }
        }
        return mSpectrum;
    }

    private static void doDarkPixSubtract(LIBZPixelSpectrum pixelSpectrum) {
        double[][] pixData = pixelSpectrum.pixels;
        for(int i=0;i<pixData.length;i++){
            DarkPixSubtract.doDarkPixSubtract(pixData[i], pixData[i].length);
        }
    }
}
