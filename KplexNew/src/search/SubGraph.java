package search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SubGraph {
	private ArrayList<Integer> res; 
	//备选节点
	private ArrayList<Integer> candidate;
	private ArrayList<Integer> not;
	//不可选节点
	//private ArrayList<Integer> noUse;
	//备选节点度数
	//private HashMap<Integer,Integer> degree;
	
	public SubGraph(int size)
	{
		res = new ArrayList<Integer>(); 
		candidate = new ArrayList<Integer>(size);
		not = new ArrayList<Integer>();

	}

	public void setRes(ArrayList<Integer> init)
	{
		this.res.addAll(init);
	}
	public void setRes(ArrayList<Integer> init,int y)
	{
		this.res.addAll(init);
		this.res.add(y);
	}
	public ArrayList<Integer> getRes()
	{
		return this.res;
	}
	public void setCandidate(ArrayList<Integer> init)
	{
		this.candidate.addAll(init);
	}
	public ArrayList<Integer> getCandidate()
	{
		return this.candidate;
	}
	public ArrayList<Integer> getNot() {
		return not;
	}

	public void setNot(ArrayList<Integer> not) {
		this.not.addAll(not);
	}
	public void setNot(ArrayList<Integer>not, int n){
		this.not.addAll(not);
		this.not.add(n);
	}
//	public String toString()
//	{
//		String re = "";
//		String key0="",key1="",key2="";
//		key0=res.toString();
//		key1=candidate.toString();
//		//key2=noUse.toString();
//		//key2=degree.toString();
//		re+=key0.substring(key0.indexOf("[")+1, key0.lastIndexOf("]"));
//		re+="%";
//		re+=key1.substring(key1.indexOf("[")+1, key1.lastIndexOf("]"));
//		//re+="%";
//		//re+=key2.substring(key2.indexOf("[")+1, key2.lastIndexOf("]"));
//		return re;
//	}
//
//	
}
