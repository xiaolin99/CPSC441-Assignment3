package main.java.cpsc441.doNOTmodify;

import java.io.*;

public class LogFactory implements ILogFactory {

    public PrintStream newLogFile(File file) throws FileNotFoundException, IOException {
        file.getParentFile().mkdirs();
        file.createNewFile();
        return new PrintStream(
                new FileOutputStream(
                        file
                )
        );
    }
}
