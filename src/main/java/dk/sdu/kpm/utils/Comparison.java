package dk.sdu.kpm.utils;

public class Comparison {
    Comparator comparator;

    public Comparison(Comparator comp){
        this.comparator = comp;
    }

    public static boolean evalate(double pval, double threshold, Comparator comp){
        switch(comp){
            case GT:
                if(pval>threshold){
                    return true;
                }
                else{
                    return false;
                }
            case LET:
                if(pval<=threshold){
                    return true;
                }
                else{
                    return false;
                }
        }
        //will not be reached
        return false;
    }
}
