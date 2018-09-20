package dk.sdu.kpm.perturbation;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.taskmonitors.IKPMTaskMonitor;
import dk.sdu.kpm.graph.KPMGraph;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Base class for perturbations. Defining the executeAsync, executeRangeAsync and toString.
 * @author Martin
 *
 */
class BasePerturbation<T> implements IPerturbation<T> {
    protected IndexRandomizer randomizer;
    protected KPMSettings kpmSettings;

    protected BasePerturbation(KPMSettings kpmSettings){
    	this.kpmSettings = kpmSettings;
    }

    protected void initIndexRandomizer(int indexSize){
        this.randomizer = new IndexRandomizer(indexSize, kpmSettings);
    }

    protected int getNextRandomIndex(){
        return this.randomizer.getNextRandomIndex();
    }

	@Override
	public String getDescription() {
		// Override by subclasses
		return null;
	}

	@Override
	public String getName() {
		// Override by subclasses
		return null;
	}

	@Override
	public T execute(int percentageToPermute, T input, IKPMTaskMonitor taskMonitor) {
		// Override by subclasses
		return null;
	}
	
	@Override
	public PerturbationTags getTag() {
		// Override by subclasses
		return null;
	}

	@Override
    public String toString() {
        return this.getName();
    }
}
