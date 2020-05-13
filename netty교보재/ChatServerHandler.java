package com.company;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 네티는 데이터의 읽기 쓰기를 위한 이벤트 핸들러 지원한다.
 * <p>
 * 데이터를 소켓으로 전송하기 위해 채널에 직접 기록하는것이 아니라 데이터핸들러를 통해서 기록한다.
 * 장점 : 서버코드를 클라이언트에서 재사용하는 장점
 * <p>
 * 이 이벤트 핸들러를 통해서 파이프라인에 전송된 이벤트를 수신하고 처리해준다.
 * <p>
 * 순서
 * 1. 클라가 접속했을때 => channelRegistered, channelActive
 * 2. 클라 -> 서버에 메세지 보낼때 => channelRead
 * 3. 모든 메시지 읽고 없을때 => channelReadComplete
 * 4. 접속이 끊어 졌을때 => channellactive, channelUnregistered
 * <p>
 * <p>
 * <p>
 * <p>
 * 채팅방 나누기 과정
 * 1. 클라 접속 -> channelActive 에서 채널그룹에 모두 저장 됨  채팅방은 따로 만들어 넣어줘야 한다.
 * 2. 채팅방 에 접속한 사람들 추가해주기 -> channelRead 에서 작업해줘야 하나?
 */
public class ChatServerHandler extends ChannelInboundHandlerAdapter {

    // 채널 그룹 이 방같은 개념인가 // 배열에 쓰이는 거구낭 개꿀띵
    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static JSONArray jsonArrayroomlist = new JSONArray(); // 라이브 방 목록 담을 리스트이다

    // 채팅방 관련 객체
    private static ArrayList<Channel> userlist; // 채팅방에 참가하는 유저를 담을 리스트이다. : 채팅방 하나
    private static HashMap<String, ArrayList<Channel>> chatroomhashmap = new HashMap<>();  // string : 방이름 ArrayList : 방에참가하고있는 유저들

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded of [SERVER]");
        Channel incoming = ctx.channel();               // 채널이 클라이언트 1, 2, 3~

