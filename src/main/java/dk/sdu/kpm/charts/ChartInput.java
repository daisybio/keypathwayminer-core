package dk.sdu.kpm.charts;

import dk.sdu.kpm.graph.Result;

import java.util.List;

/**
 * Created by Martin on 18-05-2015.
 */
public class ChartInput {
    public ChartInput(int var1, int var2, List<Result> results){
        this.VAR1 = var1;
        this.VAR2 = var2;
        this.Results = results;
    }

    public int VAR1;

    public int VAR2;

    public final List<Result> Results;
}
