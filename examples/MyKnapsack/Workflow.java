package MyKnapsack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.math.BigDecimal;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import MyKnapsack.Pipeline;
import MyKnapsack.MyTask;

public class Workflow {

	private final static String tag = "Workflow Class: ";
	private double stricestdeadline;
	private double maxEftWorkflow;
	private double criticalPath = -1;
	public ArrayList<MyTask> tasklist = new ArrayList<>();
	private int[] arr;	
	private int MaxLevel;
	int wlevel;
	private HashMap<Integer, MyTask> parentsExecutedTasksMap;
	private HashMap<Integer, MyTask> taskToIdMap;
	private HashMap<Integer, MyTask> remainingTaskMap;
	private Hashtable<Integer, Integer> NumLevelTask;
	private Hashtable<Integer, Double> spareInEachLevel;
	private Hashtable<Integer, Double> sumRunTimeInEachLevel;
	private Hashtable<Integer, Double> maxRunTimeInEachLevel;
	public LinkedHashMap<Integer, ArrayList<MyTask>> NumTaskafterSchedule;
	Hashtable<Integer, Double> estLevel;
	Hashtable<Integer, Double> lftLevel;
	Hashtable<Integer, Integer> numVminLevel;
	ArrayList<Pipeline> pipeline;
	ArrayList<MyTask> waitingTasks;
	ArrayList<MyTask> entryTask;
	
	/*constructor*/
	public Workflow(ArrayList<MyTask> tasklist) {
		super();
		this.tasklist.addAll(tasklist);
		this.pipeline = new ArrayList<>();
		this.MaxLevel = 100;
		this.arr = new int[1000];
		entryTask = new ArrayList<>();
		parentsExecutedTasksMap = new HashMap<>();
		taskToIdMap = new HashMap<>();
		remainingTaskMap = new HashMap<>();
		NumTaskafterSchedule = new LinkedHashMap<>();
		estLevel = new Hashtable<>();
		lftLevel = new Hashtable<>();
		numVminLevel = new Hashtable<>();
		setNumLevelTask(new Hashtable<>());
		spareInEachLevel = new Hashtable<>();
		sumRunTimeInEachLevel = new Hashtable<>();
		maxRunTimeInEachLevel = new Hashtable<>();
		waitingTasks = new ArrayList<MyTask>(tasklist.size());

		for(MyTask task : tasklist) {
			remainingTaskMap.put(task.getCloudletId(), task);
			if(task.getParentList().size() == 0) {
				parentsExecutedTasksMap.put(task.getCloudletId(), task);
				entryTask.add(task);
			}
			task.updateTaskStatus();
			if(taskToIdMap.containsKey(task.getCloudletId())) {
				Log.printLine("error this task exists in list");
			}
			taskToIdMap.put(task.getCloudletId(), task);			
			waitingTasks.add(task);
		}		
	}

	/**
	 * set tasks list of workflow after parsing
	 * @param tlist
	 */
	public void setTaskList(ArrayList<MyTask> tlist) {
		for(int j = 0; j < tlist.size();j++) {
			this.tasklist.add(tlist.get(j));
		}
	}

	/**
	 * get tasks list of workflow
	 * @return
	 */
	public ArrayList<MyTask> getTaskList(){
		return tasklist;
	}

	public ArrayList<MyTask> getEntryTask() {
		return entryTask;
	}


	/**
	 * find pipeline in WorkFlow
	 */
	public ArrayList<Pipeline> identifyPipeline() {
		Pipeline p = null;
		String key = "";
		List<MyTask> ischeck = new ArrayList<MyTask>();
		for (MyTask t : tasklist) {
			if (!ischeck.contains(t)) {
				p = findPipline(t, new Pipeline(), ischeck);
				if (!p.isEmpty())
					pipeline.add(p);
				for (MyTask t2 : p) {
					key += t2.getKey() + ";";
				}
				p.setKey(key);
			}
			key = "";
		}
		return pipeline;
	}

