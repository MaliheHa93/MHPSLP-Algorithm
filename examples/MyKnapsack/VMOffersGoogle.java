package MyKnapsack;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;


public class VMOffersGoogle extends MyVm{
	

    static LinkedHashMap<MyVm,Double> vmOffersTable = new  LinkedHashMap<>();
	
	public ArrayList<MyVm> VmofferList = new ArrayList<>();
	
	public  final static int n1_standard_1 =  1;
	public  final static int n1_standard_2 =  2;
	public  final static int n1_standard_4 =  3;
	public  final static int n1_standard_8 =  4;

	
	public VMOffersGoogle(int id,int userId, double mips,int numberOfPes, int ram, long bw, long size,String vmm,
			CloudletScheduler cloudletScheduler) {		
		super(id,0, userId, ram, numberOfPes, bw, size,vmm, cloudletScheduler);
		
		}
	/**
	 * get Mips of every tasks
	 * @param tagname
	 * @return
	 */
	public static double getMips(int tagname) {
		switch(tagname) {
		
		case n1_standard_1:
			return 2.75 *   MyConstant.slowMips;
			
		case n1_standard_2:
			return 5.5  *   MyConstant.slowMips;
			
		case n1_standard_4:
			return 11   *   MyConstant.slowMips;
			
		case n1_standard_8:
			return 22   *  MyConstant.slowMips;
			
		default:
			  return 2.75 * MyConstant.slowMips;
		}	
	}
	
	/**
	 * get cost of every type vm
	 * @param tagname
	 * @return
	 */
	public static double getCost(int pe) {
		switch(pe) {		
		case 1:
			return 0.00105;
			
		case 2:
			return 0.0020895;
			
		case 4:
			return 0.004098405;
			
		case 8:
			return 0.00815582595;
			
		default:
			  return 0.00105;
		}
	}
	/**
	 * get cost of every type vm
	 * @param tagname
	 * @return
	 */
//	public static double getCost(int pe) {
//		switch(pe) {
//		
//		case 1:
//			return 0.00105;
//			
//		case 2:
//			return 0.0021;
//			
//		case 4:
//			return 0.0042;
//			
//		case 8:
//			return 0.0084;			
//		default:
//			  return 0.00105;
//		}	
//	}
	/**
	 * get total time slot of period in second
	 * @return
	 */
	public static long getTimeSlot() {
		return  60 ;//60 s
	}
	
	/**
	 * get boot time delay of vm types
	 * @return
	 */
	public long getBootTime() {
		return 30;
	}
	
	public static  LinkedHashMap<MyVm, Double> getVmOffers() {
		double mips = MyConstant.slowMips;
		if(vmOffersTable.isEmpty()) {
			vmOffersTable.put( new MyVm (0,0, 2.75  * mips, 1 ,  3750,  10000, 0, "", null), 0.00105);
			vmOffersTable.put( new MyVm (1,0, 5.5   * mips, 2 ,  7500,  10000, 0, "", null), 0.0021); 
			vmOffersTable.put( new MyVm (2,0, 11    * mips, 4 ,  15000, 10000, 0, "", null), 0.0042); 
			vmOffersTable.put( new MyVm (3,0, 22    * mips, 8 ,  30000, 10000, 0, "", null), 0.0084);  
		}
		return vmOffersTable;	
	}

	public static double getCost(double mips, int memory, double bw) {
		if(memory == 3750)  return  0.00105;
		if(memory == 7500)  return  0.00210525;
		if(memory == 15000) return  0.00422102625;
		if(memory == 30000) return  0.00846315763125;
		return 0;		
	}
	
	public MyVm getVmfromTag(int tag) {
		switch(tag) {
		case  n1_standard_1:
			return new MyVm (0,0, 2.75 *  MyConstant.slowMips,  1,  3750,  10000, 2750,  "" ,null);
			
		case n1_standard_2:
			return new MyVm (1,0, 5.5  *  MyConstant.slowMips,  2,  7500,  10000, 55000, "", null);
			
		case n1_standard_4:
			return new MyVm (2,0, 11   *  MyConstant.slowMips,  4,  15000, 10000, 11000, "", null) ;
			
		case n1_standard_8:
			return new MyVm (3,0, 22   *  MyConstant.slowMips,  8,  30000, 10000, 22000, "", null);
		
		default:
			return null;
		}
	}
	public int getPe(double mips) {
		if(mips == 2.75  *  MyConstant.slowMips) return 1;
		if(mips == 5.5   *  MyConstant.slowMips) return 2;
		if(mips == 11    *  MyConstant.slowMips) return 4;
		if(mips == 22    *  MyConstant.slowMips) return 8;
		return 0;
	}
	public static MyVm getVmfromid(int tag) {
		switch(tag) {
		case  0:
			return new MyVm(0,0, 2.75   *  MyConstant.slowMips,  1,  3750,  10000, 2750,  "" ,null);			
		case 1:
			return new MyVm (1,0, 5.5  *  MyConstant.slowMips,  2,  7500,  10000, 55000, "", null);			
		case 2:
			return new MyVm (2,0, 11   *  MyConstant.slowMips,  4,  15000, 10000, 11000, "", null) ;			
		case 3:
			return new MyVm (3,0, 22   *  MyConstant.slowMips,  8,  30000, 10000, 22000, "", null);
		default:
			return null;
		}
	}
}
