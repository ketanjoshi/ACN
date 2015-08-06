import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Message
{
	public enum MessageType
	{
		AvgRequest ('1'),
		AvgReply ('2'),
		MinRequest ('3'),
		MinReply ('4'),
		MaxRequest ('5'),
		MaxReply ('6');

		private char mType;

		private MessageType(char c)
		{
			mType = c;
		}
		
		public char getValue()
		{
			return mType;
		}
	};

	private char originatorNode;
	private MessageType messageType;
	private String message;

	public Message(char originator, MessageType type, String msg)
	{
		originatorNode = originator;
		messageType = type;
		message = msg;
	}

	public MessageType getMessageType()
	{
		return messageType;
	}

	public char getOriginatorNode()
	{
		return originatorNode;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessageType(MessageType mType)
	{
		messageType = mType;
	}

	public void setOriginatorNode(char originator)
	{
		originatorNode = originator;
	}

	public void setMessage(String msg)
	{
		message = msg;
	}

	public static Message ExtractMessage(byte[] data) throws IOException
	{
		try
		{
			String msg = ConvertToString(data);
			char msgType = msg.charAt(1);
			String msgString = null;
			msgString = msg.substring(2, msg.lastIndexOf(','));
			return new Message(msg.charAt(0), Message.GetMessageType(msgType), msgString);
		} catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public String toString()
	{
		return String.format("%s%s%s,", originatorNode, messageType.getValue(), message);
	}

	public byte[] ConvertToByteStream() throws UnsupportedEncodingException
	{
		String dataString = this.toString();
		return dataString.getBytes(Constants.ENCODING_FORMAT);
	}

	private static MessageType GetMessageType(char c)
	{
		switch(c)
		{
			case '1' : return MessageType.AvgRequest;
			case '2' : return MessageType.AvgReply;
			case '3' : return MessageType.MinRequest;
			case '4' : return MessageType.MinReply;
			case '5' : return MessageType.MaxRequest;
			case '6' : return MessageType.MaxReply;
			default : return null;
		}
	}

	private static String ConvertToString(byte[] data) throws UnsupportedEncodingException
	{
		return new String(data, Constants.ENCODING_FORMAT);
	}
}
