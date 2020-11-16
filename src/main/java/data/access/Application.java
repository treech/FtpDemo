package data.access;

import data.access.ftp.FTPConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.text.MessageFormat;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        FTPConfig.host = "172.16.90.203";
        FTPConfig.port = 21;
        FTPConfig.username = "share";
        FTPConfig.password = "whjcyf";
        FTPConfig.workingDirectory = "";
        FTPConfig.ACCESS_LOG_ZIP_DIRECTORY = MessageFormat.format(FTPConfig.ACCESS_LOG_ZIP_DIRECTORY, "D:\\test" + File.separator);
        FTPConfig.ACCESS_LOG_UNZIP_DIRECTORY = MessageFormat.format(FTPConfig.ACCESS_LOG_UNZIP_DIRECTORY, "D:\\test" + File.separator);
        SpringApplication.run(Application.class, args);
    }
}
