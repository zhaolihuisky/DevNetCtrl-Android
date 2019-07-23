package com.aladin.devnetctrl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ColorRingView extends View {
	private static String TAG = "DevNetCtrl";
	private float cx = 0, cy = 0; //center point(cx, cy) of this view
	float maxr = 0, minr = 0;
	Bitmap bmpBg = null;
	CalcRgbHandler mCalcRgbHandler = null;
	private HandlerThread mCalcRgbHandlerThread = null;
	private Listener mListener = null;
	
	public ColorRingView(Context context) {
		super(context, null);
	}
	
	public ColorRingView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	class DrawGraphicThread extends Thread {
		public void run() {
			//Log.d(TAG, this.getClass().toString() + " run ");
			bmpBg = Bitmap.createBitmap((int)(cx*2), (int)(cy*2), Config.ARGB_8888);
			Canvas cvBg = new Canvas(bmpBg);
			//cvBg.drawColor(Color.WHITE);
			cvBg.drawColor(Color.TRANSPARENT);
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setStrokeWidth(6);
			paint.setStrokeCap(Paint.Cap.ROUND);
	
			float ri = minr;
			RectF oval = new RectF();
			oval.set(cx-ri, cy-ri, cx+ri, cy+ri);
			Path path = new Path();
			path.addArc(oval, 0, 360);
			cvBg.clipPath(path, Region.Op.DIFFERENCE);
			
			float nCnt = 360.0f;
			float sweep = 1.0f;
			float h = 0.0f, s = 0.4f, l = 0.5f;
			for(h=0; h<nCnt; h+=1.0f) {
				float start = h;
				s = 1.0f;
				float r = s*(maxr - 4);
				oval.set(cx-r, cy-r, cx+r, cy+r);
				float hsv[] = {h, s, l};
				int color = Color.HSVToColor(hsv);
				paint.setColor(color);
				cvBg.drawArc(oval, start, sweep, true, paint);
			}
			
			//float rc = 60.0f;
			//path.reset();
			//path.addCircle(cx, cy, rc, Direction.CW);
			//cvBg.clipPath(path, Region.Op.UNION);
			//paint.setStyle(Paint.Style.FILL);
			//paint.setColor(Color.WHITE);
			//cvBg.drawCircle(cx, cy, rc, paint);
			
			ColorRingView.this.postInvalidate();
		}
	}
	
	private void CalcRgb(float x, float y) {
		float dx = x - cx;
		float dy = y - cy;
		float r = (float) Math.sqrt(dx*dx + dy*dy);
		if(r <= maxr) {
			//CalcRgbThread thread = new CalcRgbThread();
			//thread.setMousePoint(x, y);
			//thread.start();
			if (mCalcRgbHandler != null) {
				Message msg = Message.obtain();
				Bundle b = new Bundle();
				b.putFloat("fx", x);
				b.putFloat("fy", y);
				msg.setData(b);
				mCalcRgbHandler.sendMessage(msg);
			}
		}
	}
	/*
	class CalcRgbThread extends Thread {
		float fx = 0.0f;
		float fy = 0.0f;
		
		public void setMousePoint(float x, float y) {
			fx = x;
			fy = y;
		}
		
		public void run() {
			//because the lum is 0.5, the r,g,b always less than 128 
			float h = 0.0f, s = 1.0f, l = 0.5f, d = 0.0f;
			float dx = fx - cx;
			float dy = fy - cy;
			float r = (float) Math.sqrt(dx*dx + dy*dy);
			if(0<=dx) {
				h = (float) Math.asin(dy/r);
				d = (float) Math.toDegrees(h);
				if(dy<0) d = 360 + d;
			} else {
				h = (float) Math.acos(dx/r);
				d = (float) Math.toDegrees(h);
				if(dy<0) d = 360 - d;
			}
			Log.d(TAG, this.getClass().toString() + " run r=" + r + " h=" + h + " d=" + d);
			int color = Color.rgb(0xff, 0xff, 0xff);
			if((minr<=r) && (r<=maxr)) {
				float hsv[] = {d, s, l};
				color = Color.HSVToColor(hsv);
				Log.d(TAG, this.getClass().toString() + " run color(" + Color.red(color) + "," + Color.green(color) + "," + Color.blue(color) + ")");
			}
			if(mListener != null) {
	            mListener.onRgb(color);
			}
		}
	}
	*/
	class CalcRgbHandler extends Handler {
		public CalcRgbHandler() {
		}
		
		public CalcRgbHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, this.getClass().toString() + " handleMessage thrdID:" + Thread.currentThread().getId() + " msg:" + msg.toString());
			Bundle b = msg.getData();
			float fx = b.getFloat("fx");
			float fy = b.getFloat("fy");
			
			//because the lum is 0.5, the r,g,b always less than 128 
			float h = 0.0f, s = 1.0f, l = 0.5f, d = 0.0f;
			float dx = fx - cx;
			float dy = fy - cy;
			float r = (float) Math.sqrt(dx*dx + dy*dy);
			if(0<=dx) {
				h = (float) Math.asin(dy/r);
				d = (float) Math.toDegrees(h);
				if(dy<0) d = 360 + d;
			} else {
				h = (float) Math.acos(dx/r);
				d = (float) Math.toDegrees(h);
				if(dy<0) d = 360 - d;
			}
			//Log.d(TAG, this.getClass().toString() + " run r=" + r + " h=" + h + " d=" + d);
			int color = Color.rgb(0xff, 0xff, 0xff);
			if((minr<=r) && (r<=maxr)) {
				float hsv[] = {d, s, l};
				color = Color.HSVToColor(hsv);
				//Log.d(TAG, this.getClass().toString() + " run color(" + Color.red(color) + "," + Color.green(color) + "," + Color.blue(color) + ")");
			}
			if(mListener != null) {
	            mListener.onRgb(color);
			}
		}
	}
	
	private void startCalcRgb() {
		//android application supply the handler thread
		mCalcRgbHandlerThread = new HandlerThread("CalcRgbHandlerThread");
		//the handler thread's start function must call before handler thread's getLooper function
		mCalcRgbHandlerThread.start();
		mCalcRgbHandler = new CalcRgbHandler(mCalcRgbHandlerThread.getLooper());
		Log.d(TAG, this.getClass().toString() + " startCalcRgb() HandlerThread thrdID:" + mCalcRgbHandlerThread.getId());
	}
	
	private void stopCalcRgb() {
		Log.d(TAG, this.getClass().toString() + " stopCalcRgb()");
		mCalcRgbHandlerThread.quit();
	}
	    
	public interface Listener {
		public void onRgb(int color);
	}

	public void registerListener (Listener listener) {
		mListener = listener;
	}
    
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		cx = w / 2;
		cy = h / 2;
		float fr = cx;
		if(fr > cy) fr = cy;
		maxr = fr - 4;
		float r = fr/2;
		minr = r;
		//Log.d(TAG, this.getClass().toString() + " onSizeChanged maxr=" + maxr + " minr=" + minr);
		super.onSizeChanged(w, h, oldw, oldh);
		//Log.d(TAG, this.getClass().toString() + " onSizeChanged po(" + cx + "," + cy + ")");
		if(bmpBg == null) {
			DrawGraphicThread thread = new DrawGraphicThread();
			thread.start();
		}
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		//Log.d(TAG, this.getClass().toString() + " onDraw");
		super.onDraw(canvas);
			
		if((canvas != null) && (bmpBg != null)) {
			canvas.drawBitmap(bmpBg, 0, 0, null);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		float x = event.getX();
		float y = event.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			//Log.d(TAG, this.getClass().toString() + " onTouchEvent down(" + x + "," + y + ")");
			CalcRgb(x, y);
			break;
		case MotionEvent.ACTION_MOVE:
			//Log.d(TAG, this.getClass().toString() + " onTouchEvent move(" + x + "," + y + ")");
			CalcRgb(x, y);
			break;
		default:
			break;
		}
		return true;
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		Log.d(TAG, this.getClass().toString() + " onAttachedToWindow()");
		startCalcRgb();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		Log.d(TAG, this.getClass().toString() + " onDetachedFromWindow()");
		stopCalcRgb();
	}
}
