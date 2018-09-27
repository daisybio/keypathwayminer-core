package dk.sdu.kpm.perturbation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.graph.GeneNode;
import dk.sdu.kpm.graph.KPMGraph;
import org.apache.commons.collections15.map.HashedMap;

/**
 * Used for permuting a graph, by swapping nodes, until the parameter percentage permutation degree has been reached.
 * @author Martin
 *
 */
class DegreeAwareNodeSwap extends BasePerturbation<KPMGraph>{

    public DegreeAwareNodeSwap(KPMSettings kpmSettings){
        super(kpmSettings);
    }

    @Override
    public String getDescription() {
        return "Permutation of graph by way of node swap.";
    }

    @Override
    public String getName() {
        return "Node label permutation";
    }


    @Override
    public PerturbationTags getTag() {
        return PerturbationTags.NodeSwap;
    }

    /**
     * Method for permuting
     * @param percentageToPermute
     * @param currentGraph
     * @return
     */
    @Override
    public KPMGraph execute(int percentageToPermute, KPMGraph currentGraph, IKPMTaskMonitor taskMonitor){
        if(taskMonitor == null){
            taskMonitor = new KPMDummyTaskMonitor();
        }




            taskMonitor.setTitle(String.format("Permuting graph, using '%s'.", this.getName()));
            taskMonitor.setStatusMessage("Permuting...");

            KPMGraph graph = new KPMGraph(currentGraph);
        try(BufferedWriter bw = new BufferedWriter(
                new FileWriter("/home/anne/Masterarbeit/Testing/degreeDistNodeSwap"+kpmSettings.SEED+".txt"))) {
            HashSet<GeneNode> swappedNodes = new HashSet<GeneNode>();
            GeneNode[] nodes = graph.getVertices().toArray(new GeneNode[graph.getVertices().size()]);
            Arrays.sort(nodes, new Comparator<GeneNode>() {
                @Override
                public int compare(GeneNode o1, GeneNode o2) {
                    if((currentGraph.getNeighborCount(o2)-currentGraph.getNeighborCount(o1))>0){
                return -1;
                    }
                    else{
                        return 1;
                    }
                }
            });

            int counter= 0;
            for(GeneNode n: nodes){
                if(currentGraph.getNeighborCount(n)>kpmSettings.HIGH_DEGREE_NODES){
                    counter++;
                }
                else{
                    break;
                }
            }
            int nrNodesToShift = (int) Math.ceil(((double) nodes.length / 100) * percentageToPermute);
            int total = (int) Math.ceil(((double) nodes.length / 100) * percentageToPermute);

            // Fill the list with edges indexes. Will be used to draw nodes to be removed.
            this.initIndexRandomizer(nodes.length);

            int[] degreeCounter = new int[currentGraph.getVertexCount() + 1];
            Map<Integer, ArrayList<GeneNode>> degree2NodeId = new HashMap<Integer, ArrayList<GeneNode>>();
            for (GeneNode n : currentGraph.getVertices()) {
                degreeCounter[currentGraph.getNeighborCount(n)] += 1;
                degree2NodeId.putIfAbsent(currentGraph.getNeighborCount(n), new ArrayList<GeneNode>());
                degree2NodeId.get(currentGraph.getNeighborCount(n)).add(n);
            }

            int binSize = 100;
            int sum = 0;
            int binCounter = 0;
            Map<Integer, ArrayList<Integer>> degrees = new HashMap<Integer, ArrayList<Integer>>();
            Map<Integer, Integer> invertDeg = new HashedMap<>();
            for (int i = 0; i < degreeCounter.length; i++) {
                sum += degreeCounter[i];
                if (degree2NodeId.containsKey(i)) {
                    degrees.putIfAbsent(binCounter, new ArrayList<Integer>());
                    degrees.get(binCounter).add(i);
                    invertDeg.putIfAbsent(i, binCounter);
                }
                if (sum > binSize) {
                    sum = 0;
                    binCounter++;
                }
            }

            int c= 20;
            while (nrNodesToShift > 1) {
                taskMonitor.setProgress((double) ((double) 1 - ((double) nrNodesToShift) / ((double) total)));

                int ind = 0;
                // Get random node
                if(counter>0){
                    c--;
                    ind = kpmSettings.R.nextInt(counter);
                }
                else{
                    ind = getNextRandomIndex();
                }
                GeneNode node1 = nodes[ind];

                // Get random node with similar degree
                int deg = currentGraph.getNeighborCount(node1);
                ArrayList<GeneNode> cur = new ArrayList<GeneNode>();
                // System.out.println(deg);
                int bin = invertDeg.get(deg);
                //System.out.println(bin);

                //System.out.println(counter);

                for (Integer i : degrees.get(bin)) {
                    //System.out.println(i);
                    cur.addAll(degree2NodeId.get(i));
                }
                int rand = this.kpmSettings.R.nextInt(cur.size());
                GeneNode node2 = cur.get(rand);
                counter++;
                // If it's the same ID, or either nodes have been swapped already, then we don't want to swap.
                if (node1.nodeId.equals(node2.nodeId) || swappedNodes.contains(node1) || swappedNodes.contains(node2)) {
                    continue;
                }

                bw.write(currentGraph.getNeighborCount(node1)+"\t"+currentGraph.getNeighborCount(node2)+"\n");
                // Shifting all edges between them, assuming we're iterating by reference
                for (String[] edge : graph.getEdgeList()) {

                    if (edge[0].equals(node1.nodeId)) {
                        edge[0] = node2.nodeId;
                    }

                    if (edge[1].equals(node1.nodeId)) {
                        edge[1] = node2.nodeId;
                    }

                    if (edge[0].equals(node2.nodeId)) {
                        edge[0] = node1.nodeId;
                    }

                    if (edge[1].equals(node2.nodeId)) {
                        edge[1] = node1.nodeId;
                    }
                }

                swappedNodes.add(node1);
                swappedNodes.add(node2);

                nrNodesToShift -= 2;
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return graph;
    }

}

