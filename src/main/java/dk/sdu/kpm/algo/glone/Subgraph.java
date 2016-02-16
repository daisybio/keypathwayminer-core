package dk.sdu.kpm.algo.glone;

import dk.sdu.kpm.Combine;
import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;

import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.*;


public class Subgraph extends TreeSet<GeneNode> implements Result, Serializable {

    private volatile KPMSettings kpmSettings;
    
    /**
     *
     */
    private static final long serialVersionUID = 4417208787305934006L;
    private HashMap<String, GeneNode> visitedNodes = null;
    private GeneNode lastExceptionNode = null;
    private int nonDifferentiallyExpressedCases = 0;
    private Map<String, Integer> nonDifferentiallyExpressedCasesMap;
    public int instances = 1;

    public Subgraph(KPMSettings settings) {
    	kpmSettings = settings;
    	
        nonDifferentiallyExpressedCasesMap = new HashMap<String, Integer>();
        for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
            nonDifferentiallyExpressedCasesMap.put(expId, 0);
        }
    }

    @Override
    public int compareTo(Result o) {
        // inconsistency with .equals... only important when using treeset
        // though
        int res = -(new Integer(getInstances())).compareTo(new Integer(o.getInstances()));
        if (res == 0) {
            res = -(new Integer(getFitness())).compareTo(new Integer(o.getFitness()));
        } else {
            return res;
        }
        if (res == 0) {
            res = (new Integer(getNonDifferentiallyExpressedCases()).compareTo(new Integer(o.getNonDifferentiallyExpressedCases())));
        } else {
            return res;
        }
        if (res == 0) {
            res = -(new Double(getInformationGainExpressed()).compareTo(new Double(o.getInformationGainExpressed())));
        }
        return res;
    }
