package com.example.gy.muzik;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.example.gy.muzik.Apprtc.RingActivity;
import com.example.gy.muzik.MyProfile.MaProfileActivity;
import com.example.gy.muzik.liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import com.example.gy.muzik.liveVideoPlayer.LiveVideoPlayerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class MyService extends Service {
    private final String MYSERVICE_TAG = "MYSERVICETAG";

    public static int MSG_SEND_TO_ACTIVITY = 909;    // 서버 -> 서비스 -> 액티비티
    public static int MSG_SEND_TO_BROADCAST = 808;    // 서버 -> 서비스 -> 방송자 화면
    public static int ROOMRESPONSE_SEND_TO_MAIN = 708; // 방송목록요청 결과 -> MainActivity로
    public static int MSG_SEND_TO_SERVICE = 202;
    public static int HEARTRESPONSE_SEND_TO_BROAD = 608;  // 서버 -> 서비스 -> 좋아요

    private String HOST = "52.79.94.54";
    private int PORT = 5555;

    /**
     * netty 에서 소켓채널을 통해 통신한다 파이프라인사용
     * 이곳에 서버ip 와 포트번호 정보가 담겨져 있다
     * 전역변수로 선언해서 사용
     * <p>
     * 1. onStartCommand 에 소켓연결 스레드 ( 서버에서 전달한 메세지 받는 스레드 ) 돌려서 지속적으로 서버와 연결하고 있기
     * 2. 서버 -> 서비스 Receive_thread 도 계속 연결되어야 하겠지
     * 3. Receive_thread 에서 받은 메세지 내용 -> 액티비티로 전달
     * <p>
     * ------- 메세지 보낼때 --------
     * 4. 메세지 보낼때는 그때만 연결이 필요한 상황이다. -> iBinder 사용해야한다
     * 5. 액티비티 ( 전송버튼 ) bindService(intent,connection,autobind생성) ->  서비스 onBind return messanger.getBinder()
     * 6. send_thread  서버로 보내는 스레드 -> 서버
     */
    private SocketChannel socketChannel;


    Receive_thread receive_thread; // 서버로부터 메세지 받는 스레드 /

    ServerConnectClient serverConnectClient;// 클라이언트가 서버로 연결요청 이 안에 메세지 받는 부분도 같이 있다*/

    public static Send_thread send_thread;   // 클라이언트가 채팅액티비티에서 받은 내용을 서버로 보낼때 쓰는 스레드*/

    public final IBinder mBinder = new MyBinder(); /*/ 클라이언트 activity에게 반환할 Ibinder 객체 생성*/


    public static Handler serviceHandler;  /*/ 서비스쪽 핸들러  < = > 액티비티와 통신해야한다 */

    private Messenger activity_messenger = null; // 액티비티에서 가져온 메신져 정보

    public MyService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();


        receive_thread = new Receive_thread(); // 서버 -> 서비스 받는 스레드 객체 생성

        serviceHandler = new Handler(new Handler.Callback() {  // 액티비티 -> 서비스 핸들러 ( 채팅내용 )
            @Override
            public boolean handleMessage(Message msg) {

                switch (msg.what) {

                    case LiveVideoPlayerActivity.MSG_ACTIVITY_TO_SERVICE:   // 시청자액티비티에서 채팅메세지 요청
                        String activity_msg = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "액티비티에서 전달받은msg" + activity_msg);
                        /** 액티비티 메세지를 채널소켓에 담아 서버로 보내줘야 한다
                         *  send_thread 실행
                         * */
                        send_thread = new Send_thread(activity_msg);  // 채팅메세지 서비스 -> 서버로 전송
                        send_thread.start();
                        break;
                    case LiveVideoBroadcasterActivity.ROOMCREATE_ACTIVITY_TO_SERVICE: // 방송자액티비티에서 방생성 요청
                        String roominfo = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "방생성 정보 broadcast로부터 전달받음:" + roominfo);

                        send_thread = new Send_thread(roominfo);
                        send_thread.start();

                        break;

                    case MainActivity.ROOMREQUEST_ACTIVITY_TO_SERVICE:  // 방송 목록 요청 jsonArray에 담겨있는 것을 가져올것이다
                        String roomrequest = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "라이브 방 요청 main으로부터 전달받음: " + roomrequest);

                        send_thread = new Send_thread(roomrequest);
                        send_thread.start();

                        break;

                    case MainActivity.ROOMENTER_ACTIVITY_TO_SERVICE:    // 채팅방 입장 요청 -> 서버
                        String roomenterrequest = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "라이브 방 입장 요청 main으로부터 전달받음 :" + roomenterrequest);

                        send_thread = new Send_thread(roomenterrequest);
                        send_thread.start();

                        break;

                    case LiveVideoPlayerActivity.ROOMOUT_ACTIVITY_TO_SERVICE:   // 채팅방 나가기 요청 -> 서버
                        String roomoutrequest = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "라이브 방 나가기 요청 :" + roomoutrequest);

                        send_thread = new Send_thread(roomoutrequest);
                        send_thread.start();

                        break;

                    case LiveVideoBroadcasterActivity.ROOMDELETE_ACTIVITY_TO_SERVICE: // 방송 종료 요청 메세지 서버에 전달
                        String roomquitrequest = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "방송 종료 요청 broadcast로부터 전달받음: " + roomquitrequest);

                        send_thread = new Send_thread(roomquitrequest);   // 추후에 방나누기 완성하면 라이브방송 완성하자
                        send_thread.start();

                        break;

                    case LiveVideoPlayerActivity.HEART_ACTIVITY_TO_SERVICE:
                        String heartrequest = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "하트요청 player로부터 전달받음" + heartrequest);

                        send_thread = new Send_thread(heartrequest);
                        send_thread.start();
                        break;

                    case MaProfileActivity.APPRTC_ACTIVITY_TO_SERVICE:
                        String apprtcrequest = msg.obj.toString();
                        Log.i(MYSERVICE_TAG, "apprtc profile로부터 전달받음" + apprtcrequest);

                        send_thread = new Send_thread(apprtcrequest);
                        send_thread.start();
                        break;
                }
                return false;
            }
        });


    }

    // 서비스가 호출될때마다 실행됨 //

    /**
     * 액티비티에서 전송한 인텐트를 받아서 처리하는 콜백 메소드
     * startService() 실행하면 거치는 곳
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MYSERVICE_TAG, "onStartCommand() 호출됨");

        /** 서비스가 비정상적으로 종료되었을 때 시스템이 자동으로 재시작하도록 해당 상수 리턴*/
        if (intent == null) {
            Log.e(MYSERVICE_TAG, "서비스비정상종료재시작함");
            return Service.START_NOT_STICKY;

        } else {

            /** 여기에 커낵션 스레드 내용이 들어가야 한다 */
            Log.d(MYSERVICE_TAG, "onStartCommand() 서비스돌아감 ");


            //---------- 서버와 연동하는 스레드 실행
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socketChannel = SocketChannel.open();
                        socketChannel.configureBlocking(true);
                        socketChannel.connect(new InetSocketAddress(HOST, PORT));

                    } catch (Exception e) {
                        Log.e("서버와 연동하는 스레드실패", e.getMessage() + "a");
                        e.printStackTrace();
                        ;
                    }
                    receive_thread.start();
                }
            }).start();

        }
        *//--------------접속 스레드-----------------
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }


    @Override
    public void onDestroy() {
        Log.d(MYSERVICE_TAG, "onDestroy()");
        super.onDestroy();
    }


    /**
     * 음악플레이어 처럼 액티비티에서 정지 누르면 서비스에서도 정지되듯이 액티비티와 서비스 간 연결되있는거 구현
     * <p>
     * 액티비티에서 데이터 전달 onBind(Intent intent)
     * <p>
     * 서비스에서 return 값 IBinder 이다
     * <p>
     * 클라이언트가 bindService()를 호출하면 호출되는 메소드
     * 바인더 객체를 클라이언트에게 반환하는 메소드
     * 바인더 객체를 클라이언트에게 반환하기 위해서는 서비스 안에 Binder 클래스를 정의해야한다
     * <p>
     * 서비스 바인딩은 연결된 액티비티가 사라지면 서비스도 소멸된다 ( 백그라운드에서 무한히 실행되진 않는다 )
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        Log.i(MYSERVICE_TAG, "onBind()");
        // name=intent.getExtras().getString("name","do");
        //  Log.w(MYSERVICE_TAG,"이름 받아옴 : "+name);


        return mBinder;
        // return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(MYSERVICE_TAG, "onUnbind()");
        Toast.makeText(MyService.this, "언바운드됨", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }


    /**
     * 바인더 클래스 정의 클라에게 반환해줄 바인더 객체를 위한 클래스 바인더 클래스를 상속해서 클래스를 정의하면
     * onBind()
     */
    public class MyBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }


    public class ServerConnectClient extends Thread {
        boolean isRun = true; // 서비스 상태

        public ServerConnectClient() {
        }

        @Override
        public void run() {
            super.run();

            try {
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                socketChannel.connect(new InetSocketAddress(HOST, PORT));

            } catch (IOException e) {
                Log.e(MYSERVICE_TAG, "소켓연결에러 : " + e.getMessage());
                e.printStackTrace();
            }

            /** 메세지 받는 스레드도 같이 실행시켜 주자
             *  receive_thread 도 같이 실행
             * */
            receive_thread.start();

        }
    }
