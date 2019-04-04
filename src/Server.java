import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        ServerSocket server = new ServerSocket(8080);

        System.out.println("Server has started on 127.0.0.1:80.\r\nWaiting for a connection...");

        Socket client = server.accept();

        System.out.println("A client connected.");

        InputStream in = client.getInputStream();

        OutputStream out = client.getOutputStream();

        //translate bytes of request to string
        String data = new Scanner(in,"UTF-8").useDelimiter("\\r\\n\\r\\n").next();

        Matcher get = Pattern.compile("^GET").matcher(data);

        System.out.println(data);

        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();
            byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + DatatypeConverter
                    .printBase64Binary(
                            MessageDigest
                                    .getInstance("SHA-1")
                                    .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                            .getBytes("UTF-8")))
                    + "\r\n\r\n")
                    .getBytes("UTF-8");
            for(int i=0;i<response.length;i++){
                System.out.print((char)response[i]);
            }
            out.write(response, 0, response.length);
        } else {

        }
    }
}