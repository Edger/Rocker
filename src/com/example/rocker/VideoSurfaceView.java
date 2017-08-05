package com.example.rocker;

/**
 * Created by Administrator on 2017/2/6 0006.
 */
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView implements Callback {

	private SurfaceHolder surfaceHolder;
	private Canvas canvas;
	URL videoURL;
	private String string;
	HttpURLConnection httpURLConnection;
	Bitmap bitmap;
	private Paint paint;
	InputStream inputstream = null;
	private Bitmap mBitmap;
	public static int mScreenWidth;
	public static int mScreenHeight;
	public boolean isScale = false;
	private boolean isThreadRunning = true;
	public boolean isPitcure;
	public FileOutputStream fos;
	public String file;
	private TakePicture takePicture;
	private IntentFilter filter;

	public static byte[] commands;
	public static byte[] commands2;
	public static byte cmdDirection;

	public VideoSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
		paint = new Paint();
		paint.setAntiAlias(true);
		surfaceHolder = this.getHolder();
		surfaceHolder.addCallback(this);
		this.setKeepScreenOn(true);
		setFocusable(true);
		this.getWidth();
		this.getHeight();
		filter = new IntentFilter("PICTURE");
		takePicture = new TakePicture();
		context.registerReceiver(takePicture, filter);
	}

	public void initialize() {
		GetCameraIP();
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		mScreenWidth = displayMetrics.widthPixels;
		mScreenHeight = displayMetrics.heightPixels;
		this.setKeepScreenOn(true);// 保持屏幕常亮
	}

	class DrawVideo extends Thread {

		public DrawVideo() {
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			// TODO Auto-generated method stub
			return super.clone();
		}

		public void run() {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(Color.GREEN);
			paint.setTextSize(20);
			paint.setStrokeWidth(1);

			int bufSize = 512 * 1024; // 视频图片缓冲
			byte[] jpg_buf = new byte[bufSize]; // buffer to read jpg

			int readSize = 4096; // 每次最大获取的流
			byte[] buffer = new byte[readSize]; // buffer to read stream

			while (isThreadRunning) {
				URL url = null;
				HttpURLConnection URLConnection = null;

				try {
					url = new URL(string);
					URLConnection = (HttpURLConnection) url.openConnection(); // 使用HTTPURLConnetion打开连接

					int read = 0;
					int status = 0;
					int jpg_count = 0; // jpg数据下标

					while (isThreadRunning) {
						read = URLConnection.getInputStream().read(buffer, 0, readSize);

						if (read > 0) {

							for (int i = 0; i < read; i++) {
								switch (status) {
								// Content-Length:
								case 0:
									if (buffer[i] == (byte) 'C')
										status++;
									else
										status = 0;
									break;
								case 1:
									if (buffer[i] == (byte) 'o')
										status++;
									else
										status = 0;
									break;
								case 2:
									if (buffer[i] == (byte) 'n')
										status++;
									else
										status = 0;
									break;
								case 3:
									if (buffer[i] == (byte) 't')
										status++;
									else
										status = 0;
									break;
								case 4:
									if (buffer[i] == (byte) 'e')
										status++;
									else
										status = 0;
									break;
								case 5:
									if (buffer[i] == (byte) 'n')
										status++;
									else
										status = 0;
									break;
								case 6:
									if (buffer[i] == (byte) 't')
										status++;
									else
										status = 0;
									break;
								case 7:
									if (buffer[i] == (byte) '-')
										status++;
									else
										status = 0;
									break;
								case 8:
									if (buffer[i] == (byte) 'L')
										status++;
									else
										status = 0;
									break;
								case 9:
									if (buffer[i] == (byte) 'e')
										status++;
									else
										status = 0;
									break;
								case 10:
									if (buffer[i] == (byte) 'n')
										status++;
									else
										status = 0;
									break;
								case 11:
									if (buffer[i] == (byte) 'g')
										status++;
									else
										status = 0;
									break;
								case 12:
									if (buffer[i] == (byte) 't')
										status++;
									else
										status = 0;
									break;
								case 13:
									if (buffer[i] == (byte) 'h')
										status++;
									else
										status = 0;
									break;
								case 14:
									if (buffer[i] == (byte) ':')
										status++;
									else
										status = 0;
									break;
								case 15:
									if (buffer[i] == (byte) 0xFF)
										status++;
									jpg_count = 0;
									jpg_buf[jpg_count++] = (byte) buffer[i];
									break;
								case 16:
									if (buffer[i] == (byte) 0xD8) {
										status++;
										jpg_buf[jpg_count++] = (byte) buffer[i];
									} else {
										if (buffer[i] != (byte) 0xFF)
											status = 15;

									}
									break;
								case 17:
									jpg_buf[jpg_count++] = (byte) buffer[i];
									if (buffer[i] == (byte) 0xFF)
										status++;
									if (jpg_count >= bufSize)
										status = 0;
									break;
								case 18:
									jpg_buf[jpg_count++] = (byte) buffer[i];
									if (buffer[i] == (byte) 0xD9) {
										status = 0;
										// jpg接收完成
										canvas = surfaceHolder.lockCanvas();
										// 显示图像
										if (null != canvas) {
											canvas.drawColor(Color.BLACK);

											Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(jpg_buf));

											int width = mScreenWidth;
											int height = mScreenHeight;

											mBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

											canvas.drawBitmap(mBitmap, 0, 0, null);

											surfaceHolder.unlockCanvasAndPost(canvas);// 画完一副图像，解锁画布
										}

										Log.d("MySurface", "canvas null");
									} else {
										if (buffer[i] != (byte) 0xFF)
											status = 17;
									}
									break;
								default:
									status = 0;
									break;

								}
							}
						}
					}
				} catch (IOException ex) {
					URLConnection.disconnect();
					ex.printStackTrace();
				}
			}

		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	public void GetCameraIP() {
		string = "http://192.168.1.1:8080/?action=stream";
	}

	public void surfaceCreated(SurfaceHolder holder) {
		isThreadRunning = true;
		new DrawVideo().start();
	}

	class TakePicture extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			isPitcure = true;
			file = intent.getStringExtra("file");
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}
}