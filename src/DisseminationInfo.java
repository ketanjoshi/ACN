import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.util.ArrayList;

public class DisseminationInfo implements Serializable
{

	private static final long serialVersionUID = -4037830192113740L;

	private char name;
	private ArrayList<DisseminationInfo> children;
	private char parent;

	public void setName(char Name)
	{
		name = Name;
	}

	public void setParent(char parentName)
	{
		parent = parentName;
	}

	public void addChild(DisseminationInfo childInfo)
	{
		if (children == null)
			children = new ArrayList<DisseminationInfo>();
		children.add(childInfo);
	}

	public char getName()
	{
		return name;
	}

	public char getParent()
	{
		return parent;
	}

	public ArrayList<DisseminationInfo> getChildren()
	{
		return children;
	}

	public DisseminationInfo()
	{
		parent = 0;
		children = null;
	}

	@Override
	public String toString()
	{
		return String.format("Name : %s\tParent : %s", name, parent);
	}

	@Override
	public boolean equals(Object obj)
	{
		return ((DisseminationInfo) obj).getName() == this.getName();
	}

	public byte[] ConvertToByteArray() throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
		ObjectOutputStream objectOutputStream;
		objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(this);
		return byteArrayOutputStream.toByteArray();
	}

	public static DisseminationInfo ExtractDisseminationInfo(DatagramPacket packet) throws IOException, ClassNotFoundException
	{
		ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
		DisseminationInfo dis = new DisseminationInfo();
		dis = (DisseminationInfo) iStream.readObject();
		return dis;
	}
}
