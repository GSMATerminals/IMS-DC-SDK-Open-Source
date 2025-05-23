/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.app.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.ct.ertclib.dc.core.R;

public class CircleProgressBar extends ProgressBar {
    private int mDefaultColor;
    private int mReachedColor;
    private float mDefaultHeight;
    private float mReachedHeight;
    private float mRadius;
    private Paint mPaint;

    private Status mStatus = Status.Waiting;

    public CircleProgressBar(Context context) {
        this(context, null);
    }

    public CircleProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressBar);
        //默认圆的颜色
        mDefaultColor = typedArray.getColor(R.styleable.CircleProgressBar_defaultColor, Color.parseColor("#000000"));
        //进度条的颜色
        mReachedColor = typedArray.getColor(R.styleable.CircleProgressBar_reachedColor, Color.parseColor("#FFFFFF"));
        //默认圆的高度
        mDefaultHeight = typedArray.getDimension(R.styleable.CircleProgressBar_defaultHeight, dp2px(context, 2.5f));
        //进度条的高度
        mReachedHeight = typedArray.getDimension(R.styleable.CircleProgressBar_reachedHeight, dp2px(context, 2.5f));
        //圆的半径
        mRadius = typedArray.getDimension(R.styleable.CircleProgressBar_radius, dp2px(context, 17));
        typedArray.recycle();

        setPaint();
    }

    private void setPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setDither(true);//防抖动，绘制出来的图要更加柔和清晰
        mPaint.setStyle(Paint.Style.STROKE);//设置填充样式
        /**
         *  Paint.Style.FILL    :填充内部
         *  Paint.Style.FILL_AND_STROKE  ：填充内部和描边
         *  Paint.Style.STROKE  ：仅描边
         */
        mPaint.setStrokeCap(Paint.Cap.ROUND);//设置画笔笔刷类型
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        float paintHeight = Math.max(mReachedHeight, mDefaultHeight);//比较两数，取最大值

        if (heightMode != MeasureSpec.EXACTLY) {
            int exceptHeight = (int) (getPaddingTop() + getPaddingBottom() + mRadius * 2 + paintHeight * 2);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(exceptHeight, MeasureSpec.EXACTLY);
        }
        if (widthMode != MeasureSpec.EXACTLY) {
            int exceptWidth = (int) (getPaddingLeft() + getPaddingRight() + mRadius * 2 + paintHeight * 2);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(exceptWidth, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(getPaddingStart() + mDefaultHeight, getPaddingTop() + mDefaultHeight);

        int mDiameter = (int) (mRadius * 2);
        if (mStatus == Status.Loading) {
            //绘制背景
            mPaint.setColor(Color.BLACK);
            mPaint.setAlpha(100);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);

            //画默认圆(边框)的一些设置
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mDefaultColor);
            mPaint.setAlpha(200);
            mPaint.setStrokeWidth(mDefaultHeight);
            canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);

            //画进度条的一些设置
            mPaint.setAlpha(255);
            mPaint.setColor(mReachedColor);
            mPaint.setStrokeWidth(mReachedHeight);
            //根据进度绘制圆弧
            float sweepAngle = getProgress() * 1.0f / getMax() * 360;
            canvas.drawArc(new RectF(0, 0, mRadius * 2, mRadius * 2), -90, sweepAngle, false, mPaint);

            //绘制进度
            String progress = getProgress() + "%";
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(dp2px(getContext(), 4));
            mPaint.setColor(Color.parseColor("#FFFFFF"));
            mPaint.setTextSize(25);
            mPaint.setTextAlign(Paint.Align.CENTER);

            Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
            float distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
            float baseline = getMeasuredHeight() / 2 + distance;

            canvas.drawText(progress, getMeasuredWidth() / 2 - mDefaultHeight, baseline - mDefaultHeight, mPaint);

        } else {
            int drawableInt;
            switch (mStatus) {
                case Waiting:
                default:
                    drawableInt = R.drawable.ic_resume;
                    break;
                case Pause:
                    drawableInt = R.drawable.ic_download;
                    break;
                case Finish:
                    drawableInt = R.drawable.ic_finish;
                    break;
                case Error:
                    drawableInt = R.drawable.ic_resume;
                    break;
            }
            Drawable drawable = getContext().getResources().getDrawable(drawableInt);
            drawable.setBounds(0, 0, mDiameter, mDiameter);
            drawable.draw(canvas);
        }
        canvas.restore();
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setStatus(Status status) {
        if (mStatus == status) return;
        mStatus = status;
        invalidate();
    }

    public enum Status {
        Waiting,
        Pause,
        Loading,
        Error,
        Finish
    }

    private float dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return dp * scale + 0.5f;
    }
}