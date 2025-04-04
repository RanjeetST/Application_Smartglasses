package com.st.smartglasses;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ToastView {
    public  Toast toast;
    public ToastView(Context context, String text) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.toast_view, null);
        TextView t = (TextView) view.findViewById(R.id.toast_text);
        t.setTextColor(context.getColor(R.color.yellow));
        t.setText(text);
        t.setTextSize(16);
        if (toast != null) {
            toast.cancel();
        }
        toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }

}
