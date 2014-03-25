package notwoleapversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SubGraph {
	private HashMap<Integer,Pair> candidate;
	private HashMap<Integer,Pair> not;
	private ArrayList<Pair> result;
	public SubGraph()
	{
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
