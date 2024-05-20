package MyKnapsack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

public class MyVm extends Vm {

	//private MyStorage storage = new MyStorage(100000.0);
	public static final int VM_INITIATING = 1;
	public static final int VM_IDLE = 2;
	public static final int VM_BUSY = 3;
	public static final int VM_TERMINATED = 4;
	
	private MyTask submittedTask;
	private int state;
	private double execStartTime;
	private double VM_InitiatedTime;
	private double executionFinishEventTime;
	public int brokerId; 
	public double costPerMinute;
	public double endTimeSlot;
	public static Hashtable<Integer,ArrayList<MyTask>> assingedTaskVmMap = new Hashtable<>();
	public int numTimeSlot;
	public ArrayList<MyTask> assignList = new ArrayList<>() ; 
	public ArrayList<Pipeline> assignPipeline = new ArrayList<Pipeline>() ; 
	public static Hashtable<Integer,ArrayList<Pipeline>> assingedPipeVmMap = new Hashtable<>();
	public List<MyTask> queueTasks = new ArrayList<MyTask>();
	public List<Pipeline> queuePipeline = new ArrayList<Pipeline>();
	public double vmFinishTime = -1;
	public double vmCreateTime = -1;
	public double remainingTimeSlot;
	public boolean submitTaskflag;
	public boolean submitPipelineFlag;
	public int numberTask ;
	public int numberPipeline; 
	double[] completeTime ;
	double CostVmUsed;
	private double nextEndTimeslot;
	String[] tasksSequence;
	/**constructor of class
	 * @param d 
	 * @param i 
	 * @param d 
	 * @param object 
	 * @param string 
	 * @param k 
	 * @param j 
	 * @param i 
	 * @param d **/
	public MyVm(int VmId,int userid, double mips, int numberOfPes, int ram, long bw, long size,String vmm,
			CloudletScheduler cloudletScheduler) {
		super(VmId,0,mips, numberOfPes, ram, 10000, size, "xen", new CloudletSchedulerSpaceShared());
		
		this.submittedTask = null;
		this.state = VM_INITIATING;
		this.execStartTime = -1;
		this.executionFinishEventTime =-1;
		this.VM_InitiatedTime = -1;
		this.remainingTimeSlot = -1;
		costPerMinute = -1;
		numberTask = -1;
		numberPipeline = -1;
		tasksSequence = new String[1000];
		for(int i = 0; i<1000;i++) {
			tasksSequence[i] = " ";
		}
		submitTaskflag = false;
		submitPipelineFlag = false;
		}
	
	/**set state of vm**/
	public void setState(int state) {
		this.state = state;
	}
	
	/**get state of vm**/
	public int getState() {
		return state;
	}
	
	/**
	 * if task submit in vm return true 
	 * @param task
	 * @return
	 */
	public boolean SubmitTask(MyTask task) {
		if(this.state == VM_IDLE && this.submittedTask == null) {
			 resetExecutionValue();			
			 this.submittedTask = task;
			 this.state = VM_BUSY;
			 this.execStartTime = CloudSim.clock();			 
			 submittedTask.setExecStartTime(execStartTime);

			 return true;
		 }
		 return false;
	}
	
	/**
	 * get submitted task in vm
	 **/
	public MyTask getsubmittedTask() {
		return submittedTask;
	}
	
	/**
	 * if execution of task in vm completed then execution successful
	 * 
	 **/
	static double sumRunTime = 0.0;
	public MyTask ProcessTaskcompleted() {		
		submittedTask.setStatus(MyTask.STATUS_SUCCESS);
		submittedTask.setFinishTime(submittedTask.getExecStartTime()+ submittedTask.getEstimatedRunTime(getMips()));
		/*calculate transfer time of task*/		
		MyTask t = submittedTask;
		sumRunTime += t.getTaskruntime();
		tasksSequence[this.getId()] += " id " + submittedTask.getCloudletId() + " StartTime " + execStartTime +" finish "+ executionFinishEventTime;
		resetExecutionValue();

		this.state = VM_IDLE;
		return t;
	}
	
	/**
	 * set initiatedTime of vm
	 * **/
	public void setInitiatedTimeVm(double initiatetime) {
		this.VM_InitiatedTime = initiatetime;
	}
	
	/**get initiated time of Vm**/
	public double getVmInitiatedTime() {
		return VM_InitiatedTime;
	}
	
	/**get runTime of vm**/
	public double getRunTime() {
		return CloudSim.clock() - this.VM_InitiatedTime;
	}

	/**reset all variable for next submition**/
	public void resetExecutionValue() {
		submittedTask = null;
		this.execStartTime = 0;
		this.executionFinishEventTime = 0;
	}
	 
	/**
	 * get remaining time slot of vm until 1 hour
	 * @return
	 */
	public double getRemainingTimeSlot() {
		return this.remainingTimeSlot;
	}
	public double Time2BillingPeriod() {
		double time = (CloudSim.clock() - VM_InitiatedTime)/60;
		time = Math.ceil(time)-time;
		return time * 60 ;
	}
	
	public int getBrokerId() {
		return brokerId;
	}

	public void setBrokerId(int brokerId) {
		this.brokerId = brokerId;
	}
	
	public void setCostPerMinute(double cost) {
		this.costPerMinute = cost;
	}
	
	public double getCostPerMinute() {
		return this.costPerMinute;
	}
	
