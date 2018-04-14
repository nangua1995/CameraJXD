package com.example.cameratest.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cameratest.CameraApp;
import com.example.cameratest.R;
import com.example.cameratest.adapter.GridAdapter;
import com.example.cameratest.adapter.PreviewAdapter;
import com.example.cameratest.bean.VideoBean;
import com.example.cameratest.util.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoPreviewActivity extends Activity {
    private GridView gridView = null;
    private PreviewAdapter previewAdapter = null;
    private List<VideoBean> videoBeanList = new ArrayList<>();
    private MyTask task = null;
    private TextView hintText = null;
    private MediaMetadataRetriever media =new MediaMetadataRetriever();
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    if(previewAdapter == null){
                        previewAdapter = new PreviewAdapter(getApplicationContext(),videoBeanList);
                        gridView.setAdapter(previewAdapter);
                    }else{
                        previewAdapter.notifyData(videoBeanList);
                    }

                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);
        hintText = (TextView) findViewById(R.id.hint);

        setViews();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.jxd.complete.videopreview");
        registerReceiver(broadcastReceiver,intentFilter);
        task = new MyTask();
        task.execute();
    }

    public class MyTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            initData();

            Log.d("jxd",videoBeanList.size()+"");

            return null;
        }
    }

    private void setViews() {
        gridView = (GridView) findViewById(R.id.grid_preview);
        gridView.setNumColumns(5);
        gridView.setHorizontalSpacing(10);
        gridView.setVerticalSpacing(10);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(VideoPreviewActivity.this,PlayActivity.class);
                intent.putExtra("play_path",videoBeanList.get(position).getPath());
                startActivity(intent);
            }
        });


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK){
            startActivity(new Intent(VideoPreviewActivity.this,RecordVideoActivity.class));
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initData() {
      //  String path = CameraApp.getAllSdPaths(VideoPreviewActivity.this)[0];
      //  if (path != null) {
//            File dir = new File(path + "/CameraJXD/Video");
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
//            else {
//                videoFiles = dir.listFiles();
//            }
//            List<VideoBean> vBeanList = getMyVideoList();//getVideoList(this);
//            for(int i = 0;i<vBeanList.size();i++){
//                //if(vBeanList.get(i).getPath().contains("/CameraJXD/Video"))
//                {
//
//                    VideoBean videoBean =  vBeanList.get(i);
//                    Log.d("jxd",videoBean.getPath()+"  duration "+videoBean.getDuration());
//                    if(videoBean.getThumbPath() == null){
//                        videoBean.setBitmap(getVideoPhoto(videoBean.getPath()));
//                    }
//
//                    videoBeanList.add(videoBean);
//                }
//
//            }
     //   }

        getMyVideoList();
    }

    public void getMyVideoList()
    {
        String path = CameraApp.getAllSdPaths(VideoPreviewActivity.this)[0];
        if (path != null) {
            File f = new File(path + "/CameraJXD/Video");
            File[] files = f.listFiles();
            for(File ff : files){
                VideoBean v = new VideoBean();
                v.setPath(ff.getPath());
                v.setDuration(getVideoDuration(ff.getPath()));
                v.setBitmap(getVideoPhoto(ff.getPath()));
                videoBeanList.add(v);
                handler.sendEmptyMessage(0);
            }
        }

    }


    /**
     * 获取视频缩略图
     * MediaStore.Video.Thumbnails.DATA:视频缩略图的文件路径
     */
    public List<VideoBean> getVideoList(Context context) {
        List<VideoBean> sysVideoList = new ArrayList<>();

        String[] thumbColumns = {MediaStore.Video.Thumbnails.DATA,
                MediaStore.Video.Thumbnails.VIDEO_ID};

        String[] mediaColumns = {MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION};

        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media
                        .EXTERNAL_CONTENT_URI,
                mediaColumns, null, null, null);

        if (cursor == null) {
            return sysVideoList;
        }
        if (cursor.moveToFirst()) {
            do {
                VideoBean info = new VideoBean();
                int id = cursor.getInt(cursor
                        .getColumnIndex(MediaStore.Video.Media._ID));
                Cursor thumbCursor = context.getContentResolver().query(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        thumbColumns, MediaStore.Video.Thumbnails.VIDEO_ID
                                + "=" + id, null, null);
                if (thumbCursor.moveToFirst()) {
                    info.setThumbPath(thumbCursor.getString(thumbCursor
                            .getColumnIndex(MediaStore.Video.Thumbnails.DATA)));
                }
                info.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media
                        .DATA)));
                info.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video
                        .Media.DURATION)));
                sysVideoList.add(info);
            } while (cursor.moveToNext());

            if(cursor!=null){
                cursor.close();
            }
        }
        return sysVideoList;
    }

    //根据路径得到视频缩略图
    private Bitmap getVideoPhoto(String videoPath) {

        media.setDataSource(videoPath);
        Bitmap bitmap = media.getFrameAtTime();
        return bitmap;
    }

    //获取视频总时长
    private int getVideoDuration(String path){
        media.setDataSource(path);
        String duration = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); //
        return Integer.parseInt(duration);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hintText.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if(task!=null){
            task.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}
