import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class Normaliser
{
	private static Pattern				bits		= Pattern
															.compile("(\\w*)\\s*=\\s*bits,\\s*(.*),\\s*(.*),\\s*\\[(\\d):(\\d)\\].*");
	private static Pattern				scalar		= Pattern.compile("(\\w*)\\s*=\\s*scalar,\\s*([U|S]\\d+),\\s*(\\d+),\\s*.*,.*");
	private static Pattern				expr		= Pattern.compile("(\\w*)\\s*=\\s*\\{\\s*(.*)\\s*\\}.*");
	private static Pattern				ternary		= Pattern.compile("(.*)\\?(.*)");
	private static Pattern				log			= Pattern.compile("\\s*entry\\s*=\\s*(\\w+)\\s*,\\s*\"(.*)\",.*");
	private static Pattern				binary		= Pattern.compile("(.*)0b([01]{8})(.*)");
	private static List<String>			runtime		= new ArrayList<String>();
	private static List<String>			logHeader	= new ArrayList<String>();
	private static List<String>			logRecord	= new ArrayList<String>();
	private static Map<String, String>	runtimeVars;
	private static Map<String, String>	evalVars;
	private static Set<String>			flags;

	/**
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws IOException
	{
		for (String filename : args)
		{
			process(filename);
		}
	}

	private static void process(String filename) throws IOException
	{
		runtime = new ArrayList<String>();
		logHeader = new ArrayList<String>();
		logRecord = new ArrayList<String>();
		runtimeVars = new HashMap<String, String>();
		evalVars = new HashMap<String, String>();
		flags = new HashSet<String>();

		boolean processingExpr = false;
		boolean processingLogs = false;
		BufferedReader br = new BufferedReader(new FileReader(filename));

		String line;

		while ((line = br.readLine()) != null)
		{
			if (line.trim().equals("[OutputChannels]"))
			{
				processingExpr = true;
				continue;
			}
			if (line.trim().equals("[Datalog]"))
			{
				processingExpr = false;
				processingLogs = true;
				continue;
			}
			if (processingExpr)
			{
				processExpr(line);
			}
			if (processingLogs)
			{
				processLogEntry(line);
			}
		}
		writeFile(filename);
	}

	private static void writeFile(String filename) throws IOException
	{
		PrintWriter writer = new PrintWriter(new FileWriter(filename + ".jd"));

		writer.println("public class ECU extends Megasquirt\n{");
		writer.println("//Flags");
		for(String name : flags)
		{
			writer.println("boolean "+name+";");
		}
		writer.println("//Runtime vars");
		for (String name : runtimeVars.keySet())
		{
			writer.println(runtimeVars.get(name) + " " + name + ";");
		}
		writer.println("\n//eval vars");
		for (String name : evalVars.keySet())
		{
			writer.println(evalVars.get(name) + " " + name + ";");
		}
		writer.println("\n");
		writer.println("	@Override");
		writer.println("	public void calculate(byte[] ochBuffer) throws IOException");
		writer.println("{");
		for (String defn : runtime)
		{
			writer.println(defn);
			// System.out.println(defn);
		}
		writer.println("}");
		writer.println("@Override");
		writer.println("public String getLogHeader()");
		writer.println("{");
		writer.println("	StringBuffer b = new StringBuffer();");
		for (String header : logHeader)
		{
			writer.println(header);
		}
		writer.println("    return b.toString();\n}\n");
		writer.println("@Override");
		writer.println("public String getLogRow()");
		writer.println("{");
		writer.println("	StringBuffer b = new StringBuffer();");

		for (String record : logRecord)
		{
			writer.println(record);
		}
		writer.println("    return b.toString();\n}\n");
		writer.println("\n}");
		writer.close();
	}

	private static void processLogEntry(String line)
	{
		Matcher logM = log.matcher(line);
		if (logM.matches())
		{
			logHeader.add("b.append(\"" + logM.group(2) + "\").append(\"\\t\");");
			logRecord.add("b.append(" + logM.group(1) + ").append(\"\\t\");");
		}
		else if (line.startsWith("#"))
		{
			String directive = processPreprocessor(line);
			logHeader.add(directive);
			logRecord.add(directive);
		}
	}

	private static void processExpr(String line)
	{
		String definition = null;
		line = StringUtils.trim(line).split(";")[0];
		if (StringUtils.isEmpty(line))
		{
			return;
		}
		Matcher bitsM = bits.matcher(line);
		Matcher scalarM = scalar.matcher(line);
		Matcher exprM = expr.matcher(line);
		if (bitsM.matches())
		{
			String name = bitsM.group(1);
			String offset = bitsM.group(3);
			String start = bitsM.group(4);
			String end = bitsM.group(5);
			definition = (name + " = getBits(ochBuffer," + offset + "," + start + "," + end + ");");
			runtime.add(definition);
			runtimeVars.put(name, "int");
		}
		else if (scalarM.matches())
		{
			String name = scalarM.group(1);
			String dataType = scalarM.group(2);
			String offset = scalarM.group(3);
			definition = getScalar(name, dataType, offset);
			runtime.add(definition);
			runtimeVars.put(name, "int");
		}
		else if (exprM.matches())
		{
			String name = exprM.group(1);
			String expression = deBinary(exprM.group(2).trim());
			Matcher ternaryM = ternary.matcher(expression);
			if (ternaryM.matches())
			{
				System.out.println("BEFORE : " + expression);
				expression = "((" + ternaryM.group(1) + ") != 0 ) ? " + ternaryM.group(2);
				System.out.println("AFTER  : " + expression + "\n");
			}
			definition = name + " = (" + expression + ");";
			runtime.add(definition);
			if (isFloatingExpression(expression))
			{
				evalVars.put(name, "double");
			}
			else
			{
				evalVars.put(name, "int");
			}
		}
		else if (line.startsWith("#"))
		{
			runtime.add(processPreprocessor(line));
		}
	}

	private static String deBinary(String group)
	{
		Matcher binNumber = binary.matcher(group);
		if (!binNumber.matches())
		{
			return group;
		}
		else
		{
			String binNum = binNumber.group(2);
			int num = Integer.parseInt(binNum, 2);
			String expr = binNumber.group(1) + num + binNumber.group(3);
			return deBinary(expr);
		}
	}

	private static boolean isFloatingExpression(String expression)
	{
		return expression.contains(".");
	}

	private static String processPreprocessor(String line)
	{
		String[] components = line.split(" ");
		if (components[0].equals("#if"))
		{
			flags.add(components[1]);
			return ("if (" + components[1] + ")\n{");
		}
		if (components[0].equals("#elif"))
		{
			return ("\n}\nelse if (" + components[1] + ")\n{");
		}
		if (components[0].equals("#else"))
		{
			return ("\n}\nelse\n{");
		}
		if (components[0].equals("#endif"))
		{
			return ("\n}\n");
		}
		return line;
	}

	private static String getScalar(String name, String dataType, String offset)
	{
		String definition = name + " = MSUtils.get";
		if (dataType.startsWith("S"))
		{
			definition += "Signed";
		}
		int size = Integer.parseInt(dataType.substring(1));
		switch (size)
		{
		case 8:
			definition += "Byte";
			break;
		case 16:
			definition += "Word";
			break;
		case 32:
			definition += "Long";
			break;
		default:
			definition += dataType;
			break;
		}
		definition += "(ochBuffer," + offset + ");";
		return definition;
	}

}
