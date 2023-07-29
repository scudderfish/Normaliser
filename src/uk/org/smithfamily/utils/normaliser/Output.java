package uk.org.smithfamily.utils.normaliser;

import org.apache.commons.lang3.StringUtils;
import uk.org.smithfamily.mslogger.ecuDef.Constant;
import uk.org.smithfamily.mslogger.ecuDef.OutputChannel;
import uk.org.smithfamily.mslogger.ecuDef.SettingGroup;

import java.io.PrintWriter;
import java.util.*;

public class Output {
    static final String TAB = "    ";
    private static final int MAX_LINES = 100;
    private static final Set<String> alwaysInt = new HashSet<>(List.of());
    private static final Set<String>
            alwaysDouble = new HashSet<>(Arrays.asList("pulseWidth", "throttle",
            "accDecEnrich", "accDecEnrichPcnt", "accEnrichPcnt", "accEnrichMS", "decEnrichPcnt", "decEnrichMS", "time",
            "egoVoltage", "egoVoltage2", "egoCorrection", "veCurr", "lambda", "TargetLambda"));

    @SuppressWarnings("unused")
    static void outputGaugeDoc(ECUData ecuData, PrintWriter writer) {
        writer.println("/*");
        for (String gauge : ecuData.getGaugeDoc()) {
            writer.println(gauge);
        }

        writer.println("*/");
    }

