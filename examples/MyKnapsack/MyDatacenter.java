package MyKnapsack;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.CloudSim;
import org.cloudbus.cloudsim.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import MyKnapsack.MyTask;
import MyKnapsack.MyVm;

public class MyDatacenter extends Datacenter{
	private static final String TAG = "DataCenter ";
	private boolean finish;
	static double totalCost = 0.0 ;
	Hashtable<Integer,MyVm> instantiatedVms;
	static int totalNum = 0;
	/*constructor*/
	public MyDatacenter(String name, 
						DatacenterCharacteristics datacentercharactristic, 
						VmAllocationPolicy vmAllocationPolicy, 
						List<Storage> storagelist, 
						double schedulingInterval) throws Exception {
		super(name,
				datacentercharactristic,
				vmAllocationPolicy,
				storagelist,
				schedulingInterval);
		instantiatedVms = new Hashtable<Integer,MyVm >();
		finish = false;
	}
	
	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()) {
		
			case CloudSimTags.REQUEST_VM:
				processRequestVm(ev);
				break;
			case CloudSimTags.TERMINATE_VM_IMMIDIATELLY:
				MyVm vm = (MyVm)ev.getData();				
				processTerminateVm(vm);
				break;	
							
			case CloudSimTags.SUBMIT_TASK:
				processSubmitTask(ev);
				break;
				
			case CloudSimTags.VM_EXECUTION_COMPLETED:
				processVmExecCompleted(ev);
				break;
				
			case CloudSimTags.REQUEST_VM_FAILED:
				MyVm vmh = (MyVm)ev.getData();
				Log.printLine("Vm" + vmh.getId() + "failes by some reasons");
				break;
				
			case CloudSimTags.FINISH_SECTION:
				this.finish = true;
				break;
				
			default: Log.printLine("Warning" + CloudSim.clock() + ":" + this.getName() + " Unkown Event tag" + ev.getTag());
		}
 	}
	/**
	 * in this method V run the task assign to it 
	 * and send task complete  event to broker 
	 * @param ev
	 */
	private void processVmExecCompleted(SimEvent ev) {
		MyVm vm = (MyVm) ev.getData();
		int numTimeSlot = 0;
		double completeTime = 0.0;
		if(vm.getState() != MyVm.VM_TERMINATED) {
			MyTask task = vm.ProcessTaskcompleted();
			completeTime = vm.getVmFinishTime();
			if(completeTime > vm.getEndTimeSlot()) {
				numTimeSlot = (int) Math.ceil(((completeTime - vm.getVmInitiatedTime()) ) / MyConstant.TIME_CYCLE);
				vm.setNumberTimeSlot(numTimeSlot);
				vm.setEndTimeSlot((numTimeSlot) * MyConstant.TIME_CYCLE + vm.getVmInitiatedTime());
				vm.setRemainingTimeSlot(vm.getEndTimeSlot() - completeTime);	
				sendNow(vm.getBrokerId(), CloudSimTags.VM_IDLE, vm);						
			}
			else {
				if(vm.getState() != MyVm.VM_TERMINATED) {
					vm.setRemainingTimeSlot(vm.getEndTimeSlot() - completeTime);
					sendNow(vm.getBrokerId(), CloudSimTags.VM_IDLE, vm);
				}
			}
			
			System.out.println("______________________________________________________________________________________________________________________________");
			printLog(" VM EXECUTION COMPLETE IN DC:/ " + " Vm "+ vm.getId() + " with Remaining Time Slot " + vm.getRemainingTimeSlot()+ " completed Time ");
			sendNow(task.getBrokerId(), CloudSimTags.TASK_COMPLETED , task);			
		}
	}
		
	/**
	 * terminates VM
	 * @param vm
	 * @param reason
	 */
	static int slot = 0;
	static int wasteTimeslot = 0;
	private void processTerminateVm(MyVm vm){		
		if(vm.getState() != MyVm.VM_TERMINATED){
			vm.setState(MyVm.VM_TERMINATED);
			double cost = 0.0;
			Log.printLine(" vm " + vm.getId() + " num core "+ vm.getNumberOfPes() + " num slot " + vm.getNumberTimeSlot() +" Vm Cost " + vm.getNumberTimeSlot() * vm.getCostPerMinute() + " vm initiated at " +vm.getVmInitiatedTime() + " vm Endtime "+vm.getVmFinishTime());
			Log.printLine("waste = " + (60 - (Math.round(vm.getVmFinishTime() - vm.getVmInitiatedTime()))%60));
			wasteTimeslot += vm.getNumberOfPes() * (60 - (Math.round(vm.getVmFinishTime() - vm.getVmInitiatedTime()))%60);
			
			slot+= vm.getNumberTimeSlot() * vm.getNumberOfPes();
			Log.printLine("StringArrayVM "+ vm.tasksSequence[vm.getId()] );
			cost = (vm.getNumberTimeSlot() * vm.getCostPerMinute()) + vm.getCostPerMinute()* 1 * (vm.getMips()/MyConstant.fastMips);
			totalCost += cost;
//			send(vm.getBrokerId(),MyConstant.VM_Deprovisioning_Delay, CloudSimTags.VM_TERMINATED, vm);			
			sendNow(vm.getBrokerId(), CloudSimTags.VM_TERMINATED, vm);
		}		
	}
	/**
	 * process of submit task in vm
	 * if vm is idle then go to submit method in vm class and execute task in vm 
	 * @param ev
	 */
	private void processSubmitTask(SimEvent ev) {
		MyTask t = (MyTask) ev.getData();
		int vmId = t.getVmId();		
		double completeTime = 0;
		
		if(instantiatedVms.containsKey(vmId)) {
			MyVm vm = instantiatedVms.get(t.getVmId());
			if(vm.getState() == MyVm.VM_IDLE) {
				boolean issubmitted =  vm.SubmitTask(t);
				if(issubmitted) {
					completeTime = vm.execute();
					vm.setVmFinishTime(completeTime);  
					send(getId(), completeTime - CloudSim.clock() , CloudSimTags.VM_EXECUTION_COMPLETED,vm);
				}	
				System.out.println("______________________________________________________________________________________________________________________________");
				printLog(" TASK SUBMMITION IN DC:/ " + "Task id# "+ t.getCloudletId() + " with execution Time "+ t.getExecutionTimeTask() + " started in vm " + vmId);				
				return;
			}
		}
	}

	/**
	 * create VM requested from broker with 30 second delay
	 * @param ev
	 */
	public void processRequestVm(SimEvent ev) {
		MyVm vm = (MyVm) ev.getData();
		vm.setBrokerId(ev.getSource());
		vm.setInitiatedTimeVm(CloudSim.clock());
		vm.setState(MyVm.VM_IDLE);
		vm.setNumberTimeSlot(1);
		/*put vm ininstantiatedVms maps of initiatedvm*/
		instantiatedVms.put(vm.getId(), vm);
		vm.setEndTimeSlot((vm.getVmInitiatedTime() + vm.getNumberTimeSlot() *  MyConstant.TIME_CYCLE));
//		vm.setNextStartTimeslot(vm.getEndTimeSlot() + 0.0001);
		totalNum = instantiatedVms.size();

		/*send to broker req vm success*/
		sendNow(ev.getSource(), CloudSimTags.REQUEST_VM_SUCCESS ,vm);

		System.out.println("______________________________________________________________________________________________________________________________");
		printLog(" VM INITIATED IN DC:/" + " vm# " + vm.getId() + " is initiated with core " + vm.getNumberOfPes());	
		return;
	}
	/**
	 * print string message in DataCenter class
	 * @param message
	 */
	private void printLog(String message){
		if(MyConstant.DATA_CENTER_DEBUG)
			System.out.println(TAG + message + " at "+CloudSim.clock());
	}
}