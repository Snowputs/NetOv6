import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;




public class SSLServer {

    static int SOCKET = 1337;
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(SOCKET);

            System.out.println("Server has started on localhost:" + SOCKET + ".\r\nWaiting for a connection...");

            Socket client = server.accept();

            System.out.println("A client connected.");

            InputStream in = client.getInputStream();

            OutputStream out = client.getOutputStream();

            String data = new Scanner(in,"UTF-8").useDelimiter("\\r\\n\\r\\n").next();

            Matcher get = Pattern.compile("^GET").matcher(data);

            if (get.find()) {
                Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                match.find();
                byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + DatatypeConverter.printBase64Binary(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))+ "\r\n\r\n").getBytes("UTF-8");

                out.write(response, 0, response.length);
            } else {

            }

            //Hello, World!
            byte[] send = { (byte) 0x81, 0x0d, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x2c, 0x20, 0x57, 0x6f, 0x72, 0x6c, 0x64, 0x21 };
            out.write(send, 0, send.length);




            in.close();
            out.close();
            client.close();
            server.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}