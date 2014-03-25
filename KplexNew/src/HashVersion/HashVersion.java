package HashVersion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.StringTokenizer;

public class HashVersion {

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
	//
	// public static HashMap<Integer,Integer> res = new
	// HashMap<Integer,Integer>();
	// public static ArrayList<Pair> candidate = new ArrayList<Pair>();
	// public static HashMap<Integer,Integer> not = new
	// HashMap<Integer,Integer>();
	public static HashMap<Integer, Integer> degree = new HashMap<Integer, Integer>(
			1000);
	// public static int number = 0;
	public static int levelNumber = 0;
	public static int levelExtream = 10;
	//public static HashSet<Integer> hs = new HashSet<Integer>();
	/**
	 * 缓存一个点的第二跳数据,仅仅是第二跳的不算第一跳数据
	 */
	public static HashMap<Integer, HashSet<Integer>> cacheTwoleap = new HashMap<Integer, HashSet<Integer>>();
	public static PrintStream ps = null;
	public static ArrayList<Integer> critnodes = new ArrayList<Integer>();

	public static void readInOneLeapData(String file) throws IOException {
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
		long t1 = System.currentTimeMillis();
		// 排序后，每个reduce只处理对应节点
		for (Integer current : nodeSet) {
			// if (pick.contains(nodeSet.get(i)))
			if (current % reduceNumber == 0) {
			//if (true) {
				stack.clear();
				// 构造起始状态，获得备选节点列表和度数列表
				// 此时res为T(+),candidate+res为Tx,candidate为T(-)
				HashMap<Integer, Integer> tmpres = new HashMap<Integer, Integer>();
				tmpres.put(current, 0);
				ArrayList<Pair> tmpcand = new ArrayList<Pair>();
				HashMap<Integer, Integer> tmpnot = new HashMap<Integer, Integer>();
				// 初始化备选集并按度数排序
				//initialSGraph(tmpcand, tmpnot, current);
				getCandidate2(tmpcand,tmpnot,current);
				SubGraph initsub = new SubGraph();
				initsub.setCandidate(tmpcand);
				initsub.setNot(tmpnot);
				initsub.setResult(tmpres);
				stack.add(initsub);
				// "备选集"的概念和kplexold不同，此处备选集包含“两跳”节点，是待分解的原始图
				// 若“原始图”大小>=有意义的kplex大小时才进行计算
				// 若只计算某个节点最大的k-plex时，需再加判断条件
				while (!stack.isEmpty()) {
					SubGraph top = stack.pop();
					HashMap<Integer, Integer> res = top.getResult();
					ArrayList<Pair> candidate = top.getCandidate();
					HashMap<Integer, Integer> not = top.getNot();
					//这里保证了candidate中的所有点都满足条件2:在临界点邻接表内
					candidate = filterCandidate(res, candidate, not);
					Collections.sort(candidate);
					while (res.size() + candidate.size() >= quasiCliqueSize) {
						if (candidate.isEmpty()) {
							if (not.isEmpty()) {
								String r = res.keySet().toString();
								System.out.println(r.substring(1,
										r.length() - 1));
								cliquenum++;
							} else {
								dupnum++;
							}
							break;
						}
//						if (judgeKplex2(res, candidate, critnodes))// 是kplex
//						{
//							if (duplicate(not, res, candidate, critnodes)) {
//								dupnum++;
//								break;
//							}
//							// 是clique输出
//							String r = res.keySet().toString();
//							String c = candidate.toString();
//							System.out.println(r.toString().substring(1,
//									r.length() - 1)
//									+ ", " + c.substring(1, c.length() - 1));
//							break;
//						} else {
							Pair y = candidate.get(candidate.size() - 1);// 其他节点，存在策略
							candidate.remove(candidate.size() - 1);
							// 子图包含y
							// res中多了y
							HashMap<Integer, Integer> resA = (HashMap<Integer, Integer>) res
									.clone();
							HashSet<Integer> adj = oneLeap.get(y.point);
							for (Entry<Integer, Integer> en : resA.entrySet()) {
								if (!adj.contains(en.getKey())) {
									en.setValue(en.getValue() + 1);
								}
							}
							resA.put(y.point, y.discon);
//							// 分裂点y从候选点中删除后,需要更新候选集中点的度数值
//							// 这一段正好可以找出度数最小的点
//							for (Pair p : candidate) {
//								if (!adj.contains(p.point)) {
//									p.deg--;// y加到结果集后,这个点和当前candidate的不相邻点应减1
//								}
//							}
							ArrayList<Pair> canA = new ArrayList<Pair>();
							HashMap<Integer, Integer> notA = new HashMap<Integer, Integer>();
							getT2(canA, notA, y.point, candidate, not);
							SubGraph sA = new SubGraph();
							sA.setCandidate(canA);
							sA.setResult(resA);
							sA.setNot(notA);
							stack.add(sA);

							
							not.put(y.point, y.discon);
//						}
					}
				}
			}
		}
		long t2 = System.currentTimeMillis();
		System.out.println("========" + (t2 - t1) / 1000+ " hit:"+hit);
	}

