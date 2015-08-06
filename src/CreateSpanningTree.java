import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;


public class CreateSpanningTree
{
	private LinkedHashMap<Character, NodeInfo> topologyMap;
	private NodeInfo myInfo;
	private DisseminationInfo myDisInfo;
	private int totalNodes;
	private int myLocInFile;
	public static NodeInfo[] nodes;
	public static DisseminationInfo[] disseminationInfos;
	boolean[] isAddedToTree;

	private static DatagramSocket socket = null;
	private static String queryResult;

	public NodeInfo getMyInfo()
	{
		return myInfo;
	}

	public static DatagramSocket getSocket()
	{
		return socket;
	}

	public DisseminationInfo getMyDisInfo()
	{
		return myDisInfo;
	}

	public static String getQueryResult()
	{
		return queryResult;
	}

	public static void setQueryResult(String str)
	{
		queryResult = str;
	}

	public CreateSpanningTree(int num)
	{
		topologyMap = new LinkedHashMap<Character, NodeInfo>();
		myInfo = new NodeInfo();
		myDisInfo = new DisseminationInfo();
		myLocInFile = num;
		ReadTopologyInfo();
	}

	private void SetupConnection() throws UnknownHostException
	{
		if (socket != null)
			return;
		try
		{
			socket = new DatagramSocket(myInfo.getPort());
		} catch (SocketException e)
		{
			e.printStackTrace();
		}
	}

