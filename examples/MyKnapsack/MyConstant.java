package MyKnapsack;
import MyKnapsack.Workflow;
public class MyConstant {
	/**Montage1000[46.3535,69.5303,92.7071,115.8839,185.414,370.828]**/
	/**epigenomics997[4256.404164,6384.606245999999,8512.808328,10641.010409999999]**/
	/**ligo1000[176.464566,264.696849,352.929132,441.161415]**/
	/**sipht1000[664.4014015-671.0454155150001,996.60210225,1328.802803,1661.0035037500002]**/
	/*	Deadline d1 5.846339
		Deadline d2 8.7695085
		Deadline d3 11.692678
		Deadline d4 14.615847500000001
	*/
	/*Vm constant*/
	public static final int VM_Provisioning_Delay = 30;
	public static final int VM_Deprovisioning_Delay = 3;

	/*deadline constant*/
	public static double DEADLINE =1661.0035037500002;

	/*interval of scheduling*/
	public static final double SCHEDULING_INTERVAL = 0.01;

	public static final long slowMips = 2750;
	
	public static final long fastMips = 60500;
	
	public static final double TIME_CYCLE = 60.0; /**second*/

	public static final boolean DEADLINE_REACH = false;

	public static final int bw = 1000;

	//debug options
	public static boolean DATA_CENTER_DEBUG = true ;

	public static boolean BROKER_DEBUG = true;
}
