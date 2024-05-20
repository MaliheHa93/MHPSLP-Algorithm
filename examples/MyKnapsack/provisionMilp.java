package MyKnapsack;
import org.cloudbus.cloudsim.Log;

import ilog.cplex.*;
import ilog.concert.*;
public class provisionMilp {

	//	public static void main(String[] args) {
	//		double[] costVm = {0.105,0.21,0.42,0.84};
	//		double[]runTime = {14,6,2,1};
	//		int[] maxInstanceIndex = {10,20,40,80};
	//		solveMilp(50,maxInstanceIndex , 500, costVm, runTime, 0.1);
	//
	//	}
	KnapsackResult result = new KnapsackResult();
	public KnapsackResult solveMilp(int n , int[] maxInstanceIndex , double deadline, int[] numTask, double costVm[], double[] maxProcessTimeTask, double IntTol) {
		try {
			IloCplex cplex = new IloCplex();
//			double tol= IloCplex.Param.MIP.Tolerances.MIPGap;
			/*Variables*/
			String costBot= "CostBot";
			IloNumVar cost_Bot 			   = cplex.numVar(0,Double.MAX_VALUE ,costBot);
			IloNumVar[][] UnitTime         = new IloNumVar[4][];
			IloIntVar[][] NumberTask       = new IloIntVar[4][];
			IloIntVar[][] LeasedVm         = new IloIntVar[4][];
			IloIntVar[][] NumberBillingPer = new IloIntVar[4][];
		
			for(int vmt = 0; vmt < 4;vmt++) {
				UnitTime[vmt]         = new IloNumVar[maxInstanceIndex[vmt]];
				NumberTask[vmt]       = new IloIntVar[maxInstanceIndex[vmt]];
				LeasedVm[vmt]		  = new IloIntVar[maxInstanceIndex[vmt]];
				NumberBillingPer[vmt] = new IloIntVar[maxInstanceIndex[vmt]];				 
			}

			for(int vmt = 0; vmt < 4; vmt++) {
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {
					String unitTime   = "UnitTime "      + (vmt) + " " + (k);
					String numberTask = "NumberTask "    + (vmt) + " " + (k);
					String leasedVm   = "LeasedVm "      + (vmt) + " " + (k);
					String Numberbill = "NumberBilling " + (vmt) + " " + (k);

					UnitTime[vmt][k]         = cplex.numVar(0, Double.MAX_VALUE  ,unitTime);
					NumberTask[vmt][k]       = cplex.intVar(0, Integer.MAX_VALUE ,numberTask);//integer variable
					LeasedVm[vmt][k]         = cplex.boolVar(leasedVm);//binary variable
					NumberBillingPer[vmt][k] = cplex.intVar(0, Integer.MAX_VALUE ,Numberbill);//integer variable	
				}					
			}

			/*1) Cost constraint */
			IloLinearNumExpr costVmExpr = cplex.linearNumExpr();
			for(int vmt = 0; vmt < 4; vmt++) {
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {
					costVmExpr.addTerm(costVm[vmt], NumberBillingPer[vmt][k]);			
				}			
			}
			cplex.addGe(cost_Bot,costVmExpr);	

			/*2,3) u/t < p  &&  u/t - p > IntTol-1 */
			for(int vmt = 0; vmt < 4; vmt++) {
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {
					IloLinearNumExpr UnitTimeExpr  = cplex.linearNumExpr();
					IloLinearIntExpr BillingPerExpr  = cplex.linearIntExpr();	

					UnitTimeExpr.addTerm((1 / MyConstant.TIME_CYCLE), UnitTime[vmt][k]);					
					BillingPerExpr.addTerm(1, NumberBillingPer[vmt][k]);

					cplex.addLe(UnitTimeExpr,BillingPerExpr);
					cplex.addGe(cplex.diff(UnitTimeExpr, BillingPerExpr),(IntTol - 1));
				}
			}

			/*4) N <= n*L */
			for(int vmt = 0; vmt < 4 ;vmt ++) {	
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {	
					IloLinearIntExpr LeasedVmExpr   = cplex.linearIntExpr();
					IloLinearIntExpr NumberTaskExpr = cplex.linearIntExpr();
					NumberTaskExpr.addTerm(1, NumberTask[vmt][k]);
					LeasedVmExpr.addTerm(n, LeasedVm[vmt][k]);

					cplex.addLe(NumberTaskExpr, LeasedVmExpr);
				}
			}

			/*5) sigma(sigma(N)) = n */
			IloLinearIntExpr NumberTaskExpr = cplex.linearIntExpr();
			for(int vmt = 0; vmt < 4 ;vmt ++) {
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {	
					NumberTaskExpr.addTerm(1, NumberTask[vmt][k]);
				}			
			}
			cplex.addEq(NumberTaskExpr, n);

			/*6) N >= L */
			for(int vmt = 0; vmt < 4 ;vmt ++) {
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {
					IloLinearIntExpr NumberTaskExpr1 = cplex.linearIntExpr();
					IloLinearIntExpr LeasedVmExpr   = cplex.linearIntExpr();

					NumberTaskExpr1.addTerm(1, NumberTask[vmt][k]);
					LeasedVmExpr.addTerm(1, LeasedVm[vmt][k]);
					
					cplex.addGe(NumberTaskExpr1, LeasedVmExpr);
				}						
			}

			/*7) U < deadline_bot */
			for(int vmt = 0; vmt < 4 ;vmt ++) {
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {
					IloLinearNumExpr UnitTimeExpr  = cplex.linearNumExpr();
					UnitTimeExpr.addTerm(1, UnitTime[vmt][k]);
					cplex.addLe(UnitTimeExpr, deadline);
				}		
			}

			/*8) calculate U[vmt][k] */
			for(int vmt = 0; vmt < 4 ;vmt ++) {
				for(int k = 0; k < maxInstanceIndex[vmt]; k++) {
					IloLinearNumExpr UnitTimeExpr  = cplex.linearNumExpr();

					UnitTimeExpr.addTerm(UnitTime[vmt][k], 1);
					UnitTimeExpr.addTerm(NumberTask[vmt][k], - maxProcessTimeTask[vmt]);
					UnitTimeExpr.addTerm(0.0, LeasedVm[vmt][k]);

//					cplex.addGe(UnitTimeExpr,  maxProcessTimeTask[vmt]);	
					cplex.addEq(UnitTimeExpr,0);
				}			
			}

			/*objective function */
			IloLinearNumExpr expr = cplex.linearNumExpr();
			expr.addTerm(1.0, cost_Bot);
			cplex.addMinimize(expr);

			/*export model in lp file */
			cplex.exportModel("costvm1.lp");
			
			/**
			 * Sets how often CPLEX reports about iterations during simplex optimization
			 * 0;No iteration messages until solution
			 * 1;Iteration information after each refactoring;default
			 * 2;Iteration information for each iteration
			 */
			cplex.setParam(IloCplex.Param.Simplex.Display, 2);

			
//			double start = cplex.getCplexTime();//seconds
			
			/**
			 * Decides how computation times are measured for 
			 * both reporting performance and terminating optimization 
			 * when a time limit has been set. 
			 * Small variations in measured time on identical runs may be expected 
			 * on any computer system with any setting of this parameter.
			 * 0;Automatic: let CPLEX choose
			 * 1;CPU time
			 * 2;Wall clock time
			 */
//			cplex.setParam(IloCplex.Param.ClockType, 2);

			/**
			 * Sets a relative tolerance on the gap between the 
			 * best integer objective and the
			 * objective of the best node remaining
			 */
//			cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap,1e-9);	

			/*solve the model */	
			boolean solved = cplex.solve();
			
			/*the time spend for running model*/
//			double elapsed = cplex.getCplexTime() - start;

			if(solved) {
				for(int vmt = 0; vmt < 4; vmt++) {
					for(int k = 0; k < maxInstanceIndex[vmt]; k++){
						int numberTask = (int)Math.round(cplex.getValue(NumberTask[vmt][k]))  ;
						if(numberTask >= 1) {
							int numBill =(int)Math.ceil(cplex.getValue(NumberBillingPer[vmt][k]) - 0.0001);
							result.resources.add(new ResourceProvisioning(vmt , 1 ,numberTask,numBill));
							System.out.println("UnitTime "      + "[" + vmt + "]" + "[" + k + "]" +" = " + cplex.getValue(UnitTime[vmt][k]));
							System.out.println("NumberTask "    + "[" + vmt + "]" + "[" + k + "]" +" = " + numberTask);
							System.out.println("LeasedVm "      + "[" + vmt + "]" + "[" + k + "]" +" = " + cplex.getValue(LeasedVm[vmt][k]));
							System.out.println("NumberBilling " + "[" + vmt + "]" + "[" + k + "]" +" = " + numBill);
						}					
					}
				}
				result.cost = cplex.getValue(cost_Bot);
				Log.printLine("solution status         + ========> " + cplex.getStatus());
				Log.printLine("Cplex Status            + ========> " + cplex.getCplexStatus());
				Log.printLine("Cplex Time finish       + ========> " + cplex.getDetTime());
				Log.printLine("best Objective value    + ========> " + cplex.getBestObjValue());
				Log.printLine("Objective value         + ========> " + cplex.getObjValue());
//				Log.printLine("runTime of cplex        + ========> " + elapsed);
			}
			cplex.end();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;	
	}
}