

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//
//import com.starit.ipran.kafka.KafkaUtils;
//import com.starit.ipran.load.Constants;
//import com.starit.ipran.model.IpranAlarm;
//import com.starit.ipran.util.EncodeUtils;


public class SocketServer {

//    private final static Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);
    private static int port = 8899; //监听端口号


    public void startAction(){
        ServerSocket serverSocket=null;
        try {
            serverSocket=new ServerSocket(port); //端口号
            System.out.println("服务端服务启动监听：" + port);
            //通过死循环开启长连接，开启线程去处理消息
            while(true){
                Socket socket=serverSocket.accept();
                new Thread(new MyRuns(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket!=null) {
                    serverSocket.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    class MyRuns implements Runnable{
        Socket socket;
        BufferedReader reader;
        BufferedWriter writer;

        public MyRuns(Socket socket) {
            super();
            this.socket = socket;
        }

        public void run() {
            try {
//                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));//读取客户端消息  
//                writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));//向客户端写消息
//                String lineString="";
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                byte[] buffer = new byte[64];


                while(true){
                    in.read(buffer);
                    String asciiStr = ProtocolHandler.bytesToAscii(buffer,0,64);
//                    lineString=reader.readLine();
                    System.out.println("from socket: " + socket);
                    System.out.println("收到来自客户端的发送的消息：" + asciiStr);
                    byte[] resp_buf = ProtocolHandler.decode(buffer,socket);
//                    writer.flush();
//                    !(lineString=reader.readLine()).equals("bye")
                    if (!(resp_buf == null)) {
                        byte[] resp_buffer = ProtocolHandler.addBrackets(resp_buf);
                        out.write(resp_buffer);
                        System.out.println("服务器返回：" + ProtocolHandler.bytesToAscii(resp_buffer,0,resp_buffer.length));
                    }  else {
                        System.out.println("收到来自客户端的发送的错误消息，断开连接");
                        break;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader!=null) {
                        reader.close();
                    }
                    if (writer!=null) {
                        writer.close();
                    }
                    if (socket!=null) {
                        socket.close();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

}