package MyKnapsack;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class MainClass {

	public static void main(String[] args) {
		
		// First step: Initialize the CloudSim package. It should be called
		// before creating any entities.
		try {
			int num_user = 1;
			Calendar calender = Calendar.getInstance();
			boolean trace_flag = false;
			
		// Initialize the CloudSim library
			CloudSim.init(num_user, calender, trace_flag);
			
		// Second step: Create Datacenters
		//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			MyDatacenter dc = CreateDatacenter("datacenter_0");
		
		//Third step: Create Broker	
			DatacenterBroker dcb = CreateBroker("my_broker");
			int brokerId = dcb.getId();
		
		//Fifth step: start the simulation
			CloudSim.startSimulation();
			List<Cloudlet> newList = dcb.getCloudletReceivedList();
			Log.printLine("Received " + newList.size() + " cloudlets");
			
		//Six step: stop the simulation
			CloudSim.stopSimulation();
			Log.printLine("Knapsack scheduling finished!");
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}

	}

	private static DatacenterBroker CreateBroker(String string) {
		DatacenterBroker broker = null;
		 /*WRPSAlgorithm
		  * WRPSAlgorithm_SpareLp
		  * SubdeadlineAlgorithm
		  * MILPAlgorithm
		  * MILPAlgorithmSpareTimeLp*/
		try {
			broker = (DatacenterBroker) new WRPSAlgorithm_SpareLp("My_broker");
			
		} catch (Exception e) {
			e.printStackTrace();		
		}
		return broker;
	}

	private static MyDatacenter CreateDatacenter(String string) {
		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		//    Machines
		List<Host> hostList = new ArrayList<Host>();
			
		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine
		List<Pe> peList = new ArrayList<Pe>();
			
		double mips =  VMOffersGoogle.getMips(VMOffersGoogle.n1_standard_1);
			
		// 3. Create PEs and add these into the list
		peList.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList.add(new Pe(1, new PeProvisionerSimple(mips)));
		
		
		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int hostId = 0;
		int ram = 2048;
		int bw = 10000;
		long storage = 1000000;/*Mbyte*/
		
		hostList.add(
					new Host(
							hostId, 
							new RamProvisionerSimple(ram), 
							new BwProvisionerSimple(bw), 
							new MyStorage(storage), 
							peList, 
							new VmSchedulerSpaceShared(peList)));
		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		String src = "x86";
		String os ="Linux";
		String vmm = "xen";
		double time_Zone = 10;
		double costpersec = 3.0;
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.001;	// the cost of using storage in this resource
		double costPerBw = 0.1;			// the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		DatacenterCharacteristics dcc = new DatacenterCharacteristics(src, os, vmm, hostList, time_Zone, costpersec, costPerMem, costPerStorage, costPerBw);
		
		// 6. Finally, we need to create a PowerDatacenter object.
		MyDatacenter dc = null;
		
		try {
			dc = new MyDatacenter("my_datacenter", dcc, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return dc;
	}
}
