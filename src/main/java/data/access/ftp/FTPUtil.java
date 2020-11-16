package data.access.ftp;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.NoRouteToHostException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author mjh
 */

@Slf4j
@Component
public class FTPUtil {

    /**
     * FTP的连接池
     */
    @Autowired
    public static FTPClientPool ftpClientPool;
    /**
     * FTPClient对象
     */
    public static FTPClient ftpClient;


    private static FTPUtil ftpUtils;


    /**
     * 初始化设置
     *
     * @return
     */
    @PostConstruct
    public boolean init() {
        FTPClientFactory factory = new FTPClientFactory();
        ftpUtils = this;
        try {
            if (null != FTPConfig.poolSize && FTPConfig.poolSize > 0) {
                ftpClientPool = new FTPClientPool(FTPConfig.poolSize, factory);
            } else {
                ftpClientPool = new FTPClientPool(factory);
            }
        } catch (Exception e) {
            log.error("FTPUtil init error:", e);
            return false;
        }
        return true;
    }


    /**
     * 当前命令执行完成命令完成
     *
     * @throws IOException
     */
    public void complete() throws IOException {
        ftpClient.completePendingCommand();
    }

    /**
     * 当前线程任务处理完成，加入到队列的最后
     *
     * @return
     */
    public void disconnect() throws Exception {
        ftpClientPool.addObject(ftpClient);
    }

    /**
     * Description: 向FTP服务器上传文件
     *
     * @param remoteFile 上传到FTP服务器上的文件名
     * @param input      本地文件流
     * @return 成功返回true，否则返回false
     * @Version1.0
     */
    public static boolean uploadFile(String remoteFile, InputStream input) {
        FTPClient ftpClient = null;
        boolean result = false;
        try {
            ftpClient = ftpClientPool.borrowObject();
            ftpClient.enterLocalPassiveMode();
            result = ftpClient.storeFile(remoteFile, input);
            input.close();
            ftpClient.disconnect();
        } catch (Exception e) {
            log.error("FTPUtil uploadFile error:", e);
        } finally {
            try {
                if (null != ftpClient) {
                    ftpClientPool.returnObject(ftpClient);
                }
            } catch (Exception e) {
                log.error("FTPUtil uploadFile error:", e);
            }

        }
        return result;
    }