    static void outputConstructor(PrintWriter writer, String className) {
        writer.println(TAB + "private final MSControllerInterface parent;");
        writer.println(TAB + "private final MSUtilsInterface utils;");
        writer.println(TAB + "private final GaugeRegisterInterface gauges;");
        writer.println(TAB + "public " + className + "(MSControllerInterface parent,MSUtilsInterface utils,GaugeRegisterInterface gauges)");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "this.parent = parent;");
        writer.println(TAB + TAB + "this.utils  = utils;");
        writer.println(TAB + TAB + "this.gauges = gauges;");
        writer.println(TAB + TAB + "this.parent.setImplementation(this);");
        writer.println(TAB + TAB + "setFlags();");
        writer.println(TAB + TAB + "setDefaultValues();");
        writer.println(TAB + "}");
        writer.println(TAB + "private double table(double x,String t)");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "return parent.table(x,t);");
        writer.println(TAB + "}");
        writer.println(TAB + "private double round(double x)");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "return parent.round(x);");
        writer.println(TAB + "}");
        writer.println(TAB + "private double tempCvt(double x)");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "return parent.tempCvt(x);");
        writer.println(TAB + "}");
        writer.println(TAB + "private int timeNow()");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "return parent.timeNow();");
        writer.println(TAB + "}");
    }

    /**
     * This is nasty. We need to have a set of methods to init the constants as there is a hard limit of 64K on the
     * size of a method, and MS3 will break that. We also need to ensure that if there is any preprocessor type logic
     * that we don't stop and start the next method in the middle of it.
     */
    static void outputFlagsAndConstants(ECUData ecuData, PrintWriter writer) {
        int constantMethodCount = 0;
        int lineCount = 0;
        int bracketNesting = 0;
        boolean needDeclaration = true;
        int lookahead = 3;
        for (Constant c : ecuData.getConstants()) {
            if (needDeclaration) {
                constantMethodCount++;
                writer.println(TAB + "private void initConstants" + constantMethodCount + "()\n" + TAB + "{\n");
                needDeclaration = false;
            }
            if (bracketNesting == 0 && lookahead > 0) {
                lookahead--;
            }
            lineCount++;

            if (c.getName().contains("{")) {
                bracketNesting++;
            }
            if (c.getName().contains("}")) {
                bracketNesting--;
            }

            if ("PREPROC".equals(c.getType())) {
                writer.println(TAB + TAB + c.getName());
                lookahead = 3;
            } else {
                writer.println(TAB + TAB + "constants.put(\"" + c.getName() + "\", new " + c + ");");
            }

            if (lineCount > MAX_LINES && bracketNesting == 0 && lookahead == 0) {
                writer.println(TAB + "}\n");
                needDeclaration = true;
                lineCount = 0;
            }
        }
        if (!needDeclaration) {
            writer.println(TAB + "}\n");
        }

        writer.println(TAB + "@Override");
        writer.println(TAB + "public void setFlags()");
        writer.println(TAB + "{");

        Map<String, String> vars = ecuData.getConstantVars();

        for (String flag : ecuData.getFlags()) {
            // INI_VERSION_2 should always be true
            switch (flag) {
                case "INI_VERSION_2" -> writer.println(TAB + TAB + "INI_VERSION_2 = true;");

                // MEMPAGES, LOGPAGES and MSLVV_COMPATIBLE should always be false
                case "MEMPAGES", "LOGPAGES", "MSLVV_COMPATIBLE" -> writer.println(TAB + TAB + flag + " = false;");
                case "SPEED_DENSITY" -> {
                    String varName = "algorithm1";

                    // MS1 B&G
                    if (vars.containsKey("algorithm")) {
                        varName = "algorithm";
                    }

                    writer.println(TAB + TAB + "SPEED_DENSITY = (" + varName + " == 1);");
                }
                case "ALPHA_N" -> {
                    String varName = "algorithm1";

                    // MS1 B&G
                    if (vars.containsKey("algorithm")) {
                        varName = "algorithm";
                    }

                    writer.println(TAB + TAB + "ALPHA_N = (" + varName + " == 2);");
                }
                case "AIR_FLOW_METER" -> {
                    if (vars.containsKey("AFMUse")) {
                        writer.println(TAB + TAB + "AIR_FLOW_METER = (AFMUse == 2);");
                    }
                    // MS1 B&G doesn't support MAF
                    else {
                        writer.println(TAB + TAB + "AIR_FLOW_METER = false;");
                    }
                }
                default -> writer.println(TAB + TAB + flag + " = parent.isSet(\"" + flag + "\");");
            }
        }
        writer.println(TAB + "}");

        writer.println(TAB + "@Override");
        writer.println(TAB + "public void refreshFlags()");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "setFlags();");
        writer.println(TAB + TAB + "initOutputChannels();");
        for (int i = 1; i <= constantMethodCount; i++) {
            writer.println(TAB + TAB + "initConstants" + i + "();");
        }
        writer.println(TAB + "}");

    }

    static void outputOutputChannels(ECUData ecuData, PrintWriter writer) {
        writer.println(TAB + "public void initOutputChannels()");
        writer.println(TAB + "{");

        for (OutputChannel op : ecuData.getOutputChannels()) {
            if (op.getType().equals("PREPROC")) {
                writer.println(TAB + TAB + op.getName());
            } else {
                writer.println(TAB + TAB + "outputChannels.put(\"" + op.getName() + "\", new " + op + ");");
            }
        }

        writer.println(TAB + "}");
    }

    static void outputPackageAndIncludes(PrintWriter writer) {
        writer.println("package uk.org.smithfamily.mslogger.ecuDef.gen;");
        writer.println("");
        writer.println("import java.io.IOException;");
        writer.println("import java.util.*;");
        writer.println("");
        writer.println("");
        writer.println("import uk.org.smithfamily.mslogger.ecuDef.*;");
        writer.println("import uk.org.smithfamily.mslogger.widgets.GaugeDetails;");
        writer.println("import uk.org.smithfamily.mslogger.widgets.GaugeRegisterInterface;");

    }

    private static String getType(String name, Map<String, String> vars) {
        String type = vars.get(name);
        if (alwaysInt.contains(name)) {
            type = "int";
        } else if (alwaysDouble.contains(name)) {
            type = "double";
        }
        return type;
    }

    static void outputGlobalVars(ECUData ecuData, PrintWriter writer) {
        writer.println("//Flags");

        List<String> flags = new ArrayList<>();
        for (String flag : ecuData.getFlags()) {
            if (!flag.equals("INI_VERSION_2") && !flag.equals("MEMPAGES") && !flag.equals("LOGPAGES")
                    && !flag.equals("SPEED_DENSITY") && !flag.equals("ALPHA_N") && !flag.equals("MSLVV_COMPATIBLE")
                    && !flag.equals("AIR_FLOW_METER")) {
                flags.add("\"" + flag + "\"");
            }
        }

        writer.println(TAB + "public String[] flags = {" + StringUtils.join(flags, ",") + "};");
        for (String name : ecuData.getFlags()) {
            writer.println(TAB + "public boolean " + name + ";");
        }
        writer.println("//Defaults");
        for (String d : ecuData.getDefaults()) {
            writer.println(TAB + "public " + d);
        }
        Map<String, String> vars = new TreeMap<>();
        vars.putAll(ecuData.getRuntimeVars());
        vars.putAll(ecuData.getEvalVars());
        for (String v : vars.keySet()) {
            ecuData.getConstantVars().remove(v);
        }
        writer.println("//Variables");
        for (String name : vars.keySet()) {
            String type = getType(name, vars);
            writer.println(TAB + "public " + type + " " + name + ";");
        }
        writer.println("\n//Constants");
        for (String name : ecuData.getConstantVars().keySet()) {
            if (!ecuData.getInitalisedConstants().contains(name.trim())) {
                String type = getType(name, ecuData.getConstantVars());
                writer.println(TAB + "public " + type + " " + name + ";");
            }
        }
        writer.println("\n");
        writer.println(TAB + "private String[] defaultGauges = {");
        boolean first = true;
        for (String dg : ecuData.getDefaultGauges()) {
            if (!first)
                writer.println(",");
            first = false;
            writer.print(TAB + TAB + "\"" + dg + "\"");
        }
        writer.println("\n" + TAB + "};");
        writer.println("\n@Override");
        writer.println(TAB + "public String[] getControlFlags()\n");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "return flags;");
        writer.println(TAB + "}");


    }

    static void outputRequiresPowerCycle(ECUData ecuData, PrintWriter writer) {
        writer.println("\n    //Fields that requires power cycle");

        writer.println("    public List<String> getRequiresPowerCycle()");
        writer.println("    {");
        writer.println(TAB + TAB + "List<String> requiresPowerCycle = new ArrayList<String>();");

        for (String field : ecuData.getRequiresPowerCycle()) {
            writer.println(TAB + TAB + "requiresPowerCycle.add(\"" + field + "\");");
        }

        writer.println(TAB + TAB + "return requiresPowerCycle;");
        writer.println(TAB + "}\n");
    }

    static void outputRTCalcs(ECUData ecuData, PrintWriter writer) {
        writer.println("    @Override");
        writer.println("    public void calculate(byte[] ochBuffer)");
        writer.println("    {");
        for (String defn : ecuData.getRuntime()) {
            writer.println(TAB + TAB + defn);
            // System.out.println(defn);
        }
        writer.println(TAB + "}");
    }

    static void outputLogInfo(ECUData ecuData, PrintWriter writer) {
        writer.println(TAB + "@Override");
        writer.println(TAB + "public String getLogHeader()");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "StringBuffer b = new StringBuffer();");
        for (String header : ecuData.getLogHeader()) {
            writer.println(TAB + TAB + header);
        }
        writer.println(TAB + TAB + "b.append(utils.getLocationLogHeader());");
        writer.println(TAB + TAB + "return b.toString();\n" + TAB + "}\n");
        writer.println(TAB + "@Override");
        writer.println(TAB + "public String getLogRow()");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "StringBuffer b = new StringBuffer();");

        for (String record : ecuData.getLogRecord()) {
            writer.println(TAB + TAB + record);
        }
        writer.println(TAB + TAB + "b.append(utils.getLocationLogRow());");
        writer.println(TAB + TAB + "return b.toString();\n" + TAB + "}\n");
    }

    static void outputGauges(ECUData ecuData, PrintWriter writer) {
        writer.println(TAB + "@Override");
        writer.println(TAB + "public void initGauges()");
        writer.println(TAB + "{");
        for (String gauge : ecuData.getGaugeDef()) {
            boolean okToWrite = true;
            if (gauge.contains("gauges.")) {
                String[] parts = gauge.split(",");
                String channel = parts[4];
                okToWrite = ecuData.getRuntimeVars().containsKey(channel) || ecuData.getEvalVars().containsKey(channel);
            }
            if (okToWrite) {
                writer.println(TAB + TAB + gauge);
            }
        }
        writer.println(TAB + "}\n");
    }


    static void outputLoadConstants(ECUData ecuData, PrintWriter writer) {
        int pageNo = 0;

        List<Integer> pageNumbers = new ArrayList<>();

        for (Constant c : ecuData.getConstants()) {
            if (c.getPage() != pageNo) {
                if (pageNo > 0) {
                    writer.println(TAB + "}");
                }
                pageNo = c.getPage();
                pageNumbers.add(pageNo);
                writer.println(TAB + "public void loadConstantsPage" + pageNo + "(boolean simulated)");
                writer.println(TAB + "{");
                writer.println(TAB + TAB + "byte[] pageBuffer = null;");

                int pageSize = Integer.parseInt(ecuData.getPageSizes().get(pageNo - 1).trim());
                String activateCommand = null;
                if (pageNo - 1 < ecuData.getPageActivateCommands().size()) {
                    activateCommand = ecuData.getPageActivateCommands().get(pageNo - 1);
                }
                String readCommand = null;
                if (pageNo - 1 < ecuData.getPageReadCommands().size()) {
                    readCommand = ecuData.getPageReadCommands().get(pageNo - 1);
                }

                outputLoadPage(ecuData, pageNo, pageSize, activateCommand, readCommand, writer);
            }
            // getScalar(String bufferName,String name, String dataType, String
            // offset, String scale, String numOffset)
            String name = c.getName();

            if (!"PREPROC".equals(c.getType())) {
                String def;
                if ("bits".equals(c.getClassType())) {
                    String bitspec = StringUtils.remove(StringUtils.remove(c.getShape(), '['), ']');
                    String[] bits = bitspec.split(":");
                    int offset = c.getOffset();
                    String start = bits[0];
                    String end = bits[1];
                    String ofs = "0";
                    if (end.contains("+")) {
                        String[] parts = end.split("\\+");
                        end = parts[0];
                        ofs = parts[1];
                    }
                    def = (name + " = utils.getBits(pageBuffer," + offset + "," + start + "," + end + "," + ofs + ");");
                } else if ("array".equals(c.getClassType())) {
                    def = generateLoadArray(c);
                } else {
                    def = getScalar("pageBuffer", ecuData.getConstantVars().get(name), name, c.getType(), String.valueOf(c.getOffset()), String.valueOf(c.getScale()), String.valueOf(c.getTranslate()));
                }
                writer.println(TAB + TAB + def);
            } else {
                if (pageNo > 0) {
                    writer.println(TAB + TAB + name);
                }
            }

        }
        writer.println(TAB + "}");
        writer.println(TAB + "@Override");
        writer.println(TAB + "public void loadConstants(boolean simulated)");
        writer.println(TAB + "{");
        for (int i : pageNumbers) {
            writer.println(TAB + TAB + "loadConstantsPage" + i + "(simulated);");
        }
        writer.println(TAB + TAB + "refreshFlags();");
        writer.println(TAB + "}");
    }

    private static String generateLoadArray(Constant c) {
        String loadArray;
        String arraySpec = StringUtils.remove(StringUtils.remove(c.getShape(), '['), ']');
        String[] sizes = arraySpec.split("x");
        int width = Integer.parseInt(sizes[0].trim());
        int height = sizes.length == 2 ? Integer.parseInt(sizes[1].trim()) : -1;
        String functionName = "parent.loadByte";
        String signed = "false";
        if (c.getType().contains("16")) {
            functionName = "parent.loadWord";
        }
        if (c.getType().contains("S")) {
            signed = "true";
        }
        if (height == -1) {
            functionName += "Vector";
            loadArray = String.format("%s = %s(pageBuffer, %d, %d, %s, %s, %s, %s);", c.getName(), functionName, c.getOffset(),
                    width, c.getScale(), c.getTranslate(), c.getDigits(), signed);

        } else {
            functionName += "Array";
            loadArray = String.format("%s = %s(pageBuffer, %d, %d, %d, %s, %s, %s, %s);", c.getName(), functionName, c.getOffset(),
                    width, height, c.getScale(), c.getTranslate(), c.getDigits(), signed);
        }
        return loadArray;
    }

    static void outputLoadPage(ECUData ecuData, int pageNo, int pageSize, String activate, String read,
                               PrintWriter writer) {
        if (activate != null) {

            activate = processStringToBytes(ecuData, activate, pageSize, pageNo);
        }
        if (read != null) {
            read = processStringToBytes(ecuData, read, pageSize, pageNo);
        }
        writer.println(TAB + TAB
                + String.format("pageBuffer = parent.loadPage(%d,%d,%d,%s,%s);", pageNo, 0, pageSize, activate, read));

    }

    static void outputOverrides(ECUData ecuData, PrintWriter writer) {
        String overrides = TAB + "@Override\n" + TAB + "public String getSignature()\n" + TAB + "{\n" + TAB + TAB
                + "return signature;\n" + "}\n" + TAB + "@Override\n" + TAB + "public byte[] getOchCommand()\n" + TAB + "{\n" + TAB
                + TAB + "return this.ochGetCommand;\n" + TAB + "}\n" +

                TAB + "@Override\n" + TAB + "public byte[] getSigCommand()\n" + TAB + "{\n" + TAB + TAB
                + "return this.queryCommand;\n" + TAB + "}\n" +

                TAB + "@Override\n" + TAB + "public int getBlockSize()\n" + TAB + "{\n" + TAB + TAB + "return this.ochBlockSize;\n"
                + TAB + "}\n" +

                TAB + "@Override\n" + TAB + "public int getSigSize()\n" + TAB + "{\n" + TAB + TAB + "return signature.length();\n"
                + TAB + "}\n" +

                TAB + "@Override\n" + TAB + "public int getPageActivationDelay()\n" + TAB + "{\n" + TAB + TAB + "return "
                + ecuData.getPageActivationDelayVal() + ";\n" + TAB + "}\n" +

                TAB + "@Override\n" + TAB + "public int getInterWriteDelay()\n" + TAB + "{\n" + TAB + TAB + "return "
                + ecuData.getInterWriteDelay() + ";\n" + TAB + "}\n" + TAB + "@Override\n" + TAB
                + "public boolean isCRC32Protocol()\n" + TAB + "{\n" + TAB + TAB + "return " + ecuData.isCRC32Protocol() + ";\n"
                + TAB + "}\n" +

                TAB + "@Override\n" + TAB + "public int getCurrentTPS()\n" + TAB + "{\n";
        if (ecuData.getRuntimeVars().containsKey("tpsADC")) {
            overrides += TAB + TAB + "return (int)tpsADC;\n";
        } else {
            overrides += TAB + TAB + "return 0;\n";
        }

        overrides += TAB + "}\n" +

                TAB + "@Override\n" + TAB + "public String[] defaultGauges()\n" + TAB + "{\n" + TAB + TAB + "return defaultGauges;\n" + TAB
                + "}\n";

        writer.println(overrides);
    }

    private static String processStringToBytes(ECUData ecuData, String s, int count, int pageNo) {
        String ret = "new byte[]{";

        ret += HexStringToBytes(ecuData, s, 0, count, pageNo);

        ret += "}";
        return ret;
    }

    private static String bytes(int val) {
        int hi = val / 256;
        int low = val % 256;
        if (hi > 127)
            hi -= 256;
        if (low > 127)
            low -= 256;
        return hi + "," + low;
    }

    static String HexStringToBytes(ECUData ecuData, String s, int offset, int count, int pageNo) {
        StringBuilder ret = new StringBuilder();
        boolean first = true;
        s = s.replace("$tsCanId", "x00");
        for (int p = 0; p < s.length(); p++) {
            if (!first)
                ret.append(",");

            char c = s.charAt(p);
            switch (c) {
                case '\\' -> {
                    ret.append(HexByteToDec(s.substring(p)));
                    p = p + 3;
                }
                case '%' -> {
                    p++;
                    c = s.charAt(p);
                    assert c == '2';
                    p++;
                    c = s.charAt(p);
                    if (c == 'o') {
                        ret.append(bytes(offset));
                    } else if (c == 'c') {
                        ret.append(bytes(count));
                    } else if (c == 'i') {
                        String identifier = ecuData.getPageIdentifiers().get(pageNo - 1);

                        ret.append(HexStringToBytes(ecuData, identifier, offset, count, pageNo));
                    }
                }
                default -> ret.append((byte) c);
            }
            first = false;
        }
        return ret.toString();
    }

    private static int HexByteToDec(String s) {
        String digits = "0123456789abcdef";
        int i = 0;
        char c = s.charAt(i++);
        assert c == '\\';
        c = s.charAt(i++);
        assert c == 'x';
        c = s.charAt(i++);
        c = Character.toLowerCase(c);
        int val;
        int digit = digits.indexOf(c);
        val = digit * 16;
        //noinspection ReassignedVariable
        c = s.charAt(i);
        c = Character.toLowerCase(c);
        digit = digits.indexOf(c);
        val = val + digit;
        return val;

    }

    static String getScalar(String bufferName, String javaType, String name, String dataType, String offset, String scale,
                            String numOffset) {
        if (javaType == null) {
            javaType = "int";
        }
        String definition = name + " = (" + javaType + ")((utils.get";
        if (dataType.startsWith("S")) {
            definition += "Signed";
        }
        int size = Integer.parseInt(dataType.substring(1).trim());
        switch (size) {
            case 8 -> definition += "Byte";
            case 16 -> definition += "Word";
            case 32 -> definition += "Long";
            default -> definition += dataType;
        }
        definition += "(" + bufferName + "," + offset + ") + " + numOffset + ") * " + scale + ");";
        return definition;
    }

    public static void outputSettingGroups(ECUData ecuData, PrintWriter writer) {
        writer.println(TAB + "@Override");
        writer.println(TAB + "public void createSettingGroups()");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "settingGroups.clear();");
        writer.println(TAB + TAB + "SettingGroup g;");

        for (SettingGroup group : ecuData.getSettingGroups()) {
            String desc = group.getDescription();
            if (desc.trim().length() > 1) {
                writer.println(TAB + TAB + String.format("g = new SettingGroup(\"%s\",\"%s\");", group.getName(), group.getDescription()));
                for (SettingGroup.SettingOption o : group.getOptions()) {
                    writer.println(TAB + TAB + String.format("g.addOption(\"%s\",\"%s\");", o.getFlag(), o.getDescription()));
                }
                writer.println(TAB + TAB + "settingGroups.add(g);");
            }
        }
        writer.println(TAB + "}");

        writer.println(TAB + "@Override");
        writer.println(TAB + "public List<SettingGroup> getSettingGroups()");
        writer.println(TAB + "{");
        writer.println(TAB + TAB + "return settingGroups;");
        writer.println(TAB + "}");

    }

    private static void declare(Map<String,String> vars,PrintWriter writer,String comment){
        writer.println(comment);
        vars.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    writer.printf("%s%s %s;\n", TAB, e.getValue(), e.getKey());
                });
    }
    public static void declareVariables(ECUData ecuData, PrintWriter writer) {
        declare(ecuData.getRuntimeVars(),writer,"//Runtime vars");
        declare(ecuData.getEvalVars(),writer,"//Eval vars");
        declare(ecuData.getConstantVars(),writer,"// 'Constant' vars");
        writer.println("//Flags");
        ecuData.getFlags().forEach(e->{writer.println(TAB+"boolean "+e+";");});
    }

    public static void initDefaultValues(ECUData ecuData, PrintWriter writer) {
        writer.println("private void setDefaultValues() {");
        int x = 1;
        Map<String,String> types = new HashMap<>();
        types.putAll(ecuData.getConstantVars());
        types.putAll(ecuData.getRuntimeVars());
        types.putAll(ecuData.getEvalVars());

        ecuData.getDefaults().forEach(e->{writer.println(TAB+TAB+e);});

        writer.println("}");
    }
}