	private static boolean duplicate(HashMap<Integer, Integer> not2,
			HashMap<Integer, Integer> res, ArrayList<Pair> candidate2,
			ArrayList<Integer> critnodes) {
		for (Entry<Integer, Integer> en : res.entrySet()) {
			if (en.getValue() == k_plex - 1) {
				critnodes.add(en.getKey());
			}
		}
		for (Entry<Integer, Integer> en : not2.entrySet()) {
			HashSet<Integer> adj = oneLeap.get(en.getKey());
			if (!adj.containsAll(critnodes))
				continue;
			int num = en.getValue();
			for (Pair r : candidate2) {
				if (!adj.contains(r.point))
					num++;
			}
			if (num > k_plex - 1)
				continue;
			else
				return true;
		}
		return false;
	}

	public static void init(String[] args) throws NumberFormatException,
			IOException {
//		FileReader fr = new FileReader(new File(
//				"/home/youli/CliqueHadoop/kplexnew_COMMON.txt"));
//		BufferedReader bfr = new BufferedReader(fr);
//		// 提取出所有的pick节点
//		String record = "";
//		pick.clear();
//		while ((record = bfr.readLine()) != null) {
//			String[] adjInfos = record.split(" ");
//			for (int i = 1; i < adjInfos.length; i++)
//				pick.add(Integer.valueOf(adjInfos[i]));
//		}
//		bfr.close();
		 ps=new PrintStream(new File(args[1]));
		 System.setOut(ps);
		levelExtream = 50;
		reduceNumber = 132;
		quasiCliqueSize = 5;
		k_plex = 2;
		totalPart = 32;
	}

static	int hit = 0;
	/**
	 * 生成初始的size-1子图,子图中点的disconn度数以通过是一跳数据还是两跳数据记录为0和1
	 * 子图中点的degree由于未计算都设置为默认值-1
	 * 
	 * @param candidate
	 * @param not
	 * @param current
	 */
	public static void getCandidate2(ArrayList<Pair> candidate,
			HashMap<Integer, Integer> not, int current) {
		
		int tmpdeg = 0;
		final HashSet<Integer> oneadj;
		oneadj = oneLeap.get(current);
		int curdeg = oneadj.size();
		HashSet<Integer> twoadj;
		
		//缓存命中
		if((twoadj=cacheTwoleap.get(current))!=null){
			hit++;
			for (Integer i : oneadj) {// 一跳集
				tmpdeg = oneLeap.get(i).size();
				if (tmpdeg > curdeg) {
					not.put(i, 0);
				} else if (tmpdeg < curdeg) {
					candidate.add(new Pair(i, 0, -1));
				} else {
					if (i > current) {
						candidate.add(new Pair(i, 0, -1));
					} else
						not.put(i, 0);
				}
			}
			for (Integer i : twoadj) {//二跳集
				tmpdeg = oneLeap.get(i).size();
				if (tmpdeg > curdeg) {
					not.put(i, 1);
				} else if (tmpdeg < curdeg) {
					candidate.add(new Pair(i, 1, -1));
				} else {
					if (i > current) {
						candidate.add(new Pair(i, 1, -1));
					} else
						not.put(i, 1);
				}
			}
			return;
		}
		
		//缓存未命中
		twoadj = new HashSet<Integer>();
		for (Integer i : oneadj) {// 一跳集
			twoadj.addAll(oneLeap.get(i));// 生成二跳集
			tmpdeg = oneLeap.get(i).size();
			if (tmpdeg > curdeg) {
				not.put(i, 0);
			} else if (tmpdeg < curdeg) {
				candidate.add(new Pair(i, 0, -1));
			} else {
				if (i > current) {
					candidate.add(new Pair(i, 0, -1));
				} else //if (i < current) {//一跳集中不会有current本身
					not.put(i, 0);
				//}// 相等的话即current本身,不需要加入任何集合,不处理
			}
		}
		twoadj.removeAll(oneadj);// 仅包含第二跳集
		twoadj.remove(current);
		for (Integer i : twoadj) {
			tmpdeg = oneLeap.get(i).size();
			if (tmpdeg > curdeg) {
				not.put(i, 1);
			} else if (tmpdeg < curdeg) {
				candidate.add(new Pair(i, 1, -1));
			} else {
				if (i > current) {
					candidate.add(new Pair(i, 1, -1));
				} else //if (i < current) {//上面保证了二跳集中不会有current
					not.put(i, 1);
				//}// 相等的话即current本身,不需要加入任何集合,不处理
			}
		}
		//加入到缓存
		cacheTwoleap.put(current, twoadj);
		// 过滤缓存,,防止两跳缓存过大
		// Iterator<Map.Entry<Integer, HashSet<Integer>
		// >>en=cacheTwoleap.entrySet().iterator();
		// while(en.hasNext()){
		// if(!hs.contains(en.next().getKey())){
		// en.remove();
		// }
		// }
	}

