import java.util.ArrayList;

public class NodeInfo
{

	/*
	 * VARIABLES
	 */
	private int id;
	private char name;
	private NodeInfo parent;
	private double xPos;
	private double yPos;
	private ArrayList<NodeInfo> children;
	private ArrayList<NodeInfo> neighbors;
	private boolean isRoot = false;
	private String ip;
	private int port;
	private int temperature;
	private int range;

	/*
	 * CONSTRUCTORS
	 */
	public NodeInfo()
	{
		id = -999;
		name = 0;
		parent = null;
		children = null;
		neighbors = null;
	}

	/*
	 * SETTERS
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	public void setName(char name)
	{
		this.name = name;
	}

	public void setParent(NodeInfo pNode)
	{
		parent = pNode;
	}

	public void setPosition(double x, double y)
	{
		xPos = x;
		yPos = y;
	}

	public void setRoot(boolean root)
	{
		isRoot = root;
	}

	public void setIp(String IP)
	{
		ip = IP;
	}

	public void setPort(int portnum)
	{
		port = portnum;
	}

	public void setTemperature(int temp)
	{
		temperature = temp;
	}

	public void setRange(int r)
	{
		range = r;
	}

	/*
	 * GETTERS
	 */
	public int getId()
	{
		return id;
	}

	public char getName()
	{
		return name;
	}

	public NodeInfo getParent()
	{
		return parent;
	}

	public double getXPosition()
	{
		return xPos;
	}

	public double getYPosition()
	{
		return yPos;
	}

	public ArrayList<NodeInfo> getChildren()
	{
		return children;
	}

	public ArrayList<NodeInfo> getNeighbors()
	{
		return neighbors;
	}

	public String getIp()
	{
		return ip;
	}

	public int getPort()
	{
		return port;
	}

	public int getTemperature()
	{
		return temperature;
	}

	public int getRange()
	{
		return range;
	}

	/*
	 * METHODS
	 */
	public boolean isRootNode()
	{
		return isRoot;
	}

	public boolean hasChild()
	{
		return children == null ? false : true;
	}

	public void addChild(NodeInfo child)
	{
		if (children == null)
			children = new ArrayList<NodeInfo>();
		children.add(child);
	}

	public void addNeighbor(NodeInfo neighbor)
	{
		if (neighbors == null)
			neighbors = new ArrayList<NodeInfo>();
		neighbors.add(neighbor);
	}

	@Override
	public String toString()
	{
		if (parent == null)
			return String.format("Name : %s\tId : %s\tParent : %s\tX : %s\tY : %s", name, id, "null", xPos, yPos);
		else
			return String.format("Name : %s\tId : %s\tParent : %s\tX : %s\tY : %s", name, id, parent.getName(), xPos, yPos);
	}

	@Override
	public boolean equals(Object obj)
	{
		return ((NodeInfo) obj).getName() == this.getName();
	}

}
