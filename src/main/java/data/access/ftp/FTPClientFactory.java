package data.access.ftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool.PoolableObjectFactory;

@Slf4j
@SuppressWarnings("all")
public class FTPClientFactory implements PoolableObjectFactory<FTPClient> {

    @Override
    public FTPClient makeObject() throws Exception {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setControlEncoding(FTPConfig.encoding);
        ftpClient.setConnectTimeout(FTPConfig.clientTimeout);
        try {
            ftpClient.connect(FTPConfig.host, FTPConfig.port);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log.error("FTPServer refused connection");
                return null;
            }
            boolean result = ftpClient.login(FTPConfig.username, FTPConfig.password);
            ftpClient.setFileType(FTPConfig.transferFileType);
            //被动模式
            ftpClient.enterLocalPassiveMode();

            if (!result) {
                log.error("ftpClient login failed... username is {}", FTPConfig.username);
            }else {
                log.info("ftpClient login success... username is {}", FTPConfig.username);
            }
        } catch (Exception e) {
            log.error("create hikftp connection failed...{}", e);
            throw e;
        }

        return ftpClient;
    }

    @Override
    public void destroyObject(FTPClient ftpClient) throws Exception {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.logout();
            }
        } catch (Exception e) {
            log.error("hikftp client logout failed...{}", e);
            throw e;
        } finally {
            if (ftpClient != null) {
                ftpClient.disconnect();
            }
        }

    }

    @Override
    public boolean validateObject(FTPClient ftpClient) {
        try {
            return ftpClient.sendNoOp();
        } catch (Exception e) {
            log.error("Failed to validate client: {}");
        }
        return false;
    }

    @Override
    public void activateObject(FTPClient obj) throws Exception {

    }

    @Override
    public void passivateObject(FTPClient obj) throws Exception {
    }
}