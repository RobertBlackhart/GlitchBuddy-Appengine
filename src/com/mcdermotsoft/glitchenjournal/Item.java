package com.mcdermotsoft.glitchenjournal;

public class Item 
{
	private String itemName, playerName, tsid, id; 
	private double cost, averageCost;
	private long count;
	
	public Item(String itemName, String playerName, String tsid, String id, long count, double cost, double averageCost)
	{
		this.itemName = itemName;
		this.playerName = playerName;
		this.tsid = tsid;
		this.id = id;
		this.count = count;
		this.cost = cost;
		this.averageCost = averageCost;
	}

	public String getItemName() {
		return itemName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public String getTsid() {
		return tsid;
	}

	public String getId() {
		return id;
	}

	public double getCost() {
		return cost;
	}

	public double getAverageCost() {
		return averageCost;
	}

	public long getCount() {
		return count;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public void setTsid(String tsid) {
		this.tsid = tsid;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public void setAverageCost(double averageCost) {
		this.averageCost = averageCost;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public String toJSONString()
	{
		String jsonString = "";
		jsonString += "\"properties\": {";
		jsonString += "\"itemTsid\": \"" + tsid + "\",";
		jsonString += "\"player\": \"" + playerName + "\",";
		jsonString += "\"count\": \"" + count + "\",";
		jsonString += "\"name\": \"" + itemName + "\",";
		jsonString += "\"cost\": \"" + cost + "\"";
		jsonString += "\"averageCost\": \"" + averageCost + "\"";
		jsonString += "},";
		jsonString += "\"key\": {";
		jsonString += "\"name\": \"" + id + "\"},";
		
		return jsonString;
	}
}