package concurrent.phase3;

import java.util.Random;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

/**
 * 这里模拟CyclicBarrier的使用，其中计数器可以重复使用。
 * 比如这里有假设有多个线程先需要所有人参加完长跑，然后再进行自行车比赛，然后再进行跳高。
 * 这就好比三个比赛阶段
 */
public class Demo18 {

    private static final Random random = new Random(System.currentTimeMillis());

    static class Athlete extends Thread{

        private final int no;
        private final Phaser phaser;

        public Athlete(int no, Phaser phaser) {
            this.no = no;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            try {
                doPhaseWork(": start running.", ": end running.");
                doPhaseWork(": start bicycle.", ": end bicycle.");
                doPhaseWork(": start jump.", ": end jump.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void doPhaseWork(String s, String s2) throws InterruptedException {
            System.out.println(no + s);
            TimeUnit.SECONDS.sleep(random.nextInt(5));
            System.out.println(no + s2);
            phaser.arriveAndAwaitAdvance();
        }
    }

    public static void main(String[] args) {
        // 一次性注册5个线程
        Phaser phaser = new Phaser(5);
        for (int i = 0; i < 5; i++) {
            new Athlete(i, phaser).start();
        }
    }
}
