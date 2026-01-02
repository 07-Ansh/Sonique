package com.liskovsoft.sharedutils.misc;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.LineBackgroundSpan;

// https://gist.github.com/tokudu/601320d9edb978bcbc31
public class PaddingBackgroundColorSpan implements LineBackgroundSpan {
    private int mBackgroundColor;
    private int mPadding; // in pixels
    private Rect mBgRect;

    public PaddingBackgroundColorSpan(int backgroundColor) {
        this(backgroundColor, 10);
    }

    public PaddingBackgroundColorSpan(int backgroundColor, int padding) {
        super();
        mBackgroundColor = backgroundColor;
        mPadding = padding;
        // Precreate rect for performance
        mBgRect = new Rect();
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
        final int textWidth = Math.round(p.measureText(text, start, end));
        final int paintColor = p.getColor();
        final int leftCalc = (right - left - textWidth) / 2; // assume that the text is centered
        // Draw the background
        mBgRect.set(leftCalc - mPadding * 2,
                top - (lnum == 0 ? mPadding / 2 : - (mPadding / 2)),
                leftCalc + textWidth + mPadding * 2,
                bottom + mPadding / 2);
        p.setColor(mBackgroundColor);
        c.drawRect(mBgRect, p);
        p.setColor(paintColor);
    }
}
