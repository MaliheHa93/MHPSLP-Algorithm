package MyKnapsack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModelFull;

import MyKnapsack.Pipeline;
import Cloudsim.workflow;
import MyKnapsack.MyTask;

public class MyTask extends Cloudlet{

	public static final int STATUS_WAITING = 1;
	public static final int STATUS_READY = 2;
	public static final int STATUS_SCHEDULED = 3;
	public static final int STATUS_EXECUTION = 4;
	public static final int STATUS_FAILIER = 5;
	public static final int STATUS_SUCCESS = 6;

	public static final int FAIL_DEADLINE = 1;
	public static final int FAIL_HARDWARE_PROBLEM = 2;
	public static final int FAIL_VM_TERMINATED_BY_USER = 3;	
	boolean isUpdateDeadline = false;
	public double extraTime = 0;
	/**depth of every task in W**/
	private int depth;
	/****/
	public String name;
	String id;
	double taskruntime = -1.0;
	double taskCloudletLength = -1.0;
	/**/
	private int brokerId;

	/**list of all parent of task**/
	private ArrayList<MyTask> childList;

	/**list of all child of task**/
	private ArrayList<MyTask> parentList;

	/**list of data input and output of task**/
	public  List<org.cloudbus.cloudsim.File> fileList;

	private ArrayList<MyTask> taskList;

	/**earliest finish time of task**/
	private double eft = -1;
	
	/**latest finish time of task**/
	private double lft = -1;
	
	/**latest start time of task**/
	private double lst = -1;
	
	/**earliest start time of task**/
	private double est = -1;
	
	/**sub deadline of task**/
	private double subDeadline;

	/**pipeline**/
	Pipeline pipeline;

	/***bag of task*/
	BagofTask bot;

	/**spareTime of task**/
	private double sparetime;

	public double StartTime;

	public double executionTime;
	
	public double minRunTime;
	
	List<File> dataDependencies;

	public boolean isReschedule = false;
	
	public boolean isSubmmited;
	public boolean SubmmitedAgain;
	
	public List<File> output;
	private double transferTime;
	/**constructor of class
	 * @param runTime **/
	public MyTask(int taskId, long length, double runTime, String id, String name) {
		super(taskId, length ,1,0,0, new UtilizationModelFull(),new UtilizationModelFull(),new UtilizationModelFull());
		taskruntime = runTime;
		taskCloudletLength = length;
		childList =  new ArrayList<>();
		parentList = new ArrayList<>();
		taskList = new ArrayList<>();
		sparetime = -1;
		this.id = id;
		this.name = name;
		StartTime = -1;
		setStatus(STATUS_WAITING);
		dataDependencies = new LinkedList<File>();
		output = new LinkedList<File>(); 
		subDeadline = -1;
		transferTime = -1;
	}

	/**set parent list of task**/
	public void setParentList(ArrayList<MyTask> parent) {
		this.parentList = parent;
	}


	/**get list of parents of a task**/
	public ArrayList<MyTask> getParentList(){
		return parentList;
	}


	/**set child list of task**/
	public void setChildList(ArrayList<MyTask> childs) {
		this.childList = childs;
	}


	/**get child list of task**/
	public ArrayList<MyTask> getChildList(){
		return childList;
	}


	
	public double getLft() {
		return lft;
	}

	public void setLft(double lft) {
		this.lft = lft;
	}

	public double getEst() {
		return est;
	}

	public void setEst(double est) {
		this.est = est;
	}

	public double getTaskruntime() {
		return taskruntime;
	}

	public void setTaskruntime(double taskruntime) {
		this.taskruntime = taskruntime;
	}

	/**setDepth of tasks in workflow**/
	public void setDepth(int depth) {
		this.depth = depth;
	}


	/**detDepth of task in Workflow**/
	public int getDepth() {
		return depth;
	}
	
	/**
	 * get latest start time of task
	 * @return
	 */
	public double getLstTimeTask() {
		return lst;
	}

	/**
	 * set latest start time of task
	 * @param lst
	 */
	public void setLstTimeTask(double lst) {
		this.lst = lst;
	}

