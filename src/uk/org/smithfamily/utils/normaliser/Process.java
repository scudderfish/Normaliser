package uk.org.smithfamily.utils.normaliser;

import org.apache.commons.lang3.StringUtils;
import uk.org.smithfamily.mslogger.ecuDef.Constant;
import uk.org.smithfamily.mslogger.ecuDef.OutputChannel;
import uk.org.smithfamily.mslogger.ecuDef.SettingGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class Process
{

    private static int lastOffset;

    private static String deBinary(String group)
    {
        Matcher binNumber = Patterns.binary.matcher(group);
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

    private static String removeComments(String line)
    {
        line += "; junk";
        line = StringUtils.trim(line).split(";")[0];
        line = line.trim();
        return line;
    }


    private static String convertC2JavaBoolean(String expression)
    {
        Matcher matcher = Patterns.booleanConvert.matcher(expression);
        StringBuilder result = new StringBuilder(expression.length());
        while (matcher.find())
        {
            matcher.appendReplacement(result, "");
            result.append(matcher.group(1)).append(" ? 1 : 0)");
        }
        matcher.appendTail(result);
        expression = result.toString();
        return expression;

    }

    private static boolean isFloatingExpression(ECUData ecuData, String expression)
    {
        boolean result = expression.contains(".");
        if (result)
        {
            return result;
        }
        for (String var : ecuData.getRuntimeVars().keySet())
        {
            if (expression.contains(var) && ecuData.getRuntimeVars().get(var).equals("double"))
            {
                result = true;
            }
        }
        for (String var : ecuData.getEvalVars().keySet())
        {
            if (expression.contains(var) && ecuData.getEvalVars().get(var).equals("double"))
            {
                result = true;
            }
        }

        return result;
    }

    static void processExpr(ECUData ecuData, String line)
    {
        String definition;
        line = removeComments(line);

        line = StringUtils.replace(line, "timeNow", "timeNow()");
        line = StringUtils.replace(line,"array.","");

        if (line.contains("ignLoad")) {
            int x = 1;
        }

        Matcher bitsM = Patterns.bits.matcher(line);
        Matcher scalarM = Patterns.scalar.matcher(line);
        Matcher exprM = Patterns.expr.matcher(line);
        Matcher ochGetCommandM = Patterns.ochGetCommand.matcher(line);
        Matcher ochBlockSizeM = Patterns.ochBlockSize.matcher(line);
        if (bitsM.matches())
        {
            String name = bitsM.group(1);
            String offset = bitsM.group(3);
            String start = bitsM.group(4);
            String end = bitsM.group(5);
            String ofs = "0";
            if (end.contains("+"))
            {
                String[] parts = end.split("\\+");
                end = parts[0];
                ofs = parts[1];
            }
            definition = (name + " = utils.getBits(ochBuffer," + offset + "," + start + "," + end + "," + ofs + ");");
            ecuData.getRuntime().add(definition);
            ecuData.getRuntimeVars().put(name, "int");
        }
        else if (line.contains("scalar"))
        {
            //ignLoad           = scalar,   S16,    88, { bitStringValue( algorithmUnits , ignAlgorithm  ) }, ignLoadFeedBack, 0.000
            String name = scalarM.group(1);
            if (constantDefined(ecuData, name))
            {
                name += "RT";
            }
            String dataType = scalarM.group(2);
            String offset = scalarM.group(3);
            String units = scalarM.group(4);
            String scale = scalarM.group(5);
            String numOffset = scalarM.group(6);

            if (safeDouble(scale) != 1)
            {
                ecuData.getRuntimeVars().put(name, "double");
            }
            else
            {
                ecuData.getRuntimeVars().put(name, "int");
            }
            definition = Output
                    .getScalar("ochBuffer", ecuData.getRuntimeVars().get(name), name, dataType, offset, scale, numOffset);
            ecuData.setFingerprintSource(ecuData.getFingerprintSource() + definition);
            ecuData.getRuntime().add(definition);
            
            int offsetOC = Integer.parseInt(offset);
            double scaleOC = !StringUtils.isEmpty(scale) ? safeDouble(scale) : 0;
            double translateOC = !StringUtils.isEmpty(numOffset) ? safeDouble(numOffset) : 0;
            
            OutputChannel outputChannel = new OutputChannel(name, dataType, offsetOC, units, scaleOC, translateOC);
            ecuData.getOutputChannels().add(outputChannel);
        }
        else if (exprM.matches())
        {
            String name = exprM.group(1);
            if ("pwma_load".equals(name))
            {
                // Hook to hang a break point on
                @SuppressWarnings("unused")
                int x = 1;
            }
            String expression = deBinary(exprM.group(2).trim());
            Matcher ternaryM = Patterns.ternary.matcher(expression);
            if (ternaryM.matches())
            {
                // System.out.println("BEFORE : " + expression);
                String test = ternaryM.group(1);
                String values = ternaryM.group(2);
                if (StringUtils.containsAny(test, "<>!="))
                {
                    expression = "(" + test + ") ? " + values;
                }
                else
                {
                    expression = "((" + test + ") != 0 ) ? " + values;
                }
                // System.out.println("AFTER  : " + expression + "\n");
            }
            if (expression.contains("*") && expression.contains("=="))
            {
                expression = convertC2JavaBoolean(expression);
            }
            definition = name + " = (" + expression + ");";

            // If the expression contains a division, wrap it in a try/catch to
            // avoid division by zero
            if (expression.contains("/"))
            {
                definition = "try\n" + Output.TAB + Output.TAB + "{\n" + Output.TAB + Output.TAB + Output.TAB + definition + "\n"
                        + Output.TAB + Output.TAB + "}\n" + Output.TAB + Output.TAB + "catch (ArithmeticException e) {\n"
                        + Output.TAB + Output.TAB + Output.TAB + name + " = 0;\n" + Output.TAB + Output.TAB + "}";
            }

            ecuData.getRuntime().add(definition);
            String dataType;
                dataType = "double";
            ecuData.getEvalVars().put(name, dataType);
            OutputChannel outputChannel = new OutputChannel(name, dataType, -1, "", 1, 0);
            ecuData.getOutputChannels().add(outputChannel);
            
        }
        else if (ochGetCommandM.matches())
        {
            String och = ochGetCommandM.group(1);
            if (och.length() > 1)
            {
                och = Output.HexStringToBytes(ecuData, och, 0, 0, 0);
            }
            else
            {
                och = "'" + och + "'";
            }
            ecuData.setOchGetCommandStr("byte [] ochGetCommand = new byte[]{" + och + "};");
        }
        else if (ochBlockSizeM.matches())
        {
            ecuData.setOchBlockSizeStr("int ochBlockSize = " + ochBlockSizeM.group(1) + ";");
        }
        else if (line.startsWith("#"))
        {
            String preproc = processPreprocessor(ecuData, line);
            ecuData.getRuntime().add(preproc);
            
            OutputChannel oc = new OutputChannel(preproc, "PREPROC", 0, "", 0, 0);
            ecuData.getOutputChannels().add(oc);
        }
        else if (!StringUtils.isEmpty(line))
        {
            System.out.println(line);
        }
    }

    /**
     * Occasionally we get a collision between the name of a constant and an expression. Test for that here.
     */
    private static boolean constantDefined(ECUData ecuData, String name)
    {
        for (Constant c : ecuData.getConstants())
        {
            if (c.getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }

    static void processLogEntry(ECUData ecuData, String line)
    {
        line = removeComments(line);

        Matcher logM = Patterns.log.matcher(line);
        if (logM.matches())
        {
            String header = logM.group(2);
            String variable = logM.group(1);
            if ("double".equals(ecuData.getRuntimeVars().get(variable)))
            {
                variable = "round(" + variable + ")";
            }
            ecuData.getLogHeader().add("b.append(\"" + header + "\").append(\"\\t\");");
            ecuData.getLogRecord().add("b.append(" + variable + ").append(\"\\t\");");
        }
        else if (line.startsWith("#"))
        {
            String directive = processPreprocessor(ecuData, line);
            ecuData.getLogHeader().add(directive);
            ecuData.getLogRecord().add(directive);
        }
    }

    static String processPreprocessor(ECUData ecuData, String line)
    {
        String filtered;
        boolean stripped;

        filtered = line.replace("  ", " ");
        stripped = filtered.equals(line);
        while (!stripped)
        {
            line = filtered;
            filtered = line.replace("  ", " ");
            stripped = filtered.equals(line);
        }
        String[] components = line.split(" ");
        String flagName = components.length > 1 ? sanitize(components[1]) : "";
        if (components[0].equals("#if") || components[0].equals("#ifdef"))
        {
            ecuData.getFlags().add(flagName);
            return ("if (" + flagName + ")\n        {");
        }
        if (components[0].equals("#elif"))
        {
            ecuData.getFlags().add(flagName);
            return ("}\n        else if (" + flagName + ")\n        {");
        }
        if (components[0].equals("#else"))
        {
            return ("}\n        else\n        {");
        }
        if (components[0].equals("#endif"))
        {
            return ("}");
        }

        return "";
    }

    private static String sanitize(String flagName)
    {
        return StringUtils.replace(flagName, "!", "n");
    }

    static void processFrontPage(ECUData ecuData, String line)
    {
        line = removeComments(line);

        Matcher dgM = Patterns.defaultGauge.matcher(line);
        if (dgM.matches())
        {
            ecuData.getDefaultGauges().add(dgM.group(1));
        }
    }

    static void processHeader(ECUData ecuData, String line)
    {
        Matcher queryM = Patterns.queryCommand.matcher(line);
        if (queryM.matches())
        {
            ecuData.setQueryCommandStr("byte[] queryCommand = new byte[]{'" + queryM.group(1) + "'};");
            return;
        }

        Matcher sigM = Patterns.signature.matcher(line);
        Matcher sigByteM = Patterns.byteSignature.matcher(line);
        if (sigM.matches())
        {
            String tmpsig = sigM.group(1);
            if (line.contains("null"))
            {
                tmpsig += "\\0";
            }
            ecuData.setClassSignature("\"" + tmpsig + "\"");
            ecuData.setSignatureDeclaration("String signature = \"" + tmpsig + "\";");
        }
        else if (sigByteM.matches())
        {
            String b = sigByteM.group(1).trim();
            ecuData.setClassSignature("new String(new byte[]{" + b + "})");
            ecuData.setSignatureDeclaration("String signature = \"\"+(byte)" + b + ";");
        }

    }

    static void processGaugeEntry(ECUData ecuData, String line)
    {
        line = removeComments(line);

        Matcher m = Patterns.gauge.matcher(line);
        if (m.matches())
        {
            String name = m.group(1);
            String channel = m.group(2);
            String title = m.group(3);
            String units = m.group(4);
            String lo = m.group(5);
            String hi = m.group(6);
            String loD = m.group(7);
            String loW = m.group(8);
            String hiW = m.group(9);
            String hiD = m.group(10);
            String vd = m.group(11);
            String ld = m.group(12);

            String g = String
                    .format("gauges.addGauge(new GaugeDetails(\"Gauge\",\"\",\"%s\",\"%s\",%s,\"%s\",\"%s\",%s,%s,%s,%s,%s,%s,%s,%s,45));",
                            name, channel, channel, title, units, lo, hi, loD, loW, hiW, hiD, vd, ld);

            g = g.replace("{", "").replace("}", "");
            String gd = String
                    .format("<tr><td>Gauge</td><td></td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                            name, channel, title, units, lo, hi, loD, loW, hiW, hiD, vd, ld);
            ecuData.getGaugeDoc().add(gd);
            ecuData.getGaugeDef().add(g);
        }
        else if (line.startsWith("#"))
        {
            ecuData.getGaugeDef().add(processPreprocessor(ecuData, line));
            ecuData.getGaugeDoc().add(String.format("<tr><td colspan=\"12\" id=\"preprocessor\">%s</td></tr>", line));
        }

    }

    /**
     * Process the [Constants] section of the ini file
     *
     */
    static void processConstants(ECUData ecuData, String line)
    {
        line = removeComments(line);
        if (StringUtils.isEmpty(line))
        {
            return;
        }

        if (line.contains("messageEnvelopeFormat"))
        {
            ecuData.setCRC32Protocol(line.contains("msEnvelope_1.0"));
        }
        Matcher pageM = Patterns.page.matcher(line);
        if (pageM.matches())
        {
            ecuData.setCurrentPage(Integer.parseInt(pageM.group(1).trim()));
            return;
        }
        Matcher pageSizesM = Patterns.pageSize.matcher(line);
        if (pageSizesM.matches())
        {
            String values = StringUtils.remove(pageSizesM.group(1), ' ');
            String[] list = values.split(",");
            ecuData.setPageSizes(new ArrayList<>(Arrays.asList(list)));
        }
        Matcher pageIdentifersM = Patterns.pageIdentifier.matcher(line);
        if (pageIdentifersM.matches())
        {
            String values = StringUtils.remove(pageIdentifersM.group(1), ' ');
            values = StringUtils.remove(values, '"');
            String[] list = values.split(",");
            ecuData.setPageIdentifiers(new ArrayList<>(Arrays.asList(list)));
        }

        Matcher pageActivateM = Patterns.pageActivate.matcher(line);
        if (pageActivateM.matches())
        {
            String values = StringUtils.remove(pageActivateM.group(1), ' ');
            values = StringUtils.remove(values, '"');
            String[] list = values.split(",");
            ecuData.setPageActivateCommands(new ArrayList<>(Arrays.asList(list)));
        }

        Matcher pageReadCommandM = Patterns.pageReadCommand.matcher(line);
        if (pageReadCommandM.matches())
        {
            String values = StringUtils.remove(pageReadCommandM.group(1), ' ');
            values = StringUtils.remove(values, '"');
            String[] list = values.split(",");
            ecuData.setPageReadCommands(new ArrayList<>(Arrays.asList(list)));
        }

        Matcher interWriteDelayM = Patterns.interWriteDelay.matcher(line);
        if (interWriteDelayM.matches())
        {
            ecuData.setInterWriteDelay(Integer.parseInt(interWriteDelayM.group(1).trim()));
            return;
        }
        Matcher pageActivationDelayM = Patterns.pageActivationDelay.matcher(line);
        if (pageActivationDelayM.matches())
        {
            ecuData.setPageActivationDelayVal(Integer.parseInt(pageActivationDelayM.group(1).trim()));
            return;
        }
        // To allow for MS2GS27
        line = removeCurlyBrackets(line);
        Matcher bitsM = Patterns.bits.matcher(line);
        Matcher constantSimpleM = Patterns.constantSimple.matcher(line);
        //Matcher constantArrayM = Patterns.constantArray.matcher(line);
        if(line.contains("afrTable")) {
            int x=1;
        }
        if (line.contains("scalar"))
        {
            //                      0       1           2       3               4       5       6       7           8
            //      iacCLminValue = scalar, U08,      61,       "% / Steps", idleRes,   0.0,   0.0, idleResMax,    0 ; Minimum and maximum duty cycles when using closed loop idle
            String[] components = line.split("=");
            String name = components[0].trim();
            List<String> parameters = Arrays.stream(components[1].split(","))
                    .map(String::trim)
                    .map(e-> e.replace("\"",""))
                    .toList();

            String classtype =parameters.get(0);
            String type = parameters.get(1);

            String offsetStr = parameters.get(2);
            int offset = lastOffset;
            try {
                offset = Integer.parseInt(offsetStr);
            }catch (NumberFormatException ignored){};
            lastOffset=offset;
            String units = parameters.get(3);
            String scaleText = parameters.get(4);
            String translateText = parameters.get(5);
            String lowText = parameters.size() > 6 ? parameters.get(6) : "";
            String highText = parameters.size() > 7 ? parameters.get(7) :"";
            String digitsText = parameters.size() > 8 ? parameters.get(8):"";
            double scale = !StringUtils.isEmpty(scaleText) ? safeDouble(scaleText) : 0;
            double translate = !StringUtils.isEmpty(translateText) ? safeDouble(translateText) : 0;

            int digits = !StringUtils.isEmpty(digitsText) ? (int) safeDouble(digitsText) : 0;

            //noinspection SuspiciousMethodCalls
            if (!ecuData.getConstants().contains(name))
            {
                Constant c = new Constant(ecuData.getCurrentPage(), name, classtype, type, offset, "", units, scale, translate,
                        lowText, highText, digits);

                if (scale == 1.0)
                {
                    ecuData.getConstantVars().put(name, "int");
                }
                else
                {
                    ecuData.getConstantVars().put(name, "double");
                }
                ecuData.getConstants().add(c);
            }
        }
        else if (line.contains("array"))
        {
            //                  0       1       2       3   4   5 6   7   8   9
            //algorithmLimits= array,   U16,   [8],   "", 1.0, 0, 0, 511, 0, noMsqSave
            String[] components = line.split("=");
            String name = components[0].trim();
            List<String> parameters = Arrays.stream(components[1].split(","))
                    .map(String::trim)
                    .map(e-> e.replace("\"",""))
                    .toList();

            String classtype =parameters.get(0);
            String type = parameters.get(1);




            int idx = 2;
            int offset = lastOffset;

            String offsetStr = parameters.get(idx);
            if (!offsetStr.contains("[")) {
                try {
                    offset = Integer.parseInt(offsetStr);
                }catch (NumberFormatException ignored){};
                idx++;
            }
            lastOffset=offset;

            String shape = parameters.get(idx++);
            String units = parameters.get(idx++);
            String scaleText = parameters.get(idx++);
            String translateText = parameters.get(idx++);
            String lowText = parameters.get(idx++);
            String highText = parameters.get(idx++);
            String digitsText = parameters.size() > idx ? parameters.get(idx) :"";
            highText = highText.replace("{", "").replace("}", "");
            double scale = !StringUtils.isEmpty(scaleText) ? safeDouble(scaleText) : 0;
            double translate = !StringUtils.isEmpty(translateText) ? safeDouble(translateText) : 0;

            int digits = !StringUtils.isEmpty(digitsText) ? (int) safeDouble(digitsText) : 0;

            //noinspection SuspiciousMethodCalls
            if (!ecuData.getConstants().contains(name))
            {
                Constant c = new Constant(ecuData.getCurrentPage(), name, classtype, type, offset, shape, units, scale, translate,
                        lowText, highText, digits);
                if (shape.contains("x"))
                {
                    ecuData.getConstantVars().put(name, "double[][]");
                }
                else
                {
                    ecuData.getConstantVars().put(name, "double[]");
                }
                ecuData.getConstants().add(c);
            }
        }
        else if (constantSimpleM.matches())
        {
            String name = constantSimpleM.group(1);
            String classtype = constantSimpleM.group(2);
            String type = constantSimpleM.group(3);
            int offset = Integer.parseInt(constantSimpleM.group(4).trim());
            String units = constantSimpleM.group(5);
            double scale = safeDouble(constantSimpleM.group(6));
            double translate = safeDouble(constantSimpleM.group(7));

            Constant c = new Constant(ecuData.getCurrentPage(), name, classtype, type, offset, "", units, scale, translate, "0",
                    "0", 0);

            if (scale == 1.0)
            {
                ecuData.getConstantVars().put(name, "int");
            }
            else
            {
                ecuData.getConstantVars().put(name, "double");
            }
            ecuData.getConstants().add(c);
        }
        else if (bitsM.matches())
        {
            String name = bitsM.group(1);
            String offset = bitsM.group(3);
            String start = bitsM.group(4);
            String end = bitsM.group(5);
            if(offset.trim().isEmpty()) {
                offset="0";
            }
            String strBitsValues = bitsM.group(7);
            
            String[] bitsValues = new String[]{};
            if (strBitsValues != null)
            {
                bitsValues = Patterns.bitsValues.split(strBitsValues);
            }

            Constant c = new Constant(ecuData.getCurrentPage(), name, "bits", "", Integer.parseInt(offset.trim()), "[" + start
                    + ":" + end + "]", "", 1, 0, "0", "0", 0, bitsValues);
            ecuData.getConstantVars().put(name, "int");
            ecuData.getConstants().add(c);

        }
        else if (line.startsWith("#"))
        {
            String preproc = (processPreprocessor(ecuData, line));
            Constant c = new Constant(ecuData.getCurrentPage(), preproc, "", "PREPROC", 0, "", "", 0, 0, "0", "0", 0);
            ecuData.getConstants().add(c);
        }
    }

    private static double safeDouble(String txt) {
        try {
            return Double.parseDouble(txt);
        }catch(NumberFormatException e) {
            return 0;
        }
    }

    private static String removeCurlyBrackets(String line)
    {
        return line.replaceAll("\\{", "").replaceAll("}", "");
    }









    static void processConstantsExtensions(ECUData ecuData, String line)
    {
        line = removeComments(line);

        if (line.contains("defaultValue"))
        {
            String statement;
            if(line.contains("injAng")) {
                int x=1;
            }
            String[] definition = line.split("=")[1].split(",");
            String varName = definition[0].trim();
            if(varName.equals("algorithmLimits")) {
                int x = 1;
            }
            if(ecuData.getConstantVars().containsKey(varName)) {

                String varType = ecuData.getConstantVars().get(varName);
                statement= varName + " = ";
                String value = definition[1].trim();
                
                
                
                
                
                if(varType.contains("[]")){

                    String[] values = value.split("\\s+");
                    

                    statement += "new "+varType+"{" + String.join(",",values) + "};";
                } else {
                    statement+= value +";";
                }
                ecuData.getInitalisedConstants().add(varName);
            }
            else {
                statement = definition[0] + " = " + definition[1] + ";";
            }
            ecuData.getDefaults().add(statement);
        }
        else if (line.contains("requiresPowerCycle"))
        {      
            String field = line.split("=")[1];
            ecuData.getRequiresPowerCycle().add(field.trim());
        }
    }

    static void processPcVariables(ECUData ecuData, String line)
    {

    }

    public static void processSettingGroups(ECUData ecuData, String line)
    {
        line = removeComments(line);
        ArrayList<SettingGroup> groups = ecuData.getSettingGroups();
        if(line.contains("settingGroup"))
        {
            String data = line.split("=")[1];
            int commaIndex = data.indexOf(',');
            String groupName = data.substring(0, commaIndex).trim();
            String description = data.substring(commaIndex+1).replaceAll("\"", "").trim();
            SettingGroup group = new SettingGroup(groupName,description);
            groups.add(group);
        }
        else if (line.contains("settingOption"))
        {
            SettingGroup currentGroup = groups.get(groups.size()-1);
            String data = line.split("=")[1];
            int commaIndex = data.indexOf(',');
            String flagName = data.substring(0, commaIndex).trim();
            String description = data.substring(commaIndex+1).replaceAll("\"", "").trim();
            currentGroup.addOption(flagName, description);
        }
    }

}
