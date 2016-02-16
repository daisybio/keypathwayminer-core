package dk.sdu.kpm.perturbation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by Martin on 24-01-2015.
 */
public class IndexRandomizer {
    private Random random;

    private ArrayList<Integer> indexList;

    private int indexSize;

    public IndexRandomizer(int indexSize){
        this.indexList = new ArrayList<Integer>();
        this.indexSize = indexSize;
        for(int i = 0; i < indexSize; i++){
            this.indexList.add(i);
        }
        // Get random number generator
        this.random = new Random();
        this.random.setSeed(Calendar.getInstance().getTimeInMillis());
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
