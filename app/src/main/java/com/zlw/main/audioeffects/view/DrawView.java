package com.zlw.main.audioeffects.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.zlw.main.audioeffects.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaolewei on 2018/8/17.
 */
public class DrawView extends View {

    /**
     * 频谱数量
     */
    private static final int LUMP_COUNT = 128;
    private static final int LUMP_WIDTH = 4;
    private static final int LUMP_SPACE = 2;
    private static final int LUMP_MIN_HEIGHT = LUMP_WIDTH;
    private static final int LUMP_MAX_HEIGHT = 200;//TODO: HEIGHT
    private static final int LUMP_SIZE = LUMP_WIDTH + LUMP_SPACE;
    private static final int LUMP_COLOR = Color.parseColor("#6de8fd");

    private static final int WAVE_SAMPLING_INTERVAL = 3;

    private static final float SCALE = LUMP_MAX_HEIGHT / LUMP_COUNT;

    private ShowStyle upShowStyle = ShowStyle.STYLE_HOLLOW_LUMP;
    private ShowStyle downShowStyle = ShowStyle.STYLE_WAVE;

    private byte[] waveData;
    List<Point> pointList;

    private Paint lumpPaint;
    Path wavePath = new Path();


    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        lumpPaint = new Paint();
        lumpPaint.setAntiAlias(true);
        lumpPaint.setColor(LUMP_COLOR);



//        lumpPaint.setDither(true);
//        lumpPaint.setStrokeJoin(Paint.Join.MITER);

        //线性曲线
        lumpPaint.setStrokeWidth(2);
        lumpPaint.setStyle(Paint.Style.STROKE);
        //填充图形
//        lumpPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
//        lumpPaint.setStyle(Paint.Style.FILL);

    }

    public void setWaveData(byte[] data) {
        this.waveData = readyData(data);
        genSamplingPoint(data);
        invalidate();
    }


    public void setStyle(ShowStyle upShowStyle, ShowStyle downShowStyle) {
        this.upShowStyle = upShowStyle;
        this.downShowStyle = downShowStyle;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        wavePath.reset();

        wavePath.moveTo(100, 100);
//        wavePath.cubicTo(100, 100, 118, 0, 136, 100);


//        wavePath.cubicTo(118, 0, 118, 0, 136, 100);

//        wavePath.cubicTo(100, 100, 100,50,118, 0);
//        wavePath.cubicTo(118, 0, 118,50,136, 100);
        wavePath.quadTo(109, 150, 118,200);

        canvas.drawPath(wavePath, lumpPaint);
    }

    /**
     * 预处理数据
     *
     * @return
     */
    private static byte[] readyData(byte[] fft) {
        byte[] newData = new byte[LUMP_COUNT];
        byte abs;
        for (int i = 0; i < LUMP_COUNT; i++) {
            abs = (byte) Math.abs(fft[i]);
            //描述：Math.abs -128时越界
            newData[i] = abs < 0 ? 127 : abs;
        }
        return newData;
    }

    /**
     * 绘制曲线
     * 绘制右上曲线
     *
     * @param canvas
     * @param i
     * @param reversal
     */
    private void drawWave(Canvas canvas, int i, boolean reversal) {
        if (pointList == null || pointList.size() < 2) {
            return;
        }
        float ratio = SCALE * (reversal ? -1 : 1);
        if (i <= pointList.size() - 2) {
            Point point = pointList.get(i);
            Point nextPoint = pointList.get(i + 1);

            int midX = (LUMP_SIZE * LUMP_COUNT);
            if (i == 0) {
                /**
                 * 起点移到中间位置
                 */
                wavePath.moveTo(midX, LUMP_MAX_HEIGHT - point.y * ratio);
            }

            /**
             * 高度低于8的不显示
             */
            float y1 = 0;
            float y2 = 0;
            if (point.y < 8) {
                y1 = 0;
            } else {
                y1 = point.y;
            }
            if (nextPoint.y < 8) {
                y2 = 0;
            } else {
                y2 = nextPoint.y;
            }

            wavePath.cubicTo(midX + point.x, LUMP_MAX_HEIGHT - y1 * ratio,
                    midX + point.x, LUMP_MAX_HEIGHT - y2 * ratio,
                    midX + nextPoint.x, LUMP_MAX_HEIGHT - y2 * ratio);

            /**
             * 二阶贝塞尔曲线，不够圆滑
             */
//            wavePath.quadTo(midX+point.x,LUMP_MAX_HEIGHT - point.y * ratio,midX+nextPoint.x, LUMP_MAX_HEIGHT - nextPoint.y * ratio);

//            Logger.d("tenda", "point.x:" + point.x + " nextPoint.x:" + nextPoint.x);

            canvas.drawPath(wavePath, lumpPaint);
        }
    }


    /**
     * 绘制矩形条
     */
    private void drawLump(Canvas canvas, int i, boolean reversal) {
        int minus = reversal ? -1 : 1;

        if (waveData[i] < 0) {
            Logger.w("waveData", "waveData[i] < 0 cacheData: %s", waveData[i]);
        }
        float top = (LUMP_MAX_HEIGHT - (LUMP_MIN_HEIGHT + waveData[i] * SCALE) * minus);

        canvas.drawRect(LUMP_SIZE * i,
                top,
                LUMP_SIZE * i + LUMP_WIDTH,
                LUMP_MAX_HEIGHT,
                lumpPaint);
    }

    /**
     * 生成波形图的采样数据，减少计算量
     *
     * @param data
     */
    private void genSamplingPoint(byte[] data) {
        if (upShowStyle != ShowStyle.STYLE_WAVE && downShowStyle != ShowStyle.STYLE_WAVE) {
            return;
        }
        if (pointList == null) {
            pointList = new ArrayList<>();
        } else {
            pointList.clear();
        }
        pointList.add(new Point(0, 0));
        for (int i = WAVE_SAMPLING_INTERVAL; i < LUMP_COUNT; i += WAVE_SAMPLING_INTERVAL) {
            pointList.add(new Point(LUMP_SIZE * i, waveData[i]));
        }
        pointList.add(new Point(LUMP_SIZE * LUMP_COUNT, 0));
    }


    /**
     * 可视化样式
     */
    public enum ShowStyle {
        /**
         * 空心的矩形小块
         */
        STYLE_HOLLOW_LUMP,

        /**
         * 曲线
         */
        STYLE_WAVE,

        /**
         * 不显示
         */
        STYLE_NOTHING
    }
}

