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

//MySurfaceView���Բ���onDraw-->lockCanvas unlockCanvasAndPost
//Callback-->�ص��ӿڣ�ʵ�ָýӿڣ��Ϳ����ڶ�Ӧ��״̬��ʵ�����ⷽ��
//Runnable-->�߳̽ӿڣ�ʵ�ָýӿڣ��Ϳ���ʵ���̵߳ķ���
public class MySurfaceView extends SurfaceView implements Callback, Runnable {

	Paint paint;
	Canvas canvas;
	// ����һ���߳�������ͼ
	Thread mThread;
	// SurfaceView������
	SurfaceHolder holder;

	// flag�����ж��ػ��Ƿ��������
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

	// ���ڴ����д���MySurfaceView�����ʱ�򣬻�ʹ�øù������ķ���
	public MySurfaceView(Context context) {
		super(context);
		// ��ʼ��
		init();
	}

	// ��xml�����У����ʹ�øö��󲼾֣���ʹ�øù���������
	public MySurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	// ��ʼ������
	public void init() {
		holder = getHolder();
		// ��ӵ�ǰSurfaceView��״̬�ص�
		holder.addCallback(this);
		// ������͸��
		holder.setFormat(PixelFormat.TRANSLUCENT);
		// ��ͼ���ö�
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

	// ��ͼ
	public void myDraw() {
		// �ӹ�����holder�л�ȡ��������
		canvas = holder.lockCanvas();
		// ÿ��һ�ξ͸�����һ�λ��Ƶ�ͼ��
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
		// �������holder�ύ���ƺõĶ���
		holder.unlockCanvasAndPost(canvas);
	}

	// ʱ�����
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// ���¼����зֱ����
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

	// �������
	public void DirectionMethod(final byte direction) {
		// ��Android�з���socket��Ϣ����Ҫ�߳�����
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Socket Client
					// ����Socket���󣬲���һ��IP��ַ��192.168.1.1�������������˿ںţ�2001��
					Socket socket = new Socket("192.168.1.1", 2001);
					// ���������OutputStream
					OutputStream outputStream = socket.getOutputStream();
					// �������д������
					byte[] command = new byte[] { (byte) 0xff, (byte) 0x00, direction, (byte) 0x00, (byte) 0xff };
					outputStream.write(command);
					// �ͷ�IO��
					outputStream.flush();
					// �ر�IO��
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

	// ʵ��Runnable�ӿں���д��run��������ʱ��������Ա���Ϊһ���߳�
	// Thread�౾����Ҫʵ��Runnable�ӿ�
	// �̣߳�ִ�к�ʱ����ʱ��ϳ����ظ������ر��
	@Override
	public void run() {
		while (flag) {
			myDraw();
			// ÿ��10ms����һ��ͼ��
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// ��SurfaceView������ʱ����ʹ�ø÷���
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		flag = true;
		// ͨ����Thread�д���Runnable��������
		mThread = new Thread(this);
		// �����߳�
		mThread.start();
	}

	// ��SurfacViewҳ���״̬�����ı�ʱ����ʹ�ø÷�������С����״������
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	// ��SurfaceViewҳ�汻���٣����أ�����ʹ�ø÷���
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		flag = false;
	}
}
