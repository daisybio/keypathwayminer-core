package dk.sdu.kpm.runners;

import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.taskmonitors.KPMDummyTaskMonitor;
import dk.sdu.kpm.perturbation.IPerturbation;
import dk.sdu.kpm.results.IKPMRunListener;

import java.io.Serializable;

public class BatchRunWithPerturbationParameters implements Serializable {
	public IPerturbation permuter; 
	public int minPercentage; 
	public int stepPercentage; 
	public int maxPercentage;
	public int graphsPerStep;
	public boolean isINEs; 
	public String runId;
	public IKPMTaskMonitor taskMonitor;
	public IKPMRunListener listener;
	public boolean includeStandardCharts;
	
	public BatchRunWithPerturbationParameters(IPerturbation permuter, 
			int minPercentage, 
			int stepPercentage, 
			int maxPercentage,
			int graphsPerstep,
			boolean isINEs, 
			String runId,
			IKPMTaskMonitor taskMonitor,
			IKPMRunListener listener){
		
		this.permuter = permuter;
		this.minPercentage = minPercentage;
		this.stepPercentage = stepPercentage;
		this.maxPercentage = maxPercentage;
		this.graphsPerStep = graphsPerstep;
		this.isINEs = isINEs;
		this.runId = runId;
		this.listener = listener;
		this.includeStandardCharts = false;

		if(taskMonitor != null){
			this.taskMonitor = taskMonitor;
		}else{
			this.taskMonitor = new KPMDummyTaskMonitor();
		}
	}
	
	public BatchRunWithPerturbationParameters(IPerturbation permuter, 
			int minPercentage, 
			int stepPercentage, 
			int maxPercentage,
			int graphsPerstep,
			boolean isINEs, 
			String runId,
			IKPMTaskMonitor taskMonitor,
			IKPMRunListener listener,
			boolean includeStandardCharts){
		this(permuter, 
			minPercentage, 
			stepPercentage, 
			maxPercentage,
			graphsPerstep,
			isINEs, 
			runId,
			taskMonitor,
			listener);
		this.includeStandardCharts = includeStandardCharts;
		
	}
}
