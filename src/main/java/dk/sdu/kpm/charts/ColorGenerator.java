package dk.sdu.kpm.charts;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;

public class ColorGenerator  implements Serializable {
	private Random random;
	
	private HashSet<String> usedColors;
	
	private Stack<Color> colors;
	
 	public ColorGenerator(){
 		this.usedColors = new HashSet<String>();
		this.random = new Random(3);
		this.colors = new Stack<Color>();
		for(int i = 0; i < 20; i++){
			this.colors.add(generatePastel());
		}
	}
	
	private Color generatePastel(){		
		int[] colorBytes = new int[3];
        colorBytes[0] = random.nextInt(128) + 127;
        colorBytes[1] = random.nextInt(128) + 127;
        colorBytes[2] = random.nextInt(128) + 127;
        
        String colorStr = String.format("%d%d%d", colorBytes[0],colorBytes[1],colorBytes[2]);
        if(!this.usedColors.contains(colorStr)){
        	this.usedColors.add(colorStr);
        	return new Color(colorBytes[0], colorBytes[1], colorBytes[2], 255);
        
        }else{
        	return generatePastel();
        }
	}
	
	public Color getNext(){
		if(!this.colors.empty()){
			return this.colors.pop();
		}else{
			return generatePastel();
		}
	}
}
