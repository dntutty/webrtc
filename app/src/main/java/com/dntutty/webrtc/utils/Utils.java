package com.dntutty.webrtc.utils;

import android.content.Context;
import android.view.WindowManager;

import org.apache.http.params.CoreConnectionPNames;

public class Utils {
    //    屏幕总宽度
    private static int mScreenWidth;

    public static int getWidth(Context context, int size) {
        if (mScreenWidth == 0) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                mScreenWidth = manager.getDefaultDisplay().getWidth();
            }
        }
//        只有四个人及其以下
        if (size <= 4) {
            return mScreenWidth / 2;
        } else {
            return mScreenWidth / 3;
        }
    }
//    getX getY

    public static int getX(Context context, int size, int index) {
        if (mScreenWidth == 0) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                mScreenWidth = manager.getDefaultDisplay().getWidth();
            }
        }
        if (size <= 4) {
//            当会议室只有三个人的时候 第三个人的X值偏移
            if (size == 3 && index == 2) {
                return mScreenWidth / 4;
            }
            return (index % 2) * mScreenWidth / 2;
        } else if (size <= 9) {
//            当size 5个人
            if (size == 5 || size == 8) {
                if (index == 3 || index == 6) {
                    return mScreenWidth / 6;
                }
                if (index == 4 || index == 7) {
                    return mScreenWidth / 2;
                }
            }

            if (size == 7 && index == 6) {
                return mScreenWidth / 3;
            }
            return (index % 3) * mScreenWidth / 3;
        }
        return 0;
    }

    public static int getY(Context context,int size, int index) {
        if (mScreenWidth == 0) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                mScreenWidth = manager.getDefaultDisplay().getWidth();
            }
        }
        if (size <= 4) {
            return (index / 2) * mScreenWidth / 2;
        } else if (size <= 9) {
            return (index / 3) * mScreenWidth / 3;
        }
        return 0;
    }
}
