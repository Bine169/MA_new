package whitespotGreenfield;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class AreaSegmentation {
	
	public static void main(String[] args)
			throws Exception {
		
		boolean PLZ5=true;
		boolean common=true;
		boolean microm=false;
		
		long time = System.currentTimeMillis();
		int numberlocations=10;
		FunctionsCommon.setLocations(numberlocations, microm);
		
		//create FileWriter
		FileWriter output =FunctionsCommon.createFileWriter();
		FunctionsCommon.createFileWriterLocs(numberlocations);
		
		//init variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		//calculate number of Polygons in that region
		int numberpolygons=FunctionsCommon.initialisation(numberlocations, true, PLZ5, microm);
		
		
		int weightCom = 100;
		int weightCrit =00;
		int threshold =50;
		
		FunctionsCommon.areaSegmentation(numberpolygons, numberlocations, PLZ5, microm, threshold, weightCom, weightCrit);
		
		FunctionsCommon.visualizeResults(numberpolygons, numberlocations, output);
		
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");
//		
		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
		//welche Funktionen in functionsnew überhaupt noch notwendig? welche variablen?
		
	}
}
