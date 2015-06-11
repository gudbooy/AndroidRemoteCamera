package com.example.tcpsocket;

import java.io.*;
import java.net.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;
import android.app.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem  mItemSwitchCamera = null;

    Handler mHander;
    private boolean start = false;
    EditText mEditAddr;
    EditText mEditPort;
    EditText mEditSend;
    TextView mTextMessage;
    Socket mSock = null;
    BufferedReader mReader = null;
    BufferedWriter mWriter = null;
    String mRecvData = "";
    CheckRecv mCheckRecv = null;
    DataOutputStream dout;

    class MsgSender {

        public MsgSender(){

        }

        public void sendMsg(String msg) {
            byte[] sendData = new byte[1024];

            while(msg.length() != 1024){
                msg +="!";
            }

            sendData = msg.getBytes();
            try {
               dout.write(sendData, 0, 1024); // upload
                dout.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mEditAddr = (EditText)findViewById(R.id.editAddr);

        mEditAddr.setText("192.168.0.105");
        mEditPort = (EditText)findViewById(R.id.editPort);
      //  mEditSend = (EditText)findViewById(R.id.editSend);
        mTextMessage = (TextView)findViewById(R.id.textMessage);
    }
    @Override
    public void onPause()
    {
     super.onPause();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
		//this is the resume action
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
			//openCV loader updating
           // Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        } else {
           // Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }


    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Bitmap bmp = Bitmap.createBitmap(960, 720, Bitmap.Config.RGB_565);
        Mat image = inputFrame.rgba();
        int outPutSize  = 0;


        if(start)
        {
            Utils.matToBitmap(image, bmp, true);
            try {

                Bitmap resized = Bitmap.createScaledBitmap(bmp, 380, 360, true);
                ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
                //converting to JPEG Img
				resized.compress(Bitmap.CompressFormat.JPEG, 50, tempOut);

                outPutSize = tempOut.toByteArray().length;
                //Log.d("tag", "FileSize : " +  tempOut.toByteArray().length);
                MsgSender ms = new MsgSender();
                ms.sendMsg(String.valueOf(outPutSize));
                tempOut.writeTo(dout);
               // dout.flush();
                //dout.flush();

                //start = false;
            }
             catch (Exception e)
             {
                Log.d("tag", "Exception : " + e.getMessage());
             }


            }
        return image;
    }



    private class ConnectTask extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... arg) {
            try {
                int nPort = Integer.parseInt(arg[1]);

                mSock = new Socket(arg[0], nPort);
             //   mWriter = new BufferedWriter(
               //         new OutputStreamWriter(mSock.getOutputStream()));
                dout =   new DataOutputStream(mSock.getOutputStream());
                mHander.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Connection Success", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch(Exception e) {
                Log.d("tag", "Socket connect error.");
                return "Connect Fail";
            }
            return "Connect Succeed";
        }

        protected void onPostExecute(String result) {
            mTextMessage.setText(result);
        }
    }

    public void onBtnConnect() {
        if( mSock != null )
            return;
        String serverAddr = mEditAddr.getText().toString();
        String strPort = mEditPort.getText().toString();
        new ConnectTask().execute(serverAddr, strPort);
    }

    private class CloseTask extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... arg) {
            try {
                if( mSock != null ) {
                    mSock.close();
                    mSock = null;
                }
            } catch(Exception e) {
                Log.d("tag", "Socket close error.");
                return "Close Fail";
            }
            return "Closed";
        }

        protected void onPostExecute(String result) {
            mTextMessage.setText(result);
        }
    }

    public void onBtnClose() {
        if( mSock == null )
            return;

        new CloseTask().execute();
    }

    private class SendTask extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... arg) {
            if( mWriter == null )
                return "Can not Send";

            try {
                PrintWriter out = new PrintWriter(mWriter, true);
                out.println(arg[0]);
            } catch(Exception e) {
                Log.d("tag", "Data send error.");
                return "Send Fail";
            }
            return "Send Succeed";
        }

        protected void onPostExecute(String result) {
            mTextMessage.setText(result);
        }
    }

    private void SendFrame(final Mat img)
    {
        Thread t = new Thread(new Runnable(){
            @Override
            public void run() {
                final int imgsize = (int) (img.total() * img.channels());
                byte[] data = new byte[imgsize];
                img.get(0,0,data);
                final int col = img.cols();
                final int row = img.rows();
                try {
                    if(mSock != null)
                    {

//                        DataOutputStream dout = new DataOutputStream(mSock.getOutputStream());
                       // dout.write(imgsize);
                        dout.write(data);
                       // PrintWriter out = new PrintWriter(mWriter, true);
                       // out.println(data);
                       // Log.d("tag", "data : " + data);
                    }
                } catch(Exception e) {
                    Log.d("tag", "Data send error.");

                }

            }
        });
        t.start();
    }


    public void onBtnSend() {
        if(!start)
            start = true;
        else
            start = false;
      //  mEditSend.setText("");
      //  new SendTask().execute(strSend);
    }

    public void onClick(View v) {
        switch( v.getId() ) {
        case R.id.btnConnect :
            onBtnConnect();
            break;
        case R.id.btnClose :
            onBtnClose();
            break;
        case R.id.btnSend :
            onBtnSend();
            break;
        }
    }

    Handler mReceiver = new Handler() {
        public void handleMessage(Message msg) {
            mTextMessage.setText(mRecvData);
        }
    };

    public class CheckRecv extends Thread {
        public void run() {
            try {            	
                while( !Thread.currentThread().isInterrupted() ) {
                    mRecvData = mReader.readLine();
                    mReceiver.sendEmptyMessage(0);
                }
            } catch (Exception e) {
                Log.d("tag", "Receive error");
            }
        }
    }

}
