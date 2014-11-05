package main.java.cpsc441.doNOTmodify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public interface ILogFactory {
    public PrintStream newLogFile(File file) throws FileNotFoundException, IOException;
}
