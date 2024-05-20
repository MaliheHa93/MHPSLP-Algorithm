package MyKnapsack;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import MyKnapsack.Pipeline;
import MyKnapsack.MyConstant;
import MyKnapsack.MyVm;

public class SubdeadlineAlgorithm extends DatacenterBroker{
	private static final String TAG = "Broker: ";
	private ArrayList<MyTask> ReadyTasks;	
	public ArrayList<MyTask> tasklist;	
	public List<MyVm> idleVmList;	
	public ArrayList<MyVm> requestedVmList;
	public List<MyVm> initnewVmList;
	public ArrayList<MyTask> waitTaskList = new ArrayList<>();
	public ArrayList<Pipeline> waitPipeList;
	private HashMap<Integer, MyVm> allVmMap;	
	Hashtable<MyVm, ArrayList<Pipeline>> assignPipeline;	
	private Hashtable<String, BagofTask> bagOfTask;	
	private Hashtable<String, BagofPipline> bagOfPipeline;
	private Hashtable<ArrayList<MyTask>, ArrayList<MyVm>> initvmBotMap;	
	private Hashtable<ArrayList<Pipeline>, ArrayList<MyVm>> initvmBopMap;	
	public  ArrayList<Pipeline> pipeline;	
	public  List<MyTask> successTask;	
	private Workflow w;	
	public  double totalCost = 0;	
	private double fastVmMips;
	private int vmMipsIndex = 1;
	private int dataCenterId;
	public  double startTimeMillis = 0.0;
	public  int requestedVmCount;
	private boolean finish;
	private double MakeSpan;
	double d1 ;
	double d2 ;
	double d3 ;
	double d4 ;
	double startTime = -1;
	double FinishTime = -1;
	public static double SCHEDULING_INTERVAL = 0.1;

	public SubdeadlineAlgorithm(String name) throws Exception {		
		super(name);
		ReadyTasks =  new ArrayList<MyTask>();
		tasklist = new ArrayList<MyTask>();
		initnewVmList = new ArrayList<>();
		bagOfTask = new Hashtable<>();
		bagOfPipeline = new Hashtable<>();
		idleVmList = new ArrayList<MyVm>();
		requestedVmList = new ArrayList<MyVm>();
		allVmMap = new HashMap<>();
		assignPipeline = new Hashtable<>();
		pipeline = new ArrayList<>();
		finish = false;
		successTask = new ArrayList<MyTask>();
		waitPipeList = new ArrayList<>();
		initvmBotMap = new Hashtable<>();
		initvmBopMap = new Hashtable<>();
	}

	@Override
	public void startEntity() {
		Log.printLine(getName() + " starting... ");
		ReadyTasks =  new ArrayList<MyTask>();
		sendNow(getId(), CloudSimTags.INITIAL_BROKER);		
		send(getId(), MyConstant.DEADLINE , CloudSimTags.DEADLINE_REACHED);
	}

