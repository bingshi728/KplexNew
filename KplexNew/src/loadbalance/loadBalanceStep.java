package loadbalance;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import main.RunOver;
import notwoleapversion.DegList;
import notwoleapversion.Node;
import notwoleapversion.Pair;
import notwoleapversion.SubGraph;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;


public class loadBalanceStep {
	static String rootdir = "/home/"+RunOver.usr+"/QuasicClique/";
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
	public static HashSet<Integer> pick = new HashSet<Integer>();

	public static String graphFile = "";
	public static class loadBalanceMapper extends
			Mapper<LongWritable, Text, IntWritable, Text> {
		@Override
		protected void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String str = value.toString();
			int p = Integer.parseInt(str.substring(0, str.indexOf(" ")));
			String v = str.substring(str.indexOf("%")+1,str.length());
			context.write(new IntWritable(p), new Text(v));
		}
	}

	public static class loadBalancePartitioner extends
			Partitioner<IntWritable, Text> {
		@Override
		public int getPartition(IntWritable key, Text value, int num) {
			return key.get() % num;// 平均分配到各个计算节点之上
		}
	}

	public static class loadBalanceReducer extends
			Reducer<IntWritable, Text, IntWritable, Text> {
		static long T = 0;
		static int N = 0;
		FileWriter writer = null;
		int reduceid = 0;
		static int cliquenum = 0;
		static int dupnum = 0;
		static int treesize = 0;
		static int purningsize = 0;
		static long time = 0;
		static int count = 0;
		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			FileReader fr = new FileReader(new File(rootdir+"kplexnew_COMMON.txt"));
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
			
			FileReader fr3 = new FileReader(new File(rootdir+"kplexnew_PARAMETER.txt"));
			BufferedReader bfr3 = new BufferedReader(fr3);
			// 提取出所有的参数
			String record3 = "";
			//split.clear();
			while ((record3 = bfr3.readLine()) != null) {
				String[] adjInfos = record3.split(" ");
				graphFile = adjInfos[0];
				reduceNumber = Integer.valueOf(adjInfos[1]);
				quasiCliqueSize = Integer.valueOf(adjInfos[2]);				
				k_plex = Integer.valueOf(adjInfos[3]);
				T = Integer.valueOf(adjInfos[4])*1000L;
				N = Integer.valueOf(adjInfos[5]);
			}
			bfr3.close();
			count = new Random().nextInt(reduceNumber);
			readInOneLeapData(graphFile);
		}
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
			reader.close();
		}
		@Override
		protected void reduce(IntWritable key, Iterable<Text> values,
				Context context) throws IOException, InterruptedException {
			reduceid = key.get();
			if(writer==null){
				writer = new FileWriter(RunOver.spillPath+reduceid);
			}
			for(Text t:values){
				String sStr = t.toString();
				if(time<T){
					SubGraph subGraph = new SubGraph();
					subGraph.readInString(sStr);
					stack.add(subGraph);
					treesize++;
					while(time<T&&!stack.isEmpty()){
						time+=computeOneSubGraph(stack.pop(),true,context);
					}
					while(!stack.isEmpty()){
						spillToDisk(writer,stack.pop());
					}
				}else{
					writer.write(sStr);
					writer.write("\n");
				}
			}
		}
		@Override
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			if(writer!=null)
				writer.close();
			File prevfile = new File(RunOver.spillPath+reduceid);
			if(prevfile.exists()&&prevfile.length()>0){
				if(time<T){
					File curFile = new File(RunOver.spillPath+reduceid+"#");
					BufferedReader reader = new BufferedReader(new FileReader(prevfile));
					FileWriter newWriter = new FileWriter(curFile);
					String line = "";
					stack.clear();
					while(time<T&&(line=reader.readLine())!=null){
						SubGraph graph = new SubGraph();
						graph.readInString(line.substring(line.indexOf("%")+1, line.length()));
						stack.add(graph);
						while(!stack.isEmpty() && time<T){
							time += computeOneSubGraph(stack.pop(),false,context);
							if(time>=T)
								break;
						}
					}
					while(!stack.isEmpty()){
						spillToDisk(newWriter,stack.pop());
					}
					while((line=reader.readLine())!=null){
						newWriter.write(line+"\n");
					}
					newWriter.close();
					if(curFile.exists()&& curFile.length()==0){
						curFile.delete();
					}
					reader.close();
					prevfile.delete();
				}
			}else if(prevfile.exists()){
				prevfile.delete();
			}
			System.out.println("kplex num="+cliquenum+"========" + time / 1000+" s, treesize="+treesize+
					"/purningsize:"+purningsize);
			super.cleanup(context);
		}
		
		private long computeOneSubGraph(SubGraph top, boolean spillBig, org.apache.hadoop.mapreduce.Reducer.Context context) throws IOException, InterruptedException {
			long t1 = System.currentTimeMillis();
			ArrayList<Pair> res = top.getResult();
			ArrayList<Pair> prunablenot = new ArrayList<Pair>();
			HashMap<Integer, Pair> candidate = top.getCandidate();
			HashMap<Integer, Pair> not = top.getNot();
			// 这里保证了candidate中的所有点都满足条件2:在临界点邻接表内
			// 这里输入的candidate点都应该满足两个条件,需要在生成时就保证
			// candidate = filterCandidate(res, candidate, not);
			ArrayList<Integer> critnodes = new ArrayList<Integer>();
			getCriticalSet(res, critnodes);// 母图的临界点集合 :母图的临界点必然都是子图的临界点
			DegList deglist = new DegList();
			/**
			 * 给出当前候选点,建立我设计的最小度数数据结构
			 */
			updateDeg(deglist, candidate);
			// 计算res和not的cdeg
			computeDeg(res, candidate);// 这里只是为了一致,size-1图的res的cdeg是已经有了的
			computeDeg(prunablenot,not, candidate,res.size());
			while (res.size() + candidate.size() >= quasiCliqueSize) {
				if (candidate.isEmpty()) {
					if (not.isEmpty()) {
						if(RunOver.spillRes){
							String r = res.toString();
							context.write(new Text(r.substring(1,
									r.length() - 1)), NullWritable.get());
						}
						cliquenum++;
					} else {
						dupnum++;
					}
					break;
				}
				// 判断not集中是否有点与res和candidate中的点都相邻,以提前剪枝
				if (duplicate(prunablenot, candidate.size())) {
					dupnum++;
					purningsize++;
					break;
				}
				if (judgeKplex2(res, candidate))// 是kplex
				{
					// 判断not集中是否有点可以和res+candidate构成kplex
					if (duplicate(not, res, candidate))
						break;
					// 是clique输出
					if (RunOver.spillRes) {
						String r = res.toString();
						String c = candidate.keySet().toString();
						context.write(
								new Text(r.toString().substring(1,
										r.length() - 1)
										+ ", " + c.substring(1, c.length() - 1)),
								NullWritable.get());
					}

					cliquenum++;
					break;
				} else {
					// 找到度数最小的点
					Integer yint = deglist.getHead().points.iterator()
							.next();
					Pair y = candidate.get(yint);

					// 子图包含y
					// res中多了y
					ArrayList<Pair> resA = new ArrayList<Pair>(
							res.size());
					for (Pair tp : res)
						resA.add(tp.clone());

					int nodesize = resA.size() + 1;// 结果集中应该还加上当前分裂点(在后面加上了)
					ArrayList<Integer> tmpcrit = (ArrayList<Integer>) critnodes.clone();
					if (nodesize - y.rdeg == k_plex)
						tmpcrit.add(yint);
					HashSet<Integer> adj = oneLeap.get(yint);
					for (Pair p : resA) {
						if (adj.contains(p.point)) {
							// 与分裂点相邻的点度数加一,这些点要么已经在critnodes中要么不会成为critnode
							p.rdeg++;
						} else if (nodesize - p.rdeg == k_plex) {
							// 与分裂点不相邻的点有可能因为分裂点的加入成为critnode
							tmpcrit.add(p.point);
						}
					}
					resA.add((Pair) y.clone());
					/**
					 * 更新从candidate候选点集合中删除掉当前分裂点y后candidate及res中点的cdeg
					 */
					// 分裂点y从候选点中删除后,需要更新候选集中点的度数值
					// 更新res的cdeg,.更新not的cdeg,.更新candidate的cdeg
					updateMarkDeg(res, not, candidate, deglist, y);
					HashMap<Integer, Pair> canA = new HashMap<Integer, Pair>();
					HashMap<Integer, Pair> notA = new HashMap<Integer, Pair>();
					// 用临界点条件2和条件1从候选点集合中选出满足条件的点作为切出来的子图的候选点
					// 这里要注意维护各个集合点的rdeg
					filterCandidate(canA, notA, tmpcrit, candidate,
							not, adj, nodesize);
					if (canA.size() + resA.size() >= quasiCliqueSize) {
						SubGraph sA = new SubGraph();
						sA.setCandidate(canA);
						sA.setResult(resA);
						sA.setNot(notA);
						if(spillBig&&canA.size()>N)
							spillToDisk(writer,sA);
						else//cleanup时候不会直接spill小图到磁盘
							stack.add(sA);
						treesize++;
					}
					not.put(yint, y);
					if(y.rdeg==res.size())
						prunablenot.add(y);
				}
			}
			long t2 = System.currentTimeMillis();
			return t2-t1;
		}
		private static SubGraph initSize1SubGraph(Integer current) {
			ArrayList<Pair> tmpres = new ArrayList<Pair>();
			Pair curre = new Pair(current, 0);
			tmpres.add(curre);

			HashMap<Integer, Pair> tmpcand = new HashMap<Integer, Pair>();
			HashMap<Integer, Pair> tmpnot = new HashMap<Integer, Pair>();
			// 生成size-1的子图
			getCandidate2(tmpcand, tmpnot, curre);
			if(tmpcand.size()+1<quasiCliqueSize)
				return null;
			SubGraph initsub = new SubGraph();
			initsub.setCandidate(tmpcand);
			initsub.setNot(tmpnot);
			initsub.setResult(tmpres);
			return initsub;
		}
		private static boolean duplicate(HashMap<Integer, Pair> not,
				ArrayList<Pair> res, HashMap<Integer, Pair> candidate) {
			ArrayList<Integer> critnodes = new ArrayList<Integer>();
			int size = res.size() + candidate.size() - k_plex;
			for (Pair c : candidate.values()) {
				if (c.cdeg + c.rdeg == size)
					critnodes.add(c.point);
			}
			for (Pair c : res) {
				if (c.cdeg + c.rdeg == size)
					critnodes.add(c.point);
			}
			if (!critnodes.isEmpty()) {
				HashSet<Integer> intersection = new HashSet<Integer>();
				intersection.addAll(oneLeap.get(critnodes.get(0)));// 先加入第一个元素
				for (int i = 1; i < critnodes.size(); i++) {
					intersection.retainAll(oneLeap.get(critnodes.get(i)));// 不断取交集
				}
				if (not.size() < intersection.size()) {
					Iterator<Integer> it = not.keySet().iterator();
					while (it.hasNext()) {
						if (!intersection.contains(it.next()))
							it.remove();
					}
					for (Pair n : not.values()) {
						if (n.cdeg + n.rdeg <= size)
							continue;
						return true;
					}
				} else {
					for (Integer i : intersection) {
						Pair p = not.get(i);
						if (p != null) {
							if (p.cdeg + p.rdeg <= size)
								continue;
							return true;
						}
					}
				}
			} else {
				for (Pair n : not.values()) {
					if (n.cdeg + n.rdeg <= size)
						continue;
					return true;
				}
			}
			return false;
		}

		/**
		 * 从候选点集合中删除一个点y,加入到新子图中的结果集中 那么现有的候选集及结果集包括not集都需要更新这个点从候选集中删除后所带来的cdeg的影响
		 * 
		 * @param res
		 * @param not
		 * @param candidate
		 * @param deglist
		 * @param y
		 */
		private static void updateMarkDeg(ArrayList<Pair> res,
				HashMap<Integer, Pair> not, HashMap<Integer, Pair> candidate,
				DegList deglist, Pair y) {
			HashSet<Integer> adj = oneLeap.get(y.point);
			// 点y加入结果集中,导致res和not集中与y相邻的点的度数减1
			for (Pair r : res) {
				if (adj.contains(r.point))
					r.cdeg--;
			}
			if (not.size() < adj.size()) {
				for (Pair n : not.values()) {
					if (adj.contains(n.point))
						n.cdeg--;
				}
			} else {
				Pair p;
				for (Integer a : adj) {
					p = not.get(a);
					if (p != null) {
						p.cdeg--;
					}
				}
			}

			// 将y从候选点中移除
			ArrayList<Node> toerase = new ArrayList<Node>();
			Node aimnode = candidate.get(y.point).node;
			aimnode.points.remove(y.point);
			candidate.remove(y.point);
			if (aimnode.points.isEmpty())
				toerase.add(aimnode);

			int deg = -1;

			// 将candidate中所有和y相邻的点的cdeg度数--
			if (candidate.size() < adj.size()) {
				for (Pair ca : candidate.values()) {
					if (adj.contains(ca.point)) {
						// 点从当前度数集合中移除
						aimnode = ca.node;
						aimnode.points.remove(ca.point);
						// 加入到度数-1的点集合中
						deg = aimnode.deg - 1;
						if (aimnode.prev == null || aimnode.prev.deg != deg) {
							// 需要新建一个桶
							Node tpn = new Node();
							HashSet<Integer> tps = new HashSet<Integer>();
							tps.add(ca.point);
							tpn.points = tps;
							tpn.deg = deg;
							deglist.insertBefore(aimnode, tpn);
							ca.node = tpn;
						} else {
							aimnode.prev.points.add(ca.point);
							ca.node = aimnode.prev;
						}
						ca.cdeg = deg;
						if (aimnode.points.isEmpty())
							toerase.add(aimnode);
					}
				}
			} else {
				for (Integer ad : adj) {
					Pair p = candidate.get(ad);
					if (p != null) {
						aimnode = p.node;
						aimnode.points.remove(ad);
						deg = aimnode.deg - 1;
						if (aimnode.prev == null || aimnode.prev.deg != deg) {
							// 需要新建一个桶
							Node tpn = new Node();
							HashSet<Integer> tps = new HashSet<Integer>();
							tps.add(p.point);
							tpn.points = tps;
							tpn.deg = deg;
							deglist.insertBefore(aimnode, tpn);
							p.node = tpn;
						} else {
							aimnode.prev.points.add(p.point);
							p.node = aimnode.prev;
						}
						p.cdeg = deg;
						if (aimnode.points.isEmpty())
							toerase.add(aimnode);
					}
				}
			}

			for (Node n : toerase) {
				if (n.points.isEmpty())
					deglist.remove(n);
			}
		}

		private static void computeDeg(ArrayList<Pair>prunablenot,Map<Integer, Pair> res,
				HashMap<Integer, Pair> candidate,int ressize) {
			int num;
			for (Entry<Integer, Pair> p : res.entrySet()) {
				num = 0;
				HashSet<Integer> adj = oneLeap.get(p.getKey());
				if (adj.size() < candidate.size()) {
					for (Integer a : adj) {
						if (candidate.containsKey(a))
							num++;
					}
				} else {
					for (Integer c : candidate.keySet()) {
						if (adj.contains(c))
							num++;
					}
				}
				if(p.getValue().rdeg==ressize)
					prunablenot.add(p.getValue());
				p.getValue().cdeg = num;
			}
		}

		private static void computeDeg(ArrayList<Pair> res,
				HashMap<Integer, Pair> candidate) {
			int num;
			for (Pair p : res) {
				num = 0;
				HashSet<Integer> adj = oneLeap.get(p.point);
				if (adj.size() < candidate.size()) {
					for (Integer a : adj) {
						if (candidate.containsKey(a))
							num++;
					}
				} else {
					for (Integer c : candidate.keySet()) {
						if (adj.contains(c))
							num++;
					}
				}
				p.cdeg = num;
			}
		}

		private static void updateDeg(DegList deglist,
				HashMap<Integer, Pair> candidate) {
			HashMap<Integer, Node> deg2node = new HashMap<Integer, Node>();
			ArrayList<Node> nodes = new ArrayList<Node>();
			for (Pair ca : candidate.values())// oneLeap中节点和candidate中节点的交集数
			{
				int number = 0;
				HashSet<Integer> adj = oneLeap.get(ca.point);
				if (candidate.size() < adj.size()) {
					for (Integer out : candidate.keySet()) {
						if (adj.contains(out))
							number++;
					}
				} else {
					for (Integer a : adj) {
						if (candidate.containsKey(a))
							number++;
					}
				}
				Node it = deg2node.get(number);
				if (it == null) {
					HashSet<Integer> nset = new HashSet<Integer>();
					nset.add(ca.point);
					Node tn = new Node();
					tn.deg = number;
					tn.points = nset;
					nodes.add(tn);
					deg2node.put(number, tn);
					ca.node = tn;
				} else {
					it.points.add(ca.point);
					ca.node = it;
				}
				ca.cdeg = number;
			}
			Collections.sort(nodes);
			deglist.makeList(nodes);
		}

		private static boolean duplicate(ArrayList<Pair> not,
				int csize) {
			for(Pair p:not)
				if(p.cdeg==csize)
					return true;
			return false;
		}

		/**
		 * 生成初始的size-1子图,子图中点的rdeg度数以通过是一跳数据还是两跳数据记录为0和1
		 * 子图中candidate点的cdeg由于未计算都设置为默认值-1
		 * 
		 * @param candidate
		 * @param not
		 * @param current
		 */
		public static void getCandidate2(HashMap<Integer, Pair> candidate,
				HashMap<Integer, Pair> not, Pair current) {

			int tmpdeg = 0;
			final HashSet<Integer> oneadj;
			oneadj = oneLeap.get(current.point);
			int curdeg = oneadj.size();
			current.cdeg = curdeg;
			HashSet<Integer> twoadj;

			HashSet<Integer>tmpadj;
			// 缓存未命中
			twoadj = new HashSet<Integer>();
			for (Integer i : oneadj) {// 一跳集
				tmpadj = oneLeap.get(i);
				twoadj.addAll(tmpadj);// 生成二跳集
				tmpdeg = tmpadj.size();
				if (tmpdeg > curdeg) {
					not.put(i, new Pair(i, 1));
				} else if (tmpdeg < curdeg) {
					candidate.put(i, new Pair(i, 1));
				} else {
					if (i > current.point) {
						candidate.put(i, new Pair(i, 1));
					} else
						// if (i < current) {//一跳集中不会有current本身
						not.put(i, new Pair(i, 1));
					// }// 相等的话即current本身,不需要加入任何集合,不处理
				}
			}
			twoadj.removeAll(oneadj);// 仅包含第二跳集
			twoadj.remove(current.point);
			for (Integer i : twoadj) {
				tmpdeg = oneLeap.get(i).size();
				if (tmpdeg > curdeg) {
					not.put(i, new Pair(i, 0));
				} else if (tmpdeg < curdeg) {
					candidate.put(i, new Pair(i, 0));
				} else {
					if (i > current.point) {
						candidate.put(i, new Pair(i, 0));
					} else
						// if (i < current) {//上面保证了二跳集中不会有current
						not.put(i, new Pair(i, 0));
				}
			}
		}

		/**
		 * candidate的度数记录某个点和candidate中其他点相邻的个数
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
				in.cdeg = number;
			}
		}

		public static void getCriticalSet(List<Pair> res, List<Integer> critSet) {
			int size = res.size();
			if (size <= k_plex)
				return;
			size -= k_plex;
			for (Pair p : res) {
				if (p.rdeg == size)
					critSet.add(p.point);
			}
		}

		/**
		 * 通过临界点集合和条件1来过滤候选点和not集中可以加入到新子图中的点 同时更新加入到子图中点的rdeg值,因为子图结果集中比母图多一个分裂点
		 * 
		 * @param canA
		 * @param notA
		 * @param critSet
		 * @param candidate
		 * @param not
		 * @param adj
		 * @throws CloneNotSupportedException
		 */
		private static void filterCandidate(HashMap<Integer, Pair> canA,
				HashMap<Integer, Pair> notA, ArrayList<Integer> critSet,
				HashMap<Integer, Pair> candidate, HashMap<Integer, Pair> not,
				HashSet<Integer> yadj, int ressize){
			if (!critSet.isEmpty()) {
				HashSet<Integer> intersection = new HashSet<Integer>();
				intersection.addAll(oneLeap.get(critSet.get(0)));// 先加入第一个元素
				for (int i = 1; i < critSet.size(); i++) {
					intersection.retainAll(oneLeap.get(critSet.get(i)));// 不断取交集
				}
				// 用临界点过滤candidate
				if (intersection.size() < candidate.size()) {
					for (Integer i : intersection) {
						Pair p = candidate.get(i);
						if (p != null) {
							canA.put(i, p.clone());
						}
					}
				} else {
					for (Pair p : candidate.values()) {
						if (intersection.contains(p.point))
							canA.put(p.point, p.clone());
					}
				}
				// 用临界点过滤not
				if (intersection.size() < not.size()) {
					for (Integer i : intersection) {
						Pair p = not.get(i);
						if (p != null) {
							notA.put(i, p.clone());
						}
					}
				} else {
					for (Pair p : not.values()) {
						if (intersection.contains(p.point))
							notA.put(p.point, p.clone());
					}
				}
			} else {
				for (Entry<Integer, Pair> en : candidate.entrySet())
					canA.put(en.getKey(), en.getValue().clone());
				for (Entry<Integer, Pair> en : not.entrySet())
					notA.put(en.getKey(), en.getValue().clone());
			}
			// 更新集合中点的rdeg,包含在adj中点的rdeg++,同时过滤掉其中不满足条件1的点
			updateRdeg(canA, yadj, ressize);
			updateRdeg(notA, yadj, ressize);
		}

		/**
		 * 更新集合中点的rdeg,包含在adj中点的rdeg++,同时过滤掉其中不满足条件1的点
		 * 
		 * @param can
		 * @param yadj
		 * @param ressize
		 */
		private static void updateRdeg(HashMap<Integer, Pair> can,
				HashSet<Integer> adj, int ressize) {
			Iterator<Entry<Integer, Pair>> it = can.entrySet().iterator();
			int mindeg = ressize - k_plex + 1;
			while (it.hasNext()) {
				Pair p = it.next().getValue();
				if (adj.contains(p.point))
					p.rdeg++;
				else if (p.rdeg < mindeg) {
					it.remove();
				}
			}
		}

		/**
		 * 遍历candidate和res中所有点判断是不是所有点的rdeg+cdeg都满足kplex条件
		 * 
		 * @param res
		 * @param candidate
		 * @param critnodes
		 * @return
		 */
		public static boolean judgeKplex2(ArrayList<Pair> res,
				HashMap<Integer, Pair> candidate) {
			int size = res.size() + candidate.size() - k_plex;
			for (Pair r : res) {
				if (r.rdeg + r.cdeg < size)
					return false;
			}
			for (Pair c : candidate.values()) {
				if (c.rdeg + c.cdeg < size)
					return false;
			}
			return true;
		}
		private void spillToDisk(FileWriter writer, SubGraph pop) throws IOException {
			writer.write(((count++)%reduceNumber)+" "+pop.getResult().size()+"%");
			writer.write(pop.toString());
			writer.write("\n");
		}
	}
}
