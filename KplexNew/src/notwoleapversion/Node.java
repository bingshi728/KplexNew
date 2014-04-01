package notwoleapversion;

import java.util.Set;

public class Node implements Comparable<Node>{
	public Node prev;
	public int deg;
	public Set<Integer> points;
	public Node next;
	@Override
	public int compareTo(Node o) {
		return deg-o.deg;
	}
}
