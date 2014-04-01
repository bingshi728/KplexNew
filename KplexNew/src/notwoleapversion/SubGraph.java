package notwoleapversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;

public class SubGraph {
	private HashMap<Integer,Pair> candidate;
	private HashMap<Integer,Pair> not;
	private ArrayList<Pair> result;
	public SubGraph()
	{
	}
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(candidate.size());
		for(Entry<Integer, Pair> en:candidate.entrySet()){
			sb.append(" ");
			sb.append(en.getKey());
			sb.append(" ");
			sb.append(en.getValue().point);
			sb.append(" ");
			sb.append(en.getValue().rdeg);
		}
		sb.append(" ");
		sb.append(not.size());
		for(Entry<Integer, Pair> en:not.entrySet()){
			sb.append(" ");
			sb.append(en.getKey());
			sb.append(" ");
			sb.append(en.getValue().point);
			sb.append(" ");
			sb.append(en.getValue().rdeg);
		}
		sb.append(" ");
		sb.append(result.size());
		for(Pair p:result){
			sb.append(" ");
			sb.append(p.point);
			sb.append(" ");
			sb.append(p.rdeg);
		}
		return sb.toString();
	}
	public void readInString(String s){
		StringTokenizer st = new StringTokenizer(s);
		int len = Integer.valueOf(st.nextToken());
		candidate = new HashMap<Integer,Pair>(len);
		for(;len>0;len--){
			Integer key = Integer.valueOf(st.nextToken());
			Integer point = Integer.valueOf(st.nextToken());
			Integer rdeg = Integer.valueOf(st.nextToken());
			candidate.put(key, new Pair(point,rdeg));
		}
		len = Integer.valueOf(st.nextToken());
		not = new HashMap<Integer,Pair>(len);
		for(;len>0;len--){
			Integer key = Integer.valueOf(st.nextToken());
			Integer point = Integer.valueOf(st.nextToken());
			Integer rdeg = Integer.valueOf(st.nextToken());
			not.put(key, new Pair(point,rdeg));
		}
		len = Integer.valueOf(st.nextToken());
		result = new ArrayList<Pair>(len);
		for(;len>0;len--){
			Integer point = Integer.valueOf(st.nextToken());
			Integer rdeg = Integer.valueOf(st.nextToken());
			result.add( new Pair(point,rdeg));
		}
	}
	public  HashMap<Integer,Pair> getCandidate() {
		return candidate;
	}

	public void setCandidate( HashMap<Integer,Pair>candidate) {
		this.candidate = candidate;
	}

	public  HashMap<Integer,Pair> getNot() {
		return not;
	}

	public void setNot( HashMap<Integer,Pair> not) {
		this.not = not;
	}

	public ArrayList<Pair> getResult() {
		return result;
	}
	public void setResult(ArrayList<Pair> result) {
		this.result = result;
	}

}
