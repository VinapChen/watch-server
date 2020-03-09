import java.io.File;

public class Main {

    static File log;
    public static void main(String[] args) {
        log=new File("./info.log");
        SocketServer server = new SocketServer();
        server.startAction();
    }
}
