package com.sciaps.utils;


import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.sciaps.common.data.Shot;

import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurveDataManager {

    private static final Comparator<File> StandardsFileComparator = new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private static final Pattern SingleShotPattern = Pattern.compile("([0-9]+)\\.json\\.gz");

    public final File rootDir;
    private final File[] mStandardDirs;

    public CurveDataManager(File rootDir) {
        this.rootDir = rootDir;


        mStandardDirs = rootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        Arrays.sort(mStandardDirs, StandardsFileComparator);
    }

    private static int getShotNum(String filename) {
        int retval = -1;
        Matcher m = SingleShotPattern.matcher(filename);
        if(m.find()) {
            retval = Integer.parseInt(m.group(1));
        }
        return retval;
    }

    public List<String> getStandardNamesWithShotData() {
        ArrayList<String> retval = new ArrayList<String>(mStandardDirs.length);
        for(File standardDir : mStandardDirs) {
            retval.add(standardDir.getName());
        }
        return retval;
    }

    public File[] getSingleShotFilesForStandard(String standardName) {
        File[] retval = null;
        int i = Arrays.binarySearch(mStandardDirs, new File(standardName), StandardsFileComparator);
        if(i >= 0){
            //found
            File standardDir = mStandardDirs[i];
            retval = standardDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String filename = file.getName();
                    Matcher m = SingleShotPattern.matcher(filename);
                    return m.find();
                }
            });

            Arrays.sort(retval, new Comparator<File>() {
                @Override
                public int compare(File file, File file2) {
                    int AshotNum = getShotNum(file.getName());
                    int BshotNum = getShotNum(file2.getName());

                    int retval = AshotNum - BshotNum;
                    return retval;
                }
            });
        }

        return retval;
    }

    public static Collection<Shot> createRandomAvgOf(int numShots, File[] allSingleShotFiles) {

        List<List<File>> shotGroup = Lists.partition(Arrays.asList(allSingleShotFiles), 60);
        ArrayList<Shot> retval = new ArrayList<Shot>();

        for(List<File> singleShots : shotGroup) {
            Collections.shuffle(singleShots);
            List<List<File>> smallAvg = Lists.partition(singleShots, numShots);
            retval.addAll(Lists.transform(smallAvg, new Function<List<File>, Shot>() {
                @Override
                public Shot apply(List<File> input) {
                    return new AvgShot(input.toArray(new File[input.size()]));
                }
            }));
        }

        return retval;
    }


}
