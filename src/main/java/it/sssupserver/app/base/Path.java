package it.sssupserver.app.base;

import java.util.Arrays;
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
        return p.isEmpty() || test.test(p);
    }

    // hint suggesting that the Path should point to a directory
    // code is not required to check it
    private boolean isDir;
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

    public Path(String p, boolean isDir) throws InvalidPathException {
        this(p);
        this.isDir = isDir;
    }

    private Path(String[] items) {
        this.path = items;
    }

    /**
     * Return true if the path is flat, i.e.
     * there are no folder.
     */
    public boolean isFlat()
    {
        return getPath().length == 1;
    }

    public boolean isRoot()
    {
        return isFlat() && (this.path[0].isEmpty() || this.path[0].equals("."));
    }

    public String[] getPath()
    {
        return this.path;
    }

    // Return child Path, i.e.: Path
    // withoud current base dire
    public Path child()
    {
        if (this.path.length == 0) {
            return new Path(new String[0]);
        } else {
            var tmp = Arrays.copyOfRange(this.path, 1, this.path.length);
            return new Path(tmp);
        }
    }

    /**
     * Name of the top item of this path
     */
    public String top()
    {
        return this.path.length == 0 ? "" : this.path[0];
    }

    public boolean isEmpty()
    {
        return this.path.length == 0;
    }

    public boolean isDir()
    {
        return this.isDir;
    }

    @Override
    public String toString()
    {
        return String.join("/", getPath());
    }
}
