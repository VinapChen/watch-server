import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class ProtocolHandler {

    public static byte[] decode(byte[] buffer){
        byte err[] = null;
        byte[] cmd = new byte[2];
        System.arraycopy(buffer, 0, cmd, 0, 2);


        byte MT[] = {0x4d, 0x54};
        byte CS[] = {0x43, 0x53};

        if (Arrays.equals(cmd, MT)) {
            return setIP(buffer);
        } else if (Arrays.equals(cmd, CS)) {
            return reg(buffer);
        }
        return err;
    }

    private static byte[] setIP (byte[] buffer) {
        byte err[] = null;
        String device_id;
        int package_len = 0;

        if (buffer[2] == 0x2a) // *
        {
            byte[] device_id_buffer =  new byte[10];
            System.arraycopy(buffer, 3, device_id_buffer, 0, 10);
            device_id = bytesToAscii(device_id_buffer,0,10);
            System.out.println("device id: " + device_id);
        } else return err;

        if (buffer[13] == 0x2a) {
            byte[] len_byte =  new byte[2];
            System.arraycopy(buffer, 14, len_byte, 0, 2);
            package_len = (buffer[14] << 8) + buffer[15];
            System.out.println("package length: " + package_len);
        } else return err;
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

        } else return err;
    }

    private static byte[] reg (byte[] buffer) {
        byte err[] = null;
        String device_id;
        int package_len = 0;

        if (buffer[2] == 0x2a) // *
        {
            byte[] device_id_buffer =  new byte[10];
            System.arraycopy(buffer, 3, device_id_buffer, 0, 10);
            device_id = bytesToAscii(device_id_buffer,0,10);
            System.out.println("device id: " + device_id);
        } else return err;

        if (buffer[13] == 0x2a) {
            byte[] len_byte =  new byte[2];
            System.arraycopy(buffer, 14, len_byte, 0, 2);
            package_len = (buffer[14] << 8) + buffer[15];
            System.out.println("package length: " + package_len);
        } else return err;

        byte REG[] = {0x52, 0x45, 0x47};
        if (buffer[16] == 0x2a) {
            byte[] reg_mark = new byte[package_len-1];
            System.arraycopy(buffer,17,reg_mark,0,package_len-1);
            if (Arrays.equals(reg_mark,REG)){
                int fileBufLength = 0;
                String fileName = "/Users/yunba/Downloads/client.p12";
                try {
                    byte[] fileBuf = fileToBytes(fileName);
                    fileBufLength = fileBuf.length;

                    byte[] resp_buffer = new byte[21+fileBufLength];
                    System.arraycopy(buffer, 0, resp_buffer, 0, 14);
                    int resp_buffer_len = 5+fileBufLength;
                    resp_buffer[14] = (byte) (resp_buffer_len >> 8);
                    resp_buffer[15] = (byte) (resp_buffer_len % 256);
                    resp_buffer[16] = 0x2a;
                    System.arraycopy(buffer, 17, resp_buffer, 17, 3);
                    resp_buffer[20] = 0x2c;   //,

                    System.arraycopy(fileBuf,0,resp_buffer,21,fileBufLength);

                    String resp_str = bytesToAscii(resp_buffer,0,21);
                    System.out.println("resp str: " + resp_str);




                    return resp_buffer;

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
        return err;
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
