package it.sssupclient.app;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import it.sssupclient.app.command.Append;
import it.sssupclient.app.command.Command;
import it.sssupclient.app.command.Copy;
import it.sssupclient.app.command.Create;
import it.sssupclient.app.command.CreateOrReplace;
import it.sssupclient.app.command.Delete;
import it.sssupclient.app.command.Exists;
import it.sssupclient.app.command.List;
import it.sssupclient.app.command.Mkdir;
import it.sssupclient.app.command.Move;
import it.sssupclient.app.command.Read;
import it.sssupclient.app.command.Scheduler;
import it.sssupclient.app.command.Truncate;
import it.sssupclient.app.command.Write;
import it.sssupclient.app.command.Size;

/**
 * Hello world!
 *
 */
public class App {
    static int protocol_version = 4;

    static void panic(String error) {
        System.err.println("Panic: " + error);
        System.exit(1);
    }

    static String host = "localhost";
    static int port = 5050;
    static String username;

    static SocketChannel connect(String host, int port) {
        var address = new InetSocketAddress(host, port);
        try {
            var sc = SocketChannel.open(address);
            return sc;
        } catch (Exception e) {
            System.err.println("Cannot connect to " + address + ": " + e);
            return null;
        }
    }

    /**
     * Extract trailing arguments
     */
    static String[] getArgs(String[] args, int skip) {
        if (args.length < skip) {
            System.err.println("Missing arguments");
            return null;
        }
        var ans = new String[args.length - skip];
        for (int i = 0; i != ans.length; ++i) {
            ans[i] = args[skip + i];
        }
        return ans;
    }

    static Map<String, Command> commands = new TreeMap<>();

    static void addCommmands() {
        var cmds = new Command[] {
                new Read(),
                new List(),
                new CreateOrReplace(),
                new Create(),
                new Append(),
                new Exists(),
                new Truncate(),
                new Delete(),
                new Mkdir(),
                new Move(),
                new Copy(),
                new Write(),
                new Size(),
        };
        for (var cmd : cmds) {
            commands.put(cmd.getName(), cmd);
        }
    }

    static void help() {
        help(0);
    }

    // Display help for specified command or for all them
    static boolean help(String cmd) {
        if (cmd == null) {
            help();
        } else {
            var command = commands.get(cmd);
            if (command == null) {
                if (cmd != null) {
                    System.err.println("Unknown command '" + cmd + "'");
                }
                System.err.println("Available commands:");
                for (var c : commands.entrySet()) {
                    c.getValue().printHelp("\t");
                }
            } else {
                System.err.println("Usage:");
                command.printHelp("\t");
            }
        }
        return true;
    }

    static boolean help(int exit_status) {
        System.err.println("Usage:");
        System.err.println("\tcommand [args]");
        genericHelp();
        System.err.println("\tAvailable commands:");
        for (var cmd : commands.entrySet()) {
            cmd.getValue().printHelp("\t\t");
        }
        return exit_status == 0 ? true : false;
    }

    private static void genericHelp() {
        System.err.println("\tGeneric parameters (i.e. valid for any command):");
        System.err.println("\t\t--host hostname: host name or address to connect at, default 'localhost'");
        System.err.println("\t\t--port number: TCP port to connect at, default '5050'");
        System.err.println("\t\t--user name: username to use to perform operation on the server");
        System.err.println();
    }

    static boolean handle(String[] params, ArrayList<String> response, String username, String host, int port) throws Exception {
        if (params.length == 0) {
            System.err.println("No command passed");
            help(1);
            return false;
        }
        var cmd = params[0];
        var args = getArgs(params, 1);
        if (args == null)
            return false;
        var command = commands.get(cmd);
        if (command == null) {
            System.err.println("Unknow command: '" + cmd + "'");
            return help(1);
        } else {
            return handleCommand(command, protocol_version, username, host, port, args, response);
        }
    }

    static boolean handleCommand(Command command, int version, String username, String host, int port, String[] args,
            ArrayList<String> response) throws Exception {
        try {
            command.parse(version, username, args);
        } catch (Exception e) {
            System.err.println("Error occurred while parsing command " + command.getName() + " " + e);
            command.printHelp("\t");
            return false;
        }
        var sc = connect(host, port);
        if (sc == null)
            return false;
        var scheduler = new Scheduler(sc);
        command.exec(sc, scheduler);
        var success = scheduler.parse(response);
        // System.out.println("Command handled with success: " + String.valueOf(success));
        return success ? true : false;
    }

    static String[] extract_globals(String[] args) {
        var ans = new ArrayList<String>();
        boolean takeAll = false;
        for (int i = 0; i < args.length; ++i) {
            var a = args[i];
            if (takeAll) {
                ans.add(a);
            } else if (a.equals("--")) {
                takeAll = true;
            } else if (a.startsWith("--")) {
                var parameter = a.substring(2);
                switch (parameter) {
                    case "port":
                        if (i + 1 == args.length) {
                            panic("Missing value for parameter port");
                        }
                        port = Integer.parseInt(args[++i]);
                        break;
                    case "host":
                        if (i + 1 == args.length) {
                            panic("Missing value for parameter port");
                        }
                        host = args[++i];
                        break;
                    case "user":
                        if (i + 1 == args.length) {
                            panic("Missing value for parameter user");
                        }
                        username = args[++i];
                        break;
                    case "help":
                        help(args[0]);
                        break;
                    default:
                        panic("Unknown parameter '" + parameter + "'");
                        break;
                }
            } else {
                ans.add(a);
            }
        }
        return ans.stream().toArray(String[]::new);
    }

    public static boolean execute(String[] args, ArrayList<String> response, String username, String host, int port)
            throws Exception {
        addCommmands();
        args = extract_globals(args);
        return handle(args, response, username, host, port);
    }

    public static void main(String[] args) throws Exception {
        var response = new ArrayList<String>();
        execute(args, response, username, host, port);
        for (var elem : response){
            System.out.println(elem);
        }
    }
}
