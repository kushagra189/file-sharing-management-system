import java.nio.file.Files;
import java.net.* ;
import java.nio.file.*;
import java.util.*;
import java.text.*;
import java.io.* ;
import java.awt.* ;

public class Client {
    public static DatagramSocket clientSocUDP;
    public static String ip="127.0.0.1";
    public static int port = 5001;
    public static void main(String args[]) {
        try {
            Socket clientSoc;
            DataOutputStream dout;
            DataInputStream din;
            String UserName;
            
            clientSocUDP = new DatagramSocket();
            clientSoc = new Socket(ip,5000) ;
            System.out.println("Connected to Server at localhost Port-5000(TCP)");
            
            din = new DataInputStream(clientSoc.getInputStream());
            dout = new DataOutputStream(clientSoc.getOutputStream());
            
            String a = "hello", inputLine=null;
            byte[] file_contents = new byte[1000];
            file_contents = a.getBytes();

            DatagramPacket initial = new DatagramPacket(file_contents,file_contents.length,InetAddress.getByName(ip),port);
            clientSocUDP.send(initial);

            //Send messages
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            
            for(;;) {
                try {
                    inputLine = bufferedReader.readLine();
                    dout.writeUTF(inputLine);
                    
                    StringTokenizer tokenedcommand = new StringTokenizer(inputLine);
                    String comm = tokenedcommand.nextToken();

                    if(comm.equals("create_user")) {
                        try {
                            UserName = tokenedcommand.nextToken();
                            new Thread(new RecievedMessagesHandler(din,UserName)).start();
                        }
                        catch(Exception e) {
                            dout.writeUTF("Error in create_user");
                        }
                    }

                    else if(comm.equals("upload")) {
                        try {

                            if(tokenedcommand.hasMoreTokens()) {
                                String filePath = tokenedcommand.nextToken();
                                
                                File file = new File(filePath);
                                FileInputStream fpin = new FileInputStream(file);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);
                                
                                long fileLength=file.length(), current=0;
                                
                                dout.writeUTF("LENGTH " + fileLength);
                                
                                while(true) {

                                    if(current == fileLength) {
                                        break;
                                    }

                                    int size = 1024;
                                    long temp =  fileLength - current;
                                    if(temp >= size) {
                                        current += size;
                                    }
                                    else {
                                        size = (int)(temp);
                                        current = fileLength;
                                    }

                                    try {
                                        file_contents = new byte[size];
                                        bpin.read(file_contents, 0, size);
                                        dout.write(file_contents);
                                    }
                                    catch(Exception e) { 
                                        dout.writeUTF("Error in upload while writing file_contents");
                                    }
                                    
                                }
                                System.out.println("File Sent");
                            }
                            else {
                                System.out.println("Give the path of the file that you want to upload");
                            }

                        }
                        catch(Exception e) {
                            dout.writeUTF("Error in upload");
                        }

                    }

                    else if(comm.equals("upload_udp")) {
                        try {

                            if(tokenedcommand.hasMoreTokens()) {

                                int size = 1024;
                                String filePath = tokenedcommand.nextToken();
                                File file = new File(filePath);
                                FileInputStream fpin = new FileInputStream(file);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);

                                long fileLength = file.length();
                                long current = 0;
                                dout.writeUTF("LENGTH "+fileLength);

                                while(true) {
                                    if(current == fileLength) {
                                        break;
                                    }
                                    long temp = fileLength - current;
                                    if(temp >= size) {
                                        current = current + size;
                                    }
                                    else {
                                        size = (int)(temp);
                                        current = fileLength;
                                    }
                                    try{
                                        file_contents = new byte[size];
                                        bpin.read(file_contents,0,size);
                                        DatagramPacket sendPacket = new DatagramPacket(file_contents,size,InetAddress.getByName(ip),port);
                                        clientSocUDP.send(sendPacket);
                                    }
                                    catch(Exception e) {
                                        dout.writeUTF("Error in upload_udp");
                                    }
                                }
                                System.out.println("File Sent");
                            }
                            else {
                                System.out.println("Give the path of the file that you want to upload");
                            }
                        }
                        catch(Exception e) {
                            dout.writeUTF("Error in upload_udp");
                        }
                    }
                }
                catch(Exception e){
                    System.out.println(e);
                    break;
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
            System.exit(0);
        }

    }

}


class RecievedMessagesHandler implements Runnable {

    private String UserName;
    private DataInputStream server;
    
    public RecievedMessagesHandler(DataInputStream server,String UserName) {
        
        this.UserName = UserName;
        this.server = server;
    
    }

    @Override
    public void run() {
        
        String inputLine = null;

        for(;;) {

            try {
                inputLine=server.readUTF();
                StringTokenizer st = new StringTokenizer(inputLine);
                String st_ch = st.nextToken();

                if(st_ch.equals("FILE")) {
                    
                    String fileName = st.nextToken();
                    String user = st.nextToken();
                    String fl_len = st.nextToken();
                    int fileLength = Integer.parseInt(fl_len), bytesRead = 0, size = 1000;

                    System.out.println("Recieving file "+fileName);
                    File newDir = new File(user);

                    if(newDir.exists() == false) {
                        newDir.mkdir();
                    }
                    else {
                        int po=1;
                    }
                    
                    byte[] file_contents = new byte[1000];
                    String output_path = user + '/' + fileName;
                    FileOutputStream fpout = new FileOutputStream(output_path);
                    BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                    DatagramPacket receivePacket;
                    size = Math.min(size, fileLength);

                    while((bytesRead=server.read(file_contents,0,size))!=-1) {
                        if(fileLength>0) {
                            break;
                        }
                        bpout.write(file_contents,0,size);
                        fileLength = fileLength - size;
                        size = Math.min(size, fileLength);
                    }

                    bpout.flush();
                    System.out.println("File Recieved");
                }
                else {
                    System.out.println(inputLine);
                }
            }
            catch(Exception e) {
                e.printStackTrace(System.out); 
                break;
            }
        }
    }
}