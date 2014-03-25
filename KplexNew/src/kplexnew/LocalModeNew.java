package kplexnew;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;

import search.sGraph;
import search.searchOneLeap.MyComparator;

public class LocalModeNew {
	//reduce数目
	static int reduceNumber = 36;
	//有意义的k-plex大小
	static int quasiCliqueSize = 5;
	//k-plex的k值大小
	static int k_plex = 2;
	public static int kPlexSize = 0;
	//将“一跳”信息读入内存，存在HashMap中
	public static HashMap<Integer, HashSet<Integer>> oneLeap = new HashMap<Integer, HashSet<Integer>>(
			7000);
	//数据集中的节点集合
	public static ArrayList<Integer> nodeSet = new ArrayList<Integer>(1000);
	//子状态集和结果集
	public static ArrayList<sGraph> stack = new ArrayList<sGraph>(6000);
	public static ArrayList<Integer> result = new ArrayList<Integer>(30);
	//loadbalance时候分散成多少份
	public static int totalPart = 36;// 分散成为多少部分
	//pick为不进行loadbalance的节点集，split为需要loadbalance的节点集
	public static ArrayList<Integer> pick = new ArrayList<Integer>();
	public static ArrayList<Integer> split = new ArrayList<Integer>();

	public static ArrayList<Integer> res = new ArrayList<Integer>();
	public static ArrayList<Integer> candidate = new ArrayList<Integer>(1000);
	public static HashMap<Integer, Integer> degree = new HashMap<Integer, Integer>(
			1000);
	public static int number = 0;
	public static int levelNumber = 0;
	public static int levelExtream=10;
	public static HashSet<Integer> hs = new HashSet<Integer>();
	public static void computeOneleapData(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		StringTokenizer stk;
		while ((line = reader.readLine()) != null) {
			stk = new StringTokenizer(line);
			int k = Integer.parseInt(stk.nextToken());
			HashSet<Integer> adj = new HashSet<Integer>();
			nodeSet.add(k);
			while (stk.hasMoreTokens()) {
				adj.add(Integer.parseInt(stk.nextToken()));
			}
			oneLeap.put(k, adj);
		}
		Collections.sort(nodeSet);
		long t1 = System.currentTimeMillis();
		//排序后，每个reduce只处理对应节点
		for (int i = 0; i < nodeSet.size(); i++) {
			//if (pick.contains(nodeSet.get(i)))
			if (nodeSet.get(i)%reduceNumber==0)
			{
				kPlexSize = 0;
				//当前所求节点
				int current = nodeSet.get(i);
				//if (pick.contains(current)) {
					stack.clear();
					res.clear();
					candidate.clear();
					// 构造起始状态，获得备选节点列表和度数列表
					// 此时res为T(+),candidate+res为Tx,candidate为T(-)
					res.add(current);
					//初始化备选集并按度数排序
					initialSGraph(candidate, current);
					int sum=0;
					//"备选集"的概念和kplexold不同，此处备选集包含“两跳”节点，是待分解的原始图
					//若“原始图”大小>=有意义的kplex大小时才进行计算
					//若只计算某个节点最大的k-plex时，需再加判断条件
					while((candidate.size()+1)>=quasiCliqueSize && (candidate.size()+1)>=kPlexSize)// && (candidate.size()+1)>=kPlexSize
					{
						if (judgeKplex2(res, candidate))//是kplex
						{// && sum >= kPlexSize
							//减少输出结果
							if(sum >= kPlexSize)
							{
							kPlexSize = sum;
							result.clear();
							result.addAll(res);
							result.addAll(candidate);
							//是clique,输出
							break;//剩下的部分不用再分啦
							}
						} else {
							//将备选集切分成若干小的状态，切分策略，此处为选择备选集的第一个节点
							//初始化时，备选集按度数排序
							//1.排序之后，从后面选取几个先算，即从后面取之后存入另一个栈，把栈里的状态算完
							//之后把剩余的状态再重新分散入栈，并将已经算出来的值一同发往其他计算节点
							//2.将状态栈中的状态，随机选取一部分，放入另一个栈中计算
							int y = candidate.get(0);// 其他节点，存在策略
							// 子图包含y
							ArrayList<Integer> canA = new ArrayList<Integer>();
							//分割状态，包含节点y，getT2获得y的邻节点与备选集的交集
							//y的两跳数据和当前candidate的交集
							getT2(canA, y,candidate);
							int p=canA.indexOf(y);
							if(p!=-1)
								canA.remove(p);
							sGraph sA = new sGraph(canA.size());
							sA.setCandidate(canA);
							sA.setRes(res,y);
							stack.add(sA);								
							// 子图不包含y，继续分解	
							candidate.remove(0);
						}
					}
					int number = 0;
					while (number < stack.size()) {
						sGraph here = stack.get(number);
						ArrayList<Integer> res = here.getRes();
						ArrayList<Integer> candidate = here.getCandidate();
						//分解之后的状态依次弹栈并计算
						computeKplex(res, candidate);
						levelNumber = 0;
						number++;
					}
					
				//}
				//需要进行loadbalance的节点，将状态分散
				
			}
		}
		long t2 = System.currentTimeMillis();
		System.out.println("========"+(t2-t1)/1000);
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		init();
		computeOneleapData(args[0]);
	}
	public static void init() throws NumberFormatException, IOException{
		FileReader fr = new FileReader(new File("/home/youli/CliqueHadoop/kplexnew_COMMON.txt"));
		BufferedReader bfr = new BufferedReader(fr);
		// 提取出所有的pick节点
		String record = "";
		pick.clear();
		while ((record = bfr.readLine()) != null) {
			String[] adjInfos = record.split(" ");
			for (int i = 1; i < adjInfos.length; i++)
				pick.add(Integer.valueOf(adjInfos[i]));
		}
		bfr.close();
		levelExtream = 50;
		reduceNumber = 132;
		quasiCliqueSize = 5;				
		k_plex = 2;
		totalPart = 32;
	}
	// 平均分配到各个计算节点之上,每个reduce保存一份“一跳”
	public static class oneLeapFinderPartitioner extends
			Partitioner<IntWritable, Text> {
		@Override
		public int getPartition(IntWritable key, Text value, int num) {
			return (int) ((key.get()) % num);
		}
	}

