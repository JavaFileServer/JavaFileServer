package it.sssupserver.app.filemanagers;

import java.nio.file.Paths;

import it.sssupserver.app.filemanagers.samples.*;

public class FileManagerFactory {
    // Command line arguments starting with this prefix
    // are intended as directed to an executor
    private static final String argsPrefix = "--X";

    private static class ExecutorArgs {
        public String server_dir = "file-server";
    }

    public static void Help(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + "Arguments recognised by executors:");
        System.err.println(lpadding + "\t" + argsPrefix + "data path: path where to save files");
    }

    private static ExecutorArgs parseArgs(String[] args) {
        var ans = new ExecutorArgs();
        try {
            if (args != null) for (int i=0; i!=args.length; ++i) {
                var a = args[i];
                if (a.equals("--")) {
                    break;
                } else if (a.startsWith(argsPrefix)) {
                    var parameter = a.substring(argsPrefix.length());
                    switch (parameter) {
                        case "data":
                            if (i + 1 == args.length) {
                                throw new Exception("Missing value for parameter port");
                            }
                            ans.server_dir = args[++i];
                            break;
                        default:
                            throw new Exception("Unrecognised argument '" + a + "'");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            Help("\t");
            System.exit(1);
        }
        return ans;
    }

    public static FileManager getExecutor() throws Exception {
        return getExecutor(null);
    }

    // initialize
    public static FileManager getExecutor(String[] args) throws Exception {
        //return new DummyStringExecutor();
        //return new DummyMapExecutor();
        //return new FlatTmpExecutor();
        //return new UserTreeExecutor();
        var a = parseArgs(args);
        var datadir = Paths.get("").resolve(a.server_dir);
        return new UserTreeFileManager(datadir);
    }
}
