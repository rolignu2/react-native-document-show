
package com.reactlibrary;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;


public class RNReactNativeDocumentShowModule extends ReactContextBaseJavaModule implements ActivityEventListener {

  private static final int READ_REQUEST_CODE = 41;

  private static class Fields {
      private static final String FILE_SIZE = "fileSize";
      private static final String FILE_NAME = "fileName";
      private static final String TYPE = "type";
  }

  private final ReactApplicationContext reactContext;
  private Callback callback;

  public RNReactNativeDocumentShowModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNReactNativeDocumentShow";
  }

  @ReactMethod
  public void show(ReadableMap args, Callback callback) {

      Intent intent;
      int    compatible = 0 ;


      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
          intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      } else if (android.os.Build.VERSION.SDK_INT  <= 18) {
          intent            = new Intent(Intent.ACTION_GET_CONTENT);
          compatible        = 1;
      }else {
          intent = new Intent(Intent.ACTION_PICK);
      }

      intent.addCategory(Intent.CATEGORY_OPENABLE);

      if (!args.isNull("filetype")) {
          ReadableArray filetypes = args.getArray("filetype");
          if (filetypes.size() > 0) {
              intent.setType(filetypes.getString(0));
          }
      }

      this.callback = callback;

      try {
          switch (compatible) {
              case 0:
                  getReactApplicationContext().startActivityForResult(intent, READ_REQUEST_CODE, Bundle.EMPTY);
                  break;
              case 1:
                  String fileTitle = "";
                  try{
                     fileTitle = !args.isNull("fileTitle") ? args.getString("fileTitle") : "";
                  }catch(Exception e){
                    Log.i("documentshow" , e.getMessage());
                  }
                  getReactApplicationContext().startActivityForResult(Intent.createChooser(intent , fileTitle)  , 1 ,  Bundle.EMPTY);
                  break;
          }
      }catch(Exception ex ){
          Context context = getReactApplicationContext();
          CharSequence text = "Lo sentimos hubo un error al procesar la peticion : " + ex.getMessage();
          int duration = Toast.LENGTH_SHORT;
          Toast toast = Toast.makeText(context, text, duration);
          toast.show();
          Log.i("documentshow" , ex.getMessage());
      }

  }



  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      onActivityResult(requestCode, resultCode, data);
  }

    @Override
    public void onNewIntent(Intent intent) {

    }

    // removed @Override temporarily just to get it working on RN0.33 and RN0.32
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

      if (requestCode != READ_REQUEST_CODE)
          return;

      if (resultCode != Activity.RESULT_OK) {
          callback.invoke("Bad result code: " + resultCode, null);
          return;
      }

      if (data == null) {
          callback.invoke("No data", null);
          return;
      }

      try {
          Uri uri = data.getData();
          callback.invoke(null, toMapWithMetadata(uri));
      } catch (Exception e) {
          callback.invoke(e.getMessage(), null);
      }
  }

  private WritableMap toMapWithMetadata(Uri uri) {
      WritableMap map;
      if(uri.toString().startsWith("/")) {
          map = metaDataFromFile(new File(uri.toString()));
      } else if (uri.toString().startsWith("http")) {
          map = metaDataFromUri(uri);
      } else {
          map = metaDataFromContentResolver(uri);
      }

      map.putString("uri", uri.toString());

      return map;
  }

  private WritableMap metaDataFromUri(Uri uri) {
      WritableMap map = Arguments.createMap();

      File outputDir = getReactApplicationContext().getCacheDir();
      try {
          File downloaded = download(uri, outputDir);

          map.putInt(Fields.FILE_SIZE, (int) downloaded.length());
          map.putString(Fields.FILE_NAME, downloaded.getName());
          map.putString(Fields.TYPE, mimeTypeFromName(uri.toString()));
      } catch (IOException e) {
          Log.e("DocumentPicker", "Failed to download file", e);
      }

      return map;
  }

  private WritableMap metaDataFromFile(File file) {
      WritableMap map = Arguments.createMap();

      if(!file.exists())
          return map;

      map.putInt(Fields.FILE_SIZE, (int) file.length());
      map.putString(Fields.FILE_NAME, file.getName());
      map.putString(Fields.TYPE, mimeTypeFromName(file.getAbsolutePath()));

      return map;
  }

  private WritableMap metaDataFromContentResolver(Uri uri) {
      WritableMap map = Arguments.createMap();

      ContentResolver contentResolver = getReactApplicationContext().getContentResolver();

      map.putString(Fields.TYPE, contentResolver.getType(uri));

      Cursor cursor = contentResolver.query(uri, null, null, null, null, null);

      try {
          if (cursor != null && cursor.moveToFirst()) {

              map.putString(Fields.FILE_NAME, cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));

              int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
              if (!cursor.isNull(sizeIndex)) {
                  String size = cursor.getString(sizeIndex);
                  if (size != null)
                      map.putInt(Fields.FILE_SIZE, Integer.valueOf(size));
              }
          }
      } finally {
          if (cursor != null) {
              cursor.close();
          }
      }

      return map;
  }

  private static File download(Uri uri, File outputDir) throws IOException {
      File file = File.createTempFile("prefix", "extension", outputDir);

      URL url = new URL(uri.toString());

      ReadableByteChannel channel = Channels.newChannel(url.openStream());
      try{
          FileOutputStream stream = new FileOutputStream(file);

          try {
              stream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
              return file;
          } finally {
              stream.close();
          }
      } finally {
          channel.close();
      }
  }

  private static String mimeTypeFromName(String absolutePath) {
      String extension = MimeTypeMap.getFileExtensionFromUrl(absolutePath);
      if (extension != null) {
          return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
      } else {
          return null;
      }
  }

}