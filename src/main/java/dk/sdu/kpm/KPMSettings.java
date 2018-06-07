package dk.sdu.kpm;

import dk.sdu.kpm.algo.glone.LocalSearch;
import dk.sdu.kpm.algo.glone.RhoDecay;
import dk.sdu.kpm.graph.KPMGraph;
import dk.sdu.kpm.results.PercentageParameters;
import dk.sdu.kpm.validation.ValidationOverlapResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Created by: Martin
 * Date: 17-02-14
 */
public class KPMSettings {

    public KPMSettings() {
    	this.RunID = UUID.randomUUID().toString();
    }

    public void updateRunID(String newRunID){
        this.RunID = newRunID;
    }

    public volatile String RunID;
    
    public String getKpmRunID(){
    	return this.RunID;
    }
    
    // OS-dependent line-separating character.
    public volatile String lineSep = System.getProperty("line.separator");

    // OS-dependent path-delimiter.
    public volatile String fileSep = System.getProperty("file.separator");

    // Manages the mapping of the name of the data set to its internally-used
    // identifier.
    // Internal identifiers must be used since MVEL, the package responsible for
    // evaluating the logical expression connecting the data sets,
    // can only handle valid Java identifiers. Since we do not want to
    // restrict the user to Java identifiers, this mapping is needed.
    public volatile ExternalToInternalIDManager externalToInternalIDManager = new ExternalToInternalIDManager();

    // /* ------ INPUT FILES ------------ */

    // The column delimiter used in the data set files.
    public volatile String columnDelim = "\t";

    public volatile boolean INCLUDE_CHARTS = false;

    public volatile List<String> VARYING_L_ID = new ArrayList<String>(2);

    public volatile Map<String,Boolean> VARYING_L_ID_IN_PERCENTAGE = 
            new HashMap<String, Boolean>(2);


	/* ------ BASIC PARAMETERS ------------ */

    // Maximum number of allowed genes NOT differentially expressed in each
    // solution (only used for INES strategy).
    public volatile int GENE_EXCEPTIONS = 0;

    // Maximum number of allowed genes NOT differentially expressed in each
    // gene to be considered diff. expressed in each dataset. This is a mapping
    // from the dataset id's to
    // the "L" paramater for each dataset.
    public volatile HashMap<String, Integer> CASE_EXCEPTIONS_MAP = new HashMap<String, Integer>();


    // Map storing the paths to the multiple expression files
    // NOTE1: The key ID's must be the same as the key ID's in
    // CASE_EXCEPTIONS_MAP
    // NOTE2: Will only be used if MULTI == True
    public volatile Map<String, String> MATRIX_FILES_MAP = new HashMap<String, String>();

    // Map storing the paths to the multiple p-value (=expression) files
    // NOTE1: The key ID's must be the same as the key ID's in
    public volatile Map<String, String> PVALUE_FILES_MAP = new HashMap<String, String>();
    public volatile Map<String, Double> PVALUE_MAP = new HashMap<String, Double>();

    public volatile boolean USE_DOUBLE_VALUES =false;

    // What algorithm is being used -- KPM is the old ACO that is outperformed
    // by LCG, the new ACO; GREEDY is a greedy algorithm that always takes the
    // next exception nodes with the highest fitness; OPTIMAL computes the
    // optimal solution but usually is very slow.
    public volatile Algo ALGO = Algo.GREEDY;

    // Number of solutions the algorithm has to return (it will always return
    // the best ones). A -1 will return ALL results.
    public volatile int NUM_SOLUTIONS = 20;


	/* ------ ADVANCED PARAMETERS (GENERAL) ------------ */

    // Number of processors/threads to use in parallelized computations
    public volatile int NUMBER_OF_PROCESSORS = 1;

    // The heuristic value for each node when searching for solutions.
    // This can be AVERAGE (average differentially expressed cases) or
    // TOTAl (total number of differentially expressed cases)
    public volatile Heuristic NODE_HEURISTIC_VALUE = Heuristic.AVERAGE;

    // How the datasets will be combine (OR, AND).
    public volatile Combine COMBINE_OPERATOR = Combine.OR;

    // The boolean formula used to combine the different datasets. Used
    // only if combine_operator == custom. Valid operators:
    // && = AND, || = OR, ! = negation, () = parenthesis.
    public volatile String COMBINE_FORMULA = "(L1 || L2) && !L1";

    // Determines whether certain evaluation routines should run. Enabling only
    // yields some statistics, has no effect on a "normal" algorithm run other
    // than slowing it down.
    public volatile boolean EVAL = false;

