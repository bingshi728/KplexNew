package kplexnew;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;

import search.SubGraph;

public class LocalModeNewWithNot {
	// reduce数目
	static int reduceNumber = 36;
	// 有意义的k-plex大小
	static int quasiCliqueSize = 5;
	// k-plex的k值大小
	static int k_plex = 2;
	public static int kPlexSize = 0;
	// 将“一跳”信息读入内存，存在HashMap中
	public static HashMap<Integer, HashSet<Integer>> oneLeap = new HashMap<Integer, HashSet<Integer>>(
			7000);
	// 数据集中的节点集合
	public static HashSet<Integer> nodeSet = new HashSet<Integer>(1000);
	// 子状态集和结果集
	public static Stack<SubGraph> stack = new Stack<SubGraph>();
	public static ArrayList<Integer> result = new ArrayList<Integer>(30);
	// loadbalance时候分散成多少份
	public static int totalPart = 36;// 分散成为多少部分
	// pick为不进行loadbalance的节点集，split为需要loadbalance的节点集
	public static ArrayList<Integer> pick = new ArrayList<Integer>();
	public static ArrayList<Integer> split = new ArrayList<Integer>();

	public static ArrayList<Integer> res = new ArrayList<Integer>();
	public static ArrayList<Integer> candidate = new ArrayList<Integer>(1000);
	public static ArrayList<Integer> not = new ArrayList<Integer>();
	public static HashMap<Integer, Integer> degree = new HashMap<Integer, Integer>(
			1000);
	//public static int number = 0;
	public static int levelNumber = 0;
	public static int levelExtream = 10;
	public static HashSet<Integer> hs = new HashSet<Integer>();
	public static HashMap<Integer, HashSet<Integer>> cacheTwoleap = new HashMap<Integer, HashSet<Integer>>();
	public static PrintStream ps=null;
	public static ArrayList<Integer> critnodes = new ArrayList<Integer>();
	public static void readInOneLeapData(String file) throws IOException{
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
	}
	public static void computeOneleapData(String file) throws IOException {
		readInOneLeapData(file);
		//Collections.sort(nodeSet);
		long t1 = System.currentTimeMillis();
		// 排序后，每个reduce只处理对应节点
		for (Integer current: nodeSet) {
			// if (pick.contains(nodeSet.get(i)))
			if (current % reduceNumber == 0) {
			//if (true) {
				kPlexSize = 0;
				// 当前所求节点
				//int current = nodeSet.get(i);
				// if (pick.contains(current)) {
				stack.clear();
				res.clear();
				candidate.clear();
				not.clear();
				// 构造起始状态，获得备选节点列表和度数列表
				// 此时res为T(+),candidate+res为Tx,candidate为T(-)
				res.add(current);
				// 初始化备选集并按度数排序
				initialSGraph(candidate, not, current);
				
				// "备选集"的概念和kplexold不同，此处备选集包含“两跳”节点，是待分解的原始图
				// 若“原始图”大小>=有意义的kplex大小时才进行计算
				// 若只计算某个节点最大的k-plex时，需再加判断条件
				while ((candidate.size() + 1) >= quasiCliqueSize)// &&
				{
					if (judgeKplex2(res, candidate,critnodes))// 是kplex
					{//如果是kplex,那么res和candidate中较大的那个包含了所有点
						//critnodes里面应该包含所有临界点
						if (duplicate(not,res, candidate,critnodes)) {
							break;
						}
						String r = (candidate.size()>res.size()?candidate:res).toString();
						System.out.println(r.toString().substring(1, r.length()-1));
						// 是clique,输出
						break;
					} else {
						// 将备选集切分成若干小的状态，切分策略，此处为选择备选集的第一个节点
						// 初始化时，备选集按度数排序
						// 1.排序之后，从后面选取几个先算，即从后面取之后存入另一个栈，把栈里的状态算完
						// 之后把剩余的状态再重新分散入栈，并将已经算出来的值一同发往其他计算节点
						// 2.将状态栈中的状态，随机选取一部分，放入另一个栈中计算
						int y = candidate.get(candidate.size() - 1);// 其他节点，存在策略
						// 子图包含y
						ArrayList<Integer> canA = new ArrayList<Integer>();
						ArrayList<Integer> notA = new ArrayList<Integer>();
						// 分割状态，包含节点y，getT2获得y的邻节点与备选集的交集
						// y的两跳数据和当前candidate的交集
						getT2(canA, notA, y, candidate, not);
						// 这段费时,已经在getT2中可以得到保证,不包含y
						// int p=canA.indexOf(y);
						// if(p!=-1)
						// canA.remove(p);
						SubGraph sA = new SubGraph(canA.size());
						sA.setCandidate(canA);
						sA.setNot(notA);
						sA.setRes(res, y);
						stack.add(sA);
						// 子图不包含y，继续分解
						candidate.remove(candidate.size() - 1);
						not.add(y);
					}
				}
				while (!stack.isEmpty()) {
					SubGraph here = stack.pop();
					ArrayList<Integer> res = here.getRes();
					ArrayList<Integer> candidate = here.getCandidate();
					ArrayList<Integer> not = here.getNot();
					// 分解之后的状态依次弹栈并计算
					computeKplex(res, candidate, not);
					levelNumber = 0;
				}

				// }
				// 需要进行loadbalance的节点，将状态分散

			}
		}
		long t2 = System.currentTimeMillis();
		System.out.println("========" + (t2 - t1) / 1000);
	}

