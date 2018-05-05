package com.rae.cnblogs.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.rae.cnblogs.AppRoute;
import com.rae.cnblogs.R;
import com.rae.cnblogs.message.FontChangedEvent;
import com.rae.cnblogs.widget.RaeSeekBar;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;

/**
 * 字体设置
 * Created by ChenRui on 2017/10/12 0012 23:30.
 */
@Route(path = AppRoute.PATH_FONT_SETTING)
public class FontSettingActivity extends SwipeBackBasicActivity {
    @BindView(R.id.tv_message)
    TextView mMessage;
    @BindView(R.id.seekBar)
    RaeSeekBar mSeekBar;
    private boolean mFontChanged;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_setting);
        showHomeAsUp();

        int size = config().getPageTextSize();
        if (size > 0) {
            mSeekBar.setTextSize(size);
            mMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                int size = mSeekBar.getRawTextSize(value);
                mMessage.setTextSize(size);
                mFontChanged = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        // 保存设置
        config().setPageTextSize(mSeekBar.getTextSize(mSeekBar.getProgress()));
        super.onDestroy();
        if (mFontChanged) {
            // 通知
            EventBus.getDefault().post(new FontChangedEvent());
        }
    }
}
