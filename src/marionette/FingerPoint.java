package marionette;

import org.opencv.core.Point;

public class FingerPoint extends Point implements Comparable<FingerPoint>{
	int angleTip;
	String fingerName;
	
	public FingerPoint(int finger_x, int finger_y){
		super.x = finger_x;
		super.y = finger_y;
	}
	
	public void setAngle(int offsetAngleTip){
		angleTip = offsetAngleTip;
	}
	
	public int getAngle(){
		return angleTip;
	}
	
	public void setFingerName(String fingerName){
		this.fingerName = fingerName;
	}
	
	public String getFingerName(){
		return fingerName;
	}

	@Override
	public int compareTo(FingerPoint obj) {
		FingerPoint other = (FingerPoint)obj;
		
		if(angleTip < other.angleTip){
			return -1;
		}
		
		else if(angleTip > other.angleTip){
			return 1;
		}
		
		else{
			return 0;
		}
	}
}
