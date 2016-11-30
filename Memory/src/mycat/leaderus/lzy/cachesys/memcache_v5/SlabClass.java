package mycat.leaderus.lzy.cachesys.memcache_v5;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by 行知道人 on 2016/11/29.
 */
public class SlabClass {

    private static ByteBuffer allMem = ByteBuffer.allocateDirect(Integer.MAX_VALUE);
    private static LinkedBlockingQueue<Slab> slabs = new LinkedBlockingQueue<Slab>();
    private static LinkedBlockingQueue<Slab> used = new LinkedBlockingQueue<Slab>();
    static {
        for (int i = 0; i < Integer.MAX_VALUE/MemConfig.SLAB_SIZE ; i++) {
            allMem.position(i*MemConfig.SLAB_SIZE);
            allMem.limit((i+1)*MemConfig.SLAB_SIZE);
            slabs.add(new Slab(allMem.slice()));
        }

        new Thread(){
            @Override
            public void run() {
                while(true){
                    if(used.size()>0) {
                        Slab tmp = null;
                        boolean flag = false;
                        while ((tmp = used.remove())!=null){
                            long timeout = System.currentTimeMillis();
                            Chunk[] tmpChunks = tmp.getChunks();
                            for (int i = 0; i < tmpChunks.length; i++) {
                                if(!ManagerMemory.removeEmptyChunk(tmpChunks[i])) {
                                    if (tmpChunks[i].getTimeout() > timeout) {
                                        flag = true;
                                        break;
                                    }else{
                                        ManagerMemory.removeUsedChunk(tmpChunks[i]);
                                        ReadWritePool.remove(tmpChunks[i].getKey());
                                    }
                                }
                            }
                            if(!flag){
                                slabs.add(tmp);
                            }
                        }
                    }
                    try {
                        sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
        }.start();

    }

    static Slab getSlab(){
        Slab tmp = null;
        if(slabs.size()>0) {
            tmp = slabs.remove();
            used.add(tmp);
        }
        return tmp;
    }
}