package whitespotGreenfield;

import java.io.FileWriter;

public class Whitespot {
	
	public static void main(String[] args)
			throws Exception {
		
		boolean PLZ5=true;
		boolean microm=false;
		
		long time = System.currentTimeMillis();
		
		int numberGivenLocations = 10;
		int numberNewLocations = 1;
		int numberlocations=numberGivenLocations+numberNewLocations;
		int weightCom = 100;
		int weightCrit =0;
		int threshold =50;
		
		//create FileWriter
		FileWriter output =FunctionsCommon.createFileWriter();
		
		//init variables
		double[] criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}

		
		//set startLocations
		FunctionsCommon.initLocationContainer();
		FunctionsCommon.setLocations(numberGivenLocations, microm);
		
		//determine HomePoly of given Locations
		FunctionsCommon.determineHomePoly(PLZ5, numberGivenLocations, microm);
		
		int numberpolygons=FunctionsCommon.initialisation(numberlocations, true, PLZ5, microm);
		
		//allocated Polygons
		FunctionsWhitespot.allocatePolygonsWhitespot(numberpolygons, numberGivenLocations, numberNewLocations, PLZ5);
		
		FileWriter output2 =FunctionsCommon.createFileWriter();
		 FunctionsCommon.writePolygon(output2, numberpolygons);
		 output2.close();
		 
		//check whether all polygons are allocated
		 FunctionsGreenfieldWhitespot.checkAllocation(numberpolygons, numberlocations, PLZ5, weightCom, weightCrit);
		
		//set Locations from new created ones
		FunctionsWhitespot.calculateWhitespotLocations(numberpolygons, numberGivenLocations,numberNewLocations, PLZ5);
		
		 output2 =FunctionsCommon.createFileWriter();
		 FunctionsCommon.writePolygon(output2, numberpolygons);
		 output2.close();
		 
		//init variables
		criteria = new double[numberlocations];
		for (int i=0; i<numberlocations;i++){
			criteria[i]=0;
		}
		
		//writeLocations
		FunctionsCommon.createFileWriterLocs(numberlocations);
		
		//reset Allocation
		FunctionsGreenfieldWhitespot.resetAllocations(numberpolygons, numberlocations);
		
		weightCom = 100;
		weightCrit =00;
		
		FunctionsCommon.areaSegmentation(numberpolygons, numberlocations, PLZ5, microm, threshold, weightCom, weightCrit, true, numberGivenLocations);
		
		//set endLocations
		FunctionsWhitespot.calculateWhitespotLocations(numberpolygons, numberGivenLocations,numberNewLocations, PLZ5);
		
		//writeLocations
		FunctionsCommon.createFileWriterLocs(numberlocations);
		
		FunctionsCommon.visualizeResults(numberpolygons, numberlocations, output);
		
		System.out.println("Time for whole algorithm:"+(System.currentTimeMillis()-time)+" ms");

		output.flush();
		output.close();
	    
	    System.out.println("successfully ended");
	}
}
