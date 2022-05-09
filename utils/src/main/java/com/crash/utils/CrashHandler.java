package com.crash.utils;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @Author tgl
 * @Date 2022/5/5 上午11:07
 * @Version 1.0
 * @Description 崩溃日志抓取
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    //单例
    private volatile static CrashHandler instance;

    //异常日志路径
    private File mLogPath;

    //设备信息
    private String mDeviceInfo;

    //默认的主线程异常处理器
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private static Context appContext;
    private static boolean DEBUG;
    /**
     * 同时写入外部文件夹
     */
    private static boolean mWriteExternal;

    public static Context getAppContext() {
        return appContext;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void init(Context context) {
        init(context, true, false);
    }

    //在application中初始化
    public static void init(Context context, boolean debug, boolean writeExternal) {
        DEBUG = debug;
        mWriteExternal = writeExternal;
        if (!debug) {
            return;
        }
        if (context instanceof Application) {
            appContext = context.getApplicationContext();
        } else {
            appContext = context;
        }
        CrashHandler crashHandler = getInstance();
        crashHandler.mLogPath = getCrashCacheDir(context);
        crashHandler.mDeviceInfo = CrashHandler.getDeviceInfo(context);
        crashHandler.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置主线程处理器为自定义处理器
        Thread.setDefaultUncaughtExceptionHandler(crashHandler);
    }

    //获取部分设备信息
    private static String getDeviceInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        //厂商，例如Xiaomi，Meizu，Huawei等。
        sb.append("brand: ").append(Build.BRAND).append("\n");
        //产品型号全称，例如 meizu_mx3
        sb.append("product: ").append(Build.PRODUCT).append("\n");
        //产品型号名，例如 mx3
        sb.append("device: ").append(Build.DEVICE).append("\n");
        //安卓版本名，例如 4.4.4
        sb.append("androidVersionName: ").append(Build.VERSION.RELEASE).append("\n");
        //安卓API版本，例如 19
        sb.append("androidApiVersion: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("fingerprint: ").append(Build.FINGERPRINT).append("\n");
        try {
            PackageManager pkgMgr = context.getPackageManager();
            PackageInfo pkgInfo = pkgMgr.getPackageInfo(context.getPackageName(), 0);
            sb.append("packageName: ").append(pkgInfo.packageName).append("\n");
            sb.append("versionCode: ").append(pkgInfo.versionCode).append("\n");
            sb.append("versionName: ").append(pkgInfo.versionName).append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    //获取日志缓存目录，不用权限
    private static File getCrashCacheDir(Context context) {
        String cachePath;
        if (context.getExternalCacheDir() != null) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getFilesDir().getPath();
        }

        File dir = new File(cachePath + File.separator + "log");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    //DCL双重校验
    public static CrashHandler getInstance() {
        if (instance == null) {
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler();
                }
            }
        }
        return instance;
    }


    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        //自行处理异常
        handleException(ex);
        //自行处理完再交给默认处理器
        if (null != mDefaultHandler) {
            mDefaultHandler.uncaughtException(thread, ex);
        }
    }

    //处理异常
    private void handleException(final Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        //附加设备信息
        sb.append(mDeviceInfo);
        sb.append("\n");
        //获取异常信息
        getExceptionInfo(sb, throwable);
        //保存到文件
        saveCrash2File(mLogPath.getAbsolutePath(), sb.toString());
        delMore();
        writeExternalStorageDir(sb.toString());
    }

    //获取异常信息
    private void getExceptionInfo(StringBuilder sb, Throwable throwable) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        //循环获取包装异常的异常原因
        Throwable cause = throwable.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String exStr = writer.toString();
        sb.append(exStr).append("\n");
    }


    //保存到文件
    private void saveCrash2File(String dir, String crashInfo) {
        try {
            // 用于格式化日期,作为日志文件名的一部分
            SimpleDateFormat formatter = new SimpleDateFormat("yy年M月d日 HH时mm分ss秒", Locale.CHINA);
            String time = formatter.format(new Date());
            String fileName = time + ".log";
            String filePath = dir + File.separator + fileName;
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(crashInfo.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //获取所有日志文件
    public static List<File> getCrashLogs() {
        CrashHandler crashHandler = getInstance();
        File crashDir = crashHandler.mLogPath;
        if (crashDir == null || !crashDir.exists()) {
            return new ArrayList<>();
        }
        ArrayList list = new ArrayList<>(Arrays.asList(crashDir.listFiles()));
        Collections.sort(list, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                return file.lastModified() > t1.lastModified() ? -1 : 1;
            }
        });
        //避免直接使用asList方法，产生的视图无法修改
        return list;
    }

    //读取崩溃日志文件内容并返回
    public static List<String> getCrashLogsStrings() {
        List<String> logs = new ArrayList<>();
        List<File> files = getCrashLogs();

        FileInputStream inputStream;
        for (File file : files) {
            try {
                inputStream = new FileInputStream(file);
                int len = 0;
                byte[] temp = new byte[1024];
                StringBuilder sb = new StringBuilder("");
                while ((len = inputStream.read(temp)) > 0) {
                    sb.append(new String(temp, 0, len));
                }
                inputStream.close();
                logs.add(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return logs;
    }

    //删除日志文件
    public static void removeCrashLogs(File... files) {
        removeCrashLogs(Arrays.asList(files));
    }

    //删除日志文件
    public static void removeCrashLogs(List<File> files) {
        for (File file : files) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private void delMore() {
        List<File> list = getCrashLogs();
        while (list.size() > 30) {
            File file = list.remove(list.size() - 1);
            file.delete();
        }
    }

    /**
     * 写入外部储存
     */
    private void writeExternalStorageDir(String msg) {
        if (!mWriteExternal) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            return;
        }
        File file = Environment.getExternalStorageDirectory();
        if (!file.exists() || file.isDirectory()) {
            if (!file.exists()) {
                return;
            }
            file = new File(file, Environment.DIRECTORY_DOWNLOADS);
            if (!file.exists() || file.isFile()) {
                file.mkdirs();
            }
            saveCrash2File(file.getAbsolutePath(), msg);
        }
    }
}