	/**
	 * recursive method for find pipeline in workflow		
	 * @param t
	 * @param pipeline
	 * @param ischeck
	 * @return
	 */
	private MyKnapsack.Pipeline findPipline(MyTask t, MyKnapsack.Pipeline pipeline, List<MyTask> ischeck) {
		int cNum;
		ischeck.add(t);
		cNum = t.getChildList().size();
		if (cNum > 1 || cNum == 0 || t.getChildList().get(0).getParentList().size() > 1) {
			if (!pipeline.isEmpty())
				t.putInPipeline(pipeline);
			return pipeline;
		}
		t.putInPipeline(pipeline);
		return findPipline(t.getChildList().get(0), pipeline, ischeck);
	}

	/**
	 * calculate sub deadline of tasks			
	 * @param vmMips
	 * @param readyTasks 
	 */
	public boolean calculateSubDeadlineTasks(double vmMips) {
		double currentEft = -1;
		double maxEft = -1;
		/*for all tasks that unscheduled yet calculate EFT*/
		for (MyTask task : waitingTasks) {
			currentEft = calculateEftTask(task, vmMips);
			if(currentEft > maxEft) {
				maxEft = currentEft;
			}
		}
		Log.printLine("max eft of tasks " + maxEft);

		/*calculate spareTime if spare time is negative then return false and choose the fastest vm*/
		double spareTime = stricestdeadline -  maxEft;
		if (spareTime < 0)
			return false;

		/*divide spare time between tasks*/
		getDivideSpareTime(spareTime);
//		costBasedSpareTime(spareTime,vmMips);
		/*set deadline of tasks*/
		for(MyTask t : waitingTasks) {
			setDeadlineTask(vmMips , t);			
		}
		return true;
	}

	/**
	 * using simple lp for distribute spare time
	 * @param spareTime
	 * @param vmMips
	 */
	private void LpSpareTime(double spareTime ,double vmMips) {
		LpSpareSolve lps = new LpSpareSolve();
		double sum = 0.0;
		double[] maxRunTime = calculateMaxRunTime(vmMips);
		double[] numberTaskLevel = getNumberTaskInlevel();
		double[] sumRuntimeLevel = getSumRuntimeLevel(vmMips);
		for(int i = 0; i < sumRuntimeLevel.length;i++) {
			sum += sumRuntimeLevel[i];
		}
		ArrayList<MyTask> taskLevel ;

		spareResult sResult = lps.LpSolve(maxRunTime, numberTaskLevel, spareTime,sumRuntimeLevel);
		for(spareDistribution slt :sResult.spareList ) {
			int level = slt.Level;
			double spareTimeLevel = slt.spareTime;
			spareInEachLevel.put(level, spareTimeLevel);
		}
		for(Entry<Integer, ArrayList<MyTask>> entry : NumTaskafterSchedule.entrySet()) {
			taskLevel = new ArrayList<MyTask>();
			taskLevel.addAll(entry.getValue());
			double spare = spareInEachLevel.get(entry.getKey());
			for(MyTask t: taskLevel) {
				t.setspareTime(spare);
			}
		}		
	}

	private double[] getSumRuntimeLevel(double vmMips) {
		ArrayList<MyTask> taskLevel ;
		double[] sumRunTimeLevel = new double[wlevel+1];
		
		for(Entry<Integer, ArrayList<MyTask>> entry : NumTaskafterSchedule.entrySet()) {
			taskLevel = new ArrayList<MyTask>();
			double SumRunTimelevel = 0.0;
			taskLevel.addAll(entry.getValue());
			for(MyTask t: taskLevel) {
				SumRunTimelevel += t.getEstimatedRunTime(vmMips);
			}
			sumRunTimeLevel[entry.getKey()] = SumRunTimelevel;
		}
		return sumRunTimeLevel;
	}

