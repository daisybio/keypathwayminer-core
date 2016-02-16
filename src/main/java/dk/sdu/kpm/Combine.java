package dk.sdu.kpm;

import dk.sdu.kpm.gui.tree.DataSetNode;

import java.util.List;

/**
 *
 * @author nalcaraz
 */
public enum Combine {
    OR, AND, CUSTOM;
    
    public String getLogicalClause(List<DataSetNode> nodes) {
    	String sep = " " + this.toString() + " ";
    	String returnString = "";
    	for (int i=0; i < nodes.size() - 1; i++) {
    		returnString += (nodes.get(i).getExternalName() + sep);
    	}
    	returnString += nodes.get(nodes.size() - 1).getExternalName();
    	return returnString;
    }
}