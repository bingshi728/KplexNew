package HashVersion;


public class Pair implements Comparable<Pair>{
	public Integer point;
	public int discon;
	public int deg;
	
	/**
	 * @param point
	 * @param discon
	 * @param deg
	 */
	public Pair(int point, int discon, int deg) {
		super();
		this.point = point;
		this.discon = discon;
		this.deg = deg;
	}
	public Pair(){
		this.point = -1;
		this.deg = -1;
		this.discon = -1;
	}
	public Pair(Pair c) {
		this.point = c.point;
		this.deg = c.deg;
		this.discon = c.discon;
	}
	@Override
	public int compareTo(Pair o) {
		//return deg-((Pair)o).deg;//deg由小到大
		return o.deg-deg;//deg由大到小
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return point.hashCode();
	}
	@Override
	public String toString(){
		return ""+point;
	}
	/* 两个pair相等只看其对应的点是不是相等,每个点同时只能出现在一个集合中
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==null||!(obj instanceof Pair))
			return false;
		return this.point==((Pair)obj).point;
	}
	
}