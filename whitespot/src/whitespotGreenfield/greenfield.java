package whitespotGreenfield;

import java.io.FileWriter;

public class greenfield {
	
	public static void main(String[] args)
			throws Exception {
		
		boolean PLZ5=false;
		boolean microm=false;
		
		long time = System.currentTimeMillis();
		
		int numberlocations=10;
		int weightCom = 100;
		int weightCrit =0;
		int threshold =30;
		
		//create FileWriter
		FileWriter output =FunctionsCommon.createFileWriter();
		
		//init variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		//set startLocations
		FunctionsCommon.initLocationContainer();
		
		int numberpolygons = FunctionsCommon.initialisation(numberlocations, true, PLZ5, microm);
		
		//allocated Polygons
		FunctionsGreenfield.allocatePolygonsGreenfield(numberpolygons, numberlocations, PLZ5);
		
		//check whether all polygons are allocated
		FunctionsGreenfieldWhitespot.checkAllocation(numberpolygons, numberlocations, PLZ5, weightCom, weightCrit);
		
		//set Locations
		FunctionsGreenfield.calculateGreenfieldLocations(numberpolygons, numberlocations, PLZ5);
		
		weightCom = 10;
		weightCrit =90;
		threshold =50;
		
//		FunctionsGreenfieldWhitespot.resetAllocations(numberpolygons, numberlocations);
		
//		FunctionsCommon.areaSegmentation(numberpolygons, numberlocations, PLZ5, microm, threshold, weightCom, weightCrit, true, 0);
	
		FunctionsCommon.checkThreshold(numberpolygons, numberlocations, threshold, microm, PLZ5, weightCom, weightCrit, true, 0);
		//set endLocations
		FunctionsGreenfield.calculateGreenfieldLocations(numberpolygons, numberlocations, PLZ5);
		
		//writeLocations
		FunctionsCommon.createFileWriterLocs(numberlocations);
		
		FunctionsCommon.visualizeResults(numberpolygons, numberlocations, output);
		
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");

		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
	}
}
