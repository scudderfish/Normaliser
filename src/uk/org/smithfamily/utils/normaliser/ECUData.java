package uk.org.smithfamily.utils.normaliser;

import uk.org.smithfamily.mslogger.ecuDef.Constant;
import uk.org.smithfamily.mslogger.ecuDef.OutputChannel;
import uk.org.smithfamily.mslogger.ecuDef.SettingGroup;
import uk.org.smithfamily.utils.normaliser.controllercommand.ControllerCommand;

import java.util.*;

public class ECUData
{
	private Set<String> initalisedConstants = new HashSet<>();
	private List<String> runtime = new ArrayList<>();
	private List<String> logHeader = new ArrayList<>();
	private List<String> logRecord = new ArrayList<>();
	private List<String> gaugeDef = new ArrayList<>();
	private Map<String, String> fieldControlExpressions;
	private Map<String, String> menuControlExpressions;
	private Map<String, String> runtimeVars;
	private Map<String, String> evalVars;
	private Map<String, String> constantVars;
	private List<String> defaults;
	private List<String> requiresPowerCycle;
	private Set<String> flags;
	private String fingerprintSource;
	private ArrayList<String> gaugeDoc;
	private ArrayList<Constant> constants;
	private ArrayList<OutputChannel> outputChannels;
	private ArrayList<String> pageSizes;
	private ArrayList<String> pageIdentifiers;
	private ArrayList<String> pageActivateCommands;
	private ArrayList<String> pageReadCommands;
	private String signatureDeclaration;
	private String queryCommandStr;
	private String ochGetCommandStr;
	private String ochBlockSizeStr;
	private ArrayList<String> defaultGauges;
	private boolean isCRC32Protocol;
	private int currentPage = 0;
	private int interWriteDelay;
	private int pageActivationDelayVal;
	private String classSignature;
	private ArrayList<SettingGroup> settingGroups;
	private List<String> pageValueWrites;
	private List<String> pageChunkWrites;
	private List<ControllerCommand> controllerCommands;

	public List<String> getRuntime()
	{
		return runtime;
	}

	public void setRuntime(List<String> runtime)
	{
		this.runtime = runtime;
	}

	public List<String> getLogHeader()
	{
		return logHeader;
	}

	public List<String> getLogRecord()
	{
		return logRecord;
	}

	public List<String> getGaugeDef()
	{
		return gaugeDef;
	}

	public void reset()
	{
		runtime = new ArrayList<>();
		logHeader = new ArrayList<>();
		logRecord = new ArrayList<>();
		fieldControlExpressions = new HashMap<>();
		menuControlExpressions = new HashMap<>();
		runtimeVars = new HashMap<>();
		evalVars = new HashMap<>();
		constantVars = new HashMap<>();
		defaults = new ArrayList<>();
		requiresPowerCycle = new ArrayList<>();
		constants = new ArrayList<>();
		outputChannels = new ArrayList<>();
		flags = new HashSet<>();
		gaugeDef = new ArrayList<>();
		gaugeDoc = new ArrayList<>();
		defaultGauges = new ArrayList<>();
		pageActivateCommands = new ArrayList<>();
		pageIdentifiers = new ArrayList<>();
		settingGroups = new ArrayList<>();
		controllerCommands=new ArrayList<>();
		pageValueWrites=new ArrayList<>();
		pageChunkWrites=new ArrayList<>();
		fingerprintSource = "";
		currentPage = 0;
		isCRC32Protocol = false;
		// Default for those who don't define it. I'm looking at you
		// megasquirt-I.ini!
		ochGetCommandStr = "byte [] ochGetCommand = new byte[]{'A'};";
		evalVars.put("veTuneValue", "int");
	}

	public Map<String, String> getRuntimeVars()
	{
		return runtimeVars;
	}

	public Map<String, String> getEvalVars()
	{
		return evalVars;
	}

	public Map<String, String> getConstantVars()
	{
		return constantVars;
	}

	public List<String> getDefaults()
	{
		return defaults;
	}

	public List<String> getRequiresPowerCycle()
	{
	    return requiresPowerCycle;
	}

	public Set<String> getFlags()
	{
		return flags;
	}

