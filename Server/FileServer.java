import java.util.*;
import java.net.* ;
import java.nio.file.*;
import java.awt.* ;
import java.io.* ;
import java.text.*;
import java.nio.file.Files;
import java.util.* ;

public class FileServer {
    public static Vector<Socket> ClientSockets;
    public static Vector<String> UserNames;
    public static Vector<Chatroom> Chatrooms;
    public static Map<String,Chatroom> ConnectedChatroom;
    public static int max_no_clients;
    public static Vector<Integer> Ports;
    public static DatagramSocket SocUDP;
    public static Map<String, String> UserIp;

    FileServer(int max_no_clients_) {
        try {
            System.out.println("Server running on localhost Port-5000(TCP), 5001(UDP)");
            
            ServerSocket Soc = new ServerSocket(5000) ;
            DatagramSocket SocUDP = new DatagramSocket(5001);
            
            ClientSockets = new Vector<Socket>();
            UserNames = new Vector<String>();
            Chatrooms = new Vector<Chatroom>();
            ConnectedChatroom = new HashMap<String,Chatroom>();
            max_no_clients=max_no_clients_;
            Ports = new Vector<Integer>();
            UserIp = new HashMap<String, String>();
            
            while(true)
            {
                Socket CSoc = Soc.accept();
                AcceptClient client_ = new AcceptClient(CSoc,SocUDP) ;
            }
        }
        catch(Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    public static void main(String args[]) throws Exception {

        if(args.length==0)
        {
            System.out.println("Maximum number of Users for the Server not given."); System.exit(0);
        }

        FileServer server = new FileServer(Integer.parseInt(args[0])) ;

    }
}

class AcceptClient extends Thread {
    
    Socket ClientSocket;
    DataInputStream din;
    DataOutputStream dout;
    String UserName;
    DatagramSocket SocUDP;
    
    AcceptClient (Socket CSoc, DatagramSocket SocUDP_) throws Exception {

        ClientSocket = CSoc;
        din = new DataInputStream(ClientSocket.getInputStream());
        dout = new DataOutputStream(ClientSocket.getOutputStream()) ;
        
        SocUDP = SocUDP_;

        byte[] intial = new byte[1000];
        DatagramPacket recieve_inital = new DatagramPacket(intial, intial.length);
        SocUDP.receive(recieve_inital);

        int port = recieve_inital.getPort();
        FileServer.Ports.add(port);

        start() ;
    }

    public void run() {
        for(;;)
        {
            try {
                String commandfromClient = new String() ;

                commandfromClient = din.readUTF();
                
                StringTokenizer tokenedcommand = new StringTokenizer(commandfromClient);
                String command = tokenedcommand.nextToken();

                System.out.println(command);

                if(command.equals("create_user")) {
                    try {
                        UserName = tokenedcommand.nextToken();
                        int max_clients = FileServer.max_no_clients;
                        if(FileServer.UserNames.size()==max_clients) {
                            System.out.println("Cannot create user : Max limit reached");
                            dout.writeUTF("Cannot connect: Server capacity maxed out");
                            try{
                                ClientSocket.close();
                                din.close(); 
                                dout.close(); 
                            }
                            catch(Exception e) {
                                dout.writeUTF("Error in closing connection");
                            }
                            return;
                        }

                        System.out.println("User "+ UserName +" created");

                        FileServer.UserNames.add(UserName);
                        FileServer.ClientSockets.add(ClientSocket);
                        FileServer.ConnectedChatroom.put(UserName,null);

                        String userIp = ClientSocket.getInetAddress().toString();
                        userIp = userIp.substring(1, userIp.length());

                        FileServer.UserIp.put(UserName, userIp);

                        // String folderName = UserName;
                        File newDir = new File(UserName);
                        if(newDir.exists() == true) {
                            // System.out.println("Directory Already Exists!");
                            int po=1;
                        }
                        else {
                            newDir.mkdir();
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in create_user");
                    }
                }

                else if(command.equals("create_folder")) {
                    try {
                        // String folderName = tokenedcommand.nextToken();
                        File newDir = new File(UserName+"/"+tokenedcommand.nextToken());
                        if(newDir.exists()==false) {
                            newDir.mkdir();
                        }
                        else {
                            System.out.println("Directory Exists");
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error while creating folder");
                    }
                }

                else if(command.equals("move_file")) {
                    try {
                        String sourcePath = tokenedcommand.nextToken();
                        String destPath = tokenedcommand.nextToken(), filename = sourcePath;
                        int index = sourcePath.indexOf('/'); 
                        if(index != -1) {
                            String[] names = sourcePath.split("/");
                            filename = names[names.length - 1];
                        }

                        try {
                            String dest = destPath + '/' + filename;
                            Files.move(Paths.get(sourcePath), Paths.get(dest));
                        }
                        catch(IOException i) {
                            System.out.println(i);
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in moving files");
                    }
                }

                else if(command.equals("upload")) {
                    try {
                        String fl = tokenedcommand.nextToken();
                        String st_ = din.readUTF();
                        StringTokenizer stt = new StringTokenizer(st_), fileName = new StringTokenizer(fl,"/");
                        stt.nextToken();
                        String stt_token = stt.nextToken();
                        int fileLength = Integer.parseInt(stt_token), bytesRead=0, size=1000;

                        while(fileName.hasMoreTokens()) {
                            fl = fileName.nextToken();
                        }
                        String nameOfFile = fl;
                        
                        fl = UserName + '/' + nameOfFile;

                        byte[] file_contents = new byte[1000];
                        
                        FileOutputStream fpout = new FileOutputStream(fl);
                        BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                        size = Math.min(size,fileLength);
                        while((bytesRead=din.read(file_contents,0,size))!=-1)
                        {
                            if(fileLength<=0)
                            {
                                break;
                            }
                            bpout.write(file_contents,0,size);
                            fileLength = fileLength - size; 
                            size = Math.min(size,fileLength);
                        }
                        bpout.flush();
                        System.out.println("File Recieved");
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in upload");
                    }
                }

                else if(command.equals("upload_udp")) {
                    try {
                        String fl = tokenedcommand.nextToken(), st_ = din.readUTF();
                        StringTokenizer stt = new StringTokenizer(st_), fileName = new StringTokenizer(fl,"/");
                        stt.nextToken();
                        String stt_token = stt.nextToken();
                        int fileLength = Integer.parseInt(stt_token), size = 1024;
                        byte[] file_contents = new byte[1024];
                        
                        while(fileName.hasMoreTokens()) {
                            fl = fileName.nextToken();
                        }
                        
                        String nameOfFile = fl;
                        fl = UserName + '/' + nameOfFile;
                        size = Math.min(size, fileLength);
                        DatagramPacket packetUDP;
                        FileOutputStream fos1 = new FileOutputStream(fl), fos2 = new FileOutputStream(nameOfFile);

                        while(true)
                        {
                            if(fileLength<=0){
                                break;
                            }
                            packetUDP = new DatagramPacket(file_contents,size);
                            SocUDP.receive(packetUDP);
                            packetUDP = new DatagramPacket(file_contents,size,InetAddress.getByName(FileServer.UserIp.get(UserName)),5001);

                            fos2.write(packetUDP.getData(), 0, size);
                            fos1.write(packetUDP.getData(), 0, size);

                            SocUDP.send(packetUDP);

                            fileLength = fileLength - size;
                            size = Math.min(size, fileLength);
                        }
                        System.out.println("File Recieved");
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in file upload_udp");
                    }
                }

                else if(command.equals("create_group")) {
                    try {
                        String groupName = tokenedcommand.nextToken();
                        int existFlag=0, chatroom_arr_size=FileServer.Chatrooms.size(), i=0;

                        while(i<chatroom_arr_size) {
                            String grpName = FileServer.Chatrooms.elementAt(i).name;
                            if(grpName.equals(groupName)) {
                                existFlag = 1;
                                break;
                            }
                            i++;
                        }
                        
                        if(existFlag == 1) {
                            String output = "Group "+ groupName + " already exists";
                            dout.writeUTF(output);
                        }
                        else {
                            Chatroom chatR = new Chatroom(groupName, UserName);
                            FileServer.Chatrooms.add(chatR);
                            String output = "Group " + groupName + " created\nYou are in group " + groupName;
                            dout.writeUTF(output);
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in create_groups");
                    }
                }

                else if(command.equals("list_groups")) {
                    try {
                        String outp="";
                        int chatroom_arr_size = FileServer.Chatrooms.size(); 
                        if(chatroom_arr_size == 0) {
                            dout.writeUTF("No Groups exist");
                        }
                        else {
                            int i = 0;
                            while(i<chatroom_arr_size) {
                                outp = outp + FileServer.Chatrooms.elementAt(i).name + "\n";
                                i++;
                            }
                            dout.writeUTF(outp);
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in list_groups");
                    }
                }

                else if(command.equals("join_group")) {
                    try {
                        String groupName = tokenedcommand.nextToken();
                        int i=0, chatroom_arr_size = FileServer.Chatrooms.size();
                        if(FileServer.ConnectedChatroom.get(UserName) == null) {
                            while(i<chatroom_arr_size) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    FileServer.Chatrooms.elementAt(i).Members.add(UserName);
                                    String notif = UserName + " joined the group";
                                    FileServer.Chatrooms.elementAt(i).Notify(notif, UserName);
                                    FileServer.ConnectedChatroom.put(UserName, FileServer.Chatrooms.elementAt(i));
                                    String output = UserName + " joined the group " + groupName;
                                    dout.writeUTF(output);
                                    break;
                                }
                                i++;
                            }
                        }

                        else if(FileServer.ConnectedChatroom.get(UserName).name.equals(groupName)) {
                            String output = "You are already part of group " + FileServer.ConnectedChatroom.get(UserName).name;
                            dout.writeUTF(output);
                        }

                        else {
                            while(i<chatroom_arr_size) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    if(FileServer.Chatrooms.elementAt(i).Members.contains(UserName)) {
                                        String output = "You are already part of group " + groupName;
                                        dout.writeUTF(output);
                                    }
                                    else {
                                        FileServer.Chatrooms.elementAt(i).Members.add(UserName);
                                        String notif = UserName + " joined the group " + groupName;
                                        FileServer.Chatrooms.elementAt(i).Notify(notif, UserName);
                                        FileServer.ConnectedChatroom.put(UserName, FileServer.Chatrooms.elementAt(i));
                                        String output = "You joined the group " + groupName; 
                                        dout.writeUTF(output);
                                    }
                                    break;
                                }
                                i++;
                            }

                            if(i == chatroom_arr_size) {
                                String output = groupName + " doesn't exist"; 
                                dout.writeUTF(output);
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in join_group");
                    }
                }

                else if(command.equals("leave_group")) {
                    try {
                        String groupName = tokenedcommand.nextToken();
                        
                        if(FileServer.ConnectedChatroom.get(UserName) == null) {
                            dout.writeUTF("You are not part of any group");
                        }
                        else {
                            Chatroom c = null;
                            int i = 0, chatroom_arr_size = FileServer.Chatrooms.size();
                            while(i<chatroom_arr_size) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    c = FileServer.Chatrooms.elementAt(i);
                                    break;
                                }
                                i++;
                            }

                            if(i == chatroom_arr_size) {
                                String output = groupName + " doesn't exist";
                                dout.writeUTF(output);
                            }
                            else {
                                String name_ = c.name, outp = FileServer.Chatrooms.elementAt(i).Leave(UserName);
                                String notif = UserName + " left the group " + groupName;
                                c.Notify(notif, UserName);
                                if(outp.equals("DEL"))
                                {
                                    FileServer.Chatrooms.remove(c);
                                    c = null;
                                    String output = UserName + " left Chatroom "+name_+'\n'+name_+" deleted";
                                    dout.writeUTF(output);
                                }
                                else {
                                    dout.writeUTF(outp);
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in leave_group");
                    }
                }

                else if(command.equals("list_detail")) {
                    try {
                        String groupName = tokenedcommand.nextToken(); 
                        int i = 0, chatroom_arr_size = FileServer.Chatrooms.size();

                        while(i<FileServer.Chatrooms.size()) {
                            if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                break;
                            }
                            i++;
                        }

                        if(i == chatroom_arr_size) {
                            String output = groupName + " doesn't exist";
                            dout.writeUTF(output);
                        }
                        else {
                            Vector<String> groupUsers = FileServer.Chatrooms.elementAt(i).Members;
                            
                            if(!groupUsers.contains(UserName)) {
                                String output = "You are not a member of the group " + groupName;
                                dout.writeUTF(output);
                            }

                            else {
                                String outp="";
                                int j = 0, gp_user_size = groupUsers.size(); 
                                while(j<gp_user_size) {
                                    outp = outp + groupUsers.elementAt(j) + "\n";
                                    File directory = new File(groupUsers.elementAt(j));

                                    String path = new File("").getAbsolutePath();
                                    int rootLength = path.length();

                                    Set<String> outSet = new HashSet<String>();

                                    outSet = listFilesForFolder(outSet, directory, rootLength);

                                    for(String s : outSet) {
                                        outp = outp + '\t' + s;
                                    }

                                    outp = outp + '\n';
                                    j++;
                                }
                                dout.writeUTF(outp);
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in list_detail");
                    }
                }

                else if(command.equals("share_msg")) {
                    try {
                        String groupName = tokenedcommand.nextToken(), message = "\n" + UserName + " :";
                        while(tokenedcommand.hasMoreTokens()) {
                            message = message + " " + tokenedcommand.nextToken();
                        }
                        message = message + "\n";
                        
                        if(FileServer.ConnectedChatroom.get(UserName) == null) {
                            dout.writeUTF("You are not part of any group");
                        }
                        else {
                            Chatroom grp = null;
                            int i = 0, chatroom_arr_size = FileServer.Chatrooms.size();
                            while(i<chatroom_arr_size) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    break;
                                }
                                i++;
                            }

                            if(i == chatroom_arr_size) {
                                String output = groupName + " doesn't exist";
                                dout.writeUTF(output);
                            }
                            else {
                                grp = FileServer.Chatrooms.elementAt(i);
                                int j = 0, gp_user_size = grp.Members.size();
                                while(j<gp_user_size) {
                                    if(!grp.Members.elementAt(j).equals(UserName)) {
                                        try {
                                            Socket sendSoc = FileServer.ClientSockets.elementAt(FileServer.UserNames.indexOf(grp.Members.elementAt(j)));
                                            DataOutputStream senddout = new DataOutputStream(sendSoc.getOutputStream());
                                            senddout.writeUTF(message);
                                        }
                                        catch(Exception e) {
                                            int ii=0;
                                        }
                                    }
                                    j++;
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Error in share_msg");
                    }
                }

                else if(command.equals("get_file")) {
                    try {
                        String fl = tokenedcommand.nextToken();
                        StringTokenizer name = new StringTokenizer(fl,"/");
                        String groupName = name.nextToken(), user = name.nextToken(), filePath = "";
                        filePath = filePath + name.nextToken();

                        while(name.hasMoreTokens()) {
                            filePath = filePath + '/' + name.nextToken();
                        }

                        StringTokenizer name1 = new StringTokenizer(filePath, "/");
                        String fileName = "";
                        while(name1.hasMoreTokens()) {
                            fileName = name1.nextToken();
                        }

                        fl = fileName;
                        fileName = user + '/' + fileName;

                        int i=0, chatroom_arr_size = FileServer.Chatrooms.size();
                        while(i<chatroom_arr_size) {
                            if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                break;
                            }
                            i++;
                        }

                        if(i == chatroom_arr_size) {
                            dout.writeUTF(groupName + " doesn't exist");
                        }
                        else {
                            Vector<String> grpMembers = FileServer.Chatrooms.elementAt(i).Members;
                            if(!grpMembers.contains(user)) {
                                dout.writeUTF("The group " + groupName + " doesn't have the user " + user);
                            }
                            else {
                                int size = 1024;
                                File file = new File(user + '/' + filePath);
                                FileInputStream fpin = new FileInputStream(file);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);

                                long fileLength = file.length(), current=0, start=System.nanoTime();
                                dout.writeUTF("FILE " + fl + " " + user + " " + Long.toString(fileLength));

                                while(current!=fileLength) {
                                    if(fileLength - current >= size) {
                                        current = current + size;
                                    }
                                    else {
                                        size = (int)(fileLength-current);
                                        current=fileLength;
                                    }

                                    byte[] file_contents = new byte[size];
                                    bpin.read(file_contents, 0, size);
                                    dout.write(file_contents);
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                else {
                    dout.writeUTF("Unrecognised command");
                }

            }
            catch(Exception e) {
                e.printStackTrace(System.out);
                break;
            }
        }
    }

    private Set<String> listFilesForFolder(Set<String> set, File folder, int rootLength) {
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory() && !fileEntry.isFile()) {
                // set.add(fileEntry.getAbsolutePath().substring(rootLength + 1) + '/');
                set = listFilesForFolder(set, fileEntry, rootLength);
            } else {
                String relPath = fileEntry.getAbsolutePath();
                relPath = relPath.substring(rootLength + 1);
                set.add(relPath);
                // System.out.println(fileEntry.getName());
            }
        }
        return set;
    }
}

class Chatroom {
    Vector<String> Members = new Vector<String>();
    String name;
    Chatroom (String name,String member) {
        this.name = name;
        this.Members.add(member);
        FileServer.ConnectedChatroom.put(member,this);
    }
    public String Join (String member) {
        this.Members.add(member);
        FileServer.ConnectedChatroom.put(member,this);
        return ("Joined Chatroom "+this.name);
    }
    public String Leave (String member) {
        this.Members.remove(member);
        FileServer.ConnectedChatroom.put(member,null);
        if(this.Members.isEmpty()) return ("DEL");
        else return("You left chatroom "+this.name);
    }
    public Vector<String> ListUsers() {
        return this.Members;
    }
    public String Add(String memberAdd) {
        if(this.Members.contains(memberAdd)) 
            return(memberAdd+" is already a part of "+this.name);
        if(!FileServer.UserNames.contains(memberAdd)) 
            return("The username "+memberAdd+" doesn't exist");
        for(int c=0; c<FileServer.Chatrooms.size();c++)
        {
            Chatroom C = FileServer.Chatrooms.elementAt(c);
            if(C.Members.contains(memberAdd)) 
                return("Cannot add "+memberAdd+" to chatroom "+this.name+"\n"+memberAdd+" already a part of chatroom "+C.name);
        }
        this.Members.add(memberAdd);
        FileServer.ConnectedChatroom.put(memberAdd,this);
        return(memberAdd+" added to chatroom "+this.name);
    }
    public void Notify(String msg,String no_notif) {
        for(int i=0;i<this.Members.size();i++)
        {
            if(!this.Members.elementAt(i).equals(no_notif))
            {
                try {
                    Socket sendSoc = FileServer.ClientSockets.elementAt(FileServer.UserNames.indexOf(this.Members.elementAt(i)));
                    DataOutputStream senddout = new DataOutputStream(sendSoc.getOutputStream());
                    senddout.writeUTF(msg);
                }
                catch(Exception e){ 
                    int ii=0;  
                }
            }
        }
    }
}
