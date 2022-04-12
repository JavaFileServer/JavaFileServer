package it.sssupserver.app.base;

import java.security.PublicKey;
import java.util.function.Predicate;
import java.util.regex.*;

/**
 * Represent the path identifying a file
 * or a directory on the file server
 */
public class Path {
    private static Predicate<String> test;
    {
        var re = "[/\\w\\.\\s]+";
        var pattern = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
        test = pattern.asMatchPredicate();
    }

    public static boolean checkPath(String p)
    {
        return test.test(p);
    }
    
    private String[] path;
    public Path(String p) throws Exception
    {
        if (!checkPath(p))
        {
            throw new Exception("Invalid path: " + p);
        }

        var tmp = p.split("/");
        if (tmp.length == 0)
        {
            throw new Exception("Path cannot be empty");
        }
        for (var x : tmp)
        {
            if (x.equals(".."))
            {
                throw new Exception("Path cannot contains '..'");
            }
            else if (x.startsWith(" ") || x.endsWith(" "))
            {
                throw new Exception("Path cannot be limited by ' '");
            }
        }
        this.path = tmp;
    }

    public String[] getPath()
    {
        return this.path;
    }

    @Override
    public String toString()
    {
        return String.join("/", getPath());
    }
}