    /**
     * Description: 向FTP服务器上传文件
     *
     * @param remoteFile 上传到FTP服务器上的文件名
     * @param localFile  本地文件
     * @return 成功返回true，否则返回false
     * @Version1.0
     */
    public static boolean uploadFile(String remoteFile, String localFile) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(new File(localFile));
        } catch (FileNotFoundException e) {
            log.error("FTPUtil uploadFile error:", e);
        }
        return uploadFile(remoteFile, input);
    }

    /**
     * 拷贝文件
     *
     * @param fromFile
     * @param toFile
     * @return
     * @throws Exception
     */
    public static boolean copyFile(String fromFile, String toFile) throws Exception {
        InputStream in = getFileInputStream(fromFile);
        FTPClient ftpClient = ftpClientPool.borrowObject();
        boolean flag = ftpClient.storeFile(toFile, in);
        in.close();
        ftpClientPool.returnObject(ftpClient);
        return flag;
    }

    /**
     * 获取文件输入流
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static InputStream getFileInputStream(String fileName) throws Exception {
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        FTPClient ftpClient = ftpClientPool.borrowObject();
        ftpClient.retrieveFile(fileName, fos);
        ByteArrayInputStream in = new ByteArrayInputStream(fos.toByteArray());
        fos.close();
        ftpClientPool.returnObject(ftpClient);
        return in;
    }


    /**
     * 下载文件至本地
     *
     * @param remoteFile
     * @param localFile
     * @return
     */
    public static boolean downloadFile(String remoteFile, String localFile) {
        FTPClient ftpClient = null;
        boolean result = false;
        OutputStream out = null;
        try {
            ftpClient = ftpClientPool.borrowObject();
            out = new FileOutputStream(localFile);
            Stopwatch stopwatch = Stopwatch.createStarted();
            ftpClient.retrieveFile(remoteFile, out);
            stopwatch.stop();
            log.debug(String.format("downloadFile remote={%s} local={%s},use time={%dms}",
                    remoteFile,
                    localFile,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS))
            );
            int downloadReply = ftpClient.getReplyCode();
            Preconditions.checkState(FTPReply.isPositiveCompletion(downloadReply),
                    "failed downloading file [%s]! reply=%s", remoteFile, downloadReply);
            out.flush();
            out.close();
            ftpClient.deleteFile(remoteFile);
            int deleteReply = ftpClient.getReplyCode();
            Preconditions.checkState(FTPReply.isPositiveCompletion(deleteReply),
                    "failed delete file [%s]! reply=%s", remoteFile, deleteReply);
            result = true;
        } catch (Exception e) {
            log.error("FTPUtil downloadFile error:", e);
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (null != ftpClient) {
                    ftpClientPool.returnObject(ftpClient);
                }
            } catch (Exception e) {
                log.error("FTPUtil downloadFile error:", e);
            }
        }
        return result;
    }

    /**
     * 下载文件输出字节数组
     *
     * @param remoteFile
     * @return
     */
    public static byte[] downloadFile(String remoteFile) {
        FTPClient ftpClient = null;
        byte[] bytes = null;
        ByteArrayOutputStream out = null;
        try {
            ftpClient = ftpClientPool.borrowObject();
            out = new ByteArrayOutputStream();
            Stopwatch stopwatch = Stopwatch.createStarted();
            ftpClient.retrieveFile(remoteFile, out);
            log.debug("downloadFile size={} ，use time={}", out.size(), stopwatch.stop());
            int reply = ftpClient.getReplyCode();
            Preconditions.checkState(FTPReply.isPositiveCompletion(reply),
                    "failed downloading file [%s]! reply=%s", remoteFile, reply);
            bytes = out.toByteArray();
        } catch (NoRouteToHostException e) {
            log.error("FTPUtil downloadFile error:", e);
            try {
                ftpClientPool.invalidateObject(ftpClient);
            } catch (Exception e1) {
                log.error("FTPUtil downloadFile error:", e1);
            }
        } catch (IOException e) {
            log.error("FTPUtil downloadFile error:", e);
        } catch (Exception e) {
            log.error("FTPUtil downloadFile error:", e);
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
            } catch (Exception e) {
                log.error("FTPUtil downloadFile error:", e);
            }

            try {
                if (null != ftpClient) {
                    ftpClientPool.returnObject(ftpClient);
                }
            } catch (Exception e) {
                log.error("FTPUtil downloadFile error:", e);
            }

        }
        return bytes;
    }


    /**
     * 从ftp中获取文件流
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    public static InputStream getInputStream(String filePath) throws Exception {
        FTPClient ftpClient = ftpClientPool.borrowObject();
        InputStream inputStream = ftpClient.retrieveFileStream(filePath);
        ftpClientPool.returnObject(ftpClient);
        return inputStream;
    }

    /**
     * 删除远程文件
     *
     * @param remoteFile
     */
    public static void deleteFile(String remoteFile) {
        FTPClient ftpClient = null;
        try {
            ftpClient = ftpClientPool.borrowObject();
            ftpClient.changeWorkingDirectory(FTPConfig.workingDirectory);
            ftpClient.deleteFile(remoteFile);
            int reply = ftpClient.getReplyCode();
            Preconditions.checkState(FTPReply.isPositiveCompletion(reply),
                    "failed delete file [%s]! reply=%s", remoteFile, reply);
        } catch (Exception e) {
            log.error("FTPUtil deleteFile error:", e);
        } finally {
            try {
                if (null != ftpClient) {
                    ftpClientPool.returnObject(ftpClient);
                }
            } catch (Exception e) {
                log.error("FTPUtil deleteFile error:", e);
            }

        }
    }


    /**
     * ftp中文件重命名
     *
     * @param fromFile
     * @param toFile
     * @return
     * @throws Exception
     */
    public static boolean rename(String fromFile, String toFile) throws Exception {
        FTPClient ftpClient = ftpClientPool.borrowObject();
        boolean result = ftpClient.rename(fromFile, toFile);
        log.debug(String.format("rename fromFile {%s} toFile {%s},result {%b}", fromFile, toFile, result));
        ftpClientPool.returnObject(ftpClient);
        return result;
    }

    /**
     * 获取ftp目录下的所有文件
     *
     * @param dir
     * @return
     */
    public static FTPFile[] getFiles(String dir) {
        FTPClient ftpClient = null;
        FTPFile[] files = new FTPFile[0];
        try {
            ftpClient = ftpClientPool.borrowObject();
            files = ftpClient.listFiles(dir);
        } catch (Throwable thr) {
            log.error("FTPUtil getFiles error:", thr);
        } finally {
            try {
                if (null != ftpClient) {
                    ftpClientPool.returnObject(ftpClient);
                }
            } catch (Exception e) {
                log.error("FTPUtil getFiles error:", e);
            }

        }
        return files;
    }

    /**
     * 获取ftp目录下的某种类型的文件
     *
     * @param dir
     * @param filter
     * @return
     */
    public static FTPFile[] getFiles(String dir, FTPFileFilter filter) {
        FTPClient ftpClient = null;
        FTPFile[] files = new FTPFile[0];
        try {
            ftpClient = ftpClientPool.borrowObject();
            ftpClient.changeWorkingDirectory(dir);
            files = ftpClient.listFiles(dir, filter);
        } catch (NoRouteToHostException e) {
            log.error("FTPUtil getFiles error:", e);
            try {
                ftpClientPool.invalidateObject(ftpClient);
            } catch (Exception e1) {
                log.error("FTPUtil getFiles error:", e1);
            }
        } catch (IOException e) {
            log.error("FTPUtil getFiles error:", e);
        } catch (Exception e) {
            log.error("FTPUtil getFiles error:", e);
        } finally {
            try {
                if (null != ftpClient) {
                    ftpClientPool.returnObject(ftpClient);
                }
            } catch (Exception e) {
                log.error("FTPUtil getFiles error:", e);
            }
        }
        return files;
    }

    /**
     * 创建文件夹
     *
     * @param remoteDir
     * @return 如果已经有这个文件夹返回false
     */
    public static boolean makeDirectory(String remoteDir) throws Exception {
        FTPClient ftpClient = null;
        boolean result = false;
        try {
            ftpClient = ftpClientPool.borrowObject();
            result = ftpClient.makeDirectory(remoteDir);
        } catch (IOException e) {
            log.error("FTPUtil makeDirectory error:", e);
        } finally {
            try {
                if (null != ftpClient) {
                    ftpClientPool.returnObject(ftpClient);
                }
            } catch (Exception e) {
                log.error("FTPUtil makeDirectory error:", e);
            }

        }
        return result;
    }

    public static boolean mkdirs(String dir) throws Exception {
        boolean result = false;
        if (null == dir) {
            return result;
        }
        FTPClient ftpClient = ftpClientPool.borrowObject();
        ftpClient.changeWorkingDirectory("/");
        StringTokenizer dirs = new StringTokenizer(dir, "/");
        String temp = null;
        while (dirs.hasMoreElements()) {
            temp = dirs.nextElement().toString();
            //创建目录
            ftpClient.makeDirectory(temp);
            //进入目录
            ftpClient.changeWorkingDirectory(temp);
            result = true;
        }
        ftpClient.changeWorkingDirectory("/");
        ftpClientPool.returnObject(ftpClient);
        return result;
    }

    public static final BASE64Encoder BASE_64_ENCODER = new BASE64Encoder();

    /**
     * 获取FTP上的图片Base64编码
     *
     * @param filePathAndName 数据库中存储的图片文件名（包含部分路径和文件名）,如: <br/>
     *                        <code>;20180626\0014\手动开闸00_CRD000000_20180626142123_车场入口_01.jpg;20180626\0014\手动开闸00_CRD000000_20180626142123_车场入口_02.jpg</code>
     * @return 图片Base64编码或null
     */
    public static String encodeImgToBase64(String filePathAndName) {
        return BASE_64_ENCODER.encode(downloadFile(filePathAndName));
    }

    public static void unzip(File zipFile) throws IOException {
        ZipEntry entry;
        String entryFilePath, entryDirPath;
        File entryFile, entryDir;
        int index, count, bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        BufferedInputStream bis;
        BufferedOutputStream bos = null;
        ZipFile zip = null;
        try {
            try {
                zip = new ZipFile(zipFile, Charset.forName("gbk"));
            } catch (Exception e) {
                log.error(String.format("unzip file error {%s}", zipFile.getAbsolutePath()), e);
                return;
            }

            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();
            //循环对压缩包里的每一个文件进行解压
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                //构建压缩包中一个文件解压后保存的文件全路径
                entryFilePath = FTPConfig.ACCESS_LOG_UNZIP_DIRECTORY + entry.getName();
                log.debug(String.format("unzip file from {%s} to {%s}", zipFile.getAbsolutePath(), FTPConfig.ACCESS_LOG_UNZIP_DIRECTORY + entry.getName()));
                //构建解压后保存的文件夹路径
                index = entryFilePath.lastIndexOf(File.separator);
                if (index != -1) {
                    entryDirPath = entryFilePath.substring(0, index);
                } else {
                    entryDirPath = "";
                }
                entryDir = new File(entryDirPath);
                //如果文件夹路径不存在，则创建文件夹
                if (!entryDir.exists() || !entryDir.isDirectory()) {
                    entryDir.mkdirs();
                }

                //创建解压文件
                entryFile = new File(entryFilePath);
                if (entryFile.exists()) {
                    //检测文件是否允许删除，如果不允许删除，将会抛出SecurityException
                    SecurityManager securityManager = new SecurityManager();
                    securityManager.checkDelete(entryFilePath);
                    //删除已存在的目标文件
                    entryFile.delete();
                }
                try {
                    //写入文件
                    bos = new BufferedOutputStream(new FileOutputStream(entryFile));
                    bis = new BufferedInputStream(zip.getInputStream(entry));
                    while ((count = bis.read(buffer, 0, bufferSize)) != -1) {
                        bos.write(buffer, 0, count);
                    }
                    bos.flush();
                } catch (IOException e) {
                    log.error(String.format("unzip file error {%s}", zipFile.getAbsolutePath()), e);
                } finally {
                    if (bos != null) {
                        bos.close();
                    }
                }
            }
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
//        zipFile.delete();
    }
}