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

/**
 * Hello world!
 *
 */
public class App 
{
    static void panic(String error) {
        System.err.println("Panic: " + error);
        System.exit(1);
    }

    static String host = "localhost";
    static int port = 5050;
    static SocketChannel connect() {
        var address = new InetSocketAddress(host, port);
        try {
            var sc = SocketChannel.open(address);
            return sc;
        } catch (Exception e) {
            System.err.println("Cannot connect to " + address + ": " + e);
            System.exit(1);
        }
        return null; // never reached
    }

    /**
     * Extract trailing arguments
     */
    static String[] getArgs(String[] args, int skip) {
        if (args.length < skip) {
            System.err.println("Missing arguments");
            System.exit(1);
        }
        var ans = new String[args.length - skip];
        for (int i=0; i != ans.length; ++i) {
            ans[i] = args[skip+i];
        }
        return ans;
    }
    
    static Map<String, Command> commands = new TreeMap<>(); 

    static void handleCommand(Command command, int version, String username, String[] args) throws Exception {
        try {
            command.parse(version, username, args);
        } catch (Exception e) {
            System.err.println("Error occurred while parsing command " + command.getName() + " " + e);
            command.printHelp("\t");
            System.exit(1);
        }
        var sc = connect();
        var scheduler = new Scheduler(sc);
        command.exec(sc, scheduler);
        scheduler.parse();
    }

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
        };
        for (var cmd : cmds) {
            commands.put(cmd.getName(), cmd);
        }
    }

    static void help() {
        System.err.println("Usage:");
        System.err.println("\tcommand [args]");
        System.err.println("\tGeneric parameters (i.e. valid for any command):");
        System.err.println("\t\t--host hostname: host name or address to connect at, default 'localhost'");
        System.err.println("\t\t--port number: TCP port to connect at, default '5050'");
        System.err.println();
        System.err.println("\tAvailable commands:");
        for (var cmd : commands.entrySet()) {
            cmd.getValue().printHelp("\t\t");
        }
        System.exit(1);
    }

    static void handle(String[] params) throws Exception {
        var cmd = params[0];
        var args = getArgs(params, 1);
        var command = commands.get(cmd);
        if (command == null) {
            System.err.println("Unknow command: '" + cmd + "'");
            help();
        } else {
            handleCommand(command, 1, null, args);
        }
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
                    default:
                        panic("Unknown parameter '" + parameter + "'");
                        break;
                }
            } else {
                ans.add(a);
            }
        }
        return ans.stream().toArray(String[] ::new);
    }


    public static void main( String[] args ) throws Exception
    {
        args = extract_globals(args);
        addCommmands();
        //addHandlers();
        if (args.length == 0 || args[0].equals("--help")) {
            help();
        }
        handle(args);
    }
}