	/**put the data required of task in fileList**/
	public void setFileList(ArrayList<org.cloudbus.cloudsim.File> fileList) {
		this.fileList = fileList;
	}

	/**get fileList of task**/
	public List<org.cloudbus.cloudsim.File> getFileList(){
		return fileList;
	}

	/**add child of task**/
	public void addChild(MyTask child) {
		childList.add(child);
	}


	/**add parent of tasj**/
	public void addParent(MyTask parent) {
		parentList.add(parent);
	}


	/**set sub deadline of task**/
	public void setsubDeadline(double subdeadline) {
		this.subDeadline = subdeadline;
	}


	/**get deadline of task**/
	public double getsubDeadline() {
		return subDeadline;
	}

	public boolean isAssignSubDeadline() {
		if(this.getsubDeadline() != 0) {
			return true;
		}
		else {
			return false;
		}
	}

	/**set spareTime of task**/
	public void setspareTime(double sparetime) {
		this.sparetime = sparetime; 
	}


	/**get spareTime of task**/
	public double getspareTime() {
		return sparetime;
	}


	/**get level of task**/
	public int getLevel() {
		return getDepth()+1;
	}


	/**if task belong to pipeline the put in pipeline**/
	public void putInPipeline(Pipeline pipeline) {
		if (this.pipeline == null) {
			pipeline.add(this);
			this.pipeline = pipeline;	
		}
	}


	/**
	 * if a task belong to bag of task then put in bot
	 * @param bot
	 */
	public void putInBot(BagofTask bot) {
		if (this.getBot() == null) {
			bot.add(this);
			this.bot = bot;
		}
//		else if(this.isSubmmited && this.isReschedule) {
//			bot.add(this);
//			this.bot = bot;
//		}
	}
	
	public void putInTable(ArrayList<MyTask> arrayList) {
		if(!this.taskList.contains(this)) {
			arrayList.add(this);
			this.taskList = arrayList;
		}		
	}
	//	/**
	//	 * get bag of task of that task belong to in
	//	 * @return
	//	 */
	//	public BagofTask getBop(){
	//		return this.bop;	
	//	}	

	/**
	 * get bag of task of that task belong to in
	 * @return
	 */
	public BagofTask getBot(){
		return this.bot;	
	}	
	/**
	 * if task belong to bag of tasks return true
	 * @return
	 */
	public boolean inBot() {
		if (this.getBot() != null) {
			return true;
		}
		return false;
	}

	/**
	 * get pipeline of that task
	 * @return
	 */
	public Pipeline getPipeline() {
		return this.pipeline;
	}

	/**
	 * if task belong to pipeline return true
	 * @return
	 */
	public boolean inPipe() {
		if (this.getPipeline() != null) {
			return true;
		}
		return false;
	}

	/**
	 * get key of task + get depth
	 * @return
	 */
	public String getKey() {
		return getNameTask() + " " + getDepth();
	}


	private String getNameTask() {
		return this.name;
	}

	/**
	 * get key of pipeline
	 * @param pipeline
	 * @return
	 */
	public String getKey(ArrayList<MyTask> pipeline) {
		String key =  null;
		for(int i = 0; i < pipeline.size(); i++) {
			key  += pipeline.get(i).getKey();
		}
		return key;
	}

	public double getEstimatedRunTime(double vmMips) {
		double RunTime = (double)(getTaskruntime()) * (VMOffersGoogle.getMips(VMOffersGoogle.n1_standard_1) / vmMips);
		return RunTime;
	}

	public void setEft(double current_eft) {
		this.eft = current_eft;
	}

	public double getEft() {
		return eft;
	}
	/**
	 * 
	 */
	public void updateTaskStatus() {
		if(getStatus() == STATUS_WAITING) {
			boolean setToReady = true;
			for(int i = 0; i < parentList.size() ; i++) {
				MyTask parent = parentList.get(i);
				if(parent.getStatus()!= STATUS_SUCCESS) {
					setToReady  = false;
					break;
				}
			}
			if(setToReady) {
				setStatus(STATUS_READY);
			}
		}	
	}

