package com.example.kaylee.savepicturesapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private String urlString = "http://goss.veer.com/creative/vcg/veer/1600water/veer-136153230.jpg";
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.image);
        Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                try {
                    Bitmap bitmap = getBitmapFromUrl();
                    subscriber.onNext(bitmap);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.io()).subscribe(new Action1<Bitmap>() {
            @Override
            public void call(final Bitmap bitmap) {
                if (bitmap != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                    savePicture(bitmap);

                }
            }
        });


    }

    /**
     * 根据图片的url路径获得Bitmap对象
     *
     * @return
     */
    private Bitmap getBitmapFromUrl() throws MalformedURLException {
        Bitmap bitmap = null;
        URL url = new URL(urlString);
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.connect();
            InputStream inputStream = con.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;

    }

    private void savePicture(Bitmap bitmap) {
        //保存图片的路径，手机内置内存卡
        /** 首先默认个文件保存路径 */
        final String SAVE_PIC_PATH = Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/mnt/sdcard";//保存到SD卡
        final String SAVE_REAL_PATH = SAVE_PIC_PATH + "/loyo/";//保存的确切位置
        File foder = new File(SAVE_REAL_PATH);
        if (!foder.exists()) {
            //创建文件夹
            foder.mkdirs();
        }
        File file = new File(SAVE_REAL_PATH, System.currentTimeMillis() + ".png");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream);
            outputStream.flush();
            outputStream.close();
            //发广播告诉相册有图片需要更新，这样可以在图册下看到保存的图片了
            Intent intent=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri=Uri.fromFile(file);
            intent.setData(uri);
            sendBroadcast(intent);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "图片保存成功", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