	public String getFingerprintSource()
	{
		return fingerprintSource;
	}

	public void setFingerprintSource(String fingerprintSource)
	{
		this.fingerprintSource = fingerprintSource;
	}

	public ArrayList<String> getGaugeDoc()
	{
		return gaugeDoc;
	}

	public ArrayList<Constant> getConstants()
	{
		return constants;
	}
	
	public ArrayList<OutputChannel> getOutputChannels()
	{
	    return outputChannels;
	}

	public void setConstants(ArrayList<Constant> constants)
	{
		this.constants = constants;
	}

	public ArrayList<String> getPageSizes()
	{
		return pageSizes;
	}

	public void setPageSizes(ArrayList<String> pageSizes)
	{
		this.pageSizes = pageSizes;
	}

	public ArrayList<String> getPageIdentifiers()
	{
		return pageIdentifiers;
	}

	public void setPageIdentifiers(ArrayList<String> pageIdentifiers)
	{
		this.pageIdentifiers = pageIdentifiers;
	}

	public ArrayList<String> getPageActivateCommands()
	{
		return pageActivateCommands;
	}

	public void setPageActivateCommands(ArrayList<String> pageActivateCommands)
	{
		this.pageActivateCommands = pageActivateCommands;
	}

	public ArrayList<String> getPageReadCommands()
	{
		return pageReadCommands;
	}

	public void setPageReadCommands(ArrayList<String> pageReadCommands)
	{
		this.pageReadCommands = pageReadCommands;
	}

	public String getSignatureDeclaration()
	{
		return signatureDeclaration;
	}

	public void setSignatureDeclaration(String signatureDeclaration)
	{
		this.signatureDeclaration = signatureDeclaration;
	}

	public String getQueryCommandStr()
	{
		return queryCommandStr;
	}

	public void setQueryCommandStr(String queryCommandStr)
	{
		this.queryCommandStr = queryCommandStr;
	}

	public String getOchGetCommandStr()
	{
		return ochGetCommandStr;
	}

	public void setOchGetCommandStr(String ochGetCommandStr)
	{
		this.ochGetCommandStr = ochGetCommandStr;
	}

	public String getOchBlockSizeStr()
	{
		return ochBlockSizeStr;
	}

	public void setOchBlockSizeStr(String ochBlockSizeStr)
	{
		this.ochBlockSizeStr = ochBlockSizeStr;
	}

	public ArrayList<String> getDefaultGauges()
	{
		return defaultGauges;
	}

	public boolean isCRC32Protocol()
	{
		return isCRC32Protocol;
	}

	public void setCRC32Protocol(boolean isCRC32Protocol)
	{
		this.isCRC32Protocol = isCRC32Protocol;
	}

	public int getCurrentPage()
	{
		return currentPage;
	}

	public void setCurrentPage(int currentPage)
	{
		this.currentPage = currentPage;
	}

	public int getInterWriteDelay()
	{
		return interWriteDelay;
	}

	public void setInterWriteDelay(int interWriteDelay)
	{
		this.interWriteDelay = interWriteDelay;
	}

	public int getPageActivationDelayVal()
	{
		return pageActivationDelayVal;
	}

	public void setPageActivationDelayVal(int pageActivationDelayVal)
	{
		this.pageActivationDelayVal = pageActivationDelayVal;
	}

	public String getClassSignature()
	{
		return classSignature;
	}

	public void setClassSignature(String classSignature)
	{
		this.classSignature = classSignature;
	}

	public Map<String, String> getFieldControlExpressions()
    {
        return fieldControlExpressions;
    }
    
    public Map<String, String> getMenuControlExpressions()
    {
        return menuControlExpressions;
    }

    public ArrayList<SettingGroup> getSettingGroups()
    {
        return settingGroups;
    }

	public Set<String> getInitalisedConstants() {
		return initalisedConstants;
	}


    public List<String> getPageValueWrites() {
        return pageValueWrites;
    }


	public List<String> getPageChunkWrites() {
		return pageChunkWrites;
	}

	public List<ControllerCommand> getControllerCommands() {
		return controllerCommands;
	}

	public void setControllerCommands(List<ControllerCommand> controllerCommands) {
		this.controllerCommands = controllerCommands;
	}
}
