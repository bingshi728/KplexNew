package notwoleapversion;

import java.util.List;

public class DegList {
	private Node head;
	private Node tail;
	int size;
	public DegList(){
		tail = head = null;
		size = 0;
	}
	public DegList(Node first){
		head = first;
		tail = head;
	}
	public boolean isEmpty(){
		return size==0;
	}
	public Node getHead(){
		return head;
	}
	public Node getTail(){
		return tail;
	}
	public void insertBefore(Node pos, Node newnode){
		Node prv = pos.prev;
		pos.prev = newnode;
		newnode.next = pos;
		newnode.prev = prv;
		if(prv!=null){
			prv.next = newnode;
		}else{
			head = newnode;
		}
		size++;
	}
//	public void insertAfter(Node pos, Node newnode){
//		
//	}
	public void remove(Node pos){
		Node prv = pos.prev;
		Node next = pos.next;
		if(prv!=null){
			prv.next = next;
		}else{
			head = next;
		}
		if(next!=null){
			next.prev = prv;
		}else{
			tail = prv;
		}
		size--;
	}
	public void makeList(List<Node> orderedList){
		if(orderedList.size()==0){
			head = tail = null;
			return;
		}
		Node pos = orderedList.get(0);
		head = pos;
		head.prev = null;
		for(int i=1;i<orderedList.size();i++){
			Node tmp = orderedList.get(i);
			pos.next = tmp;
			tmp.prev = pos;
			pos = tmp;
		}
		tail = pos;
		tail.next = null;
		size = orderedList.size();
	}
	public int size(){
		return size;
	}
}
