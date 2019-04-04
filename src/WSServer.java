
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class WSServer {

    private int _mPort = 8888;

    public WSServer(int port) {
        _mPort = port;
    }

    public void startServer() throws Exception {
        try {
            ServerSocket serverSocket = new ServerSocket(_mPort);

            while (true) {
                Socket server = serverSocket.accept();

                WSClientThread ws = new WSClientThread(server);

                Thread thr = new Thread(ws);
                thr.start();

            }
        } catch (IOException ex) {
            throw new Exception(ex);
        }
    }

    public static void main(String[] args) {
        WSServer ws = new WSServer(8888);
        try {
            ws.startServer();
        } catch (Exception ex) {
            System.err.println("Something went wrong.");
        }

    }

    private class WSClientThread implements Runnable {

        private InputStream _mIs = null;
        private OutputStream _mOs = null;
        private Socket _mSocket = null;

        public WSClientThread(Socket socket) throws IOException {

            //Prepare input and output streams for reading.
            _mIs = socket.getInputStream();
            _mOs = socket.getOutputStream();
            _mSocket = socket;
        }

        @Override
        public void run() {

            //Start reading.
            try {

                //Parse the input an find the websocket key from client
                String reqKey = null;
                String line = readLine();
                while (line != null && line.length() >0 ) {
                    System.out.println(line);
                    if (line.startsWith("Sec-WebSocket-Key:")) {
                        int index = line.indexOf(":");
                        reqKey = line.substring(index + 2);

                    }
                    line = readLine();
                }

                String akey = getAcceptKey(reqKey);

                //Send handshake response to client
                _mOs.write("HTTP/1.1 101 Web Socket Protocol Handshake\r\n".getBytes());
                _mOs.write("Upgrade: websocket\r\n".getBytes());
                _mOs.write("Connection: Upgrade\r\n".getBytes());
                _mOs.write(("Sec-WebSocket-Accept: " + akey + "\r\n\r\n").getBytes());

                //Start reading message.
                while(true) {
                    startTxn();

                }

            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

        }


        private void handleError() throws IOException {
            _mSocket.close();
        }


        private void startTxn() throws IOException{
            //Read first byte.
            int b = _mIs.read();

            //This example suports only single frames. Seqence is not supported.
            boolean finalFrame = (b & 0x80) == 0x80;
            if(!finalFrame) {
                handleError();
            }

            //Read opcode. We support only text messages.
            int opcode = b &0x0F;
            if(opcode != 1) {
                handleError();
            }

            //Second byte
            b = _mIs.read();
            boolean mask = (b & 0x80) == 0x80;

            //We are expecting message from client. So, mask should be 1.
            if(!mask) {
                handleError();
            }

            //Payload length. Lets consider only 7 bits or 16 bits.
            int len = b & 0x7F;
            if(len  == 127) {
                handleError();
            }
            else if(len == 126) {
                int b1 = _mIs.read();
                int b2 = _mIs.read();

                len = b1 << 8;
                len |= b2;
            }

            //Read mask. 4 bytes
            byte maskKey[] = new byte[4];
            for(int i=0;i<4;i++) {
                maskKey[i] = (byte)_mIs.read();
            }

            byte buff[] = new byte[len];
            int readLen = _mIs.read(buff);
            if(readLen < len) {
                //We could not read enough data.
                handleError();
            }

            //Apply the mask
            for(int i=0;i<len;i++) {
                buff[i] = (byte)(buff[i] ^ maskKey[i % 4]);
            }

            String message = new String(buff);

            System.out.println("Message : \n"+message);
            sendMessage("We got \""+message+"\"");

        }

        private void sendMessage(String message) throws IOException {

            byte messageBytes[] = message.getBytes();

            //We need to set only FIN and Opcode.
            _mOs.write(0x81);

            //Prepare the payload length.
            if(messageBytes.length <= 125) {
                _mOs.write(messageBytes.length);
            }

            else { //We assume it is 16 but length. Not more than that.
                _mOs.write(0x7E);
                int b1 =( messageBytes.length >> 8) &0xff;
                int b2 = messageBytes.length &0xff;
                _mOs.write(b1);
                _mOs.write(b2);
            }

            //Write the data.
            _mOs.write(messageBytes);

        }

        private String getAcceptKey(String clientKey) {
            String k = clientKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                System.err.println(e.getMessage());
                return null;
            }
            byte[] o = md.digest(k.getBytes());
            k = Base64.getEncoder().encodeToString(o);

            return k;
        }

        private String readLine() throws IOException {
            byte buff[] = new byte[1024];

            int b = _mIs.read();
            if (b == -1) {
                return null;
            }
            int count = 0;
            while (b != '\r' && b != -1) {
                buff[count] = (byte) b;
                count++;
                b = _mIs.read();
            }

            _mIs.read(); //Skip \n

            return new String(buff, 0, count);

        }

    }

}
