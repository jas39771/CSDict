
// You can use this file as a starting point for your dictionary client
// The file contains the code for command line parsing and it also
// illustrates how to read and partially parse the input typed by the user. 
// Although your main class has to be in this file, there is no requirement that you
// use this template or hav all or your classes in this file.

import java.io.*;
import java.lang.System;
import java.net.*;
import java.util.Arrays;
import java.util.regex.Pattern;

//
// This is an implementation of a simplified version of a command
// line dictionary client. The only argument the program takes is
// -d which turns on debugging output. 
//


public class CSdict {
    static final int MAX_LEN = 255;
    static Boolean debugOn = false;
    static Boolean quitStatus = false;
    static Boolean connectionStatus = false;

    
    private static final int PERMITTED_ARGUMENT_COUNT = 1;
    private static final int OPEN = 0;
    private static final int DICT = 1;
    private static final int SET = 2;
    private static final int DEFINE = 3;
    private static final int MATCH = 4;
    private static final int PREFIXMATCH = 5;
    private static final int CLOSE = 6;
    private static final int QUIT = 7;
    private static String command;
    private static String[] arguments;
    private static boolean[] connectionSpecs = {false, true,
        true, true, true, true, true};
    private static String database;
    private static final String[] emptyArray = new String[0];
    
    public static void main(String [] args) {
        // byte cmdString[] = new byte[MAX_LEN];
        int len;
        // Verify command line arguments
        if (args.length == PERMITTED_ARGUMENT_COUNT) {
            debugOn = args[0].equals("-d");
            if (debugOn) {
                System.out.println("Debugging output enabled");
            } else {
                System.out.println("997 Invalid command line option - Only -d is allowed");
                return;
            }
        } else if (args.length > PERMITTED_ARGUMENT_COUNT) {
            System.out.println("996 Too many command line options - Only -d is allowed");
            return;
        }

        runCmdLine(null);
    }

    private static void runCmdLine(Socket socket) {
        while (!quitStatus) {
            byte cmdString[] = new byte[MAX_LEN];
            readInputs(cmdString);
            switch (command) {
                case "open": openCmd();
                             break;
                case "dict": dictCmd(socket);
                             break;
                case "set": setCmd();
                            break;
                case "define": defineCmd(socket);
                               break;
                case "match": matchCmd(socket);
                              break;
                case "prefixmatch": prefixmatchCmd(socket);
                                    break;
                case "close": closeCmd(socket);
                              break;
                case "quit": quitCmd(socket);
                             break;
                default:
                    System.out.println("900 Invalid command");
                    break;
            }
        }
    }

    private static void closeCmd(Socket socket) {
        if (argumentCheck(0, CLOSE)) {
            String fromServer;
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                if (debugOn) {
                    System.out.println("> QUIT");
                }
                out.println("QUIT");
                while ((fromServer = in.readLine()) != null) {
                    if (debugOn) {
                        System.out.println("<-- " + fromServer);
                    }
                    break;
                }
                out.close();
                in.close();
            } catch (IOException e) {
                System.err.println("925 Control connection I/O error, closing control connection");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            connectionStatus = false;
            return;
        }
    }

    private static boolean argumentCheck(int argNum, int cmd) {
        if (cmd != QUIT){
            if (connectionSpecs[cmd] != connectionStatus) {
                System.out.println("903 Supplied command not expected at this time");
                return false;
            }
        }
        if (arguments.length != argNum) {
            System.out.println("901 Incorrect number of arguments");
            return false;
        }
        if ((cmd == DEFINE) || (cmd == MATCH) || (cmd == PREFIXMATCH)) {
            for (String arg: arguments) {
                if (!Pattern.matches("[a-z]+", arg)) {
                    System.out.println("902 Invalid argument");
                    return false;
                }
            }
        }
        if ((cmd == OPEN)) {
            if (!(Pattern.matches(
                    "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$",
                arguments[0])) || !(Pattern.matches("[0-9]+", arguments[1])) ) {
                System.out.println("902 Invalid argument");
                return false;
            }
        }
        return true;
    }

    private static void prefixmatchCmd(Socket socket) {
        matchHelper(socket, " prefix ");
        return;
    }

