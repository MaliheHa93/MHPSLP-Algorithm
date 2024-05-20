package MyKnapsack;

import java.util.ArrayList;

import MyKnapsack.*;

public class BagofTask extends ArrayList<MyTask>{
	ArrayList<BagofTask> bot = null;
	double deadlinebot = -1;
	String key;
	boolean isreschedule;
	int index = 0;
	
	/**
	 * put task in bot
	 * @param bot
	 */
	public void putInbot(BagofTask bot) {
		bot = new BagofTask();
		this.bot.add(bot);
	}
	
	/**
	 * set max deadline of task in bot to deadline of bot
	 * @param d
	 */
	public void setDeadlineBot(double d) {
		this.deadlinebot = d;
	}
	
	/**
	 * set key of bot
	 * @param k
	 */
	public void setKey(String k) {
		key = k;
	}
	
	/**
	 * get key of bot
	 * @param k
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * get deadline of bot
	 * @return
	 */
	public double getDeadlineBot() {
		 return this.deadlinebot;
	}
	
	/**
	 * if bot still has ready task return true
	 * @return
	 */
	public boolean hasTaskReady() {
		boolean[] isready = new boolean[this.size()];
		int i = 0;
		boolean flag = true;
		
		for(MyTask t :this)	{
			if(t.getStatus() == MyTask.STATUS_READY) {
				isready[i] = true;
				i++;
			}
		}
		for(int j = 0; j < isready.length ; j++) {
			if(isready[j]) {
				flag = true;
			}
		}
		return flag;
	}
	
	
//	public boolean hasNotScheduled() {
//		boolean[] isnotschedule = new boolean[this.size()];
//		int i = 0;
//		boolean flag = true;
//		
//		for(MyTask t :this)	{
//			if(t.getStatus() != MyTask.STATUS_SCHEDULED) {
//				isnotschedule[i] = true;
//				i++;
//			}
//		}
//		
//		for(int j = 0; j < isnotschedule.length ; j++) {
//			if(isnotschedule[j]) {
//				flag = true;
//			}
//		}
//		return flag;
//	}
//	
//	public int numberNotSchedule() {
//		boolean[] isnotschedule = new boolean[this.size()];
//		int i = 0;
//		boolean flag = true;
//		for(MyTask t :this)	{
//			if(t.getStatus() == MyTask.STATUS_READY) {
//				isnotschedule[i] = true;
//				i++;
//			}
//		}
//		return i;
//	}
	
	/**
	 * remove scheduled task from bot
	 */
	public void removeFromBot(MyTask t) {
		if(this.contains(t)) {
			this.remove(t);
		}
	}
	
	/**
	 * get the next ready task from bot
	 * @return
	 */
	public MyTask getNextReadyTaskFromBot() {
		if(!isEmpty()) {
			 return get(0);
		}
		else
			return null;
	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	public int getNumberReadyTask() {
//		int num = 0;
//		for(MyTask t: this) {
//			if(t.getStatus() == MyTask.STATUS_READY) {
//				num++;
//			}
//		}
//		return num;
//	}
}