//       @Override 
//       public int compareTo(Result o) {
//           return -(new Integer(getFitness())).compareTo(new Integer(o.getFitness()));
//       }

    @Override
    public void setInstances(int instances) {
        this.instances = instances;
    }

    @Override
    public int getInstances() {
        return instances;
    }

    @Override
    public Map<String, GeneNode> getVisitedNodes() {
        if (visitedNodes != null) {
            return visitedNodes;
        }

        visitedNodes = new HashMap<String, GeneNode>();
        for (GeneNode node : this) {
            visitedNodes.put(node.getNodeId(), node);
        }

        return visitedNodes;
    }

    /**
     * Determines if the given node can still be added to this result without
     * crossing the maximal non-differential expressed cases allowed, as given
     * by KPMParameters.GENE_EXCEPTIONS and KPMParameters.CASE_EXCEPTIONS
     *
     * @param node
     * @return
     */
    public boolean canAdd(GeneNode node) {

        if (lastExceptionNode == null && kpmSettings.GENE_EXCEPTIONS > 0) {
            return true;
        } else if (lastExceptionNode == null) {
            return checkCondition(node);
        } else {

            boolean newIsBetter = true;
            for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                if (node.getNumNoDiffExpressedCases(expId) > lastExceptionNode.getNumNoDiffExpressedCases(expId)) {
                    newIsBetter = false;
                }
            }

            if (newIsBetter) {
               return checkCondition(node);
            } else {
               return checkCondition(lastExceptionNode);
            }
        }
    }
    
    private boolean checkCondition(GeneNode node) {
        if (kpmSettings.COMBINE_OPERATOR == Combine.OR) {
            for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                if (getNonDifferentiallyExpressedCasesMap().get(expId)
                        + node.getNumNoDiffExpressedCases(expId) <= kpmSettings.CASE_EXCEPTIONS_MAP.get(expId)) {
                    return true;
                }
            }
            return false;
        } else if (kpmSettings.COMBINE_OPERATOR == Combine.AND) {
            for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                if (getNonDifferentiallyExpressedCasesMap().get(expId)
                        + node.getNumNoDiffExpressedCases(expId) > kpmSettings.CASE_EXCEPTIONS_MAP.get(expId)) {
                    return false;
                }
            }
            return true;
            
        } else if (kpmSettings.COMBINE_OPERATOR == Combine.CUSTOM) {
            Map<String, Object> context = new HashMap<String, Object>();
            for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                boolean aux = getNonDifferentiallyExpressedCasesMap().get(expId)
                        + node.getNumNoDiffExpressedCases(expId) 
                        <= kpmSettings.CASE_EXCEPTIONS_MAP.get(expId);
                context.put(expId, aux);
            }
            return MVEL.evalToBoolean(kpmSettings.COMBINE_FORMULA, context);
           
        }
        
        return false;
    }

    @Override
    public boolean addAll(java.util.Collection<? extends GeneNode> c) {
        if (size() != 0 || !(c instanceof Subgraph)) {
            throw new IllegalStateException(
                    "This method is only available for cloning. It messes up the internal state otherwise.");
        }

        boolean toReturn = super.addAll(c);
        lastExceptionNode = ((Subgraph) c).lastExceptionNode;
        nonDifferentiallyExpressedCases = ((Subgraph) c).nonDifferentiallyExpressedCases;
        nonDifferentiallyExpressedCasesMap = ((Subgraph) c).nonDifferentiallyExpressedCasesMap;

        return toReturn;
    }

    @Override
    public boolean add(GeneNode e) {
        if (!canAdd(e)) {
            throw new IllegalArgumentException("Cannot add e to solution.");
        }

        if (super.add(e)) {
            if (kpmSettings.GENE_EXCEPTIONS == 0) {
                for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                    nonDifferentiallyExpressedCasesMap.put(expId,
                            nonDifferentiallyExpressedCasesMap.get(expId) + e.getNumNoDiffExpressedCases(expId));
                }
            } else if (lastExceptionNode == null
                    && size() < kpmSettings.GENE_EXCEPTIONS) {
                ; // do nothing since we are adding exception nodes
            } else if (lastExceptionNode == null
                    && size() == kpmSettings.GENE_EXCEPTIONS) {
                lastExceptionNode = last();
            } else if (lastExceptionNode == null) {
                throw new IllegalStateException(
                        "We have more nodes in this cluster than allowed exception nodes, but we do not know what the exception nodes are");
            } else if (e.getAverageNonExpressedCases() <= lastExceptionNode.getAverageNonExpressedCases()) {
                for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                    nonDifferentiallyExpressedCasesMap.put(expId,
                            nonDifferentiallyExpressedCasesMap.get(expId) + e.getNumNoDiffExpressedCases(expId));
                }


                if (e.compareTo(lastExceptionNode) < 0) {
                    lastExceptionNode = lower(lastExceptionNode);
                }
                // only happens when 2 nodes with the same penalty are added

            } else {
                for (String expId : kpmSettings.CASE_EXCEPTIONS_MAP.keySet()) {
                    nonDifferentiallyExpressedCasesMap.put(expId,
                            nonDifferentiallyExpressedCasesMap.get(expId) + lastExceptionNode.getNumNoDiffExpressedCases(expId));
                }
                // there is a new lastExceptionNode...
                lastExceptionNode = lower(lastExceptionNode);
            }
            return true;
        } else {
            return false;
        }

    }

    ;

	@Override
    public boolean remove(Object o) {
        throw new IllegalStateException(
                "Removal does screw up the internal state. Not supported.");
    }

    public double getAverageDiffExpressedCases() {
        double numNodes = (double)visitedNodes.size();
        Map<String, Integer> sumMap = new HashMap<String, Integer>();
        for (String expId: kpmSettings.NUM_CASES_MAP.keySet()) {
            sumMap.put(expId, 0);
        }
        for (GeneNode node : this) {
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

    /**
     * Relict from older implementation -- only needed for final statistics
     * printout.
     *
     * @return I have no clue what this returns
     */
    public double getInformationGainExpressed2() {
        LinkedList<GeneNode> nodeList = new LinkedList<GeneNode>(
                getVisitedNodes().values());

        if (nodeList.size() < 2) {
            return 0.0;
        }
        int alphabetSize = 2;

        double avg = 0.0;
        double studies = 0.0;
        

        for (String expId : kpmSettings.NUM_CASES_MAP.keySet()) {
            studies++;
            int nCases = kpmSettings.NUM_CASES_MAP.get(expId);
            int[][] frequencies = new int[alphabetSize][nCases];
            for (GeneNode node : nodeList) {
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
    
    private double log2(double d) {
        return Math.log(d) / Math.log(2.0);
    }
    
    public double getInformationGainExpressed() {

        
        if (this.size() < 2) {
            return 0.0;
        }
        int alphabetSize = 2;
        double bits = log2((double)alphabetSize);
        int nDatasets = kpmSettings.NUM_CASES_MAP.size();
        double avg = 0.0;
        for (String expId : kpmSettings.NUM_CASES_MAP.keySet()) {
            int nCases = kpmSettings.NUM_CASES_MAP.get(expId);
            int[][] frequencies = new int[alphabetSize][nCases];
            for (GeneNode node : this) {
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
            double totalColumnInfo = 0.0;
            for (int j = 0; j < nCases; j++) {
                double columnInfo = 0.0;
                for (int i = 0; i < alphabetSize; i++) {
                    if (frequencies[i][j] > 0) {
                        double aux = (double)frequencies[i][j] / (double)this.size();
                        columnInfo += (aux * log2(aux));
                    }                    
                }
                columnInfo += bits;
                totalColumnInfo += columnInfo;
            }
            avg += totalColumnInfo / (double)nCases;            
        }
               
        return avg / (double)nDatasets;
    }

    @Override
    public int getFitness() {
        return size();
    }

    @Override
    public int getNumExceptionNodes() {
        return Math.min(size(), kpmSettings.GENE_EXCEPTIONS);
    }

    /**
     * Return the total number of non-differentially expressed cases; which is
     * the sum over all of those cases of the nodes in this subgraph. This works
     * kinda to tell how "bad" a solution is (as opposed to the number of
     * exception nodes in the simpler model).
     *
     * Pay attention: This method does NOT exclude the k most expensive nodes.
     *
     * @return
     */
    public int getTotalNonDifferentiallyExpressedCases() {
        int toReturn = 0;

        for (GeneNode n : this) {
            toReturn += n.getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE);
        }

        return toReturn;
    }

    public int getTotalNonDifferentiallyExpressedCasesByExp(String expId) {
        int toReturn = 0;

        for (GeneNode n : this) {
            toReturn += n.getNumNoDiffExpressedCases(expId);
        }

        return toReturn;
    }

    /**
     * Return the total number of non-differentially expressed cases; which is
     * the sum over all of those cases of the nodes in this subgraph. This works
     * kinda to tell how "bad" a solution is (as opposed to the number of
     * exception nodes in the simpler model).
     *
     * Pay attention: This method excludes the k most expensive nodes.
     *
     * @return
     */
    public int getNonDifferentiallyExpressedCases() {

        assert reevaluateNonDifferentiallyExpressedCases() == nonDifferentiallyExpressedCases;

        return nonDifferentiallyExpressedCases;

    }

    public Map<String, Integer> getNonDifferentiallyExpressedCasesMap() {
        return nonDifferentiallyExpressedCasesMap;
    }

    private int reevaluateNonDifferentiallyExpressedCases() {
        int toReturn = 0;
        int exceptions = kpmSettings.GENE_EXCEPTIONS;

        for (GeneNode n : this) {
            if (exceptions != 0) {
                exceptions--;
            } else {
                toReturn += n.getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE);
            }
        }

        return toReturn;
    }

    /**
     * Flags the k most costly nodes as exception nodes.
     */
    public void flagExceptionNodes() {
        int exceptions = kpmSettings.GENE_EXCEPTIONS;

        for (GeneNode n : this) {
            if (exceptions-- > 0) {
                n.setIsValid(false);
            } else {
                n.setIsValid(true);
            }
        }
    }

    /**
     * @return an array of all non-differentially expressed cases. E.g., the
     * solution contains 3 nodes with 3, 7 and 10 non-differentially expressed
     * cases each, the array returns [10, 7, 3]. Is sorted from biggest to
     * smallest.
     *
     */
    public List<Integer> getCasesArray() {
        List<Integer> casesArray = new ArrayList<Integer>();
        for (GeneNode n : this) {
            casesArray.add(n.getHeuristicValue(kpmSettings.NODE_HEURISTIC_VALUE));
        }

        return casesArray;
    }

    /**
     * Calculates the total non-differentially expressed cases from a given List
     * of cases. Ignores the k most costly elements, as specified by
     * KPMParameters.GENE_EXCEPTIONS.
     *
     * @param casesArray the array of non-differentially expressed cases, one
     * entry represents one node. Has to be sroted form biggest to smallest.
     * @return
     */
    public static int getNonDifferentiallyExpressedCasesFromList(
            List<Integer> casesArray, KPMSettings kpmSettings) {
        int toReturn = 0;
        int exceptions = kpmSettings.GENE_EXCEPTIONS;

        for (int n : casesArray) {
            if (exceptions != 0) {
                exceptions--;
            } else {
                toReturn += n;
            }
        }

        return toReturn;
    }

    /**
     * Checks whether this subgraph of graph g is connected using BFS
     *
     * @return
     */
    public boolean isConnected(KPMGraph g) {
        Set<GeneNode> visitedNodes = new HashSet<GeneNode>();
        Set<GeneNode> uncheckedNodes = new HashSet<GeneNode>();
        GeneNode start = iterator().next();
        visitedNodes.add(start);
        uncheckedNodes.add(start);

        while (visitedNodes.size() < size() && uncheckedNodes.size() > 0) {
            Set<GeneNode> newUncheckedNodes = new HashSet<GeneNode>();
            for (GeneNode unchecked : uncheckedNodes) {
                for (GeneNode neighbor : g.getNeighbors(unchecked)) {
                    if (contains(neighbor) && !visitedNodes.contains(neighbor)) {
                        visitedNodes.add(neighbor);
                        newUncheckedNodes.add(neighbor);
                    }
                }
            }
            uncheckedNodes = newUncheckedNodes;
        }

        return visitedNodes.size() == size();
    }

    public void updateNeighbors(Set<GeneNode> currentNeighbors,
            GeneNode newNode, KPMGraph g) {
        currentNeighbors.remove(newNode);
        for (GeneNode node : g.getNeighbors(newNode)) {
            if (!contains(node)) {
                currentNeighbors.add(node);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        Set<String> currentSet = getVisitedNodes().keySet();
        Set<String> compareSet = ((Subgraph) o).getVisitedNodes().keySet();
        if (currentSet.size() != compareSet.size()) {
            return false;
        }
        return currentSet.containsAll(compareSet);
    }

    public boolean isSubsetOf(Object o) {
        Set<String> currentSet = getVisitedNodes().keySet();
        Set<String> compareSet = ((Subgraph) o).getVisitedNodes().keySet();
        if (currentSet.size() > compareSet.size()) {
            return false;
        }
        return compareSet.containsAll(currentSet);
    }

    public boolean isSupersetOf(Object o) {
        Set<String> currentSet = getVisitedNodes().keySet();
        Set<String> compareSet = ((Subgraph) o).getVisitedNodes().keySet();
        if (currentSet.size() < compareSet.size()) {
            return false;
        }
        return currentSet.containsAll(compareSet);
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

    @Override
    public String toString() {
        String ret = "";
        LinkedList<String> currentSet = new LinkedList<String>(this.getVisitedNodes().keySet());
        Collections.sort(currentSet);
        ret += currentSet.get(0);
        for (int i = 1; i < currentSet.size(); i++) {
            ret += "-" + currentSet.get(i);
        }
        return ret;
    }
}
