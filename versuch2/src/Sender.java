import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.Scanner;
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
        Scanner scan = new Scanner(System.in);
        String str = scan.nextLine();
        str += " EOT";
        String[] str_arr = str.split(" ");

        // Socket erzeugen auf Port 9998 und Timeout auf eine Sekunde setzen
        DatagramSocket clientSocket = new DatagramSocket(9998);
        clientSocket.setSoTimeout(1000);

        // Iteration über den Konsolentext
        int c = 1;
        for (String s : str_arr) {
            // create new packet
            Packet packetOut = new Packet(c, 1, true, s.getBytes());

            // serialize Packet for sending
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(packetOut);
            byte[] send_buf = b.toByteArray();

            DatagramPacket dataPacket = new DatagramPacket(send_buf, send_buf.length, InetAddress.getLocalHost(), 9997);

            int ackNum = -1;
            c += send_buf.length;
            // Paket an Port 9997 senden
            do {
                clientSocket.send(dataPacket);
                try {
                    // Auf ACK warten und erst dann Schleifenzähler inkrementieren
                    byte[] res_buf = new byte[256];
                    DatagramPacket rcvPacketRaw = new DatagramPacket(res_buf, res_buf.length);
                    clientSocket.receive(rcvPacketRaw);
                    // deserialize Packet
                    ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(rcvPacketRaw.getData()));
                    Packet packetIn = (Packet) is.readObject();
                    ackNum = packetIn.getAckNum();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    System.out.println("Receive timed out, retrying...");
                }
            } while (ackNum < c);
        }

        // Wenn alle Packete versendet und von der Gegenseite bestätigt sind, Programm beenden
        clientSocket.close();

        if(System.getProperty("os.name").equals("Linux")) {
            clientSocket.disconnect();
        }
        System.exit(0);
    }
}
