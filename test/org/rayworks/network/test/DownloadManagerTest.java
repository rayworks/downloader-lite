package org.rayworks.network.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rayworks.network.download.DownloadEnabledStrategy;
import org.rayworks.network.download.DownloadManager;
import org.rayworks.network.download.DownloadSetting;
import org.rayworks.network.download.cache.DiskFileCache;
import org.rayworks.network.download.listener.DownloadListener;
import org.rayworks.network.download.listener.SingleDownloadListener;
import org.rayworks.network.storage.SyncStateStore;

public class DownloadManagerTest {
    private final String OUTPUT_FOLDER = "out";

    private DownloadManager downloadMgr;
    private SyncStateStore store;

    private ExecutorService executor;
    
    private DownloadSetting downloadSetting;

    @Before
    public void setUp() throws Exception {
        store = new SyncStateStore(new JsonKVStore(OUTPUT_FOLDER));
        executor = Executors.newSingleThreadExecutor();
        DiskFileCache.Limits limits = new DiskFileCache.Limits((int) 13.5 * 1024 * 1024, 0);
        downloadSetting = new DownloadSetting.Builder().setDownloadEnabledStrategy(new DownloadEnabledStrategy() {
            @Override
            public boolean isNetworkAvailableForDownloading() {
                return true;
            }
        }).setThreadNum(2).create();
        
        downloadMgr = new DownloadManager(
                store, downloadSetting, new ConnectivityServiceImpl(), new DeviceStorageMonitorImpl(),
                new DiskFileCache(new File(OUTPUT_FOLDER), executor)
        );
    }

    @After
    public void tearDown() throws Exception {
        if (downloadMgr != null) {
            downloadMgr.cancelAllTasks();
        }
        if(executor != null){
            executor.shutdownNow();
        }
    }
    
    @Test
    public void testMultiTasks(){
        addBatchedTask(new String[]{
                "http://assets-cdn.github.com/images/modules/open_graph/github-logo.png",
                "http://assets-cdn.github.com/images/modules/aws/cloud-1.png",
                //"http://assets-cdn.github.com/images/modules/aws/cloud-4.png"

        });

        addSingleTask("http://music.baidu.com/cms/BaiduMusic-musicsybutton.apk");

        addBatchedTask(new String[]{
                "http://assets-cdn.github.com/images/modules/aws/cloud-4.png",
                "http://music.baidu.com/cms/BaiduMusic-musicsybutton.apk"
        });

        // prioritize the task
        //String url = "http://";
        
        try {
            /*Thread.sleep(200);
            
            EFLogger.d("TEST", "ready to schedule the new tasks");

            DownloadListener singleDownloadListener = new DownloadListener() {
                @Override
                public void onComplete(String remotePath) {
                    System.out.println("<<< New complete single task " + remotePath);
                }

                @Override
                public void onError(String error) {
                    EFLogger.d("TEST", error);
                }

                @Override
                public void onProgress(int percentageComplete, String remotePath) {
                    System.out.println("<<< New Progress single -- " + percentageComplete);
                }
            };
            
            downloadMgr.prioritizeNewTask(url, singleDownloadListener);*/

            Thread.sleep(30 * 1000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            File file = new File(OUTPUT_FOLDER);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                assertTrue(file.length() > 1);
            }
        }
    }


    private void addBatchedTask(String[] urls) {
        if(downloadMgr.haveAllFilesDownloaded(Arrays.asList(urls))){
            System.out.println(">>> Files cache hit, task cancelled: urls " + Arrays.toString(urls));
        }else {
            downloadMgr.addBatchedTask(Arrays.asList(urls), new SingleDownloadListener());
        }
    }

    private void addSingleTask(String singleUrl ) {
        DownloadListener singleDownloadListener = new DownloadListener() {
            @Override
            public void onComplete(String remotePath) {
                System.out.println("<<< complete single task " + remotePath);
            }

            @Override
            public void onError(String error) {

            }

            @Override
            public void onProgress(int percentageComplete, String remotePath) {
                System.out.println("<<< Progress single -- " + percentageComplete);
            }
        };

        downloadMgr.add(singleUrl, singleDownloadListener);
    }
}
