package notwoleapversion;

import java.util.Set;

public class Node implements Comparable<Node>{
	Node prev;
	int deg;
	Set<Integer> points;
	Node next;
	@Override
	public int compareTo(Node o) {
		return deg-o.deg;
	}
}