	/**
	 * candidate的度数记录某个点和candidate中其他点不相邻的个数
	 * 
	 * @param candidate
	 */
	public static void initDegree(ArrayList<Pair> candidate) {

		for (Pair in : candidate)// oneLeap中节点和candidate中节点的交集数
		{
			int number = 0;
			HashSet<Integer> adj = oneLeap.get(in.point);
			for (Pair out : candidate) {
				if (!adj.contains(out.point) && in.point != out.point)
					number++;
			}
			in.deg = number;
		}
	}

	// public static void initNotDegree(ArrayList<Integer>not){
	// for (Integer in : not)// oneLeap中节点和candidate中节点的交集数
	// {int number = 0;
	// HashSet<Integer> adj = oneLeap.get(in);
	// for(Integer out:candidate){
	// if(adj.contains(out))
	// number++;
	// }
	// degree.put(in, number);
	// }
	// }
	// 初始化备选节点和其度数，并将备选节点按度数由小到大排序
	public static void initialSGraph(ArrayList<Pair> candidate,
			HashMap<Integer, Integer> not, int current) {
		// 获得current的“两跳”信息作为备选集
		getCandidate2(candidate, not, current);
		// 初始化节点度数
		// initDegree(candidate);
		// 将节点按度数排序
		// Collections.sort(candidate, new MyComparator());//节点按度数由小到大排列
		// Collections.sort(candidate);// 节点按度数由大到小排列
		// initNotDegree(not);
		// Collections.sort(not,new MyComparator2());
	}

	public static void getCriticalSet(HashMap<Integer, Integer> res,
			ArrayList<Integer> critSet) {
		for (Entry<Integer, Integer> vo : res.entrySet()) {
			if (vo.getValue() == k_plex - 1)
				critSet.add(vo.getKey());
		}
	}

	public static boolean disconnect(int a, int b) {
		if (!(oneLeap.get(a)).contains(b))
			return true;
		else
			return false;
	}

