/**
 * 
 */
package org.nullvm.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;

/**
 * @author niklas
 *
 */
public enum OS {
    linux, darwin;
    
    private static Map<File, String> llvmHostStrings = new HashMap<File, String>();
    
    public static OS getDefaultOS(File llvmHomeDir) {
        String host = getHost(llvmHomeDir);
        if (host.contains("linux")) {
            return OS.linux;
        }
        if (host.contains("darwin")) {
            return OS.darwin;
        }
        throw new IllegalArgumentException("Unrecognized OS in Host string: " + host);
    }
    
    @SuppressWarnings("rawtypes")
    static String getHost(File llvmHomeDir) {
        String llvmHostString = llvmHostStrings.get(llvmHomeDir);
        if (llvmHostString != null) {
            return llvmHostString;
        }
        String llcPath = "llc";
        if (llvmHomeDir != null) {
            llcPath = new File(new File(llvmHomeDir, "bin"), "llc").getAbsolutePath();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ExecuteStreamHandler streamHandler = new PumpStreamHandler(baos);
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);
        CommandLine command = CommandLine.parse(llcPath).addArgument("--version");
        try {
            Map env = EnvironmentUtils.getProcEnvironment();
            executor.setExitValues(new int[] {0, 1});
            executor.execute(command, env);
            String output = new String(baos.toByteArray());
            Matcher m = Pattern.compile("(?m)Host:\\s*(.*)$").matcher(output);
            if (m.find()) {
                llvmHostString = m.group(1).trim();
                llvmHostStrings.put(llvmHomeDir, llvmHostString);
                return llvmHostString;
            }
            throw new CompilerException("Failed to get Host string using command: " + command);
        } catch (IOException e) {
            throw new CompilerException("Failed to get Host string using command: " + command);
        }
    }
}
