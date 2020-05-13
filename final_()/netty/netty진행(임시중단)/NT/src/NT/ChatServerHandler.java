package NT;

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
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatServerHandler extends ChannelInboundHandlerAdapter {

    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //방 목록.
    //단지 방 목록만 저장하는 용도임(방 목록 요청시 리턴해 주기 위함)
    private static JSONArray jsonArrayroomlist = new JSONArray(); // 라이브 방 목록 담을 리스트이다
    
    // 채팅방에 참가하는 유저를 담을 리스트 : 채팅방 하나
    //userlist = (ArrayList<Channel>) chatroomhashmap.get(room);을 통해서 room에 해당하는 유저 리스트를 얻어올 수 있음.
    private static ArrayList<Channel> userlist;
    
    //chatroomhashmap.put(room, userlist);
    //또는
    //userlist = (ArrayList<Channel>) chatroomhashmap.get(room);
    //를 사용해 (키 = 방ID, Value = 유저리스트(배열))의 형태로 사용 가능
    private static HashMap<String, ArrayList<Channel>> chatroomhashmap = new HashMap<>();// string:방이름  /  ArrayList:방에참가하고있는 유저들

    /**
     * 사용자 입장시 안내
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

        System.out.println("handlerAdded of [SERVER]");

        Channel incoming = ctx.channel();
//        for (Channel channel : channelGroup) {
//            //사용자가 추가되었을 때 기존 사용자에게 알림
//            //channel.write("[SERVER] - " + incoming.remoteAddress() + "has joined!\n");
//        	/**
//        	 * 서버에만 알림으로 바꿈
//        	 */
//        	System.out.println("[안내] " + incoming.remoteAddress() + " - 입장\n");
//        }
//
//        System.out.println("[ChannelGroup Info(그룹name, indax(인원))] " + channelGroup);
//        System.out.println("[incoming / id : ] " + incoming.id());
//        System.out.println("[incoming / localAddress : ] " + incoming.localAddress());
//        System.out.println("[incoming / remoteAddress : ] " + incoming.remoteAddress());
//        System.out.println("[안내] - - - - - - - - - - - - - - - - - - - - - - - - Login user");
        channelGroup.add(incoming);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 사용자가 접속했을 때 서버에 표시.
        System.out.println("User Access - " + "Netty 채팅 서버 접속자 인원: " + channelGroup.size() + "\n");
    }

    /**
     * 사용자 퇴장시 안내
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        System.out.println("handlerRemoved of [SERVER]");

        Channel incoming = ctx.channel();
//        for (Channel channel : channelGroup) {
//            //사용자가 나갔을 때 기존 사용자에게 알림
//            //channel.write("[SERVER] - " + incoming.remoteAddress() + "has left!\n");
//        	/**
//        	 * 서버에만 알림으로 바꿈
//        	 */
//        	System.out.println("[안내] " + incoming.remoteAddress() + " - 퇴장\n");
//        }
//        System.out.println("[ChannelGroup Info(그룹name, indax(인원))] " + channelGroup);
//        System.out.println("[incoming / id : ] " + incoming.id());
//        System.out.println("[incoming / localAddress : ] " + incoming.localAddress());
//        System.out.println("[incoming / remoteAddress : ] " + incoming.remoteAddress());
//        System.out.println("[안내] - - - - - - - - - - - - - - - - - - - - - - - - Logout user");
        System.out.println("User Out - " + "Netty 채팅 서버 접속자 인원: " + channelGroup.size() + "\n");
        channelGroup.remove(incoming);
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 메세지 처리
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
       
        Channel incoming = ctx.channel();//User 핵심 정보

        String message = null;
        message = (String) msg;
        
        String type, room, id, usermsg;

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

        try {
            type = (String) jsonObject.get("type");
            room = (String) jsonObject.get("room");
            id = (String) jsonObject.get("id");
            usermsg = (String) jsonObject.get("msg");

            if ("make".equals(type)) {//방 만들기
                System.out.println("\n--------------"+id+" : 방만들기 요청--------------------");
                
                /**
                 * 방 제목(방 ID)가 만약 존재하는지 체크하고, 방이 만약 존재한다면 사용자에게 알리고 존재하지 않는 방이면 방을 생성하게됨
                 * 이걸 한번 체크하게 된 이유가 A방, B방으로 테스트 하는도중 A방을 2명이 생성하게되면 퇴장시 A방을 삭제하는데 이미 입장했던 다른 A방까지 사라져서 에러가 남.
                 */
                boolean isContains = chatroomhashmap.containsKey(room);//containsKey 는 맵(Map)에 해당 키(key)가 있는지를 조사하여 그 결과값을 리턴한다.
                if (isContains) {
                	//방이 이미 존재한다는걸 사용자에게 알려줘야함
                	//지금은 일단 서버에 메세지만 띄움.
                	System.out.println("- 방이 이미 존재하기 때문에 기존에 만들어져 있던 [" + room + "]방으로 입장처리");
                	
                }else{
                    jsonArrayroomlist.add(jsonObject);//방 목록
                    userlist = new ArrayList<>(); //방 참가자 추가를 위한(chatroomhashmap에 넣을) ArrayList
                    userlist.add(incoming); // 유저리스트에 사용자 추가
                    chatroomhashmap.put(room, userlist);//방에 이용자 추가
                    
//                    System.out.println("방생성 채팅방갯수 : " + chatroomhashmap.size());
//                    System.out.println("채팅방 참여수 : " + userlist.size());
//                    System.out.println("방id : " + room);
//                    System.out.println("방장 : " + incoming.toString());
//                    System.out.println("방생성 후 채팅방정보 : " + userlist.toString());
                    System.out.println("방제목 = [사용자들] : " + chatroomhashmap.toString());
                }
                
            } else if ("roomlist".equals(type)) {//채팅방 목록 요청
                System.out.println("\n--------------"+id+" : 채팅방 리스트 요청--------------------");

                System.out.println("방 목록 JsonArray : " + jsonArrayroomlist);
                
                JSONObject object = new JSONObject();
                object.put("response", jsonArrayroomlist);
                object.put("type", "chatroomlist");
                incoming.writeAndFlush(object.toString());//당사자에게

          }else if ("in".equals(type)) {//방 입장
                System.out.println("\n--------------"+id+" : 입장처리--------------------");
                
                userlist = (ArrayList<Channel>) chatroomhashmap.get(room);//roomid에 해당하는 방 불러오기
                
                /**
                 * 방 생성시 방참가자가 자동 추가되는데 방 생성 후 바로 방에 입장 처리를 하게되면 중복으로 2번 입장하게됨.
                 * 방장이 2번 입장하게되는셈.
                 * 그에따른 해결책 = 만약 방 입장 처리시 방장이 자신이라면 입장처리 하지 않음
                 */
                if (!chatroomhashmap.get(room).toString().equals("[" + incoming.toString() + "]")) {
                    userlist.add(incoming);// 불러온 방에 새로운 참가자 추가해주기
                    
                    boolean isContains = chatroomhashmap.containsKey(room);//containsKey 는 맵(Map)에 해당 키(key)가 있는지를 조사하여 그 결과값을 리턴한다.
                    if (isContains) {
                        chatroomhashmap.put(room, userlist);
                        System.out.println("입장한 방 ["+room+"]의 유저리스트" + userlist);
                        System.out.println("채팅방 개수 " + chatroomhashmap.size());
                    }
                }

            } else if ("out".equals(type)) {//방 퇴장
                System.out.println("\n--------------"+id+" : 퇴장처리--------------------");

                System.out.println("퇴장 전 유저리스트 : " + userlist);
                userlist = (ArrayList<Channel>) chatroomhashmap.get(room);
                
                /**
                 * 방에 남아있던 마지막 유저라면 방을 삭제해준다.
                 * if(userlist.size()==1)
                 */
                if(userlist.size()==1){
                	/**
                	 * 전달받은 room 을 이용해 chatroomhashmap에 존재하는 방이 있다면 삭제  
                	 */
                    boolean isContains = chatroomhashmap.containsKey(room); // 지우려는 방 이 있으면 true
                    //해쉬맵에서 해당 방이 있으면
                    if (isContains) {
                    	chatroomhashmap.remove(room);
                        // 전체 방 목록에서 room 이름을 이용해 해당 방목록 지워주기
                        for (int i = 0; i < jsonArrayroomlist.size(); i++) {
                            JSONObject jsonOBJ = (JSONObject) jsonArrayroomlist.get(i);
                            String targetremoveroom = (String) jsonOBJ.get("room");
                            if (targetremoveroom.equals(room)) {
                                jsonArrayroomlist.remove(i);
                                System.out.println("방의 마지막 유저가 퇴장하여 ["+room+"]방을 삭제함");
                            }
                        }
                    }
                }else{
                	userlist.remove(incoming);
                	chatroomhashmap.put(room, userlist);
                	
                	System.out.println("퇴장 후 유저리스트 : " + userlist);
                }

            } else if ("chat".equals(type)) {//채팅
                System.out.println("\n--------------"+id+" : 채팅--------------------");

                userlist = (ArrayList<Channel>) chatroomhashmap.get(room);  // 채팅내용중 roomid 가져와서 해당 방을 가져온다
                
                System.out.println("메세지를 전달할 채팅방 참여자 리스트 - ");
                System.out.println(userlist);
                System.out.println(id + " : " + usermsg+"\n");

                for (Channel channel : userlist) {
                    
                	/**
                	 * 본인(incoming)을 제외한 모두에게
                	 */
                	if (channel != incoming) {
                    	
                    	JSONObject object = new JSONObject();
                    	object.put("type", "chat");
                    	object.put("id", id);
                    	object.put("msg", usermsg);
                    	
                        channel.writeAndFlush(object.toString());
                    }
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /**
         * 명시적으로 퇴장시킬 수 있음.
         * 문자가 bkbk5515라면 ctx.close();
         */
//        if ("bkbk5515".equals(message.toLowerCase())) {
//            ctx.close();
//        }
    }
}
//출처: https://altongmon.tistory.com/503?category=799997 [IOS를 Java]