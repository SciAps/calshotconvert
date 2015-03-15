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

public class Z100FPCreator extends LIBZFingerprintCreator {

    private static final String Speciality =        "395.80-396.45,259.70-260.15,273.83-274.15,371.80-372.15,334.65-335.17,341.15-341.70,324.50-325.10,338.00-338.55,519.80-521.50,544.50-549.00,257.90-258.20,344.75-345.07,345.15-345.50,348.80-349.10,350.05-350.40,359.17-359.60,277.20-277.50,281.90-282.13,310.72-310.97,339.80-340.10,278.50-280.00,293.15-293.50,402.85-403.73,550.30-551.50,552.70-554.00,405.70-406.20,407.70-408.25,409.85-410.30,405.30-406.30,283.80-284.20,317.25-317.77,239.85-240.25,263.50-263.73,308.90-309.67,310.80-311.30,326.50-326.93,410.90-411.35,437.60-438.08,440.30-441.00,400.70-401.00,334.00-334.70,480.50-482.00,339.00-339.40,343.60-344.00,349.43-349.80,429.03-429.70,429.70-430.45";

    private static final String AlloyNI =           "251.33-251.85,288.07-288.40,394.10-394.70,395.90-396.30,334.46-335.20,337.10-337.50,310.85-311.25,410.90-411.40,437.60-438.00,359.10-359.60,360.33-360.75,425.00-425.80,428.45-429.30,293.20-293.50,294.80-295.10,402.85-403.73,259.60-260.30,373.20-373.60,257.90-258.20,344.75-345.10,348.70-349.05,352.80-353.20,341.25-341.70,324.45-325.10,327.15-327.75,281.40-281.80,386.20-386.53,550.35-551.15,405.60-406.13,407.80-408.30,409.85-410.40,400.70-401.10";
    public static final double[] AlloyNiFudge = new double[]{0.2,0.2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0.01};

    private static final String AlloyStainless =    "251.33-251.85,288.07-288.40,394.10-394.70,395.90-396.30,334.46-335.20,337.10-337.50,310.85-311.25,410.90-411.40,437.60-438.00,359.10-359.60,293.05-293.45,294.75-295.10,402.85-403.73,257.90-258.20,344.75-345.10,345.10-345.55,348.70-349.10,352.80-353.20,350.05-350.45,341.15-341.70,324.45-325.10,327.15-327.75,281.40-281.80,386.20-386.53,550.35-551.15,309.20-309.55,405.60-406.13,407.80-408.30,409.85-410.40,400.70-401.10";
    public static final double[] AlloyStainlessFudge = new double[]{1,1,1,1,1,1,1,1,1,1,1,1,3,1,1,1,1,1,1,1,1,1,0.3,3,5,1,1,1,0.1,0.01};

    private static final String AlloyCu =           "312.65-313.30,394.10-394.75,395.75-396.45,251.33-251.90,287.90-288.45,425.10-426.00,428.55-429.40,259.25-259.65,293.10-293.50,294.70-295.15,402.85-403.73,259.70-260.20,273.83-274.20,371.80-372.15,257.95-258.20,340.42-340.65,341.15-341.85,349.05-349.55,351.25-351.80,352.20-352.85,334.00-334.95,480.50-482.00,338.00-338.45,283.80-284.20,303.15-303.55,317.15-317.77,405.30-406.15,306.50-306.95";
    public static final double[] AlloyCuFudge = new double[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,0.01,0.01,1,1,1,1,3,3,0.1,1,1,1,3,3};

    private static final String AlloyAl =           "608.50-611.30,278.50-280.90,284.80-285.50,382.50-384.30,251.20-252.20,287.80-288.50,334.65-335.17,335.75-336.35,337.05-337.40,425.10-426.00,427.00-427.95,428.60-429.40,293.20-293.50,294.70-295.10,402.85-403.73,259.60-260.15,341.15-341.70,349.00-349.50,350.70-351.70,352.25-352.65,361.70-362.20,324.40-325.10,327.00-327.75,334.00-334.75,480.70-482.00";
    public static final double[] AlloyAlFudge = new double[]{1,3,1,1,1,3,0.1,0.1,0.1,1,1,1,1,1,1,0.2,1,1,1,1,1,1,1,1,1};