    private static void matchHelper(Socket socket, String strategy) {
        if (argumentCheck(1, MATCH)) {
            String fromServer;
            PrintWriter out = null;
            BufferedReader in = null;
            if (socket != null) {
                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    if (debugOn) {
                        System.out.println("> MATCH "+ database + strategy + arguments[0]);
                    }
                    out.println("MATCH "+ database + strategy + arguments[0]);
                    while ((fromServer = in.readLine()) != null) {
                        if (fromServer.startsWith("552") | fromServer.startsWith("550")) {
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                            System.out.println("*****No matching word(s) found*****");
                            break;
                        }
                        if (fromServer.startsWith("250")){
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                            break;
                        }
                        if (fromServer.startsWith("152")){
                            if (debugOn){
                                System.out.println("<-- " + fromServer);
                            }
                        } else {
                            System.out.println(fromServer);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("925 Control connection I/O error, closing control connection");
                    closeCmd(socket);
                }
            }
        }
    }

    private static void matchCmd(Socket socket) {
        matchHelper(socket, " exact ");
        return;
    }

    private static void dictCmd(Socket socket) {
        if (argumentCheck(0, DICT)) {
            String fromServer;
            PrintWriter out = null;
            BufferedReader in = null;
            if (socket != null) {
                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    if (debugOn) {
                        System.out.println("> SHOW DATABASES");
                    }
                    out.println("SHOW DATABASES");
                    while ((fromServer = in.readLine()) != null) {
                        if (fromServer.equals("250 ok")) {
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                            break;
                        }
                        if (fromServer.startsWith("110")) {
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                        }
                        else {
                            System.out.println(fromServer);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("925 Control connection I/O error, closing control connection");
                    closeCmd(socket);
                }
            }
        }
        return;
    }

    private static void setCmd() {
        if (argumentCheck(1, SET)){
            database = arguments[0];
        }
        return;
    }

    private static void defineCmd(Socket socket) {
        if (argumentCheck(1, DEFINE)){
            String fromServer;
            PrintWriter out = null;
            BufferedReader in = null;
            if (socket != null) {
                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    if (debugOn) {
                        System.out.println("> DEFINE " + database + " " + arguments[0]);
                    }
                    out.println("DEFINE " + database + " " + arguments[0]);
                    while ((fromServer = in.readLine()) != null) {
                        if (fromServer.startsWith("552") || fromServer.startsWith("550")) {
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                            System.out.println("***No definition found***");
                            command = "match";
                            String currentDatabase = database;
                            database = "*";
                            matchCmd(socket);
                            database = currentDatabase;
                            break;
                        }
                        if (fromServer.startsWith("250 ok")) {
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                            break;
                        }
                        if (fromServer.startsWith("151") || fromServer.startsWith("150")) {
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                            if (fromServer.startsWith("151")) {
                                String[] temp = fromServer.split("(?i).*" + arguments[0] + "\" ");
                                System.out.println("@ " + temp[1]);
                            }
                        }
                        else {
                            System.out.println(fromServer);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("925 Control connection I/O error, closing control connection");
                    closeCmd(socket);
                }
            }
        }
        return;
    }

    private static void openCmd() {
        if (argumentCheck(2, OPEN)){
                int portNo = Integer.parseInt(arguments[1]);
                Socket dictSocket = new Socket();
                SocketAddress dictSocketAddress = new InetSocketAddress(arguments[0], portNo);
                try {
                    dictSocket.connect(dictSocketAddress, 10000);
                } catch (SocketTimeoutException e) {
                    System.err.println("920 Control connection to "+ arguments[0]+" on port "+ arguments[1] +" failed to open");
                    for(int i = 0 ; i < 10 ; i++) {

                    }
                    return;
                } catch (IOException e) {
                    System.err.println("925 Control connection I/O error, closing control connection");
                    return;
                } try {
                    dictSocket.setSoTimeout(10000);
                    connectionStatus = true;
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(dictSocket.getInputStream()));
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                        if (fromServer.startsWith("220")) {
                            connectionStatus = true;
                            database = "*";
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                         break;
                        }
                        if (fromServer.startsWith("530") || fromServer.startsWith("421")) {
                            if (debugOn) {
                                System.out.println("<-- " + fromServer);
                            }
                            closeCmd(dictSocket);
                            break;
                        }
                    }
                    while (connectionStatus) {
                        runCmdLine(dictSocket);
                    }
                    connectionStatus = false;
                } catch (UnknownHostException e) {
                    String tempConnection = arguments[0];
                    String tempPort = arguments[1];
                    String errorMessage = "920 Control connection to " + tempConnection+ " on port " + tempPort +" failed to open";
                    System.err.println(errorMessage);
                    return;
                } catch (SocketTimeoutException e) {
                    System.err.println("999 Timed out while waiting for a response");
                    connectionStatus = false;
                    return;
                } catch (IOException e) {
                    System.err.println("925 Control connection I/O error, closing control connection");
                    connectionStatus = false;
                    return;
                }
        }
        return;
    }

    private static void quitCmd(Socket socket) {
        if (argumentCheck(0, QUIT)) {
            if (connectionStatus) {
                closeCmd(socket);
            }
            quitStatus = true;
            return;
        }
    }

    private static void readInputs(byte[] cmdString) {
        int len;
        try {
            System.out.print("csdict> ");
            System.in.read(cmdString);

            // Convert the command string to ASII
            String inputString = new String(cmdString, "ASCII");

            // Split the string into words
            String[] inputs = inputString.trim().split("( |\t)+");
            // Set the command
            command = inputs[0].toLowerCase().trim();
            // Remainder of the inputs is the arguments.
            arguments = Arrays.copyOfRange(inputs, 1, inputs.length);
        } catch (IOException exception) {
            System.err.println("998 Input error while reading commands, terminating.");
            System.exit(-1);
        }
    }
}
    
    