    // Whether the solution array is allowed to yield multiple entries of the
    // same solution
    public volatile boolean DOUBLE_SOLUTIONS_ALLOWED = false;

	/* ------ ADVANCED PARAMETERS (ONLY FOR ACO ALGORITHM) ------------ */

    // Parameter to control pheromone importance
    public volatile double ALPHA = 2.0;

    // Parameter to control edge desirability importance
    public volatile double BETA = 5.0;

    // Pheromone decay rate
    public volatile double RHO = 0.1;

    // The function that determines how fast the pheromone Rho should
    // decay: CONSTANT, LINEAR, QUADRATIC or EXPONENTIAL
    public volatile RhoDecay RHO_DECAY = RhoDecay.CONSTANT;

    // The minimum pheromone that can be on a GeneNode
    public volatile double TAU_MIN = 0.1;

    // Defines the tradeoff between pheromones and fitness when an ant picks a
    // new vertex. If true, then tradeOff(a,b) = a^(alpha)*b^(beta). If false,
    // tradeOff(a,b) = alpha * a + beta * b.
    public volatile boolean MULTIPLICATIVE_TRADEOFF = true;

    // Whether to use an iteration-based or an global-based ACO
    public volatile boolean ITERATION_BASED = true;

    // Maximum number of iterations overall
    public volatile int MAX_ITERATIONS = Integer.MAX_VALUE;

    // Maximum number of iterations where the best solution does not change
    public volatile int MAX_RUNS_WITHOUT_CHANGE = 100;

    // How many solutions should be considered before updating the pheromones
    // and begin a new iteration
    public volatile int NUMBER_OF_SOLUTIONS_PER_ITERATION = 20;

    // How many starting nodes the expection sum ACO should take
    public volatile int NUM_STARTNODES = 30;

    // which local search method is used to improve the result
    public volatile LocalSearch L_SEARCH = LocalSearch.GREEDY1;

    // Randomness Generator used
    public volatile long SEED = (new Random(System.currentTimeMillis())).nextLong();

    public volatile Random R = new Random(SEED);

	/* ------ GLOBAL VARIABLES ------------ */

    // The working graph
    public volatile KPMGraph MAIN_GRAPH;

    // The working graph, backup
    public volatile KPMGraph MAIN_GRAPH_BACKUP;

    // Mapping from the internal graph edge id's to cyedge id's
    public volatile HashMap<String, String> EDGE_ID_MAP;

    // Maximum number of allowed genes NOT differentially expressed in each
    // solution for each dataset. THis is a mapping form the dataset id's to
    // the "K" paramater for each dataset. NOT USED FOR NOW.
    public volatile HashMap<String, Integer> GENE_EXCEPTIONS_MULTI = new HashMap<String, Integer>();

    // Number of cases for each matrix/study
    public volatile HashMap<String, Integer> NUM_CASES_MAP = new HashMap<String, Integer>();

    // Number of nodes in the original graph
    public volatile int N = -1;

    // Number of expression studies
    public volatile int NUM_STUDIES = -1;

    // When this algorithm was started
    public volatile long STARTING_TIME;

    // Total running time of the algorithm
    public volatile long TOTAL_RUNNING_TIME = 0;

    // Interaction type for cytoscape network
    public volatile String INTERACTION_TYPE = "pp";

    // Main CyNetwork id
    public volatile String MAIN_CYNETWORK_ID = "ID";

    // If this is a normal of batch run
    public volatile boolean IS_BATCH_RUN = false;

    public volatile boolean REMOVE_BENs = true;

	/* ------ VARIABLES FOR BATCH RUN ------------------- */

    public volatile int MIN_K = 0;

    public volatile int MAX_K = 10;

    public volatile int INC_K = 2;

    /**
     * Default value of case exceptions for every data set.
     */
    public final int CASE_EXCEPTIONS_DEFAULT = 0;
    
    public final int MIN_L_DEFAULT = 0;

    public final int MAX_L_DEFAULT = 1;

    public final int INC_L_DEFAULT = 1;
    
    public final double MIN_PER_DEFAULT = 0.0;
    
    public final double MAX_PER_DEFAULT = 10.0;
    
    public final double INC_PER_DEFAULT = 10.0;
    

    // Whether or not to use the same L values, (L1 = 10, L2 = 10 and L1 = 20, L2 = 20)
    public volatile boolean CALCULATE_ONLY_SAME_L_VALUES = false;

    public volatile HashMap<String, Integer> MIN_L = new HashMap<String, Integer>();

    public volatile HashMap<String, Integer> MAX_L = new HashMap<String, Integer>();

    public volatile HashMap<String, Integer> INC_L = new HashMap<String, Integer>();