	/**
	 * return an array in ascending orde of level in w
	 * @return
	 */
	private double[] getNumberTaskInlevel() {
		double[] numberTaskLevel = new double[wlevel+1];
		for(Entry<Integer, ArrayList<MyTask>> entry : NumTaskafterSchedule.entrySet()) {
			if(!entry.getValue().isEmpty()) {
				numberTaskLevel[entry.getKey()] = entry.getValue().size();
			}else {
				numberTaskLevel[entry.getKey()] = -1;
			}			
		}
		return numberTaskLevel;
	}

	/**
	 * retuen an array contains max runTime in each level in ascending order
	 * @param vmMips
	 * @return
	 */
	private double[] calculateMaxRunTime(double vmMips) {
		ArrayList<MyTask> taskLevel ;
		double[] maxRunTime = new double[wlevel+1];
		
		for(Entry<Integer, ArrayList<MyTask>> entry : NumTaskafterSchedule.entrySet()) {
			taskLevel = new ArrayList<MyTask>();
			double MaxRunTimelevel = -1;
			taskLevel.addAll(entry.getValue());
			if(!taskLevel.isEmpty()) {
				for(MyTask t: taskLevel) {
					if(t.getEstimatedRunTime(vmMips) > MaxRunTimelevel) {
						MaxRunTimelevel = t.getEstimatedRunTime(vmMips);
					}
				}
				maxRunTime[entry.getKey()] = MaxRunTimelevel;
			}
			else{
//				NumTaskafterSchedule.get(entry.getKey());
				maxRunTime[entry.getKey()] = MaxRunTimelevel;
			}
//						
		}
		return maxRunTime;
	}
	public boolean assignSubDeadlineTaskswithLpSpare(double vmMips) {
		double currentEft = -1;
		double maxEft = -1;
		/*for all tasks that unscheduled yet calculate EFT*/
		for (MyTask task : waitingTasks) {
			currentEft = calculateEftTask(task, vmMips);
			if(currentEft > maxEft) {
				maxEft = currentEft;
			}
		}
		Log.printLine("max eft of tasks " + maxEft);

		/*calculate spareTime if spare time is negative then return false and choose the fastest vm*/
		double spareTime = stricestdeadline -  maxEft;
		Log.printLine("spareTime " + spareTime);
		if (spareTime < 0)
			return false;

		/*divide spare time between tasks*/
		LpSpareTime(spareTime,vmMips);
		
		/*set deadline of tasks*/
		for(MyTask t : waitingTasks) {
			setDeadlineTask_SpareLP(vmMips , t);			
		}
		return true;
	}
	/**
		 * set sub deadline of  each task
		 * @param vmMips
		 * @param task
		 */
	private void setDeadlineTask_SpareLP(double vmMips, MyTask t) {
		double maxdeadline = CloudSim.clock();
		
		for(MyTask parent : t.getParentList()) {
			if(parent.getsubDeadline() == -1) {
				setDeadlineTask_SpareLP(vmMips, parent);
			}
			if(maxdeadline < parent.getsubDeadline())
				maxdeadline = parent.getsubDeadline();
		}
		t.setsubDeadline(maxdeadline + t.getEstimatedRunTime(vmMips) + t.getspareTime());
		Log.printLine("task: " + t.getCloudletId() + " _taskRunTime: " + t.getTaskruntime() +  " _EstimatedRuntime: " + t.getEstimatedRunTime(vmMips) + " _eftTask: " + t.getEft() + " _maxDeadlineParent: " + maxdeadline + " _SpareTime: " + t.getspareTime() + " _subDeadline: " + t.getsubDeadline());
	}