	/**
	 * 
	 * @return
	 */
	public double execute() {	
		double trafnserTime = 0;
		if(submittedTask != null) {
			double execTime =  submittedTask.getEstimatedRunTime(getMips());
			if(submittedTask.inBot()) {
				trafnserTime = submittedTask.getTransferTime();
				submittedTask.setExecutionTimeTask(execTime + trafnserTime);			
			}
			else {				
				submittedTask.setExecutionTimeTask(execTime);
			}			
			executionFinishEventTime = execStartTime + execTime + trafnserTime;	

			return executionFinishEventTime;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * 
	 * @param assigntask
	 */
	public void setAssingedTask(MyTask assigntask) {
		
		List<MyTask> assignlist = new ArrayList<MyTask>();
		/*why i consider this condition?*/
		//if(assigntask.getStatus() == MyTask.STATUS_READY) {
			assignlist.add(assigntask);
			this.assignList.add(assigntask);
			assingedTaskVmMap.put(this.getId(),assignList);
		//}		
	}

	/**
	 * 
	 * @param vmid 
	 * @return
	 */
	public ArrayList<MyTask> getAssingedTaskList() {
		if(assingedTaskVmMap.keySet().contains(this.getId())) {
			if(!assingedTaskVmMap.get(this.getId()).isEmpty()) {
				assignList = assingedTaskVmMap.get(this.getId());
				return assignList;
			}				
		}	
		return assignList;
	}
	/**
	 * 
	 * @param assigntask
	 */
	public void setAssingedPipe(Pipeline p) {
		this.assignPipeline.add(p);
		assingedPipeVmMap.put(this.getId(), assignPipeline);
	}

	/**
	 * 
	 * @param vmid 
	 * @return
	 */
	public ArrayList<Pipeline> getAssingedPipelineList() {
		if(assingedPipeVmMap.keySet().contains(this.getId())) {
			if(!assingedPipeVmMap.get(this.getId()).isEmpty()) {
				assignPipeline = (assingedPipeVmMap.get(this.getId())) ;
				return assignPipeline;
			}				
		}
		return assignPipeline;
	}
	/**
	 * 
	 * @return
	 */
	public Hashtable<Integer, ArrayList<MyTask>> getAssingedTasktable() {
		return assingedTaskVmMap;
	}

	public double getExecutionFinishEventTime() {
		return executionFinishEventTime;
	}

	public void setExecutionFinishEventTime(double executionFinishEventTime) {
		this.executionFinishEventTime = executionFinishEventTime;
	}
	public double getVmFinishTime() {
		return vmFinishTime;
	}

	public void setVmFinishTime(double vmFinishTime) {
		this.vmFinishTime = vmFinishTime;
	}

	public void setRemainingTimeSlot(double d) {
		this.remainingTimeSlot = d;		
	}
	
//	public MyTask executeTaskBeforeTerminate(double time, int reason) {
//		if(submittedTask != null) {
//			double availabetime = time - execStartTime;
//			double execTimeMin = submittedTask.getCloudletLength() / this.getMips();
//			if(execTimeMin > availabetime) {
//				submittedTask.setStatus(MyTask.STATUS_FAILIER);
//				submittedTask.setFailReason(reason);
//			}
//			else {
//				submittedTask.setStatus(MyTask.STATUS_SUCCESS);
//			}
//			submittedTask.setFinishTime(time);
//			MyTask temp = submittedTask;
//			resetExecutionValue();
//			return temp;
//		}
//		resetExecutionValue();
//		return null;
//	}

	public void setNumberTaskAssign(int NT) {
		this.numberTask = NT;
	}
	public int getNumberTaskAssign() {
		return this.numberTask;
	}
	
	public boolean isAssinedTask() {
		if(submittedTask != null) {
			return true;
		}
		return false;
	}
	public void setnumberPipelineAssign(int NT) {
		this.numberPipeline = NT;
	}
	
	public int getnumberPipelineAssign() {
		return this.numberPipeline;
	}

	public double getVmStartTime() {
		return vmCreateTime;
	}

	public void setVmStartTime(double vmCreateTime) {
		this.vmCreateTime = vmCreateTime;
	}

	public double[] getCompleteTime() {
		return completeTime;
	}

	public void setCompleteTime(double[] completeTime) {
		this.completeTime = completeTime;
	}

	public double getEndTimeSlot() {
		return endTimeSlot;
	}

	public void setEndTimeSlot(double endtime) {
		this.endTimeSlot = endtime;
	}
	public void setNextStartTimeslot(double time) {
		this.nextEndTimeslot = time;
	}
	public double getNextStartTimeslot() {
		return nextEndTimeslot;
	}
	public void setNumberTimeSlot(int numTimeSlot) {
		this.numTimeSlot = numTimeSlot;	
	}
	public int getNumberTimeSlot() {
		return numTimeSlot;		
	}

	public double getCostVmUsed() {
		return CostVmUsed;
	}

	public void setCostVmUsed(double costVmUsed) {
		CostVmUsed = costVmUsed;
	}
	
	public void addTaskinQueue(MyTask tq) {
		queueTasks.add(tq);
	}
	
	public int numTaskinQueue() {
		int num =-1;
		num = queueTasks.size();
		return num;
	}
	
	public void addPipeinQueue(Pipeline pq) {
		queuePipeline.add(pq);
	}
	
	public int numPipeinQueue() {
		int num =-1;
		num = queuePipeline.size();
		return num;
	}
	
	public List<MyTask> getQueueTask(){
		return queueTasks;
	}
	
	public List<Pipeline> getQueuePipeline(){
		return queuePipeline;
	}
	
	public List<MyTask> getSortedQueueList(){
		Collections.sort(queueTasks, new Comparator<MyTask>() {

			@Override
			public int compare(MyTask t1, MyTask t2) {
				double diff = t1.getsubDeadline() - t2.getsubDeadline();
				if (diff > 0)
					return 1;
				else if (diff < 0)
					return -1;
				else return 0;
			}
		});
		return queueTasks;
	}
}