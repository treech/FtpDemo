package data.access.ftp;

import org.apache.commons.net.ftp.FTP;

import java.io.File;

public class FTPConfig {

    public static String username = "";

    public static String password = "";

    public static String host = "";

    public static Integer port = null;

    public static Integer poolSize;

    public static String encoding = "UTF-8";

    public static int clientTimeout = 120000;

    public static int transferFileType = FTP.BINARY_FILE_TYPE;

    public static String workingDirectory = "";

    public static String vehivleSaveDirectory = "";
    public static boolean vehivleEnale = false;

    public static String accesslogSaveDirectory = "";
    public static boolean accesslogEnale = false;

    public static String facelogWorkingDirectory = "";
    public static String facelogSaveDirectory = "";
    public static boolean facelogEnale = false;

    public static String ACCESS_LOG_ZIP_DIRECTORY = "{0}" + "zip" + File.separator;
    public static String ACCESS_LOG_UNZIP_DIRECTORY = "{0}" + "unzip" + File.separator;
    public static String ACCESS_LOG_BAK_DIRECTORY = "{0}" + "bak" + File.separator;
    public static String ACCESS_LOG_FAIL_DIRECTORY = "{0}" + "fail" + File.separator;

    public static String VEHICLE_PASS_LOG_ZIP_DIRECTORY = "{0}" + "zip" + File.separator;
    public static String VEHICLE_PASS_LOG_UNZIP_DIRECTORY = "{0}" + "unzip" + File.separator;
    public static String VEHICLE_PASS_LOG_BAK_DIRECTORY = "{0}" + "bak" + File.separator;
    public static String VEHICLE_PASS_LOG_FAIL_DIRECTORY = "{0}" + "fail" + File.separator;

    public static String FACE_PASS_LOG_ZIP_DIRECTORY = "{0}" + "zip" + File.separator;
    public static String FACE_PASS_LOG_UNZIP_DIRECTORY = "{0}" + "unzip" + File.separator;
    public static String FACE_PASS_LOG_BAK_DIRECTORY = "{0}" + "bak" + File.separator;
    public static String FACE_PASS_LOG_FAIL_DIRECTORY = "{0}" + "fail" + File.separator;

}
