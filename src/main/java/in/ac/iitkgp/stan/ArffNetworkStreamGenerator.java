package in.ac.iitkgp.stan;

import java.io.*;
import java.net.Socket;

public class ArffNetworkStreamGenerator
{
    private String host;
    private int port;
    private String relationName;
    private String attributes[][];
    private char lineEnding;
    private boolean hasSentHeaders;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedReader br;

    public ArffNetworkStreamGenerator(String host, int port, String relationName, String attributes[][])
    {
        this.host = host;
        this.port = port;
        this.relationName = relationName;
        this.attributes = attributes;
        this.lineEnding = (char)10;     // LF character '\n'
        this.hasSentHeaders = false;
    }

    public ArffNetworkStreamGenerator(String host, int port, String relationName, String attributes[][], char lineEnding)
    {
        this.host = host;
        this.port = port;
        this.relationName = relationName;
        this.attributes = attributes;
        this.lineEnding = lineEnding;
        this.hasSentHeaders = false;
    }

    private void sendHeaders()
    {
        if (!hasSentHeaders)
        {
            StringBuilder sb = new StringBuilder();

            sb.append("@relation ").append(relationName).append(lineEnding);
            sb.append(lineEnding);

            for (int i=0; i<attributes.length; i++)
            {
                sb.append("@attribute ").append(attributes[i][0]).append(' ').append(attributes[i][1]).append(lineEnding);
            }
            sb.append(lineEnding);
            sb.append("@data").append(lineEnding);
            sb.append(lineEnding);

            try
            {
                out.writeBytes(sb.toString());
                hasSentHeaders = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public boolean connect(String dummyData[])
    {
        try
        {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            br = new BufferedReader(new InputStreamReader(in));
            sendHeaders();
            sendData(dummyData);
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect()
    {
        try
        {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void sendData(String data[])
    {
        // send the headers first if not sent
        sendHeaders();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < data.length - 1; i++)
        {
            sb.append(data[i]);
            sb.append(',');
        }
        sb.append(data[data.length-1]);
        sb.append(lineEnding);
        sb.append(lineEnding);

        try
        {
            out.writeBytes(sb.toString());
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public boolean receiveBoolean()
    {
        String s;
        try
        {
            s = br.readLine();
            if (s.equals("1"))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public int receiveInt()
    {
        try
        {
            return in.readInt();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    public InputStream getInputStream()
    {
        try
        {
            return socket.getInputStream();
        }
        catch (IOException | NullPointerException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public OutputStream getOutputStream()
    {
        try
        {
            return socket.getOutputStream();
        }
        catch (IOException | NullPointerException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
