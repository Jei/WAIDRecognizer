package it.unibo.cs.jonus.waidrec;

public class Evaluation {
	private long id;
	private long timestamp;
	private String category;
	
	public Evaluation(long id, long timestamp, String category) {
		super();
		setId(id);
		setTimestamp(timestamp);
		setCategory(category);
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}

}
