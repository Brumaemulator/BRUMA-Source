package com.hatkid.mkxpz;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Copies only through the user's granted document tree; source files stay read-only. */
public final class RpgImporter {
    private static final long LIMIT=4L*1024*1024*1024;
    public interface Progress { void update(int done,int total,long bytes,long expected); }
    private static final class Entry {
        final Uri uri;final File file;
        Entry(Uri uri,File file){this.uri=uri;this.file=file;}
    }
    private final ContentResolver resolver;
    private final Progress progress;
    private final ArrayList<Entry> entries=new ArrayList<>();
    private long expected;private int visited;
    public RpgImporter(ContentResolver resolver,Progress progress){this.resolver=resolver;this.progress=progress;}
    private void scan(Uri tree,String id,File dest,int depth)throws IOException {
        if(depth>24)throw new IOException("Folder depth");
        if(!dest.isDirectory()&&!dest.mkdirs())throw new IOException("Cannot create directory");
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
        try(Cursor c=resolver.query(children,new String[]{"document_id","_display_name","mime_type","_size"},null,null,null)) {
            if(c==null)throw new IOException("Cannot list directory");
            while(c.moveToNext()) {
                String name=c.getString(1);
                if(name==null||name.contains("/")||name.contains("\\")||name.equals(".")||name.equals(".."))throw new IOException("Invalid filename");
                if(++visited>50000)throw new IOException("Too many files");
                File target=new File(dest,name);
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2)))scan(tree,c.getString(0),target,depth+1);
                else {
                    String lower=name.toLowerCase(Locale.ROOT);
                    if(lower.endsWith(".exe")||lower.endsWith(".dll")||lower.endsWith(".lnk"))continue;
                    long size=c.isNull(3)?0:Math.max(0,c.getLong(3));
                    if(size>LIMIT-expected)throw new IOException("Game too large");expected+=size;
                    entries.add(new Entry(DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0)),target));
                }
            }
        }
    }
    public void copy(Uri tree,String id,File dest)throws IOException {copy(tree,id,dest,0);}
    public void copy(Uri tree,String id,File dest,int workers)throws IOException {
        if(workers<0||workers>8)throw new IllegalArgumentException("workers");
        scan(tree,id,dest,0);
        int copyWorkers=workers;
        if(copyWorkers==0) {
            int deviceWorkers=Math.max(4,Math.min(8,Runtime.getRuntime().availableProcessors()));
            copyWorkers=entries.size()>=1024?deviceWorkers:4;
        }
        final AtomicLong bytes=new AtomicLong(),last=new AtomicLong();
        final AtomicInteger done=new AtomicInteger();
        final ThreadLocal<byte[]> buffers=ThreadLocal.withInitial(()->new byte[256*1024]);
        Runnable report=()->{
            long now=android.os.SystemClock.elapsedRealtime(),old=last.get();
            if(progress!=null && now-old>=250 && last.compareAndSet(old,now))progress.update(done.get(),entries.size(),bytes.get(),expected);
        };
        if(progress!=null)progress.update(0,entries.size(),0,expected);
        ExecutorService pool=Executors.newFixedThreadPool(copyWorkers);
        CompletionService<Void> finished=new ExecutorCompletionService<>(pool);
        boolean interrupted=false;
        try {
            for(Entry entry:entries)finished.submit(()->{
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
                try(InputStream in=resolver.openInputStream(entry.uri);OutputStream out=new FileOutputStream(entry.file)) {
                    if(in==null)throw new IOException("Cannot open game file");
                    byte[] buffer=buffers.get();int n;
                    while((n=in.read(buffer))!=-1) {
                        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
                        if(bytes.addAndGet(n)>LIMIT)throw new IOException("Game too large");
                        out.write(buffer,0,n);report.run();
                    }
                }
                done.incrementAndGet();report.run();return null;
            });
            for(int n=0;n<entries.size();n++)finished.take().get();
            if(progress!=null)progress.update(done.get(),entries.size(),bytes.get(),expected);
        } catch(InterruptedException e){interrupted=true;throw new InterruptedIOException("Import interrupted");}
        catch(ExecutionException e){throw new IOException("Cannot copy game",e.getCause());}
        finally {
            pool.shutdownNow();
            // Do not let the caller clean a pending directory while writers still use it.
            boolean stopped=false;
            while(!stopped){try{stopped=pool.awaitTermination(1,TimeUnit.SECONDS);}catch(InterruptedException e){interrupted=true;}}
            if(interrupted)Thread.currentThread().interrupt();
        }
    }
}
