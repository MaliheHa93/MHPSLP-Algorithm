  package MyKnapsack;
import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.CplexEndedException;
public class ExampleMilp {

	public static void main(String[] args) {
		int n = 3;
		int m = 4;
		double[][] A = {{2,3,7},{1,1,0},{5,3,0},{0.6, 0.25,1}};
		double[]b = {1250, 250,900,232.5};
		double[]c = {41,35,96};
		solveMilp(n , m, c, A, b);
	}
	
	public static void solveMilp(int n , int m ,double[] c , double[][] A, double[]b) {
		try {
			IloCplex model = new IloCplex();
			IloIntVar[]  x = new IloIntVar[n];
			for(int i = 0; i <n ; i++) {
				x[i] =  model.intVar(0, Integer.MAX_VALUE);
			}
			IloLinearNumExpr obj = model.linearNumExpr();
			for(int j = 0 ; j <n ; j++) {
				obj.addTerm(c[j], x[j]);
			}
			model.addMinimize(obj);
			
			for(int k = 0; k < m ; k++) {
				IloLinearNumExpr constraint = model.linearNumExpr();
				for( int j = 0; j < n ; j++) {
					constraint.addTerm(A[k][j], x[j]);	
				}
				model.addGe(constraint, b[k]);
			}
			boolean solved = model.solve();
			if(solved) {
				double objValue = model.getObjValue();
				System.err.println(objValue);
			}
			for(int i = 0; i< n; i++) {
				System.err.println("x[" + (i+1) +"] ="+ model.getValue(x[i]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	public static void solveModel(int n, int m ,int[]c ,int[][]A, int[]b) {
		try {
			IloCplex model = new IloCplex();
			IloNumVar[]x = new IloNumVar[n];
			for(int i = 0; i <n ; i++) {
				x[i]= model.numVar(0, Double.MAX_VALUE);
//				IloLinearNumExpr
			}
		}catch(IloException e){
			e.printStackTrace();
		}
		
		
	}
}
