package MyKnapsack;

import org.cloudbus.cloudsim.Log;

import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class LpSpareSolve {
	spareResult sResult = new spareResult();
	
	public spareResult LpSolve(double[] maxRunTime, double[] numberTaskLevel, double spareTimeworkflow, double[] sumRuntimeLevel) {
		try {
			IloCplex cplex = new IloCplex();
			
			String tMax = "tMax";
			IloNumVar Total = cplex.numVar(0, Double.MAX_VALUE, tMax);
			IloNumVar[] spareTime = new IloNumVar[numberTaskLevel.length + 1];
			
			for(int l = 1; l < numberTaskLevel.length;l++) {
				String st = "spareTime" + (l);
				if(numberTaskLevel[l]!=-1) {
					spareTime[l] = cplex.numVar(0, Double.MAX_VALUE, st);
				}else {
					spareTime[l] = cplex.numVar(0, 0,st);
				}
			}
			
			/*constraint*
			 /*runTime[l] + sp[l] / numberTaskLevel[l] * runTime[l] >= T*/		
			for(int l = 1; l < numberTaskLevel.length;l++) {	
				if(numberTaskLevel[l]!=-1 && maxRunTime[l] !=-1) {
					IloLinearNumExpr spareExpr = cplex.linearNumExpr();
					IloLinearNumExpr T = cplex.linearNumExpr();
					spareExpr.addTerm(spareTime[l], 1 /(sumRuntimeLevel[l]));
					T.addTerm(Total,1);
					cplex.addGe(cplex.diff(spareExpr,T) , -(maxRunTime[l] / (sumRuntimeLevel[l])));
				}	
			}
			
			/*2)sum spare = spateTimeW*/	
			IloLinearNumExpr spareExpr1 = cplex.linearNumExpr();
			for(int l = 1; l < numberTaskLevel.length;l++) {
				spareExpr1.addTerm(spareTime[l], 1.0);				
			}
			cplex.addEq(spareExpr1, spareTimeworkflow);
			
			/*objective*/
			IloLinearNumExpr expr = cplex.linearNumExpr();
			expr.addTerm(1.0, Total);
			cplex.addMaximize(expr);
			
			/*export model in lp file */
			cplex.exportModel("spareLevel.lp");
			
			/*solve the model */	
			boolean solved = cplex.solve();
			if(solved) {
				for(int l = 1; l < numberTaskLevel.length;l++) {
					if(numberTaskLevel[l]!=-1) {
						double spare  = cplex.getValue(spareTime[l]);
						System.out.println("spareTime "  + "[" + l + "]" +" = " + spare);
						sResult.spareList.add(new spareDistribution(l, spare));
					}				
				}
				Log.printLine("solution status         + ========> " + cplex.getStatus());
				Log.printLine("Cplex Status            + ========> " + cplex.getCplexStatus());
				Log.printLine("Cplex Time finish       + ========> " + cplex.getDetTime());
//				Log.printLine("best Objective value    + ========> " + cplex.getBestObjValue());
				Log.printLine("Objective value         + ========> " + cplex.getObjValue());
			}
			cplex.end();			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sResult;
	}	
}
