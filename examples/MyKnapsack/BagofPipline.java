package MyKnapsack;

import java.util.ArrayList;

public class BagofPipline extends ArrayList<Pipeline>{
	double deadlinebop = 0.0;
	ArrayList<ArrayList<Pipeline>> bop;
	String key;
	long runTimeBop  = -1;
	int index = 0;
	public void setDeadlineBop(double dp) {
		this.deadlinebop = dp;
	}
	public void putPipeInbag(ArrayList<Pipeline> bop) {
		this.bop.add(bop);
	}
	public ArrayList<ArrayList<Pipeline>> getBop() {
		return this.bop;
	}
	public void setKey(String k) {
		key = k;
	}	
	public String getKey() {
		return key;
	}
	public double getDeadlineBop() {
		 return this.deadlinebop ;
	}
	public long getRunTimeBop() {
		return runTimeBop;
	}
	public void setRunTimeBop(long runTimeBop) {
		this.runTimeBop = runTimeBop;
	}
	
	public void RemoveFromBop(Pipeline p) {
		if(this.contains(p)) {
			this.remove(p);
		}
	}
	/**
	 * get Next Pipeline from bop
	 * @return
	 */
	public Pipeline getNextReadyPipelineFromBop() {
		if(!this.isEmpty()) {
			 return this.get(0);
		}
		else
			return null;
	}
//	public Pipeline getNextReadyPipelineFromBop() {
//		int i= 0;
//		Pipeline p =  null;
//		if(!this.isEmpty()) {
//			p = this.get(i);
//		}
//		return p;
//	}
}
