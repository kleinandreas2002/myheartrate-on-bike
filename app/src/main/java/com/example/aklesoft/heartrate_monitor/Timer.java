//package com.example.aklesoft.heartrate_monitor;
//
///**
// * Created by Thunder on 31.10.2017.
// */
//
//public class Timer implements Runnable {
//
//    private int result = 0;
//    private boolean pause = false;
//
//    @Override public void run()
//    {
//        pause = false;
//        result = 0;
//        while (true) {
//
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                }
//
////            result++;
////            result = result % 100;
////            handler.post(new Runnable() {
////                @Override
////                public void run() {
////                    tvStatus.setText("" + result);
////                }
////            });
//
//        }
//    }
//    public void setPause(boolean pause) {
//        this.pause = pause;
//    }
//
//    public boolean isPause() {
//        return pause;
//    }
//
//    public void reset() {
//        this.result = 0;
////        handler.post(new Runnable() {
////            @Override
////            public void run() {
////                tvStatus.setText("" + result);
////            }
////        });
//    }
//}
