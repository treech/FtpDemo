package data.access;

import data.access.ftp.FTPConfig;
import data.access.ftp.FTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/ftp")
@Slf4j
public class FtpController {

    @RequestMapping("/downloadFile")
    public void downloadFile() {
        try {
            boolean result = FTPUtil.downloadFile(FTPConfig.workingDirectory + "鄂-AR223R00_CRD000000_20201112185516_六栋车场入口_01.jpg",
                    "D:\\test\\鄂-AR223R00_CRD000000_20201112185516_六栋车场入口_01.jpg");
            System.out.println("result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/unzipFile")
    public void unzipFile() {
        File zipDir = new File(FTPConfig.ACCESS_LOG_ZIP_DIRECTORY);
        if (!zipDir.exists()) {
            return;
        }
        File[] zipFiles = zipDir.listFiles();
        if (zipFiles == null) {
            return;
        }
        for (int i = 0; i < zipFiles.length; i++) {
            try {
                FTPUtil.unzip(zipFiles[i]);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    @RequestMapping("/imgToBase64")
    public void testImgToBase64() {
        try {
            String result = FTPUtil.encodeImgToBase64(FTPConfig.workingDirectory + "鄂-AR223R00_CRD000000_20201112185516_六栋车场入口_01.jpg");
            System.out.println("result:" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
