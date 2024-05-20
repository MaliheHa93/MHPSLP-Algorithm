package MyKnapsack;

import java.util.ArrayList;
import java.util.Iterator;

import org.cloudbus.cloudsim.Vm;

import MyKnapsack.MyTask;

public class Pipeline extends ArrayList<MyTask> {
	String key;
	public ArrayList<Pipeline> bop;
	private double deadlinep;
	public int idPipe = -1;
	public String pipeName;
	public double sumRunTimeTaskPipeline;
	public double sumTaskLength ;
	public int vmId = -1;
	public double sumPipeRunTime;
	private ArrayList<Pipeline> waitList = new ArrayList<>();
	boolean isset;
	boolean isreschedule = false;
	double transferTime = 0;
	public void setKey(String k) {
		key = k;
	}	
	public String getKey() {
		return key;
	}
	public void setBop(ArrayList<Pipeline> bop) {
		this.bop = bop;
	}
	public void setDeadline(double deadlinep) {
		this.deadlinep = deadlinep;
	}

	/**
	 * put pipeline in bag 
	 * @param p
	 */
	public void putInBag(ArrayList<Pipeline> p) {
		if(this.bop == null) {
			p.add(this);
			this.bop = p;
		}
	}

	public double getDeadline() {
		return this.deadlinep;
	}

	public int getIdPipe() {
		return idPipe;
	}
	public void setIdPipe(int idPipe) {
		this.idPipe = idPipe;
	}

	public String getPipeName() {
		return pipeName;
	}
	public void setPipeName(String pipeName) {
		this.pipeName = pipeName;
	}

	public double getSumRunTimeTaskPipeline() {
		return sumRunTimeTaskPipeline;
	}
	public void setSumRunTimeTaskPipeline(double sumRunTimeTaskPipeline) {
		this.sumRunTimeTaskPipeline = sumRunTimeTaskPipeline;
	}
	/**
	 * 
	 * @return
	 */
	public BagofPipline getBop() {
		return (BagofPipline) this.bop;	 
	}

	public int getVmId() {
		return vmId;
	}
	public void setVmId(int vmId) {
		this.vmId = vmId;
	}

	/**
	 * if all of task in pipeline executed then remove pipeline from list of vm
	 * @return
	 */
	public  boolean isAllTaskSubmitted() {
		boolean taskSubmitted[] = new boolean[this.size()];
		boolean flag = true;
		for(int i = 0 ; i < this.size() ; i++) {
			if(this.isSet()) {
				taskSubmitted[i] = true;
			}
		}
		for(int j = 0; j < taskSubmitted.length; j++){
			if(!taskSubmitted[j]) {
				flag = false;
			}
		}
		return flag;
	}
	public double getSumTaskLength() {
		return sumTaskLength;
	}
	public void setSumTaskLength(double sumTaskLength) {
		this.sumTaskLength = sumTaskLength;
	}
	public double getSumPipeRunTime() {
		return sumPipeRunTime;
	}
	public void setSumPipeRunTime(double sumPipeLength) {
		this.sumPipeRunTime = sumPipeLength;
	}
	public void removeCompleteTask(MyTask t) {
		Iterator<MyTask> p = this.iterator();
		while(p.hasNext()) {
			if(p.next() == t) {
				p.remove();
			}
		}
	}
//	public  boolean isoneSuccessTask() {
//		boolean taskExecuted[] = new boolean[this.size()];
//		boolean flag = false;
//		for(int i = 0 ; i < this.size() ; i++) {
//			if(this.get(i).getStatus() == MyTask.STATUS_SUCCESS) {
//				taskExecuted[i] = true;
//			}
//		}
//		for(int j = 0; j < taskExecuted.length; j++){
//			if(taskExecuted[j]) {
//				flag = true;
//			}
//		}
//		return flag;
//	} 

//	public boolean hasUnexecutedTask() {
//		boolean taskExecuted[] = new boolean[this.size()];
//		boolean flag = false;
//		for(int i = 0 ; i < this.size() ; i++) {
//			if(this.get(i).getStatus() == MyTask.STATUS_WAITING) {
//				taskExecuted[i] = true;
//			}
//		}
//		for(int j = 0; j < taskExecuted.length; j++){
//			if(taskExecuted[j]) {
//				flag = true;
//				break;
//			}
//		}
//		return flag;
//	}

	public MyTask getReadyTaskFromPipeline() {
		//int i = 0;
		MyTask temp = null;
		if(!this.isEmpty()) {
			for(MyTask t: this) {
				if(t.getStatus() == MyTask.STATUS_READY) {
					temp = t;
				}	
			}			
		}
		return temp;
	}
	
	public boolean isSet() {
		if(this.getVmId() != -1) {
			isset= true;
		}
		return isset;
	}
	/**
	 * if all of task in pipeline executed then remove pipeline from list of vm
	 * @return
	 */
	public  boolean isAllTaskExecuted() {
		boolean taskExecuted[] = new boolean[this.size()];
		boolean flag = true;
		for(int i = 0 ; i < this.size() ; i++) {
			if(this.get(i).getStatus() == MyTask.STATUS_SUCCESS) {
				taskExecuted[i] = true;
			}
		}
		for(int j = 0; j < taskExecuted.length; j++){
			if(!taskExecuted[j]) {
				flag = false;
			}
		}
		return flag;
	}


	/**
	 * if task belong to bag of tasks return true
	 * @return
	 */
	public boolean inBop() {
		if (this.getBop() != null) {
			return true;
		}
		return false;
	}
	
	public double getEstimatedRunTime(double mips) {
		double RunTime = (getSumPipeRunTime()) * (VMOffersGoogle.getMips(VMOffersGoogle.n1_standard_1) / mips);
		return RunTime;
	}
	public double getTransferTime() {
		double sumTransferTime = 0.0;
		
		for(org.cloudbus.cloudsim.File file: this.get(this.size()-1).getDataDependencies()) {
			sumTransferTime += MyStorage.addFile(file);
		}
		transferTime = sumTransferTime ;
		return transferTime;
	}
}
