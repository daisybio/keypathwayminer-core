/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.sdu.kpm.graph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author nalcaraz
 */
public class GeneEdge implements Serializable {

	String edgeId;

	double pheromone;

	double weight;

	double probability;

	Set paths;

	static final double STARTPHEROMONE = 1.0;

	private static int nextId = 0;
	
	public GeneEdge(GeneEdge e){
		this.edgeId = e.edgeId;
		this.pheromone = e.pheromone;
		this.weight = e.weight;
		this.probability = e.probability;
		this.paths = new HashSet(e.paths);
	}

	public GeneEdge(String edgeId) {
		this.edgeId = edgeId;
		this.pheromone = STARTPHEROMONE;
		this.weight = 1.0;
		this.paths = null;
	}

	public GeneEdge(String edgeId, Set paths) {
		this.edgeId = edgeId;
		this.pheromone = STARTPHEROMONE;
		this.weight = 1.0;
		this.paths = paths;
	}

	public GeneEdge(String edgeId, double weight) {
		this.edgeId = edgeId;
		this.weight = weight;
		this.pheromone = STARTPHEROMONE;
		this.paths = null;
	}

	public GeneEdge(String edgeId, double pheromone, double weight) {
		this.edgeId = edgeId;
		this.pheromone = pheromone;
		this.weight = weight;
		this.paths = null;
	}

	public GeneEdge() {
		this(nextIdString());
	}

	private static String nextIdString() {
		return "UNASSIGNED-" + nextId++;
	}

	public double getPheromone() {
		return pheromone;
	}

	public void setPheromone(double pheromone) {
		this.pheromone = pheromone;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public String getEdgeId() {
		return edgeId;
	}

	@Override
	public String toString() {
		return edgeId;
	}

	public Object getPaths() {
		return paths;
	}

	public void setPaths(Set paths) {
		this.paths = paths;
	}

}
