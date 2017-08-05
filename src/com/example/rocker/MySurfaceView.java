package com.example.rocker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

//MySurfaceView可以不用onDraw-->lockCanvas unlockCanvasAndPost
//Callback-->回调接口，实现该接口，就可以在对应的状态中实现特殊方法
//Runnable-->线程接口，实现该接口，就可以实现线程的方法
public class MySurfaceView extends SurfaceView implements Callback, Runnable {

	Paint paint;
	Canvas canvas;
	// 创建一个线程启动绘图
	Thread mThread;
	// SurfaceView管理者
	SurfaceHolder holder;

	// flag用于判断重绘是否继续进行
	boolean flag;
	byte logicType;
	float trayRadius;
	double angle, radian;

	float rockCenterX, rockCenterY, rockRadius;
	float baseCenterX, baseCenterY, baseRadius;

	final byte LOGIC_STOP = 0x00;
	final byte LOGIC_FORWARD = 0x01;
	final byte LOGIC_BACKWARD = 0x02;
	final byte LOGIC_LEFT = 0x03;
	final byte LOGIC_RIGHT = 0x04;

	// 当在代码中创建MySurfaceView对象的时候，会使用该构造器的方法
	public MySurfaceView(Context context) {
		super(context);
		// 初始化
		init();
	}

	// 在xml布局中，如果使用该对象布局，则使用该构造器方法
	public MySurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	// 初始化方法
	public void init() {
		holder = getHolder();
		// 添加当前SurfaceView的状态回调
		holder.addCallback(this);
		// 允许背景透明
		holder.setFormat(PixelFormat.TRANSLUCENT);
		// 将图层置顶
		this.setZOrderOnTop(true);
		paint = new Paint();
		paint.setAntiAlias(true);
		rockCenterX = baseCenterX = 340;
		rockCenterY = baseCenterY = 740;
		rockRadius = 90;
		trayRadius = 90;
		baseRadius = 250;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(800, 1080);
	}

	// 绘图
	public void myDraw() {
		// 从管理者holder中获取画布对象
		canvas = holder.lockCanvas();
		// 每画一次就覆盖上一次绘制的图像
		canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
		paint.setColor(Color.WHITE);
		paint.setAlpha(50);
		canvas.drawCircle(baseCenterX, baseCenterY, baseRadius, paint);
		paint.setColor(Color.WHITE);
		paint.setAlpha(10);
		canvas.drawCircle(baseCenterX, baseCenterY, trayRadius, paint);
//		RectF oval = new RectF(340, 340, 740, 740);
//		canvas.drawArc(oval, 0, 30, true, paint);
		paint.setColor(Color.BLACK);
		canvas.drawCircle(rockCenterX, rockCenterY, rockRadius, paint);
		// 向管理者holder提交绘制好的对象
		holder.unlockCanvasAndPost(canvas);
	}

	// 时间监听
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// 对事件进行分别监听
		if (event.getAction() == MotionEvent.ACTION_UP) {
			rockCenterX = baseCenterX;
			rockCenterY = baseCenterY;
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logicType   = LOGIC_STOP;
		} else {
			float distanceX = event.getX() - baseCenterX;
			float distanceY = event.getY() - baseCenterY;
			double distanceCenter = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
			if (distanceCenter <= baseRadius) {
				rockCenterX = event.getX();
				rockCenterY = event.getY();
			} else {
				rockCenterX = (float) (baseCenterX + baseRadius * distanceX / distanceCenter);
				rockCenterY = (float) (baseCenterY + baseRadius * distanceY / distanceCenter);
			}
			radian = Math.acos(distanceX / distanceCenter);
			if (event.getY() > baseCenterY) {
				angle = Math.toDegrees(-radian);
				logicType = setLogicType(angle);
			} else {
				angle = Math.toDegrees(radian);
				logicType = setLogicType(angle);
			}
		}
		DirectionMethod(logicType);
		return true;
	}

	byte setLogicType(double angle) {
		byte type = 0x00;
		if (angle > 45 && angle <= 135) {
			type = LOGIC_FORWARD;
		} else if ((angle > 135 && angle <= 180) || (angle > -180 && angle <= -135)) {
			type = LOGIC_LEFT;
		} else if ((angle > -45 && angle <= 0) || (angle > 0 && angle <= 45)) {
			type = LOGIC_RIGHT;
		} else {
			type = LOGIC_BACKWARD;
		}
		return type;
	}

	// 方向控制
	public void DirectionMethod(final byte direction) {
		// 在Android中发送socket信息，需要线程启动
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Socket Client
					// 创建Socket对象，参数一：IP地址（192.168.1.1），参数二：端口号（2001）
					Socket socket = new Socket("192.168.1.1", 2001);
					// 创建输出流OutputStream
					OutputStream outputStream = socket.getOutputStream();
					// 向输出流写入数据
					byte[] command = new byte[] { (byte) 0xff, (byte) 0x00, direction, (byte) 0x00, (byte) 0xff };
					outputStream.write(command);
					// 释放IO流
					outputStream.flush();
					// 关闭IO流
					outputStream.close();
					socket.close();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	// 实现Runnable接口后重写的run方法，此时整个类可以被作为一个线程
	// Thread类本身需要实现Runnable接口
	// 线程：执行耗时操作时间较长，重复次数特别多
	@Override
	public void run() {
		while (flag) {
			myDraw();
			// 每个10ms绘制一次图像
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// 当SurfaceView被创建时，会使用该方法
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		flag = true;
		// 通过向Thread中传入Runnable创建对象
		mThread = new Thread(this);
		// 启动线程
		mThread.start();
	}

	// 当SurfacView页面的状态发生改变时，会使用该方法（大小，形状……）
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	// 当SurfaceView页面被销毁（返回），会使用该方法
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		flag = false;
	}
}
