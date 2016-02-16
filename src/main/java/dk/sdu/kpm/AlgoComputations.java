package dk.sdu.kpm;

import dk.sdu.kpm.algo.glone.ACO;
import dk.sdu.kpm.algo.glone.Greedy;
import dk.sdu.kpm.algo.glone.Optimal;
import dk.sdu.kpm.algo.ines.GraphProcessing;
import dk.sdu.kpm.algo.ines.LComponentGraph;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.graph.Result;
import dk.sdu.kpm.logging.KpmLogger;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;

import java.util.List;
import java.util.logging.Level;

/**
 * Created by Martin on 14-03-2015.
 */
public class AlgoComputations {

    private LComponentGraph lcg = null;
    private Greedy greedy = null;
    private Optimal opt = null;
    private ACO aco = null;

    public List<Result> run(Algo algo, KPMGraph g, IKPMTaskMonitor taskMonitor, KPMSettings settings) {

        List<Result> results = null;

        // If there is no monitor, we create a dummy
        if(taskMonitor == null){
            taskMonitor = new KPMDummyTaskMonitor();
        }

        try {
            switch (algo) {

                case LCG:
                    lcg = GraphProcessing.componentGraph(g, taskMonitor, settings);
                    results = lcg.ACO();
                    lcg = null;
                    break;

                case GREEDY:
                    lcg = GraphProcessing.componentGraph(g, taskMonitor, settings);
                    results = lcg.greedy();
                    lcg = null;
                    break;

                case OPTIMAL:
                    lcg = GraphProcessing.componentGraph(g, taskMonitor, settings);
                    results = lcg.optimal();
                    lcg = null;
                    break;

                case EXCEPTIONSUMGREEDY:
                    greedy = new Greedy(g, taskMonitor, settings);
                    results = greedy.runGreedy();
                    greedy = null;
                    break;

                case EXCEPTIONSUMOPTIMAL:
                    opt = new Optimal(g, taskMonitor, settings);
                    results = opt.runOptimal();
                    opt = null;
                    break;

                case EXCEPTIONSUMACO:
                    aco = new ACO(g, taskMonitor, settings);
                    results = aco.runACO();
                    aco = null;
                    break;

                default:
                    System.out.println("Not implemented yet.");
                    break;
            }
        }catch(Exception e){
            KpmLogger.log(Level.SEVERE, e);
        }finally {
            cleanup();
        }

        return results;
    }

    private synchronized void cleanup(){
        this.aco = null;
        this.greedy = null;
        this.lcg = null;
        this.opt = null;
    }

    public synchronized void cancel(){
        if(lcg != null){
            lcg.cancel();
        }

        if(greedy != null){
            greedy.cancel();
        }

        if(opt != null){
            opt.cancel();
        }

        if(aco != null){
            aco.cancel();
        }

        cleanup();
    }
}