//---------------end ServerConnectClient----------------------

    public class Receive_thread extends Thread {
        String serverMsg = null;

        public Receive_thread() {
        }

        @Override
        public void run() {
            super.run();

            while (true) {

                try {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(2048);    // 할당량이 작아 서버에서 보낸 값을 다 못 받아온다
                    int readByteCount = socketChannel.read(byteBuffer);
                    Log.i(MYSERVICE_TAG, "readByteCount : " + readByteCount);
                    //서버가 정산적으로 소켓의 close를 호출했을 경우
                    if (readByteCount == -1) {
                        throw new IOException();
                    }
                    byteBuffer.flip(); // 바이트버퍼 문자열로 변환
                    Charset charset = Charset.forName("UTF-8");// 한글로도 변환가능
                    serverMsg = charset.decode(byteBuffer).toString();

                    Log.i(MYSERVICE_TAG, "서버에서 받은 메세지내용: " + serverMsg);

                    if (serverMsg != null) {
                        /** 채팅 메세지나 방목록 메세지 액티비티에 띄워줘야 한다
                         *  핸들러를 통해 메세지를 방 목록에 보낸다
                         *  아래 Message recivemsg 에서 null point 에러가 계속 뜬다 당연히
                         *  방송자 액티비티이 켜진 상황에서는 시청자 액티비티의 핸들러 객체생성이 안되기 때문이다
                         *  try catch 로 에러를 잡아주고 null 인 상황에서 방송자액티비티에 채팅메세지를 보내줬다
                         * */
                        // 사용자에게 메세지 전달 // 나중에 타입 분석해서 메세지 내용 뿌려줘 방, 채팅 등

                        // 아래는 채팅 내용 채팅 내용은 서버에서 이미 파싱되서 나왔고, 방목록은 jsonArray 형태로 받아와짐
                        try {
                            JSONObject serverobject = new JSONObject(serverMsg);  // 서버로부터 전달받은 메세지를 각각 알맞게 나눠줘야 한다
                            String servertype = serverobject.get("type").toString();

                            if (servertype.equals("chat")) {     // 채팅메세지 처리
                                Log.w(MYSERVICE_TAG, "서버메세지 = 채팅");

                                try {

                                    Message recivemsg = LiveVideoPlayerActivity.handler.obtainMessage(); // 방송자 화면일때 시청자액티비티는 안 띄워주니깐 핸들러 생성되지 않는다 => null point 뜬다
//                            if (recivemsg==null){
//                                 방송자 화면 상황에서 시청자쪽 핸들러를 참조하지 못했을때 => 방송자 화면에 메세지 보내기
//                                 방송자에게도 메세지 전달
//                                Log.d(MYSERVICE_TAG,"방송자에게 메세지전달");
//                                Message receivemsgtobroad= LiveVideoBroadcasterActivity.broadHandler.obtainMessage();
//                                receivemsgtobroad.what=MSG_SEND_TO_BROADCAST;
//                                receivemsgtobroad.obj=serverMsg;
//
//                                LiveVideoBroadcasterActivity.broadHandler.sendMessage(receivemsgtobroad);
//
//                            }else {
//                                 시청자에게 메세지 보내기

                                    Log.i("서비스에서 시청자에게 메세지", "널포인트 뜨냐" + recivemsg.toString());
                                    recivemsg.what = MSG_SEND_TO_ACTIVITY;
                                    recivemsg.obj = serverMsg;
                                    Log.e(MYSERVICE_TAG, "널뜨는곳 진짜 널이냐" + recivemsg.toString());
                                    LiveVideoPlayerActivity.handler.sendMessage(recivemsg);

//                            }


                                } catch (Exception e) {
                                    e.printStackTrace();                    // 시청자 액티비티의 핸들러가 null point 뜰때 예외처리해주고 여기에서 방송자액티비티의 핸들러로 메세지를 보내준다
                                    Log.e("null에러", e.getMessage());
                                    Log.e(MYSERVICE_TAG, "null 잡히나? " + serverMsg);
                                    Log.d(MYSERVICE_TAG, "방송자에게 메세지전달");
                                    Message receivemsgtobroad = LiveVideoBroadcasterActivity.broadHandler.obtainMessage();
                                    receivemsgtobroad.what = MSG_SEND_TO_BROADCAST;
                                    receivemsgtobroad.obj = serverMsg;

                                    LiveVideoBroadcasterActivity.broadHandler.sendMessage(receivemsgtobroad);
                                }


                            } else if (servertype.equals("apprtc")) {
                                Log.i(MYSERVICE_TAG, "apprtc 요청들어옴" + serverobject.toString());

                                Intent gotoRingIntent = new Intent(getApplicationContext(), RingActivity.class);
                                gotoRingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                gotoRingIntent.putExtra("apprtcresponse", serverobject.toString());
                                startActivity(gotoRingIntent);

                            } else if (servertype.equals("roomresponse")) {   // 서버에서 전달한 방송목록 응답 처리
                                Log.w(MYSERVICE_TAG, "서버메세지 = 방목록 응답");
                                String responseroom = serverobject.get("response").toString();
                                Log.i(MYSERVICE_TAG, "방요청받은거" + responseroom);

                                try {

                                    Message roomresponsemsg = MainActivity.MainHandler.obtainMessage();
                                    roomresponsemsg.what = ROOMRESPONSE_SEND_TO_MAIN;
                                    roomresponsemsg.obj = responseroom;   // jsonArray형태의 방 정보 string

                                    MainActivity.MainHandler.sendMessage(roomresponsemsg);  // 메인으로 방송목록 jsonarray 메세지 보내기

                                } catch (Exception e) {
                                    Log.e(MYSERVICE_TAG, "방요청에러메세지" + e.getMessage());


                                }


                            } else if (servertype.equals("heart")) {
                                Log.w(MYSERVICE_TAG, "서버메세지=좋아요 응답: " + serverobject.toString());
                                String heartresponse = serverobject.toString();

                                try {

                                    Message heartresponsemsg = LiveVideoBroadcasterActivity.broadHandler.obtainMessage();
                                    heartresponsemsg.what = HEARTRESPONSE_SEND_TO_BROAD;
                                    heartresponsemsg.obj = heartresponse;

                                    LiveVideoBroadcasterActivity.broadHandler.sendMessage(heartresponsemsg);


                                } catch (Exception e) {
                                    Log.e(MYSERVICE_TAG, "좋아요 에러 : " + e.getMessage());
                                }

                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

//                        try{
//
//                            Message recivemsg= LiveVideoPlayerActivity.handler.obtainMessage();
////                            if (recivemsg==null){
////                                 방송자 화면 상황에서 시청자쪽 핸들러를 참조하지 못했을때 => 방송자 화면에 메세지 보내기
////                                 방송자에게도 메세지 전달
////                                Log.d(MYSERVICE_TAG,"방송자에게 메세지전달");
////                                Message receivemsgtobroad= LiveVideoBroadcasterActivity.broadHandler.obtainMessage();
////                                receivemsgtobroad.what=MSG_SEND_TO_BROADCAST;
////                                receivemsgtobroad.obj=serverMsg;
////
////                                LiveVideoBroadcasterActivity.broadHandler.sendMessage(receivemsgtobroad);
////
////                            }else {
////                                 시청자에게 메세지 보내기
//
//                                Log.i("서비스에서 시청자에게 메세지","널포인트 뜨냐"+recivemsg.toString());
//                                recivemsg.what=MSG_SEND_TO_ACTIVITY;
//                                recivemsg.obj=serverMsg;
//                                Log.e(MYSERVICE_TAG,"널뜨는곳 진짜 널이냐"+recivemsg.toString());
//                                LiveVideoPlayerActivity.handler.sendMessage(recivemsg);
//
////                            }
//
//
//
//                        }catch (Exception e){
//                            e.printStackTrace();
//                            Log.e("null에러",e.getMessage());
//                            Log.e(MYSERVICE_TAG,"null 잡히나? "+serverMsg);
//                            Log.d(MYSERVICE_TAG,"방송자에게 메세지전달");
//                            Message receivemsgtobroad= LiveVideoBroadcasterActivity.broadHandler.obtainMessage();
//                            receivemsgtobroad.what=MSG_SEND_TO_BROADCAST;
//                            receivemsgtobroad.obj=serverMsg;
//
//                            LiveVideoBroadcasterActivity.broadHandler.sendMessage(receivemsgtobroad);
//                        }

                    }


                } catch (IOException e) {

                    Log.e(MYSERVICE_TAG, "receive_thread에서 에러나서 채널끊어줌" + e.getMessage());
                    try {
                        socketChannel.close();
                        break;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }

            }

        }
    }
//----------------end Receive_thread------------------------------

    public class Send_thread extends Thread {
        String sendmsg;

        public Send_thread(String sendmsg) {
            this.sendmsg = sendmsg;
        }

        @Override
        public void run() {
            super.run();
            try {
                socketChannel.socket().getOutputStream().write(sendmsg.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}
