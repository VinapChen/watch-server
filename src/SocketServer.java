

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
            System.out.println("Server listening:" + port);
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
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));//读取客户端消息  
//                writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));//向客户端写消息  写str
//                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();    //写bytes
//                byte[] buf = new byte[64];

                String s = null;
                while((s = readLine_customize()) != null){
                    System.out.println("from socket: " + socket);
                    System.out.println("Received a message from the client:" + s);
//                    in.read(buf);
                    byte[] buf = s.getBytes();
                    byte[] buffer = ProtocolHandler.rmBrackets(buf);
                    byte[] resp_buf = ProtocolHandler.decode(buffer,socket);
                    if (!(resp_buf == null)) {
                        byte[] resp_buffer = ProtocolHandler.addBrackets(resp_buf);
                        out.write(resp_buffer);
                        out.flush();//清空缓存区的内容

                        System.out.println("Servers response:");
                        System.out.println(ProtocolHandler.bytesToAscii(resp_buffer,0,resp_buffer.length));
                    }  else {
                        System.out.println("Received an error message from the client, disconnected");
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

        private String readLine_customize() throws IOException {
            StringBuilder sb = new StringBuilder();
            int ch = 0;
            while ((ch = this.reader.read()) != -1) {//104,101,108,108,111
                if(ch == ']'){
                    sb.append((char)ch);
                    return sb.toString();
                }else{
                    sb.append((char)ch);
                }

//                //为了防止数据丢失,判断sb的长度不能大于0
//                if(sb.length() > 0){
//                    return sb.toString();
//                }
            }
            return null;
        }

    }

}