    public volatile double MIN_PER = 0.0;
    
    public volatile double MAX_PER = 10.0;
    
    public volatile double INC_PER = 10.0;


    public volatile boolean STORE_PATHWAYS = false;

    public volatile HashMap<List<Integer>, RunStats> STATS_MAP;

    public volatile HashMap<PercentageParameters, RunStats> STATS_MAP_PER;

    public volatile HashMap<String, Integer> TOTAL_NODE_HITS;

    public volatile int TOTAL_NODE_HITS_MIN = Integer.MAX_VALUE;

    public volatile int TOTAL_NODE_HITS_MAX = 0;

    public volatile double TOTAL_ACTIVE_CASES_MAX = Double.MIN_VALUE;

    public volatile double TOTAL_ACTIVE_CASES_MIN = Double.MAX_VALUE;
    
    public volatile boolean USE_INES = true;

    public volatile HashMap<String, Integer> TOTAL_EDGE_HITS;

    public volatile int TOTAL_EDGE_HITS_MIN = Integer.MAX_VALUE;

    public volatile int TOTAL_EDGE_HITS_MAX = 0;

    public volatile HashMap<Integer, String> INDEX_L_MAP = new HashMap<Integer, String>();

    public volatile List<String> VALIDATION_GOLDSTANDARD_NODES = new ArrayList<String>();

    public boolean containsGoldStandardNodes(){
        return this.VALIDATION_GOLDSTANDARD_NODES != null && this.VALIDATION_GOLDSTANDARD_NODES.size() > 0;
    }

   
   /**
    * Validation overlap results.
    * The HashMap key is the fileName of the validation set.
    * The HashSet is a list of results for each K/L combination.
    */
   public volatile List<ValidationOverlapResult> ValidationOverlapResults = new ArrayList<ValidationOverlapResult>();
   
