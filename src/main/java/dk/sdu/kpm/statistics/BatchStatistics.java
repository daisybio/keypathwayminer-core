package dk.sdu.kpm.statistics;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.charts.BoxplotChart;
import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.logging.KpmLogger;
import dk.sdu.kpm.results.IKPMResultItem;
import dk.sdu.kpm.results.IKPMResultSet;
import dk.sdu.kpm.runners.BatchRunWithPerturbationParameters;
import dk.sdu.kpm.runners.PerturbationResult;
import dk.sdu.kpm.validation.ValidationOverlapResult;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BatchStatistics implements IStatistics, Serializable{

    // For counting overlaps
    private ConcurrentHashMap<Integer, List<Double>> percentageOverlap;

    // For counting overlaps, fixed permutation percentage, varying K and L
    private ConcurrentHashMap<Integer, Map<Pair<Integer, Integer>, List<Double>>> fixedPercentOverlap;


    // For counting overlap with gold standards:
    //  Percent    Overlap
    private ConcurrentHashMap<Integer, List<Double>> goldOverlap;

    private Map<Integer, Map<Pair<Integer, Integer>, List<Double>>> fixedPercentGoldOverlap;

    // Name of the chart	the chart
    private ConcurrentHashMap<String, IChart> charts;

    private IKPMResultSet untouchedKPMResult;

    private List<PerturbationResult> perturbedResults;

    private BatchRunWithPerturbationParameters parameters;

    private String kpmID;

    public BatchStatistics(
            IKPMResultSet untouchedKPMResult,
            BatchRunWithPerturbationParameters parameters,
            String kpmID){
        this.percentageOverlap = new ConcurrentHashMap<Integer, List<Double>>();
        this.fixedPercentOverlap = new ConcurrentHashMap<Integer, Map<Pair<Integer, Integer>, List<Double>>>();
        this.goldOverlap = new ConcurrentHashMap<Integer, List<Double>>();
        this.fixedPercentGoldOverlap = new ConcurrentHashMap<Integer, Map<Pair<Integer, Integer>, List<Double>>>();
        this.charts = new ConcurrentHashMap<String, IChart>();
        this.untouchedKPMResult = untouchedKPMResult;
        this.parameters = parameters;
        this.perturbedResults = new ArrayList<PerturbationResult>();
        this.kpmID = kpmID;
    }

    public void calculate(List<PerturbationResult> results){        
        try{
            
            KPMSettings kpmSettings = this.untouchedKPMResult.getKpmSettings();
           
            this.perturbedResults = results;
            
            // Iterate through all of the results
            
            for(PerturbationResult pertRes : results){
                // Add the overlap counting list
                if(!percentageOverlap.containsKey(pertRes.PercentagePerturbed)){
                    percentageOverlap.put(pertRes.PercentagePerturbed, new ArrayList<Double>());
                }

                // add the current percentage
                if(!fixedPercentOverlap.containsKey(pertRes.PercentagePerturbed)){
                    fixedPercentOverlap.put(pertRes.PercentagePerturbed, new HashMap<Pair<Integer, Integer>, List<Double>>());
                }

                if(!fixedPercentGoldOverlap.containsKey(pertRes.PercentagePerturbed)){
                    fixedPercentGoldOverlap.put(pertRes.PercentagePerturbed, new HashMap<Pair<Integer, Integer>, List<Double>>());
                }

                // Iterate through all K/L combinations
                for(IKPMResultItem runResUnion : pertRes.Results){
                    Pair<Integer, Integer> kl = new Pair<Integer, Integer>(runResUnion.getK(), runResUnion.getL());

                    // If we have not seen it before, add the inner list
                    if(!fixedPercentOverlap.get(pertRes.PercentagePerturbed).containsKey(kl)){
                        fixedPercentOverlap.get(pertRes.PercentagePerturbed).put(kl, new ArrayList<Double>());
                    }


                    // Get the corresponding result of the original run
                    IKPMResultItem originalKPM = getResultsFromOriginalRun(runResUnion.getK(), runResUnion.getL());

                    Map<String, GeneNode> originalUnionNodeSet = new HashMap<String, GeneNode>();
                    if(originalKPM != null && originalKPM.getUnionNodeSet() != null){
                        originalUnionNodeSet = originalKPM.getUnionNodeSet();
                    }

                    Map<String, GeneNode> runresUnionNodeSet = new HashMap<String, GeneNode>();
                    if(runResUnion != null && runResUnion.getUnionNodeSet() != null){
                        runresUnionNodeSet = runResUnion.getUnionNodeSet();
                    }

                    // Compute the overlap between the two.
                    double overlap = jaccardCoefficientOverlap(originalUnionNodeSet, runresUnionNodeSet);

                    // Add the overlap count to the lists

                    // x-axis = permutation %, y-axis = overlap between results from standard run and perturbed graphs.
                    percentageOverlap.get(pertRes.PercentagePerturbed).add(overlap);

                    // fixed permutation percentage, y-axis = same as above. Now contains both K and L information
                    fixedPercentOverlap.get(pertRes.PercentagePerturbed).get(kl).add(overlap);
                }

                // if overlap is defined for the perturbed KPM run.
                if(pertRes.ValidationOverlapResults != null){
                    if(!goldOverlap.containsKey(pertRes.PercentagePerturbed)){
                        goldOverlap.put(pertRes.PercentagePerturbed, new ArrayList<Double>());
                    }

                    for(ValidationOverlapResult res : pertRes.ValidationOverlapResults){
                        // Add the percentage overlap
                        goldOverlap.get(pertRes.PercentagePerturbed).add(res.percentageOverlap);


                        Pair<Integer, Integer> kl = new Pair<Integer, Integer>(res.K, res.L);

                        if(!fixedPercentGoldOverlap.get(pertRes.PercentagePerturbed).containsKey(kl)){
                            fixedPercentGoldOverlap.get(pertRes.PercentagePerturbed).put(kl, new ArrayList<Double>());
                        }

                        this.fixedPercentGoldOverlap.get(pertRes.PercentagePerturbed).get(kl).add(res.percentageOverlap);
                    }
                }
            }
            

            //----------------------------------------------

            // if overlap is defined for the untouched KPM run.
            if(this.untouchedKPMResult.getOverlapResults() != null){
                if(!goldOverlap.containsKey(0)){
                    goldOverlap.put(0, new ArrayList<Double>());
                }

                for(ValidationOverlapResult res : this.untouchedKPMResult.getOverlapResults()){
                    goldOverlap.get(0).add(res.percentageOverlap);
                }
            }

            // fixing K and L respectively
            String var1 = "K";
            if (!kpmSettings.USE_INES) {
                var1 = "";
            }
            String var2 = "L";
            if (kpmSettings.VARYING_L_ID.size() == 1) {
                var2 = kpmSettings.VARYING_L_ID.get(0);
            } else if (kpmSettings.VARYING_L_ID.size() == 2) {
                var1 = kpmSettings.VARYING_L_ID.get(1);
                var2 = kpmSettings.VARYING_L_ID.get(0);
            }
            for(Integer percentagePertubed : fixedPercentOverlap.keySet()) {
                HashSet<Integer> kValues = new HashSet<Integer>();
                HashSet<Integer> lValues = new HashSet<Integer>();
                for (Pair<Integer, Integer> kl : fixedPercentOverlap.get(percentagePertubed).keySet()) {
                    if (!kValues.contains(kl.getK())) {
                        kValues.add(kl.getK());
                    }

                    if (!lValues.contains(kl.getL())) {
                        lValues.add(kl.getL());
                    }
                }
                for (Integer k : kValues) {
                    
                    HashMap<Integer, List<Double>> fixedLOverlap = new HashMap<Integer, List<Double>>();
                    for (Pair<Integer, Integer> kl : fixedPercentOverlap.get(percentagePertubed).keySet()) {
                        if (!kl.getK().equals(k)) {
                            continue; // only look at the current K
                        }
                        fixedLOverlap.put(kl.getL(), fixedPercentOverlap.get(percentagePertubed).get(kl));
                    }

        
                    String pofTitle = String.format("Perturbation level %d%%, " +  var1 + " = %d", percentagePertubed, k);
                    if (!kpmSettings.USE_INES) {
                        if (kpmSettings.VARYING_L_ID.size() == 1) {
                            pofTitle = String.format("Perturbation level %d%%", percentagePertubed);
                        } else if (kpmSettings.VARYING_L_ID.size() == 3) {
                            pofTitle = String.format("Perturbation level %d%%", percentagePertubed);
                        }

                    } else if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                        pofTitle = String.format("Perturbation level %d%%, " +  var1 + " = %d", percentagePertubed, k);
                    }
                    BoxplotChart bpc = new BoxplotChart(pofTitle, "", "Jaccard overlap with unperturbed results");
                    ArrayList<Integer> Ls = new ArrayList<Integer>(fixedLOverlap.keySet());
                    Collections.sort(Ls);
                    for(int j = 0; j < Ls.size(); j++){
                        Integer L = Ls.get(j);
                        List<Double> overlapList = fixedLOverlap.get(L);
                        String legend = String.format(var2 + " = %d", L);
                        if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                            legend = String.format(var2 + " = %d%%", L);
                        }                 
                        bpc.addDataset("", legend, overlapList);
                    }

                    bpc.getTags().add(IChart.TagEnum.PERTURB_LEVEL_FIXED);
                    bpc.getTags().add(IChart.TagEnum.K_FIXED);
                    bpc.getTags().add(IChart.TagEnum.ORIGINAL_RESULT);
                    bpc.setSortName(String.format("%05d", percentagePertubed));
                    bpc.setSortTitle(String.format("Perturbation level %05d%%, " + var1 + " = %05d", percentagePertubed, k));

                    charts.put(pofTitle, bpc);
                }

                for (Integer l : lValues) {
                    HashMap<Integer, List<Double>> fixedKOverlap = new HashMap<Integer, List<Double>>();
                    for (Pair<Integer, Integer> kl : fixedPercentOverlap.get(percentagePertubed).keySet()) {
                        if (!kl.getL().equals(l)) {
                            continue; // only look at the current L
                        }

                        fixedKOverlap.put(kl.getK(), fixedPercentOverlap.get(percentagePertubed).get(kl));
                    }

                    String pofTitle = String.format("Perturbation level %d%%, " + var2 + " = %d", percentagePertubed, l);
                    if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                        pofTitle = String.format("Perturbation level %d%%, " + var2 + " = %d%%", percentagePertubed, l);
                    } else if (kpmSettings.VARYING_L_ID.isEmpty()) {
                        pofTitle = String.format("Perturbation level %d%%", percentagePertubed);
                    }
                
                    BoxplotChart bpc = new BoxplotChart(pofTitle, "", "Jaccard overlap with unperturbed results");
                    ArrayList<Integer> Ks = new ArrayList<Integer>(fixedKOverlap.keySet());
                    Collections.sort(Ks);
                    for(int j = 0; j < Ks.size(); j++){
                        Integer K = Ks.get(j);
                        List<Double> overlapList = fixedKOverlap.get(K);
                        bpc.addDataset("", String.format(var1 + " = %d", K), overlapList);
                    }

                    bpc.getTags().add(IChart.TagEnum.PERTURB_LEVEL_FIXED);
                    bpc.getTags().add(IChart.TagEnum.L_FIXED);
                    bpc.getTags().add(IChart.TagEnum.ORIGINAL_RESULT);
                    bpc.setSortName(String.format("%05d", percentagePertubed));
                    bpc.setSortTitle(String.format("Perturbation level %05d%%, " + var2 + " = %05d", percentagePertubed, l));

                    charts.put(pofTitle, bpc);
                }
            }

            // Varying perturbation:
            HashMap<Pair<Integer, Integer>, HashMap<Integer, List<Double>>> klpMap = new HashMap<Pair<Integer, Integer>, HashMap<Integer, List<Double>>>();
            for(Integer percentagePertubed : fixedPercentOverlap.keySet()) {
                for (Pair<Integer, Integer> kl : fixedPercentOverlap.get(percentagePertubed).keySet()) {
                    if(!klpMap.containsKey(kl)){
                        klpMap.put(kl, new HashMap<Integer, List<Double>>());
                    }

                    klpMap.get(kl).put(percentagePertubed, fixedPercentOverlap.get(percentagePertubed).get(kl));
                }
            }

            for(Pair<Integer, Integer> kl : klpMap.keySet()){
                 String pofTitle = String.format(var1 + " = %d, " + var2 + " = %d", kl.getK(), kl.getL());
                if (!kpmSettings.USE_INES) {
                    if (kpmSettings.VARYING_L_ID.size() == 1) {
                        pofTitle = String.format(var2 + " = %d", kl.getL());
                    } else if (kpmSettings.VARYING_L_ID.size() == 3) {
                        pofTitle = String.format(var2 + " = %d%%", kl.getL());
                    } else if (kpmSettings.VARYING_L_ID.size() == 2) {
                        pofTitle = String.format(var1 + " = %d, " + var2 + " = %d", kl.getK(), kl.getL());
                    } else {
                        pofTitle = "";
                    }

                } else if (kpmSettings.VARYING_L_ID.isEmpty()) {
                    pofTitle = "";
                } else if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                    pofTitle = String.format(var1 + " = %d, " + var2 + " = %d%%", kl.getK(), kl.getL());
                }
                BoxplotChart bpc = new BoxplotChart(pofTitle, "", "Jaccard overlap with unperturbed results");
                ArrayList<Integer> perturbationLevels = new ArrayList<Integer>(klpMap.get(kl).keySet());
                Collections.sort(perturbationLevels);
                for(int j = 0; j < perturbationLevels.size(); j++){
                    Integer perturbationLevel = perturbationLevels.get(j);
                    List<Double> overlapList = klpMap.get(kl).get(perturbationLevel);
                    bpc.addDataset("", String.format("pert. = %d%%", perturbationLevel), overlapList);
                }

                bpc.getTags().add(IChart.TagEnum.K_FIXED);
                bpc.getTags().add(IChart.TagEnum.L_FIXED);
                bpc.getTags().add(IChart.TagEnum.ORIGINAL_RESULT);
                bpc.setSortName(String.format("%05d", kl.getK()));
                bpc.setSortTitle(String.format(var1 + " = %05d, " + var2 + " = %05d", kl.getK(), kl.getL()));

                charts.put(pofTitle, bpc);
            }

            HashMap<Pair<Integer, Integer>, HashMap<Integer, List<Double>>> klpGoldMap = new HashMap<Pair<Integer, Integer>, HashMap<Integer, List<Double>>>();
            for(Integer percentagePertubed : fixedPercentGoldOverlap.keySet()) {
                for (Pair<Integer, Integer> kl : fixedPercentGoldOverlap.get(percentagePertubed).keySet()) {
                             
                    if(!klpGoldMap.containsKey(kl)){
                        klpGoldMap.put(kl, new HashMap<Integer, List<Double>>());
                    }

                    klpGoldMap.get(kl).put(percentagePertubed, fixedPercentGoldOverlap.get(percentagePertubed).get(kl));
                }
            }

            for(Pair<Integer, Integer> kl : klpGoldMap.keySet()){
                
                 String pofTitle = String.format(var1 + " = %d, " + var2 + " = %d (validation)", kl.getK(), kl.getL());
                if (!kpmSettings.USE_INES) {
                    if (kpmSettings.VARYING_L_ID.size() == 1) {
                        pofTitle = String.format(var2 + " = %d (validation)", kl.getL());
                    } else if (kpmSettings.VARYING_L_ID.size() == 3) {
                        pofTitle = String.format(var2 + " = %d%% (validation)", kl.getL());
                    } else if (kpmSettings.VARYING_L_ID.size() == 2) {
                        pofTitle = String.format(var1 + " = %d, " + var2 + " = %d (validation)", kl.getK(), kl.getL());
                    } else {
                        pofTitle = "(validation)";
                    }


                } else if (kpmSettings.VARYING_L_ID.isEmpty()) {
                    pofTitle = "(validation)";
                } else if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                    pofTitle = String.format(var1 + " = %d, " + var2 + " = %d%% (validation)", kl.getK(), kl.getL());
                }
                BoxplotChart bpc = new BoxplotChart(pofTitle, "", "Jaccard overlap with gold standard");
                ArrayList<Integer> perturbationLevels = new ArrayList<Integer>(klpGoldMap.get(kl).keySet());
                Collections.sort(perturbationLevels);
                for(int j = 0; j < perturbationLevels.size(); j++){
                    Integer perturbationLevel = perturbationLevels.get(j);
                    List<Double> overlapList = klpGoldMap.get(kl).get(perturbationLevel);
                    bpc.addDataset("", String.format("pert. = %d%%", perturbationLevel), overlapList);
                }

                bpc.getTags().add(IChart.TagEnum.K_FIXED);
                bpc.getTags().add(IChart.TagEnum.L_FIXED);
                bpc.getTags().add(IChart.TagEnum.GOLD_STANDARD);
                bpc.setSortName(String.format("%05d", kl.getK()));
                bpc.setSortTitle(String.format(var1 + " = %05d, " + var2 + " = %05d", kl.getK(), kl.getL()));
                if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                    bpc.setSortTitle(String.format(var1 + " = %05d, " + var2 + " = %05d%%", kl.getK(), kl.getL()));
                }
                charts.put(pofTitle, bpc);
            }

            // gold standard fixing K and L respectively
            for(Integer percentagePertubed : fixedPercentGoldOverlap.keySet()) {
                HashSet<Integer> kValues = new HashSet<Integer>();
                HashSet<Integer> lValues = new HashSet<Integer>();
                for (Pair<Integer, Integer> kl : fixedPercentGoldOverlap.get(percentagePertubed).keySet()) {
                    if (!kValues.contains(kl.getK())) {
                        kValues.add(kl.getK());
                    }

                    if (!lValues.contains(kl.getL())) {
                        lValues.add(kl.getL());
                    }
                }

                for (Integer k : kValues) {
                    HashMap<Integer, List<Double>> fixedLOverlap = new HashMap<Integer, List<Double>>();
                    for (Pair<Integer, Integer> kl : fixedPercentGoldOverlap.get(percentagePertubed).keySet()) {
                        if (!kl.getK().equals(k)) {
                            continue; // only look at the current K
                        }

                        fixedLOverlap.put(kl.getL(), fixedPercentGoldOverlap.get(percentagePertubed).get(kl));
                    }
                    String pofTitle = String.format("Perturbation level %d%%, " +  var1 + " = %d (validation)", percentagePertubed, k);
                    if (!kpmSettings.USE_INES) {
                        if (kpmSettings.VARYING_L_ID.size() == 1) {
                            pofTitle = String.format("Perturbation level %d%% (validation)", percentagePertubed);
                        } else if (kpmSettings.VARYING_L_ID.size() == 3) {
                            pofTitle = String.format("Perturbation level %d%% (validation)", percentagePertubed);
                        }

                    } else if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                        pofTitle = String.format("Perturbation level %d%%, " +  var1 + " = %d (validation)", percentagePertubed, k);
                    }
                    
                    BoxplotChart bpc = new BoxplotChart(pofTitle, "", "Jaccard overlap with gold standard");
                    ArrayList<Integer> Ls = new ArrayList<Integer>(fixedLOverlap.keySet());
                    Collections.sort(Ls);
                    for(int j = 0; j < Ls.size(); j++){
                        Integer L = Ls.get(j);
                        List<Double> overlapList = fixedLOverlap.get(L);
                        String legend = String.format(var2 + " = %d", L);
                        if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                            legend = String.format(var2 + " = %d%%", L);                        
                        }
                        bpc.addDataset("", legend , overlapList);
                    }

                    bpc.getTags().add(IChart.TagEnum.PERTURB_LEVEL_FIXED);
                    bpc.getTags().add(IChart.TagEnum.K_FIXED);
                    bpc.getTags().add(IChart.TagEnum.GOLD_STANDARD);
                    bpc.setSortName(String.format("%05d", percentagePertubed));
                    bpc.setSortTitle(String.format("Perturbation level%05d%%, " + var1 + " = %05d (validation)", percentagePertubed, k));

                    charts.put(pofTitle, bpc);
                }

                for (Integer l : lValues) {
                    HashMap<Integer, List<Double>> fixedKOverlap = new HashMap<Integer, List<Double>>();
                    for (Pair<Integer, Integer> kl : fixedPercentGoldOverlap.get(percentagePertubed).keySet()) {
                        if (!kl.getL().equals(l)) {
                            continue; // only look at the current L
                        }

                        fixedKOverlap.put(kl.getK(), fixedPercentGoldOverlap.get(percentagePertubed).get(kl));
                    }

                    String pofTitle = String.format("Perturbation level %d%%, " + var2 + " = %d (validation)", percentagePertubed, l);
                    if (kpmSettings.CALCULATE_ONLY_SAME_L_VALUES) {
                        pofTitle = String.format("Perturbation level %d%%, " + var2 + " = %d%% (validation)", percentagePertubed, l);
                    } else if (kpmSettings.VARYING_L_ID.isEmpty()) {
                        pofTitle = String.format("Perturbation level %d%% (validation)", percentagePertubed);
                    }
                    BoxplotChart bpc = new BoxplotChart(pofTitle, "", "Jaccard overlap with gold standard");
                    ArrayList<Integer> Ks = new ArrayList<Integer>(fixedKOverlap.keySet());
                    Collections.sort(Ks);
                    for(int j = 0; j < Ks.size(); j++){
                        Integer K = Ks.get(j);
                        List<Double> overlapList = fixedKOverlap.get(K);
                        bpc.addDataset("", String.format(var1 + " = %d", K), overlapList);
                    }

                    bpc.getTags().add(IChart.TagEnum.PERTURB_LEVEL_FIXED);
                    bpc.getTags().add(IChart.TagEnum.L_FIXED);
                    bpc.getTags().add(IChart.TagEnum.GOLD_STANDARD);
                    bpc.setSortName(String.format("%05d", percentagePertubed));
                    bpc.setSortTitle(String.format("Perturbation level %05d%%, L = %05d (validation)", percentagePertubed, l));

                    charts.put(pofTitle, bpc);
                }
            }

        }catch(Exception e){
            System.out.println("[BatchStatistics] There was an error somewhere: \n" + e.getMessage());
            KpmLogger.log(Level.SEVERE, e);
        }

    }

    public void addChart(String title, IChart chart){
        if(!this.charts.containsKey(title)){
            this.charts.put(title, chart);
        }
    }

    /**
     * Instead of just reporting the absolute overlap (i.e. the size of the intersection), to make all values comparable we need
     * to compute other overlapping measures instead.  Suppose A is the set of genes in the pathways from a normal run,
     * B the genes of the pathway from a perturbed graph run and G genes from the gold standard set. 
     * - Robustness runs:  Jaccard coefficient =  | A intersection B | / | A union B |
     *
     * @param A
     * @param B
     * @return
     */
    private double jaccardCoefficientOverlap(Map<String, GeneNode> A, Map<String, GeneNode> B){
        HashSet<String> union = new HashSet<String>();
        HashSet<String> intersection = new HashSet<String>();

        // Calculate intersection for both, and add to the union for the outer
        for(String nodeID : A.keySet()){
            if(B.containsKey(nodeID)){
                intersection.add(nodeID);
            }

            union.add(nodeID);
        }

        // Add to the union for the inner.
        for(String nodeID : B.keySet()){
            if(!union.contains(nodeID)){
                union.add(nodeID);
            }
        }

        double intersectionSize = (double) intersection.size();
        double unionSize = (double) union.size();
        double jaccard = intersectionSize/unionSize;

        return jaccard;
    }

    private IKPMResultItem getResultsFromOriginalRun(int k, int l){
        for(IKPMResultItem res : this.untouchedKPMResult.getResults()){
            if(res.getK() == k && res.getL() == l){
                return res;
            }
        }

        return null;
    }

    @Override
    public ConcurrentHashMap<String, IChart> getCharts() {
        return this.charts;
    }

    @Override
    public ConcurrentHashMap<Integer, List<Double>> getPercentageOverlap() {
        return this.percentageOverlap;
    }

    @Override
    public ConcurrentHashMap<Integer, Map<Pair<Integer, Integer>, List<Double>>> getFixedPercentOverlap() {
        return this.fixedPercentOverlap;
    }

    @Override
    public ConcurrentHashMap<Integer, List<Double>> getGoldOverlap() {
        return this.goldOverlap;
    }

    @Override
    public Map<Integer, Map<Pair<Integer, Integer>, List<Double>>> getFixedPercentGoldOverlap() {
        return this.fixedPercentGoldOverlap;
    }

    @Override
    public String getKpmID() {
        return this.kpmID;
    }
}
