package dk.sdu.kpm.perturbation;

import dk.sdu.kpm.KPMSettings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by Martin on 24-01-2015.
 * Modified by Anne on 20-09-2018
 */
public class IndexRandomizer {
    // make sure the instances of random are always the same as in the main
    // program. Needed for reproducibility.
    private KPMSettings kpmSettings;
    private Random random;

    private ArrayList<Integer> indexList;

    private int indexSize;

    public IndexRandomizer(int indexSize, KPMSettings kpmSettings){
        this.kpmSettings = kpmSettings;
        this.indexList = new ArrayList<Integer>();
        this.indexSize = indexSize;
        for(int i = 0; i < indexSize; i++){
            this.indexList.add(i);
        }
        // Get random number generator
        this.random = kpmSettings.R;
        // seed should be set by the main program
        //this.random.setSeed(Calendar.getInstance().getTimeInMillis());


    }

    public int getNextRandomIndex(){
        // draw a random node that hasn't been removed.\
 //       System.out.println("INDEX SIZE AT INDEXRANDOMIZER 2 " + indexSize);
        if (indexSize <= 0) {
            return 0;
        }
        int indexIndex = random.nextInt(indexSize);
        if(indexIndex < 0){
            return 0;
        }
        
        int index = indexList.get(indexIndex);
        indexList.remove(indexIndex);
        indexSize--;

        return index;
    }
}