    private static final String AlloyTi =           "259.70-260.15,273.83-274.15,281.50-281.70,283.80-284.08,303.15-303.55,310.85-311.25,326.60-326.90,410.90-411.40,437.60-438.15,339.10-339.30,343.55-344.00,349.50-349.80,357.65-358.00,359.23-359.43,360.30-360.80,424.90-426.00,427.00-427.95,394.10-394.55,395.90-396.55,405.70-406.13,407.90-408.30,409.85-410.30,317.25-317.77,379.70-380.00,386.20-386.50,550.35-551.15,552.80-554.00";
    public static final double[] AlloyTiFudge = new double[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};





    static Map<DoubleRange, Region> alloyNiDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyStainlessDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyCuDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyAlDenom = new HashMap<DoubleRange, Region>();

    static Map<DoubleRange, Region> alloyTiDenom = new HashMap<DoubleRange, Region>();


    static {

        alloyNiDenom.put(new DoubleRange(230, 297), new RegionBuilder("231.43-231.77").algo(SGolayIntensity.class).create());
        alloyNiDenom.put(new DoubleRange(297, 405), new RegionBuilder("352.17-352.80").algo(SGolayIntensity.class).create());
        alloyNiDenom.put(new DoubleRange(405, 670), new RegionBuilder("499.50-501.15").algo(SGolayIntensity.class).create());

        alloyStainlessDenom.put(new DoubleRange(230, 297), new RegionBuilder("259.65-260.30").algo(SGolayIntensity.class).create());
        alloyStainlessDenom.put(new DoubleRange(297, 405), new RegionBuilder("373.10-374.05").algo(SGolayIntensity.class).create());
        alloyStainlessDenom.put(new DoubleRange(405, 670), new RegionBuilder("437.50-438.90").algo(SGolayIntensity.class).create());

        alloyCuDenom.put(new DoubleRange(230, 297), new RegionBuilder("236.75-237.20").algo(SGolayIntensity.class).create());
        alloyCuDenom.put(new DoubleRange(297, 405), new RegionBuilder("324.40-325.30").algo(SGolayIntensity.class).create());
        alloyCuDenom.put(new DoubleRange(405, 670), new RegionBuilder("521.25-522.60").algo(SGolayIntensity.class).create());

        alloyAlDenom.put(new DoubleRange(230, 297), new RegionBuilder("236.20-238.00").algo(SGolayIntensity.class).create());
        alloyAlDenom.put(new DoubleRange(297, 405), new RegionBuilder("308.90-309.70").algo(SGolayIntensity.class).create());
        alloyAlDenom.put(new DoubleRange(405, 670), new RegionBuilder("498.60-502.30").algo(SGolayIntensity.class).create());

        alloyTiDenom.put(new DoubleRange(230, 297), new RegionBuilder("251.20-252.05").algo(SGolayIntensity.class).create());
        alloyTiDenom.put(new DoubleRange(297, 405), new RegionBuilder("334.50-335.35").algo(SGolayIntensity.class).create());
        alloyTiDenom.put(new DoubleRange(405, 670), new RegionBuilder("452.80-458.00").algo(SGolayIntensity.class).create());

    }

    private static void parseRegions(String str, List<Region> list) throws Exception {
        for(String regionStr : str.split(",")) {
            Region region = Region.parse(regionStr);
            region.params.put("name", SGolayIntensity.class.getName());
            list.add(region);
        }
    }

    private static void setIRDenom(List<Region> denominators, double specNm, Map<DoubleRange, Region> denomMap){
        for(Map.Entry<DoubleRange, Region> entry : denomMap.entrySet()) {
            if(entry.getKey().containsDouble(specNm)){
                denominators.add(entry.getValue());
                break;
            }
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

    public Z100FPCreator(CurveDataManager curveDataManager, EmpiricalCurvesManager empiricalCurvesManager) {
        super(curveDataManager, empiricalCurvesManager);
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
