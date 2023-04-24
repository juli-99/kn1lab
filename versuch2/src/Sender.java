import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * Die "Klasse" Sender liest einen String von der Konsole und zerlegt ihn in einzelne Worte. Jedes Wort wird in ein
 * einzelnes {@link Packet} verpackt und an das Medium verschickt. Erst nach dem Erhalt eines entsprechenden
 * ACKs wird das nächste {@link Packet} verschickt. Erhält der Sender nach einem Timeout von einer Sekunde kein ACK,
 * überträgt er das {@link Packet} erneut.
 */
public class Sender {
    /**
     * Hauptmethode, erzeugt Instanz des {@link Sender} und führt {@link #send()} aus.
     * @param args Argumente, werden nicht verwendet.
     */
    public static void main(String[] args) {
        Sender sender = new Sender();
        try {
            sender.send();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Erzeugt neuen Socket. Liest Text von Konsole ein und zerlegt diesen. Packt einzelne Worte in {@link Packet}
     * und schickt diese an Medium. Nutzt {@link SocketTimeoutException}, um eine Sekunde auf ACK zu
     * warten und das {@link Packet} ggf. nochmals zu versenden.
     * @throws IOException Wird geworfen falls Sockets nicht erzeugt werden können.
     */
    private void send() throws IOException {
    	//Text einlesen und in Worte zerlegen
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String in = br.readLine();
        in += " EOT";
        String[] words = in.split(" ");
        // Socket erzeugen auf Port 9998 und Timeout auf eine Sekunde setzen
        DatagramSocket clientSocket = new DatagramSocket(9998);
        clientSocket.setSoTimeout(1000);
        // Iteration über den Konsolentext
        int index = 0;
        int seq = 1;
        int ackNum = 1;
        while (index < words.length) {
            String w = words[index];
        	// Paket an Port 9997 senden
            Packet packetOut = new Packet(seq, ackNum, true, w.getBytes());
            int tmp = seq + w.getBytes().length;

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(packetOut);
            byte[] buf = b.toByteArray();

            DatagramPacket sndPacket = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), 9997);
            clientSocket.send(sndPacket);

            try {
                // Auf ACK warten und erst dann Schleifenzähler inkrementieren
                byte[] buf_rcv = new byte[256];
                DatagramPacket rcvPacketRaw = new DatagramPacket(buf_rcv, buf_rcv.length);
                clientSocket.receive(rcvPacketRaw);

                ObjectInputStream i = new ObjectInputStream(new ByteArrayInputStream(rcvPacketRaw.getData()));
                Packet packetIn = (Packet) i.readObject();

                if (packetIn.getAckNum() == tmp && packetIn.isAckFlag()) {
                    seq = tmp;
                    ackNum += packetIn.getPayload().length;
                    index++;
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
            	System.out.println("Receive timed out, retrying...");
            }
        }
        
        // Wenn alle Packete versendet und von der Gegenseite bestätigt sind, Programm beenden
        clientSocket.close();
        
        if(System.getProperty("os.name").equals("Linux")) {
            clientSocket.disconnect();
        }

        System.exit(0);
    }
}