	private static boolean duplicate(ArrayList<Integer> not2,ArrayList<Integer>res,
			ArrayList<Integer> candidate2,ArrayList<Integer>critnodes) {
//		for (Integer n : not2) {
//			HashSet<Integer> twoleap = cacheTwoleap.get(n);
//			if (twoleap == null) {// 生成两跳集
//				twoleap = new HashSet<Integer>();
//				HashSet<Integer> one = oneLeap.get(n);
//				twoleap.addAll(one);
//				for (Integer o : one) {
//					twoleap.addAll(oneLeap.get(o));
//				}
//				cacheTwoleap.put(n, twoleap);
//			}
//			if (twoleap.containsAll(candidate2))
//				return true;
//		}
//		return false;
		ArrayList<Integer> results = res.size()>candidate2.size()?res:candidate2;
		
		for(Integer n:not2)
		{
			HashSet<Integer> adj = oneLeap.get(n);
			if(!adj.containsAll(critnodes))
				continue;
			int num = 0;
			for(Integer r:results){
				if(!adj.contains(r))
					num++;
			}
			if(num>k_plex-1)
				continue;
			else
				return true;
		}
		return false;
	}
	public static void init(String[]args) throws NumberFormatException, IOException {
		FileReader fr = new FileReader(new File(
				"/home/youli/CliqueHadoop/kplexnew_COMMON.txt"));
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
		ps=new PrintStream(new File(args[1]));
		System.setOut(ps);
		levelExtream = 50;
		reduceNumber = 332;
		quasiCliqueSize = 5;
		k_plex = 2;
		totalPart = 32;
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

	public static void getCandidate2(ArrayList<Integer> candidate,
			ArrayList<Integer> not, int current) {
		int curdeg = oneLeap.get(current).size();
		HashSet<Integer> ca = new HashSet<Integer>();
		hs.clear();
		int tmpdeg = 0;
		HashSet<Integer> oneadj;
		if((oneadj=cacheTwoleap.get(current))!=null){
			hs = oneadj;
		}else{
			oneadj = oneLeap.get(current);
			hs.addAll(oneadj);
			for (Integer i : oneadj) {
				hs.addAll(oneLeap.get(i));
			}
		}
		
		for (Integer i : hs) {
			tmpdeg = oneLeap.get(i).size();
			if (tmpdeg > curdeg) {
				not.add(i);
			} else if (tmpdeg < curdeg) {
				ca.add(i);
			} else {
				if (i > current) {
					ca.add(i);
				} else if (i < current) {
					not.add(i);
				}// 相等的话即current本身,不需要加入任何集合,不处理
			}
		}
		// }
		candidate.addAll(ca);
		
		//过滤缓存,,防止两跳缓存过大
		Iterator<Map.Entry<Integer, HashSet<Integer> >>en=cacheTwoleap.entrySet().iterator();
		while(en.hasNext()){
			if(!hs.contains(en.next().getKey())){
				en.remove();
			}
		}
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
		
		for (Integer in : candidate)// oneLeap中节点和candidate中节点的交集数
		{int number = 0;
			//number = getIntersectionNumber(oneLeap.get(in));
			HashSet<Integer> adj = oneLeap.get(in);
			for(Integer out:candidate){
				if(adj.contains(out))
					number++;
			}
			degree.put(in, number);
		}
	}
	public static void initNotDegree(ArrayList<Integer>not){
		for (Integer in : not)// oneLeap中节点和candidate中节点的交集数
		{int number = 0;
			HashSet<Integer> adj = oneLeap.get(in);
			for(Integer out:candidate){
				if(adj.contains(out))
					number++;
			}
			degree.put(in, number);
		}
	}
	// 初始化备选节点和其度数，并将备选节点按度数由小到大排序
	public static void initialSGraph(ArrayList<Integer> candidate,
			ArrayList<Integer> not, int current) {
		// 获得current的“两跳”信息作为备选集
		getCandidate2(candidate, not, current);
		// 初始化节点度数
		initDegree(candidate);
		// 将节点按度数排序
		// Collections.sort(candidate, new MyComparator());//节点按度数由小到大排列
		Collections.sort(candidate, new MyComparator2());// 节点按度数由大到小排列
		initNotDegree(not);
		Collections.sort(not,new MyComparator2());
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
		 * hs.clear(); hs.addAll(candidate); hs.removeAll(remove);
		 * candidate.clear(); candidate.addAll(hs);
		 */
	}

	public static void filterCandidate(ArrayList<Integer> res,
			ArrayList<Integer> candidate,ArrayList<Integer>not) {// ,HashMap<Integer,Integer>
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
				not.retainAll(intersection);
			}
		}
		filterAgain(candidate, res);
		filterAgain(not,res);
	}

	public static boolean judgeKplex2(ArrayList<Integer> res,
			ArrayList<Integer> candidate,ArrayList<Integer> critnodes) {// ,ArrayList<Integer> splitter
		int number = 0;
		int sr = res.size();
		int cr = candidate.size();
		boolean flag = false;
		critnodes.clear();
		if (sr < cr)// candidate的量大
		{
			candidate.addAll(res);
			for (Integer out : candidate) {
				number = 0;
				for (Integer in : candidate) {
					if (out != in && disconnect(out, in)) {
						number++;
						if (number > k_plex - 1)
							break;
					}
				}
				if (number > k_plex - 1)// 不是kplx
				{// 新点不能是已有的
					// splitter.add(out);
					flag = true;
					break;
				}else if(number == k_plex -1){
					critnodes.add(out);
				}
			}
			
			if (flag)// 是跳出来的
			{	
			int i = 0;
			while (i < sr) {
				candidate.remove(candidate.size() - 1);
				i++;
			}
			return false;
			}
		} else {
			res.addAll(candidate);
			for (Integer out : res) {
				number = 0;
				for (Integer in : res) {
					if (out != in && disconnect(out, in)) {
						number++;
						if (number > k_plex - 1)
							break;
					}
				}
				if (number > k_plex - 1)// 不是kplx
				{
					// splitter.add(out);
					flag = true;
					break;
				}else if(number == k_plex -1){
					critnodes.add(out);
				}
			}
			
			if (flag){
				int i = 0;
				while (i < cr) {
					res.remove(res.size() - 1);
					i++;
				}
				return false;
			}
		}
		return true;// 没有splitter
	}

	public static void getT2(ArrayList<Integer> canA, ArrayList<Integer> notA,
			int y, ArrayList<Integer> candidate, ArrayList<Integer> not) {
		hs.clear();
		HashSet<Integer> yAdj = oneLeap.get(y);
		hs.addAll(yAdj);
		for (Integer i : yAdj) {
			hs.addAll(oneLeap.get(i));
		}
		hs.remove(y);// 确保结果集中不会有y分裂点
		for (Integer c : candidate) {
			if (hs.contains(c))// 有冲突,hs为canA,和candidate的交集，就是有冲突的点
				canA.add(c);
		}
		for (Integer n : not) {
			if (hs.contains(n))
				notA.add(n);
		}
		// canA.addAll(hs);
	}

	public static void getT3(ArrayList<Integer> canA, int y,
			ArrayList<Integer> candidate) {
		HashSet<Integer> yAdj = oneLeap.get(y);
		canA.addAll(yAdj);
		for (Integer i : yAdj)
			for (Integer j : oneLeap.get(i)) {
				if (!canA.contains(j))
					canA.add(j);
			}
		canA.retainAll(candidate);
	}
