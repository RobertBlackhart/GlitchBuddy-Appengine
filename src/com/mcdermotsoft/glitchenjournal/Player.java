package com.mcdermotsoft.glitchenjournal;

public class Player 
{
	private String name, tsid;
	
	public Player(String name, String tsid)
	{
		setName(name);
		setTsid(tsid);
	}

	public String getName() 
	{
		return name;
	}
	
	public String getTsid()
	{
		return tsid;
	}

	public void setName(String name) 
	{
		this.name = name;
	}
	
	public void setTsid(String tsid)
	{
		this.tsid = tsid;
	}
	
	public String toJSONString()
	{
		return "{\"name\":\"" + name + "\", \"tsid\":\"" + tsid + "\"}";
	}
	
	@Override
	public boolean equals(Object object)
	{
		if(object instanceof Player)
		{
			Player player = (Player)object;
			if(player.getName().equals(this.getName()))
				return true;
			else
				return false;
		}
		else
			return false;
	}
}
