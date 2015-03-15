package com.sciaps.utils;


import com.google.common.base.Throwables;
import com.sciaps.common.algorithms.OneIntensityValue;
import com.sciaps.common.algorithms.SGolayIntensity;
import com.sciaps.common.data.IRRatio;
import com.sciaps.common.data.Region;
import com.sciaps.common.data.RegionBuilder;
import org.apache.commons.lang.math.DoubleRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Z500FPCreator extends LIBZFingerprintCreator {

    private static final String Speciality =        "395.80-396.60,259.67-260.10,273.83-274.13,371.87-372.15,334.65-335.17,341.23-341.60,324.43-324.93,338.00-338.45,519.80-521.50,544.50-549.00,228.40-228.80,257.90-258.17,340.35-340.60,344.75-345.03,350.13-350.40,359.14-359.60,277.23-277.43,281.97-282.13,310.77-310.97,339.80-340.03,279.30-280.60,293.20-293.50,402.85-403.73,550.30-551.50,552.70-554.00,405.70-406.00,407.80-408.10,409.92-410.20,405.30-406.00,221.25-221.60,227.35-227.65,283.90-284.10,317.25-317.67,239.85-240.25,263.43-263.63,310.85-311.17,326.55-326.84,411.06-411.33,437.60-438.15,400.70-401.00,407.31-407.51,207.70-208.10,334.00-334.70,480.50-481.50,339.00-339.25,343.60-343.93,349.45-349.80";

    private static final String AlloyNI =           "251.35-251.85,288.07-288.35,394.15-394.65,395.90-396.30,334.60-335.17,337.05-337.40,360.25-360.73,359.10-359.60,293.20-293.50,294.80-295.10,402.80-403.73,259.60-260.30,371.80-372.15,257.90-258.13,344.75-345.03,345.15-345.50,350.13-350.40,341.20-341.70,324.43-324.93,327.10-327.55,281.50-281.78,386.25-386.53,550.25-551.50,552.70-554.00,309.35-309.50,405.70-406.00,407.75-408.13,409.85-410.40,207.85-208.10,400.70-401.00";
    public static final double[] AlloyNiFudge = new double[]{0.2,0.2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0.3,1,1,0.3,1,1};

    private static final String AlloyStainless =    "251.33-251.85,288.07-288.40,394.10-394.70,395.90-396.30,334.46-335.20,337.10-337.50,310.85-311.25,410.90-411.40,437.60-438.00,359.10-359.60,293.05-293.45,294.75-295.10,402.85-403.73,257.90-258.20,344.75-345.10,345.10-345.55,348.70-349.10,352.80-353.20,350.05-350.45,341.15-341.70,324.45-325.10,327.15-327.75,281.40-281.80,386.20-386.53,550.35-551.15,309.20-309.55,405.60-406.13,407.80-408.30,409.85-410.40,400.70-401.10";
    public static final double[] AlloyStainlessFudge = new double[]{1,1,1,1,1,1,1,1,1,1,1,1,3,1,1,1,1,1,1,1,1,1,0.3,3,5,1,1,1,0.1,0.01};

    private static final String AlloyCu =           "312.65-313.40,394.10-394.75,395.80-396.45,251.33-251.87,288.03-288.35,357.68-358.00,359.14-359.52,360.35-360.75,259.15-259.55,293.20-293.50,402.85-403.73,259.55-260.20,273.83-275.20,371.80-372.15,257.90-258.17,340.35-340.60,341.10-341.65,349.00-349.45,351.22-351.70,352.15-352.70,334.00-334.75,480.30-481.50,195.85-196.35,203.90-204.20,338.10-338.35,283.85-284.20,303.20-303.55,317.25-317.67,405.30-406.05,306.45-307.00";
    public static final double[] AlloyCuFudge = new double[]{1,1,1,1,1,1,1,1,1,1,1,1,1,0.3,0.01,0.01,1,1,1,1,3,3,1,1,0.1,1,1,1,1,1};

    private static final String AlloyAl =           "670.44-671.22,278.75-281.00,284.90-285.50,382.50-384.30,251.15-252.20,287.90-288.50,334.65-335.17,335.75-336.35,337.05-337.40,357.68-358.00,359.14-359.60,360.35-360.75,293.20-293.50,294.70-295.10,402.85-403.73,259.50-260.20,341.23-341.60,349.00-349.45,352.20-352.65,361.70-362.15,324.43-324.93,327.10-327.55,334.00-334.70,480.30-481.50,338.93-339.25,343.60-343.93,349.45-349.80,424.60-426.00,216.30-217.70,225.10-225.65";
    public static final double[] AlloyAlFudge = new double[]{1,1,1,1,1,1,1,1,0.1,1,1,1,1,1,1,0.5,1,1,1,1,1,1,1,1,1,1,1,1,1,1};

    private static final String AlloyTi =           "259.70-260.15,273.83-274.15,281.50-281.70,283.80-284.08,303.15-303.55,310.85-311.25,326.60-326.90,410.90-411.40,437.60-438.15,339.10-339.30,343.55-344.00,349.50-349.80,357.65-358.00,359.23-359.43,360.30-360.80,424.90-426.00,427.00-427.95,394.10-394.55,395.90-396.55,405.70-406.13,407.90-408.30,409.85-410.30,317.25-317.77,379.70-380.00,386.20-386.50,550.35-551.15,552.80-554.00";
    public static final double[] AlloyTiFudge = new double[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};



    static Map<DoubleRange, Region> alloyNiDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyStainlessDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyCuDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyAlDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyTiDenom = new HashMap<DoubleRange, Region>();


    static {

        alloyNiDenom.put(new DoubleRange(175, 255), new RegionBuilder("221.50-221.85").algo(SGolayIntensity.class).create());
        alloyNiDenom.put(new DoubleRange(255, 315), new RegionBuilder("300.18-300.55").algo(SGolayIntensity.class).create());
        alloyNiDenom.put(new DoubleRange(315, 420), new RegionBuilder("341.23-341.60").algo(SGolayIntensity.class).create());
        alloyNiDenom.put(new DoubleRange(420, 680), new RegionBuilder("507.50-509.00").algo(SGolayIntensity.class).create());

        alloyStainlessDenom.put(new DoubleRange(175, 255), new RegionBuilder("238.00-238.50").algo(SGolayIntensity.class).create());
        alloyStainlessDenom.put(new DoubleRange(255, 315), new RegionBuilder("259.67-260.10").algo(SGolayIntensity.class).create());
        alloyStainlessDenom.put(new DoubleRange(315, 420), new RegionBuilder("371.87-372.15").algo(SGolayIntensity.class).create());
        alloyStainlessDenom.put(new DoubleRange(420, 680), new RegionBuilder("438.05-438.70").algo(SGolayIntensity.class).create());

        alloyCuDenom.put(new DoubleRange(175, 255), new RegionBuilder("224.00-225.00").algo(SGolayIntensity.class).create());
        alloyCuDenom.put(new DoubleRange(255, 315), new RegionBuilder("295.95-296.30").algo(SGolayIntensity.class).create());
        alloyCuDenom.put(new DoubleRange(315, 420), new RegionBuilder("324.43-324.93").algo(SGolayIntensity.class).create());
        alloyCuDenom.put(new DoubleRange(420, 680), new RegionBuilder("521.20-522.60").algo(SGolayIntensity.class).create());

        alloyAlDenom.put(new DoubleRange(175, 255), new RegionBuilder("236.10-238.00").algo(SGolayIntensity.class).create());
        alloyAlDenom.put(new DoubleRange(255, 315), new RegionBuilder("307.80-310.00").algo(SGolayIntensity.class).create());
        alloyAlDenom.put(new DoubleRange(315, 420), new RegionBuilder("395.80-396.60").algo(SGolayIntensity.class).create());
        alloyAlDenom.put(new DoubleRange(420, 680), new RegionBuilder("395.80-396.60").algo(SGolayIntensity.class).create());

        alloyTiDenom.put(new DoubleRange(175, 255), new RegionBuilder("225.90-227.20").algo(SGolayIntensity.class).create());
        alloyTiDenom.put(new DoubleRange(255, 315), new RegionBuilder("308.50-309.00").algo(SGolayIntensity.class).create());
        alloyTiDenom.put(new DoubleRange(315, 420), new RegionBuilder("334.65-335.17").algo(SGolayIntensity.class).create());
        alloyTiDenom.put(new DoubleRange(420, 680), new RegionBuilder("429.50-431.00").algo(SGolayIntensity.class).create());

    }

    public Z500FPCreator(CurveDataManager curveDataManager, EmpiricalCurvesManager empiricalCurvesManager) {
        super(curveDataManager, empiricalCurvesManager);
    }

    private static void parseRegions(String str, List<Region> list) throws Exception {
        for(String regionStr : str.split(",")) {
            Region region = Region.parse(regionStr);
            region.params.put("name", SGolayIntensity.class.getName());
            list.add(region);
        }
    }

    private static List<IRRatio> createRatios(List<Region> numorators, Region denominator) {
        ArrayList<IRRatio> retval = new ArrayList<IRRatio>();

        for(Region num : numorators) {
            IRRatio r = new IRRatio();
            retval.add(r);
            r.numerator = new ArrayList<Region>();
            r.numerator.add(num);

            r.denominator.add(denominator);
        }

        return retval;
    }

    private static List<IRRatio> createRatios(List<Region> numorators, Map<DoubleRange, Region> denomMap) {
        ArrayList<IRRatio> retval = new ArrayList<IRRatio>();

        for(Region num : numorators) {
            IRRatio r = new IRRatio();
            retval.add(r);
            r.numerator = new ArrayList<Region>();
            r.numerator.add(num);


            final double wlmid = getMidpoint(num.wavelengthRange);
            setIRDenom(r.denominator, wlmid, denomMap);
        }

        return retval;
    }

    private static void setIRDenom(List<Region> denominators, double specNm, Map<DoubleRange, Region> denomMap){
        for(Map.Entry<DoubleRange, Region> entry : denomMap.entrySet()) {
            if(entry.getKey().containsDouble(specNm)){
                denominators.add(entry.getValue());
                break;
            }
        }
    }

    private static double getMidpoint(DoubleRange r) {
        double max = r.getMaximumDouble();
        double min = r.getMinimumDouble();
        double width = max - min;
        double mid = min + (width/2);
        return mid;
    }

    public static List<IRRatio> getNiFPRatios() {
        try {
            ArrayList<Region> nums = new ArrayList<Region>();
            parseRegions(AlloyNI, nums);

            return createRatios(nums, alloyNiDenom);
        } catch (Exception e) {
            Throwables.propagate(e);
            return null;
        }
    }

    public static List<IRRatio> getStainlessFPRatios() {
        try {
            ArrayList<Region> nums = new ArrayList<Region>();
            parseRegions(AlloyStainless, nums);

            return createRatios(nums, alloyStainlessDenom);
        } catch (Exception e) {
            Throwables.propagate(e);
            return null;
        }
    }

    public static List<IRRatio> getCuFPRatios() {
        try {
            ArrayList<Region> nums = new ArrayList<Region>();
            parseRegions(AlloyCu, nums);

            return createRatios(nums, alloyCuDenom);
        } catch (Exception e) {
            Throwables.propagate(e);
            return null;
        }
    }

    public static List<IRRatio> getAlFPRatios() {
        try {
            ArrayList<Region> nums = new ArrayList<Region>();
            parseRegions(AlloyAl, nums);

            return createRatios(nums, alloyAlDenom);
        } catch (Exception e) {
            Throwables.propagate(e);
            return null;
        }
    }

    public static List<IRRatio> getTiFPRatios() {
        try {
            ArrayList<Region> nums = new ArrayList<Region>();
            parseRegions(AlloyTi, nums);

            return createRatios(nums, alloyTiDenom);
        } catch (Exception e) {
            Throwables.propagate(e);
            return null;
        }
    }

    @Override
    List<IRRatio> getSpecilityRatios() {
        try {
            ArrayList<Region> numorator = new ArrayList<Region>();
            parseRegions(Speciality, numorator);

            Region denom = new Region();
            denom.wavelengthRange = numorator.get(0).wavelengthRange;
            denom.params.put("name", OneIntensityValue.class.getName());

            return createRatios(numorator, denom);
        } catch (Exception e) {
            Throwables.propagate(e);
            return null;
        }
    }

    @Override
    List<IRRatio> getFPRatios(String name) {
        if("Ni".equals(name)){
            return getNiFPRatios();
        } else if("Stainless".equals(name)){
            return getStainlessFPRatios();
        } else if("Cu".equals(name)){
            return getCuFPRatios();
        } else if("Al".equals(name)){
            return getAlFPRatios();
        } else if("Ti".equals(name)){
            return getTiFPRatios();
        } else {
            return null;
        }
    }

    @Override
    double[] getWeights(String name) {
        if("Ni".equals(name)){
            return AlloyNiFudge;
        } else if("Stainless".equals(name)){
            return AlloyStainlessFudge;
        } else if("Cu".equals(name)){
            return AlloyCuFudge;
        } else if("Al".equals(name)){
            return AlloyAlFudge;
        } else if("Ti".equals(name)){
            return AlloyTiFudge;
        } else {
            return null;
        }
    }
}
