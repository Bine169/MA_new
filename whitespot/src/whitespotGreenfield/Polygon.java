package whitespotGreenfield;

import java.util.ArrayList;
import java.util.List;

public class Polygon {

	private Integer id;
	private String geometry;
	private Double criteria;
	private Double area;
	private Double circumference;
	private List<Polygon> neighbour;
	private List<Double>sharedCircumferences = new ArrayList<Double>();
	private Location allocatedLocation;
	private Boolean haveAllocatedLocation;
	private Double[] centroidPoint= new Double[2];
	private Double distance;
	
	public Location getAllocatedLocation(){
		return this.allocatedLocation;
	}
	
	public double getArea(){
		return this.area;
	}
	
	public Double[] getCentroid(){
		return this.centroidPoint;
	}
	
	public Double getCircumference(){
		return this.circumference;
	}
	
	public Double getCircumferenceShared(int i){
		return this.sharedCircumferences.get(i);
	}
	
	public Double getCriteria(){
		return this.criteria;
	}
	
	public Double getDistance(){
		return this.distance;
	}
	
	public Boolean getFlagAllocatedLocation(){
		return this.haveAllocatedLocation;
	}
	
	public int getId(){
		return this.id;
	}
	
	public String getGeometry(){
		return this.geometry;
	}
	
	public int getLocationId(){
		Location loc = getAllocatedLocation();
		return loc.getId();
	}
	
	public List<Polygon> getNeighbours(){
		return neighbour;
	}
	
	public void removeAllocatedLocation(){
		this.allocatedLocation=null;
		this.haveAllocatedLocation=false;
	}
	
	public void setAllocatedLocation(Location loc){
		this.allocatedLocation=loc;
		setFlagAllocatedLocation(true);
	}
	
	public void setArea(double area){
		this.area=area;
	}
	
	public void setCentroid(double lon, double lat){
		this.centroidPoint[0]=lon;
		this.centroidPoint[1]=lat;
	}
	
	public void setCircumference(double circum){
		this.circumference=circum;
	}
	
	public void setCircumferenceshared(double circum){
		this.sharedCircumferences.add(circum);
	}
	
	public void setCriteria(double crit){
		this.criteria=crit;
	}
	
	public void setDistance(double dist){
		this.distance=dist;
	}
	
	public void setFlagAllocatedLocation(Boolean flag){
		this.haveAllocatedLocation=flag;
	}
	
	public void setGeometry(String geom){
		this.geometry=geom;
	}
	
	public void setId(int id){
		this.id=id;
	}
	
	public void setNeighbours(List<Polygon> neighbours){
		neighbour=neighbours;
	}
	

	
}