   /**
    * Method for creating a deep copy of a KPMSettings object.
    * Used especially during a BatchRunWithPerturbation for keeping the original settings results for later. 
    * @param settings
    */
   public KPMSettings(KPMSettings settings){
	   	RunID = settings.getKpmRunID();
        VALIDATION_GOLDSTANDARD_NODES = new ArrayList<String>(settings.VALIDATION_GOLDSTANDARD_NODES);
	   	USE_INES = settings.USE_INES;
   		externalToInternalIDManager = settings.externalToInternalIDManager;
	    columnDelim = settings.columnDelim;
	    GENE_EXCEPTIONS = settings.GENE_EXCEPTIONS;
	    CASE_EXCEPTIONS_MAP = new HashMap<String, Integer>(settings.CASE_EXCEPTIONS_MAP);
	    MATRIX_FILES_MAP = new HashMap<String, String>(settings.MATRIX_FILES_MAP);
	    ALGO = settings.ALGO;
	    NUM_SOLUTIONS = settings.NUM_SOLUTIONS;
	    NUMBER_OF_PROCESSORS = settings.NUMBER_OF_PROCESSORS;
	    NODE_HEURISTIC_VALUE = Heuristic.AVERAGE;
	    COMBINE_OPERATOR = settings.COMBINE_OPERATOR;
	    COMBINE_FORMULA = settings.COMBINE_FORMULA;
	    EVAL = settings.EVAL;
	    DOUBLE_SOLUTIONS_ALLOWED = settings.DOUBLE_SOLUTIONS_ALLOWED;
	    ALPHA = settings.ALPHA;
	    BETA = settings.BETA;
	    RHO = settings.RHO;
	    RHO_DECAY = settings.RHO_DECAY;
	    TAU_MIN = settings.TAU_MIN;
	    MULTIPLICATIVE_TRADEOFF = settings.MULTIPLICATIVE_TRADEOFF;
	    ITERATION_BASED = settings.ITERATION_BASED;
	    MAX_ITERATIONS = settings.MAX_ITERATIONS;
	    MAX_RUNS_WITHOUT_CHANGE = settings.MAX_RUNS_WITHOUT_CHANGE;
	    NUMBER_OF_SOLUTIONS_PER_ITERATION = settings.NUMBER_OF_SOLUTIONS_PER_ITERATION;
	    NUM_STARTNODES = settings.NUM_STARTNODES;
	    L_SEARCH = settings.L_SEARCH;
	    SEED = settings.SEED;
	    R = settings.R;
	    MAIN_GRAPH = new KPMGraph(settings.MAIN_GRAPH);
	    MAIN_GRAPH_BACKUP = new KPMGraph(settings.MAIN_GRAPH_BACKUP);
	    EDGE_ID_MAP = settings.EDGE_ID_MAP;
	    GENE_EXCEPTIONS_MULTI = new HashMap<String, Integer>(settings.GENE_EXCEPTIONS_MULTI);
	    NUM_CASES_MAP = new HashMap<String, Integer>(settings.NUM_CASES_MAP);
	    N = settings.N;
	    NUM_STUDIES = settings.NUM_STUDIES;
	    STARTING_TIME = settings.STARTING_TIME;
	    TOTAL_RUNNING_TIME = settings.TOTAL_RUNNING_TIME;
	    INTERACTION_TYPE = settings.INTERACTION_TYPE;
	    MAIN_CYNETWORK_ID = settings.MAIN_CYNETWORK_ID;
	    IS_BATCH_RUN = settings.IS_BATCH_RUN;
	    REMOVE_BENs = settings.REMOVE_BENs;
	    MIN_K = settings.MIN_K;
	    MAX_K = settings.MAX_K;
	    INC_K = settings.INC_K;
	    MIN_L = new HashMap<String, Integer>(settings.MIN_L);
	    MAX_L = new HashMap<String, Integer>(settings.MAX_L);
	    INC_L = new HashMap<String, Integer>(settings.INC_L);
	    STORE_PATHWAYS = settings.STORE_PATHWAYS;
	    TOTAL_NODE_HITS_MIN = settings.TOTAL_NODE_HITS_MIN;
	    TOTAL_NODE_HITS_MAX = settings.TOTAL_NODE_HITS_MAX;
	    TOTAL_ACTIVE_CASES_MAX = settings.TOTAL_ACTIVE_CASES_MAX;
	    TOTAL_ACTIVE_CASES_MIN = settings.TOTAL_ACTIVE_CASES_MIN;
	    TOTAL_EDGE_HITS_MIN = settings.TOTAL_EDGE_HITS_MIN;
	    TOTAL_EDGE_HITS_MAX = settings.TOTAL_EDGE_HITS_MAX;
	    INDEX_L_MAP = new HashMap<Integer, String>(settings.INDEX_L_MAP);
       
	    
	    if(settings.TOTAL_EDGE_HITS != null){
	    	 TOTAL_EDGE_HITS = new HashMap<String, Integer>(settings.TOTAL_EDGE_HITS);
	    }

	    if(settings.TOTAL_NODE_HITS != null){
	    	TOTAL_NODE_HITS = new HashMap<String, Integer>(settings.TOTAL_NODE_HITS);
	    }
	    
	    // Copying the results:
	    STATS_MAP = new HashMap<List<Integer>, RunStats>();
	    if(settings.STATS_MAP != null){
	    	for(List<Integer> key : settings.STATS_MAP.keySet()){
	    		STATS_MAP.put(key, new RunStats(settings.STATS_MAP.get(key)));
	    	}
	    }
            
            STATS_MAP_PER = new HashMap<PercentageParameters, RunStats>();
	    if(settings.STATS_MAP_PER != null){
	    	for(PercentageParameters key : settings.STATS_MAP_PER.keySet()){
	    		STATS_MAP_PER.put(key,  new RunStats(settings.STATS_MAP_PER.get(key)));
	    	}
	    }
            
            CALCULATE_ONLY_SAME_L_VALUES = settings.CALCULATE_ONLY_SAME_L_VALUES;
            MIN_PER = settings.MIN_PER;
            INC_PER = settings.INC_PER;
            MAX_PER = settings.MAX_PER;
	    VARYING_L_ID = new ArrayList<String>(2);
            if (settings.VARYING_L_ID != null) {
                for(String lid: VARYING_L_ID) {
                    VARYING_L_ID.add(lid);
                }
            }
       ValidationOverlapResults = new ArrayList<ValidationOverlapResult>(settings.ValidationOverlapResults);
   }

    public boolean allSameForVaryingL(){
        String lid = ""; //VARYING_L_ID;

        try {
            if (!MIN_L.containsKey(lid) || !INC_L.containsKey(lid) || !MAX_L.containsKey(lid)) {
                return true;
            }

            int val = MIN_L.get(lid);

            if (val != INC_L.get(lid)) {
                return false;
            }

            if (val != MAX_L.get(lid)) {
                return false;
            }
        }catch(Exception e){
            return false;
        }


        return true;
    }

