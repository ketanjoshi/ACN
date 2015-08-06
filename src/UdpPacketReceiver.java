import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class UdpPacketReceiver implements Runnable
{

	private DatagramPacket packet;
	private NodeInfo myNodeInfo;
	private int numOfReplies;
	private int numOfChildren;
	private String command;
	private DatagramSocket socket;
	private volatile boolean running;

	public UdpPacketReceiver(NodeInfo ni, String cmd)
	{
		myNodeInfo = new NodeInfo();
		myNodeInfo = ni;
		numOfChildren = ni.hasChild() ? ni.getChildren().size() : 0;
		numOfReplies = 0;
		socket = CreateSpanningTree.getSocket();
		packet = new DatagramPacket(new byte[Constants.PKT_SIZE], Constants.PKT_SIZE);
		command = cmd.toLowerCase();
		running = true;
	}

	
	@Override
	public void run()
	{
		/*
		 *  Code required for listening for command from parent and passing it to the child
		 *  Also gather replies, filter out those which are not from the children
		 *  Process the filtered replies and send it to parent
		 */

		try
		{
			while(running)
			{
				numOfReplies = 0;
				Message.MessageType replyType = null;
				String replyMsg = "";
				Message message;

				if(!myNodeInfo.isRootNode())
				{
					packet = new DatagramPacket(new byte[Constants.PKT_SIZE], Constants.PKT_SIZE);
					socket.receive(packet);
					message = Message.ExtractMessage(packet.getData());
					if(message == null)
					{
						continue;
					}
					if(!IsParent(message.getOriginatorNode()))
					{
						LogWriter.Log(String.format(
								"Received a message from %s. But it is not from my deidcated parent"
								+ " so discarding it.", message.getOriginatorNode()));
						continue;
					}
					
				}
				else
				{
					/*
					 *  This is a root node so it won't receive any packet on the socket.
					 *  It will receive the command over the console.
					 *  We create a new message from this command and broadcast it.
					 */

					Message.MessageType type = null;
					switch (command)
					{
					case "avg":
						type = Message.MessageType.AvgRequest;
						break;

					case "min":
						type = Message.MessageType.MinRequest;
						break;

					case "max":
						type = Message.MessageType.MaxRequest;
						break;

					default:
						break;
					}
					message = new Message(myNodeInfo.getName(), type, "");
				}

				Message.MessageType mtype = message.getMessageType();
				if(mtype.equals(Message.MessageType.AvgRequest) ||
						mtype.equals(Message.MessageType.MinRequest) ||
						mtype.equals(Message.MessageType.MaxRequest))
				{
					if(mtype.equals(Message.MessageType.AvgRequest))
						replyType = Message.MessageType.AvgReply;
					else if(mtype.equals(Message.MessageType.MaxRequest))
						replyType = Message.MessageType.MaxReply;
					else if(mtype.equals(Message.MessageType.MinRequest))
						replyType = Message.MessageType.MinReply;

					if(myNodeInfo.hasChild())
					{
						/*
						 *  Broadcast the message to neighbors and wait for replies.
						 */
						message.setOriginatorNode(myNodeInfo.getName());
						BroadcastMessage(message);
					}
					else
					{
						/*
						 *  This causes condition on while loop to evaluate to false.
						 *  So leaf node automatically skips the loop.
						 */
						numOfReplies += numOfChildren;
					}

					int temperature = myNodeInfo.getTemperature();
					int replyNodes = 1;
					LogWriter.Log("Waiting for reply from child nodes : " + numOfChildren);
					while(numOfReplies < numOfChildren)
					{
						// Keep listening on the port and check for replies
						packet = new DatagramPacket(new byte[Constants.PKT_SIZE], Constants.PKT_SIZE);
						socket.receive(packet);
						Message reply = Message.ExtractMessage(packet.getData());
						if(reply == null)
							continue;
						if(!IsChild(reply.getOriginatorNode()))
						{
							// Message is not from a child.... so ignore
							continue;
						}

						Message.MessageType receivedReplyType = reply.getMessageType();
						if(receivedReplyType.equals(replyType))
						{
							// Process the received reply
							String replyString = reply.getMessage();
							if(receivedReplyType.equals(Message.MessageType.AvgReply))
							{
								String[] split = replyString.split(",");
								temperature += Integer.parseInt(split[0]);
								replyNodes += Integer.parseInt(split[1]);
							}
							else if(receivedReplyType.equals(Message.MessageType.MinReply))
							{
								String[] split = replyString.split(",");
								int receivedTemp = Integer.parseInt(split[0]);
								temperature = temperature < receivedTemp ? temperature : receivedTemp;
							}
							else if(receivedReplyType.equals(Message.MessageType.MaxReply))
							{
								String[] split = replyString.split(",");
								int receivedTemp = Integer.parseInt(split[0]);
								temperature = temperature > receivedTemp ? temperature : receivedTemp;
							}
							numOfReplies++;
						}
					}
					// Exiting loop means all the child have replied

					if(myNodeInfo.isRootNode())
					{
						// Display the result
						running = false;
						String queryReply = "";

						if(replyType.equals(Message.MessageType.AvgReply))
						{
							double avg =  ((double)temperature) / ((double)replyNodes);
							System.out.println(temperature + "\t" + replyNodes + "\t" + avg);
							queryReply = "Average temperature : " + String.valueOf(avg);
						}
						else if (replyType.equals(Message.MessageType.MinReply))
						{
							queryReply = "Minimum temperature : " + temperature;
						}
						else if (replyType.equals(Message.MessageType.MaxReply))
						{
							queryReply = "Maximum temperature : " + temperature;
						}
						CreateSpanningTree.setQueryResult(queryReply);
						LogWriter.Log(queryReply);
					}
					else
					{
						StringBuilder sb = new StringBuilder();
						if(replyType.equals(Message.MessageType.AvgReply))
						{
							sb.append(temperature + "," + replyNodes);
							replyMsg = sb.toString();
						}
						else if (replyType.equals(Message.MessageType.MinReply) || 
								replyType.equals(Message.MessageType.MaxReply))
						{
							sb.append(temperature);
							replyMsg = sb.toString();
						}

						Message replyToParent = new Message(myNodeInfo.getName(), replyType, replyMsg);
						BroadcastMessage(replyToParent);
					}
				}
				else
					continue;
			}
		} catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	private void BroadcastMessage(Message message) throws InterruptedException, IOException
	{
		byte[] msgBuffer = message.ConvertToByteStream();

		StringBuilder sb = new StringBuilder();
		sb.append(myNodeInfo.getName() + " sending " + message.getMessageType().toString() + " to nodes : ");
		for (NodeInfo neighbor : myNodeInfo.getNeighbors())
		{
			InetAddress inetAddr = InetAddress.getByName(neighbor.getIp());
			DatagramPacket broadcastPacket = new DatagramPacket(msgBuffer, msgBuffer.length, 
					inetAddr, neighbor.getPort());
			CreateSpanningTree.getSocket().send(broadcastPacket);
			sb.append(neighbor.getName() + " ");
		}
		LogWriter.Log(sb.toString());
		LogWriter.Log("Message : " + message.toString());
		LogWriter.Log("Size : " + msgBuffer.length + " bytes");
	}

	private boolean IsParent(char originatorNode)
	{
		if(myNodeInfo.getParent().getName() == originatorNode)
			return true;
		else
			return false;
	}

	private boolean IsChild(char originatorNode)
	{
		for (NodeInfo node : myNodeInfo.getChildren())
		{
			if(node.getName() == originatorNode)
				return true;
		}
		return false;
	}

}