	public int getBrokerId() {
		return brokerId;
	}

	public void setBrokerId(int brokerId) {
		this.brokerId = brokerId;
	}

	public double getExecStartTime() {
		return StartTime;
	}

	public void setExecStartTime(double StartTime) {
		this.StartTime = StartTime;
	}

	public List<File> getDataDependencies() {
		return dataDependencies;
	}

	public void addDataDependencies(File inputFile ) {
		this.dataDependencies.add(inputFile) ;
	}

	public List<File> getOutput() {
		return output;
	}

	public void addOutput(File output) {
		this.output.add(output) ;
	}

	/**
	 * get the time take to executing task
	 * @return
	 */
	public double getExecutionTimeTask() {
		return executionTime; 
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setExecutionTimeTask(double executionTime) {
		this.executionTime = executionTime;
	}

	public double getTaskCloudletLength() {
		return taskCloudletLength;
	}

	public void setTaskCloudletLength(double taskCloudletLength) {
		this.taskCloudletLength = taskCloudletLength;
	}

	/**
	 * 
	 * @param vmMips 
	 * @param vmMips
	 * @return
	 */
	public double gettotalRunTime() {
		return (getTaskruntime() * (VMOffersGoogle.getMips(VMOffersGoogle.n1_standard_1))/ (VMOffersGoogle.getMips(VMOffersGoogle.n1_standard_8))) + getTransferTime();
	}

	private void setTransferTime(double sumTransferTime) {
		this.transferTime  = sumTransferTime;
		
	}

	public double getTransferTime() {
		if(transferTime == -1) {
			double sumTransferTime = 0.0;
			double readTimeoutput = 0.0;
			for(org.cloudbus.cloudsim.File file: getDataDependencies()) {
				sumTransferTime += MyStorage.addFile(file);
			}
			for(org.cloudbus.cloudsim.File file: getOutput()) {
				readTimeoutput += MyStorage.getTransferTime(file.getSize());
			}
			this.setTransferTime(sumTransferTime + readTimeoutput);
		}
		return transferTime;
	}

//	public void setFailReason(int reason) {
//		this.failReason = reason;
//
//	}
	@Override
	public String toString() {

		String toString = "";	
		toString += " task id: " + getCloudletId();	
		switch (getStatus()) {
		case STATUS_WAITING:
			toString+= " status: waiting";
			break;
		case STATUS_READY:
			toString+= " status: ready";
			break;
		case STATUS_SCHEDULED:
			toString+= " status: scheduled";
			break;
		case STATUS_EXECUTION:
			toString+= " status: execution";
			break;
		case STATUS_SUCCESS:	
			toString+= " status: success";
			break;
		default:
			toString+= " status: --- ";
		}

		toString+= " start: " + getExecStartTime();	
		toString+= " finish: " + getFinishTime();		

		toString+= " length: " + getCloudletLength();
		toString+= " depth: " + getDepth();

		toString+= " subDeadLine: " + getsubDeadline();

		String parents = "";
		for(int i = 0;i < parentList.size();i++){
			parents+= parentList.get(i).getCloudletId()+"_";
		}

		String children = "";
		for(int i=0;i < childList.size();i++){
			children+= childList.get(i).getCloudletId()+ "_";
		}

		toString+= " parents: "+parents;
		toString+= " children: "+children;
		
		toString+= " Reschedule again is: " + SubmmitedAgain;
		toString+= " delayed task: " + isReschedule;
		return toString;
	}

	public void setMinRunTime(double minRunTime) {
		this.minRunTime = minRunTime;	
	}

	public double getMinRunTime() {
		return minRunTime;
	}
	
	

//	public double getExtraTime() {
//		return extraTime;
//	}
//	/**
//	 * when each task early than subdeadline then distribute spare task and store in extraTime in task
//	 * @param extraTime
//	 */
//	public void setExtraTime(double extraTime) {
//		this.extraTime += extraTime;
//	}
}
