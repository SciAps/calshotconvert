package com.sciaps.utils;


import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
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


}
