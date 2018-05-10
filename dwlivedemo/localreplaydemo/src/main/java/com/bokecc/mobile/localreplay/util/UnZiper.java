package com.bokecc.mobile.localreplay.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * 解压缩核心类
 */
public class UnZiper {

    public interface UnZipListener {

        /**
         * 解压失败
         * @param e
         */
        void onError(IOException e);

        /**
         * 解压完成
         */
        void onUnZipFinish();
    }


    public static int ZIP_WAIT = 10;
    public static int ZIP_ING = 11;
    public static int ZIP_FINISH = 12;
    public static int ZIP_ERROR = 13;

    Thread unzipThread;
    UnZipListener listener;
    File oriFile;
    String dir;

    int status = ZIP_WAIT;

    /**
     * 构造函数
     * @param listener 监听器
     * @param oriFile 解压原始文件
     * @param dir 解压到的文件夹
     */
    public UnZiper(UnZipListener listener, File oriFile, String dir) {
        this.listener = listener;
        this.oriFile = oriFile;
        this.dir = dir;
    }

    /**
     * 开始解压
     */
    public void unZipFile() {
        if (unzipThread != null && unzipThread.isAlive()) {
            return;
        } else {
            unzipThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        status = ZIP_ING;

                        upZipFile(oriFile, dir);

                        oriFile.delete();
                        status = ZIP_FINISH;
                        if (listener != null) {
                            listener.onUnZipFinish();
                        }

                    } catch (IOException e) {
                        status = ZIP_ERROR;
                        if (listener != null) {
                            listener.onError(e);
                        }

                    }
                }
            });
            unzipThread.start();
        }
    }

    /**
     * 获取解压状态
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * 设置解压状态
     * @param status
     * @return
     */
    public UnZiper setStatus(int status) {
        this.status = status;
        return this;
    }

    /**
     * 解压缩功能.
     * 将zipFile文件解压到folderPath目录下.
     * @throws Exception
     */
    private int upZipFile(File zipFile, String folderPath)throws ZipException,IOException {
        //public static void upZipFile() throws Exception{
        ZipFile zFile = new ZipFile(zipFile);
        Enumeration zList = zFile.entries();
        ZipEntry ze=null;

        byte[] buf = new byte[1024 * 1000];
        while(zList.hasMoreElements()){
            ze = (ZipEntry)zList.nextElement();

            if(ze.isDirectory()){
                String dirstr = folderPath + "/" + ze.getName();
                dirstr = new String(dirstr.getBytes("8859_1"), "GB2312");
                File f = new File(dirstr);
                f.mkdirs();
                continue;
            }

            File fie = new File(folderPath, ze.getName());

            File parentDir = fie.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            OutputStream os = new BufferedOutputStream(new FileOutputStream(fie));
            InputStream is = new BufferedInputStream(zFile.getInputStream(ze));

            try {
                int readLen = 0;

                while ((readLen = is.read(buf, 0, 1024 * 1000)) != -1) {
                    os.write(buf, 0, readLen);
                }
            } finally {
                is.close();
                os.close();
            }
        }
        zFile.close();
        return 0;
    }
}
