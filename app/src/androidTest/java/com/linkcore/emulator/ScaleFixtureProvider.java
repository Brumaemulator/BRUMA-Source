package com.linkcore.emulator;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
public class ScaleFixtureProvider extends ContentProvider {
 private final AtomicInteger opens=new AtomicInteger(),queries=new AtomicInteger();
 private int revision=1,count=300;private File root;
 public boolean onCreate(){root=new File(getContext().getCacheDir(),"scale-roms");root.mkdirs();return true;}
 public Bundle call(String method,String arg,Bundle extras){
  if(method.equals("reset")){opens.set(0);queries.set(0);}
  if(method.equals("change"))revision++;
  if(method.equals("remove"))count=299;
  Bundle b=new Bundle();b.putInt("opens",opens.get());b.putInt("queries",queries.get());return b;
 }
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
  queries.incrementAndGet();MatrixCursor c=new MatrixCursor(projection);
  for(int n=0;n<count;n++){
   Object[] row=new Object[projection.length];
   for(int k=0;k<row.length;k++)switch(projection[k]){
    case "document_id":row[k]="rom"+n;break;
    case "_display_name":row[k]=String.format(java.util.Locale.ROOT,"Test %03d.gba",n);break;
    case "mime_type":row[k]="application/octet-stream";break;
    case "_size":row[k]=1024L;break;
    case "last_modified":row[k]=n==0?(long)revision:1L;break;
   }
   c.addRow(row);
  }
  return c;
 }
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
  if(!mode.equals("r"))throw new FileNotFoundException();opens.incrementAndGet();
  int n=Integer.parseInt(DocumentsContract.getDocumentId(uri).substring(3));
  File file=new File(root,"rom"+n);
  try(FileOutputStream out=new FileOutputStream(file)){
   byte[] bytes=new byte[1024];bytes[0]=(byte)n;bytes[1]=(byte)(n>>8);bytes[2]=(byte)(n==0?revision:1);out.write(bytes);
  }catch(IOException e){throw new FileNotFoundException(e.toString());}
  return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
 }
 public String getType(Uri uri){return "application/octet-stream";}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
