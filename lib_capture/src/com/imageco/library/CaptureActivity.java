package com.imageco.library;

import java.io.IOException;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.ClipboardManager;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

/**
 * Class CaptureActivity ...
 *
 * @author Administrator
 *         Created on 11-10-22
 */
public class CaptureActivity extends Activity implements Callback {

    /**
     * Field handler
     */
    private CaptureActivityHandler handler;
    /**
     * Field viewfinderView
     */
    private ViewfinderView viewfinderView;
    /**
     * Field hasSurface
     */
    private boolean hasSurface;
    /**
     * Field decodeFormats
     */
    private Vector<BarcodeFormat> decodeFormats;
    /**
     * Field characterSet
     */
    private String characterSet;
    /**
     * Field txtResult
     */
//    private TextView txtResult;
    /**
     * Field inactivityTimer
     */
    private InactivityTimer inactivityTimer;
    /**
     * Field mediaPlayer
     */
    private MediaPlayer mediaPlayer;
    /**
     * Field playBeep
     */
    private boolean playBeep;
    /**
     * Field BEEP_VOLUME
     */
//    private static final float BEEP_VOLUME = 0.10f;
    /**
     * Field vibrate
     */
    private boolean vibrate;
    /**
     * Field qrCode
     */
    private String qrCode;

//    变焦用的进度条android2.2以上版本支持
    private SeekBar mSeekBar;
//    private Button plus,minus;
    private int progress;
    private Camera.Parameters parameters;
    private int maxZoom,zoom;
    private Camera camera;
    private boolean isFirst = true;
//    private Handler handler = new Handler();
    /**
     * Called when the activityImpl is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.capture);
        //初始化 CameraManager
        
        CameraManager.init(getApplication());
//        Gloable.getInstance().setNewHistory(true);
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
//        txtResult = (TextView) findViewById(R.id.txtResult);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar1); //缩放条
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        
//        Button btCapture = (Button)findViewById(R.id.btn_capture);
//        btCapture.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View view) {
//				camera.takePicture(new ShtterCallBack(), null, new PictureCallBack(CaptureActivity.this));
//			}
//		});
    }
    /**
     * Method onResume ...
     */
    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
        	initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//            initCamera(surfaceHolder);
        }
        decodeFormats = null;
        characterSet = null;
        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
