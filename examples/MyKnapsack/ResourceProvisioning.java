package MyKnapsack;
public class ResourceProvisioning {
	Integer vmid;
	Integer numberVm;
	int NT;
	int numberBillingPeriod;
	ResourceProvisioning(int hint, int numVm,int numberTask, int numberBill) {
		this.vmid = hint;
		this.numberVm = numVm;
		this.NT = numberTask;
		this.numberBillingPeriod = numberBill;
	}
	
}
