package dk.sdu.kpm.algo.glone;

import dk.sdu.kpm.Heuristic;
import dk.sdu.kpm.graph.GeneNode;

import java.io.Serializable;


public class TwoNodes implements Comparable<TwoNodes>, Serializable {
	private GeneNode n1;
	private GeneNode n2;

	private volatile Heuristic node_heuristic_value;
	
	public TwoNodes(GeneNode n1, GeneNode n2, Heuristic node_heuristic_value) {
		this.n1 = n1;
		this.n2 = n2;
		this.node_heuristic_value = node_heuristic_value;
	}

	public int getNumNoDiffExpressedCases() {
		return n1.getHeuristicValue(node_heuristic_value)
				+ n2.getHeuristicValue(node_heuristic_value);

	}

	@Override
	public int compareTo(TwoNodes o) {
		return -(new Integer(getNumNoDiffExpressedCases())).compareTo(o
				.getNumNoDiffExpressedCases());
	}

	public GeneNode getN1() {
		return n1;
	}

	public GeneNode getN2() {
		return n2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((n1 == null) ? 0 : n1.hashCode());
		result = prime * result + ((n2 == null) ? 0 : n2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TwoNodes other = (TwoNodes) obj;
		if (n1 == null) {
			if (other.n1 != null)
				return false;
		} else if (!n1.equals(other.n1))
			return false;
		if (n2 == null) {
			if (other.n2 != null)
				return false;
		} else if (!n2.equals(other.n2))
			return false;
		return true;
	}

}
