package org.dhp.common.utils;

import java.util.Collection;
import java.util.Vector;

public class LimitedList<E> extends Vector<E> {
	
	int limit = Integer.MAX_VALUE;
	
	public LimitedList(int limit) {
		this.limit = limit;
	}
	
	@Override
	public synchronized boolean add(E e) {
		if(this.size()> limit) {
			this.remove(0);
		}
		return super.add(e);
	}
	
	public synchronized E addAndPop(E e) {
		E last = null;
		if(this.size()> limit) {
			last = this.remove(0);
		}
		super.add(e);
		return last;
	}
	
	@Override
	public synchronized boolean addAll(Collection<? extends E> c) {
		int addSize = c.size();
		int size = this.size();
		while(size+addSize>limit) {
			this.remove(0);
			size--;
		}
		return super.addAll(c);
	}

}