	// 获得集合T
	public static void getCandidate(ArrayList<Integer> candidate, int current) {
		for (Integer in : oneLeap.get(current))
			candidate.add(in);
		for (Integer ou : oneLeap.get(current)) {
			for (Integer in : oneLeap.get(ou))
				if (!candidate.contains(in) && in != current)
					candidate.add(in);
		}
	}
	public static void getCandidate2(ArrayList<Integer> candidate, int current) {
		hs.addAll(oneLeap.get(current));
		for (Integer ou : oneLeap.get(current)) {
			hs.addAll(oneLeap.get(ou));
		}
		candidate.addAll(hs);
		int p=candidate.indexOf(current);
		if(p!=-1)
			candidate.remove(p);
	}
	public static class MyComparator implements Comparator<Integer> {
		public int compare(Integer a, Integer b) {
			long n1 = degree.get(a);
			long n2 = degree.get(b);
			return n1 > n2 ? 1 : (n1 == n2 ? 0 : -1);
		}
	}
	public static class MyComparator2 implements Comparator<Integer> {
		public int compare(Integer a, Integer b) {
			long n1 = degree.get(a);
			long n2 = degree.get(b);
			return n1 > n2 ? -1 : (n1 == n2 ? 0 : 1);
		}
	}
	public static int getIntersectionNumber(HashSet<Integer> adj) {
		int number = 0;
		for (Integer a : adj) {
			if (hs.contains(a))
				number++;
		}
		return number;
	}
	public static void initDegree(ArrayList<Integer> candidate) {
		int number = 0;
		for (Integer in : candidate)// oneLeap中节点和candidate中节点的交集数
		{
			number = getIntersectionNumber(oneLeap.get(in));
			degree.put(in, number);
		}
	}
	// 初始化备选节点和其度数，并将备选节点按度数由小到大排序
	public static void initialSGraph(ArrayList<Integer> candidate, int current) {
		//获得current的“两跳”信息作为备选集
		getCandidate2(candidate, current);
		//初始化节点度数
		initDegree(candidate);
		//将节点按度数排序
		Collections.sort(candidate, new MyComparator());//节点按度数由小到大排列
		//Collections.sort(candidate, new MyComparator2());//节点按度数由大到小排列
	}

	public static void getCriticalSet(ArrayList<Integer> res,
			ArrayList<Integer> critSet) {
		int numberDisconnect = 0;
		for (Integer vo : res) {
			for (Integer vi : res) {
				if (vo != vi && disconnect(vo, vi))
					numberDisconnect++;
			}
			// 有些节点已经是边界了，新加的节点必须和这些节点都相连
			if (numberDisconnect == k_plex - 1)
				critSet.add(vo);
			numberDisconnect = 0;// 复位
		}
	}

	public static boolean disconnect(int a, int b) {
		if (!(oneLeap.get(a)).contains(b))
			return true;
		else
			return false;
	}

	public static void filterAgain(ArrayList<Integer> candidate,
			ArrayList<Integer> res) {
		int number = 0;
		ArrayList<Integer> remove = new ArrayList<Integer>();
		for (int i = 0; i < candidate.size(); i++) {
			number = 0;
			int v = candidate.get(i);
			for (Integer vres : res) {
				if (disconnect(v, vres))
					number++;
			}
			if (number > k_plex - 1)
				remove.add(v);
		}
		candidate.removeAll(remove);
		/*
		hs.clear();
		hs.addAll(candidate);
		hs.removeAll(remove);
		candidate.clear();
		candidate.addAll(hs);
		*/
	}

