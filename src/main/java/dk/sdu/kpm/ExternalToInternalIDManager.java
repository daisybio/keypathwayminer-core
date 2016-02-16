package dk.sdu.kpm;

import java.io.Serializable;
import java.util.*;

public class ExternalToInternalIDManager  implements Serializable{

	/**
	 * Maps a data sets external ID, as defined by the user, to its internal ID.
	 */
	private Map<String, String> externalToInternalIDMap = new HashMap<String, String>();

	/**
	 * Maps a data sets internal ID to its external ID, as defined by the user.
	 */
	private Map<String, String> internalToExternalIDMap = new HashMap<String, String>();

	/**
	 * Counter for the number of internal identifiers which have been created so
	 * far.
	 */
	private static int internalIDsCreated = 0;

	/**
	 * Prefix used to make the internal identifiers to valid Java identifiers.
	 */
	private final String internalPrefix = "L";

	/**
	 * 
	 * @param externalIdentifier
	 *            - An external identifier of a data set as given by the user.
	 * @return The internal identifier used for the given external identifier or
	 *         <code>null</code> if the internal identifier is not known.
	 */
	public String getInternalIdentifier(String externalIdentifier) {
		return externalToInternalIDMap.get(externalIdentifier);
	}
	
	public String getOrCreateInternalIdentifier(String externalIdentifier){
		String identifier = getInternalIdentifier(externalIdentifier);
		
		if(identifier == null){
			identifier = createInternalIdentifier(externalIdentifier);
		}
		
		System.out.println("identifier: " + identifier);
		
		return identifier;
	}

	/**
	 * 
	 * @param internalIdentifier
	 *            - An internal identifier of a data set.
	 * @return The external identifier used for the given internal identifier or
	 *         <code>null</code> if the internal identifier is not known.
	 */
	public String getExternalIdentifier(String internalIdentifier) {
		return internalToExternalIDMap.get(internalIdentifier);
	}

	/**
	 * 
	 * @param externalIdentifier
	 *            - An external identifier for a data set specified by a user.
	 * @return The identifier newly created for the given external identifier.
	 */
	public String createInternalIdentifier(String externalIdentifier) {
		int currentInternalID = ++internalIDsCreated;
		String newInternalID = internalPrefix + currentInternalID;
		externalToInternalIDMap.put(externalIdentifier, newInternalID);
		internalToExternalIDMap.put(newInternalID, externalIdentifier);
		return newInternalID;
	}

	/**
	 * Updates the mappings between the internal and external identifiers after
	 * modifying the external identifier.
	 * 
	 * @param internalID
	 *            - Old, unchanged internal identifier.
	 * @param newExternalName
	 *            - The new external identifier.
	 */
	public void updateExternalIdentifier(String internalID,
			String newExternalName) {
		internalToExternalIDMap.put(internalID, newExternalName);
		externalToInternalIDMap.remove(newExternalName);
		externalToInternalIDMap.put(newExternalName, internalID);
        }

	/**
	 * Removes the given external identifier from the mappings between internal
	 * and external identifiers.
	 * 
	 * @param externalName
	 *            - The user-specified name for a data set which is not being
	 *            used anymore.
	 */
	public void removeExternalIdentifier(String externalName) {
            if (externalToInternalIDMap.containsKey(externalName)) {
		String internalID = externalToInternalIDMap.get(externalName);
		externalToInternalIDMap.remove(externalName);
		internalToExternalIDMap.remove(internalID);
                internalIDsCreated = 0;
                internalToExternalIDMap = new HashMap<String, String>();
                for(String oldExternalName: externalToInternalIDMap.keySet()) {
                    String newInternalName = createInternalIdentifier(oldExternalName);
                    internalToExternalIDMap.put(newInternalName, oldExternalName);
                    externalToInternalIDMap.put(oldExternalName, newInternalName);
                }
            }
		
	}
        
        public List<String> getInternalIdentifiers() {
            List<String> ret = new ArrayList<String>(internalToExternalIDMap.keySet());
            Collections.sort(ret);
            return ret;
        }
        
        
        public List<String> getExternalIdentifiers() {
            List<String> ret = new ArrayList<String>(externalToInternalIDMap.keySet());
            Collections.sort(ret);
            return ret;
        }
}
