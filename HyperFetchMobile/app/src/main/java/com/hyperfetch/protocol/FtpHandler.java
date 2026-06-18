package com.hyperfetch.protocol;

import com.hyperfetch.model.ResourceMeta;
import com.hyperfetch.rate.RateLimiter;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;

/**
 * FTP协议处理器实现
 * 支持FTP协议的资源探测和下载
 */
public class FtpHandler implements ProtocolHandler {

    private static final String FTP_PREFIX = "ftp://";

    /**
     * FTP客户端配置
     */
    private String username = "anonymous";
    private String password = "";
    private int connectTimeout = 30000; // 30秒
    private int dataTimeout = 60000;    // 60秒

    /**
     * 默认缓冲区大小（8KB）
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * 默认构造函数
     */
    public FtpHandler() {
    }

    /**
     * 带认证信息的构造函数
     *
     * @param username FTP用户名
     * @param password FTP密码
     */
    public FtpHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean supports(String url) {
        if (url == null) {
            return false;
        }
        return url.toLowerCase().startsWith(FTP_PREFIX);
    }

    @Override
    public ResourceMeta probe(String url) throws IOException {
        FTPClient ftpClient = createFtpClient();

        try {
            // 解析URL获取主机、端口和路径
            FtpUrlInfo info = parseFtpUrl(url);

            // 连接FTP服务器
            connect(ftpClient, info.host, info.port);

            // 获取文件大小
            long fileSize = getFileSize(ftpClient, info.path);

            // 创建资源元数据
            ResourceMeta meta = new ResourceMeta();
            meta.setTotalSize(fileSize);
            // FTP协议本身支持断点续传（REST命令）
            meta.setSupportRange(true);
            // 根据文件扩展名猜测内容类型
            meta.setContentType(guessContentType(info.path));

            return meta;
        } finally {
            disconnect(ftpClient);
        }
    }

    @Override
    public void download(String url, long start, long end, RateLimiter limiter, ProgressCallback cb) throws IOException {
        FTPClient ftpClient = createFtpClient();

        try {
            // 解析URL
            FtpUrlInfo info = parseFtpUrl(url);

            // 连接FTP服务器
            connect(ftpClient, info.host, info.port);

            // 设置断点续传起始位置
            ftpClient.setRestartOffset(start);

            long total = end - start + 1;
            long downloaded = 0;

            // 获取输入流
            try (InputStream inputStream = ftpClient.retrieveFileStream(info.path)) {
                if (inputStream == null) {
                    throw new IOException("无法获取文件流，FTP响应码: " + ftpClient.getReplyCode());
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long remaining = total;

                while (remaining > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    // 应用限速控制
                    if (limiter != null) {
                        try {
                            limiter.acquire(bytesRead);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("下载被中断", e);
                        }
                    }

                    // TODO: 将数据写入文件，这里需要外部提供文件路径或输出流
                    downloaded += bytesRead;
                    remaining -= bytesRead;

                    // 回调进度
                    if (cb != null) {
                        cb.onProgress(downloaded, total);
                    }
                }

                // 完成下载
                ftpClient.completePendingCommand();

                // 下载完成回调
                if (cb != null) {
                    cb.onComplete();
                }
            }
        } catch (Exception e) {
            // 错误回调
            if (cb != null) {
                cb.onError(e instanceof IOException ? (IOException) e : new IOException(e));
            }
            throw e;
        } finally {
            disconnect(ftpClient);
        }
    }

    /**
     * 下载指定范围的数据到文件
     *
     * @param url      资源URL地址
     * @param start    起始字节位置（包含）
     * @param end      结束字节位置（包含）
     * @param filePath 目标文件路径
     * @param limiter  限速器
     * @param cb       进度回调接口
     * @throws IOException 当下载失败时抛出
     */
    public void downloadToFile(String url, long start, long end, String filePath,
                               RateLimiter limiter, ProgressCallback cb) throws IOException {
        FTPClient ftpClient = createFtpClient();

        try {
            // 解析URL
            FtpUrlInfo info = parseFtpUrl(url);

            // 连接FTP服务器
            connect(ftpClient, info.host, info.port);

            // 设置断点续传起始位置
            ftpClient.setRestartOffset(start);

            long total = end - start + 1;
            long downloaded = 0;

            // 使用RandomAccessFile支持断点续传写入
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
                 InputStream inputStream = ftpClient.retrieveFileStream(info.path)) {

                if (inputStream == null) {
                    throw new IOException("无法获取文件流，FTP响应码: " + ftpClient.getReplyCode());
                }

                // 定位到起始位置
                raf.seek(start);

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long remaining = total;

                while (remaining > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    // 应用限速控制
                    if (limiter != null) {
                        try {
                            limiter.acquire(bytesRead);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("下载被中断", e);
                        }
                    }

                    // 写入文件
                    raf.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    remaining -= bytesRead;

                    // 回调进度
                    if (cb != null) {
                        cb.onProgress(downloaded, total);
                    }
                }

                // 完成下载
                ftpClient.completePendingCommand();

                // 下载完成回调
                if (cb != null) {
                    cb.onComplete();
                }
            }
        } catch (Exception e) {
            // 错误回调
            if (cb != null) {
                cb.onError(e instanceof IOException ? (IOException) e : new IOException(e));
            }
            throw e;
        } finally {
            disconnect(ftpClient);
        }
    }