    @Override
    public String toString(){
        String str = "RunID = " + this.RunID + lineSep;
        str += "IS_BATCH_RUN = " + IS_BATCH_RUN + lineSep;
        str += "CALCULATE_ONLY_SAME_L_VALUES = " + CALCULATE_ONLY_SAME_L_VALUES + lineSep;
        str += "ALGO = " + ALGO.toString() + lineSep;
        str += "ALPHA = " + ALPHA + lineSep;
        str += "BETA = " + BETA + lineSep;
        str += "RHO = " + RHO + lineSep;
        str += "RHO_DECAY = " + RHO_DECAY+ lineSep;
        str += "TAU_MIN = " + TAU_MIN+ lineSep;
        str += "MULTIPLICATIVE_TRADEOFF = " + MULTIPLICATIVE_TRADEOFF+ lineSep;
        str += "CASE_EXCEPTIONS_DEFAULT = " + CASE_EXCEPTIONS_DEFAULT + lineSep;
        if(MAIN_GRAPH != null) {
            str += "MAIN_GRAPH, # edges = " + MAIN_GRAPH.getEdgeCount() + lineSep;
            str += "MAIN_GRAPH, # nodes = " + MAIN_GRAPH.getVertexCount() + lineSep;
        }else{
            str += "MAIN_GRAPH is null" + lineSep;
        }
        str += "COMBINE_FORMULA = " + COMBINE_FORMULA.toString() + lineSep;
        str += "DOUBLE_SOLUTIONS_ALLOWED = " + DOUBLE_SOLUTIONS_ALLOWED + lineSep;
        str += "INTERACTION_TYPE = " + INTERACTION_TYPE + lineSep;
        str += "EVAL = " + EVAL + lineSep;
        str += "GENE_EXCEPTIONS = " + GENE_EXCEPTIONS + lineSep;
        str += "MIN_K = " + MIN_K + lineSep;
        str += "INC_K = " + INC_K + lineSep;
        str += "MAX_K = " + MAX_K + lineSep;
        str += "INC_L_DEFAULT = " + INC_L_DEFAULT + lineSep;
        str += "MAX_L_DEFAULT = " + MAX_L_DEFAULT + lineSep;
        str += "MIN_L_DEFAULT = " + MIN_L_DEFAULT + lineSep;

        if(MIN_L == null || MIN_L.size() == 0){
            str += "No MIN_L " + lineSep;
        }else{
            for (String key : MIN_L.keySet()){
                str += "MIN_L[" + key + "] = " + MIN_L.get(key) + lineSep;
            }
        }

        if(MAX_L == null || MAX_L.size() == 0){
            str += "No MAX_L " + lineSep;
        }else{
            for (String key : MAX_L.keySet()){
                str += "MAX_L[" + key + "] = " + MAX_L.get(key) + lineSep;
            }
        }

        if(INC_L == null || INC_L.size() == 0){
            str += "No INC_L " + lineSep;
        }else{
            for (String key : INC_L.keySet()){
                str += "INC_L[" + key + "] = " + INC_L.get(key) + lineSep;
            }
        }

        str += "REMOVE_BENs = " + REMOVE_BENs+ lineSep;
        str += "INCLUDE_CHARTS = " + INCLUDE_CHARTS + lineSep;
        str += "ITERATION_BASED = " + ITERATION_BASED + lineSep;
        str += "NUM_SOLUTIONS = " + NUM_SOLUTIONS + lineSep;
        str += "NUM_STARTNODES = " + NUM_STARTNODES + lineSep;
        str += "NUM_STUDIES = " + NUM_STUDIES + lineSep;
        str += "MAX_ITERATIONS = " + MAX_ITERATIONS + lineSep;
        str += "MAX_RUNS_WITHOUT_CHANGE = " + MAX_RUNS_WITHOUT_CHANGE + lineSep;
        str += "NUMBER_OF_SOLUTIONS_PER_ITERATION = " + NUMBER_OF_SOLUTIONS_PER_ITERATION + lineSep;
        str += "CALCULATE_ONLY_SAME_L_VALUES = " + CALCULATE_ONLY_SAME_L_VALUES + lineSep;
        if (STATS_MAP == null) {
             str += "SIZE STATS_MAP = null" + lineSep;
        } else {
            str += "SIZE STATS_MAP = " + STATS_MAP.size() + lineSep;
        }
        
        if (STATS_MAP_PER == null) {
             str += "SIZE STATS_MAP_PER = null" + lineSep;
        } else {
            str += "SIZE STATS_MAP_PER = " + STATS_MAP_PER.size() + lineSep;
        }
        str += "IS_BATCH_RUN = " + IS_BATCH_RUN + lineSep;
        str += "MIN_PER = " + MIN_PER + lineSep;
        str += "INC_PER = " + INC_PER + lineSep;
        str += "MAX_PER = " + MAX_PER + lineSep;
        
        
        return str;
    }
}