	@Override
	public void processEvent(SimEvent ev) {

		switch(ev.getTag()){
		case CloudSimTags.INITIAL_BROKER:
			try {
				initialBroker(ev);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case CloudSimTags.REQUEST_VM_SUCCESS:
			try {
				processVmCreate(ev);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case CloudSimTags.REQUEST_VM_FAILED:
			try {
				processVmReqFailed(ev);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case CloudSimTags.VM_TERMINATED:
			try {
				processTerminateVm(ev);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case CloudSimTags.TASK_COMPLETED:
			try {
				processTaskComplete(ev);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case CloudSimTags.SUBMIT_TASK_FAILED:
			try {
				processSubmitTaskFailed(ev);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case CloudSimTags.SCHEDULING:
			try {
				StartSchedulingReadyTasks(ev);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case CloudSimTags.DEADLINE_REACHED:
			processDeadLineReached();
			break;	

		case CloudSimTags.VM_IDLE:
			processIdleVmList(ev);
			break;
		}	 
	}

	/**
	 * process add idle VM in idleVmList
	 * @param ev
	 */
	private void processIdleVmList(SimEvent ev) {
		MyVm idleVm = (MyVm) ev.getData();
		if(idleVm.getState() != MyVm.VM_TERMINATED) {
			idleVmList.add(idleVm);
		}	 
	}	

	/**
	 * if deadline reach call endsection method for ending scheduling
	 */
	private void processDeadLineReached() {
		if(!MyConstant.DEADLINE_REACH) {
			endSection();
			System.out.println("______________________");
			printLog("Deadline Reach");
		}
	}


	/**
	 * this method print log of failed task in vm
	 * @param ev
	 */
	private void processSubmitTaskFailed(SimEvent ev) {
		MyTask t = (MyTask)ev.getData(); 
		Log.printLine(" task in vm# " + t.getVmId() +" with id# "+ t.getCloudletId()+ " submit failed ");
	}

	/**
	 * if a task finish after its deadline then update its subdeadline 
	 * and put in scheduling queue again
	 * @param vm 
	 * @param isReschedulshoude 
	 * @param ev
	 * @return 
	 */
	private boolean IsReScheduling(MyTask t) {
		/**if vm that running this task has another task in its queue 
		 * then check all the queue task can run before their sub deadline in current time 
		 * if they can: no action is taken
		 * else put them in ready tasks and reschedule them again**/
		boolean shouldReSchedule = false;
		MyVm vm = allVmMap.get(t.getVmId());
		if(t.inPipe()){
			if(vm.numPipeinQueue() >= 1) {
				Iterator<Pipeline> pqitr = vm.getQueuePipeline().iterator();
				while(pqitr.hasNext()) {
					Pipeline p = pqitr.next();
					if(t.getPipeline() != p) {
						if(waitPipeList.contains(p)) {
							waitPipeList.remove(p);
						}
						p.setVmId(-1);
						for(MyTask tp: p) {
							ReadyTasks.add(tp);
							tp.pipeline = null;
						}
						shouldReSchedule = true;
						pqitr.remove();
					}					
				}				
			}
		} 
		else {
			if(vm.numTaskinQueue() >= 1) {
				Iterator<MyTask> tqitr = vm.getSortedQueueList().iterator();
				double currentTime = CloudSim.clock();
				boolean f = false;
				while(tqitr.hasNext()) {
					MyTask ts = tqitr.next();
					if(ts.getEstimatedRunTime(vm.getMips()) + currentTime < ts.getsubDeadline()) {
						currentTime += ts.getEstimatedRunTime(vm.getMips());
					}
					else {
						tqitr.remove();
						if(waitTaskList.contains(ts)) {
							waitTaskList.remove(ts);
						}
						ts.setVmId(-1);
						ts.isReschedule = true;
						if(!ReadyTasks.contains(ts)) {
							ReadyTasks.add(ts);	
						}											
						shouldReSchedule = true;
						ts.bot = null;
					}										
				}				
			}		
		}
		return shouldReSchedule;
	}

	/**
	 * this method process completed task and ready child of this task
	 * and back the vm in idle list
	 * @param ev
	 */
	private void processTaskComplete(SimEvent ev) {
		MyTask t = (MyTask) ev.getData();
		MyVm vm = allVmMap.get(t.getVmId());

		boolean f = false;
		if(t.getFinishTime() > t.getsubDeadline()) {
			t.isReschedule = true;
		}
		vm.submitTaskflag = false;
		vm.submitPipelineFlag = false;

		if(t.getStatus() == MyTask.STATUS_SUCCESS) {
			successTask.add(t);
			w.processTaskExecFinish(t);
			if(t.inBot()) {
				if(vm.getQueueTask().isEmpty() && vm.getVmFinishTime() == vm.getEndTimeSlot()) {
					sendNow(dataCenterId, CloudSimTags.TERMINATE_VM_IMMIDIATELLY, vm);
					if(idleVmList.contains(vm)) {
						idleVmList.remove(vm);
					}
				}
				else {
					for (MyTask tw : w.waitingTasks) {
						tw.setEft(-1);
					}
				}
			}
			else {
				Iterator<Pipeline> plist = vm.getQueuePipeline().iterator();
				while(plist.hasNext()) {
					Pipeline pl = plist.next();
					if(pl.contains(t)) {
						pl.removeCompleteTask(t);
						if(pl.isAllTaskExecuted()) {
							waitPipeList.remove(pl);
							plist.remove();								
						}
					}else {
						makeEFTDirty(pl.get(0));
					}
				}
				if(vm.getQueuePipeline().isEmpty() && vm.getVmFinishTime() == vm.getEndTimeSlot()) {
					sendNow(dataCenterId, CloudSimTags.TERMINATE_VM_IMMIDIATELLY, vm);
					if(idleVmList.contains(vm)) {
						idleVmList.remove(vm);
					}
				}
			}
			MakeSpan = CloudSim.clock();
			System.out.println("____________________________________________________________________________________________________________________________");
			printLog("COMPLETE TASK IN BROKER:/" + " Task with id# " + t.getCloudletId() + " with subDeadline " + t.getsubDeadline() + " is Completed" + " is Reschedule "+ t.isReschedule );	
			makeEFTDirty(t);
			
			t.setEft(t.getFinishTime());
			t.setsubDeadline(t.getFinishTime());
		
			boolean possible = w.assignSubDeadlineTask(fastVmMips) ;
			if(!possible)	{
				Log.printLine("Workflow Cant run in this deadline");
				return;
			}

			/**call rescheduling method **/
			if(t.isReschedule) {
				f = IsReScheduling(t);
				if(f) {
					t.SubmmitedAgain = true;
				}
			}
		}
	}

	/**
	 * when task complete make its eft dirty to update  its sub deadline
	 * @param t
	 */
	private void makeEFTDirty(MyTask t) {
		t.setEft(-1);
		for (MyTask child : t.getChildList())
			makeEFTDirty(child);
	}

	/**
	 * get event vm initiated  from datacenter and set state of vm to idle
	 * 
	 **/
	protected void processVmCreate(SimEvent ev) {
		MyVm vm = (MyVm) ev.getData();
		vm.setVmStartTime(CloudSim.clock());
		System.out.println("___________________________________________________");
		printLog("VM CREATE ON BROKER:/ " + " vm# " + vm.getId() + " created ");
		allVmMap.put(vm.getId(), vm);
		if(vm.getState() == MyVm.VM_IDLE){
			idleVmList.add(vm);
		}
		
	}

	/**
	 * a method for scheduling bag of tasks
	 * @param ev
	 */
	private void StartSchedulingReadyTasks(SimEvent ev) {	
		//check if run of workflow finish then end section
		Log.printLine(" Start scheduling " + CloudSim.clock());
		
		if(checkFinish()){
			endSection();			
		}					
		if(finish){
			return;
		}
		/*get ready tasks*/
		ReadyTasks = w.getReadyTask();

		/*before scheduling tasks first PreProcess ready Tasks*/
		preProcessScheduling(ReadyTasks);

		/*schedule bag of ready task*/	
		scheduleBot();

		/*schedule bag of ready pipeline*/
		scheduleBop();

		/*get finish time from clock*/
		FinishTime = CloudSim.clock();

		/*check all the tasks of workflow finish or not*/
		if(checkFinish()){		
			endSection();
		}

		/*send scheduling event*/
		send(getId(),MyConstant.SCHEDULING_INTERVAL, CloudSimTags.SCHEDULING);
		//	SCHEDULING_INTERVAL = 0.01;
	}

	/**
	 * PreProcessing before start scheduling 
	 * include create bag of tasks and bag of pipeline
	 * @param readyTasks 
	 **/
	private void preProcessScheduling(ArrayList<MyTask> readyTasks) {
		BagofTask bot = null;
		/*first group ready tasks into bags*/
		for(MyTask t:readyTasks) {
			if (t.pipeline == null && t.getBot() == null) {
				if (!bagOfTask.containsKey(t.getKey())) {
					bot = new BagofTask();
					bagOfTask.put(t.getKey(), bot);
				}
				t.putInBot(bagOfTask.get(t.getKey()));
			}
			else if(t.pipeline != null) {
				BagofPipline bop = null;
				Pipeline p = t.getPipeline();
				if (!bagOfPipeline.containsKey(p.getKey())) {
					bop = new BagofPipline();
					bagOfPipeline.put(p.getKey(), bop);
				}
				p.putInBag(bagOfPipeline.get(p.getKey()));
			}
		}		
		/*set deadline for bag of tasks*/
		assignSubDeadlineBot();

		/*set deadline to bag of pipeline*/
		assignSubDeadlineBop();		
	}	

	/**
	 * assign sub deadline to bop
	 */
	private void assignSubDeadlineBop() {
		double max_deadlineTaskinPipe = -1;
		double max_deadlinePipe = -1;
		for(BagofPipline bop : bagOfPipeline.values()) {
			for(Pipeline p:bop) {
				for(MyTask t: p) {
					if(max_deadlineTaskinPipe < t.getsubDeadline()) {
						max_deadlineTaskinPipe = t.getsubDeadline();
					}		
				}
				p.setDeadline(max_deadlineTaskinPipe);
				max_deadlineTaskinPipe = -1;
				if(p.getDeadline() > max_deadlinePipe) {
					max_deadlinePipe = p.getDeadline();
				}
			}
			bop.setDeadlineBop(max_deadlinePipe); 
		}		
	}

	/**
	 * assign deadline to bag of tasks
	 */
	private void assignSubDeadlineBot() { 
		double max_deadlineTaskinBot = -1;
		for(BagofTask bot :bagOfTask.values()) {	
			if(bot.size() > 0) {
				for(MyTask t :bot) {
					if(t.getsubDeadline() > max_deadlineTaskinBot) {
						max_deadlineTaskinBot = t.getsubDeadline();
					}
				}
				bot.setDeadlineBot(max_deadlineTaskinBot);
			}						
			max_deadlineTaskinBot = -1;												
		}		
	}

	/**
	 * scheduling bag of tasks
	 */
	private void scheduleBot () {
		submitWaitTasks();
		for(Entry<String, BagofTask> entry : bagOfTask.entrySet()) {
			BagofTask bot = entry.getValue();
			if(bot.size() != 0) {
				if(bot.size() > 1) {
					scheduleBotMultiTask(bot);
				}
				else {
					scheduleSingleBot(bot);
				}
			}
		}
	}

	/**
	 * schedule bot with size > 1 
	 * @param bot
	 */
	private void scheduleBotMultiTask(BagofTask bot) {
		boolean applicaple = false;
		double Max_length = 0.0;
		MyTask largestTask = null;
		Log.printLine("deadlineBot " + bot.getDeadlineBot() + " Bot size " + bot.size());		

		for(MyTask t: bot) {
			Log.printLine(t.getCloudletId()+" is reschedule "+ t.isReschedule);
			if(Max_length < t.getTaskruntime()) {
				Max_length = t.getTaskruntime();
				largestTask = t;
			}
		}
		if(bot.size() > 1 && idleVmList.size() == 0) {
			provision(bot);		
		}
		else if(bot.size() > 1 && idleVmList.size() != 0 ) {
			Iterator<MyVm> list =  idleVmList.iterator();
			while(list.hasNext() && !bot.isEmpty()) {
				MyVm vm = list.next();
				if(vm.getRemainingTimeSlot() == -1) {
					for(int i = 0; i < vm.numberTask; i++) {
						MyTask t = bot.getNextReadyTaskFromBot();
						if(t!= null) {
							if(vm.getAssingedTaskList().contains(t)) {
								if(t.getStatus() == MyTask.STATUS_READY) {
									boolean f =  submitTaskVm(t, vm);
									bot.removeFromBot(t);
									if(f) {
										applicaple = true;
										list.remove();
									}
									else {
										waitTaskList.add(t);
										vm.addTaskinQueue(t);
									}									
								}
							}
						}
					}
				}
				else {
					double duration = bot.getDeadlineBot() - CloudSim.clock();
					vm.numberTask = (int)(duration/ largestTask.getEstimatedRunTime(vm.getMips()));	
					if(vm.numberTask != 0) {
						for(int i = 0; i < vm.numberTask; i++) {
							MyTask t = bot.getNextReadyTaskFromBot();
							if(t!= null) {
								boolean f = submitTaskVm(t, vm);
								bot.removeFromBot(t);
								if(f) {								
									applicaple = true;									
									list.remove();	
								}
								else if(!f) {
									waitTaskList.add(t);
									vm.addTaskinQueue(t);
								}

							}else {
								break;
							}
						}
					}
					else {
						continue;
					}
				}								
			}
			if(bot.size() > 1 && idleVmList.isEmpty()) {
				provision(bot);
			}
			if(bot.size() == 1 && idleVmList.isEmpty()) {
				provisionSingleTaskVm(bot.get(0));
			}
			if(bot.size() > 1 && !idleVmList.isEmpty() && !applicaple) {
				provision(bot);
			}
			if(bot.size() == 1 && !idleVmList.isEmpty() && !applicaple) {
				provisionSingleTaskVm(bot.get(0));
			}
		}
	}

	/**
	 * if no idle vm exists then provision new Vm with knapsack algorithm
	 * First calculate how many task can run in each vm before deadline of bag(60s)
	 * then nt task from bag map into vms
	 * @param deadlineBot 
	 **/
	private void provision(BagofTask bag) {
		int[] NumTask = new int[initnewVmList.size()];
		double[] cost = new double[initnewVmList.size()];
		double Max_length = 0.0;
		MyTask largestTask = null;
		int id;
		double deadlineBot = bag.getDeadlineBot();
		Hashtable<Integer, MyVm> getvm = new Hashtable<Integer, MyVm>();
		UKP_Provisioing ukp = new UKP_Provisioing();

		if(bag.size() > 1) {
			for(MyTask t:bag) {
				if(Max_length < t.getTaskruntime()) {
					Max_length = t.getTaskruntime();
					largestTask = t;
				}
			}
		}
		double duration = deadlineBot - CloudSim.clock();
		for(MyVm vmoffer :initnewVmList) {
			id = vmoffer.getId();
			getvm.put(id, vmoffer);
			NumTask[id] = (int)(duration / largestTask.getEstimatedRunTime(vmoffer.getMips()));
			cost[id]    = VMOffersGoogle.getCost(vmoffer.getMips(), vmoffer.getRam(), vmoffer.getBw());			
		}		
		/*calculate number of resources for scheduling bag by DP*/
		KnapsackResult result = ukp.DP(bag.size(), NumTask, cost);
		for(ResourceProvisioning rp : result.resources) {
			id = rp.vmid;
			MyVm vms = getvm.get(id);
			int pe =  vms.getNumberOfPes();
			int numberVM = rp.numberVm;
			int NtTask = rp.NT;
			for(int i = 0; i < numberVM;i++) {
				MyVm vm = createNewVm(pe);
				vm.setNumberTaskAssign(NtTask);
				vm.setCostPerMinute(VMOffersGoogle.getCost(pe));
				for(MyTask t : bag) {
					vm.setAssingedTask(t);
				}
				requestedVmList.add(vm);
				sendNow(dataCenterId, CloudSimTags.REQUEST_VM , vm);
				//				SCHEDULING_INTERVAL = 30.01;
			}
			//initvmBotMap.put(bag, requestedVmList);
		}
	}
	/**
	 * schedule a bot with single task
	 * @param bot
	 */
	private void scheduleSingleBot(BagofTask bot) {
		boolean applicaple = false;
		double Max_length = 0.0;
		MyTask largestTask = null;
		for(MyTask t: bot) {
			Log.printLine("task id " + t.getCloudletId() + " task subdeadline "+ t.getsubDeadline());
			if(Max_length < t.getTaskruntime()) {
				Max_length = t.getTaskruntime();
				largestTask = t;
			}
		}
		if(idleVmList.size() == 0 && bot.size() == 1) {
			provisionSingleTaskVm(bot.get(0));
		}
		else if(bot.size() == 1 && idleVmList.size() != 0 ){
			Iterator<MyVm> list = idleVmList.iterator();
			while(list.hasNext() && !bot.isEmpty()) {
				MyVm vm = list.next();
				if(vm.getRemainingTimeSlot() == -1) {
					MyTask t = bot.getNextReadyTaskFromBot();
					if(t!= null) {
						if(vm.getAssingedTaskList().contains(t)) {
							if(t.getStatus() == MyTask.STATUS_READY) {
								boolean f = submitTaskVm(t, vm);
								bot.removeFromBot(t);
								if(!f) {
									waitTaskList.add(t);
									vm.addTaskinQueue(t);
								}
							}
						}							
					}						
					list.remove();					
				}
				else {
					double duration = bot.getDeadlineBot() - CloudSim.clock();
					vm.numberTask = (int)(duration/ largestTask.getEstimatedRunTime(vm.getMips()));
					if(vm.numberTask != 0) {
						MyTask t = bot.getNextReadyTaskFromBot();							
						if(t!= null) {
							bot.removeFromBot(t);
							boolean f = submitTaskVm(t, vm);
							if(f) {
								applicaple = true;
								list.remove();
							}
						}
						else {
							break;
						}		
					}
					else {
						continue;
					}
				}				
			}
			if(bot.size() == 1 && idleVmList.isEmpty()) {
				provisionSingleTaskVm(bot.get(0));
			}
			if(bot.size() == 1 && !idleVmList.isEmpty() && !applicaple) {
				provisionSingleTaskVm(bot.get(0));
			}
		}
	}

	/**
	 * provision vm for one task 
	 * if a vm can run the task before deadline of task with min cost selected
	 * else fastest vm provisioned
	 * @param t
	 */
	private void provisionSingleTaskVm(MyTask t) {
		int[] NumTask = new int[initnewVmList.size()];
		double[] cost = new double[initnewVmList.size()];
		int id = -1;
		double duration = t.getsubDeadline()  - CloudSim.clock();

		for(MyVm vmoffer :initnewVmList) {
			id = vmoffer.getId();
			NumTask[id] = (int)(duration / t.getEstimatedRunTime(vmoffer.getMips()));
			cost[id] = VMOffersGoogle.getCost(vmoffer.getMips(), vmoffer.getRam(), vmoffer.getBw());
		}

		MyVm vm = null;
		int minIndex = -1;
		for(int i = 0; i < NumTask.length ; i++) {
			if(NumTask[i] >= 1 && (minIndex == -1 || cost[minIndex] > cost[i])) {
				minIndex= i;				
			}
		}
		if(minIndex == -1) {
			minIndex= initnewVmList.size()-1;
		}
		vm = initnewVmList.get(minIndex);
		int pe =  vm.getNumberOfPes();
		MyVm newvm = createNewVm(pe);
		newvm.setCostPerMinute(VMOffersGoogle.getCost(pe));
		newvm.setNumberTaskAssign(1);
		newvm.setAssingedTask(t);
		sendNow(dataCenterId, CloudSimTags.REQUEST_VM , newvm);		
		//SCHEDULING_INTERVAL = 30.01;
	}

	/**
	 * schedule bag of pipelines in free vms 
	 */
	private void scheduleBop() {
		scheduleWaitPipe();	
		for(Entry<String, BagofPipline> entry : bagOfPipeline.entrySet()) {
			BagofPipline bop = entry.getValue();
			Iterator<Pipeline> pit = bop.iterator();
			if(bop.size()!= 0) {
				if(!pit.next().isEmpty()) {
					if(bop.size() > 1) {
						scheduleBagofPipe(bop);
					}
					else {
						scheduleSinglePipe(bop);
					}
				}
				else {
					pit.remove();
				}
			}
		}
	}

	/**
	 * schedule bag of pipeline
	 * @param bop 
	 */
	private void scheduleBagofPipe(BagofPipline bop) {
		boolean applicaple = false;
		double sumRunTimeTaskPie = 0;
		double maxLengthPipe = 0;
		Pipeline largestp = null;		
		
		Log.printLine("deadline bop " + bop.getDeadlineBop());
		for(Pipeline p :bop) {
			for(MyTask t: p) {
				Log.printLine("Task(p) id " + t.getCloudletId() + " task deadline "+ t.getsubDeadline());
				sumRunTimeTaskPie += t.getTaskruntime();	
			}
			p.setSumPipeRunTime(sumRunTimeTaskPie);
			sumRunTimeTaskPie = 0;

			if(maxLengthPipe < p.getSumPipeRunTime()) {
				maxLengthPipe = p.getSumPipeRunTime();
				largestp = p;
			}
		}
		if(bop.size() > 1 && idleVmList.size() == 0) {
			provisionBagofPiepline(bop);		
		}
		else if(bop.size() > 1 && idleVmList.size() > 0) {
			Iterator<MyVm> vmList = idleVmList.iterator();
			while(vmList.hasNext() && !bop.isEmpty()) {
				MyVm vm = vmList.next();
				if(requestedVmList.contains(vm)) {
					requestedVmList.remove(vm);
				}
				if(vm.getRemainingTimeSlot() == -1) {
					for(int i = 0; i < vm.numberPipeline; i++) {
						Pipeline p = bop.getNextReadyPipelineFromBop();
						if(p!= null) {
							if(vm.getAssingedPipelineList().contains(p)) {
								boolean f = submitBopVM(p, vm);
								bop.RemoveFromBop(p);
								if(!f ||!p.isAllTaskExecuted()) {
									waitPipeList.add(p);
									vm.addPipeinQueue(p);
								}
							}
						}
						else {
							break;
						}
					}
					vmList.remove();									
				}
				else {
					double duration = bop.getDeadlineBop() - CloudSim.clock();
					vm.numberPipeline =  (int) (duration/ largestp.getEstimatedRunTime(vm.getMips()));
					if(vm.numberPipeline != 0) {
						for(int i = 0; i < vm.numberPipeline; i++) {
							Pipeline p = bop.getNextReadyPipelineFromBop();	
							if(p!= null) {
								if(p.getEstimatedRunTime(vm.getMips()) < vm.getRemainingTimeSlot()) {
									bop.RemoveFromBop(p);
									boolean f = submitBopVM(p, vm);									
									if(!f ||!p.isAllTaskExecuted()) {
										waitPipeList.add(p);
										vm.addPipeinQueue(p);
									}
									if(f) {
										applicaple = true;									
									}
								}else {
									continue;
								}

							}
						}
						vmList.remove();
					}
					else {
						continue;
					}	
				}				
			}
			if(bop.size() > 1 && idleVmList.isEmpty()) {
				provisionBagofPiepline(bop);
			}
			if(bop.size() == 1 && idleVmList.isEmpty()) {
				provisionSinglePipeline(bop.get(0));
			}
			if(bop.size() > 1 && !idleVmList.isEmpty() && !applicaple) {
				provisionBagofPiepline(bop);
			}
			if(bop.size() == 1 && !idleVmList.isEmpty() && !applicaple) {
				provisionSinglePipeline(bop.get(0));
			}
		}			
	}


	/**
	 * provision Vm for a bag of pipeline
	 * @param bop
	 * @param deadlineBop
	 */
	private void provisionBagofPiepline(BagofPipline bop) {
		int[] NumPipeline = new int[initnewVmList.size()];
		double[] cost = new double[initnewVmList.size()];
		int id = -1;

		Hashtable<Integer, MyVm> getvm = new Hashtable<Integer, MyVm>();
		UKP_Provisioing ukp = new UKP_Provisioing();

		double deadlineBop = bop.getDeadlineBop();
		double sumRunTimeTaskPie = 0;
		double maxLengthPipe = 0;
		Pipeline largestp = null;

		for(Pipeline p :bop) {
			for(MyTask t: p) {
				sumRunTimeTaskPie += t.getTaskruntime();	
			}
			p.setSumPipeRunTime(sumRunTimeTaskPie);
			sumRunTimeTaskPie = 0;

			if(maxLengthPipe < p.getSumPipeRunTime()) {
				maxLengthPipe = p.getSumPipeRunTime();
				largestp = p;			
			}			
		}
		double duration = deadlineBop - CloudSim.clock();

		for(MyVm vm : initnewVmList) {
			id = vm.getId();
			NumPipeline[id] = (int)(duration/ largestp.getEstimatedRunTime(vm.getMips()));
			cost[id] = VMOffersGoogle.getCost(vm.getMips() ,vm.getRam(), vm.getBw());
			getvm.put(vm.getId(), vm);
		}
		KnapsackResult result = ukp.DP(bop.size() , NumPipeline , cost);
		for(ResourceProvisioning rp : result.resources) {
			id = rp.vmid;
			MyVm vms = getvm.get(id);

			int pe = vms.getNumberOfPes();
			int numberVm = rp.numberVm;
			int NtPipe = rp.NT;

			for(int i = 0 ; i < numberVm; i++ ) {
				MyVm vm = createNewVm(pe);
				vm.setnumberPipelineAssign(NtPipe);
				vm.setCostPerMinute(VMOffersGoogle.getCost(pe));
				for(Pipeline p :bop) {
					vm.setAssingedPipe(p);
				}
				requestedVmList.add(vm);
				sendNow(dataCenterId, CloudSimTags.REQUEST_VM , vm);	
			}
			//			initvmBopMap.put(bop, requestedVmList);
			//SCHEDULING_INTERVAL = 30.01;
		}
	}

	/**
	 * schedule single pipeline
	 * @param bop 
	 */
	private void scheduleSinglePipe(BagofPipline bop) {
		boolean applicaple = false;
		double sumRunTimeTaskPipe = 0;
		Pipeline largestp = null;
		double maxLengthPipe = 0;

		for(Pipeline p : bop) {
			for(MyTask t: p) {
				Log.printLine(" Task id " + t.getCloudletId());
				sumRunTimeTaskPipe += t.getTaskruntime();
			}
			p.setSumPipeRunTime(sumRunTimeTaskPipe);
			sumRunTimeTaskPipe = 0;
			if(maxLengthPipe < p.getSumPipeRunTime()) {
				maxLengthPipe = p.getSumPipeRunTime();
				largestp = p;
			}
		}
		if(bop.size() == 1 && idleVmList.size() > 0) {
			Iterator<MyVm> vmList = idleVmList.iterator();
			while(vmList.hasNext() && !bop.isEmpty()) {
				MyVm vm = vmList.next();
				if(vm.getRemainingTimeSlot() == -1) {
					Pipeline p = bop.getNextReadyPipelineFromBop();
					if(p!= null) {
						boolean f = submitBopVM(p, vm);
						bop.RemoveFromBop(p);
						if(!f ||!p.isAllTaskExecuted()) {
							waitPipeList.add(p);
							vm.addPipeinQueue(p);
						}
						vmList.remove();
					}														
				}
				else {
					double duration = bop.getDeadlineBop() - CloudSim.clock();
					vm.numberPipeline = (int)(duration/ largestp.getEstimatedRunTime(vm.getMips()));
					if(vm.numberPipeline != 0) {
						Pipeline p = bop.getNextReadyPipelineFromBop();
						bop.RemoveFromBop(p);
						boolean f = submitBopVM(p, vm);
						if(!f ||!p.isAllTaskExecuted()) {
							waitPipeList.add(p);
							vm.addPipeinQueue(p);
						}
						if(f) {
							applicaple = true;										
						}
					}
					else {
						continue;
					}
					vmList.remove();
				}				
			}
			if(bop.size() == 1 && idleVmList.isEmpty()) {
				provisionSinglePipeline(bop.get(0));
			}
			if(bop.size() == 1 && !idleVmList.isEmpty() && !applicaple) {
				provisionSinglePipeline(bop.get(0));
			}
		}
		else if(bop.size() == 1 && idleVmList.size() == 0 ){
			provisionSinglePipeline(bop.get(0));
		}
	}

	/**
	 * provision vm for bop with one pipeline
	 * @param p
	 * @param deadlineBop
	 */
	private void provisionSinglePipeline(Pipeline p) {
		int[] NumTask = new int[initnewVmList.size()];
		double[] cost = new double[initnewVmList.size()];
		int id =-1;
		double sumTaskRunTimePipe = 0;
		double deadlinep = p.getDeadline();
		for(MyTask t: p) {
			sumTaskRunTimePipe += t.getTaskruntime();			
		}	
		p.setSumPipeRunTime(sumTaskRunTimePipe);
		double duration = deadlinep - CloudSim.clock();

		for(MyVm vm: initnewVmList) {
			id = vm.getId();
			NumTask[id] = (int)(duration /(p.getEstimatedRunTime(vm.getMips())));
			cost[id] = (VMOffersGoogle.getCost(vm.getMips(), vm.getRam(), vm.getBw()));
		} 

		MyVm vm = null;
		int minIndex = -1;
		for(int i = 0; i < NumTask.length ; i++) {
			if(NumTask[i] >= 1 && (minIndex == -1 || cost[minIndex] > cost[i])) {
				minIndex= i;				
			}
		}
		if(minIndex == -1) {
			minIndex= initnewVmList.size()-1;
//			System.err.print("pipeline "+ p +"can run with any vm");
//			throw new RuntimeException();
		}
		vm = initnewVmList.get(minIndex);
		int pe =  vm.getNumberOfPes();
		MyVm newvm = createNewVm(pe);
		newvm.setCostPerMinute(VMOffersGoogle.getCost(pe));
		int num = 1;
		newvm.setnumberPipelineAssign(num);
		sendNow(dataCenterId, CloudSimTags.REQUEST_VM , newvm);	
		//		SCHEDULING_INTERVAL = 30.01;
	}

	/**
	 * schedule task in wait list of vms
	 */
	private void submitWaitTasks() {
		Iterator<MyTask> tlist =  waitTaskList.iterator();
		while(tlist.hasNext()) {
			MyTask t = tlist.next();
			for(int i = 0; i < idleVmList.size();i++){
				int vmid = t.getVmId();
				if(idleVmList.get(i).getId() == vmid && idleVmList.get(i).getState() == MyVm.VM_IDLE) {
					if(t.getStatus() == MyTask.STATUS_READY) {
						boolean f = submitTaskVm(t, idleVmList.get(i));
						if(f) {
							tlist.remove();
							idleVmList.get(i).getQueueTask().remove(t);
							idleVmList.remove(i);
							break;
						} else {
							System.out.println("***** something impossible happend. **********");
							throw new RuntimeException("This should not happen");
						}
					}
				}
			}		
		}
	}

	/**
	 * schedule wait pipeline
	 * @param vm 
	 */
	private void scheduleWaitPipe() {
		Iterator<Pipeline> plist = waitPipeList.iterator();
		while(plist.hasNext()) {
			Pipeline p = plist.next();
			for(int i = 0; i < idleVmList.size(); i++) {
				int vmid = p.getVmId();
				if(idleVmList.get(i).getId() == vmid && idleVmList.get(i).getState() == MyVm.VM_IDLE) {
					boolean f = submitBopVM(p, idleVmList.get(i));
					if(f ) {
						if(p.isAllTaskExecuted()) {
							plist.remove();
							idleVmList.remove(i).getQueuePipeline().remove(p);
						}
						idleVmList.remove(i);						
						break;						
					}
				}
			}
		}
	}

	/**
	 * submit bop to a vm to scheduling
	 * @param p
	 * @param idleVm
	 */
	private boolean submitBopVM(Pipeline p, MyVm idleVm) {
		boolean flag = false;
		double currentTime = CloudSim.clock();
		if(idleVm.submitPipelineFlag == true) {
			p.setVmId(idleVm.getId());
			return flag = false;				
		}
		else if(idleVm.submitPipelineFlag == false){
			p.setVmId(idleVm.getId());
			idleVm.submitPipelineFlag = true;
			for(MyTask t: p) {
				if(idleVm.submitTaskflag == true) {
					t.setVmId(idleVm.getId());
				}
				else {
					if(t.getStatus() == MyTask.STATUS_READY) {
						t.setBrokerId(getId());
						t.setVmId(idleVm.getId());
						t.setSubmissionTime(CloudSim.clock());
						t.setStatus(MyTask.STATUS_SCHEDULED);
						
						t.setEft(currentTime + t.getEstimatedRunTime(idleVm.getMips()));
						t.setsubDeadline(currentTime + t.getEstimatedRunTime(idleVm.getMips()));

						w.waitingTasks.remove(t);
						idleVm.submitTaskflag = true;
						for(ArrayList<MyTask> itr : w.getNumberTaskinLevel().values()) {
							if(itr.contains(t)) {
								itr.remove(t);
							}
						}
						System.out.println("_______________________________________________________________________________________________________________________");
						printLog("TASK(P) SUBMMITION ON BROKER:/ " + " Ready task# " + t.getCloudletId() + " Scheduled on vm# " + idleVm.getId()+ " with subdeadline " + t.getsubDeadline());		
						sendNow(dataCenterId, CloudSimTags.SUBMIT_TASK , t);
					}					
				}
			}
			flag = true;
		}
		return flag;
	}

	/**
	 * this method submit task to vm with vmId
	 * @param t
	 * @param vm
	 */
	private boolean submitTaskVm(MyTask readyTask, MyVm vm) {		
		if(vm.submitTaskflag == true) {
			readyTask.setVmId(vm.getId());		
			return false;
		}
		else {
			double currentTime = CloudSim.clock();
			readyTask.setBrokerId(getId());
			readyTask.setVmId(vm.getId());			
			readyTask.setSubmissionTime(currentTime);
			readyTask.setStatus(MyTask.STATUS_SCHEDULED);
			readyTask.setEft(currentTime + readyTask.getEstimatedRunTime(vm.getMips()));
			readyTask.setsubDeadline(currentTime + readyTask.getEstimatedRunTime(vm.getMips()));
			w.waitingTasks.remove(readyTask);
			vm.submitTaskflag = true;

			for(ArrayList<MyTask> itr : w.getNumberTaskinLevel().values()) {
				if(itr.contains(readyTask)) {
					itr.remove(readyTask);
				}
			}

			System.out.println("________________________________________________________________________________________________________________________________");
			printLog("TASK SUBMMITION ON BROKER:/ " + " Ready task# " + readyTask.getCloudletId()+" with eft updated "+ readyTask.getEft() + " Scheduled on vm# " + vm.getId() + " with subdeadline " + readyTask.getsubDeadline());

			/*put tasks in vm*/		
			sendNow(dataCenterId, CloudSimTags.SUBMIT_TASK , readyTask);
			return true;
		}
	}

	/**
	 * this method terminate vm if vm finish process
	 * @param ev
	 */
	private void processTerminateVm(SimEvent ev) {
		MyVm vm = (MyVm) ev.getData();
		if(allVmMap.containsKey(vm.getId())) {
			allVmMap.remove(vm.getId());
		}
		System.out.println("_________________________________________________________________");
		printLog("TERMINATE VM ON BROKER:/ " +" VM "+ vm.getId() + " is terminated");	
	}

	/**
	 * this method print vm failed error
	 * @param ev
	 */
	private void processVmReqFailed(SimEvent ev) {
		MyVm vm = (MyVm) ev.getData();
		System.out.println("_______________________________________");
		printLog("VM " + vm.getId() + " requested failed");	
	}

	/**
	 * this method initial broker : 
	 * 1) parse dax file
	 * 2) get tasklist from workflow class
	 * 3) set workflow deadline
	 * 4) set vmMips and vmCost
	 * 5) start static scheduling phase of algorithm
	 * 6) get vmofferTable for start dynamic phase in scheduling tag
	 * @param ev
	 * @throws Exception 
	 */
	private void initialBroker(SimEvent ev) throws Exception {
		Log.printLine("**************************************************************");
		Log.printLine("**************************************************************");
		Log.printLine("------------.****starting subDeadline scheduling****.------------");
		Log.printLine("**************************************************************");
		Log.printLine("**************************************************************");
		startTime = CloudSim.clock();

		//get data center id
		if(CloudSim.getCloudResourceList().size() > 0){
			dataCenterId = CloudSim.getCloudResourceList().get(0);			
		}
		else {
			throw new Exception("no data center available");
		}

		/*set DaxPath*/
		String daxpath = "F:/master/term 4/implementation/cloudsim-3.0/examples/dax/old/Montage_1000.xml";

		/*set the tasklist of workflow from parser class*/		
		w = DaxParser.ParseDax(daxpath);

		/*get the tasklist of worflow*/
		tasklist = w.getTaskList();

		/*set mips and cost of cheapest vm*/
		fastVmMips = VMOffersGoogle.getMips(VMOffersGoogle.n1_standard_8);
		vmMipsIndex = 1;

		/*find the criticalPath to calculate deadline of workflow*/
		d1 =  w.calculateDeadlineWorkflow();

		/*calculate toward relax deadline*/
		double dint = d1/2;
		d2 = d1 + dint;
		d3 = d2 + dint;
		d4 = d3 + dint;
		Log.printLine("Deadline d1 " + d1);
		Log.printLine("Deadline d2 " + d2);
		Log.printLine("Deadline d3 " + d3);
		/*set the deadline of workflow */
		w.setWorkflowDeadline(d2);
		Log.printLine("Deadline d4 " + d4);

		/***static planning of WorkFlow include: 
		 * 1)identifying pipelines in WorkFlow
		 * 2) the calculate subDeadline of each task 
		 * 3) identifying Bot and Bop***/
		/*1)first set number task in each level of workflows*/
		w.setNumLevelTask();

		/*identifying pipeline in Workflow */
		ArrayList<Pipeline> p = w.identifyPipeline();
		int num = p.size();
		Log.printLine("Number pipeline "+ num);
		for(int i = 0; i < num; i++) {
			for(MyTask t: p.get(i)) {
				Log.printLine("tasks in p: "+ t.getCloudletId());
			}
		}

		/*2)set sub deadline of tasks*/
		boolean feasible = w.assignSubDeadlineTask(fastVmMips);
		if(!feasible) {
			Log.printLine("Workflow Cant run in this deadline");
			return;

		}
//		w.sum(fastVmMips);
		/**first create some Vms**/
		LinkedHashMap<MyVm, Double> vmoffers = VMOffersGoogle.getVmOffers();
		for(MyVm vm :vmoffers.keySet()) {
			initnewVmList.add(vm);
		}
		
		/*3) send scheduling event*/
		sendNow(getId(),CloudSimTags.SCHEDULING);

	}

	/**
	 * create new VM from that type
	 * @param mips
	 * @return
	 */
	private MyVm createNewVm(int pe) {
		int vmId = requestedVmCount++;
		MyVm newVm = new MyVm (vmId, 0, pe* 2.75 * MyConstant.slowMips, pe, pe* 3750, 10000, 0, "" ,null);
		newVm.setBrokerId(getId());
		return newVm;
	}

	/**
	 * when deadline reach or all of tasks in WorkFlow execute successfully 
	 * then run this method
	 * first: terminate all of VMs 
	 * then send finish event to data center to finish execution and print output
	 */
	private void endSection() {
		for(Integer key: allVmMap.keySet()){
			sendNow(dataCenterId, CloudSimTags.TERMINATE_VM_IMMIDIATELLY, allVmMap.get(key));			
		}
		finish = true;
		FinishTime = CloudSim.clock();			
		sendNow(dataCenterId, CloudSimTags.FINISH_SECTION);
	}

	/**
	 * check WorkFlow is finish or not
	 * @return
	 */
	private boolean checkFinish() {
		if(w.isWorkflowFinished()){
			return true;			
		}		
		return false;
	}

	/**
	 * this method print all message that should print in broker
	 **/
	private void printLog(String message){
		if(MyConstant.BROKER_DEBUG)
			System.out.println(TAG + message+ " at "+ CloudSim.clock());
	}	

	/**
	 * at the end shutdown entity from DataCenter broker to finish execution
	 * and print Cost and makeSpan of WorkFlow and information about tasks
	 */
	@Override
	public void shutdownEntity() {
		super.shutdownEntity();
		DecimalFormat df = new DecimalFormat("#.##");
		double cost = (double)(MyDatacenter.totalCost);
		System.out.println(w.toString());
		System.out.println("______________________________________________________________________________________________________________________________");
		System.out.println("Cost of Workflow :"+ cost);
		System.out.println("Makespan : " + (MakeSpan - startTime) + " seconds");
	}
}