    /**
     * 创建FTP客户端实例
     *
     * @return FTP客户端
     */
    private FTPClient createFtpClient() {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(connectTimeout);
        ftpClient.setDataTimeout(dataTimeout);
        return ftpClient;
    }

    /**
     * 连接FTP服务器
     *
     * @param ftpClient FTP客户端
     * @param host      主机地址
     * @param port      端口号
     * @throws IOException 连接失败时抛出
     */
    private void connect(FTPClient ftpClient, String host, int port) throws IOException {
        ftpClient.connect(host, port);

        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new IOException("FTP服务器拒绝连接，响应码: " + reply);
        }

        // 登录
        if (!ftpClient.login(username, password)) {
            ftpClient.disconnect();
            throw new IOException("FTP登录失败，用户名或密码错误");
        }

        // 设置被动模式
        ftpClient.enterLocalPassiveMode();
        // 设置二进制传输模式
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    /**
     * 断开FTP连接
     *
     * @param ftpClient FTP客户端
     */
    private void disconnect(FTPClient ftpClient) {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            // 忽略断开连接时的错误
        }
    }

    /**
     * 获取FTP文件大小
     *
     * @param ftpClient FTP客户端
     * @param path      文件路径
     * @return 文件大小（字节）
     * @throws IOException 获取失败时抛出
     */
    private long getFileSize(FTPClient ftpClient, String path) throws IOException {
        long size = ftpClient.getSize(path);
        if (size < 0) {
            throw new IOException("无法获取文件大小: " + path);
        }
        return size;
    }

    /**
     * 解析FTP URL
     *
     * @param url FTP URL
     * @return URL解析信息
     * @throws IOException URL格式错误时抛出
     */
    private FtpUrlInfo parseFtpUrl(String url) throws IOException {
        // 移除ftp://前缀
        String remaining = url.substring(FTP_PREFIX.length());

        // 解析用户名和密码（如果有）
        String hostPart = remaining;
        String user = this.username;
        String pass = this.password;

        int atIndex = remaining.indexOf('@');
        if (atIndex > 0) {
            String userInfo = remaining.substring(0, atIndex);
            hostPart = remaining.substring(atIndex + 1);

            int colonIndex = userInfo.indexOf(':');
            if (colonIndex > 0) {
                user = userInfo.substring(0, colonIndex);
                pass = userInfo.substring(colonIndex + 1);
            } else {
                user = userInfo;
            }
        }

        // 解析主机和端口
        String host;
        int port = 21; // 默认FTP端口
        String path;

        int slashIndex = hostPart.indexOf('/');
        if (slashIndex > 0) {
            String hostPort = hostPart.substring(0, slashIndex);
            path = hostPart.substring(slashIndex);

            int colonIndex = hostPort.indexOf(':');
            if (colonIndex > 0) {
                host = hostPort.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(hostPort.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    throw new IOException("无效的端口号: " + hostPort.substring(colonIndex + 1));
                }
            } else {
                host = hostPort;
            }
        } else {
            throw new IOException("FTP URL缺少文件路径: " + url);
        }

        // 更新用户名密码（如果URL中包含）
        if (atIndex > 0) {
            this.username = user;
            this.password = pass;
        }

        return new FtpUrlInfo(host, port, path, user, pass);
    }

    /**
     * 根据文件扩展名猜测内容类型
     *
     * @param path 文件路径
     * @return 猜测的MIME类型
     */
    private String guessContentType(String path) {
        if (path == null) {
            return "application/octet-stream";
        }

        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".zip")) {
            return "application/zip";
        } else if (lowerPath.endsWith(".tar")) {
            return "application/x-tar";
        } else if (lowerPath.endsWith(".gz")) {
            return "application/gzip";
        } else if (lowerPath.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerPath.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerPath.endsWith(".html") || lowerPath.endsWith(".htm")) {
            return "text/html";
        } else if (lowerPath.endsWith(".xml")) {
            return "application/xml";
        } else if (lowerPath.endsWith(".json")) {
            return "application/json";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lowerPath.endsWith(".mp4")) {
            return "video/mp4";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * 设置连接超时时间
     *
     * @param connectTimeout 超时时间（毫秒）
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * 设置数据传输超时时间
     *
     * @param dataTimeout 超时时间（毫秒）
     */
    public void setDataTimeout(int dataTimeout) {
        this.dataTimeout = dataTimeout;
    }

    /**
     * 设置FTP认证信息
     *
     * @param username 用户名
     * @param password 密码
     */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * FTP URL解析结果内部类
     */
    private static class FtpUrlInfo {
        final String host;
        final int port;
        final String path;
        final String username;
        final String password;

        FtpUrlInfo(String host, int port, String path, String username, String password) {
            this.host = host;
            this.port = port;
            this.path = path;
            this.username = username;
            this.password = password;
        }
    }
}