	/**
	 * 
	 * @param spareTime
	 * @param vmMips 
//	 */
//	private void costBasedSpareTime(double spareTime, double vmMips) {
//		double totalRunTime = 0.0;
////		double levelRunTime = 0.0;
//		ArrayList<MyTask> taskLevel ;
//		
//		for(MyTask t: waitingTasks) {
//			totalRunTime += t.getEstimatedRunTime(vmMips);
//		}
//		for(Entry<Integer, ArrayList<MyTask>> entry : NumTaskafterSchedule.entrySet()) {
//			taskLevel = new ArrayList<MyTask>();
//			double levelRunTime = 0.0;
//			taskLevel.addAll(entry.getValue());
//			for(MyTask t: taskLevel) {
//				levelRunTime += t.getEstimatedRunTime(vmMips);
//			}
//			sumRunTimeInEachLevel.put(entry.getKey(), levelRunTime);
//		}
//		for(MyTask t: waitingTasks) {
//			double LRunTime = sumRunTimeInEachLevel.get(t.getDepth());
//			t.setspareTime((LRunTime/totalRunTime)*spareTime);
//		}
//	}

	/**
	 * calculate EFT of tasks
	 * @param inputTask
	 * @param vmMips
	 * @return
	 */
	private double calculateEftTask(MyTask inputTask,double vmMips) {
		if (inputTask.getEft() == -1) {
			double maxEftinput = CloudSim.clock() ;//+ MyConstant.VM_Provisioning_Delay;

			for (MyTask parent : inputTask.getParentList()) {
				double parentEft = calculateEftTask(parent, vmMips);
				if (parentEft > maxEftinput)
					maxEftinput = parentEft;
			}
			inputTask.setEft(maxEftinput + inputTask.getEstimatedRunTime(vmMips));
//			Log.printLine(" task" + inputTask.getCloudletId() + " estimatedrunTime "+ inputTask.getEstimatedRunTime(vmMips));
		}		
		return inputTask.getEft();
	}

	/**
	 * divide spare time in every level of WorkFlow
	 * @param spareTime
	 * @param Task 
	 */
	public void getDivideSpareTime(double spareTime) {
		for(MyTask t : waitingTasks) {
			int d = t.getDepth();
			double num = getNumberTaskinLevel().get(d).size();
			double s =  num / waitingTasks.size();
			double spareTask = (spareTime * s);
			t.setspareTime(spareTask);
		}
	}	

	/**
	 * set sub deadline of  each task
	 * @param vmMips
	 * @param task
	 */
	void setDeadlineTask(double vmMips ,MyTask task) {
		double maxdeadline = CloudSim.clock();
		if(task.getspareTime() != 0) {
			for(MyTask parent : task.getParentList()) {
				if(parent.getsubDeadline() == -1) {
					setDeadlineTask(vmMips, parent);
				}
				if(maxdeadline < parent.getsubDeadline())
					maxdeadline = parent.getsubDeadline();
			}
			task.setsubDeadline(maxdeadline + task.getEstimatedRunTime(vmMips) + task.getspareTime());
			Log.printLine("task: " + task.getCloudletId() + " _taskRunTime: " + task.getTaskruntime() +  " _EstimatedRuntime: " + task.getEstimatedRunTime(vmMips) + " _eftTask: " + task.getEft() + " _maxDeadlineParent: " + maxdeadline + " _SpareTime: " + task.getspareTime() + " _subDeadline: " + task.getsubDeadline());
			task.setspareTime(0);
		}
	}

	/**
	 * set number of task in each level of WorkFlow
	 */
	public void setNumLevelTask() {
		int j = 0;
		ArrayList<MyTask> tasks =  null;
		for (int i = 0; i < MaxLevel; i++) {
			arr[i] = 0;
		}
		for (MyTask ts : tasklist) {
			j = ts.getDepth();
			arr[j] += 1;
			NumLevelTask.put(j, arr[j]);
			/*this table use when we want to spare extra time between remain task that doesnot submit to any vm*/
			if(!NumTaskafterSchedule.containsKey(ts.getDepth())) {
				tasks = new ArrayList<>();
				NumTaskafterSchedule.put(ts.getDepth(),tasks);
			}
			ts.putInTable(NumTaskafterSchedule.get(ts.getDepth()));
		}
		wlevel = NumTaskafterSchedule.size();
	}

