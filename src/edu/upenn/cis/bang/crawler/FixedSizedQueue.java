

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



public class FixedSizedQueue<K, V>  { 

	private ArrayList<K> queue; 
	private int maxElements;
	private Map <K, V> data;

	public FixedSizedQueue(int maxElements) 
	{ 
		this.maxElements = maxElements;
		queue = new ArrayList<K>(maxElements); 
		data = new HashMap <K, V>();
	} 

	public boolean contains(K o){
		return queue.contains(o);
	}

	public void insert (K o, V v) throws IndexOutOfBoundsException{
		if (maxElements == queue.size()){
			K obj = queue.remove(0);
			data.remove(obj);
		} 
		queue.add(o);
		data.put(o,v);
	}

	public V get(K o){
		return data.get(o);
	}
	
	public boolean isEmpty () {
		return queue.isEmpty();
	} 

	public boolean isFull ()
	{ 
		return  maxElements == queue.size();
	} 

	public Object remove () throws IndexOutOfBoundsException {
		K obj =  queue.remove(0);
		data.remove(obj);
		return obj;
	}
}