	public static void filterCandidate(ArrayList<Integer> res,
			ArrayList<Integer> candidate) {// ,HashMap<Integer,Integer>
																		// degree
		if (res.size() >= k_plex)// 否则即便是全部不连接也没关系，起不到过滤效果
		{
			ArrayList<Integer> critSet = new ArrayList<Integer>();
			getCriticalSet(res, critSet);
			ArrayList<Integer> intersection = new ArrayList<Integer>();
			if (critSet.size() > 0)// 节点临界，用于过滤
			{
				intersection.addAll(oneLeap.get(critSet.get(0)));// 先加入第一个元素
				for (int i = 1; i < critSet.size(); i++) {
					HashSet<Integer> adj = oneLeap.get(critSet.get(i));
					intersection.retainAll(adj);// 不断取交集
				}
				// 取intersection和candidate的交集，剔除两跳外数据，自然也将res中数据剔除了
				// res中数据不在candidate中
				candidate.retainAll(intersection);// 只有这些节点可以保留
			}
		}
		filterAgain(candidate, res);
	}
	public static boolean judgeKplex2(ArrayList<Integer> res,
			ArrayList<Integer> candidate) {//,ArrayList<Integer> splitter
		int number=0;
		int sr=res.size();
		int cr=candidate.size();
		boolean flag=false;
		if(sr<cr)//candidate的量大
		{
			candidate.addAll(res);
			for (Integer out : candidate) {
				number = 0;
				for (Integer in : candidate) {
					if (out != in && disconnect(out, in))
					{
						number++;
						if(number>k_plex-1)
							break;
					}
				}
				if (number > k_plex - 1)// 不是kplx
				{//新点不能是已有的
					//splitter.add(out);
					flag=true;
					break;
				}
			}
			int i=0;
			while(i<sr)
			{
				candidate.remove(candidate.size()-1);
				i++;
			}
			if(flag)//是跳出来的
				return false;
		}
		else
		{
			res.addAll(candidate);
			for (Integer out : res) {
				number = 0;
				for (Integer in : res) {
					if (out != in && disconnect(out, in))
					{
						number++;
						if(number>k_plex-1)
							break;
					}
				}
				if (number > k_plex - 1 )// 不是kplx
				{
					//splitter.add(out);
					flag=true;
					break;
				}
			}
			int i=0;
			while(i<cr)
			{
				res.remove(res.size()-1);
				i++;
			}
			if(flag)
				return false;
		}
		return true;//没有splitter
	}
	
	public static void getT2(ArrayList<Integer> canA, int y,ArrayList<Integer> candidate) {
		hs.clear();
		HashSet<Integer> yAdj = oneLeap.get(y);
		hs.addAll(yAdj);
		for (Integer i : yAdj)
		{
			yAdj=oneLeap.get(i);
			hs.addAll(yAdj);
		}
		for(Integer c : candidate)
		{
			if(hs.contains(c))//有冲突,hs为canA,和candidate的交集，就是有冲突的点
				canA.add(c);
		}
		//canA.addAll(hs);
	}
	public static void getT3(ArrayList<Integer> canA, int y,ArrayList<Integer> candidate) {
		HashSet<Integer> yAdj = oneLeap.get(y);
		canA.addAll(yAdj);
		for (Integer i : yAdj)
			for (Integer j : oneLeap.get(i)) {
				if (!canA.contains(j))
					canA.add(j);
			}
		canA.retainAll(candidate);
	}
	@SuppressWarnings("unchecked")
	public static void computeKplex(ArrayList<Integer> res,
			ArrayList<Integer> candidate) {
		int rSize = res.size();
		//根据k-plex定义过滤备选集(最多不与k-1个节点相连)
		filterCandidate(res, candidate);
		int canSizeN = candidate.size();
		int sum = rSize + canSizeN;
		if (sum >= quasiCliqueSize && sum >= kPlexSize) {//&& sum >= kPlexSize
			if (judgeKplex2(res, candidate))// 是kplex
			{ //&& sum >= kPlexSize
				if(sum >= kPlexSize)
				{
				kPlexSize = sum;
				result.clear();
				result.addAll(res);
				result.addAll(candidate);
				//是clique输出
				}
			} else {
				int y = candidate.get(0);// 其他节点，存在策略
				// 子图包含y
				// res中多了y
				ArrayList<Integer> resA = new ArrayList<Integer>();
				resA.addAll(res);
				resA.add(y);
				ArrayList<Integer> canA = new ArrayList<Integer>();
				canA.clear();
				getT2(canA, y,candidate);
				//getT3(canA, y,candidate);
				int p=canA.indexOf(y);
				if(p!=-1)
					canA.remove(p);				
				levelNumber++;
				//避免层数过多使得系统栈溢出，当levelNumber过大时(大于levelExtream)，将状态存栈
				if (levelNumber >= levelExtream) {
					sGraph sA = new sGraph(canA.size());
					sA.setCandidate(canA);
					sA.setRes(resA);
					stack.add(sA);
					
					resA.remove(resA.size()-1);
					candidate.remove(0);
					sGraph sB = new sGraph(candidate.size());
					sB.setCandidate(candidate);
					sB.setRes(resA);
					stack.add(sB);
					return;
				}
				//子图包含y
				computeKplex(resA, canA);
				levelNumber--;
				resA.remove(resA.size()-1);
				candidate.remove(0);
				//子图不包含y
				computeKplex(resA, candidate);
				levelNumber--;
				//维护状态栈层数在合理的大小
				if (levelNumber <= -10)
					levelNumber = 0;
			}
		}
	}
}
