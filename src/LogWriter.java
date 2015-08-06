import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogWriter
{
	private static String filename;
	private static BufferedWriter logger = null;
	private static char myNodeName;

	public static char getMyNodeName()
	{
		return myNodeName;
	}

	public static void setMyNodeName(char myNodeName)
	{
		LogWriter.myNodeName = myNodeName;
	}

	public LogWriter()
	{
	}

	public static void Log(String log) throws IOException
	{
		logger.write(String.format("%s\n", log));
		logger.flush();
	}
	
	public static void Close() throws IOException
	{
		if(logger != null)
			logger.close();
	}

	public static void Initialise() throws IOException
	{
		filename = getMyNodeName() + "_" + Constants.LOG_FILE;
		logger = new BufferedWriter(new FileWriter(filename));
	}
}
