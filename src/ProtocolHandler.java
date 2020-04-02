import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class ProtocolHandler {

    private static Map<String ,Socket> deviceSocketMap = new HashMap<String,Socket>(); // 保存连接对象
    private static Map<Socket ,Boolean> deviceConnectedMap = new HashMap<Socket, Boolean>(); // 保存连接对象


    public static byte[] decode(byte[] buffer, Socket socket){

//        byte[] buffer = rmBrackets(buf);
        if (buffer == null) return null;
        byte[] cmd = new byte[2];
        System.arraycopy(buffer, 0, cmd, 0, 2);


        byte MT[] = {0x4d, 0x54};
        byte CS[] = {0x43, 0x53};

        if (Arrays.equals(cmd, MT)) {
            return setIP(buffer);
        } else if (Arrays.equals(cmd, CS)) {
            try {
                boolean connected = deviceConnectedMap.get(socket);

                if (connected)
                    return null;  //已经建立加密通道连接
                else
                    return insParseUnconnected(buffer, socket);     //尚未建立加密通道连接
            }
            catch (Exception e) {
                return insParseUnconnected(buffer, socket);     //尚未建立加密通道连接
            }
        }
        return null;
    }

    public static byte[] rmBrackets (byte[] buf) {
        // rm []
        if (buf[0] == 0x5b) {
            int i = 1;
            while (buf[i++] != 0x5d) ;
            byte[] buffer = new byte[i - 2];
            System.arraycopy(buf,1,buffer,0,buffer.length);
            return buffer;
        }
        return null;
    }

    public static byte[] addBrackets (byte[] buf) {
        // add []
        byte[] buffer = new byte[buf.length+2];
        buffer[0] = 0x5b;
        buffer[buffer.length - 1] = 0x5d;
        System.arraycopy(buf,0,buffer,1,buf.length);
        return buffer;
    }

    private static byte[] setIP (byte[] buffer) {
        String device_id;
        int package_len = 0;

        if (buffer[2] == 0x2a) // *
        {
            byte[] device_id_buffer =  new byte[10];
            System.arraycopy(buffer, 3, device_id_buffer, 0, 10);
            device_id = bytesToAscii(device_id_buffer,0,10);
            System.out.println("device id: " + device_id);
        } else return null;

        if (buffer[13] == 0x2a) {
            byte[] len_byte =  new byte[2];
            System.arraycopy(buffer, 14, len_byte, 0, 2);
            package_len = (buffer[14] << 8) + buffer[15];
            System.out.println("package length: " + package_len);
        } else return null;
        if (buffer[16] == 0x2a) {
            byte[] ip_mark =  new byte[2];
            System.arraycopy(buffer, 17, ip_mark, 0, 2);
            byte IP_mark[] = {0x49, 0x50};
            if (Arrays.equals(ip_mark, IP_mark)){
                byte[] ip =  new byte[12];
                System.arraycopy(buffer, 20, ip, 0, 12);
                String ip_str = bytesToAscii(ip,0,12);
                System.out.println("new ip address: " + ip_str  );

            }

            byte[] resp_buffer = new byte[19];
            System.arraycopy(buffer, 0, resp_buffer, 0, 14);
            resp_buffer[14] = 0;
            resp_buffer[15] = 2;
            resp_buffer[16] = 0x2a;
            System.arraycopy(buffer, 17, resp_buffer, 17, 2);
            String resp_str = bytesToAscii(resp_buffer,0,19);
            System.out.println("resp str: " + resp_str);
            return resp_buffer;

        } else return null;
    }

    private static byte[] insParseUnconnected (byte[] buffer, Socket socket) {
        String device_id;
        int package_len = 0;

        if (buffer[2] == 0x2a) // *
        {
            int bike_id_index;
            for (bike_id_index = 3; bike_id_index <= buffer.length; bike_id_index++){
                 if (buffer[bike_id_index] == 0x2a){
                     break;
                 }
            }
            if (bike_id_index >= buffer.length) return null;
            byte[] device_id_buffer =  new byte[bike_id_index-3];
            System.arraycopy(buffer, 3, device_id_buffer, 0, bike_id_index-3);
            device_id = bytesToAscii(device_id_buffer,0,device_id_buffer.length);
            System.out.println("device id: " + device_id);
            deviceSocketMap.put(device_id,socket);
//            System.out.println("socket: " + deviceSocketMap.get(device_id));

            byte[] len_byte =  new byte[4];
            System.arraycopy(buffer, bike_id_index+1, len_byte, 0, 4);
            package_len = Integer.parseInt(bytesToAscii(len_byte,0,len_byte.length),16);
            System.out.println("package length: " + package_len);

            byte REG[] = {0x52, 0x45, 0x47};
            byte CON[] = {0x43, 0x4f, 0x4E};
            if (buffer[bike_id_index+5] == 0x2a) {
                byte[] ins_mark = new byte[package_len];
                System.arraycopy(buffer,bike_id_index+6,ins_mark,0,package_len);
                if (Arrays.equals(ins_mark,REG)){
                    // 注册
                    return reg(buffer,bike_id_index);
                } else if (Arrays.equals(ins_mark,CON)){
                    // 建立连接
                    return connect(buffer,socket);
                }
            }

        } else return null;

        return null;
    }

    private static byte[] reg(byte[] buffer, int bike_id_index) {
        int fileBufLength = 0;
                String fileName = "/etc/pki/CA/client.p12";   // abj-gateway-1
//        String fileName = "/Users/yunba/Downloads/client.p12";   //localhost
        try {
            byte[] fileBuf = fileToBytes(fileName);
            fileBufLength = fileBuf.length;
            System.out.println("file length:" + fileBufLength);

            byte[] resp_buffer = new byte[10+bike_id_index+fileBufLength];
            System.arraycopy(buffer, 0, resp_buffer, 0, bike_id_index+9);
//            CS*0102030405*LEN*REG
            int resp_buffer_len = 4+fileBufLength;
            String resp_len_str =  Integer.toHexString(resp_buffer_len);
            while (resp_len_str.length() < 4) {
                resp_len_str = "0" + resp_len_str;
            }
//            String resp_len_str = String.format("%04d", resp_buffer_len);
            byte[] byteArray = resp_len_str.getBytes();
            System.arraycopy(byteArray, 0, resp_buffer, bike_id_index+1, 4);
//            change LEN
            resp_buffer[bike_id_index+9] = 0x2c;   //,
//            CS*0102030405*LEN*REG,

            System.arraycopy(fileBuf,0,resp_buffer,bike_id_index+10,fileBufLength);

            for (int i = 0; i<resp_buffer.length; i++){
                if (resp_buffer[i] == 0x5d){
                    resp_buffer[i] = 0x5a;
                }
            }
            String resp_str = bytesToAscii(resp_buffer,0,resp_buffer.length);
            System.out.println("resp str:\n=========================");
            System.out.println(resp_str);
            System.out.println("=========================");

            return resp_buffer;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] connect(byte[] buffer, Socket socket) {
        deviceConnectedMap.put(socket,true);
        System.out.println("建立加密通道连接");
        byte[] resp_buffer = new byte[20];
        System.arraycopy(buffer,0,resp_buffer,0,20);
//      [CS*YYYYYYYYYY*LEN*CON]
        return resp_buffer;
    }


// tools function

    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytesToHexFun(byte[] bytes) {
        // 一个byte为8位，可用两个十六进制位标识
        char[] buf = new char[bytes.length * 2];
        int a = 0;
        int index = 0;
        for(byte b : bytes) { // 使用除与取余进行转换
            if(b < 0) {
                a = 256 + b;
            } else {
                a = b;
            }

            buf[index++] = HEX_CHAR[a / 16];
            buf[index++] = HEX_CHAR[a % 16];
        }

        return new String(buf);
    }
    public static String bytesToAscii(byte[] bytes, int offset, int dateLen) {
        if ((bytes == null) || (bytes.length == 0) || (offset < 0) || (dateLen <= 0)) {
            return null;
        }
        if ((offset >= bytes.length) || (bytes.length - offset < dateLen)) {
            return null;
        }

        String asciiStr = null;
        byte[] data = new byte[dateLen];
        System.arraycopy(bytes, offset, data, 0, dateLen);
        try {
            asciiStr = new String(data, "ISO8859-1");
        } catch (UnsupportedEncodingException e) {
        }
        return asciiStr;
    }

    public static byte[] fileToBytes(String filename) throws IOException {

        FileChannel fc = null;
        try {
            fc = new RandomAccessFile(filename, "r").getChannel();
            MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0,
                    fc.size()).load();
            System.out.println(byteBuffer.isLoaded());
            byte[] result = new byte[(int) fc.size()];
            if (byteBuffer.remaining() > 0) {
                // System.out.println("remain");
                byteBuffer.get(result, 0, byteBuffer.remaining());
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                fc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