	private void ReadTopologyInfo()
	{
		try
		{
			FileReader fr = new FileReader(Constants.TOPOLOGY_FILE);
			BufferedReader br = new BufferedReader(fr);
			String line;
			String[] token = new String[10];
			int i = 0;
			while ((line = br.readLine()) != null)
			{
				NodeInfo ni = new NodeInfo();
				token = line.split(",");

				ni.setName(token[0].toCharArray()[0]);

				ni.setPosition(Integer.parseInt(token[1].trim()), Integer.parseInt(token[2].trim()));

				String ip_port;
				ip_port = token[3].trim();
				String[] part = new String[2];
				part = ip_port.split("/");
				ni.setIp(part[0]);
				ni.setPort(Integer.parseInt(part[1].trim()));

				ni.setTemperature(Integer.parseInt(token[4].trim()));

				ni.setRange(Integer.parseInt(token[5].trim()));

				if(Integer.parseInt(token[6].trim()) == 1)
					ni.setRoot(true);
				else
					ni.setRoot(false);

				ni.setId(i);

				if(i == myLocInFile - 1)
				{
					myInfo = ni;
					LogWriter.setMyNodeName(ni.getName());
					LogWriter.Initialise();
					LogWriter.Log("Extraction of this node information from topology file successful.");
				}
				// Add the node to topology information
				topologyMap.put(ni.getName(), ni);

				i++;
			}
			totalNodes = i;

			if (myInfo.isRootNode())
			{
				EstablishSpanningTree();
				BroadcastTreeInfo(myDisInfo, myInfo.getNeighbors());
			}
			else
			{
				EstablishNeighbors();
				WaitForTreeInfo();
			}
			br.close();

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void WaitForTreeInfo() throws IOException, ClassNotFoundException
	{
		SetupConnection();
		boolean treeEstablished = false;
		while (!treeEstablished)
		{
			byte[] buff = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buff, 1024);
			socket.receive(packet);
			LogWriter.Log("Received tree dissemination information. Extracting it and converting into"
					+ " DisseminationInfo object.");

			DisseminationInfo dis = DisseminationInfo.ExtractDisseminationInfo(packet);

			if(dis == null)
				continue;

			boolean isMyInfo = false;
			for (DisseminationInfo child : dis.getChildren())
			{
				if(child.getName() == myInfo.getName())
				{
					LogWriter.Log("I am in the children list. So this information is intended for me.");
					// This info is intended for me so save it
					myDisInfo = child;
					myInfo.setParent(topologyMap.get(myDisInfo.getParent()));
					isMyInfo = true;
					treeEstablished = true;
					System.out.println("Tree information received.");
					break;
				}
			}
			if(isMyInfo && myDisInfo.getChildren() != null)
			{
				LogWriter.Log("As I can see that I have some child nodes assigned, I need to broadcast"
						+ " tree information to them.");
				// Set children in myInfo and broadcast to neighbors
				System.out.println("Assigned child nodes :");
				LogWriter.Log("Assigned child nodes :");
				for (DisseminationInfo myChild : myDisInfo.getChildren())
				{
					System.out.println(myChild.getName());
					LogWriter.Log(myChild.getName() + " ");
					NodeInfo nodeInfo = new NodeInfo();
					nodeInfo = topologyMap.get(myChild.getName());
					myInfo.addChild(nodeInfo);
				}
				BroadcastTreeInfo(myDisInfo, myInfo.getNeighbors());
			}
			else
			{
				System.out.println("I don't have any child nodes assigned. No need to broadcast.");
				LogWriter.Log("I don't have any child nodes assigned. No need to broadcast anything.");
			}
		}
	}

	private void BroadcastTreeInfo(DisseminationInfo disInfo, ArrayList<NodeInfo> neighbors)
	{
		try
		{
			SetupConnection();

			byte[] objectBytes = disInfo.ConvertToByteArray();
			StringBuilder sb = new StringBuilder();
			sb.append("Broadcasting the tree information. Sending data of size "
			+ objectBytes.length + " bytes to : ");

			for (NodeInfo neighborInfo : neighbors)
			{
				InetAddress inetAddr = InetAddress.getByName(neighborInfo.getIp());
				DatagramPacket packet = new DatagramPacket(objectBytes, objectBytes.length, 
						inetAddr, neighborInfo.getPort());
				socket.send(packet);
				sb.append(neighborInfo.getName() + " ");
			}
			LogWriter.Log(sb.toString());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void PopulateNodesAndDisseminationInfos()
	{
		nodes = new NodeInfo[totalNodes];
		disseminationInfos = new DisseminationInfo[totalNodes];
		isAddedToTree = new boolean[totalNodes];

		int i = 0;
		// Iterate over the topologyMap and initialise NodeInfo[] nodes
		for (Map.Entry<Character, NodeInfo> entry : topologyMap.entrySet())
		{
			nodes[i] = new NodeInfo();
			nodes[i] = entry.getValue();
			disseminationInfos[i] = new DisseminationInfo();
			disseminationInfos[i].setName(nodes[i].getName());
			isAddedToTree[i] = false;
			i++;
		}
		myInfo = nodes[myLocInFile - 1];
		myDisInfo = disseminationInfos[myLocInFile - 1];
	}

	private void EstablishNeighbors() throws IOException
	{
		PopulateNodesAndDisseminationInfos();

		LogWriter.Log("Establishing neighbours.");
		System.out.println("Establishing neighbours.");
		int myRange = myInfo.getRange();
		char myName = myInfo.getName();

		myInfo.setParent(null);
		for (NodeInfo node : nodes)
		{
			if(myName == node.getName())
				continue;
			
			double distance = GetEuclideanDistance(myInfo, node);
			if (distance <= myRange)
			{
				myInfo.addNeighbor(node);
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Neighbours : ");
		for (NodeInfo node : myInfo.getNeighbors())
		{
			sb.append(node.getName() + " ");
		}
		LogWriter.Log(sb.toString());
	}

	private void EstablishSpanningTree() throws IOException
	{
		int range = myInfo.getRange();
		int count = 1;

		LogWriter.Log("As I am the root node, calculating the spanning tree.");

		PopulateNodesAndDisseminationInfos();

		Queue<NodeInfo> queue = new LinkedList<NodeInfo>();
		Queue<DisseminationInfo> disQueue = new LinkedList<DisseminationInfo>();

		queue.add(myInfo);
		disQueue.add(myDisInfo);
		isAddedToTree[(int) myInfo.getId()] = true;

		while (count < totalNodes)
		{
			// Calculate Euclidean Distance from each node
			// Start from root and keep adding new nodes to queue
			NodeInfo currentNode = (NodeInfo) queue.remove();
			DisseminationInfo currentDisInfo = disQueue.remove();

			int currentId = currentNode.getId();
			range = currentNode.getRange();

			for (NodeInfo node : nodes)
			{
				int nodeId = (int) node.getId();
				// If both nodes are same then skip
				if (currentId == nodeId)
					continue;
				double distance = GetEuclideanDistance(currentNode, node);
				if (distance <= range)
				{
					currentNode.addNeighbor(node);
					// If node has no parent assigned then assign it else skip
					if (!isAddedToTree[nodeId])
					{
						currentNode.addChild(node);
						node.setRoot(false);
						node.setParent(currentNode);
						queue.add(node);

						DisseminationInfo newDisInfo = disseminationInfos[nodeId];
						currentDisInfo.addChild(newDisInfo);
						newDisInfo.setParent(currentDisInfo.getName());
						disQueue.add(newDisInfo);

						count++;
						isAddedToTree[nodeId] = true;
					}
				}
			}
		}
		System.out.println("Spanning tree calculated.");
		LogSpanningTree();
	}

	private void LogSpanningTree() throws IOException
	{
		for (DisseminationInfo disseminationInfo : disseminationInfos)
		{
			if(disseminationInfo.getChildren() == null)
				continue;

			StringBuilder log = new StringBuilder();
			log.append(disseminationInfo.getName() + " -> ");
			for (DisseminationInfo child : disseminationInfo.getChildren())
			{
				log.append(child.getName() + " ");
			}
			LogWriter.Log(log.toString());
		}
	}

	private double GetEuclideanDistance(NodeInfo currentNode, NodeInfo node)
	{
		return Math.sqrt(Math.pow(currentNode.getXPosition() - node.getXPosition(), 2)
						+ Math.pow(currentNode.getYPosition() - node.getYPosition(), 2));
	}

	public static void main(String[] args)
	{
		try
		{
			CreateSpanningTree spt = new CreateSpanningTree(Integer.parseInt(args[0]));
			NodeInfo nodeInfo = spt.getMyInfo();

			while(true)
			{
				String commandString = "";
				if(nodeInfo.isRootNode())
				{
					System.out.println("Enter query (avg / min / max) :");
					Scanner scanner = new Scanner(System.in);
					commandString = scanner.next();
				}

				Thread t = new Thread(new UdpPacketReceiver(nodeInfo, commandString));
				t.start();
				t.join();

				System.out.println(queryResult);
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			System.out.println("End");
		}
	}

}
