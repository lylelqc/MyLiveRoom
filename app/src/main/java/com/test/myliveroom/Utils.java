package com.test.myliveroom;

import android.app.Activity;
import android.content.Context;
import android.view.WindowManager;

public class Utils {

    public static int dip2px(Activity activity, int dpValue) {
        final float scale = activity.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