	/**
	 * get ready task for scheduling
	 * @return
	 */
	public ArrayList<MyTask> getReadyTask(){
		ArrayList<MyTask> readyTasks = new ArrayList<>();
		for(Integer key :parentsExecutedTasksMap.keySet()) {
			MyTask task = parentsExecutedTasksMap.get(key);
			if(task.getStatus() == MyTask.STATUS_READY || task.getStatus() == MyTask.STATUS_FAILIER) {
				readyTasks.add(task);
			}
		}
		return readyTasks;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isWorkflowFinished(){
		if(remainingTaskMap.size()==0){
			return true;
		}		
		return false;	
	}

	/**
	 * calculate deadline of workflow
	 * @param vmMips 
	 * @param vmMips 
	 * @param vmMips 
	 * @param fastMips 
	 * @param fastMips 
	 * @param mips
	 * @return
	 */
	public double calculateDeadlineWorkflow() {
		double criticalpathLength = getEstimatedRunTimeCriticalPath();
		this.stricestdeadline = criticalpathLength;
		return stricestdeadline;
	}

	/**
	 * get runtime of critical path to set deadline of workflow
	 * @param vmMips 
	 * @param vmMips 
	 * @param vmMips 
	 * @param fastMips 
	 * @param fastMips 
	 * @return
	 */
	public double getEstimatedRunTimeCriticalPath() {
		if(criticalPath > 0) {
			return this.criticalPath;
		}
		double criticalPathLength = 0.0;
		for(int i = 0; i < tasklist.size(); i++) {
			double currentPath = 0.0;
			MyTask t = tasklist.get(i);
			if(t.getParentList().size() == 0) {
				currentPath = getLongestPath(t);
				if(criticalPathLength < currentPath) {
					criticalPathLength = currentPath;
				}
			}
		}
		this.criticalPath = criticalPathLength;
		return criticalPath;
	}

	/**
	 * get the longest path from current task to the end of workflow
	 * @param myTask
	 * @param vmMips 
	 * @param vmMips 
	 * @param vmMips 
	 * @param fastMips 
	 * @param mips
	 * @return
	 */
	private double getLongestPath(MyTask myTask) {
		if(myTask.getChildList().size() == 0) {
			return myTask.gettotalRunTime();			
		}
		double criticalPath = 0.0;
		for(int i = 0; i < myTask.getChildList().size(); i++) {
			MyTask child = myTask.getChildList().get(i);
			double currentPath = getLongestPath(child);
			if(currentPath > criticalPath) {
				criticalPath = currentPath;
			}
		}		
		return (myTask.gettotalRunTime() + criticalPath);
	}

	/**
	 * return sum of the time need for writing data dependency of tasks in storage
	 */
	public double getSumTransferTimeTasks() {	
		double sumTransferTime = 0.0;
		for(int i = 0; i < tasklist.size() ; i++) {			
			for(org.cloudbus.cloudsim.File file: tasklist.get(i).getDataDependencies()) {
				sumTransferTime += MyStorage.addFile(file);
			}
		}
		return sumTransferTime;
	}

	/**
	 * return sum of the time need from readling data from storage
	 * @return
	 */
	public double getsumReadDataTasks() {
		double readTimeoutput = 0;
		for(int i = 0; i < tasklist.size() ; i++) {
			for(org.cloudbus.cloudsim.File file: tasklist.get(i).getOutput()) {
				readTimeoutput += MyStorage.getTransferTime(file.getSize());
			}
		}
		return readTimeoutput;
	}
	/**
	 * when a task finish then put ready child task in list
	 * @param tfinish
	 */
	public void processTaskExecFinish(MyTask tfinish) {

		if(remainingTaskMap.containsKey(tfinish.getCloudletId())) {
			remainingTaskMap.remove(tfinish.getCloudletId());
		} else
			throw new RuntimeException("Something unusual happend:1");

		if(parentsExecutedTasksMap.containsKey(tfinish.getCloudletId())) {
			parentsExecutedTasksMap.remove(tfinish.getCloudletId());
		} else
			throw new RuntimeException("Something unusual happend:2");

		for(int i = 0; i < tfinish.getChildList().size();i++) {
			MyTask tchild = tfinish.getChildList().get(i);
			int idtask = tchild.getCloudletId();
			tchild.updateTaskStatus();
			if(tchild.getStatus() == MyTask.STATUS_READY || tchild.getStatus() == MyTask.STATUS_FAILIER) {
				if(!parentsExecutedTasksMap.containsKey(idtask)) {
					parentsExecutedTasksMap.put(idtask, tchild);
				}
			}			
		}
	}

	/**
	 * 
	 * @return
	 */
	public LinkedHashMap<Integer, ArrayList<MyTask>> getNumberTaskinLevel() {
		return NumTaskafterSchedule;
	}

	/**
	 * 
	 * @param stricestdeadline
	 */
	public void setWorkflowDeadline(double stricestdeadline) {
		this.stricestdeadline = stricestdeadline;
	}

	/**
	 * 
	 * @return
	 */
	public double getWorlflowDeadline() {
		return stricestdeadline;
	}

	@Override
	public String toString() {
		String toString = "";
		int j = 0;
		toString+= "************Workflow*********\n";
		if(isWorkflowFinished()){
			toString+= "status: completed\n";
		}else{
			toString+= "status: failed\n";
		}
		toString+= "task list: \n";
		for(int i=0;i<tasklist.size();i++){
			toString += tasklist.get(i).toString()+"\n";

		}	
		for(int i=0;i < tasklist.size();i++){
			if(tasklist.get(i).getStatus() == MyTask.STATUS_SUCCESS) {
				j++;
			}

		}
		toString+= "NumberTask executed " + j;
		return toString;
	}

	/**
	 * get the remain task for update deadline
	 * @return
	 */
	public ArrayList<MyTask> getRemainingTasklist(){
		ArrayList<MyTask> remainTask = new ArrayList<>();
		for(Entry<Integer,MyTask> entry: remainingTaskMap.entrySet()) {
			MyTask remain = entry.getValue();
			if(remain.getStatus() == MyTask.STATUS_READY || remain.getStatus() == MyTask.STATUS_WAITING) {
				remainTask.add(entry.getValue());
			}			
		}
		return remainTask;
	}

	public Hashtable<Integer, Integer> getNumLevelTask() {
		return NumLevelTask;
	}

	public void setNumLevelTask(Hashtable<Integer, Integer> numLevelTask) {
		NumLevelTask = numLevelTask;
	}

	
	/**
	 * calculate est and lft of tasks and levels
	 * @param fastVmMips
	 */
//	public void calculateTimeTasks(double fastVmMips) {
//		for(MyTask t: waitingTasks) {
//			calculateEftTask(t, fastVmMips);
//			calculateLstTask(t, fastVmMips);
//		}
//	
//		for(MyTask t: waitingTasks) {
//			calculateEstTask(t,fastVmMips);
//			calculateLftTask(t,fastVmMips);
//		}
//		for(Entry<Integer, ArrayList<MyTask>> entry : NumTaskafterSchedule.entrySet()) {
//			double minEstTasks = stricestdeadline;
//			double maxLftTasks = 0.0;
//			for(MyTask t: entry.getValue()) {
//				if(t.getEst() < minEstTasks) {
//					minEstTasks = t.getEst();
//				}
//				if(t.getLft()> maxLftTasks) {
//					maxLftTasks = t.getLft();
//				}
//			}
//			
//			estLevel.put(entry.getKey(), minEstTasks);
//			lftLevel.put(entry.getKey(), maxLftTasks);
//			Log.printLine("estLevel " + minEstTasks);
//			Log.printLine("lftLevel " + maxLftTasks);
//		}	
//	}
	
	/**
	 * 	
	 * @param t
	 * @param fastVmMips
	 */
//	private double calculateLstTask(MyTask t, double fastVmMips) {		
//		if(t.getChildList().isEmpty()) {
//			t.setLstTimeTask(stricestdeadline - t.getEstimatedRunTime(fastVmMips));
//		}
//		else {
//			double minlst = stricestdeadline;
//			for(MyTask child:t.getChildList()) {	
//				double childLst = calculateLstTask(child, fastVmMips);
//				minlst = Math.min(childLst, minlst);
//			}
//			t.setLstTimeTask(minlst - t.getEstimatedRunTime(fastVmMips));
//		}
//		return t.getLstTimeTask();
//	}
	
	/**
	 * calculate lft of each tasks
	 * @param t
	 * @param fastVmMips
	 */
//	private void calculateLftTask(MyTask t, double fastVmMips) {
//		double minlst = stricestdeadline;
//		if(!t.getChildList().isEmpty()) {
//			for(MyTask child: t.getChildList()) {
//				if(child.getLstTimeTask() < minlst) {
//					minlst = child.getLstTimeTask();
//				}
//			}
//		}
//		t.setLft(minlst);
//	}
//	
	/**
	 * calculate latest finish time of tasks
	 * @param t
	 * @param fastVmMips
	 */
//	private void calculateEstTask(MyTask t, double fastVmMips) {
//		double maxEft = CloudSim.clock() ;
//		if(!t.getParentList().isEmpty()) {
//			for(MyTask parent: t.getParentList()) {
//				if(parent.getEft() > maxEft) {
//					maxEft = parent.getEft();
//				}
//			}
//		}
//		t.setEst(maxEft);
//	}

//	public void calculateResourceLevel(double fastVmMips) {	
//		for(Entry<Integer, ArrayList<MyTask>> entry: NumTaskafterSchedule.entrySet()) {
//			double timeLevel = 0.0;
//			for(MyTask t: entry.getValue()) {
//				timeLevel += t.getEstimatedRunTime(fastVmMips);
//			}
//			int numVm = (int) Math.ceil(timeLevel / (lftLevel.get(entry.getKey()) - estLevel.get(entry.getKey())));
//			numVminLevel.put(entry.getKey(), numVm);
//			Log.printLine("level " + entry.getKey() + " numVm "+ numVm);
//		}	
//	}
	
//	public void pcp_DeadlineDistirbution() {
//		double minRuntime = stricestdeadline;
//		for(MyTask t: waitingTasks) {
//			double durrentMin = calculateMinRunTimeTask(t);
//			calculateESTTask(t);
//		}
//	}
//
//	private void calculateESTTask(MyTask t) {
//		double maxEst = 0.0;
//		double minRunTimeParent = stricestdeadline;
//		if(t.getParentList().isEmpty()) {
//			t.setEst(0);
//		}else {
//			for(MyTask parent: t.getParentList()) {
//				if(parent.getEst() > maxEst) {
//					maxEst = parent.getEst();
//				}
//				if(parent.getMinRunTime() < minRunTimeParent) {
//					minRunTimeParent = parent.getMinRunTime() ;
//				}
//			}
//			t.setEst(maxEst + minRunTimeParent + t.getTransferTime());
//		}
//	}
//
//
//
//	private double calculateMinRunTimeTask(MyTask t) {
//		double minRunTime = -1;
//		LinkedHashMap<MyVm, Double> vmoffers = VMOffersGoogle.getVmOffers();
//		for(MyVm vm: vmoffers.keySet()) {
//			double runTime = t.getEstimatedRunTime(vm.getMips());
//			if(runTime < minRunTime) {
//				minRunTime = runTime;
//			}
//		}
//		t.setMinRunTime(minRunTime);
//		return minRunTime;
//	}
}
