package dk.sdu.kpm.statistics;

import java.io.Serializable;

/**
 * Created by Martin on 23-02-2015.
 */
public class Pair<L,R>  implements Serializable {

    private final L l;
    private final R k;

    public Pair( R k, L l) {
        this.l = l;
        this.k = k;
    }

    public L getL() { return l; }
    public R getK() { return k; }

    @Override
    public int hashCode() { return l.hashCode() ^ k.hashCode(); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) return false;
        Pair pairo = (Pair) o;
        return this.l.equals(pairo.getL()) &&
                this.k.equals(pairo.getK());
    }

}