package com.test.myliveroom;

import android.content.Context;
import android.view.WindowManager;

public class Utils {

    private static int mScreenWidth;

    public static int getWidth(Context context, int size){
        if(mScreenWidth == 0){
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if(manager != null){
                mScreenWidth = manager.getDefaultDisplay().getWidth();
            }
        }
        // 只有四个及以下
        if(size < 4){
            return mScreenWidth / 2;
        }else{
            return mScreenWidth / 3;
        }
    }

    public static int getX(Context context, int size, int index){
        if(mScreenWidth == 0){
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if(manager != null){
                mScreenWidth = manager.getDefaultDisplay().getWidth();
            }
        }

        if(size < 4){
            // 会议室三人时， 第三人的情况
            if(size == 3 && index == 2){
                // 居中 左边距为1/4
                return mScreenWidth / 4;
            }
            return (index % 2 ) * mScreenWidth / 2;
        }else if(size <= 9){
            if(size == 5){
                // 居中 第一个左边距为1/6, 第二个为1/2
                if(index == 3){
                    return mScreenWidth / 6;
                }
                if(index == 4){
                    return mScreenWidth / 2;
                }
            }
            if(size == 7 && index == 6){
                return mScreenWidth / 3;
            }
            if (size == 8) {
                if (index == 6) {
                    return mScreenWidth / 6;
                }
                if (index == 7) {
                    return mScreenWidth / 2;
                }
            }
            return (index % 3) * mScreenWidth / 3;
        }
        return 0;
    }

    public static int getY(Context context, int size, int index) {
        if (mScreenWidth == 0) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                mScreenWidth = manager.getDefaultDisplay().getWidth();
            }
        }
        if (size < 3) {
            return mScreenWidth / 4;
        } else if (size < 5) {
            if (index < 2) {
                return 0;
            } else {
                return mScreenWidth / 2;
            }
        } else if (size < 7) {
            if (index < 3) {
                return mScreenWidth / 2 - (mScreenWidth / 3);
            } else {
                return mScreenWidth / 2;
            }
        } else if (size <= 9) {
            if (index < 3) {
                return 0;
            } else if (index < 6) {
                return mScreenWidth / 3;
            } else {
                return mScreenWidth / 3 * 2;
            }

        }
        return 0;
    }

}
