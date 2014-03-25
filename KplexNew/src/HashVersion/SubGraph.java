package HashVersion;

import java.util.ArrayList;
import java.util.HashMap;

public class SubGraph {
	private ArrayList<Pair> candidate;
	private HashMap<Integer,Integer> not;
	private HashMap<Integer,Integer> result;
	public SubGraph()
	{
	}

	public ArrayList<Pair> getCandidate() {
		return candidate;
	}

	public void setCandidate(ArrayList<Pair> candidate) {
		this.candidate = candidate;
	}

	public HashMap<Integer, Integer> getNot() {
		return not;
	}

	public void setNot(HashMap<Integer, Integer> not) {
		this.not = not;
	}

	public HashMap<Integer, Integer> getResult() {
		return result;
	}
	public void setResult(HashMap<Integer, Integer> result) {
		this.result = result;
	}

}
