package search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class sGraph {
	//已经确定的节点
	private ArrayList<Integer> res; 
	//备选节点
	private ArrayList<Integer> candidate;
	//不可选节点
	//private ArrayList<Integer> noUse;
	//备选节点度数
	//private HashMap<Integer,Integer> degree;
	
	public sGraph(int size)
	{
		res = new ArrayList<Integer>();
		//noUse = new ArrayList<Integer>();
		candidate = new ArrayList<Integer>(size);
	//	degree = new HashMap<Integer,Integer>(size);
		res.clear();
		//noUse.clear();
		candidate.clear();
	//	degree.clear();
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
	/*
	public void setNouse(ArrayList<Integer> init)
	{
		this.noUse.addAll(init);
	}
	public ArrayList<Integer> getNouse()
	{
		return this.noUse;
	}
	*/
	public void setCandidate(ArrayList<Integer> init)
	{
		this.candidate.addAll(init);
	}
	public ArrayList<Integer> getCandidate()
	{
		return this.candidate;
	}
	/*
	public void setDegree(HashMap<Integer,Integer> init)
	{
		this.degree.putAll(init);
	}
	public HashMap<Integer,Integer> getDegree()
	{
		return this.degree;
	}
	*/
	public String toString()
	{
		String re = "";
		String key0="",key1="",key2="";
		key0=res.toString();
		key1=candidate.toString();
		//key2=noUse.toString();
		//key2=degree.toString();
		re+=key0.substring(key0.indexOf("[")+1, key0.lastIndexOf("]"));
		re+="%";
		re+=key1.substring(key1.indexOf("[")+1, key1.lastIndexOf("]"));
		//re+="%";
		//re+=key2.substring(key2.indexOf("[")+1, key2.lastIndexOf("]"));
		return re;
	}

	
}