//        mSeekBar.setProgress(progress);
    }

    /**
     * Method onPause ...
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    /**
     * Method onDestroy ...
     */
    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    /**
     * Method initCamera ...
     *
     * @param surfaceHolder of type SurfaceHolder
     */
    @TargetApi(8)
	private void initCamera(SurfaceHolder surfaceHolder) {
        try {
           camera = CameraManager.get().openDriver(surfaceHolder);
           if (handler == null) {
               handler = new CaptureActivityHandler(this, decodeFormats,
                       characterSet);
           }
           if(Build.VERSION.SDK_INT>=8){
        	    parameters =camera.getParameters();
        	    maxZoom = parameters.getMaxZoom();
        	    zoom = 1;
        	    if(maxZoom>0){
                    progress = (int)(100.0/maxZoom);
            	    mSeekBar.setVisibility(View.VISIBLE);
//            	    plus.setVisibility(View.VISIBLE);
//            	    minus.setVisibility(View.VISIBLE);
    				mSeekBar.setProgress(progress);
//    				camera.cancelAutoFocus();
//    				parameters.setZoom(zoom);
        	    	isFirst = false;
        	    }else if(isFirst){
        	    	isFirst = false;
        	    	Toast.makeText(this, "手机不支持数码变焦", Toast.LENGTH_SHORT);
        	    }
//				camera.setParameters(parameters);
               	mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@SuppressLint({"NewApi", "NewApi"})
					@Override
					public void onProgressChanged(SeekBar seekBar, int progres,boolean fromUser) {
						if(camera != null){
   	   	   					zoom = (int)(progres/(100.0/maxZoom));
   	   	   					progress = progres;
   	   	   					camera.cancelAutoFocus();
   	   	   					parameters = camera.getParameters();
   	   	   	   				try {
//   	   	   	   				小米2.4.7系统缩放控制 ,key="taking-picture-zoom"为缩放控制key,value为倍率乘8
   								parameters.set("taking-picture-zoom", zoom*(40/maxZoom));
//   								parameters.set("taking-picture-zoom", zoom);
//   								maxZoom = 40;
   							} catch (Exception e) {
   							}
   	   	   					parameters.setZoom(zoom);
   	   	   					try {
								camera.setParameters(parameters);
							} catch (Exception e) {
							}
//   	   	   					获得驱动的key,和value
//   	   	   					System.out.println(parameters.flatten());
   	   	   					camera.autoFocus(CameraManager.autoFocusCallback);
   	   					}
					}
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
   				});
           }
        } catch (IOException ioe) {
        } catch (RuntimeException e) {
        }
        
    }

    /**
     * Method surfaceChanged ...
     *
     * @param holder of type SurfaceHolder
     * @param format of type int
     * @param width  of type int
     * @param height of type int
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    /**
     * Method surfaceCreated ...
     *
     * @param holder of type SurfaceHolder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    /**
     * Method surfaceDestroyed ...
     *
     * @param holder of type SurfaceHolder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    /**
     * Method getViewfinderView returns the viewfinderView of this CaptureActivity object.
     *
     * @return the viewfinderView (type ViewfinderView) of this CaptureActivity object.
     */
    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    /**
     * Method getHandler returns the handler of this CaptureActivity object.
     *
     * @return the handler (type Handler) of this CaptureActivity object.
     */
    public Handler getHandler() {
        return handler;
    }

    /**
     * Method drawViewfinder ...
     */
    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    /**
     * Method handleDecode ...
     *
     * @param obj     of type Result
     * @param barcode of type Bitmap
     */
    public void handleDecode(Result obj, Bitmap barcode) {
        inactivityTimer.onActivity();
        viewfinderView.drawResultBitmap(barcode);
        playBeepSoundAndVibrate();
        qrCode = obj.getText();
        ClipboardManager cmb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cmb.setText(qrCode); 
//        System.out.println(StringUtils.guessEncoding(qrCode.getBytes(), null));
//        System.out.println(obj);
//        qrCode = CodeOperation.all2utf8(qrCode);
        //跳转页面
        Intent intent = new Intent();
//        Bundle bundle = new Bundle();
//        
//        intent.setClass(CaptureActivity.this, PhotoresultActivity.class);
        intent.putExtra("qrCode", qrCode);
//        intent.putExtras(bundle);
//        startActivity(intent);
        setResult(200, intent);
        CaptureActivity.this.finish();

//		txtResult.setText(obj.getBarcodeFormat().toString() + ":"
//				+ obj.getText());

    }

    /**
     * Method initBeepSound ...
     */
    private void initBeepSound() {
//        if (SETTING.SOUND && playBeep && mediaPlayer == null) {
//            // The volume on STREAM_SYSTEM is not adjustable, and users found it
//            // too loud,
//            // so we now play on the music stream.
//            setVolumeControlStream(AudioManager.STREAM_MUSIC);
//            mediaPlayer = new MediaPlayer();
//            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mediaPlayer.setOnCompletionListener(beepListener);
//
//            AssetFileDescriptor file = getResources().openRawResourceFd(
//                    R.raw.beep);
//            try {
//                mediaPlayer.setDataSource(file.getFileDescriptor(),
//                        file.getStartOffset(), file.getLength());
//                file.close();
////				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
//                mediaPlayer.prepare();
//            } catch (IOException e) {
//                mediaPlayer = null;
//            }
//        }
    }


    /**
     * Field VIBRATE_DURATION
     */
    private static final long VIBRATE_DURATION = 200L;

    /**
     * Method playBeepSoundAndVibrate ...
     */
    private void playBeepSoundAndVibrate() {
//        if (SETTING.SOUND && playBeep && mediaPlayer != null) {
//            mediaPlayer.start();
//        }
//        if (SETTING.SHAKE && vibrate) {
//            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//            vibrator.vibrate(VIBRATE_DURATION);
//        }
    }

    /**
     *
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        /**
         * Method onCompletion ...
         *
         * @param mediaPlayer of type MediaPlayer
         */
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

}