        for (Channel channel : channelGroup) {          // 배열에 있는 거 다 쓰라
            //사용자가 추가되었을 때 기존 사용자에게 알림
            // channel.write("[SERVER] - " + incoming.remoteAddress() + "has joined!\n");
        }
        channelGroup.add(incoming);     // 서버에 접속해 있는 모든 사람들
        // userlist.add(incoming);       // 사람들 입장하면 유저리스트에 담아 주자 => 이건 앱에 접속한 모든 사람들을 담는 것이자나
    }

    /**
     * 채널이 이벤트루프에 등록되었을때 발생
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 사용자가 접속했을 때 서버에 표시.
        /** 사용자가 접속했을때 서버에 표시
         네티 api를 사용하여 채널 입출력을 수행할 상태다

         어떤 작업을 할 수 있을까
         클라이언트 연결 개수를 셀 때
         최초 연결 메세지
         연결된 상태에서 작업이 필요할 때 ==> 여기서 채팅방 나눠줘야 하나

         */

        System.out.println("User Access!");
        System.out.println("현재참가자수: " + channelGroup.size());
    }

    // 시청자가 나갔을때 호출되는 곳
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved of [SERVER]");
        Channel incoming = ctx.channel();
        for (Channel channel : channelGroup) {
            //사용자가 나갔을 때 기존 사용자에게 알림
            channel.write("[SERVER] - " + incoming.remoteAddress() + "has left!\n");
        }
        channelGroup.remove(incoming);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        /** 데이터 수신이 완료되었음을 알려준다
         데이터를 다 읽어 없을때 발생
         클라가 a,b,c 순차적 전송 channelRead 이벤트 발생 : 만약 수신데이터 abc 이면 channelReadComplete // 아님 channelRead
         **/

        ctx.flush();


    }


    /**
     * channelRead 는 클라이언트에서 데이터를 보내면 호출되는 메서드이다. 클라이언트에서 보낸 메세지 받는 부분
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        String message = null;
        message = (String) msg;

        //Channel person=ctx.channel();

        // jsonparser 로 받아주기 바로 jsonobject 해줄수가 없다 안드로이드랑 다름
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

//        if(jsonObject.get("type").toString().equals("live")){
//
//        }else{
//
//        }
        try {

            String type = jsonObject.get("type").toString(); // 타입에 따라 채팅인지 방요청인지 구분하기

            if (type.equals("chat")) {   // 채팅 메세지 => 받은 내용 json 형태로 그대로 주기


                String realname = jsonObject.get("chatuserid").toString();
                String realmsg = jsonObject.get("chatmsg").toString();

                System.out.println("channelRead of [SERVER] " + realname + " : " + realmsg);
                Channel incoming = ctx.channel();                       // 클라이언트 1,2,3 : 채팅방에 참가할 사람들 bj 와 사람들
                System.out.println("채팅보낸 놈 채널정보 : " + incoming.toString());

                // 채널 정보 중복체크해서 담아주자 채팅object 안의 키값과 방정보object 안의 키값은 다르다
                String chatroomid = jsonObject.get("chatroomid").toString(); // 채팅메세지 object안 채팅방정보
                // 해쉬맵에 있는 방정보를 위 메세지의 불러와 객체를 가져오기
                // 채팅할때마다 방정보를 확인하는 것은 너무 불편
                userlist = (ArrayList<Channel>) chatroomhashmap.get(chatroomid);  // 채팅내용중 roomid 가져와서 해당 방을 가져온다

                for (Channel channel : userlist) {  // for each문
                    if (channel != incoming) {
                        //메시지 전달.
                        //channel.writeAndFlush("[" + incoming.remoteAddress() + "]" + message + "\n");
                        //channel.writeAndFlush(realname+" "+realmsg);
                        channel.writeAndFlush(jsonObject.toString());

                    }
                }
                if ("bye".equals(message.toLowerCase())) {
                    ctx.close();
                }


            } else if (type.equals("heart")) {     // 하트 발송

                String chatroomid = jsonObject.get("chatroomid").toString();

                Channel incoming = ctx.channel();

                userlist = (ArrayList<Channel>) chatroomhashmap.get(chatroomid);

                for (Channel channel : userlist) {

                    if (channel != incoming) {
                        channel.writeAndFlush(jsonObject.toString());
                    }
                }
                System.out.println("좋아요누름");
                if ("bye".equals(message.toLowerCase())) {
                    ctx.close();
                }


            } else if (type.equals("apprtc")) {
                String userid = jsonObject.get("userid").toString();
                System.out.println(userid + "apprtc요청 roomid : " + jsonObject.get("apprtcroomid").toString());

                Channel incoming = ctx.channel();

                for (Channel channel : channelGroup) {

                    if (channel != incoming) {
                        channel.writeAndFlush(jsonObject.toString());
                    }

                }


            } else if (type.equals("live")) {                 // 라이브 방송 방 생성 요청
                System.out.println("방생성 메세지" + message);
                //json형태로 받은 메세지 고대로 모두에게 전달해주기 +
                jsonArrayroomlist.add(jsonObject);
                System.out.println("방생성제이슨어레이 : " + jsonArrayroomlist.toString());

                // 방생성한 bj 채팅방=방송방 입장
                Channel incoming = ctx.channel();
                userlist = new ArrayList<>(); // 채팅방 생성 // 추후 다른이가 새로운 방을 생성함 다른이가 방생성할 때 마다 new arraylist 생성
                userlist.add(incoming);   // 유저리스트에 bj 입장
                String roomkey = jsonObject.get("livelistroomid").toString();    // 방id 키값이 될것이다. key값 : 방만든 시간
                chatroomhashmap.put(roomkey, userlist);          // 방생성할때마다 채팅방리스트에 넣어주기
                System.out.println("방생성 채팅방갯수 : " + chatroomhashmap.size() + " 채팅방 참여수 : " + userlist.size() + " 방id : " + roomkey + " 방장 : " + incoming.toString());
                System.out.println("방생성 후 채팅방정보" + userlist.toString());


            } else if (type.equals("roomrequest")) {   // 라이브 방송목록 요청
                System.out.println("라이브방 요청" + message);

                // System.out.println("방목록제이슨어레이 : "+jsonArrayroomlist.toString());
                // 접속해 있는 모든이에게 라이브 방송목록 전달해주기
                // json 내용에 key 값 roomresponse
                JSONObject object = new JSONObject();
                object.put("type", "roomresponse");
                object.put("response", jsonArrayroomlist.toString());
                System.out.println("방목록 키값붙혀 : " + object.toString());

                Channel incoming = ctx.channel();
                //  for(Channel channel : channelGroup){  // 방송목록 요청한 사람한테만 방목록 보내주면 되지


                incoming.writeAndFlush(object.toString());


                //  }
                if ("bye".equals(message.toLowerCase())) {
                    ctx.close();
                }

                // 서버 -> 서비스 받아온 메세지가 방송목록인지 채팅내용인지 jsonobject에 넣어서 구분해줘야하나
            } else if (type.equals("roomquit")) {  // 라이브 방송 종료 요청 메세지 => 방송목록 삭제해줘야 한다.
                System.out.println("방송종료 요청" + message);

                // 방송목록 삭제 jsonarray에서 해당 방정보 삭제해줘야 한다
                String roomid = jsonObject.get("roomid").toString();
                String userid = jsonObject.get("userid").toString();

                // jsonarray 삭제해주고 userlist hashmap 에 있는 내용도 지워주자
                boolean isContains = chatroomhashmap.containsKey(roomid);     // 지우려는 방 이 있으면 true
                if (isContains) { // 해쉬맵에서 해당 방이 있으면
                    chatroomhashmap.remove(roomid);

                    // json array 에서 roomid 가 같은거 찾아내기 -> 해당 방목록 지워주기
                    for (int i = 0; i < jsonArrayroomlist.size(); i++) {
                        JSONObject jsonObject1 = (JSONObject) jsonArrayroomlist.get(i);
                        String targetremoveroomid = jsonObject1.get("livelistroomid").toString();
                        if (targetremoveroomid.equals(roomid)) {
                            jsonArrayroomlist.remove(i);

                            // 제거해준 후 모든이에게 메세지 뿌릴일은 없자나 삭제만 해주면 되지
                            System.out.println("방송종료 방목록 갯수 : " + jsonArrayroomlist.size() + "채팅방 개수 : " + chatroomhashmap.size());
                        }

                    }

                }


            } else if (type.equals("roomenter")) { // 라이브 방송 입장 == 채팅 방 입장
                String roomid = jsonObject.get("livelistroomid").toString();
                String userid = jsonObject.get("userid").toString();

                Channel incoming = ctx.channel();

                userlist = (ArrayList<Channel>) chatroomhashmap.get(roomid);  // roomid에 해당하는 방 불러오기
                userlist.add(incoming);// 불러온 방에 새로운 참가자 추가해주기

                System.out.println("채팅방 입장 채팅방id : " + roomid + " 시청자 :" + userid + " 채널 :" + incoming.toString() + " 참여자수 :" + userlist.size());
                System.out.println("방 입장 후 채팅방정보" + userlist.toString());
                boolean isContains = chatroomhashmap.containsKey(roomid);
                if (isContains) {

                    chatroomhashmap.put(roomid, userlist);   // 새로 추가해준

                    System.out.println("채팅방리스트 갯수" + chatroomhashmap.size());

                }


            } else if (type.equals("roomout")) {        // 시청자가 라이브 방송 나가기  방송 종료는 아니다

                Channel incoming = ctx.channel();

                String roomid = jsonObject.get("roomid").toString();
                String userid = jsonObject.get("userid").toString();
                // 새로운 참가자 리스트를 만들어줘야 한다 해쉬맵에서 룸아이디에 해당하는 걸 찾아서 가져온다
                // 가져온 어레이 리스트에서 나갈 놈을 없애준다
                System.out.println("나갈려는 놈 채널 :" + incoming.toString());
                userlist = (ArrayList<Channel>) chatroomhashmap.get(roomid);
                userlist.remove(incoming);

                chatroomhashmap.put(roomid, userlist);
                System.out.println("방나가기 요청 채팅방갯수 :" + chatroomhashmap.size() + "참여자수 :" + userlist.size());
                System.out.println("방나가기 후 채팅방정보" + userlist.toString());

            }


        } catch (Exception e) {

            System.out.println(e.getMessage());

        }


    }


}