	/**
	 * 用临界点的一跳集交集过滤候选集和not集, 保证candidate中的点满足条件2,条件1在生成时已保证 返回的结果中,对应的度数已更新
	 * 
	 * @param res
	 * @param candidate
	 * @param not
	 * @return 要保留的candidate,原有的candidate已清空
	 */
	public static ArrayList<Pair> filterCandidate(
			HashMap<Integer, Integer> res, ArrayList<Pair> candidate,
			HashMap<Integer, Integer> not) {// ,HashMap<Integer,Integer>
		ArrayList<Pair> keeped = new ArrayList<Pair>(candidate.size()); // degree
		if (res.size() >= k_plex)// 否则即便是全部不连接也没关系，起不到过滤效果
		{
			ArrayList<Integer> critSet = new ArrayList<Integer>();
			getCriticalSet(res, critSet);
			HashSet<Integer> intersection = new HashSet<Integer>();
			if (critSet.size() > 0)// 节点临界，用于过滤
			{
				intersection.addAll(oneLeap.get(critSet.get(0)));// 先加入第一个元素
				for (int i = 1; i < critSet.size(); i++) {
					HashSet<Integer> adj = oneLeap.get(critSet.get(i));
					intersection.retainAll(adj);// 不断取交集
				}
				// 取intersection和candidate的交集，剔除两跳外数据，自然也将res中数据剔除了
				// res中数据不在candidate中
				for (Pair p : candidate) {
					//if (intersection.contains(p.point) && p.discon < k_plex)//已经被保证了
					if (intersection.contains(p.point))
						keeped.add(p);
				}
				initDegree(keeped);
				candidate = null;
				Iterator<Entry<Integer, Integer>> iter = not.entrySet()
						.iterator();
				while (iter.hasNext()) {
					Entry<Integer, Integer> en = iter.next();
//					if (!intersection.contains(en.getKey())
//							|| en.getValue() > k_plex - 1) {//已经被保证了
					if (!intersection.contains(en.getKey()))
						iter.remove();
				}
			} else {
				initDegree(candidate);
				return candidate;
			}
		} else {
			initDegree(candidate);
			return candidate;
		}
		// filterAgain(keeped, res);
		// filterAgain(not,res);
		return keeped;
	}

	/**
	 * 遍历candidate中所有点,将candidate中的临界点加入到critnodes中
	 * 
	 * @param res
	 * @param candidate
	 * @param critnodes
	 * @return
	 */
	public static boolean judgeKplex2(HashMap<Integer, Integer> res,
			ArrayList<Pair> candidate, ArrayList<Integer> critnodes) {// ,ArrayList<Integer>
																		// splitter
		critnodes.clear();
		boolean r = true;
		for (Pair pair : candidate) {
			if (pair.discon + pair.deg > k_plex - 1)
				return false;
			else if (pair.discon + pair.deg == k_plex - 1) {
				critnodes.add(pair.point);
			}
		}
		return r;
	}

