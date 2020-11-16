package data.access.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

@Slf4j
public class FileUtil extends FileUtils {

    /**
     * 递归获取文件夹
     *
     * @param file
     * @param fileList
     */
    public static void getAllFile(File file, List<File> fileList) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (null != files) {
                for (File f : files) {
                    getAllFile(f, fileList);
                }
            }
        } else {
            fileList.add(file);
        }
    }

    /**
     * 获取文件短名称，不哈后缀
     *
     * @param fileName
     * @return
     */
    public static String getShortName(String fileName) {
        if (null != fileName && fileName.length() > 0 && fileName.lastIndexOf(".") > -1) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    /**
     * 路径尾部拼接分隔符
     *
     * @param path
     * @return
     */
    public static String appendSeparator(String path) {
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }

    public static void createDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
