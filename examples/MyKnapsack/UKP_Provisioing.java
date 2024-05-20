package MyKnapsack;

public final class UKP_Provisioing {
	// Returns the minimum cost with knapsack
	// W is the number of tasks in a bag
	public KnapsackResult DP(int NumBagTask ,int[] maxInstance, double duration, int[] NumberTask, double[] Cost, double[] maxProcessTime, double d) {

		// dp[i] is going to store maximum value with knapsack capacity i.
		double dp[] = new double[NumBagTask + 1];

		int hint[] = new int[NumBagTask + 1];
		for(int k = 1; k < NumBagTask + 1; k++) {
			dp[k] = Double.MAX_VALUE;
			hint[k] = -1;
		}

		for(int i = 1; i <= NumBagTask; i++) {
			if(NumberTask[0] != 0) {
				if(i >= NumberTask[0]) {
					dp[i] = dp[i-NumberTask[0]]+Cost[0];
					hint[i] = 0;
				}else {
					dp[i] = Cost[0];
					hint[i] = 0;
				}
			}
			for(int j = 1 ;j < NumberTask.length; j++) {
				if(NumberTask[j] != 0) {
					if(NumberTask[j] <= i) {
						if(dp[i] > dp[i - NumberTask[j]] + Cost[j]) {
							dp[i] = dp[i - NumberTask[j]] + Cost[j];
							hint[i] = j;
						}
					}
					else {
						if(dp[i] > Cost[j]) {
							dp[i] =  Cost[j];
							hint[i] = j;
						}
					}
				}
			}
		}
		int i = NumBagTask;
		KnapsackResult result = new KnapsackResult();
		result.cost = dp[i];
		while (i != 0) {
			int vmId = hint[i];
			int possibleTask = Math.min(i,NumberTask[vmId]);
			
			result.resources.add(new ResourceProvisioning(vmId, 1 , possibleTask,1));
			i -= possibleTask;
		}
		return result;
	}
}