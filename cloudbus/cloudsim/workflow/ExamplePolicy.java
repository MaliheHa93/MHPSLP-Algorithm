package cloudsim.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Vm;

/**
 * This class implements an example scheduler that creates 1 VM for each task.
 * The VM is the best possible.
 *
 */
public class ExamplePolicy extends Policy {
			
	List<Vm> vmOffersList;
	
	int vmId=0;
		
	@Override
	public void doScheduling(long availableExecTime, VMOffers vmOffers) {
				
		Enumeration<DataItem> dataIter = dataItems.elements();
		while(dataIter.hasMoreElements()){
			DataItem item = dataIter.nextElement();
			dataRequiredLocation.put(item.getId(), new HashSet<Integer>());
		}
		
		//===== 1.Determine available computation services =====
		
		//what's the best VM available?
		vmOffersList = getVmOfferList();

		for(Task ti:tasks){
			//use only the best available VM, one task per VM
			Vm instance = vmOffersList.get(vmOffersList.size()-1);
			Vm newVm = new Vm(vmId,ownerId,instance.getMips(),instance.getNumberOfPes(),instance.getRam(),instance.getBw(),instance.getSize(),"",new CloudletSchedulerTimeShared());
			int cost = vmOffers.getCost(newVm.getMips(), newVm.getRam(), newVm.getBw());
			provisioningInfo.add(new ProvisionedVm(newVm,0,availableExecTime,cost));
			ArrayList<Task> tList = new ArrayList<Task>();
			tList.add(ti);
			schedulingTable.put(newVm.getId(), tList);
			ti.setVmId(newVm.getId());
			
			//set data dependencies info
			for(DataItem data:ti.getDataDependencies()){
				if(!dataRequiredLocation.containsKey(data.getId())){
					dataRequiredLocation.put(data.getId(), new HashSet<Integer>());
				}
				dataRequiredLocation.get(data.getId()).add(newVm.getId());
			}
			
			for(DataItem data:ti.getOutput()){
				if(!dataRequiredLocation.containsKey(data.getId())){
					dataRequiredLocation.put(data.getId(), new HashSet<Integer>());
				}
			}
			
			vmId++;
		}
	}

	private List<Vm> getVmOfferList(){
		LinkedList<Vm> offers = new LinkedList<Vm>();
		
		//sorts offers
		LinkedList<Entry<Vm,Integer>> tempList = new LinkedList<Entry<Vm,Integer>>();
		Hashtable<Vm, Integer> table = vmOffers.getVmOffers();
		
		Iterator<Entry<Vm, Integer>> iter = table.entrySet().iterator();
		while(iter.hasNext()){
			tempList.add(iter.next());
		}
		Collections.sort(tempList, new OffersComparator());
		for(Entry<Vm, Integer> entry:tempList){
			offers.add(entry.getKey());
		}
		
		System.out.println("***********************************************");
		for(Vm vm:offers){
			System.out.println("** Vm memory:"+vm.getRam() + " vm mips:"+vm.getMips() + " vm price:"+ table.get(vm));
		}
		System.out.println("***********************************************");
		
		return offers;
	}
}
