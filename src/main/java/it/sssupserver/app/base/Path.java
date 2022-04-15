package it.sssupserver.app.base;

import java.util.function.Predicate;
import java.util.regex.*;

/**
 * Represent the path identifying a file
 * or a directory on the file server
 */
public class Path {
    private static Predicate<String> test;
    {
        var re = "[/\\w\\.\\s-]+";
        var pattern = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
        test = pattern.asMatchPredicate();
    }

    public static boolean checkPath(String p)
    {
        return test.test(p);
    }
    
    private String[] path;
    public Path(String p) throws InvalidPathException
    {
        if (!checkPath(p))
        {
            throw new InvalidPathException("Invalid path: " + p);
        }

        var tmp = p.split("/");
        if (tmp.length == 0)
        {
            throw new InvalidPathException("Path cannot be empty");
        }
        for (var x : tmp)
        {
            if (x.equals(".."))
            {
                throw new InvalidPathException("Path cannot contains '..'");
            }
            else if (x.startsWith(" ") || x.endsWith(" "))
            {
                throw new InvalidPathException("Path cannot be limited by ' '");
            }
        }
        this.path = tmp;
    }

    /**
     * Return true if the path is flat, i.e.
     * there are no folder.
     */
    public boolean isFlat()
    {
        return getPath().length == 1;
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
