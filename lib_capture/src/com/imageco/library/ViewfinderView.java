/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.imageco.library;

import java.util.Collection;
import java.util.HashSet;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

/**
 * This viewImpl is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    /**
     * Field SCANNER_ALPHA
     */
    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    /**
     * Field ANIMATION_DELAY
     */
    private static final long ANIMATION_DELAY = 100L;
    /**
     * Field OPAQUE
     */
    private static final int OPAQUE = 0xFF;

    /**
     * Field paint
     */
    private final Paint paint;
    /**
     * Field resultBitmap
     */
    private Bitmap resultBitmap;
    /**
     * Field maskColor
     */
    private final int maskColor;
    /**
     * Field resultColor
     */
    private final int resultColor;
    /**
     * Field frameColor
     */
    private final int frameColor;
    /**
     * Field laserColor
     */
    private final int laserColor;
    /**
     * Field resultPointColor
     */
    private final int resultPointColor;
    /**
     * Field scannerAlpha
     */
    private int scannerAlpha;
    /**
     * Field possibleResultPoints
     */
    private Collection<ResultPoint> possibleResultPoints;
    /**
     * Field lastPossibleResultPoints
     */
    private Collection<ResultPoint> lastPossibleResultPoints;

    /**
     * Constructor ViewfinderView creates a new ViewfinderView instance.
     *
     * @param context of type Context
     * @param attrs   of type AttributeSet
     */
    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        frameColor = resources.getColor(R.color.viewfinder_frame);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new HashSet<ResultPoint>(5);
    }

    /**
     * Method onDraw ...
     *
     * @param canvas of type Canvas
     */
    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {

            // Draw a two pixel solid black border inside the framing rect
            paint.setColor(frameColor);
            paint.setFlags(2);
//            canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
//            canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
//            canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
//            canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);
            canvas.drawRect(frame.left+15, frame.top+15, frame.left+50+15, frame.top+3+15, paint);
            canvas.drawRect(frame.left+15, frame.top+15, frame.left+3+15, frame.top+50+15, paint);
            
            canvas.drawRect(frame.right-50-15 ,frame.top+15,frame.right-15,frame.top+3+15, paint);
            canvas.drawRect(frame.right-2-15,frame.top+15,frame.right+1-15,frame.top+50+15, paint);
            
            canvas.drawRect(frame.left+15,frame.bottom-49-15,frame.left+3+15,frame.bottom+1-15, paint);
            canvas.drawRect(frame.left+15,frame.bottom-2-15,frame.left+50+15,frame.bottom+1-15, paint);
            
            canvas.drawRect(frame.right-50-15,frame.bottom-2-15,frame.right-15,frame.bottom+1-15, paint);
            canvas.drawRect(frame.right-2-15,frame.bottom-49-15,frame.right+1-15,frame.bottom+1-15, paint);
            
            // Draw a red "laser scanner" line through the middle to show decoding is active
//            paint.setColor(laserColor);
//            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
//            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
//            int middle = frame.height() / 2 + frame.top;
//            int middle2 = (frame.width()>>>1) + frame.left;
//            canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
//            canvas.drawRect(middle2-1,frame.top,middle2+2,frame.bottom, paint);

            Collection<ResultPoint> currentPossible = possibleResultPoints;
            Collection<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new HashSet<ResultPoint>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(OPAQUE);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
                }
            }
            if (currentLast != null) {
                paint.setAlpha(OPAQUE / 2);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    /**
     * Method drawViewfinder ...
     */
    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    /**
     * Method addPossibleResultPoint ...
     *
     * @param point of type ResultPoint
     */
    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

}