static int cliquenum = 0;
static int dupnum = 0;
	@SuppressWarnings("unchecked")
	public static void computeKplex(ArrayList<Integer> res,
			ArrayList<Integer> candidate, ArrayList<Integer> not) {
		int rSize = res.size();
		// 根据k-plex定义过滤备选集(最多不与k-1个节点相连)
		filterCandidate(res, candidate,not);
		
		int canSizeN = candidate.size();
		int sum = rSize + canSizeN;
		if (sum >= quasiCliqueSize) {// && sum >= kPlexSize
			if(candidate.isEmpty()){
				if(not.isEmpty()){
					String r = res.toString();
					System.out.println(r.substring(1, r.length()-1));
					cliquenum++;
				}else{
					dupnum++;
				}
				return;
			}
			if (judgeKplex2(res, candidate,critnodes))// 是kplex
			{
				if (duplicate(not,res, candidate,critnodes)) {
					dupnum++;
					return;
				}
				// 是clique输出
				String r = (candidate.size()>res.size()?candidate:res).toString();
				System.out.println(r.toString().substring(1, r.length()-1));
				return;
			} else {
				int y = candidate.get(candidate.size() - 1);// 其他节点，存在策略
				// 子图包含y
				// res中多了y
				ArrayList<Integer> resA = new ArrayList<Integer>();
				resA.addAll(res);
				resA.add(y);
				ArrayList<Integer> canA = new ArrayList<Integer>();
				ArrayList<Integer> notA = new ArrayList<Integer>();
				// canA.clear();
				getT2(canA, notA, y, candidate, not);
				// getT3(canA, y,candidate);
				// int p=canA.indexOf(y);
				// if(p!=-1)
				// canA.remove(p);
				levelNumber++;
				// 避免层数过多使得系统栈溢出，当levelNumber过大时(大于levelExtream)，将状态存栈
				if (levelNumber >= levelExtream) {
					SubGraph sA = new SubGraph(canA.size());
					sA.setCandidate(canA);
					sA.setRes(resA);
					sA.setNot(notA);
					stack.add(sA);

					ArrayList<Integer> resB = new ArrayList<Integer>(res.size());
					resB.addAll(res);
					candidate.remove(candidate.size() - 1);

					SubGraph sB = new SubGraph(candidate.size());
					sB.setCandidate(candidate);
					not.add(y);
					sB.setNot(not);
					sB.setRes(resB);
					stack.add(sB);
					return;
				}
				// 子图包含y
				computeKplex(resA, canA, notA);
				levelNumber--;
				// resA.remove(resA.size()-1);
				candidate.remove(candidate.size() - 1);
				not.add(y);
				// 子图不包含y
				computeKplex(res, candidate, not);
				levelNumber--;
				// 维护状态栈层数在合理的大小
				if (levelNumber <= -10)
					levelNumber = 0;
			}
		}
	}
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		///init(args);
		///computeOneleapData(args[0]);
		testKplex(args);
	}

	private static void testKplex(String[] args) throws IOException {
		k_plex = 2;
		readInOneLeapData(args[0]);
		BufferedReader reader = new BufferedReader(new FileReader(args[1]));
		//Compare(args[1]);
		//Compare(args[2]);
//		test(args[1],args[2]);
//		test(args[2],args[1]);
		print("serialold",args[2]);
		print("serialnew",args[1]);
	}
	public static void Compare(String str1) throws NumberFormatException, IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(str1));
		String line;
		HashMap<Integer,List<List<Integer>>> id2kplex = new HashMap<Integer,List<List<Integer>>>();
		int num = 0;
		while((line=reader.readLine())!=null){
			String[] splits = line.split(",");
			ArrayList<Integer> kplex = new ArrayList<Integer>();
			for(String split:splits)
				kplex.add(Integer.parseInt(split.trim()));
			Collections.sort(kplex);
			List<List<Integer>> listkplex = id2kplex.get(kplex.get(0));
			if(listkplex==null){
				listkplex = new ArrayList<List<Integer>>();
				id2kplex.put(kplex.get(0), listkplex);
			}
			listkplex.add( kplex);
		}int not = 0;
		for (List<List<Integer>> kplexes : id2kplex.values()) {
		
			for (int i = 0; i < kplexes.size(); i++) {
				if(!isKplex(kplexes.get(i))){
					not++;
					for(int out:kplexes.get(i)){
						for(int in:kplexes.get(i)){
							if(!(disconnect(out,in)))
								System.out.print(out+">>" +in+" ");
						}
						System.out.print("\n");
					}
					System.out.println(kplexes.get(i)+" is not kplex");
					//break;
					
				}
				for (int j = 0; j < kplexes.size(); j++) {
					if (i!=j&&kplexes.get(j).containsAll(kplexes.get(i))){
						System.out.println(kplexes.get(j)+"r1,dup"+kplexes.get(i));
						num++;
					}
				}
			}
		}
		System.out.println("dup num"+num +" not num"+not);
	}
		

	private static boolean isKplex(List<Integer> list) {
		for(Integer n:list){
			int num = 0;
			for(Integer i:list){
				if(n!=i&&disconnect(n,i))
					num++;
			}
			if(num>k_plex-1)
				return false;
		}
		return true;
	}
	private static void print(String file,String inputfile){
		FileWriter fw;
		try {
			fw = new FileWriter(file);
			ReadFile fi = new ReadFile();
			fi.readFileByLines(inputfile);
			Map<Integer, List<String>> k2c = fi.resultfile;
			for(List<String> li:k2c.values()){
				Collections.sort(li);
				for(String ss:li)
						fw.write(ss+"\n");
			}
			fw.flush();
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	public static void test(String file1, String file2) {
		ReadFile fi = new ReadFile();
		fi.readFileByLines(file1);
		Map<Integer, List<String>> k2c = fi.resultfile;
		
		ReadFile fj = new ReadFile();
		fj.readFileByLines(file2);
		Map<Integer, List<String>> k2c2 = fi.resultfile;
		
		if (k2c.size() != k2c2.size()) {
			System.out.println("size not equal");
			// return;
		}
		for(int key : k2c.keySet()){
			List<String> l1 = k2c.get(key);
			List<String> l2 = k2c2.get(key);
			for(int i = 0; i < l1.size();i++){
				if(!l1.get(i).equals(l2.get(i))){
					System.out.println(l1.get(i)+" not equals "+l2.get(i));
				}
			}
		}
				
		System.out.println("compare ok");
	}


static	class ReadFile {
		public TreeMap<Integer, List<String>> resultfile = new TreeMap<Integer, List<String>>();
		static String split = ",";
		public void readFileByLines(String fileName) {
			File file = new File(fileName);
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				int i = 0;
				String tempString = "";
				StringBuilder resultStr = new StringBuilder();
				while ((tempString = reader.readLine()) != null) {
					// resultStr = "";
					String[] strs = tempString.trim().split(split);
					
					ArrayList<Integer> tempdata = new ArrayList<Integer>();
//					tempdata.add(Integer.parseInt(strs[0].trim()));
//					String[] str = strs[1].split(split);
					for (i = 0; i < strs.length; i++) {
						if (strs[i].equals(""))
							continue;
						tempdata.add(Integer.parseInt(strs[i].trim()));
					}
					Collections.sort(tempdata);
					for (i = 0; i < tempdata.size(); i++) {
						resultStr.append(tempdata.get(i).toString() + " ");
					}
					List<String> cliques = resultfile.get(tempdata.get(0));
					if (cliques == null) {
						cliques = new ArrayList<String>();
						resultfile.put(tempdata.get(0), cliques);
					}
					cliques.add(resultStr.toString());
					resultStr.delete(0, resultStr.length());
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
		}
	}

}