	/**
	 * 用y点的两跳数据分别和candidat及not做交集,同时更新disconnect degree
	 * y一跳集中的点disconnect值不变,二跳集中的点disconnect值加一
	 * 最后结果中的canA和notA一直满足条件1,candA和notA中的点disconnect都小于k_plex-1
	 * 
	 * @param canA
	 * @param notA
	 * @param y
	 * @param candidate
	 * @param not
	 */
	public static void getT2(ArrayList<Pair> canA,
			HashMap<Integer, Integer> notA, int y, ArrayList<Pair> candidate,
			HashMap<Integer, Integer> not) {
		HashSet<Integer> yAdj = oneLeap.get(y);
		for (Pair c : candidate) {
			if (yAdj.contains(c.point))// 有冲突,hs为canA,和candidate的交集，就是有冲突的点
				canA.add(new Pair(c));
		}
		for (Entry<Integer, Integer> en : not.entrySet()) {
			if (yAdj.contains(en.getKey()))
				notA.put(en.getKey(), en.getValue());
		}
		HashSet<Integer> twoadj = cacheTwoleap.get(y);
		if(twoadj==null){
			twoadj = new HashSet<Integer>();
			cacheTwoleap.put(y, twoadj);
			
			for(Integer i: yAdj)
				twoadj.addAll(oneLeap.get(i));
			twoadj.remove(y);
			twoadj.removeAll(yAdj);
		}else
			hit++;
		
		Iterator<Pair> iter = candidate.iterator();
		while (iter.hasNext()) {
			Pair c = iter.next();
			if (twoadj.contains(c.point) && c.discon < k_plex - 1)// 有冲突,hs为canA,和candidate的交集，就是有冲突的点
				canA.add(new Pair(c.point, c.discon + 1, c.deg));
		}
		for (Entry<Integer, Integer> en : not.entrySet()) {
			if (twoadj.contains(en.getKey()) && en.getValue() < k_plex - 1)
				notA.put(en.getKey(), en.getValue() + 1);
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
	public static void computeKplex(HashMap<Integer, Integer> res,
			ArrayList<Pair> candidate, HashMap<Integer, Integer> not) {
		// 根据k-plex定义过滤备选集(最多不与k-1个节点相连)
		candidate = filterCandidate(res, candidate, not);
		// int rSize = res.size();
		// int canSizeN = candidate.size();
		// int sum = rSize + canSizeN;
		while (res.size() + candidate.size() >= quasiCliqueSize) {// && sum >=
																	// kPlexSize
			if (candidate.isEmpty()) {
				if (not.isEmpty()) {
					String r = res.keySet().toString();
					System.out.println(r.substring(1, r.length() - 1));
					cliquenum++;
				} else {
					dupnum++;
				}
				return;
			}
			if (judgeKplex2(res, candidate, critnodes))// 是kplex
			{
				if (duplicate(not, res, candidate, critnodes)) {
					dupnum++;
					return;
				}
				// 是clique输出
				String r = res.keySet().toString();
				String c = candidate.toString();
				System.out.println(r.toString().substring(1, r.length() - 1)
						+ ", " + c.substring(1, c.length() - 1));
				return;
			} else {
				Pair y = candidate.get(candidate.size() - 1);// 其他节点，存在策略
				// 子图包含y
				// res中多了y
				HashMap<Integer, Integer> resA = (HashMap<Integer, Integer>) res
						.clone();
				HashSet<Integer> adj = oneLeap.get(y.point);
				for (Entry<Integer, Integer> en : resA.entrySet()) {
					if (!adj.contains(en.getKey())) {
						en.setValue(en.getValue() + 1);
					}
				}
				resA.put(y.point, y.discon);
				// 分裂点y从候选点中删除后,需要更新候选集中点的度数值
				// 这一段正好可以找出度数最小的点
				// Iterator<Pair> iter = candidate.iterator();
				// while(iter.hasNext()){
				// Pair p = iter.next();
				for (Pair p : candidate) {
					if (!adj.contains(p.point)) {
						// //与y不相邻的候选点;y加到结果集中deg--,discon++
						// p.discon++;//y加到结果集后,这个点的disconn当加1
						// if(p.discon>k_plex-1)//超出这个度数不可能成为结果集中的点,直接删除
						// iter.remove();
						p.deg--;// y加到结果集后,这个点和当前candidate的不相邻点应减1
					}
				}
				ArrayList<Pair> canA = new ArrayList<Pair>();
				HashMap<Integer, Integer> notA = new HashMap<Integer, Integer>();
				getT2(canA, notA, y.point, candidate, not);
				levelNumber++;
				// 避免层数过多使得系统栈溢出，当levelNumber过大时(大于levelExtream)，将状态存栈
				// if (levelNumber >= levelExtream) {
				SubGraph sA = new SubGraph();
				sA.setCandidate(canA);
				sA.setResult(resA);
				sA.setNot(notA);
				stack.add(sA);

				candidate.remove(candidate.size() - 1);
				not.put(y.point, y.discon);
				// HashMap<Integer,Integer> resB = (HashMap<Integer, Integer>)
				// res.clone();
				// SubGraph sB = new SubGraph();
				// sB.setCandidate(candidate);

				// sB.setNot(not);
				// sB.setResult(resB);
				// stack.add(sB);
				// return;
				// }
				// // 子图包含y
				// computeKplex(resA, canA, notA);
				// levelNumber--;
				// // resA.remove(resA.size()-1);
				// candidate.remove(candidate.size() - 1);
				// not.put(y.point,y.discon);
				// // 子图不包含y
				// computeKplex(res, candidate, not);
				// levelNumber--;
				// // 维护状态栈层数在合理的大小
				// if (levelNumber <= -10)
				// levelNumber = 0;
			}
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		init(args);
		computeOneleapData(args[0]);
		// testKplex(args);
	}

	private static void testKplex(String[] args) throws IOException {
		k_plex = 2;
		readInOneLeapData(args[0]);
		BufferedReader reader = new BufferedReader(new FileReader(args[1]));
	}
}
