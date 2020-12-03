package dk.sdu.kpm.results;

import dk.sdu.kpm.KPMSettings;
import dk.sdu.kpm.charts.IChart;
import dk.sdu.kpm.runners.BatchResult;
import dk.sdu.kpm.validation.ValidationOverlapResult;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class StandardResultSet implements IKPMResultSet, Serializable {

    private KPMSettings settings;
    private List<BatchResult> results;
    private Map<String, IChart> charts;
    private List<ValidationOverlapResult> overlapResults;

    public StandardResultSet(KPMSettings settings, List<BatchResult> results, Map<String, IChart> charts, List<ValidationOverlapResult> overlapResults) {
        this.settings = settings;
        this.results = results;
        this.charts = charts;
        this.overlapResults = overlapResults;
    }

    @Override
    public String getKpmID() {
        return this.settings.getKpmRunID();
    }

    @Override
    public List<BatchResult> getResults() {
        return this.results;
    }

    @Override
    public Map<String, IChart> getCharts() {
        return this.charts;
    }

    @Override
    public KPMSettings getKpmSettings() {
        return this.settings;
    }

    @Override
    public void setKpmSettings(KPMSettings settings) {
        this.settings = settings;
    }

    @Override
    public List<ValidationOverlapResult> getOverlapResults() {
        return this.overlapResults;
    }

}
