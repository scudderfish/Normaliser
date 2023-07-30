package uk.org.smithfamily.mslogger.ecuDef;

import java.io.IOException;
import java.util.*;

public interface MSECUInterface
{
    Map<String, Constant> constants = new HashMap<>();

    Map<String, List<Menu>> menus = new HashMap<>();

    Map<String, MSDialog> dialogs = new HashMap<>();

    Map<String, Boolean> userDefinedVisibilityFlags = new HashMap<>();

    Map<String, Boolean> menuVisibilityFlags = new HashMap<>();

    Map<String, OutputChannel> outputChannels = new HashMap<>();

    List<SettingGroup> settingGroups = new ArrayList<>();

    Map<String, String> controllerCommands = new HashMap<>();

    void setFlags();

    String getSignature();

    byte[] getOchCommand();

    byte[] getSigCommand();

    void loadConstants(boolean simulated) throws IOException;

    void calculate(byte[] ochBuffer);

    String getLogHeader();

    String getLogRow();

    int getBlockSize();

    int getSigSize();

    int getPageActivationDelay();

    List<String> getPageIdentifiers();

    List<byte[]> getPageActivates();

    List<String> getPageValueWrites();

    List<String> getPageChunkWrites();

    int getInterWriteDelay();

    int getCurrentTPS();

    void refreshFlags();

    boolean isCRC32Protocol();

    void createTableEditors();

    void createCurveEditors();

    void createMenus();

    void createDialogs();

    void setUserDefinedVisibilityFlags();

    void setMenuVisibilityFlags();

    String[] getControlFlags();

    void createSettingGroups();

    List<SettingGroup> getSettingGroups();

    List<String> getRequiresPowerCycle();

    void createControllerCommands();

    Map<String, String> getControllerCommands();

    void initGauges();

    String[] defaultGauges();

}
