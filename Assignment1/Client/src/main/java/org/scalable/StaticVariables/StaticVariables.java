package org.scalable.StaticVariables;

import java.io.File;

public class StaticVariables {
    public static final String getPath = "/albums/1";
    public static final String postPath = "/albums";
    public static File file = new File("src/main/resources/Report.csv");
    public static String[] header = {"Start Time", "Request Type", "Latency", "Response Code"};
    public static File imageFile = new File("src/main/resources/nmtb.png");
}
