package com.crash.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.core.content.FileProvider;

import com.crash.utils.CrashHandler;
import com.crash.utils.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @Author tgl
 * @Date 2022/5/5 下午1:24
 * @Version 1.0
 * @Description 显示错误日志列表
 */
public class ShowLogDialog {

    /**
     * 长按弹出
     *
     * @param window
     */
    public static void showLongPress(Window window) {
        showLongPress(window, 5000);
    }

    /**
     * 长按弹出
     *
     * @param window
     * @param delaySecond 默认5秒
     */
    public static void showLongPress(Window window, long delaySecond) {
        window.getDecorView().setOnTouchListener(new View.OnTouchListener() {
            long t;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        t = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (System.currentTimeMillis() - t > delaySecond) {
                            show(window.getContext());
                        }
                        t = System.currentTimeMillis();
                        break;
                }
                return false;
            }
        });
    }

    public static void showContinuousClick(Window window) {
        window.getDecorView().setOnTouchListener(new View.OnTouchListener() {
            long t;
            int count;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (System.currentTimeMillis() - t < 1500) {
                            count++;
                        }
                        t = System.currentTimeMillis();
                        if (count > 8) {
                            show(window.getContext());
                            count = 0;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return false;
            }
        });
    }

    public static void show(Context context) {
        if (!CrashHandler.isDebug()) {
            return;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.layout_log, null);
        Dialog dialog = new Dialog(context);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(view);
        dialog.getWindow().getAttributes().height = (int) (context.getResources().getDisplayMetrics().widthPixels * 1.2);
        dialog.getWindow().getAttributes().width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9);
        ListView listView = view.findViewById(R.id.list_view);

        List<File> lists = CrashHandler.getCrashLogs();
        List<String> listName = new ArrayList<>();
        for (File f : lists
        ) {
            listName.add(f.getName());
        }
        listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, listName));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                Uri shareFileUri = Uri.fromFile(lists.get(i));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    shareFileUri = FileProvider.getUriForFile(context,
                            context.getPackageName() + ".provider", lists.get(i));
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                shareIntent.setType("*/*");
                shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(shareIntent);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                String data = CrashHandler.getCrashLogsStrings().get(i);
                Intent intent = new Intent(context, ShowDetailActivity.class);
                intent.putExtra("data", data);
                context.startActivity(intent);
                return true;
            }
        });
        dialog.show();
    }
}
