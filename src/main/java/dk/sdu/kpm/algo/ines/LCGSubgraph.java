package dk.sdu.kpm.algo.ines;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.algo.glone.Subgraph;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.Result;

import java.util.*;


public class LCGSubgraph extends HashSet<GeneCluster> implements Result {

    private HashMap<String, GeneNode> visitedNodes = null;
    public int instances = 1;

    private volatile KPMSettings kpmSettings;
    
    public LCGSubgraph(KPMSettings settings) {
        super();
    	kpmSettings = settings;
    }
    /**
     *
     */
    private static final long serialVersionUID = 2527907415426148608L;

    @Override
    public void setInstances(int instances) {
        this.instances = instances;
    }

    @Override
    public int getInstances() {
        return instances;
    }

    @Override
    public int compareTo(Result arg0) {
        return -(new Integer(getFitness())).compareTo(arg0.getFitness());
    }

    public int getFitness() {
        int fitness = 0;

        for (GeneCluster node : this) {
            if (node.isValid()) {
                fitness += node.getWeight();
            } else {
                fitness++;
            }
        }

        return fitness;
    }

    public HashMap<String, GeneNode> getVisitedNodes() {
        if (visitedNodes != null) {
            return visitedNodes;
        }

        visitedNodes = new HashMap<String, GeneNode>();
        for (GeneCluster cluster : this) {
            for (GeneNode node : cluster.getNodesInCluster()) {
                visitedNodes.put(node.getNodeId(), node);
            }
        }

        return visitedNodes;
    }

    public double getAverageDiffExpressedCases2() {
        int totalNumberDifferentialExpressedCases = 0;
        int numNodes = 0;
        for (GeneCluster cluster : this) {
            for (GeneNode node : cluster.getNodesInCluster()) {
                numNodes++;
                totalNumberDifferentialExpressedCases += node.getAverageExpressedCases();
            }
        }

        return ((double) totalNumberDifferentialExpressedCases) / numNodes;
    }
    
    public double getAverageDiffExpressedCases() {
        double numNodes = (double)visitedNodes.size();
        Map<String, Integer> sumMap = new HashMap<String, Integer>();
        for (String expId: kpmSettings.NUM_CASES_MAP.keySet()) {
            sumMap.put(expId, 0);
        }
        for (GeneNode node : this.getVisitedNodes().values()) {
            for (String expId: kpmSettings.NUM_CASES_MAP.keySet()) {
                sumMap.put(expId, sumMap.get(expId) + node.getNumDiffExpressedCases(expId));
            }
        }
        
        double aux = 0.0;
        for (String expId: kpmSettings.NUM_CASES_MAP.keySet()) {
            aux += ((double)sumMap.get(expId) / numNodes) / (double) kpmSettings.NUM_CASES_MAP.get(expId);
        }

        return aux / (double) kpmSettings.NUM_CASES_MAP.size();
    }

    private double log2(double d) {
        return Math.log(d) / Math.log(2.0);
    }

    public double getInformationGainExpressed2() {


        if (this.size() < 2) {
            return 0.0;
        }
        int alphabetSize = 2;
        double bits = log2((double) alphabetSize);
        int nDatasets = kpmSettings.NUM_CASES_MAP.size();
        double avg = 0.0;
        for (String expId : kpmSettings.NUM_CASES_MAP.keySet()) {
            int nCases = kpmSettings.NUM_CASES_MAP.get(expId);
            int[][] frequencies = new int[alphabetSize][nCases];
            for (GeneCluster cluster : this) {
                for (GeneNode node : cluster.getNodesInCluster()) {
            
                    char[] expressionArray = node.getDifferenceArray(expId);
                    for (int j = 0; j < expressionArray.length; j++) {
                        char symbol = expressionArray[j];
                        if (symbol == GeneNode.NODIFFERENCE) {
                            frequencies[0][j]++;
                        } else {
                            frequencies[1][j]++;
                        }
                    }
                }
            }
            double totalColumnInfo = 0.0;
            for (int j = 0; j < nCases; j++) {
                double columnInfo = 0.0;
                for (int i = 0; i < alphabetSize; i++) {
                    if (frequencies[i][j] > 0) {
                        double aux = (double) frequencies[i][j] / (double) this.size();
                        columnInfo += (aux * log2(aux));
                    }
                }
                columnInfo += bits;
                totalColumnInfo += columnInfo;
            }
            avg += totalColumnInfo / (double) nCases;
        }

        return avg / (double) nDatasets;
    }

    public double getInformationGainExpressed() {
        LinkedList<GeneNode> nodeList = new LinkedList<GeneNode>(
                getVisitedNodes().values());

        if (nodeList.size() < 2) {
            return 0.0;
        }
        int alphabetSize = 2;

        double avg = 0.0;
        double studies = 0.0;
        GeneNode aNode = nodeList.getFirst();

        for (String expId : aNode.getDifferenceMap().keySet()) {
            studies++;
            int nCases = aNode.getNumCases(expId);
            int[][] frequencies = new int[alphabetSize][nCases];
            for (GeneNode node : nodeList) {
                char[] expressionArray = node.getDifferenceArray(expId);
                if(expressionArray == null){
                    continue;
                }

                for (int j = 0; j < expressionArray.length; j++) {
                    char symbol = expressionArray[j];
                    if (symbol == GeneNode.NODIFFERENCE) {
                        frequencies[0][j]++;
                    } else {
                        frequencies[1][j]++;
                    }
                }
            }

            double[] informationVector = new double[nCases];
            for (int j = 0; j < nCases; j++) {
                double sum = 0.0;
                for (int alph = 0; alph < alphabetSize; alph++) {
                    if (frequencies[alph][j] > 0) {
                        double aux = (double) nodeList.size();
                        sum += ((double) frequencies[alph][j] / aux)
                                * (Math.log(frequencies[alph][j] / aux) / Math.log(2.0));
                    }
                }
                informationVector[j] = (Math.log(alphabetSize) / Math.log(2)) + sum;
            }
            double averageInformation = 0.0;
            for (int j = 0; j < nCases; j++) {
                averageInformation += informationVector[j];
            }

            avg += (averageInformation / (double) nCases);
        }

        return avg / studies;
    }

    public int getNumExceptionNodes() {
        int numExcNodes = 0;
        for (GeneCluster node : this) {
            if (!node.isValid()) {
                numExcNodes++;
            }
        }
        return numExcNodes;
    }

    @Override
    public int getNonDifferentiallyExpressedCases() {
        List<GeneNode> visitedNodes = new ArrayList<GeneNode>(getVisitedNodes().values());

        int toReturn = 0;
        Collections.sort(visitedNodes);

        for (int i = kpmSettings.GENE_EXCEPTIONS; i < visitedNodes.size(); i++) {
            toReturn += visitedNodes.get(i).getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE);
        }

        return toReturn;
    }

    @Override
    public void flagExceptionNodes() {
        // Do nothing
    }

    @Override
    public boolean haveOverlap(Object o) {
        Set<String> currentSet = getVisitedNodes().keySet();
        Set<String> compareSet = ((Subgraph) o).getVisitedNodes().keySet();
        if (currentSet.size() <= compareSet.size()) {
            for (String nodeId : currentSet) {
                if (compareSet.contains(nodeId)) {
                    return true;
                }
            }
        } else {
            for (String nodeId : compareSet) {
                if (currentSet.contains(nodeId)) {
                    return true;
                }
            }
        }
        return false;
    }
}
