import java.net.*;
import java.io.*;

/**
 * Open an SMTP connection to a mailserver and send one mail.
 *
 */
public class SMTPConnection {
    /* The socket to the server */
    private final Socket connection;

    /* Streams for reading and writing the socket */
    private final BufferedReader fromServer;
    private final DataOutputStream toServer;

    /* The smtp port */
    private static final int SMTP_PORT = 2526;
    /* Used for newlines */
    private static final String CRLF = "\r\n";

    /* Are we connected? Used in close() to determine what to do. */
    private boolean isConnected = false;

    /* Create an SMTPConnection object. Create the socket and the 
       associated streams. Initialize SMTP connection. */
    public SMTPConnection() throws IOException {
        /* Connecting to the mailserver on the correct port */
        connection = new Socket("datacomm.bhsi.xyz", SMTP_PORT);
        /* Make a buffereader to recieve messages from the server */
        fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));;
        /* Make a dataoutpurstream to send messages to the server */
        toServer = new DataOutputStream(connection.getOutputStream());

	/* Read a line from server and check that the reply code is 220.
	   If not, throw an IOException. */
        String ServerMsg = fromServer.readLine();

        int rc = parseReply(ServerMsg);

        if (rc != 220) {
            throw new IOException("RC codes don't match");
        }

	/* SMTP handshake. We need the name of the local machine.
	   Send the appropriate SMTP handshake command. */
        String code = "HELO";

        sendCommand(code, 250);

        isConnected = true;
    }

    /* Send the message. Write the correct SMTP-commands in the
       correct order. No checking for errors, just throw them to the
       caller. */
    public void send(Envelope envelope) throws IOException {
	/* Send all the necessary commands to send a message. Call
	   sendCommand() to do the dirty work. Do _not_ catch the
	   exception thrown from sendCommand(). */

        /* The sender email command*/
        sendCommand("MAIL FROM " + envelope.Sender, 250);

        /* The receiver email command */
        sendCommand("RCPT TO: <" + envelope.Recipient + ">", 250);

        /* The data command that has the full email body */
        sendCommand("DATA" + CRLF + envelope.Message + CRLF + "." + CRLF + ".", 354);
    }

    /* Close the connection. First, terminate on SMTP level, then
       close the socket. */
    public void close() {
        isConnected = false;
        try {
            /* Sends a quit command to the server */
            sendCommand("QUIT", 250);
            /* Closes the connection */
            connection.close();
        } catch (IOException e) {
            System.out.println("Unable to close connection: " + e);
            isConnected = true;
        }
    }

    /* Send an SMTP command to the server. Check that the reply code is
       what it is supposed to be according to RFC 821. */
    private void sendCommand(String command, int rc) throws IOException {
        /* Write command to server and read reply from server. */

        /* Sends the command to the server with a newline */
        toServer.writeBytes(command + CRLF);

        /* Reads the line response */
        String reply = fromServer.readLine();
        int replyCode = parseReply(reply);

	/* Check that the server's reply code is the same as the parameter
	   rc. If not, throw an IOException. */

        if (replyCode != rc) {
            throw new IOException("RC codes don't match");
        }
    }

    /* Parse the reply line from the server. Returns the reply code. */
    private int parseReply(String reply) {
        return Integer.parseInt(reply.split(" ")[0]);
    }

    /* Destructor. Closes the connection if something bad happens. */
    protected void finalize() throws Throwable {
        if(isConnected) {
            close();
        }
        super.finalize();
    }
}