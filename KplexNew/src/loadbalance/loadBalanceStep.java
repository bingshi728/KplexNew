package loadbalance;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;

import search.sGraph;
import search.searchOneLeap.MyComparator;

public class loadBalanceStep {

	static final int reduceNumber = 20;
	static int quasiCliqueSize = 5;
	static int k_plex = 3;
	public static int kPlexSize = 0;
	// 当前graph的节点列表
	public static HashMap<Integer, HashSet<Integer>> oneLeap = new HashMap<Integer, HashSet<Integer>>(
			7000);
	public static ArrayList<Integer> nodeSet = new ArrayList<Integer>(1000);
	public static ArrayList<sGraph> stack = new ArrayList<sGraph>(6000);
	public static ArrayList<Integer> result = new ArrayList<Integer>(30);
	public static int totalPart = 20;// 分散成为多少部分
	public static ArrayList<Integer> pick = new ArrayList<Integer>();

	public static ArrayList<Integer> res = new ArrayList<Integer>();
	public static ArrayList<Integer> candidate = new ArrayList<Integer>(1000);
	public static HashMap<Integer, Integer> degree = new HashMap<Integer, Integer>(
			1000);
	public static int number = 0;
	public static int levelNumber = 0;
	public static HashSet<Integer> hs = new HashSet<Integer>();
	public static int levelExtream=0;
	public static String path="";
	public static class loadBalanceMapper extends
			Mapper<LongWritable, Text, IntWritable, Text> {
		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String str = value.toString();
			int k = -1;
			int p = -1;
			String[] kvoriginal = str.split("\\t");
			k = Integer.parseInt(kvoriginal[0]);
			if (k == 0)
				return;
			else // if(k==-1)
			{
				String[] kv = kvoriginal[1].split("@");
				p = Integer.parseInt(kv[0]);
				context.write(new IntWritable(p), new Text(kv[1]));
			}
		}
	}

	public static class loadBalancePartitioner extends
			Partitioner<IntWritable, Text> {
		@Override
		public int getPartition(IntWritable key, Text value, int num) {
			return key.get() % num;// 平均分配到各个计算节点之上
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

	public static class MyComparator implements Comparator<Integer> {
		public int compare(Integer a, Integer b) {
			long n1 = degree.get(a);
			long n2 = degree.get(b);
			return n1 > n2 ? 1 : (n1 == n2 ? 0 : -1);
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
		getCandidate(candidate, current);
		initDegree(candidate);
		Collections.sort(candidate, new MyComparator());//节点按度数由小到大排列
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
	@SuppressWarnings("unchecked")
	public static void computeKplex(ArrayList<Integer> res,
			ArrayList<Integer> candidate,
			org.apache.hadoop.mapreduce.Reducer.Context context) {
		int rSize = res.size();
		filterCandidate(res, candidate);
		int canSizeN = candidate.size();
		int sum = rSize + canSizeN;
		if (sum >= quasiCliqueSize && sum >= kPlexSize) {// && sum >= kPlexSize
			if (judgeKplex2(res, candidate))// 是kplex
			{// && sum >= kPlexSize
				if(sum >= kPlexSize)
				{
				kPlexSize = sum;
				result.clear();
				result.addAll(res);
				result.addAll(candidate);
				try {
					context.write(new IntWritable(0), new Text(result.toString()));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
				int p=canA.indexOf(y);
				if(p!=-1)
					canA.remove(p);
				// 子图不包含y
				levelNumber++;// 避免层数过多
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
				computeKplex(resA, canA, context);
				levelNumber--;
				resA.remove(resA.size()-1);
				candidate.remove(0);
				computeKplex(resA, candidate, context);
				levelNumber--;
				if (levelNumber <= -10)
					levelNumber = 0;
			}
		}
	}

	public static class loadBalanceReducer extends
			Reducer<IntWritable, Text, IntWritable, Text> {

		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			FileReader fre = new FileReader(new File("/home/dic/kplexnew_PARAMETER.txt"));
			BufferedReader bfre = new BufferedReader(fre);
			// 提取出所有的参数
			String recorde = "";
			//pick.clear();
			while ((recorde = bfre.readLine()) != null) {
				String[] adjInfos = recorde.split(" ");
				levelExtream = Integer.valueOf(adjInfos[0]);
				quasiCliqueSize = Integer.valueOf(adjInfos[2]);		
				k_plex = Integer.valueOf(adjInfos[3]);
				//for (int i = 1; i < adjInfos.length; i++)
					//pick.add(Integer.valueOf(adjInfos[i]));
			}
			bfre.close();
		
			FileReader frp = new FileReader(new File("/home/dic/kplexnew_PATH.txt"));
			BufferedReader bfrp = new BufferedReader(frp);
			// 提取出邻接表路径
			String recordp = "";
			while ((recordp = bfrp.readLine()) != null) {
				path=recordp;
			}
			bfrp.close();
			
			FileReader fr = new FileReader(new File(path));
			BufferedReader bfr = new BufferedReader(fr);
			// 提取出所有的节点列表和节点以及邻节点的hash表
			String record = "";
			int node = -1;
			while ((record = bfr.readLine()) != null) {
				String[] adjInfos = record.split(" ");
				node = Integer.parseInt(adjInfos[0]);
				// nodeSet.add(node);
				HashSet<Integer> adj = new HashSet<Integer>(
						adjInfos.length - 1);
				for (int i = 1; i < adjInfos.length; i++)
					adj.add(Integer.valueOf(adjInfos[i]));
				oneLeap.put(node, adj);
			}
			bfr.close();
		}

		@Override
		protected void reduce(IntWritable key, Iterable<Text> value,
				Context context) throws IOException, InterruptedException {
			// oneLeap.clear();
			stack.clear();
			for (Text t : value) {
				String para = t.toString();
				String[] status = para.split("%");
				String[] search = status[0].split(",\\s");
				ArrayList<Integer> ss = new ArrayList<Integer>();
				//在这里，res[0]是current值，用hashmap存起来，判断最大值
				for (int i = 0; i < search.length; i++)
					ss.add(Integer.valueOf(search[i]));
				
				ArrayList<Integer> ss2 = new ArrayList<Integer>();
				if(status.length>1)
				{
				String[] search2 = status[1].split(",\\s");
				for (int i = 0; i < search2.length; i++) {
					try {
						ss2.add(Integer.valueOf(search2[i]));
					} catch (NumberFormatException e) {// 可能为空
						ss2.clear();
					}
				}
				}
				else
				{
					ss2.clear();
				}
				sGraph stmp = new sGraph(ss2.size());
				stmp.setRes(ss);
				stmp.setCandidate(ss2);
				stack.add(stmp);
			}
			int number=0;
			while (number < stack.size()) {
				sGraph here = stack.get(number);
				ArrayList<Integer> res = here.getRes();
				ArrayList<Integer> candidate = here.getCandidate();
				computeKplex(res, candidate, context);
				levelNumber = 0;
				number++;
			}
		}
	}
}
