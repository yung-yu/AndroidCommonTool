package com.andy.androidcommon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class PathAnimationView extends View {
	private static final String TAG = "CanvasView";
	private Paint mPaint;

	private Path mAnimPath;
	private PathMeasure mPathMeasure;
	private ValueAnimator mValueAnimator;
	private int mTextCount;

	private Path mOrignalPath;

	public PathAnimationView(Context context) {
		super(context);
		init(context, null);
	}

	public PathAnimationView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, null);
	}


	private void init(Context context, @Nullable AttributeSet attrs) {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(3.5f);
		mPaint.setStyle(Paint.Style.FILL);
		mAnimPath = new Path();
	}

	public void setPath(Path orignalPath) {

		mOrignalPath = orignalPath;
		if (mPathMeasure == null) {
			mPathMeasure = new PathMeasure();
		}

		mAnimPath.reset();
		mAnimPath.moveTo(0, 0);
		mPathMeasure.setPath(orignalPath, false);
		mTextCount = 0;

		while (mPathMeasure.nextContour()) {
			mTextCount++;
		}

		mPathMeasure.setPath(orignalPath, false);
		mPaint.setStyle(Paint.Style.STROKE);
		initEngine();
	}

	private void initEngine() {
		if (null == mValueAnimator) {

			mValueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
			mValueAnimator.setDuration(900);
			mValueAnimator.setInterpolator(new LinearInterpolator());
		}

		mValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float value = (float) animation.getAnimatedValue();

				mPathMeasure.getSegment(0, mPathMeasure.getLength() * value, mAnimPath, true);
				invalidate();
			}
		});

		mValueAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationRepeat(Animator animation) {
				super.onAnimationRepeat(animation);
				if (!mPathMeasure.nextContour()) {
					animation.end();
				}
				invalidate();
			}
		});
	}

	public void setTotalDuration(long duration) {
		handleEmptyOrignalPath();
		mValueAnimator.setDuration(duration / mTextCount);
	}

	public void start() {
		handleEmptyOrignalPath();
		if (mValueAnimator.isRunning()) {
			mValueAnimator.end();
		}
		mValueAnimator.start();
		invalidate();
	}

	private void handleEmptyOrignalPath() {
		if (null == mValueAnimator) {
			setPath(getDefPath());
		}
	}

	public void stop() {
		handleEmptyOrignalPath();
		mValueAnimator.end();
	}

	private Path getDefPath() {
		Path textPath = new Path();
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
		paint.setTextSize(50);
		String s = "";
		paint.getTextPath(s, 0, s.length(), 0, 50, textPath);
		textPath.close();
		return textPath;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (null != mPathMeasure && mPathMeasure.getLength() == 0) {
			mPaint.setStyle(Paint.Style.FILL);
			canvas.drawPath(mOrignalPath, mPaint);
			return;
		}
		canvas.drawPath(mAnimPath, mPaint);
	}
}
